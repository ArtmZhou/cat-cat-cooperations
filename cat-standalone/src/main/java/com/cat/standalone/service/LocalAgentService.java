package com.cat.standalone.service;

import com.cat.standalone.store.JsonFileStore;
import com.cat.standalone.store.entity.*;
import com.cat.agent.dto.*;
import com.cat.agent.entity.AgentCapability;
import com.cat.agent.service.AgentService;
import com.cat.common.exception.BusinessException;
import com.cat.common.model.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 本地Agent服务实现
 *
 * 使用JSON文件存储替代MyBatis数据库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalAgentService implements AgentService {

    private final JsonFileStore<StoredAgent> agentStore;
    private final JsonFileStore<StoredAgentCapability> capabilityStore;

    @Override
    public AgentResponse createAgent(CreateAgentRequest request, String userId) {
        if (!isValidAgentType(request.getType())) {
            throw new BusinessException(400, "无效的Agent类型");
        }

        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        StoredAgent agent = new StoredAgent();
        agent.setId(id);
        agent.setName(request.getName());
        agent.setDescription(request.getDescription());
        agent.setType(request.getType());
        agent.setStatus("OFFLINE");
        agent.setConfig(request.getConfig());
        agent.setCreatedBy(userId);
        agent.setCreatedAt(now);
        agent.setUpdatedAt(now);

        agentStore.save(id, agent);

        // 创建能力
        if (request.getCapabilities() != null) {
            for (CreateAgentRequest.CapabilityDto capDto : request.getCapabilities()) {
                createCapability(id, capDto);
            }
        }

        log.info("Agent created: {} by user: {}", agent.getName(), userId);
        return buildAgentResponse(agent);
    }

    private void createCapability(String agentId, CreateAgentRequest.CapabilityDto capDto) {
        String capId = UUID.randomUUID().toString();
        StoredAgentCapability capability = new StoredAgentCapability();
        capability.setId(capId);
        capability.setAgentId(agentId);
        capability.setCapabilityType(capDto.getType());
        capability.setCapabilityName(capDto.getName());
        capability.setCapabilityConfig(capDto.getConfig());
        capability.setDescription(capDto.getDescription());
        capability.setCreatedAt(LocalDateTime.now());
        capabilityStore.save(capId, capability);
    }

    @Override
    public AgentResponse updateAgent(String agentId, UpdateAgentRequest request) {
        StoredAgent agent = agentStore.findById(agentId)
            .orElseThrow(() -> new BusinessException(404, "Agent不存在"));

        if (StringUtils.hasText(request.getName())) {
            agent.setName(request.getName());
        }
        if (request.getDescription() != null) {
            agent.setDescription(request.getDescription());
        }
        if (request.getConfig() != null) {
            agent.setConfig(request.getConfig());
        }
        agent.setUpdatedAt(LocalDateTime.now());

        agentStore.save(agentId, agent);
        log.info("Agent updated: {}", agentId);
        return buildAgentResponse(agent);
    }

    @Override
    public void deleteAgent(String agentId) {
        StoredAgent agent = agentStore.findById(agentId)
            .orElseThrow(() -> new BusinessException(404, "Agent不存在"));

        // 删除关联的能力
        capabilityStore.delete(cap -> cap.getAgentId().equals(agentId));

        agentStore.deleteById(agentId);
        log.info("Agent deleted: {}", agentId);
    }

    @Override
    public AgentResponse getAgent(String agentId) {
        StoredAgent agent = agentStore.findById(agentId)
            .orElseThrow(() -> new BusinessException(404, "Agent不存在"));
        return buildAgentResponse(agent);
    }

    @Override
    public PageResult<AgentResponse> listAgents(AgentQuery query) {
        JsonFileStore.PageResult<StoredAgent> page = agentStore.findPage(
            query.getPage() != null ? query.getPage() : 1,
            query.getPageSize() != null ? query.getPageSize() : 20,
            buildPredicate(query)
        );

        List<AgentResponse> items = page.getItems().stream()
            .map(this::buildAgentResponse)
            .collect(Collectors.toList());

        return new PageResult<>(items, page.getTotal(), page.getPage(), page.getPageSize(), page.getTotalPages());
    }

    private java.util.function.Predicate<StoredAgent> buildPredicate(AgentQuery query) {
        return agent -> {
            if (query == null) return true;
            if (StringUtils.hasText(query.getName()) && !agent.getName().contains(query.getName())) {
                return false;
            }
            if (StringUtils.hasText(query.getType()) && !agent.getType().equals(query.getType())) {
                return false;
            }
            if (StringUtils.hasText(query.getStatus()) && !agent.getStatus().equals(query.getStatus())) {
                return false;
            }
            return true;
        };
    }

    @Override
    public void enableAgent(String agentId) {
        updateAgentStatus(agentId, "ONLINE");
    }

    @Override
    public void disableAgent(String agentId) {
        updateAgentStatus(agentId, "DISABLED");
    }

    private void updateAgentStatus(String agentId, String status) {
        StoredAgent agent = agentStore.findById(agentId)
            .orElseThrow(() -> new BusinessException(404, "Agent不存在"));
        agent.setStatus(status);
        agent.setUpdatedAt(LocalDateTime.now());
        agentStore.save(agentId, agent);
        log.info("Agent {} status changed to {}", agentId, status);
    }

    @Override
    public List<AgentResponse> getAvailableAgents() {
        return agentStore.find(agent -> "ONLINE".equals(agent.getStatus())).stream()
            .map(this::buildAgentResponse)
            .collect(Collectors.toList());
    }

    @Override
    public String generateAccessKey(String agentId) {
        StoredAgent agent = agentStore.findById(agentId)
            .orElseThrow(() -> new BusinessException(404, "Agent不存在"));

        String accessKey = "AK_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        agent.setAccessKey(accessKey);
        agent.setUpdatedAt(LocalDateTime.now());
        agentStore.save(agentId, agent);

        log.info("Access key generated for agent: {}", agentId);
        return accessKey;
    }

    private AgentResponse buildAgentResponse(StoredAgent agent) {
        AgentResponse response = new AgentResponse();
        response.setId(agent.getId());
        response.setName(agent.getName());
        response.setDescription(agent.getDescription());
        response.setType(agent.getType());
        response.setStatus(agent.getStatus());
        response.setAccessKey(agent.getAccessKey());
        response.setConfig(agent.getConfig());
        response.setMetadata(agent.getMetadata());
        response.setLastHeartbeat(agent.getLastHeartbeat());
        response.setCreatedBy(agent.getCreatedBy());
        response.setCreatedAt(agent.getCreatedAt());
        response.setUpdatedAt(agent.getUpdatedAt());

        // 加载能力
        List<StoredAgentCapability> capabilities = capabilityStore.find(cap -> cap.getAgentId().equals(agent.getId()));
        List<AgentCapability> capList = capabilities.stream().map(this::toCapability).collect(Collectors.toList());
        response.setCapabilities(capList);

        return response;
    }

    private AgentCapability toCapability(StoredAgentCapability cap) {
        AgentCapability capability = new AgentCapability();
        capability.setId(cap.getId());
        capability.setAgentId(cap.getAgentId());
        capability.setCapabilityType(cap.getCapabilityType());
        capability.setCapabilityName(cap.getCapabilityName());
        capability.setCapabilityConfig(cap.getCapabilityConfig());
        capability.setDescription(cap.getDescription());
        return capability;
    }

    private boolean isValidAgentType(String type) {
        return "BUILT_IN".equals(type) || "EXTERNAL".equals(type);
    }
}