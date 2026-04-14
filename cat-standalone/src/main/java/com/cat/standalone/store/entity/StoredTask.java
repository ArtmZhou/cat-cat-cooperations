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

    // === Git Worktree 并发协同字段 ===
    private String projectPath;     // 关联的项目仓库路径
    private String branchName;      // 任务对应的分支名称
    private String worktreePath;    // worktree工作目录路径
    private String workspaceId;     // 关联的工作空间ID
}