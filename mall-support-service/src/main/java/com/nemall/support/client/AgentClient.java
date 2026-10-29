package com.nemall.support.client;

import com.nemall.support.dto.AgentEventDTO;
import com.nemall.support.dto.AgentHandoffDTO;
import com.nemall.support.dto.AgentTurnDTO;
import com.nemall.support.dto.HandoffResultDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class AgentClient {

    private final WebClient agentWebClient;

    public Flux<AgentEventDTO> streamGuide(Long userId, String text, String authorization) {
        return stream(
                "/internal/guide/turns/stream",
                userId,
                text,
                authorization
        );
    }

    public Flux<AgentEventDTO> streamSupport(Long userId, String text, String authorization) {
        return stream(
                "/internal/support/turns/stream",
                userId,
                text,
                authorization
        );
    }

    public Mono<HandoffResultDTO> createSupportTicket(
            Long userId, String actionId, String authorization) {

        return agentWebClient.post()
                .uri("/internal/support/tickets")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new AgentHandoffDTO(userId.toString(), actionId))
                .retrieve()
                .bodyToMono(HandoffResultDTO.class);
    }

    private Flux<AgentEventDTO> stream(
            String path,
            Long userId,
            String text,
            String authorization) {

        AgentTurnDTO request =
                new AgentTurnDTO(userId.toString(), text);

        return agentWebClient.post()
                .uri(path)
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(AgentEventDTO.class);
    }
}
