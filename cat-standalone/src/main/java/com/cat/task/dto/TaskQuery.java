package com.cat.task.dto;

import lombok.Data;

/**
 * Task查询条件
 */
@Data
public class TaskQuery {
    private String name;
    private String type;
    private String status;
    private Integer page = 1;
    private Integer pageSize = 20;
}