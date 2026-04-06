package com.cat.standalone.store.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Token使用日志存储实体
 */
@Data
public class StoredTokenUsageLog {
    private String id;
    private String agentId;
    private Long inputTokens;
    private Long outputTokens;
    private String source;       // "cli_output" 或 "cli_result"
    private LocalDateTime recordedAt;
}