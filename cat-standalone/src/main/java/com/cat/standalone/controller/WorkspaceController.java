package com.cat.standalone.controller;

import com.cat.cliagent.service.WorkspaceService;
import com.cat.cliagent.service.WorkspaceService.*;
import com.cat.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Git Worktree 工作空间管理控制器
 *
 * 提供多Agent并发协同开发的工作空间生命周期管理API。
 * 支持创建、查询、删除工作空间，以及Git操作（提交、推送、合并、冲突检测等）。
 */
@Tag(name = "工作空间管理", description = "基于Git Worktree的多Agent并发开发工作空间管理")
@RestController
@RequestMapping("/api/v1/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    // ===== 工作空间生命周期管理 =====

    @Operation(summary = "创建工作空间", description = "基于Git Worktree创建独立的工作空间（worktree + 分支）")
    @PostMapping
    public ApiResponse<WorkspaceInfo> createWorkspace(@RequestBody CreateWorkspaceRequest request) {
        WorkspaceInfo workspace = workspaceService.createWorkspace(request);
        return ApiResponse.success(workspace);
    }

    @Operation(summary = "获取工作空间详情", description = "根据ID获取工作空间信息")
    @GetMapping("/{workspaceId}")
    public ApiResponse<WorkspaceInfo> getWorkspace(@PathVariable String workspaceId) {
        WorkspaceInfo workspace = workspaceService.getWorkspace(workspaceId);
        return ApiResponse.success(workspace);
    }

    @Operation(summary = "列出所有工作空间", description = "获取所有已创建的工作空间列表")
    @GetMapping
    public ApiResponse<List<WorkspaceInfo>> listWorkspaces(
            @RequestParam(required = false) String projectPath) {
        List<WorkspaceInfo> workspaces;
        if (projectPath != null && !projectPath.isBlank()) {
            workspaces = workspaceService.listWorkspacesByProject(projectPath);
        } else {
            workspaces = workspaceService.listWorkspaces();
        }
        return ApiResponse.success(workspaces);
    }

    @Operation(summary = "删除工作空间", description = "移除Worktree和对应的分支")
    @DeleteMapping("/{workspaceId}")
    public ApiResponse<Void> removeWorkspace(
            @PathVariable String workspaceId,
            @RequestParam(defaultValue = "false") boolean force) {
        workspaceService.removeWorkspace(workspaceId, force);
        return ApiResponse.success();
    }

    // ===== Git操作 =====

    @Operation(summary = "获取Git状态", description = "获取工作空间的Git状态（修改文件、暂存区、ahead/behind等）")
    @GetMapping("/{workspaceId}/git-status")
    public ApiResponse<WorkspaceGitStatus> getGitStatus(@PathVariable String workspaceId) {
        WorkspaceGitStatus status = workspaceService.getWorkspaceGitStatus(workspaceId);
        return ApiResponse.success(status);
    }

    @Operation(summary = "提交变更", description = "在工作空间中执行 git add + git commit")
    @PostMapping("/{workspaceId}/commit")
    public ApiResponse<CommitResult> commitChanges(
            @PathVariable String workspaceId,
            @RequestBody CommitRequest request) {
        CommitResult result = workspaceService.commitChanges(workspaceId, request.message());
        return ApiResponse.success(result);
    }

    @Operation(summary = "推送分支", description = "将工作空间分支推送到远端仓库")
    @PostMapping("/{workspaceId}/push")
    public ApiResponse<PushResult> pushBranch(@PathVariable String workspaceId) {
        PushResult result = workspaceService.pushBranch(workspaceId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "合并分支", description = "将工作空间分支合并到目标分支（如main）")
    @PostMapping("/{workspaceId}/merge")
    public ApiResponse<MergeResult> mergeBranch(
            @PathVariable String workspaceId,
            @RequestBody MergeRequest request) {
        MergeResult result = workspaceService.mergeBranch(workspaceId, request.targetBranch());
        return ApiResponse.success(result);
    }

    @Operation(summary = "检测合并冲突", description = "预检工作空间分支与目标分支是否存在合并冲突")
    @PostMapping("/{workspaceId}/check-conflicts")
    public ApiResponse<ConflictCheckResult> checkConflicts(
            @PathVariable String workspaceId,
            @RequestBody MergeRequest request) {
        ConflictCheckResult result = workspaceService.checkConflicts(workspaceId, request.targetBranch());
        return ApiResponse.success(result);
    }

    @Operation(summary = "同步主分支", description = "将主分支的最新变更同步到工作空间（rebase）")
    @PostMapping("/{workspaceId}/sync")
    public ApiResponse<SyncResult> syncFromBranch(
            @PathVariable String workspaceId,
            @RequestBody SyncRequest request) {
        SyncResult result = workspaceService.syncFromBranch(workspaceId, request.sourceBranch());
        return ApiResponse.success(result);
    }

    // ===== Request DTOs =====

    public record CommitRequest(String message) {}
    public record MergeRequest(String targetBranch) {}
    public record SyncRequest(String sourceBranch) {}
}
