package com.cat.cliagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 更新CLI Agent实例请求
 */
@Data
@Schema(description = "更新CLI Agent实例请求")
public class UpdateCliAgentRequest {

    @Schema(description = "Agent名称")
    private String name;

    @Schema(description = "Agent描述")
    private String description;

    @Schema(description = "CLI可执行文件路径")
    private String executablePath;

    @Schema(description = "启动参数")
    private List<String> args;

    @Schema(description = "环境变量")
    private Map<String, String> envVars;

    @Schema(description = "配置文件路径")
    private String configPath;

    @Schema(description = "工作目录")
    private String workingDir;

    @Schema(description = "能力配置")
    private List<CreateCliAgentRequest.CapabilityDto> capabilities;
}