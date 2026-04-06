package com.cat.cliagent.service;

import com.cat.cliagent.dto.CliAgentMonitorStatus;

import java.util.List;

/**
 * CLI Agent监控服务接口
 */
public interface CliAgentMonitorService {

    /**
     * 获取单个Agent的监控状态
     *
     * @param agentId Agent ID
     * @return 监控状态
     */
    CliAgentMonitorStatus getAgentMonitorStatus(String agentId);

    /**
     * 获取所有Agent的监控状态
     *
     * @return 监控状态列表
     */
    List<CliAgentMonitorStatus> getAllAgentMonitorStatus();

    /**
     * 获取运行中的Agent监控状态
     *
     * @return 运行中的Agent监控状态列表
     */
    List<CliAgentMonitorStatus> getRunningAgentMonitorStatus();

    /**
     * 获取系统概览统计
     *
     * @return 系统概览
     */
    SystemOverview getSystemOverview();

    /**
     * 系统概览
     */
    record SystemOverview(
        int totalAgents,
        int runningAgents,
        int executingAgents,
        int stoppedAgents,
        int errorAgents,
        long totalInputTokens,
        long totalOutputTokens,
        int activeSessions,
        int concurrentTasks
    ) {}
}