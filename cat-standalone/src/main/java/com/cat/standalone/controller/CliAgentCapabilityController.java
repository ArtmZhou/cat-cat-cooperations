package com.cat.standalone.controller;

import com.cat.cliagent.dto.CliAgentCapabilityDto;
import com.cat.cliagent.service.CliAgentCapabilityService;
import com.cat.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CLI Agent能力管理控制器
 */
@Tag(name = "CLI Agent能力管理", description = "CLI Agent的能力配置和查询")
@RestController
@RequestMapping("/api/v1/cli-agents")
@RequiredArgsConstructor
public class CliAgentCapabilityController {

    private final CliAgentCapabilityService capabilityService;

    @Operation(summary = "获取Agent能力列表", description = "获取指定Agent的所有能力")
    @GetMapping("/{agentId}/capabilities")
    public ApiResponse<List<CliAgentCapabilityDto>> getAgentCapabilities(@PathVariable String agentId) {
        List<CliAgentCapabilityDto> capabilities = capabilityService.getAgentCapabilities(agentId);
        return ApiResponse.success(capabilities);
    }

    @Operation(summary = "添加Agent能力", description = "为指定Agent添加能力")
    @PostMapping("/{agentId}/capabilities")
    public ApiResponse<CliAgentCapabilityDto> addCapability(
            @PathVariable String agentId,
            @RequestBody CliAgentCapabilityDto capability) {
        CliAgentCapabilityDto result = capabilityService.addCapability(agentId, capability);
        return ApiResponse.success(result);
    }

    @Operation(summary = "更新Agent能力", description = "更新指定Agent的能力")
    @PutMapping("/{agentId}/capabilities/{capabilityId}")
    public ApiResponse<CliAgentCapabilityDto> updateCapability(
            @PathVariable String agentId,
            @PathVariable String capabilityId,
            @RequestBody CliAgentCapabilityDto capability) {
        CliAgentCapabilityDto result = capabilityService.updateCapability(agentId, capabilityId, capability);
        return ApiResponse.success(result);
    }

    @Operation(summary = "移除Agent能力", description = "移除指定Agent的能力")
    @DeleteMapping("/{agentId}/capabilities/{capabilityId}")
    public ApiResponse<Void> removeCapability(
            @PathVariable String agentId,
            @PathVariable String capabilityId) {
        capabilityService.removeCapability(agentId, capabilityId);
        return ApiResponse.success();
    }

    @Operation(summary = "按能力查找Agent", description = "根据能力类型、领域标签、熟练度查找Agent")
    @GetMapping("/by-capability")
    public ApiResponse<List<String>> findAgentsByCapability(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String domainTag,
            @RequestParam(required = false) Integer minProficiency) {
        List<String> agentIds = capabilityService.findAgentsByCapability(type, domainTag, minProficiency);
        return ApiResponse.success(agentIds);
    }

    @Operation(summary = "获取所有能力类型", description = "获取系统支持的所有能力类型")
    @GetMapping("/capability-types")
    public ApiResponse<List<CliAgentCapabilityService.CapabilityTypeInfo>> getAllCapabilityTypes() {
        List<CliAgentCapabilityService.CapabilityTypeInfo> types = capabilityService.getAllCapabilityTypes();
        return ApiResponse.success(types);
    }
}