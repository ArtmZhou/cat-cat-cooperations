package com.cat.config;

import com.cat.chatgroup.entity.StoredChatGroup;
import com.cat.chatgroup.entity.StoredChatGroupMessage;
import com.cat.store.JsonFileStore;
import com.cat.store.entity.StoredCliAgent;
import com.cat.store.entity.StoredCliAgentCapability;
import com.cat.store.entity.StoredCliAgentOutputLog;
import com.cat.store.entity.StoredCliAgentTemplate;
import com.cat.store.entity.StoredTokenUsageLog;
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

    @Bean
    public JsonFileStore<StoredChatGroup> chatGroupStore() {
        return new JsonFileStore<>(dataDir, "chat_groups", StoredChatGroup.class);
    }

    @Bean
    public JsonFileStore<StoredChatGroupMessage> chatGroupMessageStore() {
        return new JsonFileStore<>(dataDir, "chat_group_messages", StoredChatGroupMessage.class);
    }
}
