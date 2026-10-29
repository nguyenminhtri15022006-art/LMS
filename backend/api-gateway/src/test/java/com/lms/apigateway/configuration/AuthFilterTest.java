package com.lms.apigateway.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class AuthFilterTest {

    private static final String SIGNER_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String OTHER_KEY = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210";

    private JwtUtil jwtUtil;
    private AuthFilter authFilter;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(SIGNER_KEY);
        authFilter = new AuthFilter(jwtUtil, new ObjectMapper());
    }

    @Test
    @DisplayName("Case 1: Token hợp lệ — inject đủ 4 headers X-* và loại bỏ Authorization (không inject lại Authorization)")
    void privateRequestWithValidTokenInjects4GatewayHeadersAndStripsAuthorization() {
        String token = createToken("user-1", "ROLE_STUDENT", "student@example.com", "ACCESS", 600, SIGNER_KEY);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/course/courses/my")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .build());
        AtomicReference<ServerHttpRequest> forwardedRequest = new AtomicReference<>();

        StepVerifier.create(filter(exchange, nextExchange -> {
                    forwardedRequest.set(nextExchange.getRequest());
                    return Mono.empty();
                }))
                .verifyComplete();

        HttpHeaders headers = forwardedRequest.get().getHeaders();
        // Headers X-* được inject
        assertThat(headers.getFirst("X-User-Id")).isEqualTo("user-1");
        assertThat(headers.getFirst("X-User-Role")).isEqualTo("ROLE_STUDENT");
        assertThat(headers.getFirst("X-User-Roles")).isEqualTo("ROLE_STUDENT");
        assertThat(headers.getFirst("X-User-Email")).isEqualTo("student@example.com");
        assertThat(headers.getFirst("X-Gateway-Authenticated")).isEqualTo("true");
        // Header Authorization thô bị xóa và không được inject lại
        assertThat(headers.containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
    }

    @Test
    @DisplayName("Case 2: Thiếu / Sai Bearer header — trả về 401 Unauthorized kèm mã 1401 và ngắt filter chain")
    void privateRequestWithoutBearerTokenReturns401AndStopsChain() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/course/courses/my").build());
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        StepVerifier.create(filter(exchange, nextExchange -> {
                    chainCalled.set(true);
                    return Mono.empty();
                }))
                .verifyComplete();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("\"code\":1401")
                .contains("\"path\":\"/api/course/courses/my\"");
    }

    @Test
    @DisplayName("Case 3: Token hết hạn — JwtUtil từ chối, AuthFilter trả về 401 và ngắt filter chain")
    void privateRequestWithExpiredTokenReturns401() {
        String expiredToken = createToken("user-1", "ROLE_STUDENT", "student@example.com", "ACCESS", -600, SIGNER_KEY);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/course/courses/my")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                        .build());
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        StepVerifier.create(filter(exchange, nextExchange -> {
                    chainCalled.set(true);
                    return Mono.empty();
                }))
                .verifyComplete();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Case 4: Token sai chữ ký — JwtUtil từ chối, AuthFilter trả về 401 và ngắt filter chain")
    void privateRequestWithInvalidSignatureReturns401() {
        String invalidSigToken = createToken("user-1", "ROLE_STUDENT", "student@example.com", "ACCESS", 600, OTHER_KEY);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/course/courses/my")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidSigToken)
                        .build());
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        StepVerifier.create(filter(exchange, nextExchange -> {
                    chainCalled.set(true);
                    return Mono.empty();
                }))
                .verifyComplete();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("Case 5: Header Stripping — Xóa bỏ toàn bộ 5 header giả mạo từ client (4 X-* + Authorization)")
    void requestWithSpoofedHeadersRemovesAll5HeadersBeforeDownstream() {
        String token = createToken("real-user", "ROLE_STUDENT", "real@example.com", "ACCESS", 600, SIGNER_KEY);

        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/course/courses/my")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .header("X-User-Id", "fake-admin")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .header("X-User-Roles", "ROLE_ADMIN")
                        .header("X-User-Email", "hacker@evil.com")
                        .header("X-Gateway-Authenticated", "true")
                        .build());
        AtomicReference<ServerHttpRequest> forwardedRequest = new AtomicReference<>();

        StepVerifier.create(filter(exchange, nextExchange -> {
                    forwardedRequest.set(nextExchange.getRequest());
                    return Mono.empty();
                }))
                .verifyComplete();

        HttpHeaders headers = forwardedRequest.get().getHeaders();
        // Giá trị giả mạo đã bị ghi đè hoàn toàn bằng thông tin trích xuất từ JWT hợp lệ
        assertThat(headers.getFirst("X-User-Id")).isEqualTo("real-user");
        assertThat(headers.getFirst("X-User-Role")).isEqualTo("ROLE_STUDENT");
        assertThat(headers.getFirst("X-User-Roles")).isEqualTo("ROLE_STUDENT");
        assertThat(headers.getFirst("X-User-Email")).isEqualTo("real@example.com");
        assertThat(headers.getFirst("X-Gateway-Authenticated")).isEqualTo("true");
        // Header Authorization bị xóa, không tồn tại ở downstream
        assertThat(headers.containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
    }

    @Test
    @DisplayName("Case 6: Whitelist theo HTTP Method — GET được bỏ qua, còn POST/PUT/DELETE vào cùng path không token bị trả về 401")
    void whitelistByHttpMethodAllowsGetAndRejectsMutatingMethodsWithoutToken() {
        String[] publicGetPaths = {
                "/api/blog/posts/public/x",
                "/api/blog/tags",
                "/api/course/categories",
                "/api/course/courses/public/x"
        };

        // 1. Kiểm tra request GET vào các public paths đi qua filter mà không cần JWT
        for (String path : publicGetPaths) {
            MockServerWebExchange getExchange = MockServerWebExchange.from(
                    MockServerHttpRequest.method(HttpMethod.GET, path)
                            .header(HttpHeaders.AUTHORIZATION, "Bearer some-attacker-token")
                            .header("X-User-Id", "spoofed-user")
                            .build());
            AtomicReference<ServerHttpRequest> forwarded = new AtomicReference<>();

            StepVerifier.create(filter(getExchange, next -> {
                        forwarded.set(next.getRequest());
                        return Mono.empty();
                    }))
                    .verifyComplete();

            assertThat(forwarded.get()).isNotNull();
            // Headers giả mạo bị xóa sạch
            assertThat(forwarded.get().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)).isFalse();
            assertThat(forwarded.get().getHeaders().containsKey("X-User-Id")).isFalse();
        }

        // 2. Kiểm chứng Mutating Methods: POST, PUT, DELETE vào các path này KHÔNG kèm token phải bị chặn 401
        HttpMethod[] mutatingMethods = {HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE};
        for (String path : publicGetPaths) {
            for (HttpMethod method : mutatingMethods) {
                MockServerWebExchange mutatingExchange = MockServerWebExchange.from(
                        MockServerHttpRequest.method(method, path).build());
                AtomicBoolean chainCalled = new AtomicBoolean(false);

                StepVerifier.create(filter(mutatingExchange, next -> {
                            chainCalled.set(true);
                            return Mono.empty();
                        }))
                        .verifyComplete();

                assertThat(chainCalled).isFalse();
                assertThat(mutatingExchange.getResponse().getStatusCode())
                        .as("Request %s %s phải bị trả về 401 Unauthorized", method, path)
                        .isEqualTo(HttpStatus.UNAUTHORIZED);
            }
        }
    }

    private Mono<Void> filter(MockServerWebExchange exchange, GatewayFilterChain chain) {
        return authFilter.apply(new AuthFilter.Config()).filter(exchange, chain);
    }

    private String createToken(String subject, String scope, String email, String type, long durationSeconds, String key) {
        return Jwts.builder()
                .subject(subject)
                .claim("scope", scope)
                .claim("email", email)
                .claim("type", type)
                .expiration(Date.from(Instant.now().plusSeconds(durationSeconds)))
                .signWith(Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
