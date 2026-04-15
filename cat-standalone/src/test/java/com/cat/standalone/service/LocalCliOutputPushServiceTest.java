package com.cat.standalone.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.*;

class LocalCliOutputPushServiceTest {

    private SimpMessagingTemplate messagingTemplate;
    private LocalChatGroupService chatGroupService;
    private LocalCliOutputPushService service;

    @BeforeEach
    void setUp() {
        messagingTemplate = mock(SimpMessagingTemplate.class);
        chatGroupService = mock(LocalChatGroupService.class);
        service = new LocalCliOutputPushService(messagingTemplate);
        ReflectionTestUtils.setField(service, "chatGroupService", chatGroupService);
    }

    @Test
    void pushOutput_shouldNotLeakGroupOutputToDirectChatTopic() {
        when(chatGroupService.isAgentInGroupContext("agent-1")).thenReturn(true);

        service.pushOutput("agent-1", "group only output");

        verify(chatGroupService).isAgentInGroupContext("agent-1");
        verifyNoMoreInteractions(chatGroupService);
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void pushTextDelta_shouldRouteToGroupOnlyWhenAgentInGroupContext() {
        when(chatGroupService.isAgentInGroupContext("agent-1")).thenReturn(true);

        service.pushTextDelta("agent-1", "hello");

        verify(chatGroupService).handleAgentTextDelta("agent-1", "hello");
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void pushError_shouldRouteToGroupOnlyWhenAgentInGroupContext() {
        when(chatGroupService.isAgentInGroupContext("agent-1")).thenReturn(true);

        service.pushError("agent-1", "boom");

        verify(chatGroupService).handleAgentError("agent-1", "boom");
        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void pushStatusChange_shouldSkipExecutingButSyncRunningForGroupedAgent() {
        when(chatGroupService.isAgentInGroupContext("agent-1")).thenReturn(true);

        service.pushStatusChange("agent-1", "EXECUTING");
        service.pushStatusChange("agent-1", "RUNNING");

        verify(messagingTemplate, never()).convertAndSend("/topic/cli/status/agent-1",
            new LocalCliOutputPushService.StatusMessage("agent-1", "EXECUTING"));
        verify(messagingTemplate).convertAndSend("/topic/cli/status/agent-1",
            new LocalCliOutputPushService.StatusMessage("agent-1", "RUNNING"));
    }

    @Test
    void pushDone_shouldCompleteGroupRoutingAndOnlyPublishFinalRunningStatus() {
        when(chatGroupService.isAgentInGroupContext("agent-1")).thenReturn(true);

        service.pushDone("agent-1");

        verify(chatGroupService).handleAgentDone("agent-1");
        verify(messagingTemplate).convertAndSend("/topic/cli/status/agent-1",
            new LocalCliOutputPushService.StatusMessage("agent-1", "RUNNING"));
        verify(messagingTemplate, never()).convertAndSend("/topic/cli/agent-1/output",
            new LocalCliOutputPushService.OutputMessage("done", ""));
    }

    @Test
    void pushTextDelta_shouldStillReachDirectChatTopicWhenNotInGroupContext() {
        when(chatGroupService.isAgentInGroupContext("agent-2")).thenReturn(false);

        service.pushTextDelta("agent-2", "direct reply");

        verify(messagingTemplate).convertAndSend("/topic/cli/agent-2/output",
            new LocalCliOutputPushService.OutputMessage("text_delta", "direct reply"));
        verify(chatGroupService, never()).handleAgentTextDelta(anyString(), anyString());
    }
}
