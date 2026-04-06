package com.cat.task.dto;

import lombok.Data;

/**
 * 更新Task请求
 */
@Data
public class UpdateTaskRequest {
    private String name;
    private String description;
    private Integer priority;
    private String config;
}