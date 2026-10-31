-- ==============================================================================
-- EDULEARN LMS - MASTER DATABASE BACKUP SCRIPT
-- Dự án: Hệ thống Quản lý Học tập EduLearn LMS (Microservices & Thymeleaf Web Portal)
-- Môn học: Lập trình Web / Đồ án Cuối kỳ
-- Thành viên: Nguyễn Minh Trí (24110359) & Nông Văn Cường (24110176)
-- Giảng viên hướng dẫn: ThS. Nguyễn Hữu Trung
-- Ngày xuất dữ liệu: 31/10/2026
-- Bảng mã: UTF-8 Unicode (utf8mb4 / utf8mb4_unicode_ci)
-- ==============================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ==============================================================================
-- CƠ SỞ DỮ LIỆU: identity_lms (Quản lý Tài khoản, Phân quyền RBAC, Token, OTP)
-- ==============================================================================
CREATE DATABASE IF NOT EXISTS `identity_lms` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `identity_lms`;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `email_verifications`;
DROP TABLE IF EXISTS `refresh_tokens`;
DROP TABLE IF EXISTS `users`;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `users` (
    `user_id`       CHAR(36)     NOT NULL PRIMARY KEY,
    `user_name`     VARCHAR(55)  NOT NULL UNIQUE,
    `email`         VARCHAR(255) NOT NULL UNIQUE,
    `password_hash` VARCHAR(255) NOT NULL,
    `full_name`     VARCHAR(255) NOT NULL,
    `dob`           DATE         NULL,
    `avatar_url`    VARCHAR(500) NULL,
    `bio`           TEXT         NULL,
    `phone`         VARCHAR(20)  NULL,
    `role`          ENUM('ADMIN','INSTRUCTOR','STUDENT') NOT NULL,
    `role_selected` TINYINT(1)   NOT NULL DEFAULT 0,
    `is_active`     TINYINT(1)   NOT NULL DEFAULT 1,
    `is_verified`   TINYINT(1)   NOT NULL DEFAULT 0,
    `auth_provider` ENUM('LOCAL','GOOGLE') NOT NULL DEFAULT 'LOCAL',
    `provider_id`   VARCHAR(100) NULL,
    `last_login_at` DATETIME     NULL,
    `created_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `refresh_tokens` (
    `token_id`    CHAR(36)     NOT NULL PRIMARY KEY,
    `user_id`     CHAR(36)     NOT NULL,
    `token_hash`  VARCHAR(64)  NOT NULL UNIQUE,
    `device_info` VARCHAR(255) NULL,
    `ip_address`  VARCHAR(45)  NULL,
    `expires_at`  DATETIME     NOT NULL,
    `is_revoked`  TINYINT(1)   NOT NULL DEFAULT 0,
    `created_at`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_refresh_token_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `email_verifications` (
    `id`         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id`    CHAR(36)     NOT NULL,
    `token`      VARCHAR(255) NOT NULL UNIQUE,
    `type`       ENUM('VERIFY_EMAIL','RESET_PASSWORD') NOT NULL,
    `expires_at` DATETIME     NOT NULL,
    `used_at`    DATETIME     NULL,
    `attempts`   TINYINT      NOT NULL DEFAULT 0,
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_email_verif_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX `idx_users_active_verified` ON `users` (`is_active`, `is_verified`);
CREATE INDEX `idx_users_role_created_at` ON `users` (`role`, `created_at`);
CREATE INDEX `idx_refresh_tokens_user_revoked` ON `refresh_tokens` (`user_id`, `is_revoked`);
CREATE INDEX `idx_email_verif_user_type_expires` ON `email_verifications` (`user_id`, `type`, `expires_at`);

-- Dữ liệu mẫu Users (Mật khẩu mặc định: Password123@)
INSERT INTO `users` (`user_id`, `user_name`, `email`, `password_hash`, `full_name`, `role`, `role_selected`, `is_active`, `is_verified`, `auth_provider`, `created_at`, `updated_at`) VALUES
('00000000-0000-0000-0000-000000000000', 'admin', 'admin@lms.edu.vn', '$2a$10$g4/75S9E1HK.Z2mh3BuG3O01RFOajKnhRqEL7pto2ROSgid0P6XOa', 'Quản Trị Viên Hệ Thống', 'ADMIN', 1, 1, 1, 'LOCAL', NOW(), NOW()),
('11111111-1111-1111-1111-111111111111', 'instructor_demo', 'instructor_demo@lms.edu.vn', '$2a$10$g4/75S9E1HK.Z2mh3BuG3O01RFOajKnhRqEL7pto2ROSgid0P6XOa', 'TS. Nguyễn Văn Hướng', 'INSTRUCTOR', 1, 1, 1, 'LOCAL', NOW(), NOW()),
('33333333-3333-3333-3333-333333333333', 'nguyenminhtri', 'nguyenminhtri@lms.edu.vn', '$2a$10$g4/75S9E1HK.Z2mh3BuG3O01RFOajKnhRqEL7pto2ROSgid0P6XOa', 'ThS. Nguyễn Minh Trí', 'INSTRUCTOR', 1, 1, 1, 'LOCAL', NOW(), NOW()),
('55555555-5555-5555-5555-555555555555', 'cuongnv', 'cuongnv@lms.edu.vn', '$2a$10$g4/75S9E1HK.Z2mh3BuG3O01RFOajKnhRqEL7pto2ROSgid0P6XOa', 'Nông Văn Cường', 'INSTRUCTOR', 1, 1, 1, 'LOCAL', NOW(), NOW()),
('44444444-4444-4444-4444-444444444444', 'lethimailinh', 'mailinh@lms.edu.vn', '$2a$10$g4/75S9E1HK.Z2mh3BuG3O01RFOajKnhRqEL7pto2ROSgid0P6XOa', 'TS. Lê Thị Mai Linh', 'INSTRUCTOR', 1, 1, 1, 'LOCAL', NOW(), NOW()),
('22222222-2222-2222-2222-222222222222', 'student_demo', 'student_demo@lms.edu.vn', '$2a$10$g4/75S9E1HK.Z2mh3BuG3O01RFOajKnhRqEL7pto2ROSgid0P6XOa', 'Lê Hoàng Nam', 'STUDENT', 1, 1, 1, 'LOCAL', NOW(), NOW()),
('22222222-2222-2222-2222-222222222223', 'tranvantuan', 'tuan.tran@lms.edu.vn', '$2a$10$g4/75S9E1HK.Z2mh3BuG3O01RFOajKnhRqEL7pto2ROSgid0P6XOa', 'Trần Văn Tuấn', 'STUDENT', 1, 1, 1, 'LOCAL', NOW(), NOW()),
('22222222-2222-2222-2222-222222222224', 'hoangthimai', 'mai.hoang@lms.edu.vn', '$2a$10$g4/75S9E1HK.Z2mh3BuG3O01RFOajKnhRqEL7pto2ROSgid0P6XOa', 'Hoàng Thị Mai', 'STUDENT', 1, 1, 1, 'LOCAL', NOW(), NOW()),
('22222222-2222-2222-2222-222222222225', 'nguyenvanan', 'an.nguyen@lms.edu.vn', '$2a$10$g4/75S9E1HK.Z2mh3BuG3O01RFOajKnhRqEL7pto2ROSgid0P6XOa', 'Nguyễn Văn An', 'STUDENT', 1, 1, 1, 'LOCAL', NOW(), NOW());


-- ==============================================================================
-- CƠ SỞ DỮ LIỆU: course_lms (Quản lý Danh mục, Khóa học, Đề cương, Ghi danh, Tiến độ)
-- ==============================================================================
CREATE DATABASE IF NOT EXISTS `course_lms` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `course_lms`;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `lesson_progress`;
DROP TABLE IF EXISTS `announcements`;
DROP TABLE IF EXISTS `schedules`;
DROP TABLE IF EXISTS `enrollments`;
DROP TABLE IF EXISTS `course_requirements`;
DROP TABLE IF EXISTS `course_learning_points`;
DROP TABLE IF EXISTS `course_categories`;
DROP TABLE IF EXISTS `courses`;
DROP TABLE IF EXISTS `categories`;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `categories` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(200) NOT NULL UNIQUE,
    `slug` VARCHAR(200) NULL UNIQUE,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `courses` (
    `id` VARCHAR(36) NOT NULL PRIMARY KEY,
    `title` VARCHAR(500) NOT NULL,
    `description` TEXT NULL,
    `instructor_id` VARCHAR(36) NOT NULL,
    `duration` INT NULL,
    `level` ENUM('BEGINNER', 'INTERMEDIATE', 'ADVANCED') NULL,
    `thumbnail_url` VARCHAR(1000) NULL,
    `status` ENUM('PRIVATE', 'PUBLIC', 'LOCKED') NOT NULL DEFAULT 'PRIVATE',
    `price` DECIMAL(12,2) NULL DEFAULT 0.00,
    `meeting_url` VARCHAR(500) NULL,
    `google_event_id` VARCHAR(200) NULL,
    `mongo_structure_id` VARCHAR(36) NOT NULL UNIQUE,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `course_categories` (
    `course_id` VARCHAR(36) NOT NULL,
    `category_id` BIGINT NOT NULL,
    PRIMARY KEY (`course_id`, `category_id`),
    CONSTRAINT `fk_cc_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cc_category` FOREIGN KEY (`category_id`) REFERENCES `categories` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `course_learning_points` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `course_id` VARCHAR(36) NOT NULL,
    `point` VARCHAR(500) NOT NULL,
    `order_index` INT NOT NULL DEFAULT 0,
    CONSTRAINT `fk_clp_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `course_requirements` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `course_id` VARCHAR(36) NOT NULL,
    `requirement` VARCHAR(500) NOT NULL,
    `order_index` INT NOT NULL DEFAULT 0,
    CONSTRAINT `fk_cr_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `enrollments` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` VARCHAR(36) NOT NULL,
    `course_id` VARCHAR(36) NOT NULL,
    `enrolled_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `uq_enrollment` UNIQUE (`user_id`, `course_id`),
    CONSTRAINT `fk_enroll_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `schedules` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `course_id` VARCHAR(36) NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `start_time` DATETIME NOT NULL,
    `end_time` DATETIME NOT NULL,
    `meeting_url` VARCHAR(500) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_sched_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `announcements` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `course_id` VARCHAR(36) NOT NULL,
    `title` VARCHAR(255) NOT NULL,
    `content` TEXT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_ann_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `lesson_progress` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` VARCHAR(36) NOT NULL,
    `course_id` VARCHAR(36) NOT NULL,
    `lesson_id` VARCHAR(36) NOT NULL,
    `is_completed` TINYINT(1) NOT NULL DEFAULT 0,
    `completed_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `uq_lesson_progress` UNIQUE (`user_id`, `course_id`, `lesson_id`),
    CONSTRAINT `fk_lp_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dữ liệu Danh mục
INSERT INTO `categories` (`id`, `name`, `slug`, `created_at`) VALUES
(9001, 'Công nghệ Thông tin', 'cong-nghe-thong-tin', NOW()),
(9002, 'Lập trình Web', 'lap-trinh-web', NOW()),
(9003, 'Kiến trúc Microservices', 'kien-truc-microservices', NOW()),
(9004, 'Cơ sở Dữ liệu & Cloud', 'co-so-du-lieu-va-cloud', NOW()),
(9005, 'Cấu trúc Dữ liệu & Giải thuật', 'cau-truc-du-lieu-va-giai-thuat', NOW()),
(9006, 'DevOps & Docker', 'devops-va-docker', NOW());

-- Dữ liệu Khóa học mẫu
INSERT INTO `courses` (`id`, `title`, `description`, `instructor_id`, `duration`, `level`, `thumbnail_url`, `status`, `price`, `meeting_url`, `mongo_structure_id`, `created_at`, `updated_at`) VALUES
('c1111111-1111-1111-1111-111111111111', 'Lập trình Web hiện đại với Spring Boot 3 & Thymeleaf', 'Khóa học thực chiến phát triển ứng dụng Web chuẩn doanh nghiệp, tích hợp Spring Security 6, JPA, Docker và Thymeleaf Server-Side Rendering.', '11111111-1111-1111-1111-111111111111', 36, 'BEGINNER', 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800', 'PUBLIC', 0.00, 'https://meet.google.com/abc-defg-hij', 'd1111111-1111-1111-1111-111111111111', NOW(), NOW()),
('c2222222-2222-2222-2222-222222222222', 'Kiến trúc Microservices với Spring Cloud, Docker & Resilience4j', 'Xây dựng hệ thống phân tán chịu lỗi cao, cơ chế Circuit Breaker, Service Discovery Eureka, API Gateway và Token Offloading.', '33333333-3333-3333-3333-333333333333', 48, 'INTERMEDIATE', 'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=800', 'PUBLIC', 0.00, 'https://meet.google.com/xyz-uvwx-rst', 'd2222222-2222-2222-2222-222222222222', NOW(), NOW()),
('c3333333-3333-3333-3333-333333333333', 'Thực hành Cấu trúc dữ liệu & Thuật toán và Luyện Code Sandbox', 'Rèn luyện tư duy thuật toán, giải quyết các bài toán kinh điển từ LeetCode, chấm điểm tự động qua engine Judge0 Sandbox.', '55555555-5555-5555-5555-555555555555', 40, 'ADVANCED', 'https://images.unsplash.com/photo-1515879218367-8466d910aaa4?w=800', 'PUBLIC', 799000.00, 'https://meet.google.com/opq-rstu-vwx', 'd3333333-3333-3333-3333-333333333333', NOW(), NOW());

INSERT INTO `course_categories` (`course_id`, `category_id`) VALUES
('c1111111-1111-1111-1111-111111111111', 9001),
('c1111111-1111-1111-1111-111111111111', 9002),
('c2222222-2222-2222-2222-222222222222', 9003),
('c2222222-2222-2222-2222-222222222222', 9006),
('c3333333-3333-3333-3333-333333333333', 9005);

INSERT INTO `course_learning_points` (`course_id`, `point`, `order_index`) VALUES
('c1111111-1111-1111-1111-111111111111', 'Làm chủ Spring Boot 3.3.6 và kiến trúc Monolith BFF', 1),
('c1111111-1111-1111-1111-111111111111', 'Tích hợp Spring Security 6 với xác thực JWT và OAuth2', 2),
('c1111111-1111-1111-1111-111111111111', 'Xây dựng giao diện Server-Side Rendering với Thymeleaf', 3),
('c2222222-2222-2222-2222-222222222222', 'Thiết kế hệ sinh thái Microservices chịu lỗi cao với Resilience4j', 1),
('c2222222-2222-2222-2222-222222222222', 'Cấu hình Token Offloading và Header Stripping tại Gateway', 2);

-- Ghi danh học viên vào khóa học
INSERT INTO `enrollments` (`user_id`, `course_id`, `enrolled_at`) VALUES
('22222222-2222-2222-2222-222222222222', 'c1111111-1111-1111-1111-111111111111', NOW()),
('22222222-2222-2222-2222-222222222222', 'c3333333-3333-3333-3333-333333333333', NOW()),
('22222222-2222-2222-2222-222222222223', 'c1111111-1111-1111-1111-111111111111', NOW());

-- Lịch học trực tuyến
INSERT INTO `schedules` (`course_id`, `title`, `start_time`, `end_time`, `meeting_url`) VALUES
('c1111111-1111-1111-1111-111111111111', 'Buổi 1: Tổng quan Spring Boot 3 & Cấu hình IDE', '2026-11-05 18:30:00', '2026-11-05 21:00:00', 'https://meet.google.com/abc-defg-hij'),
('c1111111-1111-1111-1111-111111111111', 'Buổi 2: Thiết kế REST API & JPA Mapping', '2026-11-08 18:30:00', '2026-11-08 21:00:00', 'https://meet.google.com/abc-defg-hij'),
('c2222222-2222-2222-2222-222222222222', 'Buổi 1: Eureka Service Discovery & Cloud Gateway', '2026-11-06 18:30:00', '2026-11-06 21:00:00', 'https://meet.google.com/xyz-uvwx-rst');

-- Thông báo khóa học
INSERT INTO `announcements` (`course_id`, `title`, `content`, `created_at`) VALUES
('c1111111-1111-1111-1111-111111111111', 'Chào mừng các bạn đến với khóa học Spring Boot 3', 'Các bạn vui lòng cài đặt JDK 21, IntelliJ IDEA và Docker Desktop trước buổi học đầu tiên.', NOW()),
('c1111111-1111-1111-1111-111111111111', 'Tài liệu slide Bài 1 đã tải lên MinIO', 'Slide bài giảng tổng quan đã có thể tải về từ tab Tài liệu học tập.', NOW());

-- Tiến độ học tập (Lesson Progress)
INSERT INTO `lesson_progress` (`user_id`, `course_id`, `lesson_id`, `is_completed`, `completed_at`) VALUES
('22222222-2222-2222-2222-222222222222', 'c1111111-1111-1111-1111-111111111111', 'les-11111111-1111-1111-1111-111111111111', 1, NOW()),
('22222222-2222-2222-2222-222222222222', 'c1111111-1111-1111-1111-111111111111', 'les-11111111-1111-1111-1111-111111111112', 1, NOW()),
('22222222-2222-2222-2222-222222222222', 'c2222222-2222-2222-2222-222222222222', 'les-22222222-2222-2222-2222-222222222221', 1, NOW());


-- ==============================================================================
-- CƠ SỞ DỮ LIỆU: assignment_lms (Quản lý Bài tập lập trình, Testcases, Quiz, Leaderboard)
-- ==============================================================================
CREATE DATABASE IF NOT EXISTS `assignment_lms` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `assignment_lms`;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `grades`;
DROP TABLE IF EXISTS `quiz_answer_records`;
DROP TABLE IF EXISTS `quiz_attempts`;
DROP TABLE IF EXISTS `quiz_questions`;
DROP TABLE IF EXISTS `answers`;
DROP TABLE IF EXISTS `questions`;
DROP TABLE IF EXISTS `quizzes`;
DROP TABLE IF EXISTS `test_cases`;
DROP TABLE IF EXISTS `problems`;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `problems` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL,
    `slug` VARCHAR(255) NOT NULL UNIQUE,
    `description` LONGTEXT NOT NULL,
    `input_description` TEXT NULL,
    `output_description` TEXT NULL,
    `difficulty` ENUM('EASY', 'MEDIUM', 'HARD') NOT NULL,
    `time_limit` DOUBLE NOT NULL DEFAULT 1.0,
    `memory_limit` INT NOT NULL DEFAULT 256,
    `points` INT NOT NULL DEFAULT 100,
    `course_id` VARCHAR(36) NULL,
    `is_public` TINYINT(1) NOT NULL DEFAULT 1,
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
    `created_by` VARCHAR(36) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `test_cases` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `problem_id` BIGINT NOT NULL,
    `input_data` LONGTEXT NOT NULL,
    `expected_output` LONGTEXT NOT NULL,
    `is_hidden` TINYINT(1) NOT NULL DEFAULT 0,
    `order_index` INT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_tc_problem` FOREIGN KEY (`problem_id`) REFERENCES `problems` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `quizzes` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(255) NOT NULL,
    `description` TEXT NULL,
    `course_id` VARCHAR(36) NULL,
    `time_limit_minutes` INT NOT NULL DEFAULT 15,
    `max_attempts` INT NOT NULL DEFAULT 3,
    `pass_score` DOUBLE NOT NULL DEFAULT 5.0,
    `is_published` TINYINT(1) NOT NULL DEFAULT 1,
    `is_deleted` TINYINT(1) NOT NULL DEFAULT 0,
    `created_by` VARCHAR(36) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `questions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `content` TEXT NOT NULL,
    `topic` VARCHAR(100) NULL,
    `difficulty` ENUM('EASY', 'MEDIUM', 'HARD') NOT NULL DEFAULT 'EASY',
    `is_multiple_choice` TINYINT(1) NOT NULL DEFAULT 0,
    `score` DOUBLE NOT NULL DEFAULT 1.0,
    `created_by` VARCHAR(36) NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `answers` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `question_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `is_correct` TINYINT(1) NOT NULL DEFAULT 0,
    `order_index` INT NOT NULL DEFAULT 0,
    CONSTRAINT `fk_ans_question` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `quiz_questions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `quiz_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `order_index` INT NOT NULL DEFAULT 0,
    `score` DOUBLE NOT NULL DEFAULT 1.0,
    CONSTRAINT `uq_quiz_question` UNIQUE (`quiz_id`, `question_id`),
    CONSTRAINT `fk_qq_quiz` FOREIGN KEY (`quiz_id`) REFERENCES `quizzes` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_qq_question` FOREIGN KEY (`question_id`) REFERENCES `questions` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `quiz_attempts` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `quiz_id` BIGINT NOT NULL,
    `user_id` VARCHAR(36) NOT NULL,
    `attempt_number` INT NOT NULL DEFAULT 1,
    `score` DOUBLE NULL,
    `status` ENUM('IN_PROGRESS', 'SUBMITTED', 'GRADED', 'TIMED_OUT') NOT NULL DEFAULT 'IN_PROGRESS',
    `in_progress_lock` VARCHAR(64) NULL,
    `started_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `submitted_at` DATETIME NULL,
    `graded_at` DATETIME NULL,
    CONSTRAINT `uq_one_in_progress` UNIQUE (`in_progress_lock`),
    CONSTRAINT `fk_qa_quiz` FOREIGN KEY (`quiz_id`) REFERENCES `quizzes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `quiz_answer_records` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `attempt_id` BIGINT NOT NULL,
    `question_id` BIGINT NOT NULL,
    `selected_answer_ids` VARCHAR(255) NULL,
    `is_correct` TINYINT(1) NULL,
    `score_earned` DOUBLE NOT NULL DEFAULT 0.0,
    CONSTRAINT `uq_attempt_question` UNIQUE (`attempt_id`, `question_id`),
    CONSTRAINT `fk_qar_attempt` FOREIGN KEY (`attempt_id`) REFERENCES `quiz_attempts` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `grades` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` VARCHAR(36) NOT NULL,
    `course_id` VARCHAR(36) NOT NULL,
    `quiz_score` DOUBLE NULL,
    `assignment_score` DOUBLE NULL,
    `total_score` DOUBLE NULL,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `uq_user_course_grade` UNIQUE (`user_id`, `course_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Dữ liệu Bài tập lập trình mẫu (Coding Problems)
INSERT INTO `problems` (`id`, `title`, `slug`, `description`, `input_description`, `output_description`, `difficulty`, `time_limit`, `memory_limit`, `points`, `course_id`, `is_public`, `created_by`, `created_at`, `updated_at`) VALUES
(1, 'Hai Số Có Tổng Bằng Target (Two Sum)', 'two-sum', 'Cho một mảng các số nguyên nums và một số nguyên target, hãy trả về chỉ số của hai số trong mảng sao cho tổng của chúng đúng bằng target.
Bạn có thể giả định rằng mỗi đầu vào sẽ có chính xác một lời giải và bạn không được sử dụng cùng một phần tử hai lần.', 'Dòng 1: Số nguyên n (số phần tử của mảng)
Dòng 2: n số nguyên cách nhau bởi khoảng trắng
Dòng 3: Số nguyên target', 'Hai chỉ số cách nhau bởi khoảng trắng (theo thứ tự tăng dần)', 'EASY', 1.0, 256, 100, 'c1111111-1111-1111-1111-111111111111', 1, '55555555-5555-5555-5555-555555555555', NOW(), NOW()),
(2, 'Kiểm Tra Dấu Ngoặc Hợp Lệ (Valid Parentheses)', 'valid-parentheses', 'Cho một chuỗi s chỉ chứa các ký tự '(', ')', '{', '}', '[' và ']'. Hãy xác định xem chuỗi đầu vào có hợp lệ hay không.
Chuỗi hợp lệ nếu: Dấu ngoặc mở phải được đóng bởi cùng loại dấu ngoặc, và phải được đóng theo đúng thứ tự.', 'Một dòng duy nhất chứa chuỗi ký tự s', 'In ra true nếu hợp lệ, ngược lại in false', 'EASY', 1.0, 128, 100, 'c1111111-1111-1111-1111-111111111111', 1, '55555555-5555-5555-5555-555555555555', NOW(), NOW()),
(3, 'Đảo Ngược Chuỗi Ký Tự (Reverse String)', 'reverse-string', 'Viết chương trình nhập vào một chuỗi ký tự và in ra chuỗi đảo ngược.', 'Một dòng chứa chuỗi ký tự ban đầu', 'Chuỗi ký tự sau khi đảo ngược', 'EASY', 1.0, 128, 100, 'c3333333-3333-3333-3333-333333333333', 1, '55555555-5555-5555-5555-555555555555', NOW(), NOW()),
(4, 'Dãy Con Tăng Dài Nhất (Longest Increasing Subsequence)', 'longest-increasing-subsequence', 'Cho mảng số nguyên nums, tìm độ dài của dãy con tăng nghiêm ngặt dài nhất.', 'Dòng 1: n số lượng phần tử
Dòng 2: n số nguyên', 'Độ dài lớn nhất của dãy con tăng', 'MEDIUM', 2.0, 256, 100, 'c3333333-3333-3333-3333-333333333333', 1, '55555555-5555-5555-5555-555555555555', NOW(), NOW()),
(5, 'Bài Toán Cái Túi 0/1 (Knapsack Problem)', 'knapsack-problem', 'Cho n đồ vật, mỗi đồ vật có trọng lượng w[i] và giá trị v[i]. Hãy chọn các đồ vật để tổng giá trị lớn nhất mà tổng trọng lượng không vượt quá W.', 'Dòng 1: n và W
Dòng 2: n số nguyên trọng lượng
Dòng 3: n số nguyên giá trị', 'Tổng giá trị lớn nhất có thể đạt được', 'MEDIUM', 2.0, 256, 100, 'c3333333-3333-3333-3333-333333333333', 1, '55555555-5555-5555-5555-555555555555', NOW(), NOW()),
(6, 'Đường Đi Ngắn Nhất Dijkstra (Dijkstra Shortest Path)', 'dijkstra-shortest-path', 'Cho đồ thị có hướng trọng số không âm gồm V đỉnh và E cạnh. Tìm khoảng cách ngắn nhất từ đỉnh nguồn S đến tất cả các đỉnh còn lại.', 'Dòng 1: V, E, S
E dòng tiếp theo: u, v, w (cạnh từ u đến v với trọng số w)', 'V số nguyên là khoảng cách ngắn nhất từ S đến từng đỉnh (in -1 nếu không đến được)', 'HARD', 3.0, 512, 100, 'c3333333-3333-3333-3333-333333333333', 1, '55555555-5555-5555-5555-555555555555', NOW(), NOW());

-- Testcases cho Bài 1 (Two Sum)
INSERT INTO `test_cases` (`id`, `problem_id`, `input_data`, `expected_output`, `is_hidden`, `order_index`) VALUES
(1, 1, '4\n2 7 11 15\n9', '0 1', 0, 1),
(2, 1, '3\n3 2 4\n6', '1 2', 0, 2),
(3, 1, '2\n3 3\n6', '0 1', 1, 3),
(4, 1, '5\n1 5 3 7 9\n12', '1 3', 1, 4);

-- Testcases cho Bài 2 (Valid Parentheses)
INSERT INTO `test_cases` (`id`, `problem_id`, `input_data`, `expected_output`, `is_hidden`, `order_index`) VALUES
(5, 2, '()', 'true', 0, 1),
(6, 2, '()[]{}', 'true', 0, 2),
(7, 2, '(]', 'false', 0, 3),
(8, 2, '([)]', 'false', 1, 4),
(9, 2, '{[]}', 'true', 1, 5);

-- Dữ liệu Đề thi trắc nghiệm (Quizzes)
INSERT INTO `quizzes` (`id`, `title`, `description`, `course_id`, `time_limit_minutes`, `max_attempts`, `pass_score`, `is_published`, `created_by`, `created_at`, `updated_at`) VALUES
(1, 'Kiểm Tra Kiến Trúc Microservices & Spring Boot 3', 'Đề kiểm tra 15 phút đánh giá sự hiểu biết về Service Discovery Eureka, API Gateway, JWT và Spring Boot Starter.', 'c1111111-1111-1111-1111-111111111111', 15, 3, 5.0, 1, '55555555-5555-5555-5555-555555555555', NOW(), NOW()),
(2, 'Đánh Giá Kiến Thức Cơ Sở Dữ Liệu & RESTful API', 'Đề kiểm tra 20 phút về tối ưu hóa truy vấn JPA N+1, Transaction và bảo mật API.', 'c2222222-2222-2222-2222-222222222222', 20, 3, 5.0, 1, '55555555-5555-5555-5555-555555555555', NOW(), NOW());

-- Câu hỏi trắc nghiệm
INSERT INTO `questions` (`id`, `content`, `topic`, `difficulty`, `is_multiple_choice`, `score`, `created_by`) VALUES
(1, 'Trong ngôn ngữ Java, từ khóa nào sau đây dùng để kế thừa một lớp cha?', 'Java Core', 'EASY', 0, 2.5, '55555555-5555-5555-5555-555555555555'),
(2, 'Annotation nào trong Spring Boot đánh dấu một lớp là Bean xử lý logic nghiệp vụ?', 'Spring Boot', 'EASY', 0, 2.5, '55555555-5555-5555-5555-555555555555'),
(3, 'Thành phần nào sau đây trong hệ sinh thái Spring Cloud đóng vai trò cổng vào duy nhất (Single Entry Point)?', 'Microservices', 'EASY', 0, 2.5, '55555555-5555-5555-5555-555555555555'),
(4, 'Giao thức nào thường được sử dụng phổ biến nhất để giao tiếp đồng bộ giữa các microservice?', 'Microservices', 'EASY', 0, 2.5, '55555555-5555-5555-5555-555555555555');

INSERT INTO `answers` (`id`, `question_id`, `content`, `is_correct`, `order_index`) VALUES
(1, 1, 'implements', 0, 1),
(2, 1, 'extends', 1, 2),
(3, 1, 'inherits', 0, 3),
(4, 1, 'super', 0, 4),
(5, 2, '@Repository', 0, 1),
(6, 2, '@Controller', 0, 2),
(7, 2, '@Service', 1, 3),
(8, 2, '@Configuration', 0, 4),
(9, 3, 'Netflix Eureka Server', 0, 1),
(10, 3, 'Spring Cloud Gateway', 1, 2),
(11, 3, 'Config Server', 0, 3),
(12, 3, 'Zipkin Tracing', 0, 4),
(13, 4, 'HTTP REST API (JSON)', 1, 1),
(14, 4, 'Trực tiếp truy cập chung CSDL MySQL', 0, 2),
(15, 4, 'Chia sẻ file trên ổ đĩa cứng chung', 0, 3),
(16, 4, 'Socket TCP thô không mã hóa', 0, 4);

INSERT INTO `quiz_questions` (`quiz_id`, `question_id`, `order_index`, `score`) VALUES
(1, 1, 1, 2.5),
(1, 2, 2, 2.5),
(1, 3, 3, 2.5),
(1, 4, 4, 2.5);

-- Bảng điểm Gradebook & Leaderboard
INSERT INTO `grades` (`user_id`, `course_id`, `quiz_score`, `assignment_score`, `total_score`) VALUES
('22222222-2222-2222-2222-222222222222', 'c1111111-1111-1111-1111-111111111111', 10.0, 9.5, 9.75),
('22222222-2222-2222-2222-222222222223', 'c1111111-1111-1111-1111-111111111111', 9.0, 8.5, 8.75),
('22222222-2222-2222-2222-222222222224', 'c1111111-1111-1111-1111-111111111111', 8.5, 9.0, 8.75);


-- ==============================================================================
-- CƠ SỞ DỮ LIỆU: blog_lms (Diễn đàn Công nghệ, Bài viết Markdown, Thẻ Tags, Bình luận, Vote)
-- ==============================================================================
CREATE DATABASE IF NOT EXISTS `blog_lms` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `blog_lms`;

SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS `votes`;
DROP TABLE IF EXISTS `comments`;
DROP TABLE IF EXISTS `post_tags`;
DROP TABLE IF EXISTS `posts`;
DROP TABLE IF EXISTS `tags`;
SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE `tags` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `name` VARCHAR(100) NOT NULL UNIQUE,
    `slug` VARCHAR(100) NOT NULL UNIQUE,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `posts` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `title` VARCHAR(500) NOT NULL,
    `slug` VARCHAR(500) NOT NULL UNIQUE,
    `summary` VARCHAR(1000) NULL,
    `content` LONGTEXT NOT NULL,
    `thumbnail` VARCHAR(1000) NULL,
    `author_id` VARCHAR(36) NOT NULL,
    `status` ENUM('DRAFT', 'PUBLISHED', 'HIDDEN') NOT NULL DEFAULT 'DRAFT',
    `view_count` BIGINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `post_tags` (
    `post_id` BIGINT NOT NULL,
    `tag_id` BIGINT NOT NULL,
    PRIMARY KEY (`post_id`, `tag_id`),
    CONSTRAINT `fk_pt_post` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_pt_tag` FOREIGN KEY (`tag_id`) REFERENCES `tags` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `comments` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `post_id` BIGINT NOT NULL,
    `user_id` VARCHAR(36) NOT NULL,
    `parent_id` BIGINT NULL,
    `content` TEXT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `fk_cmt_post` FOREIGN KEY (`post_id`) REFERENCES `posts` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_cmt_parent` FOREIGN KEY (`parent_id`) REFERENCES `comments` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE `votes` (
    `id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    `user_id` VARCHAR(36) NOT NULL,
    `target_id` BIGINT NOT NULL,
    `target_type` ENUM('POST', 'COMMENT') NOT NULL,
    `vote_type` ENUM('UPVOTE', 'DOWNVOTE') NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT `uq_votes_user_target` UNIQUE (`user_id`, `target_id`, `target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Thẻ Tags
INSERT INTO `tags` (`id`, `name`, `slug`, `created_at`, `updated_at`) VALUES
(1, 'Spring Boot 3', 'spring-boot-3', NOW(), NOW()),
(2, 'Microservices', 'microservices', NOW(), NOW()),
(3, 'Docker & DevOps', 'docker-devops', NOW(), NOW()),
(4, 'Java 21 LTS', 'java-21-lts', NOW(), NOW()),
(5, 'Judge0 Sandbox', 'judge0-sandbox', NOW(), NOW()),
(6, 'Database & Cache', 'database-cache', NOW(), NOW()),
(7, 'Security & JWT', 'security-jwt', NOW(), NOW()),
(8, 'AI & GenAI', 'ai-genai', NOW(), NOW());

-- 7 Bài viết kỹ thuật thực tế với hình ảnh Unsplash và nội dung Markdown
INSERT INTO `posts` (`id`, `title`, `slug`, `summary`, `content`, `thumbnail`, `author_id`, `status`, `view_count`, `created_at`, `updated_at`) VALUES
(1, 'Kiến trúc Microservices với Spring Boot 3 & Spring Cloud 2023: Kinh nghiệm thực chiến', 'kien-truc-microservices-spring-boot-3-kinh-nghiem-thuc-chien', 'Tổng hợp kiến trúc tổng thể, mô hình Token Offloading và Resilience4j Circuit Breaker trong hệ thống LMS phân tán.', '## 1. Giới thiệu tổng quan kiến trúc\n\nKhi xây dựng một nền tảng học tập trực tuyến quy mô lớn, việc lựa chọn kiến trúc **Microservices** giúp chia nhỏ bài toán phức tạp thành các dịch vụ độc lập.\n\nHệ thống **EduLearn LMS** được phân tách thành 5 service lõi:\n- **Identity Service (8081)**: Quản lý người dùng và xác thực JWT Rotation.\n- **Course Service (8082)**: Quản lý khóa học, đề cương MongoDB và tiến độ học tập.\n- **Assignment Service (8083)**: Thi trắc nghiệm và chấm code tự động Judge0.\n- **Blog Service (8084)**: Diễn đàn chia sẻ kiến thức.\n- **Chatbot Service (8085)**: Trợ lý học tập thông minh.\n\n## 2. Token Offloading tại API Gateway\n\nThay vì mỗi service phải tự giải mã và kiểm tra chữ ký token JWT, **Spring Cloud Gateway (8080)** đảm nhận vai trò thẩm định tập trung, sau đó gắn các thông tin danh tính vào header nội bộ (*X-User-Id*, *X-User-Role*). Mô hình này giúp giảm đáng kể thời gian xử lý và tải CPU cho các service hạ nguồn.', 'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=800', '33333333-3333-3333-3333-333333333333', 'PUBLISHED', 450, NOW(), NOW()),

(2, 'Tích hợp Judge0 Sandbox chấm code tự động an toàn trong Docker', 'tich-hop-judge0-sandbox-cham-code-tu-dong-trong-docker', 'Chi tiết giải pháp xây dựng hệ thống luyện code trực tuyến đa ngôn ngữ, phân loại lỗi CE, WA, TLE và AC.', '## 1. Thách thức bảo mật khi chấm code sinh viên\n\nCho phép người dùng thực thi mã nguồn tùy ý (C++, Java, Python) trên server là rủi ro bảo mật cực kỳ nguy hiểm. Để cô lập hoàn toàn môi trường thực thi, hệ thống sử dụng **Judge0 API Engine** chạy trong container Docker cách ly tài nguyên (CPU cgroup và giới hạn RAM 256MB).\n\n## 2. Quy trình phân loại lỗi thông minh\n\nĐể nâng cao trải nghiệm của người học và giảm tải cho sandbox, hệ thống thực hiện kiểm tra 2 lớp:\n1. **Kiểm tra cú pháp cục bộ**: Phân tích đóng/mở ngoặc `{}` và `()` trước khi nộp để phản hồi ngay lỗi *Compile Error (CE)*.\n2. **So khớp testcase**: Chấm điểm từng testcase mẫu, trả về *Wrong Answer (WA)* kèm thông tin sai lệch hoặc *Accepted (AC)* khi đạt 100%.', 'https://images.unsplash.com/photo-1515879218367-8466d910aaa4?w=800', '55555555-5555-5555-5555-555555555555', 'PUBLISHED', 620, NOW(), NOW()),

(3, 'Bảo mật Spring Security 6 với Refresh Token Rotation và SHA-256', 'bao-mat-spring-security-6-refresh-token-rotation', 'Giải pháp chống tấn công Replay Attack và quản lý phiên đăng nhập an toàn theo chuẩn RFC 6749.', '## 1. Cơ chế Refresh Token Rotation\n\nMỗi khi client yêu cầu cấp Access Token mới qua endpoint `/api/auth/refresh`, hệ thống sẽ lập tức thu hồi Refresh Token hiện tại và phát hành một cặp token hoàn toàn mới.\n\nNếu một token đã bị thu hồi mà vẫn tiếp tục được gửi lên, hệ thống sẽ phát hiện dấu hiệu xâm phạm an ninh và vô hiệu hóa toàn bộ phiên làm việc của tài khoản đó.\n\n## 2. Lưu trữ an toàn bằng hàm băm SHA-256\n\nRefresh Token không được lưu dưới dạng văn bản thô (plaintext) trong database mà được băm bằng thuật toán SHA-256 trước khi lưu vào cột `token_hash`.', 'https://images.unsplash.com/photo-1563986768609-322da13575f3?w=800', '33333333-3333-3333-3333-333333333333', 'PUBLISHED', 380, NOW(), NOW()),

(4, 'Tối ưu hóa truy vấn N+1 trong Spring Data JPA & Polyglot Persistence', 'toi-uu-hoa-truy-van-n-plus-1-spring-data-jpa', 'Cách khắc phục sụt giảm hiệu năng khi kết hợp cơ sở dữ liệu quan hệ MySQL và NoSQL MongoDB.', '## 1. Vấn đề N+1 truy vấn trong hiển thị tiến độ học tập\n\nKhi học viên truy cập trang tổng quan khóa học gồm nhiều chương và bài học, việc lặp qua từng bài để đếm số lượng hoàn thành dẫn đến việc phát sinh hàng chục câu lệnh SQL lặp đi lặp lại.\n\n## 2. Giải pháp tối ưu hóa\n\nNhóm đã tái cấu trúc tầng Repository bằng câu truy vấn `SELECT p.lessonId FROM LessonProgress p WHERE p.userId = :userId AND p.lessonId IN :lessonIds`, chỉ mất đúng 1 truy vấn SQL duy nhất và xử lý tổng hợp trên bộ nhớ RAM, giảm thời gian phản hồi từ 1,200ms xuống dưới 45ms.', 'https://images.unsplash.com/photo-1544383835-bda2bc66a55d?w=800', '33333333-3333-3333-3333-333333333333', 'PUBLISHED', 290, NOW(), NOW()),

(5, 'Ứng dụng Generative AI và SSE Streaming hỗ trợ học tập trực tuyến', 'ung-dung-generative-ai-va-sse-streaming-ho-tro-hoc-tap', 'Trải nghiệm xây dựng Chatbot AI hỗ trợ sinh viên tóm tắt bài giảng và tự động sinh đề thi trắc nghiệm.', '## 1. Server-Sent Events (SSE) trong giao tiếp AI\n\nThay vì người học phải chờ mô hình ngôn ngữ lớn (LLM) hoàn tất toàn bộ câu trả lời, hệ thống sử dụng giao thức **Server-Sent Events (SSE)** để truyền tải từng từ (stream token) theo thời gian thực.\n\n## 2. Tự động sinh câu hỏi trắc nghiệm\n\nGiảng viên chỉ cần cung cấp nội dung bài giảng, trợ lý AI sẽ tự động phân tích và tạo ra bộ câu hỏi trắc nghiệm kèm 4 đáp án và lời giải thích chi tiết, tiết kiệm 80% thời gian biên soạn đề.', 'https://images.unsplash.com/photo-1677442136019-21780ecad995?w=800', '55555555-5555-5555-5555-555555555555', 'PUBLISHED', 510, NOW(), NOW()),

(6, 'Lưu trữ đối tượng phân tán với MinIO S3 Pre-signed URL', 'luu-tru-doi-tuong-phan-tan-minio-s3-presigned-url', 'Thiết kế hệ thống lưu trữ video bài giảng và slide PDF quy mô lớn không nghẽn băng thông backend.', '## 1. Tại sao không nên lưu file trên ổ cứng máy chủ?\n\nKhi triển khai nhiều instance của microservice, việc lưu trữ tệp cục bộ sẽ gây ra tình trạng bất đồng bộ tệp và cạn kiệt dung lượng ổ đĩa. Sử dụng kho lưu trữ đối tượng tương thích Amazon S3 như **MinIO** là giải pháp chuẩn công nghiệp.\n\n## 2. Cơ chế Pre-signed URL an toàn\n\nHọc viên tải tài liệu trực tiếp từ máy chủ MinIO thông qua đường dẫn có chữ ký số (Pre-signed URL) với thời gian hết hạn an toàn trong vòng 2 giờ.', 'https://images.unsplash.com/photo-1507238691740-187a5b1d37b8?w=800', '33333333-3333-3333-3333-333333333333', 'PUBLISHED', 315, NOW(), NOW()),

(7, 'Hướng dẫn lập trình Web Server-Side Rendering với Spring Boot 3 & Thymeleaf', 'huong-dan-lap-trinh-web-ssr-spring-boot-3-thymeleaf', 'Kinh nghiệm triển khai cổng thông tin học tập Responsive, Form Validation và bảo mật CSRF.', '## 1. Ưu điểm của Server-Side Rendering (SSR)\n\nThymeleaf giúp kết xuất trực tiếp giao diện HTML tại máy chủ, tối ưu SEO, tải trang nhanh và đơn giản hóa việc quản lý trạng thái bảo mật của phiên người dùng thông qua HttpOnly Cookie.\n\n## 2. Tích hợp CSRF và Bean Validation\n\nToàn bộ biểu mẫu trên hệ thống đều được bảo vệ bởi thẻ ẩn CSRF Token của Spring Security và kiểm tra dữ liệu đầu vào bằng Bean Validation (@NotBlank, @Size) với thông báo lỗi trực quan cho người dùng.', 'https://images.unsplash.com/photo-1498050108023-c5249f4df085?w=800', '44444444-4444-4444-4444-444444444444', 'PUBLISHED', 410, NOW(), NOW());

INSERT INTO `post_tags` (`post_id`, `tag_id`) VALUES
(1, 1), (1, 2), (1, 4),
(2, 4), (2, 5), (2, 3),
(3, 1), (3, 7),
(4, 1), (4, 6),
(5, 8),
(6, 3), (6, 6),
(7, 1), (7, 7);

-- Bình luận trao đổi
INSERT INTO `comments` (`id`, `post_id`, `user_id`, `content`, `created_at`, `updated_at`) VALUES
(1, 1, '22222222-2222-2222-2222-222222222222', 'Bài viết rất chi tiết và dễ hiểu! Thầy cho em hỏi thêm về cách cấu hình Resilience4j fallback khi service bị timeout ạ?', NOW(), NOW()),
(2, 1, '33333333-3333-3333-3333-333333333333', 'Chào Nam! Em có thể cấu hình thuộc tính slidingWindowType: COUNT_BASED và khai báo fallbackMethod trong controller của Gateway nhé.', NOW(), NOW()),
(3, 2, '22222222-2222-2222-2222-222222222223', 'Phần chấm code Judge0 chạy rất mượt, mình đã nộp thử bài Two Sum và được Accepted ngay lần đầu!', NOW(), NOW());

-- Bình chọn Upvote
INSERT INTO `votes` (`user_id`, `target_id`, `target_type`, `vote_type`, `created_at`) VALUES
('22222222-2222-2222-2222-222222222222', 1, 'POST', 'UPVOTE', NOW()),
('22222222-2222-2222-2222-222222222223', 1, 'POST', 'UPVOTE', NOW()),
('22222222-2222-2222-2222-222222222224', 1, 'POST', 'UPVOTE', NOW()),
('22222222-2222-2222-2222-222222222222', 2, 'POST', 'UPVOTE', NOW()),
('22222222-2222-2222-2222-222222222223', 2, 'POST', 'UPVOTE', NOW());


SET FOREIGN_KEY_CHECKS = 1;

-- ==============================================================================
-- HOÀN TẤT NẠP TOÀN BỘ 4 CƠ SỞ DỮ LIỆU MYSQL CHO EDULEARN LMS!
-- ==============================================================================
