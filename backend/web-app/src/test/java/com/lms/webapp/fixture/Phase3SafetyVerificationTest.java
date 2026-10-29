package com.lms.webapp.fixture;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Verification only: no real database, storage or Docker mutation. */
class Phase3SafetyVerificationTest {
    @Test
    void closeMustReportCleanupFailure() {
        var manager = new IsolatedTestFixtureManager("run-a");
        manager.onCleanup(() -> { throw new IllegalStateException("cleanup failed"); });
        assertThrows(Exception.class, manager::close,
            "try-with-resources must not report success when cleanup failed");
    }

    @Test
    void missingCleanersMustNotProduceCleanSummary() {
        var manager = new IsolatedTestFixtureManager("run-a");
        manager.generateAccount("Test Student");
        manager.registerCourse("test-course-id");
        var summary = manager.cleanupAll(null, null);
        assertThat(summary.isClean())
            .as("tracked resources with no cleaners must not be reported clean: %s", summary)
            .isFalse();
    }

    @Test
    void anotherRunsMinioObjectMustNotBecomeOwnedFixture() {
        var manager = new IsolatedTestFixtureManager("run-a");
        assertThrows(Exception.class,
            () -> manager.registerMinioObject("test-bucket", "e2e/run-b/other-user.png"),
            "test prefix alone does not establish ownership by this run");
    }

    @Test
    void recoveryFailureMustNotReplaceOriginalAssertionError() {
        var original = new AssertionError("original test assertion");
        var recovery = new IllegalStateException("recovery failed");
        Throwable thrown = assertThrows(Throwable.class, () ->
            FaultRecoverySafetyWrapper.executeWithSafeRecovery("fake-service",
                context -> { throw original; },
                () -> { throw recovery; },
                () -> true, Duration.ofMillis(100), Duration.ofMillis(20)));
        assertThat(thrown).as("original assertion must remain the primary failure").isSameAs(original);
        assertThat(thrown.getSuppressed()).isNotEmpty();
    }

    @Test
    void healthRedirectToLoginMustNotCountAsReady() throws Exception {
        assertHealthRejected(302, "", "/login");
    }

    @Test
    void actuatorBodyDownMustNotCountAsReady() throws Exception {
        assertHealthRejected(200, "{\"status\":\"DOWN\"}", null);
    }

    private void assertHealthRejected(int code, String body, String redirect) throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/actuator/health", exchange -> {
            if (redirect != null) exchange.getResponseHeaders().set("Location", redirect);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(code, bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        try {
            var endpoint = DockerStackReadinessVerifier.ServiceEndpoint.http(
                "web-app", "127.0.0.1", server.getAddress().getPort(), "/actuator/health");
            var status = new DockerStackReadinessVerifier().checkEndpoint(endpoint, 1000);
            assertThat(status.isUp()).as("HTTP %s body %s must not mean ready", code, body).isFalse();
        } finally {
            server.stop(0);
        }
    }
}
