package com.lms.serviceregistry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ServiceRegistryApplicationTests {

    @Autowired(required = false)
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("Case 1: Application context loads successfully")
    void contextLoads() {
    }

    @Test
    @DisplayName("Case 2: GET /eureka/apps returns HTTP 200 OK")
    void eurekaAppsEndpointReturns200() {
        assertThat(restTemplate).isNotNull();
        ResponseEntity<String> response = restTemplate.getForEntity("/eureka/apps", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
