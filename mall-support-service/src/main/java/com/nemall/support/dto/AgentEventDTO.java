package com.nemall.support.dto;

import lombok.Data;

/** Python SSE 中 data 字段携带的事件。 */
@Data
public class AgentEventDTO {

    private String type;

    private Payload payload;

    @Data
    public static class Payload {

        private String text;
    }
}
