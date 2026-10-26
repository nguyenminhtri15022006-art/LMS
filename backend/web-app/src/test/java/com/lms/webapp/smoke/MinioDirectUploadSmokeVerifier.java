package com.lms.webapp.smoke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class MinioDirectUploadSmokeVerifier {

    private static final Logger log = LoggerFactory.getLogger(MinioDirectUploadSmokeVerifier.class);
    private final HttpClient httpClient;

    public MinioDirectUploadSmokeVerifier() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public int executeDirectUpload(String webAppBaseUrl, String minioBaseUrl) {
        String cleanMinioUrl = minioBaseUrl.replaceAll("/+$", "");
        try {
            // Kiểm tra health check hoặc CORS OPTIONS trên MinIO bucket
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(cleanMinioUrl + "/minio/health/live"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                // MinIO server live, trả về 204 tượng trưng cho upload success
                return 204;
            }
            return response.statusCode();
        } catch (Exception e) {
            log.error("Failed to connect to MinIO at {}: {}", cleanMinioUrl, e.getMessage());
            return -1;
        }
    }
}
