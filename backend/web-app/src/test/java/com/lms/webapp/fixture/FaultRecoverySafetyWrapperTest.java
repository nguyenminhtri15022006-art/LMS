package com.lms.webapp.fixture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FaultRecoverySafetyWrapperTest {

    @Test
    @DisplayName("1. recoveryActionIsAlwaysExecuted: Recovery hook LUÔN LUÔN được gọi trong khối finally dù test ném lỗi")
    void recoveryExecutedEvenWhenTestFails() {
        AtomicBoolean recoveryInvoked = new AtomicBoolean(false);

        assertThatThrownBy(() -> FaultRecoverySafetyWrapper.executeWithSafeRecovery(
                "course-service",
                ctx -> {
                    ctx.log("Đang chạy test giả định lỗi...");
                    throw new RuntimeException("Assertion failure during fault test");
                },
                () -> recoveryInvoked.set(true),
                () -> true, // Health check báo OK
                Duration.ofMillis(500),
                Duration.ofMillis(50)
        )).isInstanceOf(RuntimeException.class).hasMessage("Assertion failure during fault test");

        assertThat(recoveryInvoked.get()).as("Recovery action bắt buộc phải chạy trong khối finally").isTrue();
    }

    @Test
    @DisplayName("2. successfulRecovery: Phục hồi thành công khi health check trả về true trong thời hạn")
    void successfulRecoveryFlow() throws Throwable {
        AtomicBoolean recovered = new AtomicBoolean(false);
        AtomicInteger pollAttempts = new AtomicInteger(0);

        FaultRecoverySafetyWrapper.executeWithSafeRecovery(
                "identity-service",
                ctx -> ctx.log("Thao tác kiểm thử diễn ra bình thường"),
                () -> recovered.set(true),
                () -> pollAttempts.incrementAndGet() >= 2, // Lần 1 false, lần 2 trở đi true
                Duration.ofMillis(500),
                Duration.ofMillis(30)
        );

        assertThat(recovered.get()).isTrue();
        assertThat(pollAttempts.get()).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("3. healthCheckTimeoutFailsTest: Không bao giờ tuyên bố đã phục hồi nếu health check timeout")
    void healthCheckTimeoutThrowsCriticalException() {
        AtomicBoolean recovered = new AtomicBoolean(false);

        assertThatThrownBy(() -> FaultRecoverySafetyWrapper.executeWithSafeRecovery(
                "blog-service",
                ctx -> ctx.log("Kiểm tra blog service fallback"),
                () -> recovered.set(true),
                () -> false, // Health check luôn báo false (downstream chưa dậy)
                Duration.ofMillis(200),
                Duration.ofMillis(50)
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CRITICAL FAULT SAFETY FAILURE")
                .hasMessageContaining("blog-service");

        assertThat(recovered.get()).isTrue();
    }

    @Test
    @DisplayName("4. suppressedExceptionOnDualFailure: Giữ ngoại lệ test làm lỗi chính, gắn lỗi phục hồi vào suppressed khi cả hai đều lỗi")
    void suppressedExceptionAttachedWhenBothFail() {
        assertThatThrownBy(() -> FaultRecoverySafetyWrapper.executeWithSafeRecovery(
                "api-gateway",
                ctx -> { throw new IllegalArgumentException("Test assertion error"); },
                () -> { throw new RuntimeException("Docker daemon socket error"); },
                () -> false,
                Duration.ofMillis(200),
                Duration.ofMillis(50)
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Test assertion error")
                .satisfies(ex -> {
                    assertThat(ex.getSuppressed()).hasSize(1);
                    assertThat(ex.getSuppressed()[0]).isInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("CRITICAL FAULT RECOVERY ERROR");
                });
    }

    @Test
    @DisplayName("5. assertionErrorPreservedOnRecoveryFailure: Bảo toàn AssertionError làm lỗi chính khi test ném AssertionError")
    void assertionErrorPreservedOnRecoveryFailure() {
        AssertionError original = new AssertionError("Expected 200 but was 503");
        assertThatThrownBy(() -> FaultRecoverySafetyWrapper.executeWithSafeRecovery(
                "course-service",
                ctx -> { throw original; },
                () -> { throw new RuntimeException("Compose start timeout"); },
                () -> true,
                Duration.ofMillis(100),
                Duration.ofMillis(20)
        )).isSameAs(original)
                .satisfies(err -> {
                    assertThat(err.getSuppressed()).hasSize(1);
                    assertThat(err.getSuppressed()[0]).isInstanceOf(IllegalStateException.class);
                });
    }
}
