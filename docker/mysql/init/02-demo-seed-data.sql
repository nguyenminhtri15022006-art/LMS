-- ==============================================================================
-- LMS Demo Seeding Script (Phase 5 - Dữ liệu Demo Chuẩn Hóa)
-- Tự động thực thi khi MySQL khởi tạo lần đầu, hoặc nạp thủ công qua mysql cli
-- Mật khẩu mặc định của tất cả tài khoản demo: admin123
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
    '$2a$10$yiW144zRZFTmpv6io8DYsePkVFFEVlP3YC56JghcOak4h8QLrOTva',
    'TS. Nguyễn Văn Hướng',
    'INSTRUCTOR',
    1, 1, 1, 'LOCAL', NOW(), NOW()
) ON DUPLICATE KEY UPDATE `full_name` = VALUES(`full_name`);

-- Demo Student (Học viên mẫu)
INSERT INTO `users` (
    `user_id`, `user_name`, `email`, `password_hash`, `full_name`,
    `role`, `role_selected`, `is_active`, `is_verified`, `auth_provider`, `created_at`, `updated_at`
) VALUES (
    '22222222-2222-2222-2222-222222222222',
    'student_demo',
    'student_demo@lms.edu.vn',
    '$2a$10$yiW144zRZFTmpv6io8DYsePkVFFEVlP3YC56JghcOak4h8QLrOTva',
    'Lê Hoàng Nam',
    'STUDENT',
    1, 1, 1, 'LOCAL', NOW(), NOW()
) ON DUPLICATE KEY UPDATE `full_name` = VALUES(`full_name`);

-- ------------------------------------------------------------------------------
-- 2. DỮ LIỆU DEMO COURSE SERVICE (course_lms)
-- ------------------------------------------------------------------------------
USE `course_lms`;

-- Danh mục khóa học chuẩn (id là bigint)
INSERT INTO `categories` (`id`, `name`, `slug`, `created_at`) VALUES
(1, 'Công nghệ Thông tin', 'cong-nghe-thong-tin', NOW()),
(2, 'Lập trình Web', 'lap-trinh-web', NOW()),
(3, 'Kiến trúc Microservices', 'kien-truc-microservices', NOW()),
(4, 'Cơ sở Dữ liệu & Cloud', 'co-so-du-lieu-va-cloud', NOW())
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

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
) ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);

-- Gán danh mục cho khóa học 1
INSERT INTO `course_categories` (`course_id`, `category_id`) VALUES
('c1111111-1111-1111-1111-111111111111', 1),
('c1111111-1111-1111-1111-111111111111', 2)
ON DUPLICATE KEY UPDATE `course_id` = VALUES(`course_id`);

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
) ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);

INSERT INTO `course_categories` (`course_id`, `category_id`) VALUES
('c2222222-2222-2222-2222-222222222222', 3),
('c2222222-2222-2222-2222-222222222222', 4)
ON DUPLICATE KEY UPDATE `course_id` = VALUES(`course_id`);

-- Thông báo mẫu cho khóa học
INSERT INTO `announcements` (`id`, `course_id`, `title`, `content`, `created_at`) VALUES
(1, 'c1111111-1111-1111-1111-111111111111', 'Chào mừng học viên đến với khóa học!', 'Khóa học đã sẵn sàng toàn bộ tài liệu và bài tập thực hành. Chúc các bạn học tập tốt!', NOW()),
(2, 'c2222222-2222-2222-2222-222222222222', 'Lịch ôn tập và thảo luận đồ án', 'Thứ 7 tuần này chúng ta có buổi livestream Q&A giải đáp thắc mắc về Resilience4j.', NOW())
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);

-- Lịch học trực tuyến mẫu
INSERT INTO `schedules` (`id`, `course_id`, `title`, `start_time`, `end_time`, `note`, `created_at`) VALUES
(1, 'c1111111-1111-1111-1111-111111111111', 'Buổi 1: Tổng quan dự án và thiết lập môi trường', DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 1 DAY) + INTERVAL 2 HOUR, 'Tham gia qua Google Meet: https://meet.google.com/abc-defg-hij', NOW()),
(2, 'c2222222-2222-2222-2222-222222222222', 'Buổi 1: Phân tách Microservices & Thiết kế API Contract', DATE_ADD(NOW(), INTERVAL 2 DAY), DATE_ADD(NOW(), INTERVAL 2 DAY) + INTERVAL 2 HOUR, 'Tham gia qua Google Meet: https://meet.google.com/xyz-uvwx-rst', NOW())
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);

