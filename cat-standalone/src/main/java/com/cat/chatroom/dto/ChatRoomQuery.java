package com.cat.chatroom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 聊天室查询DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomQuery {

    /**
     * 关键词（搜索名称和描述）
     */
    private String keyword;

    /**
     * 是否只显示活跃
     */
    private Boolean active;

    /**
     * 页码（从1开始）
     */
    @Builder.Default
    private int page = 1;

    /**
     * 每页数量
     */
    @Builder.Default
    private int pageSize = 20;

    /**
     * 获取有效的页码
     */
    public int getPage() {
        return Math.max(1, page);
    }

    /**
     * 获取有效的每页数量
     */
    public int getPageSize() {
        return Math.min(Math.max(1, pageSize), 100);
    }

    /**
     * 获取偏移量
     */
    public int getOffset() {
        return (getPage() - 1) * getPageSize();
    }
}
