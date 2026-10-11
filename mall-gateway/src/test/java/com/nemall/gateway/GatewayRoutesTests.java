package com.nemall.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutesTests {

    private static final HttpServer AUTH_SERVER = upstream("auth");
    private static final HttpServer PRODUCT_SERVER = upstream("product");

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void upstreamUrls(DynamicPropertyRegistry registry) {
        registry.add("AUTH_SERVICE_URL", () -> url(AUTH_SERVER));
        registry.add("PRODUCT_SERVICE_URL", () -> url(PRODUCT_SERVER));
    }

    @AfterAll
    static void stopUpstreams() {
        AUTH_SERVER.stop(0);
        PRODUCT_SERVER.stop(0);
    }

    @Test
    void authRequestReachesAuthServiceAndPreservesBearerHeader() throws Exception {
        HttpResponse<String> response = send("GET", "/api/auth/me", "");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("auth|GET|/api/auth/me|Bearer test-token|");
    }

    @Test
    void productRequestReachesProductServiceAndPreservesQuery() throws Exception {
        HttpResponse<String> response = send("GET", "/api/products?limit=10", "");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("product|GET|/api/products?limit=10|Bearer test-token|");
    }

    @Test
    void merchantRequestReachesProductServiceAndPreservesMethodAndBody() throws Exception {
        HttpResponse<String> response = send("PATCH", "/api/merchant/products/42/status", "{\"status\":2}");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(
                "product|PATCH|/api/merchant/products/42/status|Bearer test-token|{\"status\":2}");
    }

    @Test
    void unrelatedPathDoesNotMatchAServiceRoute() throws Exception {
        assertThat(send("GET", "/unmapped", "").statusCode()).isEqualTo(404);
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .header("Authorization", "Bearer test-token")
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body))
                .build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }

    private static String url(HttpServer server) {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static HttpServer upstream(String name) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                try (exchange) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    String response = name + "|" + exchange.getRequestMethod() + "|" + exchange.getRequestURI()
                            + "|" + exchange.getRequestHeaders().getFirst("Authorization") + "|" + body;
                    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=UTF-8");
                    exchange.sendResponseHeaders(200, bytes.length);
                    exchange.getResponseBody().write(bytes);
                }
            });
            server.start();
            return server;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
