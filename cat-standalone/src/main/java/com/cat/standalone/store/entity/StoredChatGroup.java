package com.cat.standalone.store.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 存储的聊天群组实体
 *
 * 支持多Agent群聊，类似聊天群
 */
@Data
public class StoredChatGroup {
    private String id;
    private String name;
    private String description;
    private List<String> agentIds;      // 群成员Agent ID列表
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
