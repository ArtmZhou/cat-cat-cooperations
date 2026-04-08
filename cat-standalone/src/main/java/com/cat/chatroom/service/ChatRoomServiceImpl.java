package com.cat.chatroom.service;

import com.cat.chatroom.dto.*;
import com.cat.chatroom.entity.ChatMessage;
import com.cat.chatroom.entity.ChatRoom;
import com.cat.cliagent.dto.CliAgentResponse;
import com.cat.cliagent.service.CliAgentService;
import com.cat.common.exception.BusinessException;
import com.cat.common.model.PageResult;
import com.cat.standalone.store.JsonFileStore;
import com.cat.standalone.store.entity.StoredChatRoom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 聊天室服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final JsonFileStore<StoredChatRoom> chatRoomStore;
    private final CliAgentService cliAgentService;

    @Override
    public ChatRoom createRoom(CreateChatRoomRequest request, String createdBy) {
        // 验证Agent成员
        if (request.getAgentIds() != null) {
            for (String agentId : request.getAgentIds()) {
                CliAgentResponse agent = cliAgentService.getAgent(agentId);
                if (agent == null) {
                    throw new BusinessException(404, "Agent不存在: " + agentId);
                }
            }
        }

        // 创建聊天室
        ChatRoom room = ChatRoom.create(
            request.getName(),
            request.getDescription(),
            createdBy,
            request.getAgentIds()
        );

        // 保存到存储
        StoredChatRoom stored = toStored(room);
        chatRoomStore.save(stored.getId(), stored);

        log.info("创建聊天室: {}, 名称: {}, 成员数: {}",
            room.getId(), room.getName(), room.getAgentIds().size());

        return room;
    }

    @Override
    public ChatRoom getRoom(String roomId) {
        StoredChatRoom stored = chatRoomStore.findById(roomId)
            .orElseThrow(() -> new BusinessException(404, "聊天室不存在: " + roomId));
        return fromStored(stored);
    }

    @Override
    public ChatRoom updateRoom(String roomId, UpdateChatRoomRequest request) {
        ChatRoom room = getRoom(roomId);

        // 更新基本信息
        room.update(request.getName(), request.getDescription());

        // 如果传入了新的Agent列表，则更新
        if (request.getAgentIds() != null) {
            // 验证新Agent
            for (String agentId : request.getAgentIds()) {
                CliAgentResponse agent = cliAgentService.getAgent(agentId);
                if (agent == null) {
                    throw new BusinessException(404, "Agent不存在: " + agentId);
                }
            }
            room.setAgentIds(new ArrayList<>(request.getAgentIds()));
            room.setUpdatedAt(LocalDateTime.now());
        }

        // 保存
        StoredChatRoom stored = toStored(room);
        chatRoomStore.save(stored.getId(), stored);

        log.info("更新聊天室: {}, 名称: {}", roomId, room.getName());

        return room;
    }

    @Override
    public void deleteRoom(String roomId) {
        if (!chatRoomStore.existsById(roomId)) {
            throw new BusinessException(404, "聊天室不存在: " + roomId);
        }

        chatRoomStore.deleteById(roomId);
        log.info("删除聊天室: {}", roomId);
    }

    @Override
    public PageResult<ChatRoom> listRooms(ChatRoomQuery query) {
        // 获取所有聊天室
        List<StoredChatRoom> allRooms = chatRoomStore.findAll();

        // 过滤
        List<StoredChatRoom> filtered = allRooms.stream()
            .filter(r -> {
                // 关键词过滤
                if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
                    String kw = query.getKeyword().toLowerCase();
                    boolean matches = (r.getName() != null && r.getName().toLowerCase().contains(kw))
                        || (r.getDescription() != null && r.getDescription().toLowerCase().contains(kw));
                    if (!matches) return false;
                }
                // 活跃状态过滤
                if (query.getActive() != null) {
                    if (r.isActive() != query.getActive()) return false;
                }
                return true;
            })
            .sorted(Comparator.comparing(StoredChatRoom::getUpdatedAt).reversed())
            .collect(Collectors.toList());

        // 分页
        int total = filtered.size();
        int page = query.getPage();
        int pageSize = query.getPageSize();
        int totalPages = (int) Math.ceil((double) total / pageSize);
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);

        List<ChatRoom> items = fromIndex < total
            ? filtered.subList(fromIndex, toIndex).stream()
                .map(this::fromStored)
                .collect(Collectors.toList())
            : new ArrayList<>();

        return new PageResult<>(items, total, page, pageSize, totalPages);
    }

    @Override
    public ChatRoom addAgent(String roomId, String agentId) {
        ChatRoom room = getRoom(roomId);

        // 验证Agent
        CliAgentResponse agent = cliAgentService.getAgent(agentId);
        if (agent == null) {
            throw new BusinessException(404, "Agent不存在: " + agentId);
        }

        // 添加Agent
        if (!room.addAgent(agentId)) {
            throw new BusinessException(400, "添加Agent失败，可能已达到最大成员数或Agent已在聊天室中");
        }

        // 保存
        StoredChatRoom stored = toStored(room);
        chatRoomStore.save(stored.getId(), stored);

        // 添加系统消息
        ChatMessage systemMsg = ChatMessage.createSystemMessage(
            "Agent " + agent.getName() + " 加入了聊天室");
        room.addMessage(systemMsg);
        StoredChatRoom updated = toStored(room);
        chatRoomStore.save(updated.getId(), updated);

        log.info("聊天室 {} 添加Agent: {}", roomId, agentId);

        return room;
    }

    @Override
    public ChatRoom removeAgent(String roomId, String agentId) {
        ChatRoom room = getRoom(roomId);

        // 移除Agent
        if (!room.removeAgent(agentId)) {
            throw new BusinessException(400, "Agent不在聊天室中");
        }

        // 保存
        StoredChatRoom stored = toStored(room);
        chatRoomStore.save(stored.getId(), stored);

        // 添加系统消息
        try {
            CliAgentResponse agent = cliAgentService.getAgent(agentId);
            ChatMessage systemMsg = ChatMessage.createSystemMessage(
                "Agent " + agent.getName() + " 离开了聊天室");
            room.addMessage(systemMsg);
            StoredChatRoom updated = toStored(room);
            chatRoomStore.save(updated.getId(), updated);
        } catch (Exception e) {
            // 忽略Agent不存在的情况
        }

        log.info("聊天室 {} 移除Agent: {}", roomId, agentId);

        return room;
    }

    @Override
    public ChatMessage sendMessage(String roomId, SendMessageRequest request,
                                    String senderId, String senderName) {
        ChatRoom room = getRoom(roomId);

        if (!room.isActive()) {
            throw new BusinessException(400, "聊天室已关闭");
        }

        // 创建消息
        ChatMessage message = ChatMessage.createUserMessage(
            senderId,
            senderName,
            request.getContent(),
            null // targetAgentIds将在外部解析
        );
        message.setReplyTo(request.getReplyTo());

        // 添加到聊天室
        room.addMessage(message);

        // 保存
        StoredChatRoom stored = toStored(room);
        chatRoomStore.save(stored.getId(), stored);

        log.debug("聊天室 {} 发送消息: {}", roomId, message.getId());

        return message;
    }

    @Override
    public ChatMessageHistoryResponse getMessages(String roomId, String beforeId, int limit) {
        ChatRoom room = getRoom(roomId);

        List<ChatMessage> messages = room.getMessagesBefore(beforeId, limit);
        boolean hasMore = false;

        if (beforeId == null) {
            // 返回最新消息，检查是否还有更多
            hasMore = room.getMessageCount() > messages.size();
        } else {
            // 查找beforeId之前是否还有更多
            int beforeIndex = -1;
            for (int i = 0; i < room.getMessages().size(); i++) {
                if (room.getMessages().get(i).getId().equals(beforeId)) {
                    beforeIndex = i;
                    break;
                }
            }
            hasMore = beforeIndex > limit;
        }

        return ChatMessageHistoryResponse.builder()
            .messages(messages)
            .hasMore(hasMore)
            .totalCount(room.getMessageCount())
            .build();
    }

    @Override
    public List<ChatMessage> getRecentMessages(String roomId, int count) {
        ChatRoom room = getRoom(roomId);
        return room.getRecentMessages(count);
    }

    @Override
    public void clearMessages(String roomId) {
        ChatRoom room = getRoom(roomId);
        room.clearMessages();

        StoredChatRoom stored = toStored(room);
        chatRoomStore.save(stored.getId(), stored);

        log.info("聊天室 {} 清空消息", roomId);
    }

    @Override
    public boolean hasAccess(String roomId, String userId) {
        try {
            ChatRoom room = getRoom(roomId);
            // 创建者有权限
            if (room.getCreatedBy().equals(userId)) {
                return true;
            }
            // 其他权限逻辑可以在这里扩展
            return true; // 默认开放访问
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public List<ChatRoom> findRoomsByAgentId(String agentId) {
        return chatRoomStore.find(r -> r.getAgentIds().contains(agentId))
            .stream()
            .filter(StoredChatRoom::isActive)
            .map(this::fromStored)
            .collect(Collectors.toList());
    }

    /**
     * 添加Agent消息
     */
    public ChatMessage addAgentMessage(String roomId, String agentId, String content,
                                        java.util.Map<String, Object> metadata) {
        ChatRoom room = getRoom(roomId);

        // 获取Agent名称
        String agentName = agentId;
        try {
            CliAgentResponse agent = cliAgentService.getAgent(agentId);
            if (agent != null) {
                agentName = agent.getName();
            }
        } catch (Exception e) {
            // 使用ID作为名称
        }

        // 创建Agent消息
        ChatMessage message = ChatMessage.createAgentMessage(agentId, agentName, content, metadata);
        room.addMessage(message);

        // 保存
        StoredChatRoom stored = toStored(room);
        chatRoomStore.save(stored.getId(), stored);

        return message;
    }

    /**
     * 转换为存储实体
     */
    private StoredChatRoom toStored(ChatRoom room) {
        StoredChatRoom stored = new StoredChatRoom();
        stored.setId(room.getId());
        stored.setName(room.getName());
        stored.setDescription(room.getDescription());
        stored.setCreatedBy(room.getCreatedBy());
        stored.setCreatedAt(room.getCreatedAt());
        stored.setUpdatedAt(room.getUpdatedAt());
        stored.setAgentIds(new ArrayList<>(room.getAgentIds()));
        stored.setMessages(new ArrayList<>(room.getMessages()));
        stored.setActive(room.isActive());
        return stored;
    }

    /**
     * 从存储实体转换
     */
    private ChatRoom fromStored(StoredChatRoom stored) {
        return ChatRoom.builder()
            .id(stored.getId())
            .name(stored.getName())
            .description(stored.getDescription())
            .createdBy(stored.getCreatedBy())
            .createdAt(stored.getCreatedAt())
            .updatedAt(stored.getUpdatedAt())
            .agentIds(stored.getAgentIds() != null ? new ArrayList<>(stored.getAgentIds()) : new ArrayList<>())
            .messages(stored.getMessages() != null ? new ArrayList<>(stored.getMessages()) : new ArrayList<>())
            .active(stored.isActive())
            .build();
    }
}
