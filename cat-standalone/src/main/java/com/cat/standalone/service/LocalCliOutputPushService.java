package com.cat.standalone.service;

import com.cat.cliagent.service.CliOutputPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * CLI输出推送服务实现
 *
 * 负责将CLI输出通过WebSocket推送到前端
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalCliOutputPushService implements CliOutputPushService {

    private final SimpMessagingTemplate messagingTemplate;

    @Lazy
    @Autowired
    private LocalChatGroupService chatGroupService;

    // WebSocket目标前缀
    private static final String TOPIC_OUTPUT = "/topic/cli/";
    private static final String TOPIC_STATUS = "/topic/cli/status/";
    private static final String TOPIC_TOKEN = "/topic/cli/token/";

    private boolean isAgentInGroupContext(String agentId) {
        return chatGroupService != null && chatGroupService.isAgentInGroupContext(agentId);
    }

    @Override
    public void pushOutput(String agentId, String line) {
        if (line == null || line.isEmpty()) {
            return;
        }

        if (isAgentInGroupContext(agentId)) {
            log.debug("Skip direct output push for agent {} because it is in group chat context", agentId);
            return;
        }

        String destination = TOPIC_OUTPUT + agentId + "/output";
        try {
            messagingTemplate.convertAndSend(destination, new OutputMessage("output", line));
            log.debug("Pushed output to {}: {}", destination, line);
        } catch (Exception e) {
            log.error("Failed to push output for agent: {}", agentId, e);
        }
    }

    @Override
    public void pushError(String agentId, String line) {
        if (line == null || line.isEmpty()) {
            return;
        }

        if (isAgentInGroupContext(agentId)) {
            log.debug("Route error to group chat only for agent {}", agentId);
            chatGroupService.handleAgentError(agentId, line);
            return;
        }

        String destination = TOPIC_OUTPUT + agentId + "/error";
        try {
            messagingTemplate.convertAndSend(destination, new OutputMessage("error", line));
            log.debug("Pushed error to {}: {}", destination, line);
        } catch (Exception e) {
            log.error("Failed to push error for agent: {}", agentId, e);
        }
    }

    @Override
    public void pushStatusChange(String agentId, String status) {
        if (isAgentInGroupContext(agentId) && !"RUNNING".equals(status)) {
            log.debug("Skip direct status push for agent {} in group chat context: {}", agentId, status);
            return;
        }

        String destination = TOPIC_STATUS + agentId;
        try {
            messagingTemplate.convertAndSend(destination, new StatusMessage(agentId, status));
            log.info("Pushed status change for agent {}: {}", agentId, status);
        } catch (Exception e) {
            log.error("Failed to push status change for agent: {}", agentId, e);
        }
    }

    @Override
    public void pushTokenUsage(String agentId, Long inputTokens, Long outputTokens) {
        String destination = TOPIC_TOKEN + agentId;
        try {
            messagingTemplate.convertAndSend(destination, new TokenMessage(inputTokens, outputTokens));
            log.debug("Pushed token usage for agent {}: input={}, output={}", agentId, inputTokens, outputTokens);
        } catch (Exception e) {
            log.error("Failed to push token usage for agent: {}", agentId, e);
        }
    }

    @Override
    public void pushTextDelta(String agentId, String text) {
        if (text == null) {
            return;
        }

        if (isAgentInGroupContext(agentId)) {
            chatGroupService.handleAgentTextDelta(agentId, text);
            return;
        }

        // 允许推送空字符串（用于触发消息更新），但null不行
        String destination = TOPIC_OUTPUT + agentId + "/output";
        try {
            messagingTemplate.convertAndSend(destination, new OutputMessage("text_delta", text));
            log.debug("Pushed text delta to {}: {}", destination, text);
        } catch (Exception e) {
            log.error("Failed to push text delta for agent: {}", agentId, e);
        }
    }

    @Override
    public void pushDone(String agentId) {
        if (isAgentInGroupContext(agentId)) {
            chatGroupService.handleAgentDone(agentId);
            // 群聊上下文已在handleAgentDone中清理，这里只向agent状态topic同步最终空闲状态
            try {
                messagingTemplate.convertAndSend(TOPIC_STATUS + agentId, new StatusMessage(agentId, "RUNNING"));
            } catch (Exception e) {
                log.error("Failed to push final running status for grouped agent: {}", agentId, e);
            }
            return;
        }

        String destination = TOPIC_OUTPUT + agentId + "/output";
        try {
            messagingTemplate.convertAndSend(destination, new OutputMessage("done", ""));
            log.debug("Pushed done signal for agent: {}", agentId);
        } catch (Exception e) {
            log.error("Failed to push done for agent: {}", agentId, e);
        }
    }

    // ===== 群聊输出推送 =====

    private static final String TOPIC_GROUP = "/topic/chat-group/";

    /**
     * 推送群聊消息
     */
    public void pushGroupMessage(String groupId, Object message) {
        String destination = TOPIC_GROUP + groupId + "/message";
        try {
            messagingTemplate.convertAndSend(destination, message);
            log.debug("Pushed group message to {}", destination);
        } catch (Exception e) {
            log.error("Failed to push group message to {}: {}", destination, e.getMessage());
        }
    }

    /**
     * 推送agent在群聊中的流式输出
     */
    public void pushGroupAgentOutput(String groupId, String agentId, String agentName, String type, String content) {
        String destination = TOPIC_GROUP + groupId + "/agent-output";
        try {
            messagingTemplate.convertAndSend(destination,
                new GroupAgentOutput(agentId, agentName, type, content));
            log.debug("Pushed group agent output to {}: agent={}, type={}", destination, agentId, type);
        } catch (Exception e) {
            log.error("Failed to push group agent output to {}: {}", destination, e.getMessage());
        }
    }

    // 输出消息DTO
    public record OutputMessage(String type, String content) {}

    // 状态消息DTO
    public record StatusMessage(String agentId, String status) {}

    // Token消息DTO
    public record TokenMessage(Long inputTokens, Long outputTokens) {}

    // 群聊Agent输出DTO
    public record GroupAgentOutput(String agentId, String agentName, String type, String content) {}
}
