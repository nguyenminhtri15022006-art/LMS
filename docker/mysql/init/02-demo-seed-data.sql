SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ==============================================================================
-- LMS Demo Seeding Script (Phase 5 - Dữ liệu Demo Chuẩn Hóa)
-- Lưu ý: Kịch bản này được nạp sau khi các microservices đã khởi chạy và Flyway
-- đã tạo cấu trúc bảng hoàn chỉnh. Sử dụng scripts/seed-demo-data.cjs để thực thi.
-- Mật khẩu người dùng demo được truyền động từ biến môi trường DEMO_USER_PASSWORD qua kịch bản nạp.
-- Các ID demo sử dụng dải ID độc lập (9000+) và kiểm tra quyền sở hữu nghiêm ngặt
-- (slug, author_id, user_name). Nếu trùng với dữ liệu không thuộc demo sẽ dừng và báo lỗi.
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. DỮ LIỆU DEMO IDENTITY SERVICE (identity_lms)
-- ------------------------------------------------------------------------------
USE `identity_lms`;

-- Demo Instructor (Giảng viên mẫu)
INSERT INTO `users` (
    `user_id`, `user_name`, `email`, `password_hash`, `full_name`,
    `role`, `role_selected`, `is_active`, `is_verified`, `auth_provider`, `created_at`, `updated_at`
) VALUES (
    '11111111-1111-1111-1111-111111111111',
    'instructor_demo',
    'instructor_demo@lms.edu.vn',
    '__DEMO_PASSWORD_HASH__',
    'TS. Nguyễn Văn Hướng',
    'INSTRUCTOR',
    1, 1, 1, 'LOCAL', NOW(), NOW()
) ON DUPLICATE KEY UPDATE
    `full_name` = IF(`user_id` = VALUES(`user_id`) AND `user_name` = VALUES(`user_name`), VALUES(`full_name`), (SELECT 1 UNION ALL SELECT 2));

-- Demo Student (Học viên mẫu)
INSERT INTO `users` (
    `user_id`, `user_name`, `email`, `password_hash`, `full_name`,
    `role`, `role_selected`, `is_active`, `is_verified`, `auth_provider`, `created_at`, `updated_at`
) VALUES (
    '22222222-2222-2222-2222-222222222222',
    'student_demo',
    'student_demo@lms.edu.vn',
    '__DEMO_PASSWORD_HASH__',
    'Lê Hoàng Nam',
    'STUDENT',
    1, 1, 1, 'LOCAL', NOW(), NOW()
) ON DUPLICATE KEY UPDATE
    `full_name` = IF(`user_id` = VALUES(`user_id`) AND `user_name` = VALUES(`user_name`), VALUES(`full_name`), (SELECT 1 UNION ALL SELECT 2));

-- ------------------------------------------------------------------------------
-- 2. DỮ LIỆU DEMO COURSE SERVICE (course_lms)
-- ------------------------------------------------------------------------------
USE `course_lms`;

-- Danh mục khóa học chuẩn (Dải ID demo 9001-9004; báo lỗi nếu trùng ID với slug khác)
INSERT INTO `categories` (`id`, `name`, `slug`, `created_at`) VALUES
(9001, 'Công nghệ Thông tin', 'cong-nghe-thong-tin', NOW()),
(9002, 'Lập trình Web', 'lap-trinh-web', NOW()),
(9003, 'Kiến trúc Microservices', 'kien-truc-microservices', NOW()),
(9004, 'Cơ sở Dữ liệu & Cloud', 'co-so-du-lieu-va-cloud', NOW())
ON DUPLICATE KEY UPDATE
    `name` = IF(`slug` = VALUES(`slug`), VALUES(`name`), (SELECT 1 UNION ALL SELECT 2));

-- Khóa học mẫu 1: Lập trình Web Fullstack với Spring Boot & Thymeleaf
INSERT INTO `courses` (
    `id`, `title`, `description`, `instructor_id`, `duration`, `level`, `status`,
    `mongo_structure_id`, `created_at`, `updated_at`
) VALUES (
    'c1111111-1111-1111-1111-111111111111',
    'Lập trình Web hiện đại với Spring Boot 3 & Thymeleaf',
    'Khóa học thực chiến phát triển ứng dụng Web chuẩn doanh nghiệp, tích hợp Spring Security, JPA và Bootstrap 5.',
    '11111111-1111-1111-1111-111111111111',
    24,
    'BEGINNER',
    'PUBLIC',
    'd1111111-1111-1111-1111-111111111111',
    NOW(), NOW()
) ON DUPLICATE KEY UPDATE
    `title` = IF(`instructor_id` = VALUES(`instructor_id`), VALUES(`title`), (SELECT 1 UNION ALL SELECT 2)),
    `description` = IF(`instructor_id` = VALUES(`instructor_id`), VALUES(`description`), (SELECT 1 UNION ALL SELECT 2));

