package com.lms.webapp.fixture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Quản trị vòng đời dữ liệu kiểm thử cô lập (Isolated Test Fixtures) cho các kịch bản E2E / Smoke Test.
 * Đảm bảo:
 * 1. Định danh ngẫu nhiên duy nhất, phân tách hoàn toàn giữa các lần chạy.
 * 2. Prefix Guard & RunId Ownership Guard: Chặn tạo hoặc xóa tài nguyên ngoài namespace kiểm thử,
 *    đồng thời chặn quyền sở hữu tài nguyên thuộc về runId khác.
 * 3. Tự động dọn dẹp sạch sẽ (cleanup) qua khối finally / AutoCloseable, không nuốt lỗi và không xóa lặp.
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

    // Theo dõi các tài nguyên đã được dọn dẹp thành công để tránh gọi lặp lại
    private final Set<FixtureAccount> cleanedAccounts = new HashSet<>();
    private final Set<FixtureMinioObject> cleanedMinioObjects = new HashSet<>();
    private final Set<String> cleanedCourses = new HashSet<>();
    private final Set<Long> cleanedCategories = new HashSet<>();
    private final Set<Long> cleanedPosts = new HashSet<>();
    private final Set<Runnable> executedCustomTasks = new HashSet<>();

    private Consumer<FixtureAccount> defaultAccountCleaner;
    private Consumer<FixtureMinioObject> defaultMinioCleaner;
    private Consumer<String> defaultCourseCleaner;
    private Consumer<Long> defaultCategoryCleaner;
    private Consumer<Long> defaultPostCleaner;

    public record FixtureAccount(String username, String email, String password, String fullname) {}
    public record FixtureMinioObject(String bucket, String objectKey) {}

    public record FixtureCleanupSummary(
            int totalTracked,
            int successCleaned,
            int failedCleaned,
            List<String> errors
    ) {
        public boolean isClean() {
            if (totalTracked == 0) {
                return failedCleaned == 0 && errors.isEmpty();
            }
            return successCleaned == totalTracked && failedCleaned == 0 && errors.isEmpty();
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

    public void setDefaultAccountCleaner(Consumer<FixtureAccount> cleaner) {
        this.defaultAccountCleaner = cleaner;
    }

    public void setDefaultMinioCleaner(Consumer<FixtureMinioObject> cleaner) {
        this.defaultMinioCleaner = cleaner;
    }

    public void setDefaultCourseCleaner(Consumer<String> cleaner) {
        this.defaultCourseCleaner = cleaner;
    }

    public void setDefaultCategoryCleaner(Consumer<Long> cleaner) {
        this.defaultCategoryCleaner = cleaner;
    }

    public void setDefaultPostCleaner(Consumer<Long> cleaner) {
        this.defaultPostCleaner = cleaner;
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
     * Đăng ký đối tượng MinIO trong namespace kiểm thử.
     * Bắt buộc:
     * 1. Bắt đầu bằng smoke/ hoặc e2e/ (Prefix Guard)
     * 2. Phải thuộc quyền sở hữu của runId hiện tại: e2e/<runId>/... hoặc smoke/<runId>/... (Ownership Guard)
     */
    public FixtureMinioObject registerMinioObject(String bucket, String objectKey) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("Bucket name cannot be blank");
        }
        if (objectKey == null || (!objectKey.startsWith(S3_SMOKE_PREFIX) && !objectKey.startsWith(S3_E2E_PREFIX))) {
            throw new IllegalArgumentException("Fixture namespace violation: MinIO object key must start with '"
                    + S3_SMOKE_PREFIX + "' or '" + S3_E2E_PREFIX + "'. Rejected: " + objectKey);
        }

        String expectedSmoke = S3_SMOKE_PREFIX + runId + "/";
        String expectedE2e = S3_E2E_PREFIX + runId + "/";
        if (!objectKey.startsWith(expectedSmoke) && !objectKey.startsWith(expectedE2e)) {
            throw new IllegalArgumentException("Fixture ownership violation: MinIO object key '" + objectKey
                    + "' does not belong to runId '" + runId + "'");
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

    public FixtureCleanupSummary cleanupAll(
            Consumer<FixtureAccount> accountCleaner,
            Consumer<FixtureMinioObject> minioCleaner
    ) {
        return cleanupAll(accountCleaner, minioCleaner, defaultCourseCleaner, defaultCategoryCleaner, defaultPostCleaner);
    }

    /**
     * Thực thi toàn bộ các tác vụ dọn dẹp đã đăng ký cho cả 5 nhóm tài nguyên + custom tasks.
     * Đảm bảo:
     * - Báo cáo đầy đủ mọi tài nguyên được theo dõi.
     * - Nếu có tài nguyên chưa có cleaner, ghi nhận lỗi và không báo clean.
     * - Không xóa lặp tài nguyên đã được dọn thành công trước đó.
     */
    public FixtureCleanupSummary cleanupAll(
            Consumer<FixtureAccount> accountCleaner,
            Consumer<FixtureMinioObject> minioCleaner,
            Consumer<String> courseCleaner,
            Consumer<Long> categoryCleaner,
            Consumer<Long> postCleaner
    ) {
        int total = registeredAccounts.size()
                + registeredMinioObjects.size()
                + registeredCourses.size()
                + registeredCategories.size()
                + registeredPosts.size()
                + customCleanupTasks.size();

        int success = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        Consumer<FixtureAccount> effAccountCleaner = accountCleaner != null ? accountCleaner : defaultAccountCleaner;
        Consumer<FixtureMinioObject> effMinioCleaner = minioCleaner != null ? minioCleaner : defaultMinioCleaner;
        Consumer<String> effCourseCleaner = courseCleaner != null ? courseCleaner : defaultCourseCleaner;
        Consumer<Long> effCategoryCleaner = categoryCleaner != null ? categoryCleaner : defaultCategoryCleaner;
        Consumer<Long> effPostCleaner = postCleaner != null ? postCleaner : defaultPostCleaner;

        // 1. MinIO Objects
        for (FixtureMinioObject obj : registeredMinioObjects) {
            if (cleanedMinioObjects.contains(obj)) {
                success++;
            } else if (effMinioCleaner != null) {
                try {
                    effMinioCleaner.accept(obj);
                    cleanedMinioObjects.add(obj);
                    success++;
                } catch (Exception e) {
                    failed++;
                    errors.add("Failed to clean MinIO object " + obj.objectKey() + ": " + e.getMessage());
                }
            } else {
                failed++;
                errors.add("No cleaner provided for MinIO object: " + obj.objectKey());
            }
        }

        // 2. Tài khoản User
        for (FixtureAccount acc : registeredAccounts) {
            if (cleanedAccounts.contains(acc)) {
                success++;
            } else if (effAccountCleaner != null) {
                try {
                    effAccountCleaner.accept(acc);
                    cleanedAccounts.add(acc);
                    success++;
                } catch (Exception e) {
                    failed++;
                    errors.add("Failed to clean account " + acc.username() + ": " + e.getMessage());
                }
            } else {
                failed++;
                errors.add("No cleaner provided for account: " + acc.username());
            }
        }

        // 3. Khóa học
        for (String courseId : registeredCourses) {
            if (cleanedCourses.contains(courseId)) {
                success++;
            } else if (effCourseCleaner != null) {
                try {
                    effCourseCleaner.accept(courseId);
                    cleanedCourses.add(courseId);
                    success++;
                } catch (Exception e) {
                    failed++;
                    errors.add("Failed to clean course " + courseId + ": " + e.getMessage());
                }
            } else {
                failed++;
                errors.add("No cleaner provided or cleanup not supported for course: " + courseId);
            }
        }

        // 4. Danh mục Category
        for (Long catId : registeredCategories) {
            if (cleanedCategories.contains(catId)) {
                success++;
            } else if (effCategoryCleaner != null) {
                try {
                    effCategoryCleaner.accept(catId);
                    cleanedCategories.add(catId);
                    success++;
                } catch (Exception e) {
                    failed++;
                    errors.add("Failed to clean category " + catId + ": " + e.getMessage());
                }
            } else {
                failed++;
                errors.add("No cleaner provided or cleanup not supported for category: " + catId);
            }
        }

        // 5. Bài viết Post
        for (Long postId : registeredPosts) {
            if (cleanedPosts.contains(postId)) {
                success++;
            } else if (effPostCleaner != null) {
                try {
                    effPostCleaner.accept(postId);
                    cleanedPosts.add(postId);
                    success++;
                } catch (Exception e) {
                    failed++;
                    errors.add("Failed to clean post " + postId + ": " + e.getMessage());
                }
            } else {
                failed++;
                errors.add("No cleaner provided or cleanup not supported for post: " + postId);
            }
        }

        // 6. Custom cleanup tasks (thực thi theo thứ tự đảo ngược)
        List<Runnable> reversed = new ArrayList<>(customCleanupTasks);
        Collections.reverse(reversed);
        for (Runnable task : reversed) {
            if (executedCustomTasks.contains(task)) {
                success++;
            } else {
                try {
                    task.run();
                    executedCustomTasks.add(task);
                    success++;
                } catch (Exception e) {
                    failed++;
                    errors.add("Failed to execute custom cleanup task: " + e.getMessage());
                }
            }
        }

        return new FixtureCleanupSummary(total, success, failed, errors);
    }

    @Override
    public void close() throws Exception {
        FixtureCleanupSummary summary = cleanupAll(defaultAccountCleaner, defaultMinioCleaner, defaultCourseCleaner, defaultCategoryCleaner, defaultPostCleaner);
        if (!summary.isClean()) {
            throw new IllegalStateException("Fixture cleanup failed or incomplete: " + summary.errors());
        }
    }
}
