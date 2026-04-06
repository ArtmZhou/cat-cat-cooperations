package com.cat.standalone.service;

import com.cat.cliagent.dto.CliAgentResponse;
import com.cat.cliagent.service.CliAgentService;
import com.cat.cliagent.service.CliAgentTemplateService;
import com.cat.cliagent.service.CliOutputPushService;
import com.cat.cliagent.service.CliSessionService;
import com.cat.cliagent.service.TokenUsageService;
import com.cat.common.exception.BusinessException;
import com.cat.standalone.store.JsonFileStore;
import com.cat.standalone.store.entity.StoredCliAgent;
import com.cat.standalone.store.entity.StoredCliAgentOutputLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * CLI会话通信服务实现
 *
 * 使用 --print 模式进行每请求独立执行，支持多轮对话
 */
@Slf4j
@Service
public class LocalCliSessionService implements CliSessionService {

    private final JsonFileStore<StoredCliAgent> cliAgentStore;
    private final JsonFileStore<StoredCliAgentOutputLog> outputLogStore;
    private final CliAgentService cliAgentService;
    private final CliAgentTemplateService templateService;
    private final ObjectMapper objectMapper;

    @Lazy
    @Autowired
    private CliOutputPushService outputPushService;

    @Lazy
    @Autowired
    private TokenUsageService tokenUsageService;

    // 备用推送方法（防止outputPushService为null）
    private void safePushOutput(String agentId, String line) {
        // 保存到日志
        saveOutputLog(agentId, "output", line);
        if (outputPushService != null) {
            outputPushService.pushOutput(agentId, line);
        } else {
            log.warn("outputPushService is null, cannot push output for agent {}", agentId);
        }
    }

    private void safePushTextDelta(String agentId, String text) {
        // 保存到日志
        saveOutputLog(agentId, "text", text);
        if (outputPushService != null) {
            outputPushService.pushTextDelta(agentId, text);
        } else {
            log.warn("outputPushService is null, cannot push text delta for agent {}", agentId);
        }
    }

    private void safePushError(String agentId, String error) {
        // 保存到日志
        saveOutputLog(agentId, "error", error);
        if (outputPushService != null) {
            outputPushService.pushError(agentId, error);
        } else {
            log.warn("outputPushService is null, cannot push error for agent {}", agentId);
        }
    }

    private void saveOutputLog(String agentId, String type, String content) {
        if (content == null || content.isEmpty()) {
            return;
        }

        String id = UUID.randomUUID().toString();
        StoredCliAgentOutputLog outputLog = new StoredCliAgentOutputLog();
        outputLog.setId(id);
        outputLog.setAgentId(agentId);
        outputLog.setType(type);
        outputLog.setContent(content);
        outputLog.setTimestamp(LocalDateTime.now());

        outputLogStore.save(id, outputLog);

        // 定期清理旧日志
        cleanupOldOutputLogs(agentId);
    }

    /**
     * 清理旧输出日志，保持每个Agent最近100条
     */
    private void cleanupOldOutputLogs(String agentId) {
        List<StoredCliAgentOutputLog> logs = outputLogStore.find(
            l -> l.getAgentId().equals(agentId)
        );

        if (logs.size() > MAX_LOG_ENTRIES) {
            // 按时间倒序排列，删除最旧的
            logs.sort((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()));
            List<StoredCliAgentOutputLog> toDelete = logs.subList(MAX_LOG_ENTRIES, logs.size());
            for (StoredCliAgentOutputLog oldLog : toDelete) {
                outputLogStore.deleteById(oldLog.getId());
            }
        }
    }

    /**
     * 获取输出日志
     */
    public List<OutputLogEntry> getOutputLogs(String agentId, int limit) {
        List<StoredCliAgentOutputLog> logs = outputLogStore.find(
            l -> l.getAgentId().equals(agentId)
        );

        // 按时间升序排列
        logs.sort(Comparator.comparing(StoredCliAgentOutputLog::getTimestamp));

        if (logs.size() <= limit) {
            return logs.stream().map(this::toOutputLogEntry).collect(Collectors.toList());
        }

        return logs.subList(logs.size() - limit, logs.size()).stream()
            .map(this::toOutputLogEntry)
            .collect(Collectors.toList());
    }

