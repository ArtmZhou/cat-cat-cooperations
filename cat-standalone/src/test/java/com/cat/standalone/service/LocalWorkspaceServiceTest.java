package com.cat.standalone.service;

import com.cat.cliagent.service.WorkspaceService;
import com.cat.cliagent.service.WorkspaceService.*;
import com.cat.common.exception.BusinessException;
import com.cat.standalone.store.JsonFileStore;
import com.cat.standalone.store.entity.StoredWorkspace;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for LocalWorkspaceService.
 *
 * Uses a real temporary Git repository to verify the full workspace lifecycle:
 * create → git status → commit → remove.
 */
class LocalWorkspaceServiceTest {

    @TempDir
    Path tempDir;

    private Path projectPath;
    private Path dataDir;
    private LocalWorkspaceService workspaceService;

    @BeforeEach
    void setUp() throws Exception {
        // Create a temp data directory for JSON storage
        dataDir = tempDir.resolve("data");
        Files.createDirectories(dataDir);

        // Create a test Git repository
        projectPath = tempDir.resolve("test-repo");
        Files.createDirectories(projectPath);

        // git init
        exec(projectPath, "git", "init");
        exec(projectPath, "git", "config", "user.email", "test@test.com");
        exec(projectPath, "git", "config", "user.name", "Test");

        // Create initial commit on main branch
        Path readme = projectPath.resolve("README.md");
        Files.writeString(readme, "# Test Project\n");
        exec(projectPath, "git", "add", "-A");
        exec(projectPath, "git", "commit", "-m", "Initial commit");

        // Ensure we are on a branch called "main"
        exec(projectPath, "git", "branch", "-M", "main");

        // Set up the workspace service
        JsonFileStore<StoredWorkspace> workspaceStore =
            new JsonFileStore<>(dataDir.toString(), "workspaces", StoredWorkspace.class);
        workspaceService = new LocalWorkspaceService(workspaceStore);

        // Use reflection to set the @Value properties (not Spring context)
        var worktreeDirField = LocalWorkspaceService.class.getDeclaredField("worktreeDirName");
        worktreeDirField.setAccessible(true);
        worktreeDirField.set(workspaceService, ".worktrees");

        var baseBranchField = LocalWorkspaceService.class.getDeclaredField("defaultBaseBranch");
        baseBranchField.setAccessible(true);
        baseBranchField.set(workspaceService, "main");
    }

