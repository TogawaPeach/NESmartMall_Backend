package com.nemall.support.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 发给 Python 的可信用户标识与按钮动作标识。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentHandoffDTO {

    private String userId;

    private String actionId;
}
