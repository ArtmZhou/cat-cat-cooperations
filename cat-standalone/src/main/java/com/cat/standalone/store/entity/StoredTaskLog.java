package com.cat.standalone.store.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 存储的任务日志实体
 */
@Data
public class StoredTaskLog {
    private String id;
    private String taskId;
    private String agentId;
    private String level;  // INFO, WARN, ERROR, DEBUG
    private String message;
    private String detail;
    private LocalDateTime createdAt;
}