    /**
     * 清空输出日志
     */
    public void clearOutputLogs(String agentId) {
        outputLogStore.delete(l -> l.getAgentId().equals(agentId));
    }

    /**
     * 将存储实体转换为OutputLogEntry
     */
    private OutputLogEntry toOutputLogEntry(StoredCliAgentOutputLog outputLog) {
        return new OutputLogEntry(outputLog.getType(), outputLog.getContent(), outputLog.getTimestamp());
    }

    private void safePushStatusChange(String agentId, String status) {
        if (outputPushService != null) {
            outputPushService.pushStatusChange(agentId, status);
        } else {
            log.warn("outputPushService is null, cannot push status change for agent {}", agentId);
        }
    }

    private void safePushDone(String agentId) {
        if (outputPushService != null) {
            outputPushService.pushDone(agentId);
        } else {
            log.warn("outputPushService is null, cannot push done for agent {}", agentId);
        }
    }

    private void safePushTokenUsage(String agentId, long inputTokens, long outputTokens) {
        if (outputPushService != null) {
            outputPushService.pushTokenUsage(agentId, inputTokens, outputTokens);
        } else {
            log.warn("outputPushService is null, cannot push token usage for agent {}", agentId);
        }
    }

    // 正在处理的Agent集合（防止并发请求）
    private final Set<String> processingAgents = ConcurrentHashMap.newKeySet();

    // 异步线程池
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // 会话统计
    private final Map<String, AtomicLong> linesReceivedCount = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> bytesSentCount = new ConcurrentHashMap<>();
    private final Map<String, String> lastErrors = new ConcurrentHashMap<>();

    // 输出日志最大保留条数（每个Agent）
    private static final int MAX_LOG_ENTRIES = 100;

    // 输出日志条目
    public record OutputLogEntry(String type, String content, LocalDateTime timestamp) {}

    // 加密前缀
    private static final String ENCRYPTION_PREFIX = "ENC:";

