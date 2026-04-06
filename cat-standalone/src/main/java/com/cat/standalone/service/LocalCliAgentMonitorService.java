package com.cat.standalone.service;

import com.cat.cliagent.dto.CliAgentMonitorStatus;
import com.cat.cliagent.service.*;
import com.cat.common.exception.BusinessException;
import com.cat.standalone.store.JsonFileStore;
import com.cat.standalone.store.entity.StoredCliAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * CLI Agent监控服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalCliAgentMonitorService implements CliAgentMonitorService {

    private final JsonFileStore<StoredCliAgent> cliAgentStore;
    private final CliProcessService processService;
    private final CliSessionService sessionService;
    private final CliTaskExecutionService taskExecutionService;
    private final TokenUsageService tokenUsageService;

    @Override
    public CliAgentMonitorStatus getAgentMonitorStatus(String agentId) {
        StoredCliAgent agent = cliAgentStore.findById(agentId)
            .orElseThrow(() -> new BusinessException(404, "CLI Agent不存在: " + agentId));

        return buildMonitorStatus(agent);
    }

    @Override
    public List<CliAgentMonitorStatus> getAllAgentMonitorStatus() {
        List<CliAgentMonitorStatus> result = new ArrayList<>();

        cliAgentStore.findAll().forEach(agent -> {
            result.add(buildMonitorStatus(agent));
        });

        return result;
    }

    @Override
    public List<CliAgentMonitorStatus> getRunningAgentMonitorStatus() {
        List<CliAgentMonitorStatus> result = new ArrayList<>();

        cliAgentStore.findAll().stream()
            .filter(a -> "RUNNING".equals(a.getStatus()) || "EXECUTING".equals(a.getStatus()))
            .forEach(agent -> result.add(buildMonitorStatus(agent)));

        return result;
    }

    @Override
    public SystemOverview getSystemOverview() {
        List<StoredCliAgent> allAgents = cliAgentStore.findAll();

        int total = allAgents.size();
        int running = 0;
        int executing = 0;
        int stopped = 0;
        int error = 0;
        long totalInputTokens = 0;
        long totalOutputTokens = 0;
        int activeSessions = 0;
        int concurrentTasks = 0;

        for (StoredCliAgent agent : allAgents) {
            switch (agent.getStatus()) {
                case "RUNNING" -> running++;
                case "EXECUTING" -> executing++;
                case "STOPPED" -> stopped++;
                case "ERROR" -> error++;
            }

            // Token统计
            TokenUsageService.TokenUsageStats tokenStats = tokenUsageService.getAgentStats(agent.getId());
            totalInputTokens += tokenStats.totalInputTokens();
            totalOutputTokens += tokenStats.totalOutputTokens();

            // 会话统计
            if (sessionService.isSessionActive(agent.getId())) {
                activeSessions++;
            }

            // 任务统计
            concurrentTasks += taskExecutionService.getConcurrentTaskCount(agent.getId());
        }

        return new SystemOverview(
            total, running, executing, stopped, error,
            totalInputTokens, totalOutputTokens,
            activeSessions, concurrentTasks
        );
    }

    private CliAgentMonitorStatus buildMonitorStatus(StoredCliAgent agent) {
        CliAgentMonitorStatus status = new CliAgentMonitorStatus();
        status.setAgentId(agent.getId());
        status.setName(agent.getName());
        status.setStatus(agent.getStatus());
        status.setUpdatedAt(agent.getUpdatedAt());

        // 进程状态
        CliAgentMonitorStatus.ProcessStatus processStatus = new CliAgentMonitorStatus.ProcessStatus();
        CliProcessService.ProcessStatus ps = processService.getProcessStatus(agent.getId());
        processStatus.setProcessId(ps.processId());
        processStatus.setUptimeMs(ps.uptimeMs());
        processStatus.setAlive(ps.processId() != null);
        processStatus.setLastError(ps.errorMessage());
        status.setProcessStatus(processStatus);

        // 会话状态
        CliAgentMonitorStatus.SessionStatus sessionStatus = new CliAgentMonitorStatus.SessionStatus();
        CliSessionService.SessionStatus ss = sessionService.getSessionStatus(agent.getId());
        sessionStatus.setActive(ss.active());
        sessionStatus.setInputStreamOpen(ss.inputStreamOpen());
        sessionStatus.setOutputStreamOpen(ss.outputStreamOpen());
        sessionStatus.setLinesReceived(ss.linesReceived());
        sessionStatus.setBytesSent(ss.bytesSent());
        status.setSessionStatus(sessionStatus);

        // Token状态
        CliAgentMonitorStatus.TokenStatus tokenStatus = new CliAgentMonitorStatus.TokenStatus();
        TokenUsageService.TokenUsageStats ts = tokenUsageService.getAgentStats(agent.getId());
        tokenStatus.setTotalInputTokens(ts.totalInputTokens());
        tokenStatus.setTotalOutputTokens(ts.totalOutputTokens());
        tokenStatus.setTotalTokens(ts.totalTokens());
        tokenStatus.setRecordCount(ts.recordCount());
        status.setTokenStatus(tokenStatus);

        // 任务状态
        CliAgentMonitorStatus.TaskStatus taskStatus = new CliAgentMonitorStatus.TaskStatus();
        taskStatus.setConcurrentTasks(taskExecutionService.getConcurrentTaskCount(agent.getId()));
        taskStatus.setCanAcceptTask(taskExecutionService.canAcceptTask(agent.getId()));
        status.setTaskStatus(taskStatus);

        return status;
    }
}