package com.cat.standalone.controller;

import com.cat.agent.dto.*;
import com.cat.agent.service.AgentService;
import com.cat.common.model.ApiResponse;
import com.cat.common.model.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent管理控制器
 */
@Tag(name = "Agent管理", description = "Agent创建、配置、状态管理")
@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @Operation(summary = "创建Agent")
    @PostMapping
    public ApiResponse<AgentResponse> createAgent(
            @Valid @RequestBody CreateAgentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        AgentResponse response = agentService.createAgent(request, userId != null ? userId : "system");
        return ApiResponse.success(response);
    }

    @Operation(summary = "更新Agent")
    @PutMapping("/{agentId}")
    public ApiResponse<AgentResponse> updateAgent(
            @PathVariable String agentId,
            @Valid @RequestBody UpdateAgentRequest request) {
        AgentResponse response = agentService.updateAgent(agentId, request);
        return ApiResponse.success(response);
    }

    @Operation(summary = "删除Agent")
    @DeleteMapping("/{agentId}")
    public ApiResponse<Void> deleteAgent(@PathVariable String agentId) {
        agentService.deleteAgent(agentId);
        return ApiResponse.success();
    }

    @Operation(summary = "获取Agent详情")
    @GetMapping("/{agentId}")
    public ApiResponse<AgentResponse> getAgent(@PathVariable String agentId) {
        AgentResponse response = agentService.getAgent(agentId);
        return ApiResponse.success(response);
    }

    @Operation(summary = "查询Agent列表")
    @GetMapping
    public ApiResponse<PageResult<AgentResponse>> listAgents(AgentQuery query) {
        PageResult<AgentResponse> result = agentService.listAgents(query);
        return ApiResponse.success(result);
    }

    @Operation(summary = "启用Agent")
    @PostMapping("/{agentId}/actions/enable")
    public ApiResponse<Void> enableAgent(@PathVariable String agentId) {
        agentService.enableAgent(agentId);
        return ApiResponse.success();
    }

    @Operation(summary = "禁用Agent")
    @PostMapping("/{agentId}/actions/disable")
    public ApiResponse<Void> disableAgent(@PathVariable String agentId) {
        agentService.disableAgent(agentId);
        return ApiResponse.success();
    }

    @Operation(summary = "获取可用Agent")
    @GetMapping("/available")
    public ApiResponse<List<AgentResponse>> getAvailableAgents() {
        return ApiResponse.success(agentService.getAvailableAgents());
    }

    @Operation(summary = "生成接入凭证")
    @GetMapping("/{agentId}/credential")
    public ApiResponse<String> generateAccessKey(@PathVariable String agentId) {
        String accessKey = agentService.generateAccessKey(agentId);
        return ApiResponse.success(accessKey);
    }
}