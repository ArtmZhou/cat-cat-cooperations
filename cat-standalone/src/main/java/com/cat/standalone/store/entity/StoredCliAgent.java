package com.cat.standalone.store.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 存储的CLI Agent实例实体
 */
@Data
public class StoredCliAgent {
    private String id;
    private String name;
    private String description;
    private String templateId;
    private String templateName;  // 冗余存储，便于显示
    private String cliType;       // 冗余存储，便于查询
    private String status;        // STOPPED, STARTING, RUNNING, EXECUTING, ERROR
    private String executablePath;
    private String args;          // JSON数组
    private String envVars;       // JSON对象（敏感信息加密存储）
    private String configPath;
    private String workingDir;
    private String processId;     // 系统进程ID
    private String sessionId;     // CLI会话ID，用于恢复对话上下文
    private LocalDateTime lastStartedAt;
    private LocalDateTime lastStoppedAt;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}