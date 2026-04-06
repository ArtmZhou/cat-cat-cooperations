package com.cat.task.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

/**
 * 创建Task请求
 */
@Data
public class CreateTaskRequest {
    @NotBlank(message = "任务名称不能为空")
    private String name;

    private String description;

    @NotBlank(message = "任务类型不能为空")
    private String type;

    private Integer priority;
    private String input;
    private String config;
    private Integer timeoutSeconds;
    private Integer maxRetry;
    private String createdBy;
}