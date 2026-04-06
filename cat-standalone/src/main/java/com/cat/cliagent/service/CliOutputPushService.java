package com.cat.cliagent.service;

/**
 * CLI输出推送服务接口
 *
 * 负责将CLI输出通过WebSocket推送到前端
 */
public interface CliOutputPushService {

    /**
     * 推送输出行到订阅者
     *
     * @param agentId Agent ID
     * @param line 输出行内容
     */
    void pushOutput(String agentId, String line);

    /**
     * 推送错误行到订阅者
     *
     * @param agentId Agent ID
     * @param line 错误行内容
     */
    void pushError(String agentId, String line);

    /**
     * 推送状态变更通知
     *
     * @param agentId Agent ID
     * @param status 新状态
     */
    void pushStatusChange(String agentId, String status);

    /**
     * 推送Token使用信息
     *
     * @param agentId Agent ID
     * @param inputTokens 输入Token数
     * @param outputTokens 输出Token数
     */
    void pushTokenUsage(String agentId, Long inputTokens, Long outputTokens);

    /**
     * 推送流式文本片段（用于--print模式的stream-json输出）
     */
    default void pushTextDelta(String agentId, String text) {
        pushOutput(agentId, text);
    }

    /**
     * 推送响应完成信号
     */
    default void pushDone(String agentId) {
        // default no-op
    }
}