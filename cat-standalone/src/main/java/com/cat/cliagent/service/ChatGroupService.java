package com.cat.cliagent.service;

import java.util.List;

/**
 * 聊天群组服务接口
 *
 * 管理多Agent群聊功能，支持群组CRUD、消息发送、@提及和广播
 */
public interface ChatGroupService {

    /**
     * 创建聊天群组
     *
     * @param name 群组名称
     * @param description 群组描述
     * @param agentIds 群成员Agent ID列表
     * @return 群组信息
     */
    ChatGroupInfo createGroup(String name, String description, List<String> agentIds);

    /**
     * 更新聊天群组
     *
     * @param groupId 群组ID
     * @param name 群组名称
     * @param description 群组描述
     * @param agentIds 群成员Agent ID列表
     * @return 群组信息
     */
    ChatGroupInfo updateGroup(String groupId, String name, String description, List<String> agentIds);

    /**
     * 删除聊天群组
     *
     * @param groupId 群组ID
     */
    void deleteGroup(String groupId);

    /**
     * 获取群组详情
     *
     * @param groupId 群组ID
     * @return 群组信息
     */
    ChatGroupInfo getGroup(String groupId);

    /**
     * 获取所有群组
     *
     * @return 群组列表
     */
    List<ChatGroupInfo> listGroups();

    /**
     * 发送群聊消息（用户发送，@指定agent或广播）
     *
     * @param groupId 群组ID
     * @param content 消息内容
     * @param mentionedAgentIds @提及的Agent ID列表（为空则广播）
     * @return 消息信息
     */
    ChatMessageInfo sendUserMessage(String groupId, String content, List<String> mentionedAgentIds);

    /**
     * 获取群聊历史消息
     *
     * @param groupId 群组ID
     * @param limit 最大条数
     * @return 消息列表
     */
    List<ChatMessageInfo> getGroupMessages(String groupId, int limit);

    /**
     * 清空群聊历史消息
     *
     * @param groupId 群组ID
     */
    void clearGroupMessages(String groupId);

    /**
     * 群组信息
     */
    record ChatGroupInfo(
        String id,
        String name,
        String description,
        List<String> agentIds,
        List<AgentBrief> agents,
        String createdAt,
        String updatedAt
    ) {}

    /**
     * Agent简要信息
     */
    record AgentBrief(
        String id,
        String name,
        String status
    ) {}

    /**
     * 群聊消息信息
     */
    record ChatMessageInfo(
        String id,
        String groupId,
        String senderType,
        String senderAgentId,
        String senderName,
        String content,
        List<String> mentionedAgentIds,
        boolean broadcast,
        String createdAt
    ) {}
}
