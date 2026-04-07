package com.cat.chatroom.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 添加Agent到聊天室请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddAgentRequest {

    /**
     * Agent ID
     */
    @NotBlank(message = "Agent ID不能为空")
    private String agentId;
}
