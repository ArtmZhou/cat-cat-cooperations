package com.cat.chatroom.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 聊天室实体
 *
 * 表示一个多Agent聊天会话的容器
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoom {

    /**
     * 聊天室最大成员数
     */
    public static final int MAX_AGENTS = 10;

    /**
     * 聊天室最大消息历史数
     */
    public static final int MAX_MESSAGES = 100;

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
     * 创建新的聊天室
     */
    public static ChatRoom create(String name, String description,
                                   String createdBy, List<String> agentIds) {
        LocalDateTime now = LocalDateTime.now();
        return ChatRoom.builder()
            .id(java.util.UUID.randomUUID().toString())
            .name(name)
            .description(description)
            .createdBy(createdBy)
            .createdAt(now)
            .updatedAt(now)
            .agentIds(agentIds != null ? new ArrayList<>(agentIds) : new ArrayList<>())
            .messages(new ArrayList<>())
            .active(true)
            .build();
    }

    /**
     * 添加Agent成员
     */
    public boolean addAgent(String agentId) {
        if (agentIds.size() >= MAX_AGENTS) {
            return false;
        }
        if (agentIds.contains(agentId)) {
            return false;
        }
        agentIds.add(agentId);
        updatedAt = LocalDateTime.now();
        return true;
    }

    /**
     * 移除Agent成员
     */
    public boolean removeAgent(String agentId) {
        boolean removed = agentIds.remove(agentId);
        if (removed) {
            updatedAt = LocalDateTime.now();
        }
        return removed;
    }

    /**
     * 添加消息
     */
    public void addMessage(ChatMessage message) {
        if (messages.size() >= MAX_MESSAGES) {
            // 移除最旧的消息
            messages.remove(0);
        }
        messages.add(message);
        updatedAt = LocalDateTime.now();
    }

    /**
     * 获取最近N条消息
     */
    public List<ChatMessage> getRecentMessages(int count) {
        if (count <= 0 || messages.isEmpty()) {
            return new ArrayList<>();
        }
        int startIndex = Math.max(0, messages.size() - count);
        return new ArrayList<>(messages.subList(startIndex, messages.size()));
    }

    /**
     * 获取消息（分页）
     */
    public List<ChatMessage> getMessagesBefore(String beforeMessageId, int limit) {
        if (beforeMessageId == null || messages.isEmpty()) {
            // 返回最新的消息
            return getRecentMessages(limit);
        }

        // 找到指定消息的索引
        int index = -1;
        for (int i = 0; i < messages.size(); i++) {
            if (messages.get(i).getId().equals(beforeMessageId)) {
                index = i;
                break;
            }
        }

        if (index <= 0) {
            return new ArrayList<>();
        }

        int startIndex = Math.max(0, index - limit);
        return new ArrayList<>(messages.subList(startIndex, index));
    }

    /**
     * 更新聊天室信息
     */
    public void update(String name, String description) {
        if (name != null && !name.isEmpty()) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查Agent是否在聊天室中
     */
    public boolean hasAgent(String agentId) {
        return agentIds.contains(agentId);
    }

    /**
     * 获取成员数量
     */
    public int getAgentCount() {
        return agentIds.size();
    }

    /**
     * 获取消息数量
     */
    public int getMessageCount() {
        return messages.size();
    }

    /**
     * 清空消息历史
     */
    public void clearMessages() {
        messages.clear();
        updatedAt = LocalDateTime.now();
    }
}
