package com.nemall.support.client;

import com.nemall.support.dto.AgentEventDTO;
import com.nemall.support.dto.AgentTurnDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

@Component
@RequiredArgsConstructor
public class AgentClient {

    private final WebClient agentWebClient;

    public Flux<AgentEventDTO> streamGuide(Long userId, String text) {
        return stream(
                "/internal/guide/turns/stream",
                userId,
                text
        );
    }

    public Flux<AgentEventDTO> streamSupport(Long userId, String text) {
        return stream(
                "/internal/support/turns/stream",
                userId,
                text
        );
    }

    private Flux<AgentEventDTO> stream(
            String path,
            Long userId,
            String text) {

        AgentTurnDTO request =
                new AgentTurnDTO(userId.toString(), text);

        return agentWebClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(AgentEventDTO.class);
    }
}
