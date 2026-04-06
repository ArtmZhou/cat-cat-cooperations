package com.cat.standalone.controller;

import com.cat.cliagent.dto.CreateCliAgentTemplateRequest;
import com.cat.cliagent.dto.CliAgentTemplateResponse;
import com.cat.cliagent.service.CliAgentTemplateService;
import com.cat.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CLI Agent模板管理控制器
 */
@Tag(name = "CLI Agent模板管理", description = "CLI Agent模板的创建、查询、删除")
@RestController
@RequestMapping("/api/v1/cli-agent/templates")
@RequiredArgsConstructor
public class CliAgentTemplateController {

    private final CliAgentTemplateService templateService;

    @Operation(summary = "获取所有模板", description = "获取所有CLI Agent模板（内置+自定义）")
    @GetMapping
    public ApiResponse<List<CliAgentTemplateResponse>> getAllTemplates() {
        List<CliAgentTemplateResponse> templates = templateService.getAllTemplates();
        return ApiResponse.success(templates);
    }

    @Operation(summary = "获取内置模板", description = "获取内置的CLI Agent模板列表")
    @GetMapping("/built-in")
    public ApiResponse<List<CliAgentTemplateResponse>> getBuiltInTemplates() {
        List<CliAgentTemplateResponse> templates = templateService.getBuiltInTemplates();
        return ApiResponse.success(templates);
    }

    @Operation(summary = "获取模板详情", description = "根据ID获取CLI Agent模板详情")
    @GetMapping("/{templateId}")
    public ApiResponse<CliAgentTemplateResponse> getTemplate(@PathVariable String templateId) {
        CliAgentTemplateResponse template = templateService.getTemplate(templateId);
        return ApiResponse.success(template);
    }

    @Operation(summary = "创建自定义模板", description = "创建自定义CLI Agent模板")
    @PostMapping
    public ApiResponse<CliAgentTemplateResponse> createTemplate(
            @Valid @RequestBody CreateCliAgentTemplateRequest request) {
        CliAgentTemplateResponse template = templateService.createTemplate(request);
        return ApiResponse.success(template);
    }

    @Operation(summary = "更新模板", description = "更新自定义CLI Agent模板")
    @PutMapping("/{templateId}")
    public ApiResponse<CliAgentTemplateResponse> updateTemplate(
            @PathVariable String templateId,
            @Valid @RequestBody CreateCliAgentTemplateRequest request) {
        CliAgentTemplateResponse template = templateService.updateTemplate(templateId, request);
        return ApiResponse.success(template);
    }

    @Operation(summary = "删除模板", description = "删除自定义CLI Agent模板（内置模板不可删除）")
    @DeleteMapping("/{templateId}")
    public ApiResponse<Void> deleteTemplate(@PathVariable String templateId) {
        templateService.deleteTemplate(templateId);
        return ApiResponse.success();
    }
}