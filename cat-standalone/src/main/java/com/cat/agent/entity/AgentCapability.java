package com.cat.agent.entity;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Agent能力实体
 */
@Data
public class AgentCapability {
    private String id;
    private String agentId;
    private String capabilityType;
    private String capabilityName;
    private String capabilityConfig;
    private String description;
    private LocalDateTime createdAt;
}