-- Học viên demo ghi danh vào khóa học 1
INSERT INTO `enrollments` (`id`, `user_id`, `course_id`, `status`, `enrolled_at`, `updated_at`) VALUES
(1, '22222222-2222-2222-2222-222222222222', 'c1111111-1111-1111-1111-111111111111', 'ACTIVE', NOW(), NOW())
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`);

-- ------------------------------------------------------------------------------
-- 3. DỮ LIỆU DEMO BLOG SERVICE (blog_lms)
-- ------------------------------------------------------------------------------
USE `blog_lms`;

-- Thẻ bài viết chuẩn
INSERT INTO `tags` (`id`, `name`, `slug`, `created_at`, `updated_at`) VALUES
(1, 'Spring Boot', 'spring-boot', NOW(), NOW()),
(2, 'Docker', 'docker', NOW(), NOW()),
(3, 'Microservices', 'microservices', NOW(), NOW()),
(4, 'TDD', 'tdd', NOW(), NOW())
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);

-- Bài viết demo 1
INSERT INTO `posts` (
    `id`, `title`, `slug`, `summary`, `content`, `author_id`, `status`, `view_count`, `created_at`, `updated_at`
) VALUES (
    1,
    'Hướng dẫn xây dựng kiến trúc Microservices chuẩn doanh nghiệp với Spring Boot 3',
    'huong-dan-xay-dung-kien-truc-microservices-chuan-doanh-nghiep-voi-spring-boot-3',
    'Tổng quan về kiến trúc Microservices với Spring Cloud, Eureka và Gateway.',
    '# Kiến Trúc Microservices Với Spring Boot 3\n\nKiến trúc Microservices mang lại khả năng mở rộng linh hoạt và tính sẵn sàng cao cho các hệ thống lớn.\n\n## 1. Các Thành Phần Trọng Yếu\n- **Service Discovery**: Netflix Eureka giúp định vị tự động các dịch vụ.\n- **API Gateway**: Cổng điều hướng tập trung, xác thực JWT và kiểm soát lưu lượng.\n- **Circuit Breaker**: Resilience4j ngăn chặn lỗi xếp tầng (cascading failure).\n\n## 2. Kết Luận\nViệc áp dụng TDD và Docker Compose giúp đảm bảo độ tin cậy 100% trước khi triển khai sản xuất.',
    '11111111-1111-1111-1111-111111111111',
    'PUBLISHED',
    42,
    NOW(), NOW()
) ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);

INSERT INTO `post_tags` (`post_id`, `tag_id`) VALUES
(1, 1),
(1, 3)
ON DUPLICATE KEY UPDATE `post_id` = VALUES(`post_id`);

-- Bài viết demo 2
INSERT INTO `posts` (
    `id`, `title`, `slug`, `summary`, `content`, `author_id`, `status`, `view_count`, `created_at`, `updated_at`
) VALUES (
    2,
    'Thực hành Test-Driven Development (TDD) trong phát triển Backend hiện đại',
    'thuc-hanh-test-driven-development-tdd-trong-phat-trien-backend-hien-dai',
    'Kinh nghiệm áp dụng TDD Red-Green-Refactor giảm thiểu 90% lỗi hồi quy.',
    '# Quy Trình TDD Đỏ - Xanh - Tái Cấu Trúc (Red - Green - Refactor)\n\nPhương pháp TDD giúp lập trình viên viết code an toàn, giảm thiểu tối đa regression bugs.\n\n```java\n@Test\nvoid shouldReturnSuccessWhenInputIsValid() {\n    // Arrange, Act, Assert\n}\n```\n\nHãy luôn viết test trước khi viết mã nghiệp vụ!',
    '11111111-1111-1111-1111-111111111111',
    'PUBLISHED',
    28,
    NOW(), NOW()
) ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);

INSERT INTO `post_tags` (`post_id`, `tag_id`) VALUES
(2, 2),
(2, 4)
ON DUPLICATE KEY UPDATE `post_id` = VALUES(`post_id`);

-- Bình luận mẫu
INSERT INTO `comments` (`id`, `post_id`, `user_id`, `parent_id`, `content`, `created_at`, `updated_at`) VALUES
(1, 1, '22222222-2222-2222-2222-222222222222', NULL, 'Bài viết rất chi tiết và dễ hiểu, cảm ơn thầy!', NOW(), NOW()),
(2, 1, '11111111-1111-1111-1111-111111111111', 1, 'Cảm ơn em, chúc em thực hành tốt nhé!', NOW(), NOW())
ON DUPLICATE KEY UPDATE `content` = VALUES(`content`);
