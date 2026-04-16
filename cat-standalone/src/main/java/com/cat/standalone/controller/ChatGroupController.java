package com.cat.standalone.controller;

import com.cat.cliagent.service.ChatGroupService;
import com.cat.cliagent.service.ChatGroupService.*;
import com.cat.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 聊天群组控制器
 *
 * 管理多Agent群聊，支持创建群组、发送消息、@提及和广播
 */
@Tag(name = "聊天群组", description = "多Agent群聊管理")
@RestController
@RequestMapping("/api/v1/chat-groups")
@RequiredArgsConstructor
public class ChatGroupController {

    private final ChatGroupService chatGroupService;

    @Operation(summary = "创建群组", description = "创建多Agent聊天群组")
    @PostMapping
    public ApiResponse<ChatGroupInfo> createGroup(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ApiResponse.error(400, "群组名称不能为空");
        }
        String description = (String) request.get("description");
        @SuppressWarnings("unchecked")
        List<String> agentIds = (List<String>) request.get("agentIds");

        ChatGroupInfo group = chatGroupService.createGroup(name.trim(), description, agentIds);
        return ApiResponse.success(group);
    }

    @Operation(summary = "更新群组", description = "更新聊天群组信息")
    @PutMapping("/{groupId}")
    public ApiResponse<ChatGroupInfo> updateGroup(
            @PathVariable String groupId,
            @RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String description = (String) request.get("description");
        @SuppressWarnings("unchecked")
        List<String> agentIds = (List<String>) request.get("agentIds");

        ChatGroupInfo group = chatGroupService.updateGroup(groupId, name, description, agentIds);

        // 更新自动讨论设置
        if (request.containsKey("autoDiscussion") || request.containsKey("maxAutoRounds")) {
            Boolean autoDiscussion = request.get("autoDiscussion") != null
                ? Boolean.valueOf(request.get("autoDiscussion").toString()) : null;
            Integer maxAutoRounds = request.get("maxAutoRounds") != null
                ? Integer.valueOf(request.get("maxAutoRounds").toString()) : null;
            if (autoDiscussion != null || maxAutoRounds != null) {
                group = chatGroupService.updateAutoDiscussionSettings(groupId, autoDiscussion, maxAutoRounds);
            }
        }

        return ApiResponse.success(group);
    }

    @Operation(summary = "删除群组", description = "删除聊天群组")
    @DeleteMapping("/{groupId}")
    public ApiResponse<Void> deleteGroup(@PathVariable String groupId) {
        chatGroupService.deleteGroup(groupId);
        return ApiResponse.success();
    }

    @Operation(summary = "获取群组详情", description = "获取聊天群组详情信息")
    @GetMapping("/{groupId}")
    public ApiResponse<ChatGroupInfo> getGroup(@PathVariable String groupId) {
        ChatGroupInfo group = chatGroupService.getGroup(groupId);
        return ApiResponse.success(group);
    }

    @Operation(summary = "获取群组列表", description = "获取所有聊天群组")
    @GetMapping
    public ApiResponse<List<ChatGroupInfo>> listGroups() {
        List<ChatGroupInfo> groups = chatGroupService.listGroups();
        return ApiResponse.success(groups);
    }

    @Operation(summary = "发送群聊消息", description = "发送消息到群组，支持@指定agent或广播")
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

        ChatMessageInfo message = chatGroupService.sendUserMessage(groupId, content.trim(), mentionedAgentIds);
        return ApiResponse.success(message);
    }

    @Operation(summary = "获取群聊消息", description = "获取群组历史消息")
    @GetMapping("/{groupId}/messages")
    public ApiResponse<List<ChatMessageInfo>> getMessages(
            @PathVariable String groupId,
            @RequestParam(defaultValue = "100") int limit) {
        List<ChatMessageInfo> messages = chatGroupService.getGroupMessages(groupId, limit);
        return ApiResponse.success(messages);
    }

    @Operation(summary = "清空群聊消息", description = "清空群组的所有历史消息")
    @PostMapping("/{groupId}/messages/clear")
    public ApiResponse<Void> clearMessages(@PathVariable String groupId) {
        chatGroupService.clearGroupMessages(groupId);
        return ApiResponse.success();
    }

    @Operation(summary = "停止自动讨论", description = "中断群组中正在进行的自动讨论")
    @PostMapping("/{groupId}/auto-discussion/stop")
    public ApiResponse<Void> stopAutoDiscussion(@PathVariable String groupId) {
        chatGroupService.stopAutoDiscussion(groupId);
        return ApiResponse.success();
    }

    @Operation(summary = "获取自动讨论状态", description = "获取群组中自动讨论是否正在进行")
    @GetMapping("/{groupId}/auto-discussion/status")
    public ApiResponse<Map<String, Object>> getAutoDiscussionStatus(@PathVariable String groupId) {
        boolean running = chatGroupService.isAutoDiscussionRunning(groupId);
        Map<String, Object> status = new HashMap<>();
        status.put("running", running);
        return ApiResponse.success(status);
    }
}
