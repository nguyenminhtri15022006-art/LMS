package com.lms.apigateway.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest(controllers = GatewayFallbackController.class)
@ActiveProfiles("test")
class GatewayFallbackControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("Red 1 -> Red 2 -> Green: fallbackReturns503WithStandardErrorJson")
    void fallbackReturns503WithStandardErrorJson() {
        webTestClient.get()
                .uri("/fallback/course-service")
                .accept(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isEqualTo(503)
                .expectHeader().contentType(MediaType.APPLICATION_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.error").isEqualTo("Service Unavailable")
                .jsonPath("$.code").isEqualTo(1405)
                .jsonPath("$.message").isEqualTo("Dịch vụ đích hiện không khả dụng hoặc không thể kết nối.")
                .jsonPath("$.service").isEqualTo("course-service")
                .jsonPath("$.timestamp").isNotEmpty();
    }
}
