package com.lms.webapp.fixture;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DockerStackReadinessVerifierTest {

    private HttpServer testServer;
    private int httpPort;
    private ServerSocket tcpSocket;
    private int tcpPort;
    private DockerStackReadinessVerifier verifier;

    @BeforeEach
    void setUp() throws IOException {
        verifier = new DockerStackReadinessVerifier();

        // Khởi tạo HTTP test server cổng động
        testServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        testServer.start();
        httpPort = testServer.getAddress().getPort();

        // Khởi tạo TCP socket cổng động
        tcpSocket = new ServerSocket(0);
        tcpPort = tcpSocket.getLocalPort();
    }

    @AfterEach
    void tearDown() {
        if (testServer != null) {
            testServer.stop(0);
        }
        if (tcpSocket != null && !tcpSocket.isClosed()) {
            try {
                tcpSocket.close();
            } catch (IOException ignored) {}
        }
    }

    @Test
    @DisplayName("1. checkHttp: Dịch vụ HTTP trả 200 -> trạng thái UP với latency đo được")
    void http200ReturnsUp() {
        testServer.createContext("/actuator/health", exchange -> {
            byte[] response = "{\"status\":\"UP\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });

        DockerStackReadinessVerifier.ServiceEndpoint endpoint =
                DockerStackReadinessVerifier.ServiceEndpoint.http("test-svc", "127.0.0.1", httpPort, "/actuator/health");

        DockerStackReadinessVerifier.ServiceHealthStatus status = verifier.checkEndpoint(endpoint, 1000);

        assertThat(status.isUp()).isTrue();
        assertThat(status.statusCode()).isEqualTo(200);
        assertThat(status.latencyMs()).isGreaterThanOrEqualTo(0);
        assertThat(status.message()).contains("OK (HTTP 200)");
    }

    @Test
    @DisplayName("2. checkHttp: Dịch vụ HTTP trả 500 -> trạng thái DOWN")
    void http500ReturnsDown() {
        testServer.createContext("/actuator/health", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });

        DockerStackReadinessVerifier.ServiceEndpoint endpoint =
                DockerStackReadinessVerifier.ServiceEndpoint.http("test-svc", "127.0.0.1", httpPort, "/actuator/health");

        DockerStackReadinessVerifier.ServiceHealthStatus status = verifier.checkEndpoint(endpoint, 1000);

        assertThat(status.isUp()).isFalse();
        assertThat(status.statusCode()).isEqualTo(500);
        assertThat(status.message()).contains("HTTP 500");
    }

    @Test
    @DisplayName("3. checkHttp: Kết nối tới cổng không tồn tại -> trạng thái DOWN với thông báo lỗi")
    void connectionRefusedReturnsDown() {
        DockerStackReadinessVerifier.ServiceEndpoint endpoint =
                DockerStackReadinessVerifier.ServiceEndpoint.http("unreachable", "127.0.0.1", 65530, "/actuator/health");

        DockerStackReadinessVerifier.ServiceHealthStatus status = verifier.checkEndpoint(endpoint, 200);

        assertThat(status.isUp()).isFalse();
        assertThat(status.statusCode()).isEqualTo(-1);
        assertThat(status.message()).isNotBlank();
    }

    @Test
    @DisplayName("4. checkTcp: Cổng TCP mở -> trạng thái UP; cổng đóng -> trạng thái DOWN")
    void tcpPortPingVerification() throws IOException {
        // Cổng TCP đang mở
        DockerStackReadinessVerifier.ServiceEndpoint openTcp =
                DockerStackReadinessVerifier.ServiceEndpoint.tcp("mysql-test", "127.0.0.1", tcpPort);
        DockerStackReadinessVerifier.ServiceHealthStatus openStatus = verifier.checkEndpoint(openTcp, 1000);
        assertThat(openStatus.isUp()).isTrue();
        assertThat(openStatus.message()).contains("Connected");

        // Đóng cổng TCP
        tcpSocket.close();
        DockerStackReadinessVerifier.ServiceHealthStatus closedStatus = verifier.checkEndpoint(openTcp, 200);
        assertThat(closedStatus.isUp()).isFalse();
        assertThat(closedStatus.message()).contains("Connection failed");
    }

    @Test
    @DisplayName("5. verifyStack: Tổng hợp báo cáo kiểm tra đa dịch vụ chính xác (isAllUp, downServices)")
    void multiServiceReportEvaluation() {
        testServer.createContext("/up", exchange -> {
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        testServer.createContext("/down", exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });

        List<DockerStackReadinessVerifier.ServiceEndpoint> endpoints = List.of(
                DockerStackReadinessVerifier.ServiceEndpoint.http("service-up", "127.0.0.1", httpPort, "/up"),
                DockerStackReadinessVerifier.ServiceEndpoint.http("service-down", "127.0.0.1", httpPort, "/down"),
                DockerStackReadinessVerifier.ServiceEndpoint.tcp("tcp-up", "127.0.0.1", tcpPort)
        );

        DockerStackReadinessVerifier.DockerReadinessReport report =
                verifier.verifyStack(endpoints, Duration.ofMillis(1000));

        assertThat(report.getTotalServices()).isEqualTo(3);
        assertThat(report.getUpCount()).isEqualTo(2);
        assertThat(report.isAllUp()).isFalse();
        assertThat(report.getDownServices()).hasSize(1);
        assertThat(report.getDownServices().get(0)).contains("service-down");
    }
}
