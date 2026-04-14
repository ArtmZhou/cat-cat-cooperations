package com.cat.standalone.service;

import com.cat.cliagent.service.WorkspaceService;
import com.cat.common.exception.BusinessException;
import com.cat.standalone.store.JsonFileStore;
import com.cat.standalone.store.entity.StoredWorkspace;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Git Worktree 工作空间管理服务实现
 *
 * 通过调用 git CLI 命令管理 worktree 的生命周期。
 * 每个工作空间对应一个独立的 worktree + 分支，
 * 确保多Agent并发开发时互不干扰。
 *
 * 目录结构示例：
 *   /projects/my-repo/                    ← 主仓库 (main)
 *   /projects/my-repo/.worktrees/         ← worktree存放目录
 *     ws-{id}-{branchName}/               ← Agent-1的工作目录
 *     ws-{id}-{branchName}/               ← Agent-2的工作目录
 */
@Slf4j
@Service
public class LocalWorkspaceService implements WorkspaceService {

    private final JsonFileStore<StoredWorkspace> workspaceStore;

    @Value("${cat.workspace.worktree-dir-name:.worktrees}")
    private String worktreeDirName;

    @Value("${cat.workspace.default-base-branch:main}")
    private String defaultBaseBranch;

    public LocalWorkspaceService(JsonFileStore<StoredWorkspace> workspaceStore) {
        this.workspaceStore = workspaceStore;
    }

    @Override
    public WorkspaceInfo createWorkspace(CreateWorkspaceRequest request) {
        // 验证主仓库路径
        File projectDir = new File(request.projectPath());
        if (!projectDir.exists() || !projectDir.isDirectory()) {
            throw new BusinessException(400, "项目路径不存在: " + request.projectPath());
        }

        // 验证是Git仓库
        File gitDir = new File(projectDir, ".git");
        if (!gitDir.exists()) {
            throw new BusinessException(400, "项目路径不是Git仓库: " + request.projectPath());
        }

        // 生成工作空间ID
        String workspaceId = UUID.randomUUID().toString().substring(0, 8);

        // 确定分支名称
        String baseBranch = request.baseBranch() != null ? request.baseBranch() : defaultBaseBranch;
        String branchName = request.branchName();
        if (branchName == null || branchName.isBlank()) {
            // 自动生成分支名
            String prefix = "workspace";
            if (request.taskId() != null) {
                prefix = "task/" + request.taskId().substring(0, Math.min(8, request.taskId().length()));
            }
            if (request.agentId() != null) {
                prefix += "/agent-" + request.agentId().substring(0, Math.min(8, request.agentId().length()));
            }
            branchName = prefix + "/" + workspaceId;
        }

        // 确定worktree目录
        String sanitizedBranch = branchName.replaceAll("[^a-zA-Z0-9\\-_]", "-");
        String worktreeDirPath = new File(request.projectPath(), worktreeDirName).getAbsolutePath();
        String worktreePath = new File(worktreeDirPath, "ws-" + workspaceId + "-" + sanitizedBranch).getAbsolutePath();

        // 确保worktree父目录存在
        File worktreeParent = new File(worktreeDirPath);
        if (!worktreeParent.exists()) {
            worktreeParent.mkdirs();
        }

        // 检查分支是否已存在
        boolean branchExists = checkBranchExists(request.projectPath(), branchName);
        if (branchExists) {
            throw new BusinessException(400, "分支已存在: " + branchName);
        }

        try {
            // 创建worktree + 新分支
            // git worktree add -b <branch> <path> <base-branch>
            GitCommandResult result = executeGitCommand(
                request.projectPath(),
                "worktree", "add", "-b", branchName, worktreePath, baseBranch
            );

            if (!result.success()) {
                throw new BusinessException(500, "创建Worktree失败: " + result.error());
            }

            log.info("Created worktree: {} on branch {} (base: {})", worktreePath, branchName, baseBranch);

            // 保存到存储
            LocalDateTime now = LocalDateTime.now();
            StoredWorkspace workspace = new StoredWorkspace();
            workspace.setId(workspaceId);
            workspace.setProjectPath(request.projectPath());
            workspace.setWorktreePath(worktreePath);
            workspace.setBranchName(branchName);
            workspace.setBaseBranch(baseBranch);
            workspace.setStatus("ACTIVE");
            workspace.setTaskId(request.taskId());
            workspace.setAgentId(request.agentId());
            workspace.setDescription(request.description());
            workspace.setCreatedAt(now);
            workspace.setUpdatedAt(now);

            workspaceStore.save(workspaceId, workspace);

            return toWorkspaceInfo(workspace);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create workspace", e);
            throw new BusinessException(500, "创建工作空间失败: " + e.getMessage());
        }
    }

