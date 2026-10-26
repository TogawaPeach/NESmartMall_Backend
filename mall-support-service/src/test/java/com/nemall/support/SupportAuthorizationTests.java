package com.nemall.support;

import com.nemall.support.client.AgentClient;
import com.nemall.support.controller.SupportController;
import com.nemall.support.service.impl.SupportServiceImpl;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SupportAuthorizationTests {

    private HttpServer python;
    private MockMvc mvc;
    private final BlockingQueue<ReceivedRequest> received = new LinkedBlockingQueue<>();

    @BeforeEach
    void setUp() throws Exception {
        python = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        python.createContext("/internal/", exchange -> {
            received.add(new ReceivedRequest(
                    exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst(HttpHeaders.AUTHORIZATION),
                    new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            byte[] done = "data: {\"type\":\"done\",\"payload\":{}}\n\n"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_EVENT_STREAM_VALUE);
            exchange.sendResponseHeaders(200, done.length);
            try (var output = exchange.getResponseBody()) {
                output.write(done);
            }
        });
        python.start();
        WebClient webClient = WebClient.builder()
                .baseUrl("http://127.0.0.1:" + python.getAddress().getPort())
                .build();
        mvc = MockMvcBuilders.standaloneSetup(
                new SupportController(new SupportServiceImpl(new AgentClient(webClient))))
                .build();
    }

    @AfterEach
    void tearDown() {
        if (python != null) {
            python.stop(0);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"guide", "support"})
    void forwardsEachRequestsAuthorizationToPython(String entry) throws Exception {
        // Reuse the client for two users to catch token leakage through shared headers.
        for (long userId : new long[]{42L, 73L}) {
            String authorization = "Bearer user-" + userId + "-jwt";
            var result = mvc.perform(post("/api/" + entry + "/turns/stream")
                            .header("user-info", userId)
                            .header(HttpHeaders.AUTHORIZATION, authorization)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"text\":\"hello\"}"))
                    .andExpect(status().isOk())
                    .andReturn();

            ReceivedRequest request = received.poll(5, TimeUnit.SECONDS);
            assertThat(request).isNotNull();
            assertThat(request.authorization()).isEqualTo(authorization);
            assertThat(request.path()).isEqualTo("/internal/" + entry + "/turns/stream");
            assertThat(request.body()).contains("\"userId\":\"" + userId + "\"", "\"text\":\"hello\"");
            result.getAsyncResult(5_000);
        }
    }

    private record ReceivedRequest(String path, String authorization, String body) {
    }
}
