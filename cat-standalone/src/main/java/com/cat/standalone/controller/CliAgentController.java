package com.cat.standalone.controller;

import com.cat.cliagent.dto.*;
import com.cat.cliagent.service.CliAgentService;
import com.cat.cliagent.service.CliProcessService;
import com.cat.cliagent.service.CliSessionService;
import com.cat.cliagent.service.CliTaskExecutionService;
import com.cat.cliagent.service.TokenUsageService;
import com.cat.common.model.ApiResponse;
import com.cat.common.model.PageResult;
import com.cat.standalone.service.LocalCliSessionService;
import com.cat.standalone.service.LocalCliTaskExecutionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Future;

/**
 * CLI Agent实例管理控制器
 */
@Tag(name = "CLI Agent实例管理", description = "CLI Agent实例的创建、配置、查询、删除、进程管理、会话通信、任务执行、Token统计")
@RestController
@RequestMapping("/api/v1/cli-agents")
@RequiredArgsConstructor
public class CliAgentController {

    private final CliAgentService cliAgentService;
    private final CliProcessService cliProcessService;
    private final CliSessionService cliSessionService;
    private final LocalCliSessionService localCliSessionService;
    private final CliTaskExecutionService taskExecutionService;
    private final TokenUsageService tokenUsageService;

    @Operation(summary = "创建CLI Agent实例", description = "基于模板创建CLI Agent实例")
    @PostMapping
    public ApiResponse<CliAgentResponse> createAgent(
            @Valid @RequestBody CreateCliAgentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        CliAgentResponse response = cliAgentService.createAgent(request, userId != null ? userId : "system");
        return ApiResponse.success(response);
    }

