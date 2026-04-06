package com.cat.cliagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * CLI Agent能力DTO
 */
@Data
@Schema(description = "CLI Agent能力")
public class CliAgentCapabilityDto {

    @Schema(description = "能力ID")
    private String id;

    @Schema(description = "能力类型", example = "CODE_GEN")
    private String type;

    @Schema(description = "能力类型名称")
    private String typeName;

    @Schema(description = "领域标签", example = "[\"java\", \"spring\", \"web\"]")
    private List<String> domainTags;

    @Schema(description = "熟练度等级 1-5", example = "4")
    private Integer proficiencyLevel;

    @Schema(description = "熟练度描述")
    private String proficiencyDescription;

    /**
     * 能力类型枚举
     */
    public enum CapabilityType {
        CODE_GEN("代码生成"),
        CODE_REVIEW("代码审查"),
        CODE_REFACTOR("代码重构"),
        DEBUG("调试"),
        TEST("测试"),
        DOC("文档"),
        ANALYSIS("分析"),
        DESIGN("设计"),
        DEPLOY("部署"),
        OTHER("其他");

        private final String description;

        CapabilityType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 熟练度等级描述
     */
    public static String getProficiencyDescription(Integer level) {
        if (level == null || level < 1 || level > 5) {
            return "未知";
        }
        return switch (level) {
            case 1 -> "初学者";
            case 2 -> "入门";
            case 3 -> "熟练";
            case 4 -> "精通";
            case 5 -> "专家";
            default -> "未知";
        };
    }
}