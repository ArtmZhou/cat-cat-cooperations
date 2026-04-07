package com.cat.chatroom.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 创建聊天室请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChatRoomRequest {

    /**
     * 聊天室名称
     */
    @NotBlank(message = "聊天室名称不能为空")
    @Size(max = 100, message = "聊天室名称最多100个字符")
    private String name;

    /**
     * 聊天室描述
     */
    @Size(max = 500, message = "聊天室描述最多500个字符")
    private String description;

    /**
     * 成员Agent ID列表
     */
    @NotEmpty(message = "至少选择一个Agent成员")
    @Size(max = 10, message = "最多选择10个Agent成员")
    private List<String> agentIds;
}
