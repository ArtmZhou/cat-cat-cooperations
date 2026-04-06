package com.cat.cliagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CLI任务执行响应
 */
@Data
@Schema(description = "CLI任务执行响应")
public class CliTaskExecuteResponse {

    @Schema(description = "任务ID")
    private String taskId;

    @Schema(description = "Agent ID")
    private String agentId;

    @Schema(description = "执行状态")
    private String status;

    @Schema(description = "输出内容")
    private String output;

    @Schema(description = "错误信息")
    private String error;

    @Schema(description = "执行时间（毫秒）")
    private Long executionTimeMs;

    @Schema(description = "Token使用情况")
    private TokenUsage tokenUsage;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "完成时间")
    private LocalDateTime completedAt;

    @Data
    @Schema(description = "Token使用情况")
    public static class TokenUsage {
        private Long inputTokens;
        private Long outputTokens;
        private Long totalTokens;
    }

    public static CliTaskExecuteResponse fromResult(
            com.cat.cliagent.service.CliTaskExecutionService.TaskExecutionResult result) {
        CliTaskExecuteResponse response = new CliTaskExecuteResponse();
        response.setTaskId(result.taskId());
        response.setAgentId(result.agentId());
        response.setStatus(result.success() ? "COMPLETED" : "FAILED");
        response.setOutput(result.output());
        response.setError(result.error());
        response.setExecutionTimeMs(result.executionTimeMs());
        response.setCompletedAt(LocalDateTime.now());
        return response;
    }
}