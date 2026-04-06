package com.cat.standalone.service;

import com.cat.cliagent.dto.CliAgentCapabilityDto;
import com.cat.cliagent.service.CliAgentCapabilityService;
import com.cat.common.exception.BusinessException;
import com.cat.standalone.store.JsonFileStore;
import com.cat.standalone.store.entity.StoredCliAgent;
import com.cat.standalone.store.entity.StoredCliAgentCapability;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CLI Agent能力管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalCliAgentCapabilityService implements CliAgentCapabilityService {

    private final JsonFileStore<StoredCliAgent> cliAgentStore;
    private final JsonFileStore<StoredCliAgentCapability> capabilityStore;

    @Override
    public List<CliAgentCapabilityDto> getAgentCapabilities(String agentId) {
        // 验证Agent存在
        cliAgentStore.findById(agentId)
            .orElseThrow(() -> new BusinessException(404, "CLI Agent不存在: " + agentId));

        // 查询能力
        List<StoredCliAgentCapability> capabilities = capabilityStore.findAll().stream()
            .filter(c -> agentId.equals(c.getAgentId()))
            .collect(Collectors.toList());

        return capabilities.stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Override
    public CliAgentCapabilityDto addCapability(String agentId, CliAgentCapabilityDto capability) {
        // 验证Agent存在
        cliAgentStore.findById(agentId)
            .orElseThrow(() -> new BusinessException(404, "CLI Agent不存在: " + agentId));

        // 创建能力实体
        StoredCliAgentCapability entity = new StoredCliAgentCapability();
        entity.setId(UUID.randomUUID().toString());
        entity.setAgentId(agentId);
        entity.setCapabilityType(capability.getType());
        entity.setDomainTags(capability.getDomainTags() != null ? new ArrayList<>(capability.getDomainTags()) : new ArrayList<>());
        entity.setProficiencyLevel(capability.getProficiencyLevel() != null ? capability.getProficiencyLevel() : 3);

        // 保存
        capabilityStore.save(entity.getId(), entity);

        log.info("Added capability {} to agent {}", capability.getType(), agentId);

        return toDto(entity);
    }

    @Override
    public CliAgentCapabilityDto updateCapability(String agentId, String capabilityId, CliAgentCapabilityDto capability) {
        // 验证Agent存在
        cliAgentStore.findById(agentId)
            .orElseThrow(() -> new BusinessException(404, "CLI Agent不存在: " + agentId));

        // 查找能力
        StoredCliAgentCapability entity = capabilityStore.findById(capabilityId)
            .orElseThrow(() -> new BusinessException(404, "能力不存在: " + capabilityId));

        // 验证能力属于该Agent
        if (!agentId.equals(entity.getAgentId())) {
            throw new BusinessException(400, "能力不属于该Agent");
        }

        // 更新
        if (capability.getType() != null) {
            entity.setCapabilityType(capability.getType());
        }
        if (capability.getDomainTags() != null) {
            entity.setDomainTags(new ArrayList<>(capability.getDomainTags()));
        }
        if (capability.getProficiencyLevel() != null) {
            entity.setProficiencyLevel(capability.getProficiencyLevel());
        }

        capabilityStore.save(capabilityId, entity);

        log.info("Updated capability {} for agent {}", capabilityId, agentId);

        return toDto(entity);
    }

    @Override
    public void removeCapability(String agentId, String capabilityId) {
        // 验证Agent存在
        cliAgentStore.findById(agentId)
            .orElseThrow(() -> new BusinessException(404, "CLI Agent不存在: " + agentId));

        // 查找能力
        StoredCliAgentCapability entity = capabilityStore.findById(capabilityId)
            .orElseThrow(() -> new BusinessException(404, "能力不存在: " + capabilityId));

        // 验证能力属于该Agent
        if (!agentId.equals(entity.getAgentId())) {
            throw new BusinessException(400, "能力不属于该Agent");
        }

        capabilityStore.deleteById(capabilityId);

        log.info("Removed capability {} from agent {}", capabilityId, agentId);
    }

    @Override
    public List<String> findAgentsByCapability(String capabilityType, String domainTag, Integer minProficiency) {
        return capabilityStore.findAll().stream()
            .filter(c -> capabilityType == null || capabilityType.equals(c.getCapabilityType()))
            .filter(c -> domainTag == null || (c.getDomainTags() != null && c.getDomainTags().contains(domainTag)))
            .filter(c -> minProficiency == null || c.getProficiencyLevel() >= minProficiency)
            .map(StoredCliAgentCapability::getAgentId)
            .distinct()
            .collect(Collectors.toList());
    }

    @Override
    public List<CapabilityTypeInfo> getAllCapabilityTypes() {
        return Arrays.stream(CliAgentCapabilityDto.CapabilityType.values())
            .map(type -> new CapabilityTypeInfo(
                type.name(),
                type.name(),
                type.getDescription()
            ))
            .collect(Collectors.toList());
    }

    private CliAgentCapabilityDto toDto(StoredCliAgentCapability entity) {
        CliAgentCapabilityDto dto = new CliAgentCapabilityDto();
        dto.setId(entity.getId());
        dto.setType(entity.getCapabilityType());
        dto.setTypeName(getTypeName(entity.getCapabilityType()));
        dto.setDomainTags(entity.getDomainTags());
        dto.setProficiencyLevel(entity.getProficiencyLevel());
        dto.setProficiencyDescription(CliAgentCapabilityDto.getProficiencyDescription(entity.getProficiencyLevel()));
        return dto;
    }

    private String getTypeName(String type) {
        try {
            return CliAgentCapabilityDto.CapabilityType.valueOf(type).getDescription();
        } catch (IllegalArgumentException e) {
            return type;
        }
    }
}