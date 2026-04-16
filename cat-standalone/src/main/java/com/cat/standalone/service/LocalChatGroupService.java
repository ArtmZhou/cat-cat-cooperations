package com.cat.standalone.service;

import com.cat.cliagent.service.ChatGroupService;
import com.cat.cliagent.service.CliSessionService;
import com.cat.standalone.store.JsonFileStore;
import com.cat.standalone.store.entity.StoredChatGroup;
import com.cat.standalone.store.entity.StoredChatGroupMessage;
import com.cat.standalone.store.entity.StoredCliAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 聊天群组服务实现
 *
 * 支持多Agent群聊，消息广播、@提及和自动讨论（Agent博弈）功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalChatGroupService implements ChatGroupService {

    private final JsonFileStore<StoredChatGroup> chatGroupStore;
    private final JsonFileStore<StoredChatGroupMessage> chatGroupMessageStore;
    private final JsonFileStore<StoredCliAgent> cliAgentStore;
    private final CliSessionService cliSessionService;
    private final SimpMessagingTemplate messagingTemplate;

    private static final String TOPIC_GROUP = "/topic/chat-group/";
    private static final int MAX_MESSAGES_PER_GROUP = 200;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final int AUTO_DISCUSSION_DELAY_MS = 2000; // 自动讨论间隔延迟

    // 追踪agent当前所在的群聊上下文：agentId → groupId
    // 当agent在群聊中收到消息后，其输出会被转发到对应群聊
    private final Map<String, String> agentGroupContext = new ConcurrentHashMap<>();

    // 追踪每个agent在群聊中的累积输出：agentId → 累积文本
    private final Map<String, StringBuilder> agentGroupOutputBuffers = new ConcurrentHashMap<>();

    // ===== 自动讨论（博弈）相关状态 =====

    // 追踪每个群聊的自动讨论轮数计数：groupId → 当前轮数
    private final Map<String, Integer> autoDiscussionRoundCounter = new ConcurrentHashMap<>();

    // 追踪每个群聊的自动讨论是否活跃：groupId → boolean
    private final Map<String, Boolean> autoDiscussionActive = new ConcurrentHashMap<>();

    // 追踪每个群聊中最后一个完成的agentId：groupId → agentId
    private final Map<String, String> lastCompletedAgent = new ConcurrentHashMap<>();

    // 自动讨论的调度线程池
    private final ScheduledExecutorService autoDiscussionScheduler =
        Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "auto-discussion-scheduler");
            t.setDaemon(true);
            return t;
        });

    @jakarta.annotation.PreDestroy
    public void destroy() {
        autoDiscussionScheduler.shutdownNow();
    }

    private static String generateId(int length) {
        return UUID.randomUUID().toString().replace("-", "").substring(0, length);
    }

    @Override
    public ChatGroupInfo createGroup(String name, String description, List<String> agentIds) {
        String id = generateId(12);
        LocalDateTime now = LocalDateTime.now();

        StoredChatGroup group = new StoredChatGroup();
        group.setId(id);
        group.setName(name);
        group.setDescription(description);
        group.setAgentIds(agentIds != null ? new ArrayList<>(agentIds) : new ArrayList<>());
        group.setCreatedAt(now);
        group.setUpdatedAt(now);

        chatGroupStore.save(id, group);
        log.info("Created chat group: {} with {} agents", name, agentIds != null ? agentIds.size() : 0);

        return toGroupInfo(group);
    }

    @Override
    public ChatGroupInfo updateGroup(String groupId, String name, String description, List<String> agentIds) {
        StoredChatGroup group = chatGroupStore.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("群组不存在: " + groupId));

        if (name != null) group.setName(name);
        if (description != null) group.setDescription(description);
        if (agentIds != null) group.setAgentIds(new ArrayList<>(agentIds));
        group.setUpdatedAt(LocalDateTime.now());

        chatGroupStore.save(groupId, group);
        log.info("Updated chat group: {}", groupId);

        return toGroupInfo(group);
    }

    @Override
    public void deleteGroup(String groupId) {
        chatGroupStore.deleteById(groupId);
        // 清理该群组的消息
        chatGroupMessageStore.delete(msg -> groupId.equals(msg.getGroupId()));
        log.info("Deleted chat group and messages: {}", groupId);
    }

    @Override
    public ChatGroupInfo getGroup(String groupId) {
        StoredChatGroup group = chatGroupStore.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("群组不存在: " + groupId));
        return toGroupInfo(group);
    }

    @Override
    public List<ChatGroupInfo> listGroups() {
        return chatGroupStore.findAll().stream()
            .sorted(Comparator.comparing(StoredChatGroup::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
            .map(this::toGroupInfo)
            .collect(Collectors.toList());
    }

    @Override
    public ChatMessageInfo sendUserMessage(String groupId, String content, List<String> mentionedAgentIds) {
        StoredChatGroup group = chatGroupStore.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("群组不存在: " + groupId));

        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("消息内容不能为空");
        }

        boolean isBroadcast = mentionedAgentIds == null || mentionedAgentIds.isEmpty();

        // 用户发送消息时，重置自动讨论状态（用户介入）
        resetAutoDiscussionState(groupId);

        // 保存用户消息
        String msgId = generateId(16);
        StoredChatGroupMessage msg = new StoredChatGroupMessage();
        msg.setId(msgId);
        msg.setGroupId(groupId);
        msg.setSenderType("user");
        msg.setSenderName("我");
        msg.setContent(content);
        msg.setMentionedAgentIds(mentionedAgentIds != null ? new ArrayList<>(mentionedAgentIds) : new ArrayList<>());
        msg.setBroadcast(isBroadcast);
        msg.setCreatedAt(LocalDateTime.now());

        chatGroupMessageStore.save(msgId, msg);

        // 推送用户消息到群组WebSocket
        pushGroupMessage(groupId, toMessageInfo(msg));

        // 确定目标agent列表
        List<String> targetAgentIds;
        if (isBroadcast) {
            targetAgentIds = group.getAgentIds();
        } else {
            // 只发送给@提及的agent（且必须在群组中）
            targetAgentIds = mentionedAgentIds.stream()
                .filter(id -> group.getAgentIds().contains(id))
                .collect(Collectors.toList());
        }

        // 如果群组启用了自动讨论模式，初始化自动讨论状态
        if (group.isAutoDiscussion()) {
            autoDiscussionActive.put(groupId, true);
            autoDiscussionRoundCounter.put(groupId, 0);
            pushAutoDiscussionStatus(groupId, true);
            log.info("Auto-discussion activated for group {} with max {} rounds", groupId, group.getMaxAutoRounds());
        }

        // 向目标agent发送消息（使用CLI session input）
        for (String agentId : targetAgentIds) {
            sendToAgent(groupId, agentId, content, true);
        }

        // 限制消息数量
        trimGroupMessages(groupId);

        return toMessageInfo(msg);
    }

    @Override
    public List<ChatMessageInfo> getGroupMessages(String groupId, int limit) {
        return chatGroupMessageStore.find(msg -> groupId.equals(msg.getGroupId()))
            .stream()
            .sorted(Comparator.comparing(StoredChatGroupMessage::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())))
            .limit(limit > 0 ? limit : 100)
            .map(this::toMessageInfo)
            .collect(Collectors.toList());
    }

    @Override
    public void clearGroupMessages(String groupId) {
        chatGroupMessageStore.delete(msg -> groupId.equals(msg.getGroupId()));
        log.info("Cleared messages for group: {}", groupId);
    }

    /**
     * 添加agent响应消息（由WebSocket输出回调触发）
     */
    public void addAgentResponseMessage(String groupId, String agentId, String agentName, String content) {
        String msgId = generateId(16);
        StoredChatGroupMessage msg = new StoredChatGroupMessage();
        msg.setId(msgId);
        msg.setGroupId(groupId);
        msg.setSenderType("agent");
        msg.setSenderAgentId(agentId);
        msg.setSenderName(agentName);
        msg.setContent(content);
        msg.setMentionedAgentIds(new ArrayList<>());
        msg.setBroadcast(false);
        msg.setCreatedAt(LocalDateTime.now());

        chatGroupMessageStore.save(msgId, msg);
        pushGroupMessage(groupId, toMessageInfo(msg));
    }

    // ===== 内部方法 =====

    /**
     * 构建发送给Agent的prompt，包含群聊上下文
     * 让Agent能感知到群聊中其他参与者的消息
     */
    private String buildAgentPrompt(String groupId, String content, String currentAgentId) {
        // 获取最近的群聊消息作为上下文（排除空内容的占位消息）
        List<StoredChatGroupMessage> recentMessages = chatGroupMessageStore.find(
            msg -> groupId.equals(msg.getGroupId()) && msg.getContent() != null && !msg.getContent().isEmpty());

        recentMessages.sort(Comparator.comparing(StoredChatGroupMessage::getCreatedAt,
            Comparator.nullsLast(Comparator.naturalOrder())));

        // 最多取最近20条消息作为上下文
        int contextLimit = 20;
        int startIndex = Math.max(0, recentMessages.size() - contextLimit);
        List<StoredChatGroupMessage> contextMessages = recentMessages.subList(startIndex, recentMessages.size());

        // 如果没有历史消息，直接返回用户消息
        if (contextMessages.isEmpty()) {
            return content;
        }

        // 获取群组信息
        StoredChatGroup group = chatGroupStore.findById(groupId).orElse(null);
        String groupName = group != null ? group.getName() : "群聊";

        // 获取当前agent名称
        String currentAgentName = cliAgentStore.findById(currentAgentId)
            .map(StoredCliAgent::getName)
            .orElse(currentAgentId);

        // 构建群成员列表（排除当前agent）
        List<String> otherMemberNames = new ArrayList<>();
        if (group != null && group.getAgentIds() != null) {
            for (String aid : group.getAgentIds()) {
                if (!aid.equals(currentAgentId)) {
                    cliAgentStore.findById(aid).ifPresent(a -> otherMemberNames.add(a.getName()));
                }
            }
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("[群聊上下文] 你正在参与群聊「").append(groupName).append("」，你的身份是「").append(currentAgentName).append("」。\n");

        // 列出群成员
        if (!otherMemberNames.isEmpty()) {
            prompt.append("群聊成员：你(").append(currentAgentName).append(")、");
            prompt.append(String.join("、", otherMemberNames)).append("\n");
        }

        prompt.append("以下是最近的群聊记录，请结合上下文理解并回复最新消息：\n");
        prompt.append("---\n");

        for (StoredChatGroupMessage msg : contextMessages) {
            String senderLabel;
            if ("user".equals(msg.getSenderType())) {
                senderLabel = "用户(" + msg.getSenderName() + ")";
            } else if ("agent".equals(msg.getSenderType())) {
                // 标注是否是当前agent自己的消息
                if (currentAgentId.equals(msg.getSenderAgentId())) {
                    senderLabel = "你(" + msg.getSenderName() + ")";
                } else {
                    senderLabel = msg.getSenderName();
                }
            } else {
                senderLabel = "系统";
            }
            prompt.append(senderLabel).append(": ").append(msg.getContent()).append("\n");
        }

        prompt.append("---\n");
        prompt.append("最新消息 - 用户: ").append(content).append("\n");
        prompt.append("请以「").append(currentAgentName).append("」的身份回复。\n");

        // 如果群组开启了自动讨论，告知agent可以@其他成员
        if (group != null && group.isAutoDiscussion() && !otherMemberNames.isEmpty()) {
            prompt.append("【重要】如果你想让其他成员回应你，请在回复中使用 @成员名称。");
            prompt.append("只有被你@的成员才会回应。可用的成员：");
            for (int i = 0; i < otherMemberNames.size(); i++) {
                if (i > 0) prompt.append("、");
                prompt.append("@").append(otherMemberNames.get(i));
            }
            prompt.append("\n");
        }

        return prompt.toString();
    }

    private void addSystemMessage(String groupId, String content) {
        String msgId = generateId(16);
        StoredChatGroupMessage msg = new StoredChatGroupMessage();
        msg.setId(msgId);
        msg.setGroupId(groupId);
        msg.setSenderType("system");
        msg.setSenderName("系统");
        msg.setContent(content);
        msg.setMentionedAgentIds(new ArrayList<>());
        msg.setBroadcast(false);
        msg.setCreatedAt(LocalDateTime.now());

        chatGroupMessageStore.save(msgId, msg);
        pushGroupMessage(groupId, toMessageInfo(msg));
    }

    private void addAgentPlaceholderMessage(String groupId, String agentId, String agentName) {
        String msgId = generateId(16);
        StoredChatGroupMessage msg = new StoredChatGroupMessage();
        msg.setId(msgId);
        msg.setGroupId(groupId);
        msg.setSenderType("agent");
        msg.setSenderAgentId(agentId);
        msg.setSenderName(agentName);
        msg.setContent("");
        msg.setMentionedAgentIds(new ArrayList<>());
        msg.setBroadcast(false);
        msg.setCreatedAt(LocalDateTime.now());

        chatGroupMessageStore.save(msgId, msg);
        pushGroupMessage(groupId, toMessageInfo(msg));
    }

    private void pushGroupMessage(String groupId, ChatMessageInfo message) {
        String destination = TOPIC_GROUP + groupId + "/message";
        try {
            messagingTemplate.convertAndSend(destination, message);
            log.debug("Pushed group message to {}", destination);
        } catch (Exception e) {
            log.error("Failed to push group message to {}: {}", destination, e.getMessage());
        }
    }

    private void trimGroupMessages(String groupId) {
        List<StoredChatGroupMessage> groupMessages = chatGroupMessageStore.find(
            msg -> groupId.equals(msg.getGroupId()));

        if (groupMessages.size() > MAX_MESSAGES_PER_GROUP) {
            groupMessages.sort(Comparator.comparing(StoredChatGroupMessage::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));

            int toRemove = groupMessages.size() - MAX_MESSAGES_PER_GROUP;
            for (int i = 0; i < toRemove; i++) {
                chatGroupMessageStore.deleteById(groupMessages.get(i).getId());
            }
        }
    }

    private ChatGroupInfo toGroupInfo(StoredChatGroup group) {
        List<AgentBrief> agentBriefs = new ArrayList<>();
        if (group.getAgentIds() != null) {
            for (String agentId : group.getAgentIds()) {
                StoredCliAgent agent = cliAgentStore.findById(agentId).orElse(null);
                if (agent != null) {
                    agentBriefs.add(new AgentBrief(agent.getId(), agent.getName(), agent.getStatus()));
                }
            }
        }

        return new ChatGroupInfo(
            group.getId(),
            group.getName(),
            group.getDescription(),
            group.getAgentIds(),
            agentBriefs,
            group.isAutoDiscussion(),
            group.getMaxAutoRounds(),
            Boolean.TRUE.equals(autoDiscussionActive.get(group.getId())),
            group.getCreatedAt() != null ? group.getCreatedAt().format(FORMATTER) : null,
            group.getUpdatedAt() != null ? group.getUpdatedAt().format(FORMATTER) : null
        );
    }

    private ChatMessageInfo toMessageInfo(StoredChatGroupMessage msg) {
        return new ChatMessageInfo(
            msg.getId(),
            msg.getGroupId(),
            msg.getSenderType(),
            msg.getSenderAgentId(),
            msg.getSenderName(),
            msg.getContent(),
            msg.getMentionedAgentIds(),
            msg.isBroadcast(),
            msg.getCreatedAt() != null ? msg.getCreatedAt().format(FORMATTER) : null
        );
    }

    // ===== 群聊输出转发方法 =====

    /**
     * 获取agent当前所在的群聊ID
     *
     * @return 群聊ID，如果agent不在任何群聊上下文中则返回null
     */
    public String getAgentGroupContext(String agentId) {
        return agentGroupContext.get(agentId);
    }

    /**
     * 判断agent当前是否处于群聊上下文中
     */
    public boolean isAgentInGroupContext(String agentId) {
        return agentGroupContext.containsKey(agentId);
    }

    /**
     * 处理agent的流式文本输出，转发到群聊
     */
    public void handleAgentTextDelta(String agentId, String text) {
        String groupId = agentGroupContext.get(agentId);
        if (groupId == null) return;

        StringBuilder buffer = agentGroupOutputBuffers.get(agentId);
        if (buffer != null) {
            buffer.append(text);
        }

        StoredCliAgent agent = cliAgentStore.findById(agentId).orElse(null);
        String agentName = agent != null ? agent.getName() : agentId;

        // 推送流式输出到群聊topic
        pushGroupAgentOutput(groupId, agentId, agentName, "text_delta", text);
    }

    /**
     * 处理agent输出完成信号，保存最终消息，并触发自动讨论链
     */
    public void handleAgentDone(String agentId) {
        String groupId = agentGroupContext.get(agentId);
        if (groupId == null) return;

        StringBuilder buffer = agentGroupOutputBuffers.remove(agentId);
        agentGroupContext.remove(agentId);

        String finalContent = null;
        if (buffer != null && !buffer.isEmpty()) {
            finalContent = buffer.toString();

            // 解析agent输出中的@mention
            List<String> mentionedIds = parseAgentMentions(groupId, agentId, finalContent);

            // 更新最后一条该agent的占位消息内容和@提及信息
            updateLastAgentMessage(groupId, agentId, finalContent, mentionedIds);
        }

        // 推送完成信号到群聊topic
        StoredCliAgent agent = cliAgentStore.findById(agentId).orElse(null);
        String agentName = agent != null ? agent.getName() : agentId;
        pushGroupAgentOutput(groupId, agentId, agentName, "done", "");

        // 记录最后完成的agent
        lastCompletedAgent.put(groupId, agentId);

        // 检查是否需要触发自动讨论（异步延迟执行，避免阻塞当前线程）
        if (finalContent != null && !finalContent.isBlank()) {
            final String completedAgentId = agentId;
            final String completedContent = finalContent;
            // 解析agent输出中的@mention用于路由下一轮讨论
            final List<String> mentionedIds = parseAgentMentions(groupId, agentId, finalContent);
            autoDiscussionScheduler.schedule(
                () -> triggerAutoDiscussion(groupId, completedAgentId, completedContent, mentionedIds),
                AUTO_DISCUSSION_DELAY_MS, TimeUnit.MILLISECONDS
            );
        }
    }

    /**
     * 处理agent在群聊中的错误输出，并清理群聊上下文
     */
    public void handleAgentError(String agentId, String error) {
        String groupId = agentGroupContext.remove(agentId);
        if (groupId == null) return;

        StringBuilder buffer = agentGroupOutputBuffers.remove(agentId);
        if (buffer != null && !buffer.isEmpty()) {
            updateLastAgentMessage(groupId, agentId, buffer.toString(), null);
        }

        StoredCliAgent agent = cliAgentStore.findById(agentId).orElse(null);
        String agentName = agent != null ? agent.getName() : agentId;
        String errorMessage = error != null && !error.isBlank() ? error : "未知错误";

        addSystemMessage(groupId, "⚠️ " + agentName + " 执行失败: " + errorMessage);
        pushGroupAgentOutput(groupId, agentId, agentName, "error", errorMessage);
    }

    /**
     * 更新该agent在群聊中的最后一条消息内容（含@提及信息）
     */
    private void updateLastAgentMessage(String groupId, String agentId, String content, List<String> mentionedAgentIds) {
        List<StoredChatGroupMessage> groupMessages = chatGroupMessageStore.find(
            msg -> groupId.equals(msg.getGroupId()) &&
                   "agent".equals(msg.getSenderType()) &&
                   agentId.equals(msg.getSenderAgentId()));

        if (!groupMessages.isEmpty()) {
            groupMessages.sort(Comparator.comparing(StoredChatGroupMessage::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));
            StoredChatGroupMessage lastMsg = groupMessages.get(groupMessages.size() - 1);
            lastMsg.setContent(content);
            if (mentionedAgentIds != null && !mentionedAgentIds.isEmpty()) {
                lastMsg.setMentionedAgentIds(new ArrayList<>(mentionedAgentIds));
            }
            chatGroupMessageStore.save(lastMsg.getId(), lastMsg);
        }
    }

    private void pushGroupAgentOutput(String groupId, String agentId, String agentName, String type, String content) {
        String destination = TOPIC_GROUP + groupId + "/agent-output";
        try {
            Map<String, String> output = new HashMap<>();
            output.put("agentId", agentId);
            output.put("agentName", agentName);
            output.put("type", type);
            output.put("content", content);
            messagingTemplate.convertAndSend(destination, output);
            log.debug("Pushed group agent output to {}: agent={}, type={}", destination, agentId, type);
        } catch (Exception e) {
            log.error("Failed to push group agent output to {}: {}", destination, e.getMessage());
        }
    }

    // ===== 自动讨论（Agent博弈）功能 =====

    /**
     * 向指定agent发送群聊消息（抽取的通用方法）
     */
    private void sendToAgent(String groupId, String agentId, String content, boolean isUserMessage) {
        try {
            StoredCliAgent agent = cliAgentStore.findById(agentId).orElse(null);
            if (agent == null) {
                log.warn("Agent not found: {}", agentId);
                return;
            }

            if (!"RUNNING".equals(agent.getStatus())) {
                log.warn("Agent {} is not running (status: {}), skipping", agentId, agent.getStatus());
                addSystemMessage(groupId, "⚠️ " + agent.getName() + " 未运行，消息未送达");
                return;
            }

            // 构建发送给agent的消息（包含群聊上下文）
            String agentPrompt = isUserMessage
                ? buildAgentPrompt(groupId, content, agentId)
                : buildAutoDiscussionPrompt(groupId, agentId);

            boolean sent = cliSessionService.sendInput(agentId, agentPrompt);

            if (sent) {
                String logContent = content != null ? content.substring(0, Math.min(50, content.length())) : "[auto-discussion]";
                log.info("Sent group message to agent {}: {}", agentId, logContent);
                // 注册agent的群聊上下文（用于输出转发）
                agentGroupContext.put(agentId, groupId);
                agentGroupOutputBuffers.put(agentId, new StringBuilder());
                // 预添加一条agent空消息（用于流式更新）
                addAgentPlaceholderMessage(groupId, agentId, agent.getName());
            } else {
                log.warn("Failed to send message to agent {}", agentId);
                addSystemMessage(groupId, "❌ 发送给 " + agent.getName() + " 失败");
            }
        } catch (Exception e) {
            log.error("Error sending message to agent {}: {}", agentId, e.getMessage());
            StoredCliAgent agent = cliAgentStore.findById(agentId).orElse(null);
            String agentName = agent != null ? agent.getName() : agentId;
            addSystemMessage(groupId, "❌ 发送给 " + agentName + " 出错: " + e.getMessage());
        }
    }

    /**
     * 触发自动讨论：当一个agent完成回复后，只触发其@提及的agent进行回复。
     *
     * 路由策略（纯@驱动）：
     * - 如果agent在输出中@了特定agent → 只触发被@的agent
     * - 如果没有@任何agent → 讨论链自然结束（不自动轮流）
     */
    private void triggerAutoDiscussion(String groupId, String completedAgentId, String completedContent, List<String> mentionedAgentIds) {
        // 检查自动讨论是否仍然活跃
        if (!Boolean.TRUE.equals(autoDiscussionActive.get(groupId))) {
            log.debug("Auto-discussion not active for group {}, skipping", groupId);
            return;
        }

        // 检查群组是否存在且开启了自动讨论
        StoredChatGroup group = chatGroupStore.findById(groupId).orElse(null);
        if (group == null || !group.isAutoDiscussion()) {
            log.debug("Group {} not found or auto-discussion disabled", groupId);
            return;
        }

        // 检查轮数限制
        int currentRound = autoDiscussionRoundCounter.getOrDefault(groupId, 0);
        if (currentRound >= group.getMaxAutoRounds()) {
            log.info("Auto-discussion reached max rounds ({}) for group {}", group.getMaxAutoRounds(), groupId);
            autoDiscussionActive.put(groupId, false);
            addSystemMessage(groupId, "🔄 自动讨论已达到最大轮数 (" + group.getMaxAutoRounds() + " 轮)，讨论结束");
            pushAutoDiscussionStatus(groupId, false);
            return;
        }

        // 检查是否还有其他agent正在执行中（等待所有agent完成当前轮）
        boolean anyAgentExecuting = group.getAgentIds().stream()
            .anyMatch(agentGroupContext::containsKey);
        if (anyAgentExecuting) {
            log.debug("Other agents still executing in group {}, waiting for all to complete", groupId);
            return;
        }

        // 纯@驱动：只有被@的agent才会响应
        if (mentionedAgentIds == null || mentionedAgentIds.isEmpty()) {
            // 没有@任何agent，讨论链自然结束
            StoredCliAgent completedAgent = cliAgentStore.findById(completedAgentId).orElse(null);
            String completedName = completedAgent != null ? completedAgent.getName() : completedAgentId;
            log.info("Auto-discussion: {} did not @mention anyone, discussion chain paused", completedName);
            addSystemMessage(groupId, "💬 " + completedName + " 没有@其他成员，讨论暂停。你可以发送新消息继续讨论");
            autoDiscussionActive.put(groupId, false);
            pushAutoDiscussionStatus(groupId, false);
            return;
        }

        // 过滤：必须在群组内，排除自己@自己
        List<String> nextAgentIds = mentionedAgentIds.stream()
            .filter(id -> group.getAgentIds().contains(id))
            .filter(id -> !id.equals(completedAgentId))
            .distinct()
            .collect(Collectors.toList());

        if (nextAgentIds.isEmpty()) {
            log.debug("No valid target agents from @mentions in group {}", groupId);
            autoDiscussionActive.put(groupId, false);
            pushAutoDiscussionStatus(groupId, false);
            return;
        }

        // 检查目标agent是否可用（RUNNING状态）
        List<String> availableAgentIds = nextAgentIds.stream()
            .filter(id -> {
                StoredCliAgent a = cliAgentStore.findById(id).orElse(null);
                return a != null && "RUNNING".equals(a.getStatus());
            })
            .collect(Collectors.toList());

        if (availableAgentIds.isEmpty()) {
            log.info("No available agents to continue auto-discussion in group {}", groupId);
            autoDiscussionActive.put(groupId, false);
            addSystemMessage(groupId, "⚠️ 被@的Agent未运行，讨论暂停");
            pushAutoDiscussionStatus(groupId, false);
            return;
        }

        // 递增轮数
        autoDiscussionRoundCounter.put(groupId, currentRound + 1);

        StoredCliAgent completedAgent = cliAgentStore.findById(completedAgentId).orElse(null);
        String completedAgentName = completedAgent != null ? completedAgent.getName() : completedAgentId;
        String mentionNames = availableAgentIds.stream()
            .map(id -> cliAgentStore.findById(id).map(StoredCliAgent::getName).orElse(id))
            .collect(Collectors.joining(", "));
        log.info("Auto-discussion round {} in group {}: {} @mentioned [{}]",
            currentRound + 1, groupId, completedAgentName, mentionNames);

        // 触发被@的agent回复
        for (String nextAgentId : availableAgentIds) {
            sendToAgent(groupId, nextAgentId, completedContent, false);
        }
    }

    /**
     * 从agent输出中解析@mention（@AgentName形式）。
     * 将检测到的agent名称映射回agent ID。
     */
    private List<String> parseAgentMentions(String groupId, String senderAgentId, String content) {
        if (content == null || content.isBlank()) return new ArrayList<>();

        StoredChatGroup group = chatGroupStore.findById(groupId).orElse(null);
        if (group == null || group.getAgentIds() == null) return new ArrayList<>();

        // 构建agentName→agentId映射（排除发送者自己）
        Map<String, String> nameToId = new HashMap<>();
        for (String aid : group.getAgentIds()) {
            if (aid.equals(senderAgentId)) continue;
            cliAgentStore.findById(aid).ifPresent(agent ->
                nameToId.put(agent.getName(), agent.getId()));
        }

        if (nameToId.isEmpty()) return new ArrayList<>();

        // 在内容中查找 @AgentName 模式
        List<String> mentioned = new ArrayList<>();
        for (Map.Entry<String, String> entry : nameToId.entrySet()) {
            String name = entry.getKey();
            String id = entry.getValue();
            // 匹配 @AgentName（后面跟空格、标点、换行或末尾）
            if (content.contains("@" + name)) {
                mentioned.add(id);
            }
        }

        return mentioned;
    }

    /**
     * 构建自动讨论模式下的Agent Prompt
     * 与普通的buildAgentPrompt不同，这里强调agent应该回应其他agent的观点，
     * 并告知其如何@mention群内其他agent
     */
    private String buildAutoDiscussionPrompt(String groupId, String currentAgentId) {
        // 获取最近的群聊消息作为上下文（排除空内容的占位消息）
        List<StoredChatGroupMessage> recentMessages = chatGroupMessageStore.find(
            msg -> groupId.equals(msg.getGroupId()) && msg.getContent() != null && !msg.getContent().isEmpty());

        recentMessages.sort(Comparator.comparing(StoredChatGroupMessage::getCreatedAt,
            Comparator.nullsLast(Comparator.naturalOrder())));

        // 最多取最近20条消息作为上下文
        int contextLimit = 20;
        int startIndex = Math.max(0, recentMessages.size() - contextLimit);
        List<StoredChatGroupMessage> contextMessages = recentMessages.subList(startIndex, recentMessages.size());

        // 获取群组信息
        StoredChatGroup group = chatGroupStore.findById(groupId).orElse(null);
        String groupName = group != null ? group.getName() : "群聊";

        // 获取当前agent名称
        String currentAgentName = cliAgentStore.findById(currentAgentId)
            .map(StoredCliAgent::getName)
            .orElse(currentAgentId);

        // 构建群成员列表（排除当前agent）
        List<String> otherMemberNames = new ArrayList<>();
        if (group != null && group.getAgentIds() != null) {
            for (String aid : group.getAgentIds()) {
                if (!aid.equals(currentAgentId)) {
                    cliAgentStore.findById(aid).ifPresent(a -> otherMemberNames.add(a.getName()));
                }
            }
        }

        // 找到最后一条agent消息作为讨论焦点
        StoredChatGroupMessage lastAgentMsg = null;
        for (int i = contextMessages.size() - 1; i >= 0; i--) {
            StoredChatGroupMessage msg = contextMessages.get(i);
            if ("agent".equals(msg.getSenderType()) && !currentAgentId.equals(msg.getSenderAgentId())) {
                lastAgentMsg = msg;
                break;
            }
        }

        StringBuilder prompt = new StringBuilder();
        prompt.append("[群聊讨论模式] 你正在参与群聊「").append(groupName).append("」的讨论，你的身份是「").append(currentAgentName).append("」。\n");
        prompt.append("这是一场多Agent讨论/辩论，你需要认真思考并回应其他参与者的观点。\n\n");

        // 列出所有群成员
        prompt.append("群聊成员：你(").append(currentAgentName).append(")");
        if (!otherMemberNames.isEmpty()) {
            prompt.append("、").append(String.join("、", otherMemberNames));
        }
        prompt.append("\n\n");

        prompt.append("以下是最近的群聊记录：\n");
        prompt.append("---\n");

        for (StoredChatGroupMessage msg : contextMessages) {
            String senderLabel;
            if ("user".equals(msg.getSenderType())) {
                senderLabel = "用户(" + msg.getSenderName() + ")";
            } else if ("agent".equals(msg.getSenderType())) {
                if (currentAgentId.equals(msg.getSenderAgentId())) {
                    senderLabel = "你(" + msg.getSenderName() + ")";
                } else {
                    senderLabel = msg.getSenderName();
                }
            } else {
                senderLabel = "系统";
            }
            prompt.append(senderLabel).append(": ").append(msg.getContent()).append("\n");
        }

        prompt.append("---\n\n");

        if (lastAgentMsg != null) {
            prompt.append("请针对「").append(lastAgentMsg.getSenderName()).append("」的最新发言进行回应。\n");
        }

        prompt.append("要求：\n");
        prompt.append("1. 以「").append(currentAgentName).append("」的身份发表你的观点\n");
        prompt.append("2. 你可以赞同、反驳、补充或提出新的角度\n");
        prompt.append("3. 保持讨论的深度和质量，避免简单重复他人观点\n");
        prompt.append("4. 回复要简洁有力，控制在合理的篇幅内\n");
        if (!otherMemberNames.isEmpty()) {
            prompt.append("5. 【重要】你必须在回复中使用 @成员名称 来指定你希望谁来回应你。");
            prompt.append("只有被你@的成员才会看到并回应你的消息。\n");
            prompt.append("   可用的成员：");
            for (int i = 0; i < otherMemberNames.size(); i++) {
                if (i > 0) prompt.append("、");
                prompt.append("@").append(otherMemberNames.get(i));
            }
            prompt.append("\n");
            prompt.append("   你可以@一个或多个成员。例如在回复末尾加上 @").append(otherMemberNames.get(0));
            if (otherMemberNames.size() > 1) {
                prompt.append(" 或同时 @").append(otherMemberNames.get(0)).append(" @").append(otherMemberNames.get(1));
            }
            prompt.append("\n");
            prompt.append("   如果你认为讨论已经充分或达成共识，可以不@任何人，讨论将自然结束。\n");
        }

        return prompt.toString();
    }

    /**
     * 重置自动讨论状态（用户介入时调用）
     */
    private void resetAutoDiscussionState(String groupId) {
        autoDiscussionActive.remove(groupId);
        autoDiscussionRoundCounter.remove(groupId);
        lastCompletedAgent.remove(groupId);
    }

    /**
     * 推送自动讨论状态变更到前端
     */
    private void pushAutoDiscussionStatus(String groupId, boolean running) {
        String destination = TOPIC_GROUP + groupId + "/auto-discussion-status";
        try {
            Map<String, Object> status = new HashMap<>();
            status.put("running", running);
            status.put("groupId", groupId);
            messagingTemplate.convertAndSend(destination, status);
            log.debug("Pushed auto-discussion status to {}: running={}", destination, running);
        } catch (Exception e) {
            log.error("Failed to push auto-discussion status to {}: {}", destination, e.getMessage());
        }
    }

    @Override
    public void stopAutoDiscussion(String groupId) {
        boolean wasActive = Boolean.TRUE.equals(autoDiscussionActive.get(groupId));
        resetAutoDiscussionState(groupId);
        if (wasActive) {
            addSystemMessage(groupId, "🛑 用户中断了自动讨论");
            pushAutoDiscussionStatus(groupId, false);
        }
        log.info("Auto-discussion stopped for group {}", groupId);
    }

    @Override
    public boolean isAutoDiscussionRunning(String groupId) {
        return Boolean.TRUE.equals(autoDiscussionActive.get(groupId));
    }

    @Override
    public ChatGroupInfo updateAutoDiscussionSettings(String groupId, Boolean autoDiscussion, Integer maxAutoRounds) {
        StoredChatGroup group = chatGroupStore.findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("群组不存在: " + groupId));

        if (autoDiscussion != null) {
            group.setAutoDiscussion(autoDiscussion);
            if (!autoDiscussion) {
                // 关闭自动讨论时停止正在进行的讨论
                stopAutoDiscussion(groupId);
            }
        }
        if (maxAutoRounds != null && maxAutoRounds > 0) {
            group.setMaxAutoRounds(maxAutoRounds);
        }
        group.setUpdatedAt(LocalDateTime.now());
        chatGroupStore.save(groupId, group);

        log.info("Updated auto-discussion settings for group {}: autoDiscussion={}, maxAutoRounds={}",
            groupId, group.isAutoDiscussion(), group.getMaxAutoRounds());
        return toGroupInfo(group);
    }
}
