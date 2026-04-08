package com.cat.chatroom.service;

import com.cat.chatroom.dto.SendMessageRequest;
import com.cat.chatroom.entity.ChatMessage;
import com.cat.chatroom.entity.ChatRoom;
import com.cat.cliagent.service.CliSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 消息路由服务
 *
 * 负责处理消息的路由逻辑：
 * 1. 解析@提及
 * 2. 决定消息发送给哪些Agent
 * 3. 构建Agent提示词
 * 4. 触发Agent处理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageRouterService {

    private final ChatRoomService chatRoomService;
    private final MentionParserService mentionParserService;
    private final ChatRoomPushService pushService;
    private final CliSessionService cliSessionService;

    /**
     * 处理用户发送的消息
     *
     * @param roomId 聊天室ID
     * @param request 发送消息请求
     * @param senderId 发送者ID
     * @param senderName 发送者名称
     * @return 创建的消息
     */
    public ChatMessage processUserMessage(String roomId, SendMessageRequest request,
                                           String senderId, String senderName) {
        // 1. 获取聊天室信息
        ChatRoom room = chatRoomService.getRoom(roomId);

        // 2. 解析@提及
        List<String> mentionedAgentIds = mentionParserService.parseMentions(
            request.getContent(), room.getAgentIds());

        log.info("聊天室 {} 收到消息，@提及Agent: {}", roomId, mentionedAgentIds);

        // 3. 创建并保存消息（带@提及信息）
        ChatMessage message = ChatMessage.createUserMessage(
            senderId,
            senderName,
            request.getContent(),
            mentionedAgentIds
        );
        message.setReplyTo(request.getReplyTo());

        // 保存到聊天室
        room.addMessage(message);

        // 4. 推送消息到WebSocket（通知前端）
        pushService.pushMessage(roomId, message);

        // 5. 路由到目标Agent
        List<String> targetAgentIds = mentionedAgentIds.isEmpty()
            ? room.getAgentIds()  // 无@则广播给所有
            : mentionedAgentIds;  // 有@则只发给被@的

        // 6. 向目标Agent发送
        for (String agentId : targetAgentIds) {
            if (room.hasAgent(agentId)) {
                sendToAgent(roomId, agentId, message, room);
            }
        }

        return message;
    }

    /**
     * 向指定Agent发送消息
     */
    private void sendToAgent(String roomId, String agentId, ChatMessage message, ChatRoom room) {
        try {
            // 构建给Agent的提示词
            String agentPrompt = buildAgentPrompt(roomId, message, room);

            log.debug("向Agent {} 发送消息，prompt长度: {}", agentId, agentPrompt.length());

            // 推送Agent输入状态
            pushService.pushTypingStatus(roomId, agentId, getAgentName(agentId), true);

            // 调用Agent会话服务发送输入
            boolean sent = cliSessionService.sendInput(agentId, agentPrompt);

            if (!sent) {
                log.warn("向Agent {} 发送消息失败", agentId);
                // 发送系统通知
                pushService.pushSystemNotification(roomId,
                    "Agent " + getAgentName(agentId) + " 暂时无法响应", "error");
            }

        } catch (Exception e) {
            log.error("向Agent {} 发送消息异常", agentId, e);
            pushService.pushSystemNotification(roomId,
                "Agent " + getAgentName(agentId) + " 处理消息时出错: " + e.getMessage(), "error");
        }
    }

    /**
     * 构建Agent提示词
     */
    private String buildAgentPrompt(String roomId, ChatMessage message, ChatRoom room) {
        StringBuilder prompt = new StringBuilder();

        // 聊天室上下文
        prompt.append("【聊天室消息】\n");
        prompt.append("聊天室: ").append(room.getName()).append("\n");
        prompt.append("发送者: ").append(message.getSenderName()).append("\n");
        prompt.append("消息内容:\n");
        prompt.append(message.getContent()).append("\n\n");

        // 提及信息
        if (message.getTargetAgentIds() != null && !message.getTargetAgentIds().isEmpty()) {
            prompt.append("【提及说明】\n");
            prompt.append("这条消息是定向发送给你的，请优先响应。\n\n");
        } else {
            prompt.append("【广播说明】\n");
            prompt.append("这条消息是发送给聊天室中所有Agent的，请根据你的专业领域决定是否响应。\n\n");
        }

        // 聊天室上下文（最近几条消息）
        List<ChatMessage> recentMessages = room.getRecentMessages(5);
        if (!recentMessages.isEmpty() && recentMessages.size() > 1) {
            prompt.append("【最近对话上下文】\n");
            for (ChatMessage ctx : recentMessages) {
                // 跳过当前消息
                if (ctx.getId().equals(message.getId())) {
                    continue;
                }
                prompt.append(ctx.getSenderName()).append(" (").append(ctx.getType()).append("): ");
                // 截断过长的消息
                String content = ctx.getContent();
                if (content.length() > 200) {
                    content = content.substring(0, 200) + "...";
                }
                prompt.append(content).append("\n");
            }
            prompt.append("\n");
        }

        // 响应要求
        prompt.append("【响应要求】\n");
        prompt.append("1. 请简洁、专业地回应用户的消息\n");
        prompt.append("2. 如果需要，可以请求其他Agent协助\n");
        prompt.append("3. 保持友好和协作的态度\n\n");

        return prompt.toString();
    }

    /**
     * 获取Agent名称
     */
    private String getAgentName(String agentId) {
        // 这里可以从缓存或AgentService获取
        // 简化处理，返回ID
        return agentId;
    }

    /**
     * 处理Agent响应
     *
     * @param roomId 聊天室ID
     * @param agentId Agent ID
     * @param agentName Agent名称
     * @param responseContent Agent响应内容
     * @param metadata 元数据
     */
    public void processAgentResponse(String roomId, String agentId, String agentName,
                                      String responseContent, java.util.Map<String, Object> metadata) {
        try {
            // 获取聊天室
            ChatRoom room = chatRoomService.getRoom(roomId);

            // 创建Agent消息
            ChatMessage agentMessage = ChatMessage.createAgentMessage(
                agentId,
                agentName,
                responseContent,
                metadata
            );

            // 保存到聊天室
            room.addMessage(agentMessage);

            // 推送消息
            pushService.pushMessage(roomId, agentMessage);
            pushService.pushTypingStatus(roomId, agentId, agentName, false);

            log.debug("Agent {} 的响应已推送到聊天室 {}", agentId, roomId);

        } catch (Exception e) {
            log.error("处理Agent {} 响应失败", agentId, e);
        }
    }
}
