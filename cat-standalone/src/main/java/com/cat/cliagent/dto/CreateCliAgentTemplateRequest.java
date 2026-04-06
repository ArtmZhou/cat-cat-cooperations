package com.cat.cliagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * CLI Agent模板创建请求
 */
@Data
@Schema(description = "CLI Agent模板创建请求")
public class CreateCliAgentTemplateRequest {

    @NotBlank(message = "模板名称不能为空")
    @Schema(description = "模板名称", example = "Claude Code")
    private String name;

    @NotBlank(message = "CLI类型不能为空")
    @Schema(description = "CLI类型标识", example = "claude")
    private String cliType;

    @Schema(description = "模板描述", example = "Anthropic Claude CLI工具")
    private String description;

    @Schema(description = "CLI可执行文件路径", example = "/usr/local/bin/claude")
    private String executablePath;

    @Schema(description = "默认启动参数")
    private List<String> defaultArgs;

    @Schema(description = "必需的环境变量列表", example = "[\"ANTHROPIC_API_KEY\"]")
    private List<String> requiredEnvVars;

    @Schema(description = "可选的环境变量列表", example = "[\"CLAUDE_MODEL\", \"CLAUDE_MAX_TOKENS\"]")
    private List<String> optionalEnvVars;

    @Schema(description = "配置文件模板")
    private Map<String, Object> configTemplate;

    @Schema(description = "输出格式: STREAM, STREAM_JSON, JSON", example = "STREAM_JSON")
    private String outputFormat;

    @Schema(description = "Token解析规则")
    private TokenParsingRuleDto tokenParsingRule;

    @Data
    @Schema(description = "Token解析规则")
    public static class TokenParsingRuleDto {
        @Schema(description = "解析类型: json_field, regex", example = "json_field")
        private String type;

        @Schema(description = "JSON字段路径", example = "$.usage")
        private String field;

        @Schema(description = "输入Token字段名", example = "input_tokens")
        private String inputTokensField;

        @Schema(description = "输出Token字段名", example = "output_tokens")
        private String outputTokensField;

        @Schema(description = "正则表达式模式")
        private String pattern;
    }
}