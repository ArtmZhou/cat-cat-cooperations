package com.cat.standalone.controller;

import com.cat.cliagent.service.AgentMessageService;
import com.cat.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Agent消息通信控制器
 */
@Tag(name = "Agent消息通信", description = "Agent之间的消息传递")
@RestController
@RequestMapping("/api/v1/cli-agents/messages")
@RequiredArgsConstructor
public class AgentMessageController {

    private final AgentMessageService messageService;

    @Operation(summary = "发送消息", description = "发送消息给指定Agent")
    @PostMapping("/send")
    public ApiResponse<Boolean> sendMessage(@RequestBody Map<String, String> request) {
        String fromAgentId = request.get("fromAgentId");
        String toAgentId = request.get("toAgentId");
        String message = request.get("message");

        boolean result = messageService.sendMessage(fromAgentId, toAgentId, message);
        return ApiResponse.success(result);
    }

    @Operation(summary = "广播消息", description = "广播消息给所有运行中的Agent")
    @PostMapping("/broadcast")
    public ApiResponse<Integer> broadcastMessage(@RequestBody Map<String, String> request) {
        String fromAgentId = request.get("fromAgentId");
        String message = request.get("message");

        int count = messageService.broadcastMessage(fromAgentId, message);
        return ApiResponse.success(count);
    }

    @Operation(summary = "获取待处理消息", description = "获取Agent的待处理消息")
    @GetMapping("/{agentId}/pending")
    public ApiResponse<List<AgentMessageService.AgentMessage>> getPendingMessages(@PathVariable String agentId) {
        List<AgentMessageService.AgentMessage> messages = messageService.getPendingMessages(agentId);
        return ApiResponse.success(messages);
    }

    @Operation(summary = "确认消息", description = "确认消息已处理")
    @PostMapping("/{agentId}/ack/{messageId}")
    public ApiResponse<Void> ackMessage(
            @PathVariable String agentId,
            @PathVariable String messageId) {
        messageService.ackMessage(agentId, messageId);
        return ApiResponse.success();
    }
}