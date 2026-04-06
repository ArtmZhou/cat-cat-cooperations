package com.cat.agent.dto;

import lombok.Data;

/**
 * Agent查询条件
 */
@Data
public class AgentQuery {
    private String name;
    private String type;
    private String status;
    private Integer page = 1;
    private Integer pageSize = 20;
}