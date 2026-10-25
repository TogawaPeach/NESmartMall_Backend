package com.nemall.support.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 发给 Python Agent 的可信用户 ID 和本轮消息。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentTurnDTO {

    private String userId;

    private String text;
}
