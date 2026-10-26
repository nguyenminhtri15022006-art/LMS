package com.lms.webapp.deployment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.webapp.smoke.ActuatorHealthSmokeVerifier;
import com.lms.webapp.smoke.AuthSessionSmokeVerifier;
import com.lms.webapp.smoke.GatewayRoutingSmokeVerifier;
import com.lms.webapp.smoke.MinioDirectUploadSmokeVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Docker Deployment Smoke Tests
 * Được cấu hình chạy độc lập trên môi trường Docker Compose container qua Maven Profile:
 * mvn test -pl web-app -f backend/pom.xml -Pdocker-smoke
 */
@Tag("docker-smoke")
class WebAppDockerDeploymentSmokeTest {

    private final String webAppBaseUrl = System.getProperty("smoke.webapp.url", "http://localhost:8088");
    private final String gatewayBaseUrl = System.getProperty("smoke.gateway.url", "http://localhost:8080");
    private final String minioBaseUrl = System.getProperty("smoke.minio.url", "http://localhost:9000");
    private final String mailpitBaseUrl = System.getProperty("smoke.mailpit.url", "http://localhost:8025");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("1. Actuator Health endpoint returns UP status")
    void actuatorHealthReturnsUp() {
        ActuatorHealthSmokeVerifier verifier = new ActuatorHealthSmokeVerifier();
        String healthStatus = verifier.verifyActuatorHealth(webAppBaseUrl);
        assertThat(healthStatus).isEqualTo("UP");
    }

    @Test
    @DisplayName("2. Gateway routing: kiểm chứng danh mục thực tế xuất hiện qua BFF -> Gateway -> course-service")
    void gatewayRoutingAccessibleAndRendersData() {
        GatewayRoutingSmokeVerifier verifier = new GatewayRoutingSmokeVerifier();

        String uniqueCategoryName = "SmokeCategory-" + UUID.randomUUID().toString().substring(0, 8);
        String adminToken = System.getProperty("smoke.admin.token", null);

        // Chuẩn bị dữ liệu kiểm thử qua API Gateway nếu có token admin
        Long categoryId = null;
        if (adminToken != null && !adminToken.isBlank()) {
            categoryId = verifier.createTestCategory(gatewayBaseUrl, adminToken, uniqueCategoryName);
        }

        try {
            String expectedIdentifier = (categoryId != null) ? uniqueCategoryName : "courses";
            GatewayRoutingSmokeVerifier.GatewayRoutingResult result =
                    verifier.verifyGatewayDataViaBff(webAppBaseUrl, expectedIdentifier, categoryId);

            assertThat(result.getHttpStatus())
                    .withFailMessage("BFF /courses phải trả về HTTP 200, nhưng nhận được: " + result.getHttpStatus())
                    .isEqualTo(200);

            assertThat(result.isSuccess())
                    .withFailMessage("Kiểm chứng dữ liệu qua Gateway thất bại: " + result.getErrorMessage())
                    .isTrue();

        } finally {
            if (categoryId != null) {
                verifier.cleanupTestCategory(gatewayBaseUrl, adminToken, categoryId);
            }
        }
    }

    @Test
    @DisplayName("3. Auth & Session: kiểm tra từ chối anonymous, từ chối sai pass, đăng nhập chuẩn CookieStore và đọc profile của đúng user")
    void authSecurityAndSessionFlowE2E() {
        AuthSessionSmokeVerifier verifier = new AuthSessionSmokeVerifier();

        // 3.1 Anonymous không được phép truy cập /profile (bắt buộc redirect 302 về /login)
        boolean anonymousDenied = verifier.verifyAnonymousAccessDenied(webAppBaseUrl);
        assertThat(anonymousDenied)
                .withFailMessage("Truy cập /profile ở trạng thái anonymous phải bị từ chối chuyển hướng về /login")
                .isTrue();

        // 3.2 Đăng nhập với mật khẩu sai phải bị từ chối
        String testUser = System.getProperty("smoke.test.user", "admin");
        String badPassword = "WrongPassword_999!";
        boolean invalidLoginRejected = verifier.verifyInvalidLoginRejected(webAppBaseUrl, testUser, badPassword);
        assertThat(invalidLoginRejected)
                .withFailMessage("Đăng nhập sai mật khẩu phải bị từ chối, không được cấp session")
                .isTrue();

        // 3.3 Đăng nhập với tài khoản hợp lệ (đọc từ env an toàn)
        String testPassword = System.getProperty("smoke.test.password", "admin123");

        AuthSessionSmokeVerifier.LoginProfileResult result =
                verifier.executeLoginAndFetchProfile(webAppBaseUrl, testUser, testPassword);

        assertThat(result.isSuccess())
                .withFailMessage("Đăng nhập hoặc xác minh profile thất bại: " + result.getErrorMessage())
                .isTrue();

        assertThat(result.getCleanSessionCookie())
                .withFailMessage("Cookie JSESSIONID phải được lưu đúng định dạng name=value")
                .isNotNull()
                .startsWith("JSESSIONID=");

        assertThat(result.isProfileVerified())
                .withFailMessage("Profile HTML phải chứa đúng định danh username của người dùng đăng nhập")
                .isTrue();
    }