    @Override
    public WorkspaceInfo getWorkspace(String workspaceId) {
        StoredWorkspace workspace = workspaceStore.findById(workspaceId)
            .orElseThrow(() -> new BusinessException(404, "工作空间不存在: " + workspaceId));
        return toWorkspaceInfo(workspace);
    }

    @Override
    public List<WorkspaceInfo> listWorkspaces() {
        return workspaceStore.findAll().stream()
            .map(this::toWorkspaceInfo)
            .collect(Collectors.toList());
    }

    @Override
    public List<WorkspaceInfo> listWorkspacesByProject(String projectPath) {
        return workspaceStore.find(ws -> ws.getProjectPath().equals(projectPath)).stream()
            .map(this::toWorkspaceInfo)
            .collect(Collectors.toList());
    }

    @Override
    public void removeWorkspace(String workspaceId, boolean force) {
        StoredWorkspace workspace = workspaceStore.findById(workspaceId)
            .orElseThrow(() -> new BusinessException(404, "工作空间不存在: " + workspaceId));

        if ("REMOVED".equals(workspace.getStatus())) {
            throw new BusinessException(400, "工作空间已被删除");
        }

        try {
            // 移除worktree
            List<String> args = new ArrayList<>();
            args.add("worktree");
            args.add("remove");
            if (force) {
                args.add("--force");
            }
            args.add(workspace.getWorktreePath());

            GitCommandResult result = executeGitCommand(
                workspace.getProjectPath(),
                args.toArray(new String[0])
            );

            if (!result.success()) {
                // 如果worktree目录已不存在，直接更新状态
                File worktreeDir = new File(workspace.getWorktreePath());
                if (!worktreeDir.exists()) {
                    log.warn("Worktree directory already removed: {}", workspace.getWorktreePath());
                } else {
                    throw new BusinessException(500, "移除Worktree失败: " + result.error());
                }
            }

            // 删除分支（仅本地分支）
            executeGitCommand(
                workspace.getProjectPath(),
                "branch", "-D", workspace.getBranchName()
            );

            log.info("Removed worktree and branch: {} / {}", workspace.getWorktreePath(), workspace.getBranchName());

            // 更新状态
            workspace.setStatus("REMOVED");
            workspace.setUpdatedAt(LocalDateTime.now());
            workspaceStore.save(workspaceId, workspace);

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to remove workspace: {}", workspaceId, e);
            workspace.setStatus("ERROR");
            workspace.setErrorMessage(e.getMessage());
            workspace.setUpdatedAt(LocalDateTime.now());
            workspaceStore.save(workspaceId, workspace);
            throw new BusinessException(500, "删除工作空间失败: " + e.getMessage());
        }
    }

    @Override
    public WorkspaceGitStatus getWorkspaceGitStatus(String workspaceId) {
        StoredWorkspace workspace = workspaceStore.findById(workspaceId)
            .orElseThrow(() -> new BusinessException(404, "工作空间不存在: " + workspaceId));

        if (!"ACTIVE".equals(workspace.getStatus()) && !"COMMITTED".equals(workspace.getStatus())) {
            throw new BusinessException(400, "工作空间不处于活跃状态");
        }

        String worktreePath = workspace.getWorktreePath();

        // 获取当前分支
        String branchName = getGitOutput(worktreePath, "rev-parse", "--abbrev-ref", "HEAD").trim();

        // 获取当前commit
        String currentCommit = getGitOutput(worktreePath, "rev-parse", "HEAD").trim();

        // 获取修改的文件
        String statusOutput = getGitOutput(worktreePath, "status", "--porcelain");
        List<String> statusLines = statusOutput.lines()
            .filter(line -> !line.isBlank())
            .collect(Collectors.toList());

        List<String> modifiedFiles = new ArrayList<>();
        List<String> untrackedFiles = new ArrayList<>();
        int stagedCount = 0;

        for (String line : statusLines) {
            if (line.length() < 3) continue;
            char index = line.charAt(0);
            char workTree = line.charAt(1);
            String fileName = line.substring(3).trim();

            if (index == '?' && workTree == '?') {
                untrackedFiles.add(fileName);
            } else {
                modifiedFiles.add(fileName);
                if (index != ' ' && index != '?') {
                    stagedCount++;
                }
            }
        }

        // 获取ahead/behind计数
        int aheadCount = 0;
        int behindCount = 0;
        try {
            String countOutput = getGitOutput(worktreePath,
                "rev-list", "--left-right", "--count",
                workspace.getBranchName() + "..." + workspace.getBaseBranch());
            String[] counts = countOutput.trim().split("\\s+");
            if (counts.length == 2) {
                aheadCount = Integer.parseInt(counts[0]);
                behindCount = Integer.parseInt(counts[1]);
            }
        } catch (Exception e) {
            log.debug("Cannot get ahead/behind count: {}", e.getMessage());
        }

        return new WorkspaceGitStatus(
            workspaceId,
            branchName,
            currentCommit,
            modifiedFiles.size(),
            stagedCount,
            untrackedFiles.size(),
            !modifiedFiles.isEmpty() || !untrackedFiles.isEmpty(),
            aheadCount,
            behindCount,
            modifiedFiles,
            untrackedFiles
        );
    }