    @Test
    @DisplayName("Complete lifecycle: create → git status → commit → remove")
    void testCompleteLifecycle() throws Exception {
        // === 1. Create Workspace ===
        CreateWorkspaceRequest request = new CreateWorkspaceRequest(
            projectPath.toString(),
            "feature/test-branch",
            "main",
            "task-001",
            "agent-001",
            "Test workspace"
        );

        WorkspaceInfo ws = workspaceService.createWorkspace(request);

        assertNotNull(ws, "Workspace should be created");
        assertNotNull(ws.id(), "Workspace should have an ID");
        assertEquals("feature/test-branch", ws.branchName());
        assertEquals("main", ws.baseBranch());
        assertEquals("ACTIVE", ws.status());
        assertEquals("task-001", ws.taskId());
        assertEquals("agent-001", ws.agentId());
        assertTrue(new File(ws.worktreePath()).exists(), "Worktree directory should exist");

        String workspaceId = ws.id();

        // === 2. List Workspaces ===
        var allWorkspaces = workspaceService.listWorkspaces();
        assertEquals(1, allWorkspaces.size(), "Should have 1 workspace");
        assertEquals(workspaceId, allWorkspaces.get(0).id());

        var projectWorkspaces = workspaceService.listWorkspacesByProject(projectPath.toString());
        assertEquals(1, projectWorkspaces.size());

        var noWorkspaces = workspaceService.listWorkspacesByProject("/nonexistent");
        assertTrue(noWorkspaces.isEmpty());

        // === 3. Get Workspace ===
        WorkspaceInfo fetched = workspaceService.getWorkspace(workspaceId);
        assertEquals(workspaceId, fetched.id());

        // === 4. Git Status (no changes) ===
        WorkspaceGitStatus status = workspaceService.getWorkspaceGitStatus(workspaceId);
        assertNotNull(status);
        assertEquals(workspaceId, status.workspaceId());
        assertEquals("feature/test-branch", status.branchName());
        assertFalse(status.hasUncommittedChanges(), "No changes yet");
        assertEquals(0, status.modifiedFiles());
        assertEquals(0, status.untrackedFiles());

        // === 5. Make changes in worktree and check status ===
        Path worktreeDir = Path.of(ws.worktreePath());
        Path newFile = worktreeDir.resolve("new-feature.txt");
        Files.writeString(newFile, "Hello from worktree!\n");

        WorkspaceGitStatus status2 = workspaceService.getWorkspaceGitStatus(workspaceId);
        assertTrue(status2.hasUncommittedChanges(), "Should have changes now");
        assertEquals(1, status2.untrackedFiles(), "Should have 1 untracked file");
        assertTrue(status2.untrackedFileList().contains("new-feature.txt"));

        // === 6. Modify existing file ===
        Path readmeInWorktree = worktreeDir.resolve("README.md");
        Files.writeString(readmeInWorktree, "# Modified in worktree\n");

        WorkspaceGitStatus status3 = workspaceService.getWorkspaceGitStatus(workspaceId);
        assertTrue(status3.modifiedFiles() > 0 || status3.untrackedFiles() > 0,
            "Should detect file changes");

        // === 7. Commit changes ===
        CommitResult commitResult = workspaceService.commitChanges(workspaceId, "feat: add feature file");
        assertTrue(commitResult.success(), "Commit should succeed");
        assertNotNull(commitResult.commitHash(), "Should have commit hash");
        assertEquals("feat: add feature file", commitResult.message());

        // Verify workspace status updated
        WorkspaceInfo afterCommit = workspaceService.getWorkspace(workspaceId);
        assertEquals("COMMITTED", afterCommit.status());
        assertEquals(commitResult.commitHash(), afterCommit.lastCommitHash());

        // === 8. Commit when nothing to commit ===
        CommitResult emptyCommit = workspaceService.commitChanges(workspaceId, "empty commit");
        assertTrue(emptyCommit.success(), "Should succeed with nothing to commit");
        assertEquals("Nothing to commit", emptyCommit.message());

        // === 9. Git status after commit (should be clean) ===
        WorkspaceGitStatus status4 = workspaceService.getWorkspaceGitStatus(workspaceId);
        assertFalse(status4.hasUncommittedChanges(), "Should be clean after commit");
        assertTrue(status4.aheadCount() > 0, "Should be ahead of main branch");

        // === 10. Check conflicts (should be clean merge) ===
        ConflictCheckResult conflictResult = workspaceService.checkConflicts(workspaceId, "main");
        assertNotNull(conflictResult);
        assertFalse(conflictResult.hasConflicts(), "Should not have conflicts");
        assertTrue(conflictResult.totalChangedFiles() > 0, "Should have changed files");

        // === 11. Merge branch ===
        MergeResult mergeResult = workspaceService.mergeBranch(workspaceId, "main");
        assertTrue(mergeResult.success(), "Merge should succeed: " + mergeResult.error());
        assertNotNull(mergeResult.mergeCommitHash(), "Should have merge commit hash");
        assertFalse(mergeResult.hasConflicts());

        // Verify workspace status updated to MERGED
        WorkspaceInfo afterMerge = workspaceService.getWorkspace(workspaceId);
        assertEquals("MERGED", afterMerge.status());

        // === 12. Verify the merge on main ===
        // The file should now exist in the main repo
        Path newFileOnMain = projectPath.resolve("new-feature.txt");
        assertTrue(Files.exists(newFileOnMain), "Feature file should be on main after merge");

        // === 13. Remove workspace ===
        workspaceService.removeWorkspace(workspaceId, false);

        WorkspaceInfo removed = workspaceService.getWorkspace(workspaceId);
        assertEquals("REMOVED", removed.status());
        assertFalse(new File(ws.worktreePath()).exists(), "Worktree directory should be deleted");

        // === 14. Verify remove of already removed throws ===
        assertThrows(BusinessException.class, () ->
            workspaceService.removeWorkspace(workspaceId, false));
    }

    @Test
    @DisplayName("Auto-generated branch name works correctly")
    void testAutoGeneratedBranchName() {
        CreateWorkspaceRequest request = new CreateWorkspaceRequest(
            projectPath.toString(),
            null,     // auto-generate
            "main",
            "task-uuid-12345678",
            "agent-ab",
            "Auto branch test"
        );

        WorkspaceInfo ws = workspaceService.createWorkspace(request);
        assertNotNull(ws);
        assertTrue(ws.branchName().startsWith("task/task-uui"),
            "Branch should start with task prefix: " + ws.branchName());
        assertTrue(ws.branchName().contains("agent-"),
            "Branch should contain agent prefix: " + ws.branchName());

        // Cleanup
        workspaceService.removeWorkspace(ws.id(), true);
    }

    @Test
    @DisplayName("Creating workspace with non-existent project path throws")
    void testCreateWithInvalidProjectPath() {
        CreateWorkspaceRequest request = new CreateWorkspaceRequest(
            "/nonexistent/path",
            "test-branch",
            "main",
            null, null, null
        );

        assertThrows(BusinessException.class, () ->
            workspaceService.createWorkspace(request));
    }

    @Test
    @DisplayName("Creating workspace with non-git directory throws")
    void testCreateWithNonGitDirectory() throws Exception {
        Path nonGitDir = tempDir.resolve("non-git");
        Files.createDirectories(nonGitDir);

        CreateWorkspaceRequest request = new CreateWorkspaceRequest(
            nonGitDir.toString(),
            "test-branch",
            "main",
            null, null, null
        );

        assertThrows(BusinessException.class, () ->
            workspaceService.createWorkspace(request));
    }

