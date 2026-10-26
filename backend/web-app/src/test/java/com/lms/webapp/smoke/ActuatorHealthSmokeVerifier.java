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

public class ActuatorHealthSmokeVerifier {

    private static final Logger log = LoggerFactory.getLogger(ActuatorHealthSmokeVerifier.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ActuatorHealthSmokeVerifier() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public String verifyActuatorHealth(String baseUrl) {
        String url = baseUrl.replaceAll("/+$", "") + "/actuator/health";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.has("status")) {
                    return root.get("status").asText();
                }
            }
            log.warn("Health endpoint returned HTTP status {}", response.statusCode());
            return "HTTP_" + response.statusCode();
        } catch (Exception e) {
            log.error("Failed to connect to health endpoint at {}: {}", url, e.getMessage());
            return "CONNECTION_FAILED: " + e.getMessage();
        }
    }
}
