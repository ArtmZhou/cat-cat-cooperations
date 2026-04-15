package com.cat.standalone.service;

import com.cat.cliagent.dto.CliAgentResponse;
import com.cat.cliagent.service.CliAgentService;
import com.cat.cliagent.service.CliAgentTemplateService;
import com.cat.cliagent.service.CliProcessService;
import com.cat.common.exception.BusinessException;
import com.cat.standalone.store.JsonFileStore;
import com.cat.standalone.store.entity.StoredCliAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;

/**
 * CLI进程管理服务实现
 *
 * 使用 --print 模式进行每请求独立执行
 */
@Slf4j
@Service
public class LocalCliProcessService implements CliProcessService {

    private final JsonFileStore<StoredCliAgent> cliAgentStore;
    private final CliAgentTemplateService templateService;
    private final CliAgentService cliAgentService;

    public LocalCliProcessService(JsonFileStore<StoredCliAgent> cliAgentStore,
                                   CliAgentTemplateService templateService,
                                   CliAgentService cliAgentService) {
        this.cliAgentStore = cliAgentStore;
        this.templateService = templateService;
        this.cliAgentService = cliAgentService;
    }

    /**
     * 服务启动时重置所有运行中的Agent状态为STOPPED
     * 因为服务重启后，之前的进程上下文已丢失
     */
    @PostConstruct
    public void resetAgentStatusOnStartup() {
        List<StoredCliAgent> runningAgents = cliAgentStore.find(
            agent -> "RUNNING".equals(agent.getStatus()) || "EXECUTING".equals(agent.getStatus())
        );

        if (!runningAgents.isEmpty()) {
            log.info("Service restarted, resetting {} running/executing agents to STOPPED", runningAgents.size());
            for (StoredCliAgent agent : runningAgents) {
                log.info("Resetting agent '{}' (id={}) from {} to STOPPED", agent.getName(), agent.getId(), agent.getStatus());
                agent.setStatus("STOPPED");
                agent.setProcessId(null);
                agent.setSessionId(null);
                agent.setLastStoppedAt(LocalDateTime.now());
                agent.setUpdatedAt(LocalDateTime.now());
                cliAgentStore.save(agent.getId(), agent);
            }
            log.info("All agents have been reset to STOPPED state");
        }
    }

    @Override
    public CliAgentResponse startProcess(String agentId) {
        StoredCliAgent agent = cliAgentStore.findById(agentId)
            .orElseThrow(() -> new BusinessException(404, "CLI Agent不存在: " + agentId));

        // 检查当前状态
        if ("RUNNING".equals(agent.getStatus()) || "EXECUTING".equals(agent.getStatus())) {
            throw new BusinessException(400, "Agent已在运行中");
        }

        try {
            // 验证可执行文件路径
            String executablePath = agent.getExecutablePath();
            if (executablePath == null) {
                var template = templateService.getTemplate(agent.getTemplateId());
                executablePath = template.getExecutablePath();
            }

            // 验证可执行文件是否存在
            File executable = new File(executablePath);
            if (!executable.exists() && !executablePath.contains("/") && !executablePath.contains("\\")) {
                // 可能是系统命令（如claude），尝试在PATH中查找
                log.info("Executable not found as file, assuming it's in PATH: {}", executablePath);
            } else if (!executable.exists()) {
                throw new BusinessException(400, "可执行文件不存在: " + executablePath);
            }

            // 验证工作目录
            if (agent.getWorkingDir() != null) {
                File workingDir = new File(agent.getWorkingDir());
                if (!workingDir.exists() || !workingDir.isDirectory()) {
                    throw new BusinessException(400, "工作目录不存在: " + agent.getWorkingDir());
                }
            }

            log.info("Starting CLI agent {} (per-request --print mode)", agent.getName());

            // 更新状态为RUNNING
            agent.setStatus("RUNNING");
            agent.setProcessId("per-request-mode"); // 标记为每请求模式
            agent.setLastStartedAt(LocalDateTime.now());
            agent.setUpdatedAt(LocalDateTime.now());
            cliAgentStore.save(agentId, agent);

            log.info("CLI agent {} is ready for per-request execution", agent.getName());

            return cliAgentService.getAgent(agentId);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to start CLI agent: {}", agentId, e);

            // 更新状态为ERROR
            agent.setStatus("ERROR");
            agent.setUpdatedAt(LocalDateTime.now());
            cliAgentStore.save(agentId, agent);

            throw new BusinessException(500, "启动CLI Agent失败: " + e.getMessage());
        }
    }

    @Override
    public CliAgentResponse stopProcess(String agentId) {
        StoredCliAgent agent = cliAgentStore.findById(agentId)
            .orElseThrow(() -> new BusinessException(404, "CLI Agent不存在: " + agentId));

        // 检查当前状态
        if ("STOPPED".equals(agent.getStatus())) {
            throw new BusinessException(400, "Agent已停止");
        }

        log.info("Stopping CLI agent: {}", agent.getName());

        // 清除会话ID（丢弃对话上下文）
        agent.setSessionId(null);

        // 更新状态
        agent.setStatus("STOPPED");
        agent.setProcessId(null);
        agent.setLastStoppedAt(LocalDateTime.now());
        agent.setUpdatedAt(LocalDateTime.now());
        cliAgentStore.save(agentId, agent);

        log.info("CLI agent stopped: {}", agent.getName());

        return cliAgentService.getAgent(agentId);
    }

    @Override
    public CliAgentResponse restartProcess(String agentId) {
        StoredCliAgent agent = cliAgentStore.findById(agentId)
            .orElseThrow(() -> new BusinessException(404, "CLI Agent不存在: " + agentId));

        // 先停止
        if (!"STOPPED".equals(agent.getStatus())) {
            stopProcess(agentId);
        }

        // 短暂等待
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 再启动
        return startProcess(agentId);
    }

    @Override
    public ProcessStatus getProcessStatus(String agentId) {
        StoredCliAgent agent = cliAgentStore.findById(agentId)
            .orElseThrow(() -> new BusinessException(404, "CLI Agent不存在: " + agentId));

        // 在--print模式下，返回有意义的状态信息
        Long startTime = agent.getLastStartedAt() != null
            ? agent.getLastStartedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
            : null;

        // 计算运行时长（如果处于RUNNING或EXECUTING状态）
        Long uptime = null;
        if (startTime != null && ("RUNNING".equals(agent.getStatus()) || "EXECUTING".equals(agent.getStatus()))) {
            uptime = System.currentTimeMillis() - startTime;
        }

        // 确定进程模式
        String processMode = "per-request-mode".equals(agent.getProcessId()) ? "每请求模式 (--print)" : agent.getProcessId();

        return new ProcessStatus(
            agentId,
            agent.getStatus(),
            processMode,
            startTime,
            uptime,
            agent.getSessionId(),
            null
        );
    }

    @Override
    public boolean isProcessHealthy(String agentId) {
        StoredCliAgent agent = cliAgentStore.findById(agentId)
            .orElse(null);

        if (agent == null) {
            return false;
        }

        // 在--print模式下，只要状态不是ERROR就算健康
        return !"ERROR".equals(agent.getStatus());
    }
}
