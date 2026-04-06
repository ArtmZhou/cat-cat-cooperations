package com.cat.cliagent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * CLI任务执行请求
 */
@Data
@Schema(description = "CLI任务执行请求")
public class CliTaskExecuteRequest {

    @Schema(description = "Agent ID")
    private String agentId;

    @Schema(description = "输入类型", example = "PROMPT")
    private InputType inputType;

    @Schema(description = "输入内容（Prompt文本）")
    private String prompt;

    @Schema(description = "JSON输入数据")
    private Map<String, Object> jsonData;

    @Schema(description = "文件路径列表")
    private List<String> filePaths;

    @Schema(description = "超时秒数", example = "300")
    private Integer timeoutSeconds;

    @Schema(description = "是否异步执行")
    private Boolean async;

    @Schema(description = "回调URL（异步执行时）")
    private String callbackUrl;

    /**
     * 输入类型枚举
     */
    public enum InputType {
        PROMPT,      // 纯文本Prompt
        JSON,        // JSON数据
        FILES,       // 文件路径
        MIXED        // 混合输入
    }

    /**
     * 获取实际输入内容
     */
    public String getActualInput() {
        if (inputType == null) {
            inputType = InputType.PROMPT;
        }

        return switch (inputType) {
            case PROMPT -> prompt;
            case JSON -> jsonData != null ? jsonData.toString() : null;
            case FILES -> filePaths != null ? String.join("\n", filePaths) : null;
            case MIXED -> buildMixedInput();
        };
    }

    private String buildMixedInput() {
        StringBuilder sb = new StringBuilder();

        if (prompt != null && !prompt.isEmpty()) {
            sb.append(prompt);
        }

        if (jsonData != null && !jsonData.isEmpty()) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append("JSON Data:\n").append(jsonData.toString());
        }

        if (filePaths != null && !filePaths.isEmpty()) {
            if (sb.length() > 0) sb.append("\n\n");
            sb.append("Files:\n").append(String.join("\n", filePaths));
        }

        return sb.toString();
    }
}