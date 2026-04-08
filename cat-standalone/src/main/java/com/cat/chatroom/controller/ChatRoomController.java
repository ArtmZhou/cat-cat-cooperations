package com.cat.chatroom.controller;

import com.cat.chatroom.dto.*;
import com.cat.chatroom.entity.ChatMessage;
import com.cat.chatroom.entity.ChatRoom;
import com.cat.chatroom.service.ChatRoomService;
import com.cat.common.model.ApiResponse;
import com.cat.common.model.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 聊天室管理控制器
 *
 * 提供聊天室的创建、查询、更新、删除，以及消息发送和成员管理功能
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/chat-rooms")
@RequiredArgsConstructor
@Tag(name = "聊天室管理", description = "多Agent聊天室的创建、配置、消息发送与成员管理")
public class ChatRoomController {

    private final ChatRoomService chatRoomService;

    @Operation(summary = "创建聊天室", description = "创建一个新的多Agent聊天室")
    @PostMapping
    public ApiResponse<ChatRoomResponse> createRoom(
            @Valid @RequestBody CreateChatRoomRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        String creator = userId != null ? userId : "anonymous";
        ChatRoom room = chatRoomService.createRoom(request, creator);

        return ApiResponse.success(toResponse(room));
    }

    @Operation(summary = "获取聊天室列表", description = "分页查询聊天室列表，支持关键词搜索")
    @GetMapping
    public ApiResponse<PageResult<ChatRoomResponse>> listRooms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        ChatRoomQuery query = ChatRoomQuery.builder()
            .keyword(keyword)
            .active(active)
            .page(page)
            .pageSize(pageSize)
            .build();

        PageResult<ChatRoom> result = chatRoomService.listRooms(query);

        List<ChatRoomResponse> items = result.getItems().stream()
            .map(this::toResponse)
            .toList();

        PageResult<ChatRoomResponse> response = new PageResult<>(
            items,
            result.getTotal(),
            result.getPage(),
            result.getPageSize(),
            result.getTotalPages()
        );

        return ApiResponse.success(response);
    }

    @Operation(summary = "获取聊天室详情", description = "获取指定聊天室的详细信息和消息历史")
    @GetMapping("/{roomId}")
    public ApiResponse<ChatRoomDetailResponse> getRoom(
            @Parameter(description = "聊天室ID") @PathVariable String roomId) {

        ChatRoom room = chatRoomService.getRoom(roomId);
        return ApiResponse.success(toDetailResponse(room));
    }

    @Operation(summary = "更新聊天室", description = "更新聊天室的基本信息和成员")
    @PutMapping("/{roomId}")
    public ApiResponse<ChatRoomResponse> updateRoom(
            @Parameter(description = "聊天室ID") @PathVariable String roomId,
            @Valid @RequestBody UpdateChatRoomRequest request) {

        ChatRoom room = chatRoomService.updateRoom(roomId, request);
        return ApiResponse.success(toResponse(room));
    }

    @Operation(summary = "删除聊天室", description = "删除指定的聊天室及其所有消息")
    @DeleteMapping("/{roomId}")
    public ApiResponse<Void> deleteRoom(
            @Parameter(description = "聊天室ID") @PathVariable String roomId) {

        chatRoomService.deleteRoom(roomId);
        return ApiResponse.success();
    }

    // ========== 成员管理 ==========

    @Operation(summary = "添加Agent到聊天室", description = "将Agent添加到指定聊天室")
    @PostMapping("/{roomId}/agents")
    public ApiResponse<ChatRoomResponse> addAgent(
            @Parameter(description = "聊天室ID") @PathVariable String roomId,
            @Valid @RequestBody AddAgentRequest request) {

        ChatRoom room = chatRoomService.addAgent(roomId, request.getAgentId());
        return ApiResponse.success(toResponse(room));
    }

    @Operation(summary = "从聊天室移除Agent", description = "从指定聊天室移除Agent")
    @DeleteMapping("/{roomId}/agents/{agentId}")
    public ApiResponse<ChatRoomResponse> removeAgent(
            @Parameter(description = "聊天室ID") @PathVariable String roomId,
            @Parameter(description = "Agent ID") @PathVariable String agentId) {

        ChatRoom room = chatRoomService.removeAgent(roomId, agentId);
        return ApiResponse.success(toResponse(room));
    }

    // ========== 消息操作 ==========

    @Operation(summary = "发送消息", description = "向聊天室发送消息，支持@提及语法")
    @PostMapping("/{roomId}/messages")
    public ApiResponse<ChatMessageResponse> sendMessage(
            @Parameter(description = "聊天室ID") @PathVariable String roomId,
            @Valid @RequestBody SendMessageRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {

        String senderId = userId != null ? userId : "anonymous";
        String senderName = userName != null ? userName : senderId;

        ChatMessage message = chatRoomService.sendMessage(roomId, request, senderId, senderName);
        return ApiResponse.success(ChatMessageResponse.from(message));
    }

    @Operation(summary = "获取消息历史", description = "获取聊天室的消息历史，支持分页")
    @GetMapping("/{roomId}/messages")
    public ApiResponse<ChatMessageHistoryResponse> getMessages(
            @Parameter(description = "聊天室ID") @PathVariable String roomId,
            @RequestParam(required = false) String beforeId,
            @RequestParam(defaultValue = "50") int limit) {

        // 限制最大返回数量
        int actualLimit = Math.min(Math.max(1, limit), 100);
        ChatMessageHistoryResponse history = chatRoomService.getMessages(roomId, beforeId, actualLimit);
        return ApiResponse.success(history);
    }

    @Operation(summary = "清空消息历史", description = "清空聊天室的所有消息")
    @DeleteMapping("/{roomId}/messages")
    public ApiResponse<Void> clearMessages(
            @Parameter(description = "聊天室ID") @PathVariable String roomId) {

        chatRoomService.clearMessages(roomId);
        return ApiResponse.success();
    }

    // ========== 私有方法 ==========

    private ChatRoomResponse toResponse(ChatRoom room) {
        return ChatRoomResponse.builder()
            .id(room.getId())
            .name(room.getName())
            .description(room.getDescription())
            .createdBy(room.getCreatedBy())
            .createdAt(room.getCreatedAt())
            .updatedAt(room.getUpdatedAt())
            .agentIds(room.getAgentIds())
            .agentCount(room.getAgentCount())
            .messageCount(room.getMessageCount())
            .active(room.isActive())
            .build();
    }

    private ChatRoomDetailResponse toDetailResponse(ChatRoom room) {
        return ChatRoomDetailResponse.builder()
            .id(room.getId())
            .name(room.getName())
            .description(room.getDescription())
            .createdBy(room.getCreatedBy())
            .createdAt(room.getCreatedAt().toString())
            .updatedAt(room.getUpdatedAt().toString())
            .agentIds(room.getAgentIds())
            .messages(room.getMessages())
            .agentCount(room.getAgentCount())
            .messageCount(room.getMessageCount())
            .active(room.isActive())
            .hasMoreMessages(room.getMessageCount() > 0)
            .build();
    }
}
