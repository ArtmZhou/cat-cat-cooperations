package com.cat.cliagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 创建CLI Agent实例请求
 */
@Data
@Schema(description = "创建CLI Agent实例请求")
public class CreateCliAgentRequest {

    @NotBlank(message = "Agent名称不能为空")
    @Schema(description = "Agent名称", example = "Claude Code Agent")
    private String name;

    @Schema(description = "Agent描述", example = "主要处理前端代码生成任务")
    private String description;

    @NotBlank(message = "模板ID不能为空")
    @Schema(description = "模板ID", example = "claude-code")
    private String templateId;

    @Schema(description = "CLI可执行文件路径（可选，默认使用模板路径）", example = "/usr/local/bin/claude")
    private String executablePath;

    @Schema(description = "启动参数（追加到模板默认参数）")
    private List<String> args;

    @Schema(description = "环境变量")
    private Map<String, String> envVars;

    @Schema(description = "配置文件路径")
    private String configPath;

    @Schema(description = "工作目录")
    private String workingDir;

    @Schema(description = "能力配置")
    private List<CapabilityDto> capabilities;

    @Data
    @Schema(description = "能力配置")
    public static class CapabilityDto {
        @Schema(description = "能力类型", example = "CODE_GEN")
        private String type;

        @Schema(description = "领域标签", example = "[\"frontend\", \"vue\"]")
        private List<String> domainTags;

        @Schema(description = "熟练度1-5", example = "4")
        private Integer proficiencyLevel;
    }
}