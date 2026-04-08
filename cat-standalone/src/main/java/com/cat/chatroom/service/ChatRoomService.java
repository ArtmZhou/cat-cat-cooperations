package com.cat.chatroom.service;

import com.cat.chatroom.dto.*;
import com.cat.chatroom.entity.ChatMessage;
import com.cat.chatroom.entity.ChatRoom;
import com.cat.common.model.PageResult;

import java.util.List;

/**
 * 聊天室服务接口
 */
public interface ChatRoomService {

    /**
     * 创建聊天室
     */
    ChatRoom createRoom(CreateChatRoomRequest request, String createdBy);

    /**
     * 获取聊天室详情
     */
    ChatRoom getRoom(String roomId);

    /**
     * 更新聊天室
     */
    ChatRoom updateRoom(String roomId, UpdateChatRoomRequest request);

    /**
     * 删除聊天室
     */
    void deleteRoom(String roomId);

    /**
     * 查询聊天室列表
     */
    PageResult<ChatRoom> listRooms(ChatRoomQuery query);

    /**
     * 添加Agent到聊天室
     */
    ChatRoom addAgent(String roomId, String agentId);

    /**
     * 从聊天室移除Agent
     */
    ChatRoom removeAgent(String roomId, String agentId);

    /**
     * 发送消息到聊天室
     */
    ChatMessage sendMessage(String roomId, SendMessageRequest request, String senderId, String senderName);

    /**
     * 获取消息历史
     */
    ChatMessageHistoryResponse getMessages(String roomId, String beforeId, int limit);

    /**
     * 获取最近消息
     */
    List<ChatMessage> getRecentMessages(String roomId, int count);

    /**
     * 清空消息历史
     */
    void clearMessages(String roomId);

    /**
     * 检查用户是否有权限访问聊天室
     */
    boolean hasAccess(String roomId, String userId);

    /**
     * 查找包含指定Agent的所有聊天室
     */
    List<ChatRoom> findRoomsByAgentId(String agentId);
}