    @Override
    public CommitResult commitChanges(String workspaceId, String commitMessage) {
        StoredWorkspace workspace = workspaceStore.findById(workspaceId)
            .orElseThrow(() -> new BusinessException(404, "工作空间不存在: " + workspaceId));

        if (!"ACTIVE".equals(workspace.getStatus()) && !"COMMITTED".equals(workspace.getStatus())) {
            throw new BusinessException(400, "工作空间不处于活跃状态");
        }

        String worktreePath = workspace.getWorktreePath();

        try {
            // git add -A
            GitCommandResult addResult = executeGitCommand(worktreePath, "add", "-A");
            if (!addResult.success()) {
                return new CommitResult(false, null, null, "git add failed: " + addResult.error());
            }

            // git commit -m "message"
            GitCommandResult commitResult = executeGitCommand(worktreePath, "commit", "-m", commitMessage);
            if (!commitResult.success()) {
                // 可能没有变更需要提交
                if (commitResult.error().contains("nothing to commit")) {
                    return new CommitResult(true, null, "Nothing to commit", null);
                }
                return new CommitResult(false, null, null, "git commit failed: " + commitResult.error());
            }

            // 获取提交hash
            String commitHash = getGitOutput(worktreePath, "rev-parse", "HEAD").trim();

            // 更新工作空间状态
            workspace.setStatus("COMMITTED");
            workspace.setLastCommitHash(commitHash);
            workspace.setLastCommitMessage(commitMessage);
            workspace.setUpdatedAt(LocalDateTime.now());
            workspaceStore.save(workspaceId, workspace);

            log.info("Committed changes in workspace {}: {} - {}", workspaceId, commitHash, commitMessage);

            return new CommitResult(true, commitHash, commitMessage, null);

        } catch (Exception e) {
            log.error("Failed to commit in workspace: {}", workspaceId, e);
            return new CommitResult(false, null, null, e.getMessage());
        }
    }

    @Override
    public PushResult pushBranch(String workspaceId) {
        StoredWorkspace workspace = workspaceStore.findById(workspaceId)
            .orElseThrow(() -> new BusinessException(404, "工作空间不存在: " + workspaceId));

        String worktreePath = workspace.getWorktreePath();

        try {
            // git push origin <branch>
            GitCommandResult result = executeGitCommand(
                worktreePath,
                "push", "-u", "origin", workspace.getBranchName()
            );

            if (!result.success()) {
                return new PushResult(false, workspace.getBranchName(), "origin", "git push failed: " + result.error());
            }

            log.info("Pushed branch {} to origin", workspace.getBranchName());

            return new PushResult(true, workspace.getBranchName(), "origin", null);

        } catch (Exception e) {
            log.error("Failed to push branch for workspace: {}", workspaceId, e);
            return new PushResult(false, workspace.getBranchName(), "origin", e.getMessage());
        }
    }

