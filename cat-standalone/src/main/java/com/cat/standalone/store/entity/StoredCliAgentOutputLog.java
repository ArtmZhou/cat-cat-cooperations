package com.cat.standalone.store.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * CLI Agent输出日志存储实体
 */
@Data
public class StoredCliAgentOutputLog {
    private String id;
    private String agentId;
    private String type;         // "output", "text", "error"
    private String content;
    private LocalDateTime timestamp;
}