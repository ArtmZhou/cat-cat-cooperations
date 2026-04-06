package com.cat.standalone.store.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 存储的任务分配实体
 */
@Data
public class StoredTaskAssignment {
    private String id;
    private String taskId;
    private String agentId;
    private String role;
    private String status;  // ASSIGNED, RUNNING, COMPLETED, FAILED
    private LocalDateTime assignedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String result;
    private String errorMessage;
}