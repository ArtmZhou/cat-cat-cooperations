package com.cat.standalone.store.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 存储的Git Worktree工作空间实体
 *
 * 记录每个worktree工作空间的状态信息，
 * 用于多Agent并发开发时的工作空间隔离管理。
 */
@Data
public class StoredWorkspace {
    private String id;
    private String projectPath;     // 主仓库路径
    private String worktreePath;    // worktree工作目录路径
    private String branchName;      // 分支名称
    private String baseBranch;      // 基线分支（如 main）
    private String status;          // ACTIVE, COMMITTED, MERGED, REMOVED, ERROR
    private String taskId;          // 关联的任务ID
    private String agentId;         // 关联的Agent ID
    private String description;     // 工作空间描述
    private String lastCommitHash;  // 最后一次提交的hash
    private String lastCommitMessage; // 最后一次提交的信息
    private String errorMessage;    // 错误信息
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
