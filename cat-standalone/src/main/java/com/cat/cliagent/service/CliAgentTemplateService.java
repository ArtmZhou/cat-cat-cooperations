package com.cat.cliagent.service;

import com.cat.cliagent.dto.CreateCliAgentTemplateRequest;
import com.cat.cliagent.dto.CliAgentTemplateResponse;

import java.util.List;

/**
 * CLI Agent模板服务接口
 */
public interface CliAgentTemplateService {

    /**
     * 获取所有内置模板
     */
    List<CliAgentTemplateResponse> getBuiltInTemplates();

    /**
     * 获取所有模板（内置+自定义）
     */
    List<CliAgentTemplateResponse> getAllTemplates();

    /**
     * 获取模板详情
     */
    CliAgentTemplateResponse getTemplate(String templateId);

    /**
     * 创建自定义模板
     */
    CliAgentTemplateResponse createTemplate(CreateCliAgentTemplateRequest request);

    /**
     * 更新模板
     */
    CliAgentTemplateResponse updateTemplate(String templateId, CreateCliAgentTemplateRequest request);

    /**
     * 删除模板（仅限自定义模板）
     */
    void deleteTemplate(String templateId);

    /**
     * 初始化内置模板
     */
    void initBuiltInTemplates();
}