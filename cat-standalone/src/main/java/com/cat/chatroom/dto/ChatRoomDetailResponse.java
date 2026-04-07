package com.cat.chatroom.dto;

import com.cat.chatroom.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 聊天室详情响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomDetailResponse {

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
    private String createdAt;

    /**
     * 更新时间
     */
    private String updatedAt;

    /**
     * 成员Agent ID列表
     */
    private List<String> agentIds;

    /**
     * 消息历史（最近N条）
     */
    private List<ChatMessage> messages;

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
     * 是否有更多消息
     */
    private boolean hasMoreMessages;
}
