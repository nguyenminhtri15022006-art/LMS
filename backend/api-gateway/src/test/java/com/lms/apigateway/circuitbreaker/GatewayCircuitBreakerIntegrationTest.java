package com.lms.apigateway.circuitbreaker;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(GatewayCircuitBreakerIntegrationTest.TestRouteConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GatewayCircuitBreakerIntegrationTest {

    private static HttpServer downstreamServer;
    private static int downstreamPort;
    private static final AtomicInteger requestCounter = new AtomicInteger(0);
    private static volatile String responseMode = "SUCCESS"; // SUCCESS, ERROR_500, TIMEOUT

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeAll
    static void startDownstreamServer() throws IOException {
        downstreamServer = HttpServer.create(new InetSocketAddress(0), 0);
        downstreamPort = downstreamServer.getAddress().getPort();

        downstreamServer.createContext("/test-cb", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                requestCounter.incrementAndGet();

                if ("TIMEOUT".equals(responseMode)) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException ignored) {
                    }
                }

                if ("ERROR_500".equals(responseMode)) {
                    byte[] response = "{\"status\":\"ERROR\",\"message\":\"downstream failure\"}".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(500, response.length);
                    try (OutputStream os = exchange.getResponseBody()) {
                        os.write(response);
                    }
                    return;
                }

                byte[] response = "{\"status\":\"OK\",\"data\":\"hello from downstream\"}".getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response);
                }
            }
        });

        downstreamServer.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        downstreamServer.start();
    }

    @AfterAll
    static void stopDownstreamServer() {
        if (downstreamServer != null) {
            downstreamServer.stop(0);
        }
    }

    @DynamicPropertySource
    static void registerDynamicProperties(DynamicPropertyRegistry registry) {
        // Cấu hình Resilience4j với ngưỡng và thời gian ngắn có giới hạn để test nhanh và cô lập
        registry.add("resilience4j.circuitbreaker.instances.testCircuitBreaker.sliding-window-size", () -> 4);
        registry.add("resilience4j.circuitbreaker.instances.testCircuitBreaker.minimum-number-of-calls", () -> 2);
        registry.add("resilience4j.circuitbreaker.instances.testCircuitBreaker.failure-rate-threshold", () -> 50);
        registry.add("resilience4j.circuitbreaker.instances.testCircuitBreaker.wait-duration-in-open-state", () -> "600ms");
        registry.add("resilience4j.circuitbreaker.instances.testCircuitBreaker.permitted-number-of-calls-in-half-open-state", () -> 1);
        registry.add("resilience4j.timelimiter.instances.testCircuitBreaker.timeout-duration", () -> "300ms");
    }

    @TestConfiguration
    static class TestRouteConfig {
        @Bean
        public RouteLocator testRouteLocator(RouteLocatorBuilder builder) {
            return builder.routes()
                    .route("test-cb-route", r -> r
                            .order(-500)
                            .path("/api/test-cb/**")
                            .filters(f -> f
                                    .stripPrefix(1)
                                    .circuitBreaker(config -> config
                                            .setName("testCircuitBreaker")
                                            .setFallbackUri(URI.create("forward:/fallback/test-service"))
                                            .addStatusCode("500")
                                            .addStatusCode("INTERNAL_SERVER_ERROR")))
                            .uri("http://127.0.0.1:" + downstreamPort))
                    .build();
        }
    }

    @BeforeEach
    void resetState() {
        requestCounter.set(0);
        responseMode = "SUCCESS";

        circuitBreakerRegistry.getAllCircuitBreakers().forEach(cb -> {
            cb.transitionToClosedState();
            cb.reset();
        });
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("testCircuitBreaker");
        if (cb != null) {
            cb.transitionToClosedState();
            cb.reset();
        }
    }

    @Test
    @DisplayName("1. downstreamSuccessResponseForwardedCorrectly: Downstream trả 200 -> Gateway chuyển tiếp 200 với body đúng")
    void downstreamSuccessResponseForwardedCorrectly() {
        responseMode = "SUCCESS";

        webTestClient.get()
                .uri("/api/test-cb/data")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo("OK")
                .jsonPath("$.data").isEqualTo("hello from downstream");

        assertThat(requestCounter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("2. downstream500TriggersCircuitBreakerFallback: Downstream trả 500 -> Gateway kích hoạt fallback 503")
    void downstream500TriggersCircuitBreakerFallback() {
        responseMode = "ERROR_500";

        webTestClient.get()
                .uri("/api/test-cb/data")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.error").isEqualTo("Service Unavailable")
                .jsonPath("$.code").isEqualTo(1405)
                .jsonPath("$.service").isEqualTo("test-service")
                .jsonPath("$.path").isEqualTo("/api/test-cb/data");

        assertThat(requestCounter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("3. downstreamTimeoutTriggersCircuitBreakerFallback: Downstream trễ 1s -> Gateway ngắt timeout 300ms và trả fallback 503")
    void downstreamTimeoutTriggersCircuitBreakerFallback() {
        responseMode = "TIMEOUT";
        long start = System.currentTimeMillis();

        webTestClient.get()
                .uri("/api/test-cb/data")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.code").isEqualTo(1405)
                .jsonPath("$.service").isEqualTo("test-service");

        long elapsed = System.currentTimeMillis() - start;
        // Kiểm chứng timeout ngắt trong thời gian giới hạn (nhanh hơn đáng kể so với 1000ms downstream delay)
        assertThat(elapsed).isLessThan(900);
        assertThat(requestCounter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("4. circuitBreakerOpensAfterFailureThreshold: Đủ điều kiện lỗi -> Mạch OPEN và chặn request tiếp theo trước khi tới downstream")
    void circuitBreakerOpensAfterFailureThreshold() {
        responseMode = "ERROR_500";

        // Gửi 2 cuộc gọi lỗi liên tiếp (đạt minimum-calls=2, failure rate 100% >= 50%)
        webTestClient.get().uri("/api/test-cb/data").exchange().expectStatus().isEqualTo(503);
        webTestClient.get().uri("/api/test-cb/data").exchange().expectStatus().isEqualTo(503);

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("testCircuitBreaker");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        int countBeforeShortCircuit = requestCounter.get();
        assertThat(countBeforeShortCircuit).isEqualTo(2);

        // Cuộc gọi thứ 3: Mạch đã OPEN nên bị ngắt mạch ngay lập tức (short-circuit), không gọi tới downstream
        webTestClient.get()
                .uri("/api/test-cb/data")
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.code").isEqualTo(1405);

        // Khẳng định requestCounter KHÔNG tăng, chứng minh request bị chặn tại Gateway trước khi tới downstream
        assertThat(requestCounter.get()).isEqualTo(countBeforeShortCircuit);
    }

    @Test
    @DisplayName("5. circuitBreakerRecoversFromOpenToHalfOpenToClosed: Sau wait-duration mạch chuyển sang HALF_OPEN và phục hồi CLOSED khi downstream 200")
    void circuitBreakerRecoversFromOpenToHalfOpenToClosed() throws InterruptedException {
        responseMode = "ERROR_500";

        // Tạo 2 lỗi để mạch OPEN
        webTestClient.get().uri("/api/test-cb/data").exchange().expectStatus().isEqualTo(503);
        webTestClient.get().uri("/api/test-cb/data").exchange().expectStatus().isEqualTo(503);

        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker("testCircuitBreaker");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Đợi vượt quá wait-duration-in-open-state (600ms)
        Thread.sleep(700);

        // Chuyển downstream sang trạng thái bình thường (200 OK)
        responseMode = "SUCCESS";

        // Cuộc gọi thử nghiệm trong trạng thái HALF_OPEN
        webTestClient.get()
                .uri("/api/test-cb/data")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("OK");

        // Sau khi cuộc gọi thử nghiệm thành công, mạch phục hồi về CLOSED
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Cuộc gọi tiếp theo hoạt động bình thường
        webTestClient.get()
                .uri("/api/test-cb/data")
                .exchange()
                .expectStatus().isOk();
    }
}
