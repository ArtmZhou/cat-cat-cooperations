package com.cat.cliagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * CLI Agent查询参数
 */
@Data
@Schema(description = "CLI Agent查询参数")
public class CliAgentQuery {

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页大小", example = "20")
    private Integer pageSize = 20;

    @Schema(description = "名称关键字")
    private String name;

    @Schema(description = "模板ID")
    private String templateId;

    @Schema(description = "CLI类型")
    private String cliType;

    @Schema(description = "状态")
    private String status;
}