    @Test
    @DisplayName("Creating duplicate branch throws")
    void testCreateDuplicateBranchThrows() {
        CreateWorkspaceRequest request1 = new CreateWorkspaceRequest(
            projectPath.toString(),
            "dup-branch",
            "main",
            null, null, null
        );

        WorkspaceInfo ws1 = workspaceService.createWorkspace(request1);
        assertNotNull(ws1);

        CreateWorkspaceRequest request2 = new CreateWorkspaceRequest(
            projectPath.toString(),
            "dup-branch",
            "main",
            null, null, null
        );

        assertThrows(BusinessException.class, () ->
            workspaceService.createWorkspace(request2));

        // Cleanup
        workspaceService.removeWorkspace(ws1.id(), true);
    }

    @Test
    @DisplayName("Get non-existent workspace throws 404")
    void testGetNonExistentWorkspace() {
        assertThrows(BusinessException.class, () ->
            workspaceService.getWorkspace("nonexistent-id"));
    }

    @Test
    @DisplayName("Multiple concurrent workspaces can coexist")
    void testMultipleConcurrentWorkspaces() throws Exception {
        // Create two workspaces for the same project
        WorkspaceInfo ws1 = workspaceService.createWorkspace(new CreateWorkspaceRequest(
            projectPath.toString(), "feature/ws1", "main", null, null, "Workspace 1"
        ));
        WorkspaceInfo ws2 = workspaceService.createWorkspace(new CreateWorkspaceRequest(
            projectPath.toString(), "feature/ws2", "main", null, null, "Workspace 2"
        ));

        // Both should exist and be active
        assertEquals("ACTIVE", ws1.status());
        assertEquals("ACTIVE", ws2.status());

        // Modify different files in each worktree
        Files.writeString(Path.of(ws1.worktreePath(), "file1.txt"), "Content from ws1");
        Files.writeString(Path.of(ws2.worktreePath(), "file2.txt"), "Content from ws2");

        // Commit in both
        CommitResult commit1 = workspaceService.commitChanges(ws1.id(), "ws1: add file1");
        CommitResult commit2 = workspaceService.commitChanges(ws2.id(), "ws2: add file2");
        assertTrue(commit1.success());
        assertTrue(commit2.success());

        // Both should be independently committed
        assertEquals("COMMITTED", workspaceService.getWorkspace(ws1.id()).status());
        assertEquals("COMMITTED", workspaceService.getWorkspace(ws2.id()).status());

        // Merge ws1 first
        MergeResult merge1 = workspaceService.mergeBranch(ws1.id(), "main");
        assertTrue(merge1.success(), "ws1 merge should succeed: " + merge1.error());

        // Merge ws2 (no conflict since different files)
        MergeResult merge2 = workspaceService.mergeBranch(ws2.id(), "main");
        assertTrue(merge2.success(), "ws2 merge should succeed: " + merge2.error());

        // Verify both files exist on main
        assertTrue(Files.exists(projectPath.resolve("file1.txt")), "file1 should exist on main");
        assertTrue(Files.exists(projectPath.resolve("file2.txt")), "file2 should exist on main");

        // Cleanup
        workspaceService.removeWorkspace(ws1.id(), true);
        workspaceService.removeWorkspace(ws2.id(), true);
    }

    @Test
    @DisplayName("Conflict detection works when two workspaces modify same file")
    void testConflictDetection() throws Exception {
        // Create two workspaces
        WorkspaceInfo ws1 = workspaceService.createWorkspace(new CreateWorkspaceRequest(
            projectPath.toString(), "feature/conflict1", "main", null, null, null
        ));
        WorkspaceInfo ws2 = workspaceService.createWorkspace(new CreateWorkspaceRequest(
            projectPath.toString(), "feature/conflict2", "main", null, null, null
        ));

        // Both modify the same file differently
        Files.writeString(Path.of(ws1.worktreePath(), "README.md"), "# Version from ws1\n");
        Files.writeString(Path.of(ws2.worktreePath(), "README.md"), "# Version from ws2\n");

        // Commit both
        assertTrue(workspaceService.commitChanges(ws1.id(), "ws1 change").success());
        assertTrue(workspaceService.commitChanges(ws2.id(), "ws2 change").success());

        // Merge ws1 into main first
        MergeResult merge1 = workspaceService.mergeBranch(ws1.id(), "main");
        assertTrue(merge1.success());

        // Now check conflicts for ws2 against main
        ConflictCheckResult conflictCheck = workspaceService.checkConflicts(ws2.id(), "main");
        assertTrue(conflictCheck.hasConflicts(), "Should detect conflicts");
        assertFalse(conflictCheck.conflictFiles().isEmpty(), "Should list conflict files");

        // Cleanup
        workspaceService.removeWorkspace(ws1.id(), true);
        workspaceService.removeWorkspace(ws2.id(), true);
    }

    // Helper to execute a command in a directory
    private void exec(Path dir, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(dir.toFile());
        pb.redirectErrorStream(true);
        Process p = pb.start();
        String output = new String(p.getInputStream().readAllBytes());
        int exitCode = p.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Command failed (" + exitCode + "): "
                + String.join(" ", command) + "\n" + output);
        }
    }
}
