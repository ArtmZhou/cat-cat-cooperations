package com.cat.cliagent.service;

import com.cat.cliagent.dto.CliAgentResponse;

import java.util.function.Consumer;

/**
 * CLI会话通信服务接口
 *
 * 负责与CLI进程进行stdin/stdout管道通信
 */
public interface CliSessionService {

    /**
     * 发送输入到CLI进程
     *
     * @param agentId Agent ID
     * @param input 输入内容（Prompt/JSON/文件路径）
     * @return 是否发送成功
     */
    boolean sendInput(String agentId, String input);

    /**
     * 发送输入并开始流式读取输出
     *
     * @param agentId Agent ID
     * @param input 输入内容
     * @param outputConsumer 输出行消费者
     * @param errorConsumer 错误消费者
     * @return 是否启动成功
     */
    boolean sendInputWithStreaming(String agentId, String input,
                                    Consumer<String> outputConsumer,
                                    Consumer<String> errorConsumer);

    /**
     * 开始流式读取输出（不发送输入）
     *
     * @param agentId Agent ID
     * @param outputConsumer 输出行消费者
     * @param errorConsumer 错误消费者
     * @return 是否启动成功
     */
    boolean startStreaming(String agentId,
                           Consumer<String> outputConsumer,
                           Consumer<String> errorConsumer);

    /**
     * 停止流式读取
     *
     * @param agentId Agent ID
     */
    void stopStreaming(String agentId);

    /**
     * 检查会话是否活跃
     *
     * @param agentId Agent ID
     * @return 会话是否活跃
     */
    boolean isSessionActive(String agentId);

    /**
     * 获取会话状态
     *
     * @param agentId Agent ID
     * @return 会话状态
     */
    SessionStatus getSessionStatus(String agentId);

    /**
     * 关闭会话
     *
     * @param agentId Agent ID
     * @return Agent响应
     */
    CliAgentResponse closeSession(String agentId);

    /**
     * 会话状态
     */
    record SessionStatus(
        String agentId,
        boolean active,
        boolean inputStreamOpen,
        boolean outputStreamOpen,
        boolean errorStreamOpen,
        Long linesReceived,
        Long bytesSent,
        String lastError
    ) {}
}