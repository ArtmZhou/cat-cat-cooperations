package com.cat.chatroom.dto;

import com.cat.chatroom.entity.ChatMessage;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 聊天室响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponse {

    /**
     * 聊天室ID
     */
    private String id;

    /**
     * 聊天室名称
     */
    private String name;

    /**
     * 聊天室描述
     */
    private String description;

    /**
     * 创建者ID
     */
    private String createdBy;

    /**
     * 创建时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 成员Agent ID列表
     */
    private List<String> agentIds;

    /**
     * 成员Agent数量
     */
    private int agentCount;

    /**
     * 消息数量
     */
    private int messageCount;

    /**
     * 是否活跃
     */
    private boolean active;

    /**
     * 不包含消息历史的简化响应
     */
    public static ChatRoomResponse summary(String id, String name, String description,
                                            String createdBy, LocalDateTime createdAt,
                                            LocalDateTime updatedAt, List<String> agentIds,
                                            boolean active) {
        return ChatRoomResponse.builder()
            .id(id)
            .name(name)
            .description(description)
            .createdBy(createdBy)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .agentIds(agentIds)
            .agentCount(agentIds != null ? agentIds.size() : 0)
            .messageCount(0)
            .active(active)
            .build();
    }
}
