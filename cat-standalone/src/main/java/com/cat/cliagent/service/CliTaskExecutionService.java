package com.cat.cliagent.service;

import com.cat.cliagent.dto.CliAgentResponse;

import java.util.concurrent.Future;

/**
 * CLI任务执行服务接口
 *
 * 负责任务的执行控制：超时、取消、并发限制
 */
public interface CliTaskExecutionService {

    /**
     * 执行任务
     *
     * @param agentId Agent ID
     * @param input 输入内容
     * @param timeoutSeconds 超时秒数（0表示不超时）
     * @return 任务执行结果
     */
    TaskExecutionResult executeTask(String agentId, String input, int timeoutSeconds);

    /**
     * 异步执行任务
     *
     * @param agentId Agent ID
     * @param input 输入内容
     * @param timeoutSeconds 超时秒数
     * @return Future用于跟踪执行状态
     */
    Future<TaskExecutionResult> executeTaskAsync(String agentId, String input, int timeoutSeconds);

    /**
     * 取消任务
     *
     * @param taskId 任务ID
     * @return 是否取消成功
     */
    boolean cancelTask(String taskId);

    /**
     * 获取任务执行状态
     *
     * @param taskId 任务ID
     * @return 任务执行状态
     */
    TaskExecutionStatus getTaskStatus(String taskId);

    /**
     * 获取Agent的并发任务数
     *
     * @param agentId Agent ID
     * @return 当前并发任务数
     */
    int getConcurrentTaskCount(String agentId);

    /**
     * 检查Agent是否可接受新任务
     *
     * @param agentId Agent ID
     * @return 是否可接受
     */
    boolean canAcceptTask(String agentId);

    /**
     * 任务执行结果
     */
    record TaskExecutionResult(
        String taskId,
        String agentId,
        boolean success,
        String output,
        String error,
        Long executionTimeMs,
        String failureReason
    ) {}

    /**
     * 任务执行状态
     */
    record TaskExecutionStatus(
        String taskId,
        String agentId,
        String status, // PENDING, RUNNING, COMPLETED, FAILED, CANCELLED, TIMEOUT
        Long startTime,
        Long endTime,
        Integer timeoutSeconds,
        String output,
        String error
    ) {}
}