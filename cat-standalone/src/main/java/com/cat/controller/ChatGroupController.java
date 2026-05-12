package com.cat.controller;

import com.cat.chatgroup.ChatGroupService;
import com.cat.chatgroup.ChatGroupService.*;
import com.cat.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "聊天群组", description = "多Agent群聊管理")
@RestController
@RequestMapping("/api/v1/chat-groups")
@RequiredArgsConstructor
public class ChatGroupController {

    private final ChatGroupService chatGroupService;

    @Operation(summary = "创建群组")
    @PostMapping
    public ApiResponse<ChatGroupInfo> createGroup(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ApiResponse.error(400, "群组名称不能为空");
        }
        String description = (String) request.get("description");
        @SuppressWarnings("unchecked")
        List<String> agentIds = (List<String>) request.get("agentIds");
        @SuppressWarnings("unchecked")
        List<String> kbIds = (List<String>) request.get("knowledgeBaseIds");

        return ApiResponse.success(chatGroupService.createGroup(name.trim(), description, agentIds, kbIds));
    }

    @Operation(summary = "更新群组")
    @PutMapping("/{groupId}")
    public ApiResponse<ChatGroupInfo> updateGroup(
            @PathVariable String groupId,
            @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        @SuppressWarnings("unchecked")
        List<String> agentIds = (List<String>) request.get("agentIds");
        @SuppressWarnings("unchecked")
        List<String> kbIds = (List<String>) request.get("knowledgeBaseIds");

        return ApiResponse.success(chatGroupService.updateGroup(groupId, name, description, agentIds, kbIds));
    }

    @Operation(summary = "删除群组")
    @DeleteMapping("/{groupId}")
    public ApiResponse<Void> deleteGroup(@PathVariable String groupId) {
        chatGroupService.deleteGroup(groupId);
        return ApiResponse.success();
    }

    @Operation(summary = "获取群组详情")
    @GetMapping("/{groupId}")
    public ApiResponse<ChatGroupInfo> getGroup(@PathVariable String groupId) {
        return ApiResponse.success(chatGroupService.getGroup(groupId));
    }

    @Operation(summary = "获取群组列表")
    @GetMapping
    public ApiResponse<List<ChatGroupInfo>> listGroups() {
        return ApiResponse.success(chatGroupService.listGroups());
    }

    @Operation(summary = "发送群聊消息")
    @PostMapping("/{groupId}/messages")
    public ApiResponse<ChatMessageInfo> sendMessage(
            @PathVariable String groupId,
            @RequestBody Map<String, Object> request) {
        String content = (String) request.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ApiResponse.error(400, "消息内容不能为空");
        }
        @SuppressWarnings("unchecked")
        List<String> mentionedAgentIds = (List<String>) request.get("mentionedAgentIds");

        return ApiResponse.success(chatGroupService.sendUserMessage(groupId, content.trim(), mentionedAgentIds));
    }

    @Operation(summary = "获取群聊消息")
    @GetMapping("/{groupId}/messages")
    public ApiResponse<List<ChatMessageInfo>> getMessages(
            @PathVariable String groupId,
            @RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.success(chatGroupService.getGroupMessages(groupId, limit));
    }

    @Operation(summary = "清空群聊消息")
    @PostMapping("/{groupId}/messages/clear")
    public ApiResponse<Void> clearMessages(@PathVariable String groupId) {
        chatGroupService.clearGroupMessages(groupId);
        return ApiResponse.success();
    }

    // ---- Knowledge base binding ----

    @Operation(summary = "获取群组绑定的知识库")
    @GetMapping("/{groupId}/knowledge-bases")
    public ApiResponse<ChatGroupInfo> getGroupKnowledgeBases(@PathVariable String groupId) {
        return ApiResponse.success(chatGroupService.getGroup(groupId));
    }

    @Operation(summary = "设置群组绑定的知识库")
    @PutMapping("/{groupId}/knowledge-bases")
    public ApiResponse<Void> setGroupKnowledgeBases(
            @PathVariable String groupId,
            @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<String> kbIds = (List<String>) body.get("knowledgeBaseIds");
        chatGroupService.updateGroup(groupId, null, null, null, kbIds);
        return ApiResponse.success();
    }
}
