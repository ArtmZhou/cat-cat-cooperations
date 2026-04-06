package com.cat.task.dto;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Task日志响应
 */
@Data
public class TaskLogResponse {
    private String id;
    private String taskId;
    private String agentId;
    private String level;
    private String message;
    private String detail;
    private LocalDateTime createdAt;
}