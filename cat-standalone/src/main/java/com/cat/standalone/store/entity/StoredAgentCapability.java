package com.cat.standalone.store.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 存储的Agent能力实体
 */
@Data
public class StoredAgentCapability {
    private String id;
    private String agentId;
    private String capabilityType;  // COMMAND, API_CALL, FILE, TEXT, MCP_SKILL
    private String capabilityName;
    private String capabilityConfig;
    private String description;
    private LocalDateTime createdAt;
}