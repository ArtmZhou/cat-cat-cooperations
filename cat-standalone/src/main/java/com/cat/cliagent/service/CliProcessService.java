package com.cat.cliagent.service;

import com.cat.cliagent.dto.CliAgentResponse;

/**
 * CLI进程管理服务接口
 */
public interface CliProcessService {

    /**
     * 启动CLI进程
     */
    CliAgentResponse startProcess(String agentId);

    /**
     * 停止CLI进程
     */
    CliAgentResponse stopProcess(String agentId);

    /**
     * 重启CLI进程
     */
    CliAgentResponse restartProcess(String agentId);

    /**
     * 获取进程状态
     */
    ProcessStatus getProcessStatus(String agentId);

    /**
     * 检查进程是否健康
     */
    boolean isProcessHealthy(String agentId);

    /**
     * 进程状态
     */
    record ProcessStatus(
        String agentId,
        String status,       // STOPPED, STARTING, RUNNING, EXECUTING, ERROR
        String processMode,  // 进程模式描述，如 "per-request"
        Long startTime,
        Long uptimeMs,
        String sessionId,
        String errorMessage
    ) {}
}