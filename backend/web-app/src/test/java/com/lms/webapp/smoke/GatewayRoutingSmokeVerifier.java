package com.lms.webapp.smoke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class GatewayRoutingSmokeVerifier {

    private static final Logger log = LoggerFactory.getLogger(GatewayRoutingSmokeVerifier.class);
    private final HttpClient httpClient;

    public GatewayRoutingSmokeVerifier() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public int verifyGatewayRoutingViaBff(String baseUrl) {
        String url = baseUrl.replaceAll("/+$", "") + "/courses";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode();
        } catch (Exception e) {
            log.error("Failed to connect to public BFF route {}: {}", url, e.getMessage());
            return -1;
        }
    }
}
