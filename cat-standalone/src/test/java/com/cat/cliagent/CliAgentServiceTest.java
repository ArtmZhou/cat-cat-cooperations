package com.cat.cliagent;

import com.cat.cliagent.dto.*;
import com.cat.common.exception.BusinessException;
import com.cat.common.model.PageResult;
import com.cat.store.JsonFileStore;
import com.cat.store.entity.StoredCliAgent;
import com.cat.store.entity.StoredCliAgentCapability;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CliAgentServiceTest {

    private JsonFileStore<StoredCliAgent> cliAgentStore;
    private JsonFileStore<StoredCliAgentCapability> capabilityStore;
    private CliAgentTemplateService templateService;
    private ObjectMapper objectMapper;
    private CliAgentService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        cliAgentStore = new JsonFileStore<>(tempDir.toString(), "test_agents", StoredCliAgent.class);
        capabilityStore = new JsonFileStore<>(tempDir.toString(), "test_capabilities", StoredCliAgentCapability.class);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        templateService = mock(CliAgentTemplateService.class);
        service = new CliAgentService(cliAgentStore, capabilityStore, templateService, objectMapper);
    }

    // --------------- createAgent ---------------

    @Test
    void createAgent_shouldSaveAndReturnAgentWithNonNullId() {
        CliAgentTemplateResponse template = new CliAgentTemplateResponse();
        template.setId("claude-code");
        template.setName("Claude Code");
        template.setCliType("claude");
        template.setExecutablePath("claude");
        when(templateService.getTemplate("claude-code")).thenReturn(template);

        CreateCliAgentRequest request = new CreateCliAgentRequest();
        request.setName("test-agent");
        request.setDescription("test description");
        request.setTemplateId("claude-code");

        CliAgentResponse response = service.createAgent(request, "user-1");

        assertNotNull(response.getId());
        assertEquals("test-agent", response.getName());
        assertEquals("test description", response.getDescription());
        assertEquals("claude-code", response.getTemplateId());
        assertEquals("Claude Code", response.getTemplateName());
        assertEquals("claude", response.getCliType());
        assertEquals("STOPPED", response.getStatus());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
        assertTrue(cliAgentStore.findById(response.getId()).isPresent());
    }

    @Test
    void createAgent_shouldHandleCapabilities() {
        CliAgentTemplateResponse template = new CliAgentTemplateResponse();
        template.setId("claude-code");
        template.setName("Claude Code");
        template.setCliType("claude");
        template.setExecutablePath("claude");
        when(templateService.getTemplate("claude-code")).thenReturn(template);

        CreateCliAgentRequest request = new CreateCliAgentRequest();
        request.setName("cap-agent");
        request.setDescription("with capabilities");
        request.setTemplateId("claude-code");

        CreateCliAgentRequest.CapabilityDto capDto = new CreateCliAgentRequest.CapabilityDto();
        capDto.setType("CODE_GEN");
        capDto.setDomainTags(List.of("frontend", "vue"));
        capDto.setProficiencyLevel(4);
        request.setCapabilities(List.of(capDto));

        CliAgentResponse response = service.createAgent(request, "user-1");

        assertNotNull(response.getId());
        assertNotNull(response.getCapabilities());
        assertEquals(1, response.getCapabilities().size());
        assertEquals("CODE_GEN", response.getCapabilities().get(0).getType());
        assertEquals(List.of("frontend", "vue"), response.getCapabilities().get(0).getDomainTags());
        assertEquals(4, response.getCapabilities().get(0).getProficiencyLevel());
    }

    // --------------- getAgent ---------------

    @Test
    void getAgent_shouldReturnExistingAgent() {
        StoredCliAgent stored = new StoredCliAgent();
        stored.setId("agent-1");
        stored.setName("test-agent");
        stored.setStatus("STOPPED");
        cliAgentStore.save("agent-1", stored);

        CliAgentResponse response = service.getAgent("agent-1");

        assertEquals("agent-1", response.getId());
        assertEquals("test-agent", response.getName());
        assertEquals("STOPPED", response.getStatus());
    }

    @Test
    void getAgent_shouldThrowForNonexistentAgent() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getAgent("nonexistent-id"));
        assertEquals(404, ex.getCode());
    }

    // --------------- deleteAgent ---------------

    @Test
    void deleteAgent_shouldRemoveAgent() {
        StoredCliAgent stored = new StoredCliAgent();
        stored.setId("agent-1");
        stored.setName("test-agent");
        stored.setStatus("STOPPED");
        cliAgentStore.save("agent-1", stored);
        assertTrue(cliAgentStore.findById("agent-1").isPresent());

        service.deleteAgent("agent-1");

        assertFalse(cliAgentStore.findById("agent-1").isPresent());
    }

    @Test
    void deleteAgent_shouldThrowForNonexistentAgent() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.deleteAgent("nonexistent-id"));
        assertEquals(404, ex.getCode());
    }

    @Test
    void deleteAgent_shouldThrowForRunningAgent() {
        StoredCliAgent stored = new StoredCliAgent();
        stored.setId("agent-1");
        stored.setName("test-agent");
        stored.setStatus("RUNNING");
        cliAgentStore.save("agent-1", stored);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.deleteAgent("agent-1"));
        assertEquals(400, ex.getCode());
        assertTrue(cliAgentStore.findById("agent-1").isPresent());
    }

    @Test
    void deleteAgent_shouldThrowForExecutingAgent() {
        StoredCliAgent stored = new StoredCliAgent();
        stored.setId("agent-1");
        stored.setName("test-agent");
        stored.setStatus("EXECUTING");
        cliAgentStore.save("agent-1", stored);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.deleteAgent("agent-1"));
        assertEquals(400, ex.getCode());
        assertTrue(cliAgentStore.findById("agent-1").isPresent());
    }

    // --------------- listAgents ---------------

    @Test
    void listAgents_shouldReturnPagedResults() {
        for (int i = 0; i < 5; i++) {
            StoredCliAgent stored = new StoredCliAgent();
            stored.setId("agent-" + i);
            stored.setName("agent-" + i);
            stored.setStatus("STOPPED");
            cliAgentStore.save("agent-" + i, stored);
        }

        CliAgentQuery query = new CliAgentQuery();
        query.setPage(1);
        query.setPageSize(2);

        PageResult<CliAgentResponse> result = service.listAgents(query);

        assertEquals(5, result.getTotal());
        assertEquals(2, result.getItems().size());
        assertEquals(1, result.getPage());
        assertEquals(2, result.getPageSize());
        assertEquals(3, result.getTotalPages());
    }

    @Test
    void listAgents_shouldReturnEmptyPageWhenNoAgents() {
        CliAgentQuery query = new CliAgentQuery();
        query.setPage(1);
        query.setPageSize(20);

        PageResult<CliAgentResponse> result = service.listAgents(query);

        assertEquals(0, result.getTotal());
        assertTrue(result.getItems().isEmpty());
    }

    @Test
    void listAgents_shouldFilterByName() {
        StoredCliAgent agent1 = new StoredCliAgent();
        agent1.setId("agent-1");
        agent1.setName("alpha-agent");
        agent1.setStatus("STOPPED");
        cliAgentStore.save("agent-1", agent1);

        StoredCliAgent agent2 = new StoredCliAgent();
        agent2.setId("agent-2");
        agent2.setName("beta-agent");
        agent2.setStatus("STOPPED");
        cliAgentStore.save("agent-2", agent2);

        CliAgentQuery query = new CliAgentQuery();
        query.setName("alpha");

        PageResult<CliAgentResponse> result = service.listAgents(query);

        assertEquals(1, result.getTotal());
        assertEquals("alpha-agent", result.getItems().get(0).getName());
    }

    @Test
    void listAgents_shouldFilterByStatus() {
        StoredCliAgent agent1 = new StoredCliAgent();
        agent1.setId("agent-1");
        agent1.setName("agent-1");
        agent1.setStatus("RUNNING");
        cliAgentStore.save("agent-1", agent1);

        StoredCliAgent agent2 = new StoredCliAgent();
        agent2.setId("agent-2");
        agent2.setName("agent-2");
        agent2.setStatus("STOPPED");
        cliAgentStore.save("agent-2", agent2);

        CliAgentQuery query = new CliAgentQuery();
        query.setStatus("RUNNING");

        PageResult<CliAgentResponse> result = service.listAgents(query);

        assertEquals(1, result.getTotal());
        assertEquals("RUNNING", result.getItems().get(0).getStatus());
    }

    @Test
    void listAgents_shouldFilterByCliType() {
        StoredCliAgent agent1 = new StoredCliAgent();
        agent1.setId("agent-1");
        agent1.setName("agent-1");
        agent1.setStatus("STOPPED");
        agent1.setCliType("claude");
        cliAgentStore.save("agent-1", agent1);

        StoredCliAgent agent2 = new StoredCliAgent();
        agent2.setId("agent-2");
        agent2.setName("agent-2");
        agent2.setStatus("STOPPED");
        agent2.setCliType("opencode");
        cliAgentStore.save("agent-2", agent2);

        CliAgentQuery query = new CliAgentQuery();
        query.setCliType("claude");

        PageResult<CliAgentResponse> result = service.listAgents(query);

        assertEquals(1, result.getTotal());
        assertEquals("claude", result.getItems().get(0).getCliType());
    }

    @Test
    void listAgents_withDefaultQuery_shouldReturnAll() {
        StoredCliAgent agent1 = new StoredCliAgent();
        agent1.setId("agent-1");
        agent1.setName("agent-1");
        agent1.setStatus("STOPPED");
        cliAgentStore.save("agent-1", agent1);

        StoredCliAgent agent2 = new StoredCliAgent();
        agent2.setId("agent-2");
        agent2.setName("agent-2");
        agent2.setStatus("STOPPED");
        cliAgentStore.save("agent-2", agent2);

        CliAgentQuery query = new CliAgentQuery();
        PageResult<CliAgentResponse> result = service.listAgents(query);

        assertEquals(2, result.getTotal());
    }

    @Test
    void getAvailableAgents_shouldReturnOnlyRunning() {
        StoredCliAgent running = new StoredCliAgent();
        running.setId("agent-1");
        running.setName("running-agent");
        running.setStatus("RUNNING");
        cliAgentStore.save("agent-1", running);

        StoredCliAgent stopped = new StoredCliAgent();
        stopped.setId("agent-2");
        stopped.setName("stopped-agent");
        stopped.setStatus("STOPPED");
        cliAgentStore.save("agent-2", stopped);

        List<CliAgentResponse> available = service.getAvailableAgents();

        assertEquals(1, available.size());
        assertEquals("agent-1", available.get(0).getId());
    }

    // --------------- updateAgent ---------------

    @Test
    void updateAgent_shouldUpdateNameAndDescription() {
        StoredCliAgent stored = new StoredCliAgent();
        stored.setId("agent-1");
        stored.setName("old-name");
        stored.setDescription("old description");
        stored.setStatus("STOPPED");
        cliAgentStore.save("agent-1", stored);

        UpdateCliAgentRequest request = new UpdateCliAgentRequest();
        request.setName("new-name");
        request.setDescription("new description");

        CliAgentResponse response = service.updateAgent("agent-1", request);

        assertEquals("new-name", response.getName());
        assertEquals("new description", response.getDescription());
    }

    @Test
    void updateAgent_shouldOnlyUpdateProvidedFields() {
        StoredCliAgent stored = new StoredCliAgent();
        stored.setId("agent-1");
        stored.setName("original-name");
        stored.setDescription("original description");
        stored.setExecutablePath("/usr/bin/claude");
        stored.setStatus("STOPPED");
        cliAgentStore.save("agent-1", stored);

        // Only update name, leave description unchanged
        UpdateCliAgentRequest request = new UpdateCliAgentRequest();
        request.setName("updated-name");

        CliAgentResponse response = service.updateAgent("agent-1", request);

        assertEquals("updated-name", response.getName());
    }

    @Test
    void updateAgent_shouldThrowForNonexistentAgent() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.updateAgent("nonexistent", new UpdateCliAgentRequest()));
        assertEquals(404, ex.getCode());
    }

    @Test
    void updateAgent_shouldUpdateCapabilitiesWhenProvided() {
        StoredCliAgent stored = new StoredCliAgent();
        stored.setId("agent-1");
        stored.setName("cap-agent");
        stored.setStatus("STOPPED");
        cliAgentStore.save("agent-1", stored);

        UpdateCliAgentRequest request = new UpdateCliAgentRequest();
        request.setName("cap-agent-updated");

        CreateCliAgentRequest.CapabilityDto capDto = new CreateCliAgentRequest.CapabilityDto();
        capDto.setType("CODE_REVIEW");
        capDto.setDomainTags(List.of("backend"));
        capDto.setProficiencyLevel(5);
        request.setCapabilities(List.of(capDto));

        CliAgentResponse response = service.updateAgent("agent-1", request);

        assertEquals("cap-agent-updated", response.getName());
        assertNotNull(response.getCapabilities());
        assertEquals(1, response.getCapabilities().size());
        assertEquals("CODE_REVIEW", response.getCapabilities().get(0).getType());
    }
}
