package com.cat.standalone.service;

import com.cat.cliagent.dto.*;
import com.cat.cliagent.service.CliAgentService;
import com.cat.cliagent.service.CliAgentTemplateService;
import com.cat.common.exception.BusinessException;
import com.cat.common.model.PageResult;
import com.cat.standalone.store.JsonFileStore;
import com.cat.standalone.store.entity.StoredCliAgent;
import com.cat.standalone.store.entity.StoredCliAgentCapability;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CLI Agent实例服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalCliAgentService implements CliAgentService {

    private final JsonFileStore<StoredCliAgent> cliAgentStore;
    private final JsonFileStore<StoredCliAgentCapability> cliAgentCapabilityStore;
    private final CliAgentTemplateService templateService;
    private final ObjectMapper objectMapper;

    // 简单加密密钥（生产环境应使用更安全的方案）
    private static final String ENCRYPTION_PREFIX = "ENC:";

    @Override
    public CliAgentResponse createAgent(CreateCliAgentRequest request, String userId) {
        // 验证模板存在
        CliAgentTemplateResponse template = templateService.getTemplate(request.getTemplateId());

        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        StoredCliAgent agent = new StoredCliAgent();
        agent.setId(id);
        agent.setName(request.getName());
        agent.setDescription(request.getDescription());
        agent.setTemplateId(request.getTemplateId());
        agent.setTemplateName(template.getName());
        agent.setCliType(template.getCliType());
        agent.setStatus("STOPPED");
        agent.setExecutablePath(
            StringUtils.hasText(request.getExecutablePath())
                ? request.getExecutablePath()
                : template.getExecutablePath()
        );
        agent.setArgs(toJson(mergeArgs(template.getDefaultArgs(), request.getArgs())));
        agent.setEnvVars(toJson(encryptEnvVars(request.getEnvVars())));
        agent.setConfigPath(request.getConfigPath());
        agent.setWorkingDir(request.getWorkingDir());
        agent.setCreatedBy(userId);
        agent.setCreatedAt(now);
        agent.setUpdatedAt(now);

        cliAgentStore.save(id, agent);

        // 创建能力
        if (request.getCapabilities() != null) {
            for (CreateCliAgentRequest.CapabilityDto capDto : request.getCapabilities()) {
                createCapability(id, capDto);
            }
        }

        log.info("CLI Agent created: {} (template: {})", agent.getName(), template.getName());
        return buildAgentResponse(agent);
    }

    private List<String> mergeArgs(List<String> templateArgs, List<String> requestArgs) {
        if (templateArgs == null && requestArgs == null) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        if (templateArgs != null) {
            result.addAll(templateArgs);
        }
        if (requestArgs != null) {
            result.addAll(requestArgs);
        }
        return result;
    }

    private Map<String, String> encryptEnvVars(Map<String, String> envVars) {
        if (envVars == null) {
            return null;
        }
        Map<String, String> encrypted = new HashMap<>();
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            // 简单加密：Base64编码（生产环境应使用AES）
            String value = entry.getValue();
            if (value != null && !value.startsWith(ENCRYPTION_PREFIX)) {
                encrypted.put(entry.getKey(), ENCRYPTION_PREFIX + Base64.getEncoder().encodeToString(value.getBytes()));
            } else {
                encrypted.put(entry.getKey(), value);
            }
        }
        return encrypted;
    }

    private Map<String, String> decryptEnvVars(Map<String, String> envVars) {
        if (envVars == null) {
            return null;
        }
        Map<String, String> decrypted = new HashMap<>();
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            String value = entry.getValue();
            if (value != null && value.startsWith(ENCRYPTION_PREFIX)) {
                try {
                    decrypted.put(entry.getKey(), new String(Base64.getDecoder().decode(value.substring(ENCRYPTION_PREFIX.length()))));
                } catch (Exception e) {
                    decrypted.put(entry.getKey(), value);
                }
            } else {
                decrypted.put(entry.getKey(), value);
            }
        }
        return decrypted;
    }

    private Map<String, String> maskEnvVars(Map<String, String> envVars) {
        if (envVars == null) {
            return null;
        }
        Map<String, String> masked = new HashMap<>();
        for (Map.Entry<String, String> entry : envVars.entrySet()) {
            String value = entry.getValue();
            if (value != null && value.startsWith(ENCRYPTION_PREFIX)) {
                // 脱敏显示
                masked.put(entry.getKey(), "******");
            } else {
                masked.put(entry.getKey(), value);
            }
        }
        return masked;
    }

    private void createCapability(String agentId, CreateCliAgentRequest.CapabilityDto capDto) {
        String capId = UUID.randomUUID().toString();
        StoredCliAgentCapability capability = new StoredCliAgentCapability();
        capability.setId(capId);
        capability.setAgentId(agentId);
        capability.setCapabilityType(capDto.getType());
        capability.setDomainTags(capDto.getDomainTags());
        capability.setProficiencyLevel(capDto.getProficiencyLevel());
        capability.setCreatedAt(LocalDateTime.now());
        cliAgentCapabilityStore.save(capId, capability);
    }

    @Override
    public CliAgentResponse updateAgent(String agentId, UpdateCliAgentRequest request) {
        StoredCliAgent agent = cliAgentStore.findById(agentId)
            .orElseThrow(() -> new BusinessException(404, "CLI Agent不存在: " + agentId));

        if (StringUtils.hasText(request.getName())) {
            agent.setName(request.getName());
        }
        if (request.getDescription() != null) {
            agent.setDescription(request.getDescription());
        }
        if (StringUtils.hasText(request.getExecutablePath())) {
            agent.setExecutablePath(request.getExecutablePath());
        }
        if (request.getArgs() != null) {
            agent.setArgs(toJson(request.getArgs()));
        }
        if (request.getEnvVars() != null) {
            agent.setEnvVars(toJson(encryptEnvVars(request.getEnvVars())));
        }
        if (request.getConfigPath() != null) {
            agent.setConfigPath(request.getConfigPath());
        }
        if (request.getWorkingDir() != null) {
            agent.setWorkingDir(request.getWorkingDir());
        }
        agent.setUpdatedAt(LocalDateTime.now());

        // 更新能力
        if (request.getCapabilities() != null) {
            cliAgentCapabilityStore.delete(cap -> cap.getAgentId().equals(agentId));
            for (CreateCliAgentRequest.CapabilityDto capDto : request.getCapabilities()) {
                createCapability(agentId, capDto);
            }
        }

        cliAgentStore.save(agentId, agent);

        log.info("CLI Agent updated: {}", agentId);
        return buildAgentResponse(agent);
    }

    @Override
    public void deleteAgent(String agentId) {
        StoredCliAgent agent = cliAgentStore.findById(agentId)
            .orElseThrow(() -> new BusinessException(404, "CLI Agent不存在: " + agentId));

        // 检查状态，不允许删除运行中的Agent
        if ("RUNNING".equals(agent.getStatus()) || "EXECUTING".equals(agent.getStatus())) {
            throw new BusinessException(400, "无法删除运行中的Agent，请先停止Agent");
        }

        // 删除关联的能力
        cliAgentCapabilityStore.delete(cap -> cap.getAgentId().equals(agentId));

        cliAgentStore.deleteById(agentId);
        log.info("CLI Agent deleted: {}", agentId);
    }

    @Override
    public CliAgentResponse getAgent(String agentId) {
        StoredCliAgent agent = cliAgentStore.findById(agentId)
            .orElseThrow(() -> new BusinessException(404, "CLI Agent不存在: " + agentId));
        return buildAgentResponse(agent);
    }

    @Override
    public PageResult<CliAgentResponse> listAgents(CliAgentQuery query) {
        JsonFileStore.PageResult<StoredCliAgent> page = cliAgentStore.findPage(
            query.getPage() != null ? query.getPage() : 1,
            query.getPageSize() != null ? query.getPageSize() : 20,
            buildPredicate(query)
        );

        List<CliAgentResponse> items = page.getItems().stream()
            .map(this::buildAgentResponse)
            .collect(Collectors.toList());

        return new PageResult<>(items, page.getTotal(), page.getPage(), page.getPageSize(), page.getTotalPages());
    }

    private java.util.function.Predicate<StoredCliAgent> buildPredicate(CliAgentQuery query) {
        return agent -> {
            if (query == null) return true;
            if (StringUtils.hasText(query.getName()) && !agent.getName().contains(query.getName())) {
                return false;
            }
            if (StringUtils.hasText(query.getTemplateId()) && !agent.getTemplateId().equals(query.getTemplateId())) {
                return false;
            }
            if (StringUtils.hasText(query.getCliType()) && !agent.getCliType().equals(query.getCliType())) {
                return false;
            }
            if (StringUtils.hasText(query.getStatus()) && !agent.getStatus().equals(query.getStatus())) {
                return false;
            }
            return true;
        };
    }

    @Override
    public List<CliAgentResponse> getAvailableAgents() {
        return cliAgentStore.find(agent -> "RUNNING".equals(agent.getStatus())).stream()
            .map(this::buildAgentResponse)
            .collect(Collectors.toList());
    }

    private CliAgentResponse buildAgentResponse(StoredCliAgent agent) {
        CliAgentResponse response = new CliAgentResponse();
        response.setId(agent.getId());
        response.setName(agent.getName());
        response.setDescription(agent.getDescription());
        response.setTemplateId(agent.getTemplateId());
        response.setTemplateName(agent.getTemplateName());
        response.setCliType(agent.getCliType());
        response.setStatus(agent.getStatus());
        response.setExecutablePath(agent.getExecutablePath());
        response.setArgs(parseJsonList(agent.getArgs()));
        response.setEnvVarsMasked(maskEnvVars(parseJsonMap(agent.getEnvVars())));
        response.setConfigPath(agent.getConfigPath());
        response.setWorkingDir(agent.getWorkingDir());
        response.setProcessId(agent.getProcessId());
        response.setLastStartedAt(agent.getLastStartedAt());
        response.setLastStoppedAt(agent.getLastStoppedAt());
        response.setCreatedBy(agent.getCreatedBy());
        response.setCreatedAt(agent.getCreatedAt());
        response.setUpdatedAt(agent.getUpdatedAt());

        // 加载能力
        List<StoredCliAgentCapability> capabilities = cliAgentCapabilityStore.find(
            cap -> cap.getAgentId().equals(agent.getId())
        );
        List<CliAgentResponse.CapabilityResponse> capList = capabilities.stream()
            .map(this::toCapabilityResponse)
            .collect(Collectors.toList());
        response.setCapabilities(capList);

        return response;
    }

    private CliAgentResponse.CapabilityResponse toCapabilityResponse(StoredCliAgentCapability cap) {
        CliAgentResponse.CapabilityResponse response = new CliAgentResponse.CapabilityResponse();
        response.setId(cap.getId());
        response.setType(cap.getCapabilityType());
        response.setDomainTags(cap.getDomainTags());
        response.setProficiencyLevel(cap.getProficiencyLevel());
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

    private Map<String, String> parseJsonMap(String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON map: {}", json, e);
            return null;
        }
    }
}