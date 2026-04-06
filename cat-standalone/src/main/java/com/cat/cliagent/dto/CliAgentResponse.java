package com.cat.cliagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * CLI Agent实例响应
 */
@Data
@Schema(description = "CLI Agent实例响应")
public class CliAgentResponse {

    @Schema(description = "Agent ID")
    private String id;

    @Schema(description = "Agent名称")
    private String name;

    @Schema(description = "Agent描述")
    private String description;

    @Schema(description = "模板ID")
    private String templateId;

    @Schema(description = "模板名称")
    private String templateName;

    @Schema(description = "CLI类型")
    private String cliType;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "CLI可执行文件路径")
    private String executablePath;

    @Schema(description = "启动参数")
    private List<String> args;

    @Schema(description = "环境变量（敏感信息脱敏）")
    private Map<String, String> envVarsMasked;

    @Schema(description = "配置文件路径")
    private String configPath;

    @Schema(description = "工作目录")
    private String workingDir;

    @Schema(description = "进程ID")
    private String processId;

    @Schema(description = "能力列表")
    private List<CapabilityResponse> capabilities;

    @Schema(description = "指标")
    private AgentMetrics metrics;

    @Schema(description = "上次启动时间")
    private LocalDateTime lastStartedAt;

    @Schema(description = "上次停止时间")
    private LocalDateTime lastStoppedAt;

    @Schema(description = "创建者")
    private String createdBy;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Data
    @Schema(description = "能力响应")
    public static class CapabilityResponse {
        private String id;
        private String type;
        private List<String> domainTags;
        private Integer proficiencyLevel;
    }

    @Data
    @Schema(description = "Agent指标")
    public static class AgentMetrics {
        private Integer tasksTotal;
        private Integer tasksSuccess;
        private Integer tasksFailed;
        private Double successRate;
        private Long totalTokensUsed;
    }
}