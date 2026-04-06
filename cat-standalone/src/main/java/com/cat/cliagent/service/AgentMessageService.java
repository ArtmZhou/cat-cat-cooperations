package com.cat.cliagent.service;

import com.cat.cliagent.dto.CliAgentResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent消息通信服务接口
 */
public interface AgentMessageService {

    /**
     * 发送消息给Agent
     *
     * @param fromAgentId 发送方Agent ID
     * @param toAgentId 接收方Agent ID
     * @param message 消息内容
     * @return 是否发送成功
     */
    boolean sendMessage(String fromAgentId, String toAgentId, String message);

    /**
     * 广播消息给所有运行中的Agent
     *
     * @param fromAgentId 发送方Agent ID
     * @param message 消息内容
     * @return 接收到消息的Agent数量
     */
    int broadcastMessage(String fromAgentId, String message);

    /**
     * 获取Agent的待处理消息
     *
     * @param agentId Agent ID
     * @return 消息列表
     */
    List<AgentMessage> getPendingMessages(String agentId);

    /**
     * 确认消息已处理
     *
     * @param agentId Agent ID
     * @param messageId 消息ID
     */
    void ackMessage(String agentId, String messageId);

    /**
     * 清理过期消息
     *
     * @param beforeTime 清理此时间之前的消息
     * @return 清理的消息数量
     */
    int cleanupExpiredMessages(LocalDateTime beforeTime);

    /**
     * Agent消息
     */
    record AgentMessage(
        String id,
        String fromAgentId,
        String fromAgentName,
        String toAgentId,
        String message,
        LocalDateTime createdAt,
        boolean processed
    ) {}
}