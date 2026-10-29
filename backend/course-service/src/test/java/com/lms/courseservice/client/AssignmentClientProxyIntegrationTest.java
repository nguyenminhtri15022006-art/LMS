package com.lms.courseservice.client;

import com.lms.courseservice.configuration.AssignmentClient;
import com.lms.courseservice.dto.ApiResponse;
import com.lms.courseservice.repository.mongo.CourseStructureRepository;
import com.lms.courseservice.repository.mongo.LessonRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm chứng cơ chế Feign Client Proxy thật của AssignmentClient:
 * - KHÔNG mock AssignmentClient.
 * - KHÔNG gọi trực tiếp class Fallback.
 * - Gọi qua bean Feign proxy do Spring sinh ra, kết nối tới HTTP Server cục bộ kiểm soát theo test lifecycle.
 */
@SpringBootTest
@ActiveProfiles("test")
class AssignmentClientProxyIntegrationTest {

    @MockBean
    private CourseStructureRepository courseStructureRepository;

    @MockBean
    private LessonRepository lessonRepository;

    @Autowired
    private AssignmentClient assignmentClient;

    private static HttpServer localHttpServer;
    private static int localServerPort;
    private static final AtomicInteger receivedRequestCount = new AtomicInteger(0);
    private static volatile int stubStatusCode = 200;
    private static volatile String stubResponseBody = "{\"code\":1000,\"data\":42}";

    @BeforeAll
    static void startLocalServer() throws IOException {
        // Cổng 0 để hệ điều hành cấp phát cổng động ngẫu nhiên không xung đột
        localHttpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        localHttpServer.createContext("/assignment", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws IOException {
                receivedRequestCount.incrementAndGet();
                byte[] responseBytes = stubResponseBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(stubStatusCode, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            }
        });
        localHttpServer.start();
        localServerPort = localHttpServer.getAddress().getPort();
    }

    @AfterAll
    static void stopLocalServer() {
        if (localHttpServer != null) {
            localHttpServer.stop(0);
        }
    }

    @BeforeEach
    void resetCounters() {
        receivedRequestCount.set(0);
        stubStatusCode = 200;
        stubResponseBody = "{\"code\":1000,\"data\":42}";
    }

    @DynamicPropertySource
    static void configureFeignClientUrl(DynamicPropertyRegistry registry) {
        // Trỏ AssignmentClient Feign url tới HTTP Server cục bộ
        registry.add("services.assignment-service.url", () -> "http://localhost:" + localServerPort + "/assignment");
    }

    @Test
    @DisplayName("1. exerciseCountReturnsRealDataWhenDownstreamSucceeds: Downstream trả HTTP 200, Feign proxy trả dữ liệu thật 42L (khác fallback 0L)")
    void exerciseCountReturnsRealDataWhenDownstreamSucceeds() {
        stubStatusCode = 200;
        stubResponseBody = "{\"code\":1000,\"data\":42}";

        ApiResponse<Long> response = assignmentClient.getExerciseCount("course-101");

        assertNotNull(response);
        assertEquals(1000, response.getCode());
        assertEquals(42L, response.getData(), "Dữ liệu trả về từ downstream phải là 42L, khác hoàn toàn giá trị fallback 0L");
        assertTrue(receivedRequestCount.get() > 0, "Yêu cầu HTTP thực sự đã được gửi tới server test cục bộ");
    }

    @Test
    @DisplayName("2. exerciseCountProxyTriggersFallbackWhenDownstreamFails: Downstream trả HTTP 500, Feign proxy tự động chuyển mạch sang fallback trả 0L")
    void exerciseCountProxyTriggersFallbackWhenDownstreamFails() {
        stubStatusCode = 500;
        stubResponseBody = "{\"status\":500,\"message\":\"Internal Server Error\"}";

        ApiResponse<Long> response = assignmentClient.getExerciseCount("course-500");

        assertNotNull(response, "Response không được null khi fallback kích hoạt");
        assertEquals(0L, response.getData(), "Feign proxy phải kích hoạt fallback trả về 0L thay vì ném FeignException");
        assertTrue(receivedRequestCount.get() > 0, "Yêu cầu HTTP thực sự đã được gửi tới server test và gặp lỗi 500");
    }

    @Test
    @DisplayName("3. exerciseCountBatchProxyTriggersFallbackWhenDownstreamFails: Downstream trả HTTP 500, Feign proxy batch tự động fallback trả Map rỗng")
    void exerciseCountBatchProxyTriggersFallbackWhenDownstreamFails() {
        stubStatusCode = 500;
        stubResponseBody = "{\"status\":500,\"message\":\"Internal Server Error\"}";

        ApiResponse<Map<String, Long>> response = assignmentClient.getExerciseCountBatch(List.of("course-1", "course-2"));

        assertNotNull(response, "Response không được null khi fallback kích hoạt");
        assertNotNull(response.getData());
        assertTrue(response.getData().isEmpty(), "Feign proxy phải kích hoạt fallback trả về map rỗng thay vì ném FeignException");
        assertTrue(receivedRequestCount.get() > 0, "Yêu cầu HTTP thực sự đã được gửi tới server test và gặp lỗi 500");
    }
}
