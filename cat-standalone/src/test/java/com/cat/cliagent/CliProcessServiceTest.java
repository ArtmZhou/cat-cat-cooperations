package com.cat.cliagent;

import com.cat.cliagent.dto.CliAgentResponse;
import com.cat.common.exception.BusinessException;
import com.cat.store.JsonFileStore;
import com.cat.store.entity.StoredCliAgent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CliProcessServiceTest {

    private JsonFileStore<StoredCliAgent> cliAgentStore;
    private CliAgentTemplateService templateService;
    private CliAgentService cliAgentService;
    private CliProcessService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        cliAgentStore = new JsonFileStore<>(tempDir.toString(), "test_agents", StoredCliAgent.class);
        templateService = mock(CliAgentTemplateService.class);
        cliAgentService = mock(CliAgentService.class);
        service = new CliProcessService(cliAgentStore, templateService, cliAgentService);
    }

    private StoredCliAgent createAgent(String id, String status) {
        StoredCliAgent agent = new StoredCliAgent();
        agent.setId(id);
        agent.setName(id);
        agent.setStatus(status);
        agent.setExecutablePath("claude"); // system PATH command, skips file existence check
        return agent;
    }

    private void mockGetAgent(String agentId, String status) {
        CliAgentResponse resp = new CliAgentResponse();
        resp.setId(agentId);
        resp.setStatus(status);
        when(cliAgentService.getAgent(agentId)).thenReturn(resp);
    }

    // --------------- startProcess ---------------

    @Test
    void startProcess_shouldChangeStatusToRunning() {
        StoredCliAgent agent = createAgent("agent-1", "STOPPED");
        cliAgentStore.save("agent-1", agent);
        mockGetAgent("agent-1", "RUNNING");

        CliAgentResponse response = service.startProcess("agent-1");

        assertEquals("RUNNING", response.getStatus());
        StoredCliAgent updated = cliAgentStore.findById("agent-1").orElseThrow();
        assertEquals("RUNNING", updated.getStatus());
        assertEquals("per-request-mode", updated.getProcessId());
        assertNotNull(updated.getLastStartedAt());
        assertNotNull(updated.getUpdatedAt());
    }

    @Test
    void startProcess_shouldThrowWhenAlreadyRunning() {
        StoredCliAgent agent = createAgent("agent-1", "RUNNING");
        cliAgentStore.save("agent-1", agent);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.startProcess("agent-1"));
        assertEquals(400, ex.getCode());
    }

    @Test
    void startProcess_shouldThrowWhenAlreadyExecuting() {
        StoredCliAgent agent = createAgent("agent-1", "EXECUTING");
        cliAgentStore.save("agent-1", agent);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.startProcess("agent-1"));
        assertEquals(400, ex.getCode());
    }

    @Test
    void startProcess_shouldThrowForNonexistentAgent() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.startProcess("nonexistent"));
        assertEquals(404, ex.getCode());
    }

    @Test
    void startProcess_shouldAllowStartFromErrorState() {
        StoredCliAgent agent = createAgent("agent-1", "ERROR");
        cliAgentStore.save("agent-1", agent);
        mockGetAgent("agent-1", "RUNNING");

        CliAgentResponse response = service.startProcess("agent-1");

        assertEquals("RUNNING", response.getStatus());
        StoredCliAgent updated = cliAgentStore.findById("agent-1").orElseThrow();
        assertEquals("RUNNING", updated.getStatus());
    }

    @Test
    void startProcess_shouldAllowStartFromStartingState() {
        StoredCliAgent agent = createAgent("agent-1", "STARTING");
        cliAgentStore.save("agent-1", agent);
        mockGetAgent("agent-1", "RUNNING");

        CliAgentResponse response = service.startProcess("agent-1");

        assertEquals("RUNNING", response.getStatus());
        StoredCliAgent updated = cliAgentStore.findById("agent-1").orElseThrow();
        assertEquals("RUNNING", updated.getStatus());
    }

    // --------------- stopProcess ---------------

    @Test
    void stopProcess_shouldChangeStatusToStopped() {
        StoredCliAgent agent = createAgent("agent-1", "RUNNING");
        agent.setProcessId("per-request-mode");
        agent.setSessionId("session-1");
        cliAgentStore.save("agent-1", agent);
        mockGetAgent("agent-1", "STOPPED");

        CliAgentResponse response = service.stopProcess("agent-1");

        assertEquals("STOPPED", response.getStatus());
        StoredCliAgent updated = cliAgentStore.findById("agent-1").orElseThrow();
        assertEquals("STOPPED", updated.getStatus());
        assertNull(updated.getProcessId());
        assertNull(updated.getSessionId());
        assertNotNull(updated.getLastStoppedAt());
        assertNotNull(updated.getUpdatedAt());
    }

    @Test
    void stopProcess_shouldThrowWhenAlreadyStopped() {
        StoredCliAgent agent = createAgent("agent-1", "STOPPED");
        cliAgentStore.save("agent-1", agent);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.stopProcess("agent-1"));
        assertEquals(400, ex.getCode());
    }

    @Test
    void stopProcess_shouldAllowStopFromExecutingState() {
        StoredCliAgent agent = createAgent("agent-1", "EXECUTING");
        agent.setSessionId("session-1");
        cliAgentStore.save("agent-1", agent);
        mockGetAgent("agent-1", "STOPPED");

        service.stopProcess("agent-1");

        StoredCliAgent updated = cliAgentStore.findById("agent-1").orElseThrow();
        assertEquals("STOPPED", updated.getStatus());
        assertNull(updated.getSessionId());
    }

    @Test
    void stopProcess_shouldAllowStopFromErrorState() {
        StoredCliAgent agent = createAgent("agent-1", "ERROR");
        agent.setProcessId("per-request-mode");
        cliAgentStore.save("agent-1", agent);
        mockGetAgent("agent-1", "STOPPED");

        service.stopProcess("agent-1");

        StoredCliAgent updated = cliAgentStore.findById("agent-1").orElseThrow();
        assertEquals("STOPPED", updated.getStatus());
    }

    // --------------- getProcessStatus ---------------

    @Test
    void getProcessStatus_shouldReturnCorrectInfo() {
        StoredCliAgent agent = createAgent("agent-1", "RUNNING");
        agent.setProcessId("per-request-mode");
        agent.setLastStartedAt(java.time.LocalDateTime.now());
        cliAgentStore.save("agent-1", agent);

        CliProcessService.ProcessStatus status = service.getProcessStatus("agent-1");

        assertEquals("agent-1", status.agentId());
        assertEquals("RUNNING", status.status());
        assertNotNull(status.startTime());
        assertNotNull(status.uptimeMs());
    }

    @Test
    void getProcessStatus_shouldReturnStoppedStatus() {
        StoredCliAgent agent = createAgent("agent-1", "STOPPED");
        cliAgentStore.save("agent-1", agent);

        CliProcessService.ProcessStatus status = service.getProcessStatus("agent-1");

        assertEquals("agent-1", status.agentId());
        assertEquals("STOPPED", status.status());
        assertNull(status.startTime());
        assertNull(status.uptimeMs());
    }

    @Test
    void getProcessStatus_shouldThrowForNonexistentAgent() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.getProcessStatus("nonexistent"));
        assertEquals(404, ex.getCode());
    }

    // --------------- isProcessHealthy ---------------

    @Test
    void isProcessHealthy_shouldReturnTrueForRunning() {
        StoredCliAgent agent = createAgent("agent-1", "RUNNING");
        cliAgentStore.save("agent-1", agent);
        assertTrue(service.isProcessHealthy("agent-1"));
    }

    @Test
    void isProcessHealthy_shouldReturnTrueForStopped() {
        StoredCliAgent agent = createAgent("agent-1", "STOPPED");
        cliAgentStore.save("agent-1", agent);
        assertTrue(service.isProcessHealthy("agent-1"));
    }

    @Test
    void isProcessHealthy_shouldReturnTrueForExecuting() {
        StoredCliAgent agent = createAgent("agent-1", "EXECUTING");
        cliAgentStore.save("agent-1", agent);
        assertTrue(service.isProcessHealthy("agent-1"));
    }

    @Test
    void isProcessHealthy_shouldReturnFalseForError() {
        StoredCliAgent agent = createAgent("agent-1", "ERROR");
        cliAgentStore.save("agent-1", agent);
        assertFalse(service.isProcessHealthy("agent-1"));
    }

    @Test
    void isProcessHealthy_shouldReturnFalseForNonexistentAgent() {
        assertFalse(service.isProcessHealthy("nonexistent"));
    }

    // --------------- resetAgentStatusOnStartup ---------------

    @Test
    void resetAgentStatusOnStartup_shouldResetRunningAndExecutingAgents() {
        StoredCliAgent running = createAgent("agent-1", "RUNNING");
        running.setProcessId("per-request-mode");
        running.setSessionId("session-1");
        cliAgentStore.save("agent-1", running);

        StoredCliAgent executing = createAgent("agent-2", "EXECUTING");
        executing.setProcessId("per-request-mode");
        executing.setSessionId("session-2");
        cliAgentStore.save("agent-2", executing);

        // stopped agents should be unaffected
        StoredCliAgent stopped = createAgent("agent-3", "STOPPED");
        stopped.setSessionId("session-3");
        cliAgentStore.save("agent-3", stopped);

        service.resetAgentStatusOnStartup();

        // Running agent reset
        StoredCliAgent updated1 = cliAgentStore.findById("agent-1").orElseThrow();
        assertEquals("STOPPED", updated1.getStatus());
        assertNull(updated1.getProcessId());
        assertNull(updated1.getSessionId());
        assertNotNull(updated1.getLastStoppedAt());

        // Executing agent reset
        StoredCliAgent updated2 = cliAgentStore.findById("agent-2").orElseThrow();
        assertEquals("STOPPED", updated2.getStatus());
        assertNull(updated2.getProcessId());
        assertNull(updated2.getSessionId());
        assertNotNull(updated2.getLastStoppedAt());

        // Stopped agent unchanged
        StoredCliAgent updated3 = cliAgentStore.findById("agent-3").orElseThrow();
        assertEquals("STOPPED", updated3.getStatus());
        assertEquals("session-3", updated3.getSessionId());
    }

    @Test
    void resetAgentStatusOnStartup_shouldHandleNoRunningAgents() {
        StoredCliAgent stopped = createAgent("agent-1", "STOPPED");
        cliAgentStore.save("agent-1", stopped);
        StoredCliAgent error = createAgent("agent-2", "ERROR");
        cliAgentStore.save("agent-2", error);

        // Should not throw
        service.resetAgentStatusOnStartup();

        assertEquals("STOPPED", cliAgentStore.findById("agent-1").get().getStatus());
        assertEquals("ERROR", cliAgentStore.findById("agent-2").get().getStatus());
    }

    @Test
    void resetAgentStatusOnStartup_shouldHandleEmptyStore() {
        // Should not throw
        service.resetAgentStatusOnStartup();
    }
}
