package com.cat.task.dto;

import com.cat.task.entity.TaskAssignment;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Task响应DTO
 */
@Data
public class TaskResponse {
    private String id;
    private String name;
    private String description;
    private String type;
    private String status;
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
    private List<TaskAssignment> assignments;
}