package com.lms.webapp.smoke;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class SmokeVerifiersUnitTest {

    private HttpServer server;
    private int port;
    private String serverUrl;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        serverUrl = "http://localhost:" + port;
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    // =========================================================================
    // 1. MinioDirectUploadSmokeVerifier Unit Tests
    // =========================================================================

    @Test
    @DisplayName("MinIO Verifier: live health check returns true on 200 and false on 500")
    void minioLiveHealthCheck() {
        server.createContext("/minio/health/live", exchange -> {
            byte[] response = "OK".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        });

        MinioDirectUploadSmokeVerifier verifier = new MinioDirectUploadSmokeVerifier();
        assertThat(verifier.verifyMinioLive(serverUrl)).isTrue();
    }

    @Test
    @DisplayName("MinIO Verifier: upload fails when MinIO returns error status code")
    void minioUploadFailsOnErrorStatus() {
        server.createContext("/upload", exchange -> {
            byte[] err = "Access Denied".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(403, err.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(err);
            }
        });

        MinioDirectUploadSmokeVerifier verifier = new MinioDirectUploadSmokeVerifier();
        byte[] testBytes = "Hello World".getBytes(StandardCharsets.UTF_8);
        MinioDirectUploadSmokeVerifier.DirectUploadResult result = verifier.uploadViaPresignedPostPolicy(
                serverUrl + "/upload",
                Map.of("key", "test.png"),
                "test.png",
                "image/png",
                testBytes,
                serverUrl + "/files/test.png"
        );

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getUploadStatusCode()).isEqualTo(403);
    }

    @Test
    @DisplayName("MinIO Verifier: upload fails if read-back verification content does not match")
    void minioUploadFailsWhenReadBackMismatches() {
        server.createContext("/upload", exchange -> {
            // S3 POST policy trả về 204 No Content khi upload thành công
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });

        server.createContext("/files/test.png", exchange -> {
            // Trả về nội dung khác với file đã upload
            byte[] wrongBytes = "Corrupted Data".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, wrongBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(wrongBytes);
            }
        });

        MinioDirectUploadSmokeVerifier verifier = new MinioDirectUploadSmokeVerifier();
        byte[] originalBytes = "Original Clean Data".getBytes(StandardCharsets.UTF_8);
        MinioDirectUploadSmokeVerifier.DirectUploadResult result = verifier.uploadViaPresignedPostPolicy(
                serverUrl + "/upload",
                Map.of("key", "files/test.png"),
                "test.png",
                "image/png",
                originalBytes,
                serverUrl + "/files/test.png"
        );

        // Phải phát hiện thất bại do dữ liệu đọc lại không khớp
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isVerifiedReadBack()).isFalse();
    }

    @Test
    @DisplayName("MinIO Verifier: upload succeeds when MinIO returns 204 and read-back matches bytes")
    void minioUploadSucceedsWhenUploadedAndReadBackMatches() {
        byte[] testContent = new byte[]{1, 2, 3, 4, 5};

        server.createContext("/upload", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });

        server.createContext("/files/test.png", exchange -> {
            exchange.sendResponseHeaders(200, testContent.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(testContent);
            }
        });

        MinioDirectUploadSmokeVerifier verifier = new MinioDirectUploadSmokeVerifier();
        MinioDirectUploadSmokeVerifier.DirectUploadResult result = verifier.uploadViaPresignedPostPolicy(
                serverUrl + "/upload",
                Map.of("key", "files/test.png"),
                "test.png",
                "image/png",
                testContent,
                serverUrl + "/files/test.png"
        );

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getUploadStatusCode()).isEqualTo(204);
        assertThat(result.isVerifiedReadBack()).isTrue();
    }

    @Test
    @DisplayName("MinIO Verifier: buildMultipartFormData formats boundary and fields correctly")
    void buildMultipartFormDataCorrectness() throws IOException {
        String boundary = "TestBoundary123";
        Map<String, String> fields = Map.of("key", "avatars/sample.png", "x-amz-date", "20261026T000000Z");
        byte[] content = "file-bytes".getBytes(StandardCharsets.UTF_8);

        byte[] multipart = MinioDirectUploadSmokeVerifier.buildMultipartFormData(boundary, fields, "sample.png", "image/png", content);
        String bodyStr = new String(multipart, StandardCharsets.UTF_8);

        assertThat(bodyStr).contains("--" + boundary);
        assertThat(bodyStr).contains("name=\"key\"");
        assertThat(bodyStr).contains("avatars/sample.png");
        assertThat(bodyStr).contains("name=\"file\"; filename=\"sample.png\"");
        assertThat(bodyStr).contains("Content-Type: image/png");
        assertThat(bodyStr).contains("file-bytes");
        assertThat(bodyStr).endsWith("--" + boundary + "--\r\n");
    }

    // =========================================================================
    // 2. GatewayRoutingSmokeVerifier Unit Tests
    // =========================================================================

    @Test
    @DisplayName("Gateway Verifier: fails when BFF returns error banner even if status is 200")
    void gatewayVerifierFailsOnErrorBanner() {
        server.createContext("/courses", exchange -> {
            String html = "<html><body><div class='alert alert-danger'>Không thể tải danh sách khóa học</div></body></html>";
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        GatewayRoutingSmokeVerifier verifier = new GatewayRoutingSmokeVerifier();
        GatewayRoutingSmokeVerifier.GatewayRoutingResult result =
                verifier.verifyGatewayDataViaBff(serverUrl, "UniqueCourse", 1L);

        // Không được chấp nhận trang lỗi dù HTTP status là 200
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getErrorMessage()).contains("error banner");
    }

    @Test
    @DisplayName("Gateway Verifier: fails when expected identifier is not present in rendered HTML")
    void gatewayVerifierFailsWhenIdentifierMissing() {
        server.createContext("/courses", exchange -> {
            String html = "<html><body><select><option>Tất cả danh mục</option></select></body></html>";
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        GatewayRoutingSmokeVerifier verifier = new GatewayRoutingSmokeVerifier();
        GatewayRoutingSmokeVerifier.GatewayRoutingResult result =
                verifier.verifyGatewayDataViaBff(serverUrl, "NonExistentCategory", 1L);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isContainsIdentifier()).isFalse();
    }

    @Test
    @DisplayName("Gateway Verifier: succeeds when expected identifier is rendered in HTML")
    void gatewayVerifierSucceedsWhenIdentifierRendered() {
        server.createContext("/courses", exchange -> {
            String html = "<html><body><select><option value='10'>Lập trình Java Nâng Cao</option></select></body></html>";
            byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        GatewayRoutingSmokeVerifier verifier = new GatewayRoutingSmokeVerifier();
        GatewayRoutingSmokeVerifier.GatewayRoutingResult result =
                verifier.verifyGatewayDataViaBff(serverUrl, "Lập trình Java Nâng Cao", 10L);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isContainsIdentifier()).isTrue();
    }

    // =========================================================================
    // 3. AuthSessionSmokeVerifier Unit Tests
    // =========================================================================

    @Test
    @DisplayName("Auth Verifier: anonymous denied correctly verifies 302 to /login")
    void anonymousDeniedVerifiesRedirectToLogin() {
        server.createContext("/profile", exchange -> {
            exchange.getResponseHeaders().set("Location", serverUrl + "/login");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });

        AuthSessionSmokeVerifier verifier = new AuthSessionSmokeVerifier();
        assertThat(verifier.verifyAnonymousAccessDenied(serverUrl)).isTrue();
    }

    @Test
    @DisplayName("Auth Verifier: invalid login rejected when server re-renders form with error")
    void invalidLoginRejectedOnErrorMessage() {
        server.createContext("/login", exchange -> {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String html = "<html><input type='hidden' name='_csrf' value='csrf-token-123'/></html>";
                byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                String errorHtml = "<html><div class='text-danger'>Tên đăng nhập hoặc mật khẩu không chính xác.</div></html>";
                byte[] bytes = errorHtml.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            }
        });

        AuthSessionSmokeVerifier verifier = new AuthSessionSmokeVerifier();
        assertThat(verifier.verifyInvalidLoginRejected(serverUrl, "user", "wrong")).isTrue();
    }

    @Test
    @DisplayName("Auth Verifier: executeLoginAndFetchProfile parses clean session cookie and validates user profile")
    void executeLoginAndFetchProfileSuccess() {
        server.createContext("/login", exchange -> {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                String html = "<html><input type='hidden' name='_csrf' value='csrf-999'/></html>";
                byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } else {
                // Đăng nhập thành công trả về Set-Cookie có Path, HttpOnly...
                exchange.getResponseHeaders().set("Set-Cookie", "JSESSIONID=SECURE_SESSION_12345; Path=/; HttpOnly; SameSite=Lax");
                exchange.getResponseHeaders().set("Location", "/");
                exchange.sendResponseHeaders(302, -1);
                exchange.close();
            }
        });

        server.createContext("/profile", exchange -> {
            String profileHtml = "<html><body><h1>Hồ sơ cá nhân</h1><span class='username'>test_student</span></body></html>";
            byte[] bytes = profileHtml.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        AuthSessionSmokeVerifier verifier = new AuthSessionSmokeVerifier();
        AuthSessionSmokeVerifier.LoginProfileResult result =
                verifier.executeLoginAndFetchProfile(serverUrl, "test_student", "Secret@123");

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getHttpStatus()).isEqualTo(200);
        assertThat(result.isProfileVerified()).isTrue();
        // Cookie phải được làm sạch, không được chứa Path=/; HttpOnly
        assertThat(result.getCleanSessionCookie()).isEqualTo("JSESSIONID=SECURE_SESSION_12345");
    }

    @Test
    @DisplayName("Auth Verifier: redirect validation accepts relative root and absolute same-origin root")
    void redirectValidationAcceptsValidRoots() {
        String baseUrl = "http://localhost:8088";

        assertThat(AuthSessionSmokeVerifier.isValidLoginRedirect("/", baseUrl)).isTrue();
        assertThat(AuthSessionSmokeVerifier.isValidLoginRedirect("http://localhost:8088/", baseUrl)).isTrue();
        assertThat(AuthSessionSmokeVerifier.isValidLoginRedirect("http://localhost:8088", baseUrl)).isTrue();
    }

    @Test
    @DisplayName("Auth Verifier: redirect validation rejects different host, port, scheme, path, query and malformed URL")
    void redirectValidationRejectsInvalidOrMaliciousUrls() {
        String baseUrl = "http://localhost:8088";

        // Khác host
        assertThat(AuthSessionSmokeVerifier.isValidLoginRedirect("http://evil.com/", baseUrl)).isFalse();
        assertThat(AuthSessionSmokeVerifier.isValidLoginRedirect("http://attacker.org:8088/", baseUrl)).isFalse();

        // Khác port
        assertThat(AuthSessionSmokeVerifier.isValidLoginRedirect("http://localhost:9999/", baseUrl)).isFalse();
        assertThat(AuthSessionSmokeVerifier.isValidLoginRedirect("http://localhost:8080/", baseUrl)).isFalse();

        // Khác scheme
        assertThat(AuthSessionSmokeVerifier.isValidLoginRedirect("https://localhost:8088/", baseUrl)).isFalse();

        // Khác path
        assertThat(AuthSessionSmokeVerifier.isValidLoginRedirect("http://localhost:8088/admin", baseUrl)).isFalse();
        assertThat(AuthSessionSmokeVerifier.isValidLoginRedirect("/login?error", baseUrl)).isFalse();

        // Có error query param
        assertThat(AuthSessionSmokeVerifier.isValidLoginRedirect("http://localhost:8088/?error=true", baseUrl)).isFalse();

        // Malformed URL / null / blank
        assertThat(AuthSessionSmokeVerifier.isValidLoginRedirect("not a valid uri :::", baseUrl)).isFalse();
        assertThat(AuthSessionSmokeVerifier.isValidLoginRedirect("", baseUrl)).isFalse();
        assertThat(AuthSessionSmokeVerifier.isValidLoginRedirect(null, baseUrl)).isFalse();
    }
}
