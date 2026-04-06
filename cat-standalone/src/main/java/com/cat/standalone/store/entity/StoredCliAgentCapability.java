package com.cat.standalone.store.entity;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 存储的CLI Agent能力实体
 */
@Data
public class StoredCliAgentCapability {
    private String id;
    private String agentId;
    private String capabilityType;
    private List<String> domainTags;
    private Integer proficiencyLevel;
    private LocalDateTime createdAt;
}