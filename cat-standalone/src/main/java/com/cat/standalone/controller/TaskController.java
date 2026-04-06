package com.cat.standalone.controller;

import com.cat.task.dto.*;
import com.cat.task.service.TaskService;
import com.cat.common.model.ApiResponse;
import com.cat.common.model.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Task管理控制器
 */
@Tag(name = "Task管理", description = "任务创建、分配、执行、状态管理")
@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @Operation(summary = "创建Task")
    @PostMapping
    public ApiResponse<TaskResponse> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        TaskResponse response = taskService.createTask(request, userId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "更新Task")
    @PutMapping("/{taskId}")
    public ApiResponse<TaskResponse> updateTask(
            @PathVariable String taskId,
            @Valid @RequestBody UpdateTaskRequest request) {
        TaskResponse response = taskService.updateTask(taskId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "删除Task")
    @DeleteMapping("/{taskId}")
    public ApiResponse<Void> deleteTask(@PathVariable String taskId) {
        taskService.deleteTask(taskId);
        return ApiResponse.success();
    }

    @Operation(summary = "获取Task详情")
    @GetMapping("/{taskId}")
    public ApiResponse<TaskResponse> getTask(@PathVariable String taskId) {
        TaskResponse response = taskService.getTask(taskId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "查询Task列表")
    @GetMapping
    public ApiResponse<PageResult<TaskResponse>> listTasks(TaskQuery query) {
        PageResult<TaskResponse> result = taskService.listTasks(query);
        return ApiResponse.success(result);
    }

    @Operation(summary = "分配Task")
    @PostMapping("/{taskId}/assign")
    public ApiResponse<TaskResponse> assignTask(
            @PathVariable String taskId,
            @RequestBody List<String> agentIds) {
        TaskResponse response = taskService.assignTask(taskId, agentIds);
        return ApiResponse.success(response);
    }

    @Operation(summary = "启动Task")
    @PostMapping("/{taskId}/start")
    public ApiResponse<TaskResponse> startTask(@PathVariable String taskId) {
        TaskResponse response = taskService.startTask(taskId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "完成Task")
    @PostMapping("/{taskId}/complete")
    public ApiResponse<TaskResponse> completeTask(
            @PathVariable String taskId,
            @RequestBody(required = false) String output) {
        TaskResponse response = taskService.completeTask(taskId, output);
        return ApiResponse.success(response);
    }

    @Operation(summary = "失败Task")
    @PostMapping("/{taskId}/fail")
    public ApiResponse<TaskResponse> failTask(
            @PathVariable String taskId,
            @RequestBody(required = false) String errorMessage) {
        TaskResponse response = taskService.failTask(taskId, errorMessage);
        return ApiResponse.success(response);
    }

    @Operation(summary = "取消Task")
    @PostMapping("/{taskId}/cancel")
    public ApiResponse<TaskResponse> cancelTask(@PathVariable String taskId) {
        TaskResponse response = taskService.cancelTask(taskId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "重试Task")
    @PostMapping("/{taskId}/retry")
    public ApiResponse<TaskResponse> retryTask(@PathVariable String taskId) {
        TaskResponse response = taskService.retryTask(taskId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "获取Task日志")
    @GetMapping("/{taskId}/logs")
    public ApiResponse<List<TaskLogResponse>> getTaskLogs(@PathVariable String taskId) {
        List<TaskLogResponse> logs = taskService.getTaskLogs(taskId);
        return ApiResponse.success(logs);
    }
}