    public LocalCliSessionService(JsonFileStore<StoredCliAgent> cliAgentStore,
                                   JsonFileStore<StoredCliAgentOutputLog> outputLogStore,
                                   CliAgentService cliAgentService,
                                   CliAgentTemplateService templateService,
                                   ObjectMapper objectMapper) {
        this.cliAgentStore = cliAgentStore;
        this.outputLogStore = outputLogStore;
        this.cliAgentService = cliAgentService;
        this.templateService = templateService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean sendInput(String agentId, String input) {
        StoredCliAgent agent = cliAgentStore.findById(agentId)
            .orElseThrow(() -> new BusinessException(404, "CLI Agent不存在: " + agentId));

        // 检查Agent状态
        if (!"RUNNING".equals(agent.getStatus())) {
            throw new BusinessException(400, "Agent未处于运行状态，当前状态: " + agent.getStatus());
        }

        // 检查是否已有请求在处理中
        if (processingAgents.contains(agentId)) {
            throw new BusinessException(400, "Agent正在处理上一个请求，请等待完成");
        }

        // 更新状态为执行中
        agent.setStatus("EXECUTING");
        agent.setUpdatedAt(LocalDateTime.now());
        cliAgentStore.save(agentId, agent);

        // 推送状态变更
        safePushStatusChange(agentId, "EXECUTING");

        // 统计字节数
        bytesSentCount.computeIfAbsent(agentId, k -> new AtomicLong(0))
            .addAndGet(input.length());

        // 初始化行数统计
        linesReceivedCount.computeIfAbsent(agentId, k -> new AtomicLong(0));

        // 提交异步任务执行
        processingAgents.add(agentId);
        executor.submit(() -> executePrompt(agentId, agent, input));

        return true;
    }

    /**
     * 执行单条Prompt（--print模式）
     */
    private void executePrompt(String agentId, StoredCliAgent agent, String input) {
        Process process = null;
        try {
            // 构建命令
            List<String> command = buildCommand(agent);
            log.info("Executing command for agent {}: {}", agent.getName(), command);

            // 构建ProcessBuilder
            ProcessBuilder pb = new ProcessBuilder(command);

            // 设置环境变量
            Map<String, String> envVars = parseEnvVars(agent.getEnvVars());
            if (envVars != null) {
                pb.environment().putAll(envVars);
            }
            // 移除CLAUDECODE环境变量以允许嵌套运行
            pb.environment().remove("CLAUDECODE");

            // 设置工作目录
            if (agent.getWorkingDir() != null) {
                pb.directory(new File(agent.getWorkingDir()));
            }

            // 重定向错误流到标准输出
            pb.redirectErrorStream(true);

            // 启动进程
            process = pb.start();

            // 将输入写入stdin
            try (OutputStream os = process.getOutputStream()) {
                os.write(input.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }

            // 读取stdout输出（流式）
            readOutput(agentId, agent, process);

            // 等待进程结束（带超时）
            boolean finished = process.waitFor(300, TimeUnit.SECONDS);
            if (!finished) {
                log.warn("Process timeout for agent: {}", agentId);
                process.destroyForcibly();
                lastErrors.put(agentId, "请求超时");
                safePushError(agentId, "请求超时（300秒）");
            } else {
                int exitCode = process.exitValue();
                log.info("Process finished for agent {} with exit code: {}", agentId, exitCode);
            }

        } catch (Exception e) {
            log.error("Error executing prompt for agent: {}", agentId, e);
            lastErrors.put(agentId, e.getMessage());
            safePushError(agentId, "执行错误: " + e.getMessage());
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
            processingAgents.remove(agentId);

            // 恢复状态为RUNNING
            agent.setStatus("RUNNING");
            agent.setUpdatedAt(LocalDateTime.now());
            cliAgentStore.save(agentId, agent);

            // 推送状态变更和完成信号
            safePushStatusChange(agentId, "RUNNING");
            safePushDone(agentId);
        }
    }

    /**
     * 读取进程输出（支持stream-json格式解析）
     */
    private void readOutput(String agentId, StoredCliAgent agent, Process process) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                linesReceivedCount.get(agentId).incrementAndGet();
                log.info("Raw output from agent {}: {}", agentId, line);

                // 尝试解析为stream-json格式
                if (line.trim().startsWith("{")) {
                    parseAndPushStreamJson(agentId, agent, line);
                } else {
                    // 非JSON行，直接推送
                    log.info("Non-JSON line, pushing directly: {}", line);
                    safePushOutput(agentId, line);
                }
            }
        } catch (IOException e) {
            log.error("Error reading output for agent: {}", agentId, e);
        }
    }

    /**
     * 解析stream-json事件并推送到前端
     */
    private void parseAndPushStreamJson(String agentId, StoredCliAgent agent, String line) {
        try {
            JsonNode node = objectMapper.readTree(line);
            String type = node.path("type").asText("");

            switch (type) {
                case "system":
                    // 系统初始化事件，可能包含session_id
                    String sessionId = node.path("session_id").asText(null);
                    if (sessionId != null && agent.getSessionId() == null) {
                        agent.setSessionId(sessionId);
                        cliAgentStore.save(agentId, agent);
                        log.info("Captured session_id for agent {}: {}", agentId, sessionId);
                    }
                    break;

                case "assistant":
                    // 助手输出事件
                    String subtype = node.path("subtype").asText("");
                    log.info("Assistant event received: subtype={}", subtype);
                    if ("text".equals(subtype)) {
                        // 尝试多个可能的字段名
                        String text = "";
                        if (node.has("text")) {
                            text = node.path("text").asText("");
                        } else if (node.has("content")) {
                            text = node.path("content").asText("");
                        } else if (node.has("delta")) {
                            text = node.path("delta").asText("");
                        } else if (node.has("message")) {
                            text = node.path("message").asText("");
                        }
                        log.info("Assistant text content: [{}]", text.substring(0, Math.min(text.length(), 100)));
                        if (!text.isEmpty()) {
                            safePushTextDelta(agentId, text);
                        }
                    } else if ("thinking".equals(subtype)) {
                        String thinking = node.path("thinking").asText("");
                        if (!thinking.isEmpty()) {
                            safePushOutput(agentId, "💭 Thinking: " + thinking);
                        }
                    } else {
                        // 没有subtype或者是其他subtype，尝试直接提取text/content
                        String text = "";
                        if (node.has("text")) {
                            text = node.path("text").asText("");
                        } else if (node.has("content")) {
                            text = node.path("content").asText("");
                        } else if (node.has("message")) {
                            text = node.path("message").asText("");
                        }
                        log.info("Assistant other content: [{}]", text.substring(0, Math.min(text.length(), 100)));
                        if (!text.isEmpty()) {
                            safePushTextDelta(agentId, text);
                        }
                    }
                    break;

                case "tool_use":
                    // 工具使用事件
                    String toolName = node.path("name").asText("");
                    if (!toolName.isEmpty()) {
                        safePushOutput(agentId, "🔧 使用工具: " + toolName);
                    }
                    break;

                case "tool_result":
                    // 工具结果事件
                    String result = node.path("content").asText("");
                    if (!result.isEmpty()) {
                        // 工具结果通常不需要显示给用户，或者可以简化显示
                        log.debug("Tool result for agent {}: {}", agentId, result);
                    }
                    break;

                case "result":
                    // 最终结果事件
                    handleResultEvent(agentId, agent, node);
                    break;

                case "error":
                    // 错误事件
                    String errorMsg = node.path("error").asText("未知错误");
                    safePushError(agentId, errorMsg);
                    break;

                default:
                    // 未知类型，作为原始输出推送
                    log.debug("Unknown event type: {}", type);
                    safePushOutput(agentId, line);
            }
        } catch (Exception e) {
            // 解析失败，推送原始行
            log.debug("Failed to parse JSON line, pushing as raw: {}", line);
            safePushOutput(agentId, line);
        }
    }

    /**
     * 处理result事件
     */
    private void handleResultEvent(String agentId, StoredCliAgent agent, JsonNode node) {
        try {
            // 提取session_id
            String sessionId = node.path("session_id").asText(null);
            if (sessionId != null) {
                agent.setSessionId(sessionId);
                cliAgentStore.save(agentId, agent);
                log.info("Updated session_id for agent {}: {}", agentId, sessionId);
            }

            // 提取结果文本
            String resultText = node.path("result").asText("");
            if (!resultText.isEmpty()) {
                // result中包含完整响应，推送到前端
                log.info("Pushing result text for agent {}: length={}", agentId, resultText.length());
                safePushTextDelta(agentId, resultText);
            }

            // 提取token使用量
            long inputTokens = node.path("usage").path("input_tokens").asLong(0);
            long outputTokens = node.path("usage").path("output_tokens").asLong(0);
            if ((inputTokens > 0 || outputTokens > 0)) {
                safePushTokenUsage(agentId, inputTokens, outputTokens);
                // 记录到TokenUsageService
                if (tokenUsageService != null) {
                    tokenUsageService.recordTokenUsage(agentId, inputTokens, outputTokens);
                }
            }

            // 提取成本信息
            double costUsd = node.path("cost_usd").asDouble(0);
            long durationMs = node.path("duration_ms").asLong(0);
            log.info("Request completed for agent {}: cost=${}, duration={}ms, tokens={}/{}",
                agentId, costUsd, durationMs, inputTokens, outputTokens);

        } catch (Exception e) {
            log.error("Error handling result event for agent: {}", agentId, e);
        }
    }

    /**
     * 构建CLI命令（--print模式）
     */
    private List<String> buildCommand(StoredCliAgent agent) {
        List<String> command = new ArrayList<>();

        // 可执行文件路径
        String executablePath = agent.getExecutablePath();
        if (executablePath == null) {
            var template = templateService.getTemplate(agent.getTemplateId());
            executablePath = template.getExecutablePath();
        }
        command.add(executablePath);

        // 配置文件路径
        String configPath = agent.getConfigPath();
        if (configPath != null && !configPath.isEmpty()) {
            command.add("--settings");
            command.add(configPath);
        }

        // 其他启动参数
        List<String> args = parseArgs(agent.getArgs());
        if (args != null && !args.isEmpty()) {
            command.addAll(args);
        }

        // --print 模式（非交互式）
        command.add("--print");

        // stream-json 输出格式（用于结构化解析）
        command.add("--output-format");
        command.add("stream-json");

        // --verbose 是 stream-json 在 --print 模式下必需的
        command.add("--verbose");

        // 如果有sessionId，添加 --resume 参数
        if (agent.getSessionId() != null && !agent.getSessionId().isEmpty()) {
            command.add("--resume");
            command.add(agent.getSessionId());
        }

        log.info("Built per-request command for agent {}: {}", agent.getName(), command);
        return command;
    }

    private List<String> parseArgs(String argsJson) {
        if (argsJson == null || argsJson.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(argsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse args JSON: {}", argsJson, e);
            return Collections.emptyList();
        }
    }

    private Map<String, String> parseEnvVars(String envVarsJson) {
        if (envVarsJson == null || envVarsJson.isEmpty()) {
            return null;
        }
        try {
            Map<String, String> encrypted = objectMapper.readValue(envVarsJson, new TypeReference<Map<String, String>>() {});
            // 解密
            Map<String, String> decrypted = new HashMap<>();
            for (Map.Entry<String, String> entry : encrypted.entrySet()) {
                String value = entry.getValue();
                if (value != null && value.startsWith(ENCRYPTION_PREFIX)) {
                    try {
                        decrypted.put(entry.getKey(), new String(Base64.getDecoder().decode(value.substring(ENCRYPTION_PREFIX.length()))));
                    } catch (Exception e) {
                        decrypted.put(entry.getKey(), value);
                    }
                } else {
                    decrypted.put(entry.getKey(), value);
                }
            }
            return decrypted;
        } catch (JsonProcessingException e) {
            log.error("Failed to parse env vars JSON: {}", envVarsJson, e);
            return null;
        }
    }

    @Override
    public boolean sendInputWithStreaming(String agentId, String input,
                                           Consumer<String> outputConsumer,
                                           Consumer<String> errorConsumer) {
        // 在--print模式下，此方法是同步的，但为了保持接口兼容性，我们仍然返回true
        // 实际输出通过WebSocket推送
        return sendInput(agentId, input);
    }

    @Override
    public boolean startStreaming(String agentId,
                                   Consumer<String> outputConsumer,
                                   Consumer<String> errorConsumer) {
        // 在--print模式下，流式读取是每请求自动进行的
        // 此方法保持接口兼容性，不做任何操作
        log.debug("startStreaming called for agent {} (no-op in --print mode)", agentId);
        return true;
    }

    @Override
    public void stopStreaming(String agentId) {
        // 在--print模式下，无法真正"停止"流式读取，因为进程是每请求独立的
        // 但我们可以标记请求应该被取消
        log.debug("stopStreaming called for agent {} (no-op in --print mode)", agentId);
    }

    @Override
    public boolean isSessionActive(String agentId) {
        return processingAgents.contains(agentId);
    }

    @Override
    public SessionStatus getSessionStatus(String agentId) {
        StoredCliAgent agent = cliAgentStore.findById(agentId)
            .orElse(null);

        AtomicLong linesCount = linesReceivedCount.get(agentId);
        AtomicLong bytesCount = bytesSentCount.get(agentId);
        boolean isProcessing = processingAgents.contains(agentId);

        return new SessionStatus(
            agentId,
            isProcessing,
            agent != null && agent.getSessionId() != null,
            isProcessing,
            isProcessing,
            linesCount != null ? linesCount.get() : 0L,
            bytesCount != null ? bytesCount.get() : 0L,
            lastErrors.get(agentId)
        );
    }

    @Override
    public CliAgentResponse closeSession(String agentId) {
        StoredCliAgent agent = cliAgentStore.findById(agentId)
            .orElseThrow(() -> new BusinessException(404, "CLI Agent不存在: " + agentId));

        // 清除sessionId（这会丢弃对话上下文）
        agent.setSessionId(null);
        agent.setStatus("RUNNING");
        agent.setUpdatedAt(LocalDateTime.now());
        cliAgentStore.save(agentId, agent);

        // 清除统计
        linesReceivedCount.remove(agentId);
        bytesSentCount.remove(agentId);
        lastErrors.remove(agentId);

        log.info("Session closed for agent: {}", agentId);

        return cliAgentService.getAgent(agentId);
    }
}
