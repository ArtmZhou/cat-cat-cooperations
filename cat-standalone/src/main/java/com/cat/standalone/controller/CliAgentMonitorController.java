package com.cat.standalone.controller;

import com.cat.cliagent.dto.CliAgentMonitorStatus;
import com.cat.cliagent.service.CliAgentMonitorService;
import com.cat.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CLI Agent监控控制器
 */
@Tag(name = "CLI Agent监控", description = "CLI Agent的状态监控和系统概览")
@RestController
@RequestMapping("/api/v1/cli-agents/monitor")
@RequiredArgsConstructor
public class CliAgentMonitorController {

    private final CliAgentMonitorService monitorService;

    @Operation(summary = "获取Agent监控状态", description = "获取指定Agent的详细监控状态")
    @GetMapping("/{agentId}")
    public ApiResponse<CliAgentMonitorStatus> getAgentMonitorStatus(@PathVariable String agentId) {
        CliAgentMonitorStatus status = monitorService.getAgentMonitorStatus(agentId);
        return ApiResponse.success(status);
    }

    @Operation(summary = "获取所有Agent监控状态", description = "获取所有Agent的监控状态")
    @GetMapping("/all")
    public ApiResponse<List<CliAgentMonitorStatus>> getAllAgentMonitorStatus() {
        List<CliAgentMonitorStatus> statusList = monitorService.getAllAgentMonitorStatus();
        return ApiResponse.success(statusList);
    }

    @Operation(summary = "获取运行中Agent监控状态", description = "获取所有运行中Agent的监控状态")
    @GetMapping("/running")
    public ApiResponse<List<CliAgentMonitorStatus>> getRunningAgentMonitorStatus() {
        List<CliAgentMonitorStatus> statusList = monitorService.getRunningAgentMonitorStatus();
        return ApiResponse.success(statusList);
    }

    @Operation(summary = "获取系统概览", description = "获取系统整体统计概览")
    @GetMapping("/overview")
    public ApiResponse<CliAgentMonitorService.SystemOverview> getSystemOverview() {
        CliAgentMonitorService.SystemOverview overview = monitorService.getSystemOverview();
        return ApiResponse.success(overview);
    }
}