-- Gán danh mục cho khóa học 1 (tra cứu dynamic ID theo slug để bảo đảm an toàn FK)
INSERT INTO `course_categories` (`course_id`, `category_id`)
SELECT 'c1111111-1111-1111-1111-111111111111', `id` FROM `categories` WHERE `slug` IN ('cong-nghe-thong-tin', 'lap-trinh-web')
ON DUPLICATE KEY UPDATE `course_id` = `course_id`;

-- Khóa học mẫu 2: Thiết kế Kiến trúc Microservices
INSERT INTO `courses` (
    `id`, `title`, `description`, `instructor_id`, `duration`, `level`, `status`,
    `mongo_structure_id`, `created_at`, `updated_at`
) VALUES (
    'c2222222-2222-2222-2222-222222222222',
    'Kiến trúc Microservices với Spring Cloud, Docker & Resilience4j',
    'Xây dựng hệ thống phân tán, cơ chế Circuit Breaker, Service Discovery Eureka, API Gateway và xử lý nhất quán dữ liệu.',
    '11111111-1111-1111-1111-111111111111',
    36,
    'INTERMEDIATE',
    'PUBLIC',
    'd2222222-2222-2222-2222-222222222222',
    NOW(), NOW()
) ON DUPLICATE KEY UPDATE
    `title` = IF(`instructor_id` = VALUES(`instructor_id`), VALUES(`title`), (SELECT 1 UNION ALL SELECT 2)),
    `description` = IF(`instructor_id` = VALUES(`instructor_id`), VALUES(`description`), (SELECT 1 UNION ALL SELECT 2));

-- Gán danh mục cho khóa học 2 (tra cứu dynamic ID theo slug để bảo đảm an toàn FK)
INSERT INTO `course_categories` (`course_id`, `category_id`)
SELECT 'c2222222-2222-2222-2222-222222222222', `id` FROM `categories` WHERE `slug` IN ('kien-truc-microservices', 'co-so-du-lieu-va-cloud')
ON DUPLICATE KEY UPDATE `course_id` = `course_id`;

-- Thông báo mẫu cho khóa học (Idempotent: chỉ thêm nếu chưa tồn tại tiêu đề)
INSERT INTO `announcements` (`id`, `course_id`, `title`, `content`, `created_at`)
SELECT 9001, 'c1111111-1111-1111-1111-111111111111', 'Chào mừng học viên đến với khóa học!', 'Khóa học đã sẵn sàng toàn bộ tài liệu và bài tập thực hành. Chúc các bạn học tập tốt!', NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `announcements` WHERE `course_id` = 'c1111111-1111-1111-1111-111111111111' AND `title` = 'Chào mừng học viên đến với khóa học!'
)
ON DUPLICATE KEY UPDATE `title` = IF(`course_id` = VALUES(`course_id`), VALUES(`title`), (SELECT 1 UNION ALL SELECT 2));

INSERT INTO `announcements` (`id`, `course_id`, `title`, `content`, `created_at`)
SELECT 9002, 'c2222222-2222-2222-2222-222222222222', 'Lịch ôn tập và thảo luận đồ án', 'Thứ 7 tuần này chúng ta có buổi livestream Q&A giải đáp thắc mắc về Resilience4j.', NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `announcements` WHERE `course_id` = 'c2222222-2222-2222-2222-222222222222' AND `title` = 'Lịch ôn tập và thảo luận đồ án'
)
ON DUPLICATE KEY UPDATE `title` = IF(`course_id` = VALUES(`course_id`), VALUES(`title`), (SELECT 1 UNION ALL SELECT 2));

-- Lịch học trực tuyến mẫu (Idempotent: chỉ thêm nếu chưa tồn tại tiêu đề)
INSERT INTO `schedules` (`id`, `course_id`, `title`, `start_time`, `end_time`, `note`, `created_at`)
SELECT 9001, 'c1111111-1111-1111-1111-111111111111', 'Buổi 1: Tổng quan dự án và thiết lập môi trường', DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY) + INTERVAL 2 HOUR, 'Tham gia qua Google Meet: https://meet.google.com/abc-defg-hij', NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `schedules` WHERE `course_id` = 'c1111111-1111-1111-1111-111111111111' AND `title` = 'Buổi 1: Tổng quan dự án và thiết lập môi trường'
)
ON DUPLICATE KEY UPDATE `title` = IF(`course_id` = VALUES(`course_id`), VALUES(`title`), (SELECT 1 UNION ALL SELECT 2));