    @Operation(summary = "更新CLI Agent实例", description = "更新CLI Agent配置")
    @PutMapping("/{agentId}")
    public ApiResponse<CliAgentResponse> updateAgent(
            @PathVariable String agentId,
            @Valid @RequestBody UpdateCliAgentRequest request) {
        CliAgentResponse response = cliAgentService.updateAgent(agentId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "删除CLI Agent实例", description = "删除CLI Agent（运行中的Agent需先停止）")
    @DeleteMapping("/{agentId}")
    public ApiResponse<Void> deleteAgent(@PathVariable String agentId) {
        cliAgentService.deleteAgent(agentId);
        return ApiResponse.success();
    }

    @Operation(summary = "获取CLI Agent详情", description = "根据ID获取CLI Agent详情")
    @GetMapping("/{agentId}")
    public ApiResponse<CliAgentResponse> getAgent(@PathVariable String agentId) {
        CliAgentResponse response = cliAgentService.getAgent(agentId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "查询CLI Agent列表", description = "分页查询CLI Agent列表")
    @GetMapping
    public ApiResponse<PageResult<CliAgentResponse>> listAgents(CliAgentQuery query) {
        PageResult<CliAgentResponse> result = cliAgentService.listAgents(query);
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取可用Agent列表", description = "获取状态为RUNNING的Agent列表")
    @GetMapping("/available")
    public ApiResponse<List<CliAgentResponse>> getAvailableAgents() {
        List<CliAgentResponse> agents = cliAgentService.getAvailableAgents();
        return ApiResponse.success(agents);
    }

    // ========== 进程生命周期管理 ==========

    @Operation(summary = "启动CLI进程", description = "启动指定Agent的CLI进程")
    @PostMapping("/{agentId}/actions/start")
    public ApiResponse<CliAgentResponse> startProcess(@PathVariable String agentId) {
        CliAgentResponse response = cliProcessService.startProcess(agentId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "停止CLI进程", description = "停止指定Agent的CLI进程")
    @PostMapping("/{agentId}/actions/stop")
    public ApiResponse<CliAgentResponse> stopProcess(@PathVariable String agentId) {
        CliAgentResponse response = cliProcessService.stopProcess(agentId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "重启CLI进程", description = "重启指定Agent的CLI进程")
    @PostMapping("/{agentId}/actions/restart")
    public ApiResponse<CliAgentResponse> restartProcess(@PathVariable String agentId) {
        CliAgentResponse response = cliProcessService.restartProcess(agentId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "获取进程状态", description = "获取指定Agent的CLI进程详细状态")
    @GetMapping("/{agentId}/status")
    public ApiResponse<CliProcessService.ProcessStatus> getProcessStatus(@PathVariable String agentId) {
        CliProcessService.ProcessStatus status = cliProcessService.getProcessStatus(agentId);
        return ApiResponse.success(status);
    }

    @Operation(summary = "检查进程健康", description = "检查指定Agent的CLI进程是否健康")
    @GetMapping("/{agentId}/health")
    public ApiResponse<Boolean> checkProcessHealth(@PathVariable String agentId) {
        boolean healthy = cliProcessService.isProcessHealthy(agentId);
        return ApiResponse.success(healthy);
    }

    // ========== 会话通信管理 ==========

    @Operation(summary = "发送输入到CLI", description = "向CLI进程发送输入内容（Prompt/JSON）")
    @PostMapping("/{agentId}/session/input")
    public ApiResponse<Boolean> sendInput(
            @PathVariable String agentId,
            @RequestBody String input) {
        boolean success = cliSessionService.sendInput(agentId, input);
        return ApiResponse.success(success);
    }

    @Operation(summary = "获取会话状态", description = "获取CLI会话的详细状态信息")
    @GetMapping("/{agentId}/session/status")
    public ApiResponse<CliSessionService.SessionStatus> getSessionStatus(@PathVariable String agentId) {
        CliSessionService.SessionStatus status = cliSessionService.getSessionStatus(agentId);
        return ApiResponse.success(status);
    }

    @Operation(summary = "检查会话活跃", description = "检查CLI会话是否活跃")
    @GetMapping("/{agentId}/session/active")
    public ApiResponse<Boolean> isSessionActive(@PathVariable String agentId) {
        boolean active = cliSessionService.isSessionActive(agentId);
        return ApiResponse.success(active);
    }

    @Operation(summary = "关闭会话", description = "关闭CLI会话（不停止进程）")
    @PostMapping("/{agentId}/session/close")
    public ApiResponse<CliAgentResponse> closeSession(@PathVariable String agentId) {
        CliAgentResponse response = cliSessionService.closeSession(agentId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "获取输出日志", description = "获取Agent最近的输出日志")
    @GetMapping("/{agentId}/logs")
    public ApiResponse<List<LocalCliSessionService.OutputLogEntry>> getOutputLogs(
            @PathVariable String agentId,
            @RequestParam(defaultValue = "50") int limit) {
        List<LocalCliSessionService.OutputLogEntry> logs = localCliSessionService.getOutputLogs(agentId, limit);
        return ApiResponse.success(logs);
    }

    @Operation(summary = "清空输出日志", description = "清空Agent的输出日志")
    @PostMapping("/{agentId}/logs/clear")
    public ApiResponse<Void> clearOutputLogs(@PathVariable String agentId) {
        localCliSessionService.clearOutputLogs(agentId);
        return ApiResponse.success();
    }

    // ========== 任务执行控制 ==========

    @Operation(summary = "执行任务", description = "同步执行任务并等待结果")
    @PostMapping("/{agentId}/tasks/execute")
    public ApiResponse<CliTaskExecutionService.TaskExecutionResult> executeTask(
            @PathVariable String agentId,
            @RequestBody TaskExecuteRequest request) {
        CliTaskExecutionService.TaskExecutionResult result =
            taskExecutionService.executeTask(agentId, request.input(), request.timeoutSeconds());
        return ApiResponse.success(result);
    }

    @Operation(summary = "异步执行任务", description = "异步执行任务，立即返回任务ID")
    @PostMapping("/{agentId}/tasks/execute-async")
    public ApiResponse<String> executeTaskAsync(
            @PathVariable String agentId,
            @RequestBody TaskExecuteRequest request) {
        Future<CliTaskExecutionService.TaskExecutionResult> future =
            taskExecutionService.executeTaskAsync(agentId, request.input(), request.timeoutSeconds());
        // 返回任务状态查询端点的标识
        return ApiResponse.success("Task submitted");
    }

    @Operation(summary = "使用工作空间执行任务", description = "在独立的Git Worktree工作空间中执行任务，支持多Agent并发开发")
    @PostMapping("/{agentId}/tasks/execute-with-workspace")
    public ApiResponse<String> executeTaskWithWorkspace(
            @PathVariable String agentId,
            @RequestBody WorkspaceTaskExecuteRequest request) {
        ((LocalCliTaskExecutionService) taskExecutionService).executeTaskWithWorkspace(
            agentId,
            request.input(),
            request.timeoutSeconds() != null ? request.timeoutSeconds() : 0,
            request.projectPath(),
            request.baseBranch(),
            request.description()
        );
        return ApiResponse.success("Task with workspace submitted");
    }

    @Operation(summary = "取消任务", description = "取消正在执行的任务")
    @PostMapping("/tasks/{taskId}/cancel")
    public ApiResponse<Boolean> cancelTask(@PathVariable String taskId) {
        boolean result = taskExecutionService.cancelTask(taskId);
        return ApiResponse.success(result);
    }

    @Operation(summary = "获取任务状态", description = "获取任务执行状态")
    @GetMapping("/tasks/{taskId}/status")
    public ApiResponse<CliTaskExecutionService.TaskExecutionStatus> getTaskStatus(@PathVariable String taskId) {
        CliTaskExecutionService.TaskExecutionStatus status = taskExecutionService.getTaskStatus(taskId);
        return ApiResponse.success(status);
    }

    @Operation(summary = "获取Agent并发任务数", description = "获取Agent当前并发执行的任务数")
    @GetMapping("/{agentId}/concurrent-count")
    public ApiResponse<Integer> getConcurrentTaskCount(@PathVariable String agentId) {
        int count = taskExecutionService.getConcurrentTaskCount(agentId);
        return ApiResponse.success(count);
    }

    @Operation(summary = "检查Agent是否可接受任务", description = "检查Agent是否可以接受新任务")
    @GetMapping("/{agentId}/can-accept-task")
    public ApiResponse<Boolean> canAcceptTask(@PathVariable String agentId) {
        boolean canAccept = taskExecutionService.canAcceptTask(agentId);
        return ApiResponse.success(canAccept);
    }

    // 任务执行请求DTO
    public record TaskExecuteRequest(String input, Integer timeoutSeconds) {}

    // 工作空间任务执行请求DTO
    public record WorkspaceTaskExecuteRequest(
        String input,
        Integer timeoutSeconds,
        String projectPath,
        String baseBranch,
        String description
    ) {}

    // ========== Token使用统计 ==========

    @Operation(summary = "获取Agent Token统计", description = "获取指定Agent的Token使用统计")
    @GetMapping("/{agentId}/token-stats")
    public ApiResponse<TokenUsageService.TokenUsageStats> getAgentTokenStats(@PathVariable String agentId) {
        TokenUsageService.TokenUsageStats stats = tokenUsageService.getAgentStats(agentId);
        return ApiResponse.success(stats);
    }

    @Operation(summary = "获取Agent Token记录", description = "获取指定Agent在时间范围内的Token使用记录")
    @GetMapping("/{agentId}/token-records")
    public ApiResponse<List<TokenUsageService.TokenUsageRecord>> getAgentTokenRecords(
            @PathVariable String agentId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        List<TokenUsageService.TokenUsageRecord> records = tokenUsageService.getAgentRecords(agentId, startTime, endTime);
        return ApiResponse.success(records);
    }

    @Operation(summary = "获取系统Token统计", description = "获取系统整体Token使用统计")
    @GetMapping("/system/token-stats")
    public ApiResponse<TokenUsageService.TokenUsageStats> getSystemTokenStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        TokenUsageService.TokenUsageStats stats = tokenUsageService.getSystemStats(startTime, endTime);
        return ApiResponse.success(stats);
    }
}