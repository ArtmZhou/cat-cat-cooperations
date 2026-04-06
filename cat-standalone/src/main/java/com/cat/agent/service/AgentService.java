package com.cat.agent.service;

import com.cat.agent.dto.*;
import com.cat.common.model.PageResult;
import java.util.List;

/**
 * Agent服务接口
 */
public interface AgentService {
    AgentResponse createAgent(CreateAgentRequest request, String userId);
    AgentResponse updateAgent(String agentId, UpdateAgentRequest request);
    void deleteAgent(String agentId);
    AgentResponse getAgent(String agentId);
    PageResult<AgentResponse> listAgents(AgentQuery query);
    void enableAgent(String agentId);
    void disableAgent(String agentId);
    List<AgentResponse> getAvailableAgents();
    String generateAccessKey(String agentId);
}