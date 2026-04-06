package com.cat.standalone.service;

import com.cat.standalone.store.JsonFileStore;
import com.cat.standalone.store.entity.*;
import com.cat.task.dto.*;
import com.cat.task.entity.TaskAssignment;
import com.cat.task.service.TaskService;
import com.cat.common.exception.BusinessException;
import com.cat.common.model.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 本地Task服务实现
 *
 * 使用JSON文件存储替代MyBatis数据库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalTaskService implements TaskService {

    private final JsonFileStore<StoredTask> taskStore;
    private final JsonFileStore<StoredTaskAssignment> assignmentStore;
    private final JsonFileStore<StoredTaskLog> taskLogStore;

    @Override
    public TaskResponse createTask(CreateTaskRequest request, String userId) {
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        StoredTask task = new StoredTask();
        task.setId(id);
        task.setName(request.getName());
        task.setDescription(request.getDescription());
        task.setType(request.getType());
        task.setStatus("PENDING");
        task.setPriority(request.getPriority() != null ? request.getPriority() : 0);
        task.setInput(request.getInput());
        task.setConfig(request.getConfig());
        task.setTimeoutSeconds(request.getTimeoutSeconds() != null ? request.getTimeoutSeconds() : 3600);
        task.setMaxRetry(request.getMaxRetry() != null ? request.getMaxRetry() : 3);
        task.setRetryCount(0);
        task.setCreatedBy(userId != null ? userId : "system");
        task.setCreatedAt(now);
        task.setUpdatedAt(now);

        taskStore.save(id, task);

        log.info("Task created: {} by user: {}", task.getName(), userId);
        return buildTaskResponse(task);
    }

    @Override
    public TaskResponse updateTask(String taskId, UpdateTaskRequest request) {
        StoredTask task = taskStore.findById(taskId)
            .orElseThrow(() -> new BusinessException(404, "Task不存在"));

        if (StringUtils.hasText(request.getName())) {
            task.setName(request.getName());
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            task.setPriority(request.getPriority());
        }
        if (request.getConfig() != null) {
            task.setConfig(request.getConfig());
        }
        task.setUpdatedAt(LocalDateTime.now());

        taskStore.save(taskId, task);
        log.info("Task updated: {}", taskId);
        return buildTaskResponse(task);
    }

    @Override
    public void deleteTask(String taskId) {
        StoredTask task = taskStore.findById(taskId)
            .orElseThrow(() -> new BusinessException(404, "Task不存在"));

        // 删除关联的分配和日志
        assignmentStore.delete(a -> a.getTaskId().equals(taskId));
        taskLogStore.delete(l -> l.getTaskId().equals(taskId));

        taskStore.deleteById(taskId);
        log.info("Task deleted: {}", taskId);
    }

    @Override
    public TaskResponse getTask(String taskId) {
        StoredTask task = taskStore.findById(taskId)
            .orElseThrow(() -> new BusinessException(404, "Task不存在"));
        return buildTaskResponse(task);
    }

    @Override
    public PageResult<TaskResponse> listTasks(TaskQuery query) {
        JsonFileStore.PageResult<StoredTask> page = taskStore.findPage(
            query.getPage() != null ? query.getPage() : 1,
            query.getPageSize() != null ? query.getPageSize() : 20,
            buildPredicate(query)
        );

        List<TaskResponse> items = page.getItems().stream()
            .map(this::buildTaskResponse)
            .collect(Collectors.toList());

        return new PageResult<>(items, page.getTotal(), page.getPage(), page.getPageSize(), page.getTotalPages());
    }

    private java.util.function.Predicate<StoredTask> buildPredicate(TaskQuery query) {
        return task -> {
            if (query == null) return true;
            if (StringUtils.hasText(query.getName()) && !task.getName().contains(query.getName())) {
                return false;
            }
            if (StringUtils.hasText(query.getType()) && !task.getType().equals(query.getType())) {
                return false;
            }
            if (StringUtils.hasText(query.getStatus()) && !task.getStatus().equals(query.getStatus())) {
                return false;
            }
            return true;
        };
    }

    @Override
    public TaskResponse assignTask(String taskId, List<String> agentIds) {
        StoredTask task = taskStore.findById(taskId)
            .orElseThrow(() -> new BusinessException(404, "Task不存在"));

        LocalDateTime now = LocalDateTime.now();

        for (String agentId : agentIds) {
            String assignmentId = UUID.randomUUID().toString();
            StoredTaskAssignment assignment = new StoredTaskAssignment();
            assignment.setId(assignmentId);
            assignment.setTaskId(taskId);
            assignment.setAgentId(agentId);
            assignment.setStatus("ASSIGNED");
            assignment.setAssignedAt(now);
            assignmentStore.save(assignmentId, assignment);
        }

        task.setStatus("ASSIGNED");
        task.setUpdatedAt(now);
        taskStore.save(taskId, task);

        log.info("Task {} assigned to {} agents", taskId, agentIds.size());
        return buildTaskResponse(task);
    }

    @Override
    public TaskResponse startTask(String taskId) {
        StoredTask task = taskStore.findById(taskId)
            .orElseThrow(() -> new BusinessException(404, "Task不存在"));

        task.setStatus("RUNNING");
        task.setStartedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskStore.save(taskId, task);

        log.info("Task started: {}", taskId);
        return buildTaskResponse(task);
    }

    @Override
    public TaskResponse completeTask(String taskId, String output) {
        StoredTask task = taskStore.findById(taskId)
            .orElseThrow(() -> new BusinessException(404, "Task不存在"));

        task.setStatus("COMPLETED");
        task.setOutput(output);
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskStore.save(taskId, task);

        log.info("Task completed: {}", taskId);
        return buildTaskResponse(task);
    }

    @Override
    public TaskResponse failTask(String taskId, String errorMessage) {
        StoredTask task = taskStore.findById(taskId)
            .orElseThrow(() -> new BusinessException(404, "Task不存在"));

        task.setStatus("FAILED");
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskStore.save(taskId, task);

        log.info("Task failed: {}, error: {}", taskId, errorMessage);
        return buildTaskResponse(task);
    }

    @Override
    public TaskResponse cancelTask(String taskId) {
        StoredTask task = taskStore.findById(taskId)
            .orElseThrow(() -> new BusinessException(404, "Task不存在"));

        task.setStatus("CANCELLED");
        task.setCompletedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskStore.save(taskId, task);

        log.info("Task cancelled: {}", taskId);
        return buildTaskResponse(task);
    }

    @Override
    public TaskResponse retryTask(String taskId) {
        StoredTask task = taskStore.findById(taskId)
            .orElseThrow(() -> new BusinessException(404, "Task不存在"));

        // 只有失败或取消的任务可以重试
        if (!"FAILED".equals(task.getStatus()) && !"CANCELLED".equals(task.getStatus())) {
            throw new BusinessException(400, "只有失败或已取消的任务可以重试");
        }

        task.setStatus("PENDING");
        task.setRetryCount(task.getRetryCount() + 1);
        task.setStartedAt(null);
        task.setCompletedAt(null);
        task.setUpdatedAt(LocalDateTime.now());
        taskStore.save(taskId, task);

        log.info("Task retried: {}", taskId);
        return buildTaskResponse(task);
    }

    @Override
    public void addTaskLog(String taskId, String agentId, String level, String message) {
        String logId = UUID.randomUUID().toString();
        StoredTaskLog taskLog = new StoredTaskLog();
        taskLog.setId(logId);
        taskLog.setTaskId(taskId);
        taskLog.setAgentId(agentId);
        taskLog.setLevel(level);
        taskLog.setMessage(message);
        taskLog.setCreatedAt(LocalDateTime.now());
        taskLogStore.save(logId, taskLog);
    }

    @Override
    public List<TaskLogResponse> getTaskLogs(String taskId) {
        return taskLogStore.find(l -> l.getTaskId().equals(taskId)).stream()
            .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
            .map(this::toLogResponse)
            .collect(Collectors.toList());
    }

    private TaskLogResponse toLogResponse(StoredTaskLog log) {
        TaskLogResponse response = new TaskLogResponse();
        response.setId(log.getId());
        response.setTaskId(log.getTaskId());
        response.setAgentId(log.getAgentId());
        response.setLevel(log.getLevel());
        response.setMessage(log.getMessage());
        response.setDetail(log.getDetail());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }

    private TaskResponse buildTaskResponse(StoredTask task) {
        TaskResponse response = new TaskResponse();
        response.setId(task.getId());
        response.setName(task.getName());
        response.setDescription(task.getDescription());
        response.setType(task.getType());
        response.setStatus(task.getStatus());
        response.setPriority(task.getPriority());
        response.setInput(task.getInput());
        response.setOutput(task.getOutput());
        response.setConfig(task.getConfig());
        response.setTimeoutSeconds(task.getTimeoutSeconds());
        response.setRetryCount(task.getRetryCount());
        response.setMaxRetry(task.getMaxRetry());
        response.setScheduledAt(task.getScheduledAt());
        response.setStartedAt(task.getStartedAt());
        response.setCompletedAt(task.getCompletedAt());
        response.setCreatedBy(task.getCreatedBy());
        response.setCreatedAt(task.getCreatedAt());
        response.setUpdatedAt(task.getUpdatedAt());

        // 加载分配
        List<StoredTaskAssignment> assignments = assignmentStore.find(a -> a.getTaskId().equals(task.getId()));
        List<TaskAssignment> assignList = assignments.stream().map(this::toAssignment).collect(Collectors.toList());
        response.setAssignments(assignList);

        return response;
    }

    private TaskAssignment toAssignment(StoredTaskAssignment a) {
        TaskAssignment assignment = new TaskAssignment();
        assignment.setId(a.getId());
        assignment.setTaskId(a.getTaskId());
        assignment.setAgentId(a.getAgentId());
        assignment.setRole(a.getRole());
        assignment.setStatus(a.getStatus());
        assignment.setAssignedAt(a.getAssignedAt());
        assignment.setStartedAt(a.getStartedAt());
        assignment.setCompletedAt(a.getCompletedAt());
        assignment.setResult(a.getResult());
        assignment.setErrorMessage(a.getErrorMessage());
        return assignment;
    }
}