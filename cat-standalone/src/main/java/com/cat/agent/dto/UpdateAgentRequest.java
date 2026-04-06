package com.cat.agent.dto;

import lombok.Data;

/**
 * 更新Agent请求
 */
@Data
public class UpdateAgentRequest {
    private String name;
    private String description;
    private String config;
}