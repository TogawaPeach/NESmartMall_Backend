package com.nemall.support.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

/** Python SSE 中 data 字段携带的事件。 */
@Data
public class AgentEventDTO {

    private String type;

    private Payload payload;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Payload {

        private String text;

        private String kind;

        private String label;

        private String actionId;
    }
}
