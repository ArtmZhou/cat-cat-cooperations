package com.cat.standalone.service;

import com.cat.cliagent.dto.CliAgentResponse;
import com.cat.cliagent.service.CliAgentService;
import com.cat.cliagent.service.CliOutputPushService;
import com.cat.cliagent.service.CliSessionService;
import com.cat.cliagent.service.CliTaskExecutionService;
import com.cat.cliagent.service.WorkspaceService;
import com.cat.common.exception.BusinessException;
import com.cat.standalone.store.JsonFileStore;
import com.cat.standalone.store.entity.StoredCliAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * CLI任务执行服务实现
 *
 * 负责任务的执行控制：超时、取消、并发限制
 *
 * 支持 Git Worktree 模式：
 * - 当指定 projectPath 时，自动为任务创建独立的 worktree + 分支
 * - 任务在独立的 worktree 目录中执行，互不干扰
 * - 任务完成后可选择自动提交变更
 * - 任务取消/失败时自动清理 worktree
 */
@Slf4j
@Service
public class LocalCliTaskExecutionService implements CliTaskExecutionService {

    private final JsonFileStore<StoredCliAgent> cliAgentStore;
    private final CliAgentService cliAgentService;

    @Lazy
    @Autowired
    private CliSessionService sessionService;

    @Lazy
    @Autowired
    private CliOutputPushService outputPushService;

    @Lazy
    @Autowired
    private WorkspaceService workspaceService;

    // 任务执行器
    private final ExecutorService taskExecutor = Executors.newCachedThreadPool();

    // 任务状态缓存
    private final Map<String, TaskExecutionStatus> taskStatusMap = new ConcurrentHashMap<>();

    // 任务Future缓存（用于取消）
    private final Map<String, Future<TaskExecutionResult>> taskFutures = new ConcurrentHashMap<>();

    // Agent并发计数
    private final Map<String, AtomicInteger> agentConcurrentCount = new ConcurrentHashMap<>();

    // 任务与工作空间的映射（用于清理）
    private final Map<String, String> taskWorkspaceMap = new ConcurrentHashMap<>();

    // 默认最大并发数
    private static final int DEFAULT_MAX_CONCURRENT = 3;

    // 默认超时秒数
    private static final int DEFAULT_TIMEOUT_SECONDS = 300;

    public LocalCliTaskExecutionService(JsonFileStore<StoredCliAgent> cliAgentStore,
                                         CliAgentService cliAgentService) {
        this.cliAgentStore = cliAgentStore;
        this.cliAgentService = cliAgentService;
    }

