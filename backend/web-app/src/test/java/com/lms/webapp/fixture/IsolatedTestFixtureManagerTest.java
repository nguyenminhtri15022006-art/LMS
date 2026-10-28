package com.lms.webapp.fixture;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IsolatedTestFixtureManagerTest {

    @Test
    @DisplayName("1. generateAccount: Sinh tài khoản định danh ngẫu nhiên đúng regex e2e_<uuid32> và domain @test.lms.invalid")
    void generateAccountConformsToSpec() {
        IsolatedTestFixtureManager manager = new IsolatedTestFixtureManager();

        IsolatedTestFixtureManager.FixtureAccount acc1 = manager.generateAccount("Nguyen Van A");
        IsolatedTestFixtureManager.FixtureAccount acc2 = manager.generateAccount("Tran Van B");

        assertThat(acc1.username()).matches("^e2e_[a-f0-9]{32}$");
        assertThat(acc1.email()).isEqualTo(acc1.username() + "@test.lms.invalid");
        assertThat(acc1.fullname()).isEqualTo("Nguyen Van A");
        assertThat(acc1.password()).startsWith("TestPwd#");

        // Không trùng lặp
        assertThat(acc1.username()).isNotEqualTo(acc2.username());
        assertThat(acc1.email()).isNotEqualTo(acc2.email());
        assertThat(manager.getRegisteredAccounts()).containsExactly(acc1, acc2);
    }

    @Test
    @DisplayName("2. assertFixtureIdentity: Từ chối tài khoản ngoài namespace kiểm thử (bảo vệ tài khoản thật)")
    void prefixGuardRejectsInvalidIdentities() {
        // Tài khoản không bắt đầu bằng e2e_
        IsolatedTestFixtureManager.FixtureAccount admin =
                new IsolatedTestFixtureManager.FixtureAccount("admin", "admin@test.lms.invalid", "pwd", "Admin");
        assertThatThrownBy(() -> IsolatedTestFixtureManager.assertFixtureIdentity(admin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Fixture namespace violation");

        // Email sai domain
        IsolatedTestFixtureManager.FixtureAccount wrongDomain =
                new IsolatedTestFixtureManager.FixtureAccount("e2e_0123456789abcdef0123456789abcdef", "user@gmail.com", "pwd", "User");
        assertThatThrownBy(() -> IsolatedTestFixtureManager.assertFixtureIdentity(wrongDomain))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Fixture namespace violation");
    }

    @Test
    @DisplayName("3. registerMinioObject: Chỉ chấp nhận prefix smoke/ hoặc e2e/, từ chối các prefix nhạy cảm")
    void minioObjectPrefixGuard() {
        IsolatedTestFixtureManager manager = new IsolatedTestFixtureManager("test-run-1");

        IsolatedTestFixtureManager.FixtureMinioObject validSmoke =
                manager.registerMinioObject("bucket-test", "smoke/run-1/avatar.png");
        IsolatedTestFixtureManager.FixtureMinioObject validE2E =
                manager.registerMinioObject("bucket-test", "e2e/run-1/video.mp4");

        assertThat(manager.getRegisteredMinioObjects()).containsExactly(validSmoke, validE2E);

        // Khóa không thuộc namespace cho phép
        assertThatThrownBy(() -> manager.registerMinioObject("bucket-test", "avatars/admin.png"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Fixture namespace violation");

        assertThatThrownBy(() -> manager.registerMinioObject("bucket-test", "course-thumbnails/1.jpg"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Fixture namespace violation");
    }

    @Test
    @DisplayName("4. generateMinioKey: Sinh đường dẫn S3 theo định dạng e2e/<runId>/<filename>")
    void generateMinioKeyIncludesRunId() {
        IsolatedTestFixtureManager manager = new IsolatedTestFixtureManager("run-abc-123");
        String key = manager.generateMinioKey("avatar.png");
        assertThat(key).isEqualTo("e2e/run-abc-123/avatar.png");
    }

    @Test
    @DisplayName("5. cleanupAll: Thực thi dọn dẹp đầy đủ MinIO, Account và custom tasks")
    void cleanupExecutesAllRegisteredHandlers() {
        IsolatedTestFixtureManager manager = new IsolatedTestFixtureManager();
        IsolatedTestFixtureManager.FixtureAccount acc = manager.generateAccount("Student");
        IsolatedTestFixtureManager.FixtureMinioObject obj = manager.registerMinioObject("b", "smoke/run/img.png");

        List<String> cleaned = new ArrayList<>();
        manager.onCleanup(() -> cleaned.add("custom-hook"));

        IsolatedTestFixtureManager.FixtureCleanupSummary summary = manager.cleanupAll(
                a -> cleaned.add("acc:" + a.username()),
                o -> cleaned.add("obj:" + o.objectKey())
        );

        assertThat(summary.isClean()).isTrue();
        assertThat(summary.totalTracked()).isEqualTo(3);
        assertThat(summary.successCleaned()).isEqualTo(3);
        assertThat(summary.failedCleaned()).isEqualTo(0);
        assertThat(cleaned).contains("obj:smoke/run/img.png", "acc:" + acc.username(), "custom-hook");
    }

    @Test
    @DisplayName("6. cleanupAll: Ghi nhận lỗi dọn dẹp mà không làm gián đoạn các tác vụ khác")
    void cleanupHandlesErrorsGracefully() {
        IsolatedTestFixtureManager manager = new IsolatedTestFixtureManager();
        manager.generateAccount("Student 1");
        manager.generateAccount("Student 2");

        AtomicInteger count = new AtomicInteger();
        IsolatedTestFixtureManager.FixtureCleanupSummary summary = manager.cleanupAll(
                a -> {
                    if (count.incrementAndGet() == 1) {
                        throw new RuntimeException("DB Connection timeout");
                    }
                },
                null
        );

        assertThat(summary.isClean()).isFalse();
        assertThat(summary.successCleaned()).isEqualTo(1);
        assertThat(summary.failedCleaned()).isEqualTo(1);
        assertThat(summary.errors()).hasSize(1);
        assertThat(summary.errors().get(0)).contains("DB Connection timeout");
    }
}
