package com.cat.chatroom.dto;

import com.cat.chatroom.entity.ChatMessage;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 聊天消息响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    /**
     * 消息ID
     */
    private String id;

    /**
     * 消息类型
     */
    private String type;

    /**
     * 发送者ID
     */
    private String senderId;

    /**
     * 发送者名称
     */
    private String senderName;

    /**
     * 发送者头像
     */
    private String senderAvatar;

    /**
     * 消息内容
     */
    private String content;

    /**
     * @提及的目标Agent ID列表
     */
    private List<String> targetAgentIds;

    /**
     * 回复的消息ID
     */
    private String replyTo;

    /**
     * 消息时间戳
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * 元数据
     */
    private Map<String, Object> metadata;

    /**
     * 从ChatMessage实体转换
     */
    public static ChatMessageResponse from(ChatMessage message) {
        if (message == null) {
            return null;
        }
        return ChatMessageResponse.builder()
            .id(message.getId())
            .type(message.getType())
            .senderId(message.getSenderId())
            .senderName(message.getSenderName())
            .senderAvatar(message.getSenderAvatar())
            .content(message.getContent())
            .targetAgentIds(message.getTargetAgentIds())
            .replyTo(message.getReplyTo())
            .timestamp(message.getTimestamp())
            .metadata(message.getMetadata())
            .build();
    }
}
