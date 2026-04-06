package com.cat.standalone.store;

import com.cat.standalone.store.entity.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 存储配置
 *
 * 创建各实体的JSON文件存储实例
 */
@Configuration
public class StoreConfig {

    @Value("${cat.data-dir:./data}")
    private String dataDir;

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    @Bean
    public JsonFileStore<StoredAgent> agentStore() {
        return new JsonFileStore<>(dataDir, "agents", StoredAgent.class);
    }

    @Bean
    public JsonFileStore<StoredTask> taskStore() {
        return new JsonFileStore<>(dataDir, "tasks", StoredTask.class);
    }

    @Bean
    public JsonFileStore<StoredAgentCapability> capabilityStore() {
        return new JsonFileStore<>(dataDir, "capabilities", StoredAgentCapability.class);
    }

    @Bean
    public JsonFileStore<StoredTaskAssignment> assignmentStore() {
        return new JsonFileStore<>(dataDir, "assignments", StoredTaskAssignment.class);
    }

    @Bean
    public JsonFileStore<StoredTaskLog> taskLogStore() {
        return new JsonFileStore<>(dataDir, "task_logs", StoredTaskLog.class);
    }

    @Bean
    public JsonFileStore<StoredCliAgentTemplate> cliAgentTemplateStore() {
        return new JsonFileStore<>(dataDir, "cli_agent_templates", StoredCliAgentTemplate.class);
    }

    @Bean
    public JsonFileStore<StoredCliAgent> cliAgentStore() {
        return new JsonFileStore<>(dataDir, "cli_agents", StoredCliAgent.class);
    }

    @Bean
    public JsonFileStore<StoredCliAgentCapability> cliAgentCapabilityStore() {
        return new JsonFileStore<>(dataDir, "cli_agent_capabilities", StoredCliAgentCapability.class);
    }

    @Bean
    public JsonFileStore<StoredTokenUsageLog> tokenUsageLogStore() {
        return new JsonFileStore<>(dataDir, "token_usage_logs", StoredTokenUsageLog.class);
    }

    @Bean
    public JsonFileStore<StoredCliAgentOutputLog> cliAgentOutputLogStore() {
        return new JsonFileStore<>(dataDir, "cli_agent_output_logs", StoredCliAgentOutputLog.class);
    }
}