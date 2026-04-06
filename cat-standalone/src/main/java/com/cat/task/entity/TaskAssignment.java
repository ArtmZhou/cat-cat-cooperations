package com.cat.task.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 任务分配实体
 */
@Data
public class TaskAssignment {
    private String id;
    private String taskId;
    private String agentId;
    private String role;
    private String status;
    private LocalDateTime assignedAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String result;
    private String errorMessage;
}