    @Override
    public MergeResult mergeBranch(String workspaceId, String targetBranch) {
        StoredWorkspace workspace = workspaceStore.findById(workspaceId)
            .orElseThrow(() -> new BusinessException(404, "工作空间不存在: " + workspaceId));

        // 合并操作在主仓库中执行
        String projectPath = workspace.getProjectPath();
        String sourceBranch = workspace.getBranchName();

        try {
            // 先切换到目标分支
            GitCommandResult checkoutResult = executeGitCommand(projectPath, "checkout", targetBranch);
            if (!checkoutResult.success()) {
                return new MergeResult(false, null, sourceBranch, targetBranch, false, List.of(),
                    "Checkout failed: " + checkoutResult.error());
            }

            // 执行合并
            GitCommandResult mergeResult = executeGitCommand(projectPath, "merge", sourceBranch, "--no-edit");

            if (!mergeResult.success()) {
                // 检测冲突
                if (mergeResult.error().contains("CONFLICT") || mergeResult.output().contains("CONFLICT")) {
                    List<String> conflictFiles = getConflictFiles(projectPath);

                    // 中止合并
                    executeGitCommand(projectPath, "merge", "--abort");

                    return new MergeResult(false, null, sourceBranch, targetBranch, true, conflictFiles,
                        "Merge conflicts detected");
                }
                return new MergeResult(false, null, sourceBranch, targetBranch, false, List.of(),
                    "Merge failed: " + mergeResult.error());
            }

            // 获取合并提交hash
            String mergeCommitHash = getGitOutput(projectPath, "rev-parse", "HEAD").trim();

            // 更新工作空间状态
            workspace.setStatus("MERGED");
            workspace.setUpdatedAt(LocalDateTime.now());
            workspaceStore.save(workspaceId, workspace);

            log.info("Merged branch {} into {} (commit: {})", sourceBranch, targetBranch, mergeCommitHash);

            return new MergeResult(true, mergeCommitHash, sourceBranch, targetBranch, false, List.of(), null);

        } catch (Exception e) {
            log.error("Failed to merge workspace {} into {}", workspaceId, targetBranch, e);
            // 尝试恢复到目标分支
            executeGitCommand(projectPath, "merge", "--abort");
            return new MergeResult(false, null, sourceBranch, targetBranch, false, List.of(), e.getMessage());
        }
    }

    @Override
    public ConflictCheckResult checkConflicts(String workspaceId, String targetBranch) {
        StoredWorkspace workspace = workspaceStore.findById(workspaceId)
            .orElseThrow(() -> new BusinessException(404, "工作空间不存在: " + workspaceId));

        String projectPath = workspace.getProjectPath();
        String sourceBranch = workspace.getBranchName();

        try {
            // 使用 git merge --no-commit --no-ff 进行预检
            // 先保存当前分支
            String currentBranch = getGitOutput(projectPath, "rev-parse", "--abbrev-ref", "HEAD").trim();

            // 切换到目标分支
            executeGitCommand(projectPath, "checkout", targetBranch);

            // 尝试合并（不提交）
            GitCommandResult mergeResult = executeGitCommand(
                projectPath, "merge", "--no-commit", "--no-ff", sourceBranch
            );

            boolean hasConflicts = false;
            List<String> conflictFiles = List.of();

            if (!mergeResult.success()) {
                if (mergeResult.error().contains("CONFLICT") || mergeResult.output().contains("CONFLICT")) {
                    hasConflicts = true;
                    conflictFiles = getConflictFiles(projectPath);
                }
            }

            // 获取变更的文件数（使用 --name-only 精确计数）
            String diffOutput = getGitOutput(projectPath, "diff", "--name-only", "--cached");
            int changedFiles = (int) diffOutput.lines().filter(l -> !l.isBlank()).count();

            // 中止合并，恢复原状态（仅在合并进行中时中止）
            File mergeHead = new File(projectPath, ".git/MERGE_HEAD");
            if (mergeHead.exists()) {
                executeGitCommand(projectPath, "merge", "--abort");
            } else {
                // 没有merge状态但有暂存内容，重置
                executeGitCommand(projectPath, "reset", "--hard", "HEAD");
            }
            executeGitCommand(projectPath, "checkout", currentBranch);

            return new ConflictCheckResult(hasConflicts, conflictFiles, changedFiles, null);

        } catch (Exception e) {
            log.error("Failed to check conflicts for workspace: {}", workspaceId, e);
            // 尝试恢复（仅在合并进行中时中止）
            File mergeHead = new File(projectPath, ".git/MERGE_HEAD");
            if (mergeHead.exists()) {
                executeGitCommand(projectPath, "merge", "--abort");
            }
            return new ConflictCheckResult(false, List.of(), 0, e.getMessage());
        }
    }

