package com.cat.standalone.store.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 存储的Task实体
 */
@Data
public class StoredTask {
    private String id;
    private String name;
    private String description;
    private String type;  // SIMPLE, WORKFLOW, PARALLEL, NEGOTIATION
    private String status;  // PENDING, ASSIGNED, RUNNING, COMPLETED, FAILED, CANCELLED
    private Integer priority;
    private String input;
    private String output;
    private String config;
    private Integer timeoutSeconds;
    private Integer retryCount;
    private Integer maxRetry;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}