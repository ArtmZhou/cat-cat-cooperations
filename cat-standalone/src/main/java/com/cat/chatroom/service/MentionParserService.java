package com.cat.chatroom.service;

import com.cat.cliagent.dto.CliAgentResponse;
import com.cat.cliagent.service.CliAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @提及解析服务
 *
 * 解析消息中的@提及语法，支持按Agent名称或ID提及
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MentionParserService {

    private final CliAgentService cliAgentService;

    // @提及匹配模式：支持中英文、数字、下划线、连字符
    private static final Pattern MENTION_PATTERN = Pattern.compile("@([\\w\\-\\u4e00-\\u9fa5]+)");

    /**
     * 解析消息中的@提及
     *
     * @param content 消息内容
     * @param availableAgentIds 可用的Agent ID列表
     * @return 被提及的Agent ID列表
     */
    public List<String> parseMentions(String content, List<String> availableAgentIds) {
        if (content == null || content.isEmpty() || availableAgentIds == null || availableAgentIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<String> mentions = new ArrayList<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);

        while (matcher.find()) {
            String mention = matcher.group(1);
            log.debug("解析到@提及: {}", mention);

            // 尝试匹配Agent ID或名称
            String matchedAgentId = findMatchingAgent(mention, availableAgentIds);
            if (matchedAgentId != null && !mentions.contains(matchedAgentId)) {
                mentions.add(matchedAgentId);
                log.debug("匹配到Agent: {} -> {}", mention, matchedAgentId);
            }
        }

        return mentions;
    }

    /**
     * 检查消息是否包含@提及
     */
    public boolean hasMentions(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        return MENTION_PATTERN.matcher(content).find();
    }

    /**
     * 提取所有@提及的原始文本
     */
    public List<String> extractMentionTexts(String content) {
        List<String> mentions = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return mentions;
        }

        Matcher matcher = MENTION_PATTERN.matcher(content);
        while (matcher.find()) {
            mentions.add(matcher.group(1));
        }

        return mentions.stream().distinct().collect(Collectors.toList());
    }

    /**
     * 将消息内容中的@提及替换为高亮格式（用于前端展示）
     */
    public String highlightMentions(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }

        return MENTION_PATTERN.matcher(content)
            .replaceAll("<span class=\"mention highlight\">@$1</span>");
    }

    /**
     * 查找匹配的Agent
     *
     * 匹配规则：
     * 1. 完全匹配Agent ID
     * 2. 完全匹配Agent名称
     * 3. Agent名称前缀匹配
     */
    private String findMatchingAgent(String mention, List<String> availableAgentIds) {
        // 1. 首先尝试完全匹配Agent ID
        for (String agentId : availableAgentIds) {
            if (agentId.equals(mention)) {
                return agentId;
            }
        }

        // 2. 尝试匹配Agent名称
        for (String agentId : availableAgentIds) {
            try {
                CliAgentResponse agent = cliAgentService.getAgent(agentId);
                if (agent != null) {
                    String agentName = agent.getName();
                    // 完全匹配名称
                    if (agentName.equals(mention)) {
                        return agentId;
                    }
                    // 前缀匹配（如输入@Code，匹配CodeReviewer）
                    if (agentName.toLowerCase().startsWith(mention.toLowerCase())) {
                        return agentId;
                    }
                }
            } catch (Exception e) {
                log.warn("获取Agent信息失败: {}", agentId, e);
            }
        }

        return null;
    }

    /**
     * 获取建议的Agent列表（用于@提及自动完成）
     *
     * @param prefix 用户输入的前缀
     * @param availableAgentIds 可用的Agent ID列表
     * @param limit 最大返回数量
     * @return 匹配的Agent信息列表
     */
    public List<AgentSuggestion> getAgentSuggestions(String prefix, List<String> availableAgentIds, int limit) {
        List<AgentSuggestion> suggestions = new ArrayList<>();
        if (availableAgentIds == null || availableAgentIds.isEmpty()) {
            return suggestions;
        }

        String lowerPrefix = prefix != null ? prefix.toLowerCase() : "";

        for (String agentId : availableAgentIds) {
            try {
                CliAgentResponse agent = cliAgentService.getAgent(agentId);
                if (agent != null) {
                    String agentName = agent.getName();
                    String lowerName = agentName.toLowerCase();

                    // 匹配条件：ID或名称包含前缀
                    boolean matches = lowerName.contains(lowerPrefix)
                        || agentId.toLowerCase().contains(lowerPrefix);

                    if (matches || lowerPrefix.isEmpty()) {
                        suggestions.add(new AgentSuggestion(
                            agentId,
                            agentName,
                            agent.getCliType(),
                            getAgentAvatar(agent)
                        ));

                        if (suggestions.size() >= limit) {
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("获取Agent信息失败: {}", agentId, e);
            }
        }

        return suggestions;
    }

    private String getAgentAvatar(CliAgentResponse agent) {
        // 根据CLI类型返回不同的emoji
        if (agent.getCliType() != null) {
            return switch (agent.getCliType().toLowerCase()) {
                case "claude" -> "🟠";
                case "opencode" -> "🔵";
                default -> "🤖";
            };
        }
        return "🤖";
    }

    /**
     * Agent建议信息
     */
    public record AgentSuggestion(
        String id,
        String name,
        String type,
        String avatar
    ) {}
}
