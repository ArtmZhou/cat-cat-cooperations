package com.cat.standalone.service;

import com.cat.cliagent.service.AgentMessageService;
import com.cat.cliagent.service.CliOutputPushService;
import com.cat.standalone.store.JsonFileStore;
import com.cat.standalone.store.entity.StoredCliAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Agent消息通信服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalAgentMessageService implements AgentMessageService {

    private final JsonFileStore<StoredCliAgent> cliAgentStore;
    private final CliOutputPushService outputPushService;

    // 消息队列：agentId -> 消息列表
    private final Map<String, List<AgentMessage>> messageQueues = new ConcurrentHashMap<>();

    // 消息ID计数器
    private final AtomicLong messageIdCounter = new AtomicLong(0);

    @Override
    public boolean sendMessage(String fromAgentId, String toAgentId, String message) {
        // 验证发送方
        StoredCliAgent fromAgent = cliAgentStore.findById(fromAgentId).orElse(null);
        if (fromAgent == null) {
            log.warn("From agent not found: {}", fromAgentId);
            return false;
        }

        // 验证接收方
        StoredCliAgent toAgent = cliAgentStore.findById(toAgentId).orElse(null);
        if (toAgent == null) {
            log.warn("To agent not found: {}", toAgentId);
            return false;
        }

        // 创建消息
        AgentMessage msg = new AgentMessage(
            String.valueOf(messageIdCounter.incrementAndGet()),
            fromAgentId,
            fromAgent.getName(),
            toAgentId,
            message,
            LocalDateTime.now(),
            false
        );

        // 添加到接收方队列
        messageQueues.computeIfAbsent(toAgentId, k -> new ArrayList<>()).add(msg);

        // 推送通知
        if (outputPushService != null) {
            outputPushService.pushOutput(toAgentId, "[MESSAGE] " + fromAgent.getName() + ": " + message);
        }

        log.info("Message sent from {} to {}: {}", fromAgentId, toAgentId, message);
        return true;
    }

    @Override
    public int broadcastMessage(String fromAgentId, String message) {
        // 验证发送方
        StoredCliAgent fromAgent = cliAgentStore.findById(fromAgentId).orElse(null);
        if (fromAgent == null) {
            log.warn("From agent not found: {}", fromAgentId);
            return 0;
        }

        int count = 0;

        // 获取所有运行中的Agent
        for (StoredCliAgent agent : cliAgentStore.findAll()) {
            if (agent.getId().equals(fromAgentId)) {
                continue; // 不发送给自己
            }

            if ("RUNNING".equals(agent.getStatus()) || "EXECUTING".equals(agent.getStatus())) {
                if (sendMessage(fromAgentId, agent.getId(), message)) {
                    count++;
                }
            }
        }

        log.info("Broadcast message from {} to {} agents", fromAgentId, count);
        return count;
    }

    @Override
    public List<AgentMessage> getPendingMessages(String agentId) {
        List<AgentMessage> messages = messageQueues.get(agentId);
        if (messages == null) {
            return Collections.emptyList();
        }

        return messages.stream()
            .filter(m -> !m.processed())
            .toList();
    }

    @Override
    public void ackMessage(String agentId, String messageId) {
        List<AgentMessage> messages = messageQueues.get(agentId);
        if (messages == null) {
            return;
        }

        for (int i = 0; i < messages.size(); i++) {
            AgentMessage msg = messages.get(i);
            if (msg.id().equals(messageId)) {
                messages.set(i, new AgentMessage(
                    msg.id(),
                    msg.fromAgentId(),
                    msg.fromAgentName(),
                    msg.toAgentId(),
                    msg.message(),
                    msg.createdAt(),
                    true
                ));
                log.debug("Message {} acknowledged by {}", messageId, agentId);
                return;
            }
        }
    }

    @Override
    public int cleanupExpiredMessages(LocalDateTime beforeTime) {
        int cleaned = 0;

        for (Map.Entry<String, List<AgentMessage>> entry : messageQueues.entrySet()) {
            List<AgentMessage> messages = entry.getValue();
            int beforeSize = messages.size();
            messages.removeIf(m -> m.createdAt().isBefore(beforeTime));
            cleaned += beforeSize - messages.size();
        }

        log.info("Cleaned {} expired messages before {}", cleaned, beforeTime);
        return cleaned;
    }
}