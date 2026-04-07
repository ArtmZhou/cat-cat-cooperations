package com.cat.chatroom.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天室消息实体
 *
 * 表示聊天室中的一条消息，可以是用户发送、Agent回复或系统通知
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {

    /**
     * 消息ID
     */
    private String id;

    /**
     * 消息类型
     * user - 用户发送的消息
     * agent - Agent回复的消息
     * system - 系统通知消息
     */
    private String type;

    /**
     * 发送者ID（用户ID或Agent ID）
     */
    private String senderId;

    /**
     * 发送者显示名称
     */
    private String senderName;

    /**
     * 发送者头像（emoji或URL）
     */
    @Builder.Default
    private String senderAvatar = "🤖";

    /**
     * 消息内容
     */
    private String content;

    /**
     * @提及的目标Agent ID列表
     */
    private List<String> targetAgentIds;

    /**
     * 回复的消息ID（如果是回复消息）
     */
    private String replyTo;

    /**
     * 消息时间戳
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 元数据（token使用量、处理时间等）
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * 创建用户消息
     */
    public static ChatMessage createUserMessage(String senderId, String senderName,
                                                 String content, List<String> targetAgentIds) {
        return ChatMessage.builder()
            .id(java.util.UUID.randomUUID().toString())
            .type("user")
            .senderId(senderId)
            .senderName(senderName)
            .senderAvatar("👤")
            .content(content)
            .targetAgentIds(targetAgentIds)
            .timestamp(LocalDateTime.now())
            .metadata(new HashMap<>())
            .build();
    }

    /**
     * 创建Agent消息
     */
    public static ChatMessage createAgentMessage(String agentId, String agentName,
                                                  String content, Map<String, Object> metadata) {
        return ChatMessage.builder()
            .id(java.util.UUID.randomUUID().toString())
            .type("agent")
            .senderId(agentId)
            .senderName(agentName)
            .senderAvatar("🤖")
            .content(content)
            .timestamp(LocalDateTime.now())
            .metadata(metadata != null ? metadata : new HashMap<>())
            .build();
    }

    /**
     * 创建系统消息
     */
    public static ChatMessage createSystemMessage(String content) {
        return ChatMessage.builder()
            .id(java.util.UUID.randomUUID().toString())
            .type("system")
            .senderId("system")
            .senderName("System")
            .senderAvatar("🔔")
            .content(content)
            .timestamp(LocalDateTime.now())
            .metadata(new HashMap<>())
            .build();
    }

    /**
     * 添加元数据
     */
    public void addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new HashMap<>();
        }
        this.metadata.put(key, value);
    }

    /**
     * 获取token使用量
     */
    public Long getInputTokens() {
        if (metadata != null && metadata.containsKey("inputTokens")) {
            return Long.valueOf(metadata.get("inputTokens").toString());
        }
        return 0L;
    }

    public Long getOutputTokens() {
        if (metadata != null && metadata.containsKey("outputTokens")) {
            return Long.valueOf(metadata.get("outputTokens").toString());
        }
        return 0L;
    }
}
