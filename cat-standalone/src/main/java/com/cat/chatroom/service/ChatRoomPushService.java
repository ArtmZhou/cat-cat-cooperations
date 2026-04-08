package com.cat.chatroom.service;

import com.cat.chatroom.entity.ChatMessage;
import com.cat.cliagent.dto.CliAgentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 聊天室消息推送服务
 *
 * 负责将聊天室消息通过WebSocket推送到前端订阅者
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomPushService {

    private final SimpMessagingTemplate messagingTemplate;

    // WebSocket Topic前缀
    private static final String TOPIC_MESSAGES = "/topic/chat-room/";
    private static final String TOPIC_AGENTS = "/topic/chat-room/";
    private static final String TOPIC_TYPING = "/topic/chat-room/";
    private static final String TOPIC_SYSTEM = "/topic/chat-room/";

    /**
     * 推送聊天室消息
     */
    public void pushMessage(String roomId, ChatMessage message) {
        String destination = TOPIC_MESSAGES + roomId + "/messages";
        try {
            messagingTemplate.convertAndSend(destination, message);
            log.debug("推送消息到 {}: type={}, sender={}",
                destination, message.getType(), message.getSenderName());
        } catch (Exception e) {
            log.error("推送消息失败: roomId={}, messageId={}", roomId, message.getId(), e);
        }
    }

    /**
     * 推送Agent状态变更
     */
    public void pushAgentStatus(String roomId, String agentId, String agentName, String status) {
        String destination = TOPIC_AGENTS + roomId + "/agents";
        try {
            AgentStatusEvent event = new AgentStatusEvent(roomId, agentId, agentName, status, LocalDateTime.now());
            messagingTemplate.convertAndSend(destination, event);
            log.debug("推送Agent状态: roomId={}, agentId={}, status={}",
                roomId, agentId, status);
        } catch (Exception e) {
            log.error("推送Agent状态失败: roomId={}, agentId={}", roomId, agentId, e);
        }
    }

    /**
     * 推送Agent输入状态
     */
    public void pushTypingStatus(String roomId, String agentId, String agentName, boolean typing) {
        String destination = TOPIC_TYPING + roomId + "/typing";
        try {
            TypingEvent event = new TypingEvent(roomId, agentId, agentName, typing, LocalDateTime.now());
            messagingTemplate.convertAndSend(destination, event);
            log.debug("推送输入状态: roomId={}, agentId={}, typing={}",
                roomId, agentId, typing);
        } catch (Exception e) {
            log.error("推送输入状态失败: roomId={}, agentId={}", roomId, agentId, e);
        }
    }

    /**
     * 推送系统通知
     */
    public void pushSystemNotification(String roomId, String notification, String type) {
        String destination = TOPIC_SYSTEM + roomId + "/system";
        try {
            SystemNotificationEvent event = new SystemNotificationEvent(roomId, notification, type, LocalDateTime.now());
            messagingTemplate.convertAndSend(destination, event);
            log.debug("推送系统通知: roomId={}, type={}", roomId, type);
        } catch (Exception e) {
            log.error("推送系统通知失败: roomId={}", roomId, e);
        }
    }

    /**
     * Agent状态事件
     */
    public record AgentStatusEvent(
        String roomId,
        String agentId,
        String agentName,
        String status,  // ONLINE, OFFLINE, TYPING, ERROR
        LocalDateTime timestamp
    ) {}

    /**
     * 输入状态事件
     */
    public record TypingEvent(
        String roomId,
        String agentId,
        String agentName,
        boolean typing,
        LocalDateTime timestamp
    ) {}

    /**
     * 系统通知事件
     */
    public record SystemNotificationEvent(
        String roomId,
        String message,
        String type,  // agent_joined, agent_left, error, info
        LocalDateTime timestamp
    ) {}

    /**
     * WebSocket消息包装器
     */
    public record ChatRoomWsMessage(
        String roomId,
        String type,  // message, agent_status, typing, system
        Object payload,
        LocalDateTime timestamp
    ) {}
}
