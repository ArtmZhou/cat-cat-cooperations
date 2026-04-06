package com.cat.cliagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * CLI Agent模板响应
 */
@Data
@Schema(description = "CLI Agent模板响应")
public class CliAgentTemplateResponse {

    @Schema(description = "模板ID")
    private String id;

    @Schema(description = "模板名称")
    private String name;

    @Schema(description = "CLI类型标识")
    private String cliType;

    @Schema(description = "模板描述")
    private String description;

    @Schema(description = "CLI可执行文件路径")
    private String executablePath;

    @Schema(description = "默认启动参数")
    private List<String> defaultArgs;

    @Schema(description = "必需的环境变量列表")
    private List<String> requiredEnvVars;

    @Schema(description = "可选的环境变量列表")
    private List<String> optionalEnvVars;

    @Schema(description = "配置文件模板")
    private Map<String, Object> configTemplate;

    @Schema(description = "输出格式")
    private String outputFormat;

    @Schema(description = "Token解析规则")
    private Map<String, Object> tokenParsingRule;

    @Schema(description = "是否内置模板")
    private Boolean isBuiltin;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;
}