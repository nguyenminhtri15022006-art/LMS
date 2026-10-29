package com.lms.webapp.smoke;

import com.sun.net.httpserver.HttpServer;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

class SmokePreconditionsTest {
    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() { server.stop(0); }

    @Test
    void missingCredentialsFailWithoutDefaultAccount() {
        assertThatThrownBy(() -> SmokeTestSettings.required("smoke.test.password", k -> null, k -> null))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("SMOKE_TEST_PASSWORD");
    }

    @Test
    void credentialsCanComeFromEnvironmentWithoutTrimmingPassword() {
        assertThat(SmokeTestSettings.required("smoke.test.password", k -> null,
                k -> "SMOKE_TEST_PASSWORD".equals(k) ? " test-only password " : null))
                .isEqualTo(" test-only password ");
    }

    @Test
    void blankExplicitPropertyFailsWithoutExposingSecretOrFallingBack() {
        assertThatThrownBy(() -> SmokeTestSettings.required("smoke.admin.token", k -> " ", k -> "secret-env"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("SMOKE_ADMIN_TOKEN")
                .hasMessageNotContaining("secret-env");
    }

    @Test
    void genericCoursesPageCannotPassWithoutCreatedFixture() {
        server.createContext("/courses", exchange -> {
            byte[] html = "<a href='/courses'>courses</a>".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, html.length);
            exchange.getResponseBody().write(html);
            exchange.close();
        });
        assertThat(new GatewayRoutingSmokeVerifier()
                .verifyGatewayDataViaBff(baseUrl, "courses", null).isSuccess()).isFalse();
    }

    private MinioClient minio() {
        return MinioClient.builder().endpoint(baseUrl).region("us-east-1")
                .credentials("test-access-key", "test-only-secret-key").build();
    }

    @Test
    void cleanupSignsDeleteForExactObjectAndChecksStatus() {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        server.createContext("/smoke-bucket/smoke/run-123/avatar.png", exchange -> {
            method.set(exchange.getRequestMethod());
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
        new MinioDirectUploadSmokeVerifier().cleanupTestObject(
                minio(), "smoke-bucket", "smoke/run-123/avatar.png", "smoke/run-123/");
        assertThat(method.get()).isEqualTo("DELETE");
        assertThat(authorization.get()).startsWith("AWS4-HMAC-SHA256 ").contains("Signature=");
    }

    @Test
    void cleanupDeniedFailsInsteadOfBeingIgnored() {
        server.createContext("/smoke-bucket/smoke/run-123/avatar.png", exchange -> {
            byte[] body = "<Error><Code>AccessDenied</Code><Message>Denied</Message></Error>"
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/xml");
            exchange.sendResponseHeaders(403, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        assertThatThrownBy(() -> new MinioDirectUploadSmokeVerifier().cleanupTestObject(
                minio(), "smoke-bucket", "smoke/run-123/avatar.png", "smoke/run-123/"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("cleanup");
    }

    @Test
    void cleanupRejectsObjectsOutsideCurrentRunBeforeSendingRequest() {
        assertThatThrownBy(() -> new MinioDirectUploadSmokeVerifier().cleanupTestObject(
                minio(), "smoke-bucket", "avatars/existing-user.png", "smoke/run-123/"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
