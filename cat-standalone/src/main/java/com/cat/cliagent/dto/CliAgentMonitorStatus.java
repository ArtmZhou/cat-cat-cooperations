package com.cat.cliagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * CLI Agent监控状态DTO
 */
@Data
@Schema(description = "CLI Agent监控状态")
public class CliAgentMonitorStatus {

    @Schema(description = "Agent ID")
    private String agentId;

    @Schema(description = "Agent名称")
    private String name;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "进程状态")
    private ProcessStatus processStatus;

    @Schema(description = "会话状态")
    private SessionStatus sessionStatus;

    @Schema(description = "Token使用统计")
    private TokenStatus tokenStatus;

    @Schema(description = "任务执行状态")
    private TaskStatus taskStatus;

    @Schema(description = "最后更新时间")
    private LocalDateTime updatedAt;

    @Data
    @Schema(description = "进程状态")
    public static class ProcessStatus {
        private String processMode;
        private Long uptimeMs;
        private boolean alive;
        private String lastError;
    }

    @Data
    @Schema(description = "会话状态")
    public static class SessionStatus {
        private boolean active;
        private boolean inputStreamOpen;
        private boolean outputStreamOpen;
        private Long linesReceived;
        private Long bytesSent;
    }

    @Data
    @Schema(description = "Token使用状态")
    public static class TokenStatus {
        private Long totalInputTokens;
        private Long totalOutputTokens;
        private Long totalTokens;
        private Long recordCount;
    }

    @Data
    @Schema(description = "任务执行状态")
    public static class TaskStatus {
        private int concurrentTasks;
        private boolean canAcceptTask;
    }
}