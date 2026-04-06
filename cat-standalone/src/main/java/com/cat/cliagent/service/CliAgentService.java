package com.cat.cliagent.service;

import com.cat.cliagent.dto.*;
import com.cat.common.model.PageResult;

/**
 * CLI Agent实例服务接口
 */
public interface CliAgentService {

    /**
     * 创建CLI Agent实例
     */
    CliAgentResponse createAgent(CreateCliAgentRequest request, String userId);

    /**
     * 更新CLI Agent实例
     */
    CliAgentResponse updateAgent(String agentId, UpdateCliAgentRequest request);

    /**
     * 删除CLI Agent实例
     */
    void deleteAgent(String agentId);

    /**
     * 获取CLI Agent详情
     */
    CliAgentResponse getAgent(String agentId);

    /**
     * 查询CLI Agent列表
     */
    PageResult<CliAgentResponse> listAgents(CliAgentQuery query);

    /**
     * 获取可用Agent列表
     */
    java.util.List<CliAgentResponse> getAvailableAgents();
}