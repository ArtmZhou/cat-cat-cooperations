package com.cat.agent.dto;

import com.cat.agent.entity.AgentCapability;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Agent响应DTO
 */
@Data
public class AgentResponse {
    private String id;
    private String name;
    private String description;
    private String type;
    private String status;
    private String accessKey;
    private String config;
    private String metadata;
    private LocalDateTime lastHeartbeat;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AgentCapability> capabilities;
}