package com.cat.cliagent.service;

import com.cat.cliagent.dto.CliAgentCapabilityDto;

import java.util.List;

/**
 * CLI Agent能力管理服务接口
 */
public interface CliAgentCapabilityService {

    /**
     * 获取Agent的所有能力
     *
     * @param agentId Agent ID
     * @return 能力列表
     */
    List<CliAgentCapabilityDto> getAgentCapabilities(String agentId);

    /**
     * 添加能力到Agent
     *
     * @param agentId Agent ID
     * @param capability 能力信息
     * @return 添加后的能力
     */
    CliAgentCapabilityDto addCapability(String agentId, CliAgentCapabilityDto capability);

    /**
     * 更新Agent的能力
     *
     * @param agentId Agent ID
     * @param capabilityId 能力ID
     * @param capability 更新的能力信息
     * @return 更新后的能力
     */
    CliAgentCapabilityDto updateCapability(String agentId, String capabilityId, CliAgentCapabilityDto capability);

    /**
     * 移除Agent的能力
     *
     * @param agentId Agent ID
     * @param capabilityId 能力ID
     */
    void removeCapability(String agentId, String capabilityId);

    /**
     * 根据能力类型查找Agent
     *
     * @param capabilityType 能力类型
     * @param domainTag 领域标签（可选）
     * @param minProficiency 最低熟练度（可选）
     * @return 匹配的Agent ID列表
     */
    List<String> findAgentsByCapability(String capabilityType, String domainTag, Integer minProficiency);

    /**
     * 获取所有能力类型
     *
     * @return 能力类型列表
     */
    List<CapabilityTypeInfo> getAllCapabilityTypes();

    /**
     * 能力类型信息
     */
    record CapabilityTypeInfo(
        String code,
        String name,
        String description
    ) {}
}