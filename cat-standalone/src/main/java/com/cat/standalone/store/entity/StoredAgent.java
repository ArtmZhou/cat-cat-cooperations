package com.cat.standalone.store.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 存储的Agent实体
 */
@Data
public class StoredAgent {
    private String id;
    private String name;
    private String description;
    private String type;  // BUILT_IN, EXTERNAL
    private String status;  // OFFLINE, ONLINE, BUSY, ERROR, DISABLED
    private String accessKey;
    private String config;
    private String metadata;
    private LocalDateTime lastHeartbeat;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}