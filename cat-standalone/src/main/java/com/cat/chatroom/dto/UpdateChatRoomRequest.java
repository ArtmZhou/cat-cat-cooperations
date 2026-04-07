package com.cat.chatroom.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 更新聊天室请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateChatRoomRequest {

    /**
     * 聊天室名称
     */
    @Size(max = 100, message = "聊天室名称最多100个字符")
    private String name;

    /**
     * 聊天室描述
     */
    @Size(max = 500, message = "聊天室描述最多500个字符")
    private String description;

    /**
     * 成员Agent ID列表（可选，如果不传则不更新）
     */
    @Size(max = 10, message = "最多选择10个Agent成员")
    private List<String> agentIds;
}
