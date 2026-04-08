package com.cat.chatroom.dto;

import com.cat.chatroom.entity.ChatMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 聊天室消息历史响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageHistoryResponse {

    /**
     * 消息列表
     */
    private List<ChatMessage> messages;

    /**
     * 是否有更多消息
     */
    private boolean hasMore;

    /**
     * 总消息数
     */
    private int totalCount;
}
