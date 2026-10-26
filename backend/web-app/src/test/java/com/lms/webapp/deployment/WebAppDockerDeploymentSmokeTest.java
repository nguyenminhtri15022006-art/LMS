package com.lms.webapp.deployment;

import com.lms.webapp.smoke.ActuatorHealthSmokeVerifier;
import com.lms.webapp.smoke.AuthSessionSmokeVerifier;
import com.lms.webapp.smoke.GatewayRoutingSmokeVerifier;
import com.lms.webapp.smoke.MinioDirectUploadSmokeVerifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Docker Deployment Smoke Tests
 * Được cấu hình chạy độc lập trên môi trường Docker Compose container qua Maven Profile:
 * mvn test -pl web-app -f backend/pom.xml -Pdocker-smoke
 */
@Tag("docker-smoke")
class WebAppDockerDeploymentSmokeTest {

    private final String webAppBaseUrl = System.getProperty("smoke.webapp.url", "http://localhost:8088");
    private final String minioBaseUrl = System.getProperty("smoke.minio.url", "http://localhost:9000");

    @Test
    @DisplayName("1. Actuator Health endpoint returns UP status")
    void actuatorHealthReturnsUp() {
        ActuatorHealthSmokeVerifier verifier = new ActuatorHealthSmokeVerifier();
        String healthStatus = verifier.verifyActuatorHealth(webAppBaseUrl);
        assertThat(healthStatus).isEqualTo("UP");
    }

    @Test
    @DisplayName("2. Gateway routing is accessible via real public BFF endpoint")
    void gatewayRoutingAccessible() {
        GatewayRoutingSmokeVerifier verifier = new GatewayRoutingSmokeVerifier();
        int httpStatusCode = verifier.verifyGatewayRoutingViaBff(webAppBaseUrl);
        assertThat(httpStatusCode).isEqualTo(200);
    }

    @Test
    @DisplayName("3. Auth and Session flow works end-to-end with real database and gateway")
    void authAndSessionFlowE2E() {
        String testUser = System.getProperty("smoke.test.user", "test_student_smoke");
        String testPassword = System.getProperty("smoke.test.password", "Test@123456");

        AuthSessionSmokeVerifier verifier = new AuthSessionSmokeVerifier();
        AuthSessionSmokeVerifier.LoginProfileResult result = verifier.executeLoginAndFetchProfile(webAppBaseUrl, testUser, testPassword);

        assertThat(result.getSessionCookie()).isNotNull();
        assertThat(result.getHttpStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("4. MinIO direct upload with presigned POST policy works end-to-end")
    void directMinioUploadE2E() {
        MinioDirectUploadSmokeVerifier verifier = new MinioDirectUploadSmokeVerifier();
        int uploadResponseStatus = verifier.executeDirectUpload(webAppBaseUrl, minioBaseUrl);
        assertThat(uploadResponseStatus).isEqualTo(204);
    }
}
