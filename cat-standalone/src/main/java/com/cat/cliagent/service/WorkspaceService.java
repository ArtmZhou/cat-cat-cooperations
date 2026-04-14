package com.cat.cliagent.service;

import java.util.List;

/**
 * Git Worktree 工作空间管理服务接口
 *
 * 基于 Git Worktree 实现多Agent并发开发的工作空间隔离。
 * 每个任务分配独立的 worktree + 分支，确保并发操作不会产生冲突。
 *
 * 核心流程：
 * 1. 创建工作空间 → git worktree add + git checkout -b
 * 2. Agent在独立worktree中执行任务
 * 3. 任务完成后 → git commit + push + 创建PR/合并
 * 4. 清理工作空间 → git worktree remove
 */
public interface WorkspaceService {

    /**
     * 创建工作空间（worktree + 分支）
     *
     * @param request 创建请求
     * @return 工作空间信息
     */
    WorkspaceInfo createWorkspace(CreateWorkspaceRequest request);

    /**
     * 获取工作空间信息
     *
     * @param workspaceId 工作空间ID
     * @return 工作空间信息
     */
    WorkspaceInfo getWorkspace(String workspaceId);

    /**
     * 列出所有工作空间
     *
     * @return 工作空间列表
     */
    List<WorkspaceInfo> listWorkspaces();

    /**
     * 列出指定项目的工作空间
     *
     * @param projectPath 项目路径（主仓库路径）
     * @return 工作空间列表
     */
    List<WorkspaceInfo> listWorkspacesByProject(String projectPath);

    /**
     * 删除工作空间（移除worktree + 删除分支）
     *
     * @param workspaceId 工作空间ID
     * @param force 是否强制删除（忽略未提交的变更）
     */
    void removeWorkspace(String workspaceId, boolean force);

    /**
     * 获取工作空间的Git状态
     *
     * @param workspaceId 工作空间ID
     * @return Git状态信息
     */
    WorkspaceGitStatus getWorkspaceGitStatus(String workspaceId);

    /**
     * 在工作空间中提交变更
     *
     * @param workspaceId 工作空间ID
     * @param commitMessage 提交信息
     * @return 提交结果
     */
    CommitResult commitChanges(String workspaceId, String commitMessage);

    /**
     * 将工作空间的分支推送到远端
     *
     * @param workspaceId 工作空间ID
     * @return 推送结果
     */
    PushResult pushBranch(String workspaceId);

    /**
     * 合并工作空间分支到目标分支
     *
     * @param workspaceId 工作空间ID
     * @param targetBranch 目标分支（如 main）
     * @return 合并结果
     */
    MergeResult mergeBranch(String workspaceId, String targetBranch);

    /**
     * 检测合并冲突（预检）
     *
     * @param workspaceId 工作空间ID
     * @param targetBranch 目标分支
     * @return 冲突检测结果
     */
    ConflictCheckResult checkConflicts(String workspaceId, String targetBranch);

    /**
     * 同步主分支变更到工作空间（rebase）
     *
     * @param workspaceId 工作空间ID
     * @param sourceBranch 源分支（如 main）
     * @return 同步结果
     */
    SyncResult syncFromBranch(String workspaceId, String sourceBranch);

    // ===== Record DTOs =====

    /**
     * 创建工作空间请求
     */
    record CreateWorkspaceRequest(
        String projectPath,     // 主仓库路径
        String branchName,      // 新分支名称（可选，自动生成）
        String baseBranch,      // 基线分支（默认 main）
        String taskId,          // 关联的任务ID（可选）
        String agentId,         // 关联的Agent ID（可选）
        String description      // 工作空间描述
    ) {}

    /**
     * 工作空间信息
     */
    record WorkspaceInfo(
        String id,
        String projectPath,     // 主仓库路径
        String worktreePath,    // worktree工作目录路径
        String branchName,      // 分支名称
        String baseBranch,      // 基线分支
        String status,          // ACTIVE, COMMITTED, MERGED, REMOVED, ERROR
        String taskId,          // 关联任务ID
        String agentId,         // 关联Agent ID
        String description,
        String lastCommitHash,
        String lastCommitMessage,
        String createdAt,
        String updatedAt
    ) {}

    /**
     * Git状态信息
     */
    record WorkspaceGitStatus(
        String workspaceId,
        String branchName,
        String currentCommit,
        int modifiedFiles,
        int stagedFiles,
        int untrackedFiles,
        boolean hasUncommittedChanges,
        int aheadCount,         // 领先baseBranch的提交数
        int behindCount,        // 落后baseBranch的提交数
        List<String> modifiedFileList,
        List<String> untrackedFileList
    ) {}

    /**
     * 提交结果
     */
    record CommitResult(
        boolean success,
        String commitHash,
        String message,
        String error
    ) {}

    /**
     * 推送结果
     */
    record PushResult(
        boolean success,
        String branchName,
        String remoteName,
        String error
    ) {}

    /**
     * 合并结果
     */
    record MergeResult(
        boolean success,
        String mergeCommitHash,
        String sourceBranch,
        String targetBranch,
        boolean hasConflicts,
        List<String> conflictFiles,
        String error
    ) {}

    /**
     * 冲突检测结果
     */
    record ConflictCheckResult(
        boolean hasConflicts,
        List<String> conflictFiles,
        int totalChangedFiles,
        String error
    ) {}

    /**
     * 同步结果
     */
    record SyncResult(
        boolean success,
        String strategy,        // rebase or merge
        boolean hasConflicts,
        List<String> conflictFiles,
        String error
    ) {}
}