    @Test
    @DisplayName("4. MinIO: kiểm tra riêng health live và thực hiện upload file multipart thật qua Presigned POST Policy")
    void minioHealthAndDirectUploadE2E() throws Exception {
        MinioDirectUploadSmokeVerifier verifier = new MinioDirectUploadSmokeVerifier();

        // 4.1 Health check MinIO độc lập (tuyệt đối không dùng để tuyên bố upload thành công)
        boolean minioLive = verifier.verifyMinioLive(minioBaseUrl);
        assertThat(minioLive)
                .withFailMessage("MinIO live healthcheck tại %s thất bại", minioBaseUrl)
                .isTrue();

        // 4.2 Lấy Presigned URL qua Identity Service / Gateway
        String testFileName = "smoke-avatar-" + UUID.randomUUID() + ".png";
        byte[] dummyPngBytes = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        String presignedReqBody = objectMapper.writeValueAsString(Map.of(
                "fileName", testFileName,
                "fileType", "image/png",
                "folder", "avatars"
        ));

        String adminToken = System.getProperty("smoke.admin.token", null);
        HttpRequest.Builder policyReqBuilder = HttpRequest.newBuilder()
                .uri(URI.create(gatewayBaseUrl.replaceAll("/+$", "") + "/api/identity/storage/presigned-url"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(presignedReqBody));

        if (adminToken != null && !adminToken.isBlank()) {
            policyReqBuilder.header("Authorization", "Bearer " + adminToken);
        }

        HttpResponse<String> policyRes = httpClient.send(policyReqBuilder.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(policyRes.statusCode())
                .withFailMessage("Lấy Presigned URL từ Gateway thất bại với mã: %d, body: %s", policyRes.statusCode(), policyRes.body())
                .isEqualTo(200);

        JsonNode root = objectMapper.readTree(policyRes.body());
        JsonNode data = root.has("data") ? root.get("data") : root;

        String uploadUrl = data.get("uploadUrl").asText();
        String publicUrl = data.get("publicUrl").asText();
        JsonNode formFieldsNode = data.get("formFields");

        Map<String, String> formFields = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = formFieldsNode.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            formFields.put(entry.getKey(), entry.getValue().asText());
        }

        // 4.3 Upload multipart thật lên MinIO
        MinioDirectUploadSmokeVerifier.DirectUploadResult uploadResult =
                verifier.uploadViaPresignedPostPolicy(uploadUrl, formFields, testFileName, "image/png", dummyPngBytes, publicUrl);

        try {
            assertThat(uploadResult.isSuccess())
                    .withFailMessage("Upload multipart lên MinIO hoặc đọc lại object thất bại: %s", uploadResult.getErrorMessage())
                    .isTrue();

            assertThat(uploadResult.getUploadStatusCode())
                    .withFailMessage("MinIO upload response phải thuộc dải 200/204, nhận được: %d", uploadResult.getUploadStatusCode())
                    .isIn(200, 204);

            assertThat(uploadResult.isVerifiedReadBack())
                    .withFailMessage("Nội dung object đọc lại từ publicUrl không trùng khớp với file gốc")
                    .isTrue();

        } finally {
            // 4.4 Dọn dẹp đúng object do test tạo ra
            verifier.cleanupTestObject(publicUrl);
        }
    }
}
