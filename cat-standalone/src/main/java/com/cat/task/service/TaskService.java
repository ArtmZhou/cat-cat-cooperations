package com.cat.task.service;

import com.cat.task.dto.*;
import com.cat.common.model.PageResult;
import java.util.List;

/**
 * Task服务接口
 */
public interface TaskService {
    TaskResponse createTask(CreateTaskRequest request, String userId);
    TaskResponse updateTask(String taskId, UpdateTaskRequest request);
    void deleteTask(String taskId);
    TaskResponse getTask(String taskId);
    PageResult<TaskResponse> listTasks(TaskQuery query);
    TaskResponse assignTask(String taskId, List<String> agentIds);
    TaskResponse startTask(String taskId);
    TaskResponse completeTask(String taskId, String output);
    TaskResponse failTask(String taskId, String errorMessage);
    TaskResponse cancelTask(String taskId);
    TaskResponse retryTask(String taskId);
    void addTaskLog(String taskId, String agentId, String level, String message);
    List<TaskLogResponse> getTaskLogs(String taskId);
}