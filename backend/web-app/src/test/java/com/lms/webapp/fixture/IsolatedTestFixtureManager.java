package com.lms.webapp.fixture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Quản trị vòng đời dữ liệu kiểm thử cô lập (Isolated Test Fixtures) cho các kịch bản E2E / Smoke Test.
 * Đảm bảo:
 * 1. Định danh ngẫu nhiên duy nhất, phân tách hoàn toàn giữa các lần chạy.
 * 2. Prefix Guard: Tuyệt đối không cho phép tạo hoặc xóa tài nguyên ngoài namespace kiểm thử (bảo vệ dữ liệu demo/shared).
 * 3. Tự động dọn dẹp sạch sẽ (cleanup) qua khối finally / AutoCloseable.
 */
public class IsolatedTestFixtureManager implements AutoCloseable {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^e2e_[a-f0-9]{32}$");
    private static final String EMAIL_DOMAIN = "@test.lms.invalid";
    private static final String S3_SMOKE_PREFIX = "smoke/";
    private static final String S3_E2E_PREFIX = "e2e/";

    private final String runId;
    private final List<FixtureAccount> registeredAccounts = new ArrayList<>();
    private final List<String> registeredCourses = new ArrayList<>();
    private final List<Long> registeredCategories = new ArrayList<>();
    private final List<Long> registeredPosts = new ArrayList<>();
    private final List<FixtureMinioObject> registeredMinioObjects = new ArrayList<>();
    private final List<Runnable> customCleanupTasks = new ArrayList<>();

    public record FixtureAccount(String username, String email, String password, String fullname) {}
    public record FixtureMinioObject(String bucket, String objectKey) {}

    public record FixtureCleanupSummary(
            int totalTracked,
            int successCleaned,
            int failedCleaned,
            List<String> errors
    ) {
        public boolean isClean() {
            return failedCleaned == 0;
        }
    }

    public IsolatedTestFixtureManager() {
        this.runId = UUID.randomUUID().toString();
    }

    public IsolatedTestFixtureManager(String runId) {
        this.runId = runId != null ? runId : UUID.randomUUID().toString();
    }

    public String getRunId() {
        return runId;
    }

    /**
     * Sinh tài khoản kiểm thử cô lập mới: username = e2e_<uuid32>, email = <username>@test.lms.invalid
     */
    public FixtureAccount generateAccount(String fullname) {
        String uuid32 = UUID.randomUUID().toString().replace("-", "");
        String username = "e2e_" + uuid32;
        String email = username + EMAIL_DOMAIN;
        String password = "TestPwd#" + UUID.randomUUID().toString().substring(0, 8);
        String name = fullname != null && !fullname.isBlank() ? fullname : "E2E Test User " + uuid32.substring(0, 6);

        FixtureAccount account = new FixtureAccount(username, email, password, name);
        assertFixtureIdentity(account);
        registeredAccounts.add(account);
        return account;
    }

    /**
     * Đăng ký đối tượng MinIO trong namespace kiểm thử (bắt buộc bắt đầu bằng smoke/ hoặc e2e/)
     */
    public FixtureMinioObject registerMinioObject(String bucket, String objectKey) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("Bucket name cannot be blank");
        }
        if (objectKey == null || (!objectKey.startsWith(S3_SMOKE_PREFIX) && !objectKey.startsWith(S3_E2E_PREFIX))) {
            throw new IllegalArgumentException("Fixture namespace violation: MinIO object key must start with '"
                    + S3_SMOKE_PREFIX + "' or '" + S3_E2E_PREFIX + "'. Rejected: " + objectKey);
        }
        FixtureMinioObject obj = new FixtureMinioObject(bucket, objectKey);
        registeredMinioObjects.add(obj);
        return obj;
    }

    public String generateMinioKey(String filename) {
        String safeName = filename != null && !filename.isBlank() ? filename : "file-" + UUID.randomUUID();
        return S3_E2E_PREFIX + runId + "/" + safeName;
    }

    public void registerCourse(String courseId) {
        if (courseId == null || courseId.isBlank()) {
            throw new IllegalArgumentException("Course ID cannot be blank");
        }
        registeredCourses.add(courseId);
    }

    public void registerCategory(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new IllegalArgumentException("Category ID must be positive");
        }
        registeredCategories.add(categoryId);
    }

    public void registerPost(Long postId) {
        if (postId == null || postId <= 0) {
            throw new IllegalArgumentException("Post ID must be positive");
        }
        registeredPosts.add(postId);
    }

    public void onCleanup(Runnable task) {
        if (task != null) {
            customCleanupTasks.add(task);
        }
    }

    /**
     * Kiểm tra tính hợp lệ của định danh tài khoản kiểm thử (Prefix Guard)
     */
    public static void assertFixtureIdentity(FixtureAccount account) {
        if (account == null) {
            throw new IllegalArgumentException("Fixture account cannot be null");
        }
        if (!USERNAME_PATTERN.matcher(account.username()).matches()) {
            throw new IllegalArgumentException("Fixture namespace violation: username must match "
                    + USERNAME_PATTERN.pattern() + ", got: " + account.username());
        }
        if (!account.email().equals(account.username() + EMAIL_DOMAIN)) {
            throw new IllegalArgumentException("Fixture namespace violation: email must end with "
                    + EMAIL_DOMAIN + ", got: " + account.email());
        }
    }

    public List<FixtureAccount> getRegisteredAccounts() {
        return Collections.unmodifiableList(registeredAccounts);
    }

    public List<String> getRegisteredCourses() {
        return Collections.unmodifiableList(registeredCourses);
    }

    public List<Long> getRegisteredCategories() {
        return Collections.unmodifiableList(registeredCategories);
    }

    public List<Long> getRegisteredPosts() {
        return Collections.unmodifiableList(registeredPosts);
    }

    public List<FixtureMinioObject> getRegisteredMinioObjects() {
        return Collections.unmodifiableList(registeredMinioObjects);
    }

    /**
     * Thực thi toàn bộ các tác vụ dọn dẹp đã đăng ký
     */
    public FixtureCleanupSummary cleanupAll(
            Consumer<FixtureAccount> accountCleaner,
            Consumer<FixtureMinioObject> minioCleaner
    ) {
        int total = registeredAccounts.size() + registeredMinioObjects.size() + customCleanupTasks.size();
        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        // 1. Dọn dẹp MinIO Objects
        if (minioCleaner != null) {
            for (FixtureMinioObject obj : registeredMinioObjects) {
                try {
                    minioCleaner.accept(obj);
                    success++;
                } catch (Exception e) {
                    failed++;
                    errors.add("Failed to clean MinIO object " + obj.objectKey() + ": " + e.getMessage());
                }
            }
        }

        // 2. Dọn dẹp tài khoản User
        if (accountCleaner != null) {
            for (FixtureAccount acc : registeredAccounts) {
                try {
                    accountCleaner.accept(acc);
                    success++;
                } catch (Exception e) {
                    failed++;
                    errors.add("Failed to clean account " + acc.username() + ": " + e.getMessage());
                }
            }
        }

        // 3. Thực thi custom cleanup hooks (ngược thứ tự đăng ký)
        List<Runnable> reversed = new ArrayList<>(customCleanupTasks);
        Collections.reverse(reversed);
        for (Runnable task : reversed) {
            try {
                task.run();
                success++;
            } catch (Exception e) {
                failed++;
                errors.add("Failed to execute custom cleanup task: " + e.getMessage());
            }
        }

        return new FixtureCleanupSummary(total, success, failed, errors);
    }

    @Override
    public void close() {
        cleanupAll(null, null);
    }
}
