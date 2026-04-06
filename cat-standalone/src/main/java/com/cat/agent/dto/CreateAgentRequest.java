package com.cat.agent.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 创建Agent请求
 */
@Data
public class CreateAgentRequest {
    @NotBlank(message = "Agent名称不能为空")
    @Size(min = 2, max = 100, message = "Agent名称长度必须在2-100之间")
    private String name;

    @Size(max = 500, message = "描述长度不能超过500")
    private String description;

    @NotBlank(message = "Agent类型不能为空")
    private String type;

    private List<CapabilityDto> capabilities;
    private String config;

    @Data
    public static class CapabilityDto {
        private String type;
        private String name;
        private String config;
        private String description;
    }
}