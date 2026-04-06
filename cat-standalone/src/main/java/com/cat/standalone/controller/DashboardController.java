package com.cat.standalone.controller;

import com.cat.standalone.store.JsonFileStore;
import com.cat.standalone.store.entity.StoredAgent;
import com.cat.standalone.store.entity.StoredTask;
import com.cat.common.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘统计控制器
 */
@Tag(name = "仪表盘统计", description = "系统统计数据")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final JsonFileStore<StoredAgent> agentStore;
    private final JsonFileStore<StoredTask> taskStore;

    @Operation(summary = "获取仪表盘统计数据")
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();

        // Agent统计
        List<StoredAgent> agents = agentStore.findAll();
        stats.put("agentCount", agents.size());
        stats.put("agentOnline", agents.stream().filter(a -> "ONLINE".equals(a.getStatus())).count());
        stats.put("agentOffline", agents.stream().filter(a -> "OFFLINE".equals(a.getStatus())).count());
        stats.put("agentBusy", agents.stream().filter(a -> "BUSY".equals(a.getStatus())).count());
        stats.put("agentDisabled", agents.stream().filter(a -> "DISABLED".equals(a.getStatus())).count());

        // Task统计
        List<StoredTask> tasks = taskStore.findAll();
        stats.put("taskCount", tasks.size());
        stats.put("taskPending", tasks.stream().filter(t -> "PENDING".equals(t.getStatus())).count());
        stats.put("taskRunning", tasks.stream().filter(t -> "RUNNING".equals(t.getStatus())).count());
        stats.put("taskCompleted", tasks.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count());
        stats.put("taskFailed", tasks.stream().filter(t -> "FAILED".equals(t.getStatus())).count());
        stats.put("taskCancelled", tasks.stream().filter(t -> "CANCELLED".equals(t.getStatus())).count());

        // 成功率计算
        long totalFinished = tasks.stream()
            .filter(t -> "COMPLETED".equals(t.getStatus()) || "FAILED".equals(t.getStatus()))
            .count();
        long completed = tasks.stream().filter(t -> "COMPLETED".equals(t.getStatus())).count();
        double successRate = totalFinished > 0 ? (completed * 100.0 / totalFinished) : 0;
        stats.put("successRate", Math.round(successRate * 100) / 100.0); // 保留两位小数

        // 平均耗时计算（毫秒）
        double avgDuration = tasks.stream()
            .filter(t -> "COMPLETED".equals(t.getStatus()) && t.getStartedAt() != null && t.getCompletedAt() != null)
            .mapToLong(t -> {
                long start = t.getStartedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                long end = t.getCompletedAt().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
                return end - start;
            })
            .average()
            .orElse(0.0);
        stats.put("avgDuration", Math.round(avgDuration));

        return ApiResponse.success(stats);
    }
}