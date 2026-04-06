package com.cat.cliagent.service;

import com.cat.cliagent.dto.CliAgentResponse;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Token使用统计服务接口
 *
 * 负责解析CLI输出中的Token使用信息并统计
 */
public interface TokenUsageService {

    /**
     * 解析并记录Token使用
     *
     * @param agentId Agent ID
     * @param output CLI输出内容
     * @param templateId 模板ID（用于获取解析规则）
     * @return 是否成功解析
     */
    boolean parseAndRecord(String agentId, String output, String templateId);

    /**
     * 获取Agent的Token使用统计
     *
     * @param agentId Agent ID
     * @return Token使用统计
     */
    TokenUsageStats getAgentStats(String agentId);

    /**
     * 获取Agent在指定时间范围内的Token使用记录
     *
     * @param agentId Agent ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return Token使用记录列表
     */
    List<TokenUsageRecord> getAgentRecords(String agentId, LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 获取系统整体Token使用统计
     *
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 系统Token使用统计
     */
    TokenUsageStats getSystemStats(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 直接记录Token使用（无需解析）
     *
     * @param agentId Agent ID
     * @param inputTokens 输入Token数
     * @param outputTokens 输出Token数
     * @return 是否成功记录
     */
    boolean recordTokenUsage(String agentId, Long inputTokens, Long outputTokens);

    /**
     * 清理过期的Token使用记录
     *
     * @param beforeTime 清理此时间之前的记录
     * @return 清理的记录数
     */
    int cleanupOldRecords(LocalDateTime beforeTime);

    /**
     * Token使用统计
     */
    record TokenUsageStats(
        String agentId,
        Long totalInputTokens,
        Long totalOutputTokens,
        Long totalTokens,
        Long recordCount,
        LocalDateTime firstRecordTime,
        LocalDateTime lastRecordTime
    ) {}

    /**
     * Token使用记录
     */
    record TokenUsageRecord(
        String id,
        String agentId,
        Long inputTokens,
        Long outputTokens,
        String source, // CLI输出来源标识
        LocalDateTime recordedAt
    ) {}
}