package com.lms.webapp.smoke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

public class MinioDirectUploadSmokeVerifier {

    private static final Logger log = LoggerFactory.getLogger(MinioDirectUploadSmokeVerifier.class);
    private final HttpClient httpClient;

    public MinioDirectUploadSmokeVerifier() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build());
    }

    public MinioDirectUploadSmokeVerifier(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public static class DirectUploadResult {
        private final boolean success;
        private final int uploadStatusCode;
        private final boolean verifiedReadBack;
        private final String objectKey;
        private final String publicUrl;
        private final String errorMessage;

        public DirectUploadResult(boolean success, int uploadStatusCode, boolean verifiedReadBack,
                                  String objectKey, String publicUrl, String errorMessage) {
            this.success = success;
            this.uploadStatusCode = uploadStatusCode;
            this.verifiedReadBack = verifiedReadBack;
            this.objectKey = objectKey;
            this.publicUrl = publicUrl;
            this.errorMessage = errorMessage;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getUploadStatusCode() {
            return uploadStatusCode;
        }

        public boolean isVerifiedReadBack() {
            return verifiedReadBack;
        }

        public String getObjectKey() {
            return objectKey;
        }

        public String getPublicUrl() {
            return publicUrl;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    /**
     * Health check MinIO riêng biệt: tuyệt đối KHÔNG dùng để tuyên bố upload thành công.
     */
    public boolean verifyMinioLive(String minioBaseUrl) {
        String cleanMinioUrl = minioBaseUrl.replaceAll("/+$", "");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(cleanMinioUrl + "/minio/health/live"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            log.warn("MinIO live healthcheck failed at {}: {}", cleanMinioUrl, e.getMessage());
            return false;
        }
    }

    /**
     * Thực hiện upload multipart trực tiếp lên MinIO theo đúng cơ chế S3 POST Policy thực tế.
     * Xác minh object đã lưu bằng cách đọc lại nội dung thật.
     */
    public DirectUploadResult uploadViaPresignedPostPolicy(
            String uploadUrl,
            Map<String, String> formFields,
            String fileName,
            String fileType,
            byte[] fileContent,
            String publicUrl
    ) {
        String boundary = "----SmokeTestBoundary" + UUID.randomUUID().toString().replace("-", "");
        try {
            byte[] multipartBody = buildMultipartFormData(boundary, formFields, fileName, fileType, fileContent);

            HttpRequest uploadReq = HttpRequest.newBuilder()
                    .uri(URI.create(uploadUrl))
                    .timeout(Duration.ofSeconds(15))
                    .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                    .build();

            HttpResponse<byte[]> uploadRes = httpClient.send(uploadReq, HttpResponse.BodyHandlers.ofByteArray());
            int statusCode = uploadRes.statusCode();

            // S3 POST policy trả về 204 No Content hoặc 200 OK tùy policy cấu hình
            boolean uploadHttpOk = (statusCode == 200 || statusCode == 204);
            if (!uploadHttpOk) {
                String errBody = new String(uploadRes.body(), StandardCharsets.UTF_8);
                log.error("MinIO direct upload failed with status {}: {}", statusCode, errBody);
                return new DirectUploadResult(false, statusCode, false,
                        formFields != null ? formFields.get("key") : null,
                        publicUrl,
                        "Upload HTTP failed with status " + statusCode + ": " + errBody);
            }

            // Đọc lại nội dung object từ publicUrl để kiểm chứng tính toàn vẹn
            boolean readBackOk = false;
            if (publicUrl != null && !publicUrl.isBlank()) {
                readBackOk = verifyObjectContent(publicUrl, fileContent);
            }

            return new DirectUploadResult(readBackOk, statusCode, readBackOk,
                    formFields != null ? formFields.get("key") : null,
                    publicUrl,
                    readBackOk ? null : "Object verification read-back failed at " + publicUrl);

        } catch (Exception e) {
            log.error("Exception during direct upload to MinIO at {}: {}", uploadUrl, e.getMessage(), e);
            return new DirectUploadResult(false, -1, false,
                    formFields != null ? formFields.get("key") : null,
                    publicUrl, e.getMessage());
        }
    }

    /**
     * Xác minh object đã lưu bằng cách đọc lại nội dung thật.
     */
    public boolean verifyObjectContent(String objectUrl, byte[] expectedContent) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(objectUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                log.warn("Object read-back returned non-200 status {}: {}", response.statusCode(), objectUrl);
                return false;
            }

            byte[] actualBytes = response.body();
            if (expectedContent != null && expectedContent.length > 0) {
                return Arrays.equals(actualBytes, expectedContent);
            }
            return actualBytes != null && actualBytes.length > 0;
        } catch (Exception e) {
            log.warn("Failed to read back object from {}: {}", objectUrl, e.getMessage());
            return false;
        }
    }

    /**
     * Dọn dẹp đúng object do test tạo ra; tuyệt đối không xóa bucket hoặc dữ liệu khác.
     */
    public boolean cleanupTestObject(String objectUrl) {
        if (objectUrl == null || objectUrl.isBlank()) {
            return true;
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(objectUrl))
                    .timeout(Duration.ofSeconds(10))
                    .DELETE()
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return response.statusCode() == 204 || response.statusCode() == 200 || response.statusCode() == 404;
        } catch (Exception e) {
            log.warn("Cleanup test object failed at {}: {}", objectUrl, e.getMessage());
            return false;
        }
    }

    public static byte[] buildMultipartFormData(
            String boundary,
            Map<String, String> formFields,
            String fileName,
            String fileType,
            byte[] fileContent
    ) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String lineBreak = "\r\n";

        if (formFields != null) {
            for (Map.Entry<String, String> entry : formFields.entrySet()) {
                out.write(("--" + boundary + lineBreak).getBytes(StandardCharsets.UTF_8));
                out.write(("Content-Disposition: form-data; name=\"" + entry.getKey() + "\"" + lineBreak + lineBreak).getBytes(StandardCharsets.UTF_8));
                out.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                out.write(lineBreak.getBytes(StandardCharsets.UTF_8));
            }
        }

        out.write(("--" + boundary + lineBreak).getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"" + lineBreak).getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Type: " + fileType + lineBreak + lineBreak).getBytes(StandardCharsets.UTF_8));
        out.write(fileContent);
        out.write(lineBreak.getBytes(StandardCharsets.UTF_8));

        out.write(("--" + boundary + "--" + lineBreak).getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }
}
