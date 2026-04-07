package com.cat.standalone.store.entity;

import com.cat.chatroom.entity.ChatMessage;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天室存储实体
 *
 * 用于JSON文件存储的聊天室实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoredChatRoom {

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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    /**
     * 成员Agent ID列表
     */
    @Builder.Default
    private List<String> agentIds = new ArrayList<>();

    /**
     * 消息历史（最近100条）
     */
    @Builder.Default
    private List<ChatMessage> messages = new ArrayList<>();

    /**
     * 是否活跃
     */
    @Builder.Default
    private boolean active = true;

    /**
     * 最大Agent数量
     */
    @Builder.Default
    private int maxAgents = 10;

    /**
     * 最大消息历史数
     */
    @Builder.Default
    private int maxMessages = 100;
}