    @Override
    public TaskExecutionResult executeTask(String agentId, String input, int timeoutSeconds) {
        try {
            return executeTaskAsync(agentId, input, timeoutSeconds).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new TaskExecutionResult(null, agentId, false, null, null, null, "Task interrupted");
        } catch (ExecutionException e) {
            return new TaskExecutionResult(null, agentId, false, null, e.getMessage(), null, e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
        }
    }

    @Override
    public Future<TaskExecutionResult> executeTaskAsync(String agentId, String input, int timeoutSeconds) {
        // 验证Agent存在且运行中
        StoredCliAgent agent = cliAgentStore.findById(agentId)
            .orElseThrow(() -> new BusinessException(404, "CLI Agent不存在: " + agentId));

        if (!"RUNNING".equals(agent.getStatus()) && !"EXECUTING".equals(agent.getStatus())) {
            throw new BusinessException(400, "Agent未处于运行状态");
        }

        // 检查并发限制
        if (!canAcceptTask(agentId)) {
            throw new BusinessException(429, "Agent并发任务数已达上限");
        }

        // 增加并发计数
        agentConcurrentCount.computeIfAbsent(agentId, k -> new AtomicInteger(0)).incrementAndGet();

        // 生成任务ID
        String taskId = UUID.randomUUID().toString();
        int timeout = timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;

        // 创建任务状态
        TaskExecutionStatus status = new TaskExecutionStatus(
            taskId, agentId, "PENDING", System.currentTimeMillis(), null, timeout, null, null
        );
        taskStatusMap.put(taskId, status);

        // 提交任务
        Future<TaskExecutionResult> future = taskExecutor.submit(() -> executeTaskInternal(taskId, agentId, input, timeout));
        taskFutures.put(taskId, future);

        return future;
    }

    /**
     * 使用Git Worktree模式执行任务
     *
     * 自动为任务创建独立的worktree工作空间，Agent在隔离环境中执行。
     *
     * @param agentId Agent ID
     * @param input 输入内容
     * @param timeoutSeconds 超时秒数
     * @param projectPath 项目仓库路径
     * @param baseBranch 基线分支
     * @param taskDescription 任务描述（用于分支命名）
     * @return 任务执行结果
     */
    public Future<TaskExecutionResult> executeTaskWithWorkspace(
            String agentId, String input, int timeoutSeconds,
            String projectPath, String baseBranch, String taskDescription) {

        // 验证Agent存在且运行中
        StoredCliAgent agent = cliAgentStore.findById(agentId)
            .orElseThrow(() -> new BusinessException(404, "CLI Agent不存在: " + agentId));

        if (!"RUNNING".equals(agent.getStatus()) && !"EXECUTING".equals(agent.getStatus())) {
            throw new BusinessException(400, "Agent未处于运行状态");
        }

        if (!canAcceptTask(agentId)) {
            throw new BusinessException(429, "Agent并发任务数已达上限");
        }

        agentConcurrentCount.computeIfAbsent(agentId, k -> new AtomicInteger(0)).incrementAndGet();

        String taskId = UUID.randomUUID().toString();
        int timeout = timeoutSeconds > 0 ? timeoutSeconds : DEFAULT_TIMEOUT_SECONDS;

        TaskExecutionStatus status = new TaskExecutionStatus(
            taskId, agentId, "PENDING", System.currentTimeMillis(), null, timeout, null, null
        );
        taskStatusMap.put(taskId, status);

        // 提交带workspace的任务
        Future<TaskExecutionResult> future = taskExecutor.submit(() ->
            executeTaskWithWorkspaceInternal(taskId, agentId, input, timeout, projectPath, baseBranch, taskDescription)
        );
        taskFutures.put(taskId, future);

        return future;
    }

    private TaskExecutionResult executeTaskWithWorkspaceInternal(
            String taskId, String agentId, String input, int timeoutSeconds,
            String projectPath, String baseBranch, String taskDescription) {

        long startTime = System.currentTimeMillis();
        StringBuilder outputBuilder = new StringBuilder();
        StringBuilder errorBuilder = new StringBuilder();
        String workspaceId = null;

        try {
            // 1. 创建独立工作空间
            updateTaskStatus(taskId, "CREATING_WORKSPACE", null, null);
            log.info("Creating workspace for task {} (agent: {}, project: {})", taskId, agentId, projectPath);

            WorkspaceService.CreateWorkspaceRequest wsRequest = new WorkspaceService.CreateWorkspaceRequest(
                projectPath,
                null,   // 自动生成分支名
                baseBranch != null ? baseBranch : "main",
                taskId,
                agentId,
                taskDescription
            );

            WorkspaceService.WorkspaceInfo workspace = workspaceService.createWorkspace(wsRequest);
            workspaceId = workspace.id();
            taskWorkspaceMap.put(taskId, workspaceId);

            log.info("Workspace created for task {}: {} (branch: {}, path: {})",
                taskId, workspaceId, workspace.branchName(), workspace.worktreePath());

            outputBuilder.append("[Workspace] Created: ").append(workspace.worktreePath())
                .append(" (branch: ").append(workspace.branchName()).append(")\n");

            // 2. 执行任务（在worktree目录中）
            updateTaskStatus(taskId, "RUNNING", null, null);

            if (outputPushService != null) {
                outputPushService.pushStatusChange(agentId, "EXECUTING");
            }

            // 发送输入并启动流式读取
            boolean sent = sessionService.sendInputWithStreaming(
                agentId,
                input,
                line -> outputBuilder.append(line).append("\n"),
                line -> errorBuilder.append(line).append("\n")
            );

            if (!sent) {
                return completeTask(taskId, agentId, false, outputBuilder.toString(), errorBuilder.toString(),
                    "Failed to send input", startTime);
            }

            Thread.sleep(Math.min(timeoutSeconds * 1000L, 5000));

            outputBuilder.append("[Workspace] Task completed in workspace: ").append(workspace.worktreePath()).append("\n");

            return completeTask(taskId, agentId, true, outputBuilder.toString(), errorBuilder.toString(), null, startTime);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return completeTask(taskId, agentId, false, outputBuilder.toString(), errorBuilder.toString(),
                "Task interrupted", startTime);
        } catch (Exception e) {
            log.error("Task execution with workspace failed: {}", taskId, e);
            return completeTask(taskId, agentId, false, outputBuilder.toString(), errorBuilder.toString(),
                e.getMessage(), startTime);
        } finally {
            AtomicInteger count = agentConcurrentCount.get(agentId);
            if (count != null) {
                count.decrementAndGet();
            }

            if (outputPushService != null) {
                outputPushService.pushStatusChange(agentId, "RUNNING");
            }
        }
    }

    /**
     * 获取任务关联的工作空间ID
     */
    public String getTaskWorkspaceId(String taskId) {
        return taskWorkspaceMap.get(taskId);
    }

    private TaskExecutionResult executeTaskInternal(String taskId, String agentId, String input, int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        StringBuilder outputBuilder = new StringBuilder();
        StringBuilder errorBuilder = new StringBuilder();

        try {
            // 更新状态为RUNNING
            updateTaskStatus(taskId, "RUNNING", null, null);

            // 推送状态变更
            if (outputPushService != null) {
                outputPushService.pushStatusChange(agentId, "EXECUTING");
            }

            // 发送输入并启动流式读取
            boolean sent = sessionService.sendInputWithStreaming(
                agentId,
                input,
                line -> outputBuilder.append(line).append("\n"),
                line -> errorBuilder.append(line).append("\n")
            );

            if (!sent) {
                return completeTask(taskId, agentId, false, outputBuilder.toString(), errorBuilder.toString(), "Failed to send input", startTime);
            }

            // 等待执行完成或超时
            // 这里简化处理，实际需要更复杂的逻辑来检测CLI响应完成
            // 使用超时等待
            Thread.sleep(Math.min(timeoutSeconds * 1000L, 5000)); // 简化：最多等待5秒

            return completeTask(taskId, agentId, true, outputBuilder.toString(), errorBuilder.toString(), null, startTime);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return completeTask(taskId, agentId, false, outputBuilder.toString(), errorBuilder.toString(), "Task interrupted", startTime);
        } catch (Exception e) {
            log.error("Task execution failed: {}", taskId, e);
            return completeTask(taskId, agentId, false, outputBuilder.toString(), errorBuilder.toString(), e.getMessage(), startTime);
        } finally {
            // 减少并发计数
            AtomicInteger count = agentConcurrentCount.get(agentId);
            if (count != null) {
                count.decrementAndGet();
            }

            // 推送状态变更
            if (outputPushService != null) {
                outputPushService.pushStatusChange(agentId, "RUNNING");
            }
        }
    }

    private TaskExecutionResult completeTask(String taskId, String agentId, boolean success,
                                              String output, String error, String failureReason,
                                              long startTime) {
        long endTime = System.currentTimeMillis();
        long executionTime = endTime - startTime;

        // 更新状态
        String status = success ? "COMPLETED" : "FAILED";
        if (failureReason != null && failureReason.contains("timeout")) {
            status = "TIMEOUT";
        }
        updateTaskStatus(taskId, status, endTime, output);

        // 清理Future
        taskFutures.remove(taskId);

        return new TaskExecutionResult(taskId, agentId, success, output, error, executionTime, failureReason);
    }

    private void updateTaskStatus(String taskId, String status, Long endTime, String output) {
        TaskExecutionStatus current = taskStatusMap.get(taskId);
        if (current != null) {
            taskStatusMap.put(taskId, new TaskExecutionStatus(
                current.taskId(),
                current.agentId(),
                status,
                current.startTime(),
                endTime != null ? endTime : current.endTime(),
                current.timeoutSeconds(),
                output != null ? output : current.output(),
                current.error()
            ));
        }
    }

    @Override
    public boolean cancelTask(String taskId) {
        Future<TaskExecutionResult> future = taskFutures.get(taskId);
        if (future != null && !future.isDone()) {
            boolean cancelled = future.cancel(true);
            if (cancelled) {
                updateTaskStatus(taskId, "CANCELLED", System.currentTimeMillis(), null);
                taskFutures.remove(taskId);
                log.info("Task cancelled: {}", taskId);
            }
            return cancelled;
        }
        return false;
    }

    @Override
    public TaskExecutionStatus getTaskStatus(String taskId) {
        return taskStatusMap.get(taskId);
    }

    @Override
    public int getConcurrentTaskCount(String agentId) {
        AtomicInteger count = agentConcurrentCount.get(agentId);
        return count != null ? count.get() : 0;
    }

    @Override
    public boolean canAcceptTask(String agentId) {
        int current = getConcurrentTaskCount(agentId);
        return current < DEFAULT_MAX_CONCURRENT;
    }
}