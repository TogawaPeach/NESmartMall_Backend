package com.nemall.gateway;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutesTests {

    private static final String JWT_SECRET = "gateway-routes-test-secret";
    private static final String AUTHORIZATION = "Bearer " + accessToken();

    private static final HttpServer AUTH_SERVER = upstream("auth");
    private static final HttpServer PRODUCT_SERVER = upstream("product");
    private static final HttpServer CART_SERVER = upstream("cart");

    @LocalServerPort
    private int port;

    @DynamicPropertySource
    static void upstreamUrls(DynamicPropertyRegistry registry) {
        registry.add("AUTH_SERVICE_URL", () -> url(AUTH_SERVER));
        registry.add("PRODUCT_SERVICE_URL", () -> url(PRODUCT_SERVER));
        registry.add("CART_SERVICE_URL", () -> url(CART_SERVER));
        registry.add("jwt.secret", () -> JWT_SECRET);
    }

    @AfterAll
    static void stopUpstreams() {
        AUTH_SERVER.stop(0);
        PRODUCT_SERVER.stop(0);
        CART_SERVER.stop(0);
    }

    @Test
    void authRequestReachesAuthServiceAndPreservesBearerHeader() throws Exception {
        HttpResponse<String> response = send("GET", "/api/auth/me", "");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("auth|GET|/api/auth/me|" + AUTHORIZATION + "|");
    }

    @Test
    void productRequestReachesProductServiceAndPreservesQuery() throws Exception {
        HttpResponse<String> response = send("GET", "/api/products?limit=10", "");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("product|GET|/api/products?limit=10|" + AUTHORIZATION + "|");
    }

    @Test
    void merchantRequestReachesProductServiceAndPreservesMethodAndBody() throws Exception {
        HttpResponse<String> response = send("PATCH", "/api/merchant/products/42/status", "{\"status\":2}");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(
                "product|PATCH|/api/merchant/products/42/status|" + AUTHORIZATION + "|{\"status\":2}");
    }

    @Test
    void cartRequestReachesCartServiceAndPreservesQueryAndBearerHeader() throws Exception {
        HttpResponse<String> response = send("GET", "/api/cart?selected=true", "");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("cart|GET|/api/cart?selected=true|" + AUTHORIZATION + "|");
    }

    @Test
    void cartItemRequestPreservesMethodPathAndBody() throws Exception {
        HttpResponse<String> response = send("PATCH", "/api/cart/items/42", "{\"quantity\":2}");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo(
                "cart|PATCH|/api/cart/items/42|" + AUTHORIZATION + "|{\"quantity\":2}");
    }

    @Test
    void cartRequestWithoutTokenIsUnauthorized() throws Exception {
        HttpResponse<String> response = send("GET", "/api/cart", "", null);

        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("WWW-Authenticate")).contains("Bearer");
    }

    @Test
    void cartRequestWithInvalidTokenIsUnauthorized() throws Exception {
        assertThat(send("GET", "/api/cart/items", "", "Bearer invalid-token").statusCode()).isEqualTo(401);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CUSTOMER", "MERCHANT"})
    void cartReceivesVerifiedIdentityInsteadOfForgedHeaders(String role) throws Exception {
        HttpResponse<String> response = send("GET", "/api/cart", "",
                "Bearer " + accessToken("42", List.of(role)),
                "user-info", "999", "user-info", "888",
                "user-role", "ADMIN", "user-role", "FORGED",
                "X-Service-Token", "external-forged-value");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("X-Seen-User-Info")).contains("42");
        assertThat(response.headers().firstValue("X-Seen-User-Role")).contains(role);
        assertThat(response.headers().firstValue("X-Seen-Service-Token").orElse("")).isEmpty();
    }

    @Test
    void publicRequestDropsForgedIdentityEvenWithInvalidToken() throws Exception {
        HttpResponse<String> response = send("GET", "/api/products", "", "Bearer invalid-token",
                "user-info", "999", "user-role", "MERCHANT",
                "X-Service-Token", "external-forged-value");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.headers().firstValue("X-Seen-User-Info").orElse("")).isEmpty();
        assertThat(response.headers().firstValue("X-Seen-User-Role").orElse("")).isEmpty();
        assertThat(response.headers().firstValue("X-Seen-Service-Token").orElse("")).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("invalidIdentityClaims")
    void cartRejectsTokensWithInvalidIdentityClaims(String subject, Object roles) throws Exception {
        HttpResponse<String> response = send("GET", "/api/cart", "",
                "Bearer " + accessToken(subject, roles));

        assertThat(response.statusCode()).isEqualTo(401);
    }

    private static Stream<Arguments> invalidIdentityClaims() {
        return Stream.of(
                Arguments.of("0", List.of("CUSTOMER")),
                Arguments.of("not-an-id", List.of("CUSTOMER")),
                Arguments.of("9223372036854775808", List.of("CUSTOMER")),
                Arguments.of("42", null),
                Arguments.of("42", List.of()),
                Arguments.of("42", List.of("CUSTOMER", "MERCHANT")),
                Arguments.of("42", List.of("ADMIN")),
                Arguments.of("42", "CUSTOMER"),
                Arguments.of("42", List.of(17))
        );
    }

    @Test
    void similarPathDoesNotMatchCartRoute() throws Exception {
        assertThat(send("GET", "/api/cartography", "").statusCode()).isEqualTo(404);
    }

    @Test
    void unrelatedPathDoesNotMatchAServiceRoute() throws Exception {
        assertThat(send("GET", "/unmapped", "").statusCode()).isEqualTo(404);
    }

    private HttpResponse<String> send(String method, String path, String body) throws Exception {
        return send(method, path, body, AUTHORIZATION);
    }

    private HttpResponse<String> send(String method, String path, String body, String authorization,
                                      String... headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .method(method, HttpRequest.BodyPublishers.ofString(body));
        if (authorization != null) {
            builder.header("Authorization", authorization);
        }
        if (headers.length > 0) {
            builder.headers(headers);
        }
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        }
    }

    private static String accessToken() {
        return accessToken("1", List.of("CUSTOMER"));
    }

    private static String accessToken(String subject, Object roles) {
        try {
            byte[] key = MessageDigest.getInstance("SHA-256").digest(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
            var encoder = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(key, "HmacSHA256"))
                    .algorithm(MacAlgorithm.HS256)
                    .build();
            Instant now = Instant.now();
            JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                    .subject(subject)
                    .issuer("NESmartMall")
                    .audience(List.of("nesmartmall-api"))
                    .issuedAt(now)
                    .expiresAt(now.plusSeconds(3600));
            if (roles != null) {
                claims.claim("roles", roles);
            }
            return encoder.encode(JwtEncoderParameters.from(
                    JwsHeader.with(MacAlgorithm.HS256).type("JWT").build(), claims.build())).getTokenValue();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
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
                    exchange.getResponseHeaders().set("X-Seen-User-Info",
                            String.join(",", exchange.getRequestHeaders().getOrDefault("user-info", List.of())));
                    exchange.getResponseHeaders().set("X-Seen-User-Role",
                            String.join(",", exchange.getRequestHeaders().getOrDefault("user-role", List.of())));
                    exchange.getResponseHeaders().set("X-Seen-Service-Token",
                            String.join(",", exchange.getRequestHeaders().getOrDefault("X-Service-Token", List.of())));
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
