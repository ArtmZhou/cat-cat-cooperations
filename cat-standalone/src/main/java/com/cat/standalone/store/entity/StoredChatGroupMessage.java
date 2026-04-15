package com.cat.standalone.store.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 存储的群聊消息实体
 */
@Data
public class StoredChatGroupMessage {
    private String id;
    private String groupId;
    private String senderType;        // "user" 或 "agent"
    private String senderAgentId;     // 如果是agent发送的，记录agent ID
    private String senderName;        // 发送者名称
    private String content;           // 消息内容
    private List<String> mentionedAgentIds;  // @提到的Agent ID列表
    private boolean broadcast;        // 是否是广播消息（发给群内所有agent）
    private LocalDateTime createdAt;
}
