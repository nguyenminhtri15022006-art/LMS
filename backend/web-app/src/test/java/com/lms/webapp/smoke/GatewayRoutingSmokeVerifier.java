package com.lms.webapp.smoke;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public class GatewayRoutingSmokeVerifier {

    private static final Logger log = LoggerFactory.getLogger(GatewayRoutingSmokeVerifier.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GatewayRoutingSmokeVerifier() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    }

    public GatewayRoutingSmokeVerifier(HttpClient httpClient) {
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
    }

    public static class GatewayRoutingResult {
        private final boolean success;
        private final int httpStatus;
        private final boolean containsIdentifier;
        private final Long createdCategoryId;
        private final String errorMessage;

        public GatewayRoutingResult(boolean success, int httpStatus, boolean containsIdentifier,
                                    Long createdCategoryId, String errorMessage) {
            this.success = success;
            this.httpStatus = httpStatus;
            this.containsIdentifier = containsIdentifier;
            this.createdCategoryId = createdCategoryId;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getHttpStatus() {
            return httpStatus;
        }

        public boolean isContainsIdentifier() {
            return containsIdentifier;
        }

        public Long getCreatedCategoryId() {
            return createdCategoryId;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Chuẩn bị dữ liệu danh mục kiểm thử qua API Gateway trực tiếp (POST /api/course/categories)
     */
    public Long createTestCategory(String gatewayBaseUrl, String adminToken, String categoryName) {
        String cleanGatewayUrl = gatewayBaseUrl.replaceAll("/+$", "");
        try {
            String payload = objectMapper.writeValueAsString(Map.of("name", categoryName));
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(cleanGatewayUrl + "/api/course/categories"))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload));

            if (adminToken != null && !adminToken.isBlank()) {
                reqBuilder.header("Authorization", "Bearer " + adminToken);
            }

            HttpResponse<String> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.has("data") && root.get("data").has("id")) {
                    return root.get("data").get("id").asLong();
                }
            }
            log.warn("Create test category returned status {}: {}", response.statusCode(), response.body());
            return null;
        } catch (Exception e) {
            log.error("Failed to create test category at {}: {}", cleanGatewayUrl, e.getMessage());
            return null;
        }
    }

    /**
     * Kiểm chứng dữ liệu xuất hiện qua BFF -> Gateway -> course-service.
     * Assert cả status 200 VÀ nội dung chứa định danh duy nhất; test thất bại nếu gặp trang lỗi hoặc rỗng.
     */
    public GatewayRoutingResult verifyGatewayDataViaBff(String webAppBaseUrl, String expectedIdentifier, Long categoryId) {
        String cleanBffUrl = webAppBaseUrl.replaceAll("/+$", "");
        String url = cleanBffUrl + "/courses";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status != 200) {
                return new GatewayRoutingResult(false, status, false, categoryId,
                        "BFF returned non-200 status code: " + status);
            }

            String html = response.body();

            // Kiểm tra các dấu hiệu lỗi backend bị nuốt
            if (html.contains("alert-danger") || html.contains("Không thể tải danh sách khóa học")) {
                return new GatewayRoutingResult(false, status, false, categoryId,
                        "BFF rendered error banner, indicating Gateway or backend service failure");
            }

            // Kiểm tra định danh duy nhất có xuất hiện trong HTML trả về không
            boolean containsId = html.contains(expectedIdentifier);
            if (!containsId) {
                return new GatewayRoutingResult(false, status, false, categoryId,
                        "BFF HTML response does not contain the expected data identifier: " + expectedIdentifier);
            }

            return new GatewayRoutingResult(true, status, true, categoryId, null);

        } catch (Exception e) {
            log.error("Failed to connect to public BFF route {}: {}", url, e.getMessage());
            return new GatewayRoutingResult(false, -1, false, categoryId, e.getMessage());
        }
    }

    /**
     * Dọn dẹp dữ liệu danh mục kiểm thử sau khi verify
     */
    public boolean cleanupTestCategory(String gatewayBaseUrl, String adminToken, Long categoryId) {
        if (categoryId == null) return true;
        String cleanGatewayUrl = gatewayBaseUrl.replaceAll("/+$", "");
        try {
            HttpRequest.Builder reqBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(cleanGatewayUrl + "/api/course/categories/" + categoryId))
                    .timeout(Duration.ofSeconds(10))
                    .DELETE();

            if (adminToken != null && !adminToken.isBlank()) {
                reqBuilder.header("Authorization", "Bearer " + adminToken);
            }

            HttpResponse<Void> response = httpClient.send(reqBuilder.build(), HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 200 || response.statusCode() == 204 || response.statusCode() == 404;
        } catch (Exception e) {
            log.warn("Failed to cleanup test category {}: {}", categoryId, e.getMessage());
            return false;
        }
    }
}