INSERT INTO `schedules` (`id`, `course_id`, `title`, `start_time`, `end_time`, `note`, `created_at`)
SELECT 9002, 'c2222222-2222-2222-2222-222222222222', 'Buổi 1: Phân tách Microservices & Thiết kế API Contract', DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY) + INTERVAL 2 HOUR, 'Tham gia qua Google Meet: https://meet.google.com/xyz-uvwx-rst', NOW()
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM `schedules` WHERE `course_id` = 'c2222222-2222-2222-2222-222222222222' AND `title` = 'Buổi 1: Phân tách Microservices & Thiết kế API Contract'
)
ON DUPLICATE KEY UPDATE `title` = IF(`course_id` = VALUES(`course_id`), VALUES(`title`), (SELECT 1 UNION ALL SELECT 2));

-- Học viên demo ghi danh vào khóa học 1 (Idempotent trên unique key user_id + course_id)
INSERT INTO `enrollments` (`user_id`, `course_id`, `status`, `enrolled_at`, `updated_at`)
VALUES ('22222222-2222-2222-2222-222222222222', 'c1111111-1111-1111-1111-111111111111', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `status` = IF(`user_id` = VALUES(`user_id`) AND `course_id` = VALUES(`course_id`), VALUES(`status`), (SELECT 1 UNION ALL SELECT 2));

-- ------------------------------------------------------------------------------
-- 3. DỮ LIỆU DEMO BLOG SERVICE (blog_lms)
-- ------------------------------------------------------------------------------
USE `blog_lms`;

-- Thẻ bài viết chuẩn (Dải ID demo 9001-9004; báo lỗi nếu trùng ID với slug khác)
INSERT INTO `tags` (`id`, `name`, `slug`, `created_at`, `updated_at`) VALUES
(9001, 'Spring Boot', 'spring-boot', NOW(), NOW()),
(9002, 'Docker', 'docker', NOW(), NOW()),
(9003, 'Microservices', 'microservices', NOW(), NOW()),
(9004, 'TDD', 'tdd', NOW(), NOW())
ON DUPLICATE KEY UPDATE
    `name` = IF(`slug` = VALUES(`slug`), VALUES(`name`), (SELECT 1 UNION ALL SELECT 2));

-- Bài viết demo 1 (Xác minh quyền sở hữu author_id; báo lỗi nếu slug thuộc tác giả khác)
INSERT INTO `posts` (
    `id`, `title`, `slug`, `summary`, `content`, `author_id`, `status`, `view_count`, `created_at`, `updated_at`
) VALUES (
    9001,
    'Hướng dẫn xây dựng kiến trúc Microservices chuẩn doanh nghiệp với Spring Boot 3',
    'huong-dan-xay-dung-kien-truc-microservices-chuan-doanh-nghiep-voi-spring-boot-3',
    'Tổng quan về kiến trúc Microservices với Spring Cloud, Eureka và Gateway.',
    '# Kiến Trúc Microservices Với Spring Boot 3\n\nKiến trúc Microservices mang lại khả năng mở rộng linh hoạt và tính sẵn sàng cao cho các hệ thống lớn.\n\n## 1. Các Thành Phần Trọng Yếu\n- **Service Discovery**: Netflix Eureka giúp định vị tự động các dịch vụ.\n- **API Gateway**: Cổng điều hướng tập trung, xác thực JWT và kiểm soát lưu lượng.\n- **Circuit Breaker**: Resilience4j ngăn chặn lỗi xếp tầng (cascading failure).\n\n## 2. Kết Luận\nViệc áp dụng TDD và Docker Compose giúp đảm bảo độ tin cậy 100% trước khi triển khai sản xuất.',
    '11111111-1111-1111-1111-111111111111',
    'PUBLISHED',
    42,
    NOW(), NOW()
) ON DUPLICATE KEY UPDATE
    `title` = IF(`author_id` = VALUES(`author_id`), VALUES(`title`), (SELECT 1 UNION ALL SELECT 2)),
    `content` = IF(`author_id` = VALUES(`author_id`), VALUES(`content`), (SELECT 1 UNION ALL SELECT 2));

-- Gán tag cho bài viết 1 (tra cứu dynamic post_id và tag_id theo slug)
INSERT INTO `post_tags` (`post_id`, `tag_id`)
SELECT p.`id`, t.`id`
FROM `posts` p
JOIN `tags` t ON t.`slug` IN ('spring-boot', 'microservices')
WHERE p.`slug` = 'huong-dan-xay-dung-kien-truc-microservices-chuan-doanh-nghiep-voi-spring-boot-3'
ON DUPLICATE KEY UPDATE `post_id` = `post_id`;

-- Bài viết demo 2 (Xác minh quyền sở hữu author_id; báo lỗi nếu slug thuộc tác giả khác)
INSERT INTO `posts` (
    `id`, `title`, `slug`, `summary`, `content`, `author_id`, `status`, `view_count`, `created_at`, `updated_at`
) VALUES (
    9002,
    'Thực hành Test-Driven Development (TDD) trong phát triển Backend hiện đại',
    'thuc-hanh-test-driven-development-tdd-trong-phat-trien-backend-hien-dai',
    'Kinh nghiệm áp dụng TDD Red-Green-Refactor giảm thiểu 90% lỗi hồi quy.',
    '# Quy Trình TDD Đỏ - Xanh - Tái Cấu Trúc (Red - Green - Refactor)\n\nPhương pháp TDD giúp lập trình viên viết code an toàn, giảm thiểu tối đa regression bugs.\n\n```java\n@Test\nvoid shouldReturnSuccessWhenInputIsValid() {\n    // Arrange, Act, Assert\n}\n```\n\nHãy luôn viết test trước khi viết mã nghiệp vụ!',
    '11111111-1111-1111-1111-111111111111',
    'PUBLISHED',
    28,
    NOW(), NOW()
) ON DUPLICATE KEY UPDATE
    `title` = IF(`author_id` = VALUES(`author_id`), VALUES(`title`), (SELECT 1 UNION ALL SELECT 2)),
    `content` = IF(`author_id` = VALUES(`author_id`), VALUES(`content`), (SELECT 1 UNION ALL SELECT 2));

-- Gán tag cho bài viết 2 (tra cứu dynamic post_id và tag_id theo slug)
INSERT INTO `post_tags` (`post_id`, `tag_id`)
SELECT p.`id`, t.`id`
FROM `posts` p
JOIN `tags` t ON t.`slug` IN ('docker', 'tdd')
WHERE p.`slug` = 'thuc-hanh-test-driven-development-tdd-trong-phat-trien-backend-hien-dai'
ON DUPLICATE KEY UPDATE `post_id` = `post_id`;

-- Bình luận mẫu (tra cứu dynamic post_id và parent_id theo quan hệ thực tế)
INSERT INTO `comments` (`post_id`, `user_id`, `parent_id`, `content`, `created_at`, `updated_at`)
SELECT p.`id`, '22222222-2222-2222-2222-222222222222', NULL, 'Bài viết rất chi tiết và dễ hiểu, cảm ơn thầy!', NOW(), NOW()
FROM `posts` p
WHERE p.`slug` = 'huong-dan-xay-dung-kien-truc-microservices-chuan-doanh-nghiep-voi-spring-boot-3'
  AND NOT EXISTS (
      SELECT 1 FROM `comments` c
      WHERE c.`post_id` = p.`id`
        AND c.`user_id` = '22222222-2222-2222-2222-222222222222'
        AND c.`parent_id` IS NULL
  );

INSERT INTO `comments` (`post_id`, `user_id`, `parent_id`, `content`, `created_at`, `updated_at`)
SELECT p.`id`, '11111111-1111-1111-1111-111111111111', c.`id`, 'Cảm ơn em, chúc em thực hành tốt nhé!', NOW(), NOW()
FROM `posts` p
JOIN `comments` c ON c.`post_id` = p.`id` AND c.`user_id` = '22222222-2222-2222-2222-222222222222' AND c.`parent_id` IS NULL
WHERE p.`slug` = 'huong-dan-xay-dung-kien-truc-microservices-chuan-doanh-nghiep-voi-spring-boot-3'
  AND NOT EXISTS (
      SELECT 1 FROM `comments` c2
      WHERE c2.`post_id` = p.`id`
        AND c2.`user_id` = '11111111-1111-1111-1111-111111111111'
        AND c2.`parent_id` = c.`id`
  );
