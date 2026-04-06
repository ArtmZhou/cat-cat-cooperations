package com.cat.standalone.store.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 存储的CLI Agent模板实体
 */
@Data
public class StoredCliAgentTemplate {
    private String id;
    private String name;
    private String cliType;
    private String description;
    private String executablePath;
    private String defaultArgs;  // JSON数组
    private String requiredEnvVars;  // JSON数组
    private String optionalEnvVars;  // JSON数组
    private String configTemplate;  // JSON对象
    private String outputFormat;  // STREAM, STREAM_JSON, JSON
    private String tokenParsingRule;  // JSON对象
    private Boolean isBuiltin;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}