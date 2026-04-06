package com.cat.standalone.service;

import com.cat.cliagent.service.CliAgentTemplateService;
import com.cat.cliagent.service.TokenUsageService;
import com.cat.cliagent.dto.CliAgentTemplateResponse;
import com.cat.standalone.store.JsonFileStore;
import com.cat.standalone.store.entity.StoredTokenUsageLog;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Token使用统计服务实现
 *
 * 负责解析CLI输出中的Token使用信息并统计，数据持久化到JSON文件
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalTokenUsageService implements TokenUsageService {

    private final JsonFileStore<StoredTokenUsageLog> tokenUsageLogStore;
    private final CliAgentTemplateService templateService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean parseAndRecord(String agentId, String output, String templateId) {
        if (output == null || output.isEmpty()) {
            return false;
        }

        try {
            // 获取模板的Token解析规则
            CliAgentTemplateResponse template = templateService.getTemplate(templateId);
            if (template == null || template.getTokenParsingRule() == null) {
                log.debug("No token parsing rule for template: {}", templateId);
                return false;
            }

            Map<String, Object> rule = template.getTokenParsingRule();
            Long inputTokens = null;
            Long outputTokens = null;

            // 根据解析类型处理
            String type = (String) rule.get("type");
            if ("json_field".equals(type)) {
                // JSON字段解析
                String field = (String) rule.get("field");
                String inputField = (String) rule.get("inputTokensField");
                String outputField = (String) rule.get("outputTokensField");
                inputTokens = extractFromJson(output, field, inputField);
                outputTokens = extractFromJson(output, field, outputField);
            } else if ("regex".equals(type)) {
                // 正则表达式解析
                String patternStr = (String) rule.get("pattern");
                Pattern pattern = Pattern.compile(patternStr);
                Matcher matcher = pattern.matcher(output);
                if (matcher.find() && matcher.groupCount() >= 2) {
                    inputTokens = parseLong(matcher.group(1));
                    outputTokens = parseLong(matcher.group(2));
                }
            }

            if (inputTokens != null || outputTokens != null) {
                // 创建记录并持久化
                String id = UUID.randomUUID().toString();
                StoredTokenUsageLog usageLog = new StoredTokenUsageLog();
                usageLog.setId(id);
                usageLog.setAgentId(agentId);
                usageLog.setInputTokens(inputTokens != null ? inputTokens : 0L);
                usageLog.setOutputTokens(outputTokens != null ? outputTokens : 0L);
                usageLog.setSource("cli_output");
                usageLog.setRecordedAt(LocalDateTime.now());

                tokenUsageLogStore.save(id, usageLog);

                log.info("Token usage recorded for agent {}: input={}, output={}",
                    agentId, inputTokens, outputTokens);
                return true;
            }

        } catch (Exception e) {
            log.error("Failed to parse token usage for agent: {}", agentId, e);
        }

        return false;
    }

    private Long extractFromJson(String output, String field, String tokenField) {
        if (field == null || tokenField == null) {
            return null;
        }

        try {
            // 尝试解析JSON
            JsonNode root = objectMapper.readTree(output);

            // 导航到指定字段
            String[] parts = field.replace("$.", "").split("\\.");
            JsonNode current = root;
            for (String part : parts) {
                if (current.has(part)) {
                    current = current.get(part);
                } else {
                    return null;
                }
            }

            // 获取token字段值
            if (current.has(tokenField)) {
                return current.get(tokenField).asLong();
            }
        } catch (Exception e) {
            log.debug("Failed to extract JSON field: {}", e.getMessage());
        }

        return null;
    }

    private Long parseLong(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    public TokenUsageStats getAgentStats(String agentId) {
        List<StoredTokenUsageLog> records = tokenUsageLogStore.find(
            r -> r.getAgentId().equals(agentId)
        );

        if (records.isEmpty()) {
            return new TokenUsageStats(agentId, 0L, 0L, 0L, 0L, null, null);
        }

        long totalInput = records.stream()
            .mapToLong(r -> r.getInputTokens() != null ? r.getInputTokens() : 0L)
            .sum();
        long totalOutput = records.stream()
            .mapToLong(r -> r.getOutputTokens() != null ? r.getOutputTokens() : 0L)
            .sum();

        LocalDateTime firstTime = records.stream()
            .map(StoredTokenUsageLog::getRecordedAt)
            .filter(Objects::nonNull)
            .min(LocalDateTime::compareTo)
            .orElse(null);
        LocalDateTime lastTime = records.stream()
            .map(StoredTokenUsageLog::getRecordedAt)
            .filter(Objects::nonNull)
            .max(LocalDateTime::compareTo)
            .orElse(null);

        return new TokenUsageStats(
            agentId,
            totalInput,
            totalOutput,
            totalInput + totalOutput,
            (long) records.size(),
            firstTime,
            lastTime
        );
    }

    @Override
    public List<TokenUsageRecord> getAgentRecords(String agentId, LocalDateTime startTime, LocalDateTime endTime) {
        return tokenUsageLogStore.find(r -> {
            if (!r.getAgentId().equals(agentId)) return false;
            if (startTime != null && r.getRecordedAt() != null &&
                r.getRecordedAt().isBefore(startTime)) return false;
            if (endTime != null && r.getRecordedAt() != null &&
                r.getRecordedAt().isAfter(endTime)) return false;
            return true;
        }).stream()
          .sorted(Comparator.comparing(StoredTokenUsageLog::getRecordedAt))
          .map(this::toRecord)
          .collect(Collectors.toList());
    }

    @Override
    public TokenUsageStats getSystemStats(LocalDateTime startTime, LocalDateTime endTime) {
        List<StoredTokenUsageLog> records = tokenUsageLogStore.find(r -> {
            if (startTime != null && r.getRecordedAt() != null &&
                r.getRecordedAt().isBefore(startTime)) return false;
            if (endTime != null && r.getRecordedAt() != null &&
                r.getRecordedAt().isAfter(endTime)) return false;
            return true;
        });

        if (records.isEmpty()) {
            return new TokenUsageStats("system", 0L, 0L, 0L, 0L, null, null);
        }

        long totalInput = records.stream()
            .mapToLong(r -> r.getInputTokens() != null ? r.getInputTokens() : 0L)
            .sum();
        long totalOutput = records.stream()
            .mapToLong(r -> r.getOutputTokens() != null ? r.getOutputTokens() : 0L)
            .sum();

        LocalDateTime firstTime = records.stream()
            .map(StoredTokenUsageLog::getRecordedAt)
            .filter(Objects::nonNull)
            .min(LocalDateTime::compareTo)
            .orElse(null);
        LocalDateTime lastTime = records.stream()
            .map(StoredTokenUsageLog::getRecordedAt)
            .filter(Objects::nonNull)
            .max(LocalDateTime::compareTo)
            .orElse(null);

        return new TokenUsageStats(
            "system",
            totalInput,
            totalOutput,
            totalInput + totalOutput,
            (long) records.size(),
            firstTime,
            lastTime
        );
    }

    @Override
    public boolean recordTokenUsage(String agentId, Long inputTokens, Long outputTokens) {
        if ((inputTokens == null || inputTokens == 0) && (outputTokens == null || outputTokens == 0)) {
            return false;
        }

        try {
            String id = UUID.randomUUID().toString();
            StoredTokenUsageLog usageLog = new StoredTokenUsageLog();
            usageLog.setId(id);
            usageLog.setAgentId(agentId);
            usageLog.setInputTokens(inputTokens != null ? inputTokens : 0L);
            usageLog.setOutputTokens(outputTokens != null ? outputTokens : 0L);
            usageLog.setSource("cli_result");
            usageLog.setRecordedAt(LocalDateTime.now());

            tokenUsageLogStore.save(id, usageLog);

            log.info("Token usage recorded for agent {}: input={}, output={}",
                agentId, inputTokens, outputTokens);
            return true;
        } catch (Exception e) {
            log.error("Failed to record token usage for agent: {}", agentId, e);
            return false;
        }
    }

    @Override
    public int cleanupOldRecords(LocalDateTime beforeTime) {
        List<StoredTokenUsageLog> toDelete = tokenUsageLogStore.find(
            r -> r.getRecordedAt() != null && r.getRecordedAt().isBefore(beforeTime)
        );

        for (StoredTokenUsageLog oldLog : toDelete) {
            tokenUsageLogStore.deleteById(oldLog.getId());
        }

        log.info("Cleaned {} old token usage records before {}", toDelete.size(), beforeTime);
        return toDelete.size();
    }

    /**
     * 将存储实体转换为接口record
     */
    private TokenUsageRecord toRecord(StoredTokenUsageLog usageLog) {
        return new TokenUsageRecord(
            usageLog.getId(),
            usageLog.getAgentId(),
            usageLog.getInputTokens(),
            usageLog.getOutputTokens(),
            usageLog.getSource(),
            usageLog.getRecordedAt()
        );
    }
}