    @Override
    public SyncResult syncFromBranch(String workspaceId, String sourceBranch) {
        StoredWorkspace workspace = workspaceStore.findById(workspaceId)
            .orElseThrow(() -> new BusinessException(404, "工作空间不存在: " + workspaceId));

        String worktreePath = workspace.getWorktreePath();

        try {
            // 先fetch最新
            executeGitCommand(worktreePath, "fetch", "origin", sourceBranch);

            // 尝试rebase
            GitCommandResult rebaseResult = executeGitCommand(
                worktreePath, "rebase", "origin/" + sourceBranch
            );

            if (rebaseResult.success()) {
                workspace.setUpdatedAt(LocalDateTime.now());
                workspaceStore.save(workspaceId, workspace);

                return new SyncResult(true, "rebase", false, List.of(), null);
            }

            // rebase失败，检查冲突
            if (rebaseResult.error().contains("CONFLICT") || rebaseResult.output().contains("CONFLICT")) {
                List<String> conflictFiles = getConflictFiles(worktreePath);

                // 中止rebase
                executeGitCommand(worktreePath, "rebase", "--abort");

                return new SyncResult(false, "rebase", true, conflictFiles, "Rebase conflicts detected");
            }

            // 其他失败，中止
            executeGitCommand(worktreePath, "rebase", "--abort");
            return new SyncResult(false, "rebase", false, List.of(), rebaseResult.error());

        } catch (Exception e) {
            log.error("Failed to sync workspace {} from {}", workspaceId, sourceBranch, e);
            // 仅在rebase进行中时中止
            File rebaseDir = new File(worktreePath, ".git/rebase-merge");
            File rebaseApplyDir = new File(worktreePath, ".git/rebase-apply");
            if (rebaseDir.exists() || rebaseApplyDir.exists()) {
                executeGitCommand(worktreePath, "rebase", "--abort");
            }
            return new SyncResult(false, "rebase", false, List.of(), e.getMessage());
        }
    }

    // ===== Helper Methods =====

    private boolean checkBranchExists(String repoPath, String branchName) {
        GitCommandResult result = executeGitCommand(repoPath, "rev-parse", "--verify", branchName);
        return result.success();
    }

    private List<String> getConflictFiles(String repoPath) {
        String output = getGitOutput(repoPath, "diff", "--name-only", "--diff-filter=U");
        return output.lines()
            .filter(line -> !line.isBlank())
            .collect(Collectors.toList());
    }

    private String getGitOutput(String workingDir, String... args) {
        GitCommandResult result = executeGitCommand(workingDir, args);
        return result.output() != null ? result.output() : "";
    }

    /**
     * 执行Git命令
     */
    private GitCommandResult executeGitCommand(String workingDir, String... args) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.addAll(Arrays.asList(args));

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.directory(new File(workingDir));
            pb.redirectErrorStream(false);

            // 设置环境变量，禁用交互式提示
            Map<String, String> env = pb.environment();
            env.put("GIT_TERMINAL_PROMPT", "0");

            Process process = pb.start();

            String stdout;
            String stderr;
            try (BufferedReader outReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));
                 BufferedReader errReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                stdout = outReader.lines().collect(Collectors.joining("\n"));
                stderr = errReader.lines().collect(Collectors.joining("\n"));
            }

            int exitCode = process.waitFor();
            boolean success = exitCode == 0;

            if (!success) {
                log.debug("Git command failed (exit {}): {} in {}\nstdout: {}\nstderr: {}",
                    exitCode, String.join(" ", command), workingDir, stdout, stderr);
            }

            return new GitCommandResult(success, exitCode, stdout, stderr);

        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("Failed to execute git command: {} in {}", String.join(" ", command), workingDir, e);
            return new GitCommandResult(false, -1, "", e.getMessage());
        }
    }

    private WorkspaceInfo toWorkspaceInfo(StoredWorkspace workspace) {
        return new WorkspaceInfo(
            workspace.getId(),
            workspace.getProjectPath(),
            workspace.getWorktreePath(),
            workspace.getBranchName(),
            workspace.getBaseBranch(),
            workspace.getStatus(),
            workspace.getTaskId(),
            workspace.getAgentId(),
            workspace.getDescription(),
            workspace.getLastCommitHash(),
            workspace.getLastCommitMessage(),
            workspace.getCreatedAt() != null ? workspace.getCreatedAt().toString() : null,
            workspace.getUpdatedAt() != null ? workspace.getUpdatedAt().toString() : null
        );
    }

    /**
     * Git命令执行结果
     */
    private record GitCommandResult(
        boolean success,
        int exitCode,
        String output,
        String error
    ) {}
}
