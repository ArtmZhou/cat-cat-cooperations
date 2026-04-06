package com.cat.standalone.service;

import com.cat.cliagent.dto.CreateCliAgentTemplateRequest;
import com.cat.cliagent.dto.CliAgentTemplateResponse;
import com.cat.cliagent.service.CliAgentTemplateService;
import com.cat.common.exception.BusinessException;
import com.cat.standalone.store.JsonFileStore;
import com.cat.standalone.store.entity.StoredCliAgentTemplate;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * CLI Agent模板服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalCliAgentTemplateService implements CliAgentTemplateService {

    private final JsonFileStore<StoredCliAgentTemplate> templateStore;
    private final ObjectMapper objectMapper;

    @PostConstruct
    @Override
    public void initBuiltInTemplates() {
        // 检查是否已初始化
        if (templateStore.count() > 0) {
            log.debug("Templates already initialized, skipping...");
            return;
        }

        log.info("Initializing built-in CLI Agent templates...");

        // Claude Code 模板
        StoredCliAgentTemplate claudeTemplate = new StoredCliAgentTemplate();
        claudeTemplate.setId("claude-code");
        claudeTemplate.setName("Claude Code");
        claudeTemplate.setCliType("claude");
        claudeTemplate.setDescription("Anthropic Claude CLI工具，支持代码生成、文件操作等");
        claudeTemplate.setExecutablePath("claude");
        claudeTemplate.setDefaultArgs(toJson(List.of("--output-format", "stream-json")));
        claudeTemplate.setRequiredEnvVars(toJson(List.of("ANTHROPIC_API_KEY")));
        claudeTemplate.setOptionalEnvVars(toJson(List.of("CLAUDE_MODEL", "CLAUDE_MAX_TOKENS")));
        claudeTemplate.setOutputFormat("STREAM_JSON");
        claudeTemplate.setTokenParsingRule(toJson(Map.of(
            "type", "json_field",
            "field", "$.usage",
            "inputTokensField", "input_tokens",
            "outputTokensField", "output_tokens"
        )));
        claudeTemplate.setIsBuiltin(true);
        claudeTemplate.setCreatedAt(LocalDateTime.now());
        claudeTemplate.setUpdatedAt(LocalDateTime.now());
        templateStore.save("claude-code", claudeTemplate);

        // OpenCode 模板
        StoredCliAgentTemplate openCodeTemplate = new StoredCliAgentTemplate();
        openCodeTemplate.setId("opencode");
        openCodeTemplate.setName("OpenCode");
        openCodeTemplate.setCliType("opencode");
        openCodeTemplate.setDescription("开源代码助手CLI，支持多种AI模型");
        openCodeTemplate.setExecutablePath("opencode");
        openCodeTemplate.setDefaultArgs(toJson(List.of("--stream")));
        openCodeTemplate.setRequiredEnvVars(toJson(List.of("OPENAI_API_KEY")));
        openCodeTemplate.setOptionalEnvVars(toJson(List.of("OPENAI_MODEL", "OPENAI_BASE_URL")));
        openCodeTemplate.setOutputFormat("STREAM");
        openCodeTemplate.setTokenParsingRule(toJson(Map.of(
            "type", "regex",
            "pattern", "tokens:\\s*(\\d+)/(\\d+)"
        )));
        openCodeTemplate.setIsBuiltin(true);
        openCodeTemplate.setCreatedAt(LocalDateTime.now());
        openCodeTemplate.setUpdatedAt(LocalDateTime.now());
        templateStore.save("opencode", openCodeTemplate);

        log.info("Built-in templates initialized: claude-code, opencode");
    }

    @Override
    public List<CliAgentTemplateResponse> getBuiltInTemplates() {
        return templateStore.find(t -> Boolean.TRUE.equals(t.getIsBuiltin())).stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    public List<CliAgentTemplateResponse> getAllTemplates() {
        return templateStore.findAll().stream()
            .map(this::toResponse)
            .toList();
    }

    @Override
    public CliAgentTemplateResponse getTemplate(String templateId) {
        StoredCliAgentTemplate template = templateStore.findById(templateId)
            .orElseThrow(() -> new BusinessException(404, "模板不存在: " + templateId));
        return toResponse(template);
    }

    @Override
    public CliAgentTemplateResponse createTemplate(CreateCliAgentTemplateRequest request) {
        // 检查cliType是否已存在
        Optional<StoredCliAgentTemplate> existing = templateStore.findFirst(
            t -> t.getCliType().equals(request.getCliType())
        );
        if (existing.isPresent()) {
            throw new BusinessException(400, "CLI类型已存在: " + request.getCliType());
        }

        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        StoredCliAgentTemplate template = new StoredCliAgentTemplate();
        template.setId(id);
        template.setName(request.getName());
        template.setCliType(request.getCliType());
        template.setDescription(request.getDescription());
        template.setExecutablePath(request.getExecutablePath());
        template.setDefaultArgs(toJson(request.getDefaultArgs()));
        template.setRequiredEnvVars(toJson(request.getRequiredEnvVars()));
        template.setOptionalEnvVars(toJson(request.getOptionalEnvVars()));
        template.setConfigTemplate(toJson(request.getConfigTemplate()));
        template.setOutputFormat(request.getOutputFormat());
        template.setTokenParsingRule(toJson(request.getTokenParsingRule()));
        template.setIsBuiltin(false);
        template.setCreatedAt(now);
        template.setUpdatedAt(now);

        templateStore.save(id, template);

        log.info("CLI Agent template created: {} ({})", template.getName(), template.getCliType());
        return toResponse(template);
    }

    @Override
    public CliAgentTemplateResponse updateTemplate(String templateId, CreateCliAgentTemplateRequest request) {
        StoredCliAgentTemplate template = templateStore.findById(templateId)
            .orElseThrow(() -> new BusinessException(404, "模板不存在: " + templateId));

        if (Boolean.TRUE.equals(template.getIsBuiltin())) {
            throw new BusinessException(400, "内置模板不允许修改");
        }

        if (request.getName() != null) {
            template.setName(request.getName());
        }
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        if (request.getExecutablePath() != null) {
            template.setExecutablePath(request.getExecutablePath());
        }
        if (request.getDefaultArgs() != null) {
            template.setDefaultArgs(toJson(request.getDefaultArgs()));
        }
        if (request.getRequiredEnvVars() != null) {
            template.setRequiredEnvVars(toJson(request.getRequiredEnvVars()));
        }
        if (request.getOptionalEnvVars() != null) {
            template.setOptionalEnvVars(toJson(request.getOptionalEnvVars()));
        }
        if (request.getConfigTemplate() != null) {
            template.setConfigTemplate(toJson(request.getConfigTemplate()));
        }
        if (request.getOutputFormat() != null) {
            template.setOutputFormat(request.getOutputFormat());
        }
        if (request.getTokenParsingRule() != null) {
            template.setTokenParsingRule(toJson(request.getTokenParsingRule()));
        }
        template.setUpdatedAt(LocalDateTime.now());

        templateStore.save(templateId, template);

        log.info("CLI Agent template updated: {}", templateId);
        return toResponse(template);
    }

    @Override
    public void deleteTemplate(String templateId) {
        StoredCliAgentTemplate template = templateStore.findById(templateId)
            .orElseThrow(() -> new BusinessException(404, "模板不存在: " + templateId));

        if (Boolean.TRUE.equals(template.getIsBuiltin())) {
            throw new BusinessException(400, "内置模板不允许删除");
        }

        templateStore.deleteById(templateId);
        log.info("CLI Agent template deleted: {}", templateId);
    }

    private CliAgentTemplateResponse toResponse(StoredCliAgentTemplate template) {
        CliAgentTemplateResponse response = new CliAgentTemplateResponse();
        response.setId(template.getId());
        response.setName(template.getName());
        response.setCliType(template.getCliType());
        response.setDescription(template.getDescription());
        response.setExecutablePath(template.getExecutablePath());
        response.setDefaultArgs(parseJsonList(template.getDefaultArgs()));
        response.setRequiredEnvVars(parseJsonList(template.getRequiredEnvVars()));
        response.setOptionalEnvVars(parseJsonList(template.getOptionalEnvVars()));
        response.setConfigTemplate(parseJsonMap(template.getConfigTemplate()));
        response.setOutputFormat(template.getOutputFormat());
        response.setTokenParsingRule(parseJsonMap(template.getTokenParsingRule()));
        response.setIsBuiltin(template.getIsBuiltin());
        response.setCreatedAt(template.getCreatedAt());
        response.setUpdatedAt(template.getUpdatedAt());
        return response;
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON", e);
            return null;
        }
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON list: {}", json, e);
            return null;
        }
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON map: {}", json, e);
            return null;
        }
    }
}