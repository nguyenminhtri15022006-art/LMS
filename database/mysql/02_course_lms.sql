SET NAMES utf8mb4;
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
INSERT INTO `courses` (`id`, `title`, `description`, `instructor_id`, `duration`, `level`, `thumbnail_url`, `status`, `meeting_url`, `mongo_structure_id`, `created_at`, `updated_at`) VALUES
('c1111111-1111-1111-1111-111111111111', 'Lập trình Web hiện đại với Spring Boot 3 & Thymeleaf', 'Khóa học thực chiến phát triển ứng dụng Web chuẩn doanh nghiệp, tích hợp Spring Security 6, JPA, Docker và Thymeleaf Server-Side Rendering.', '11111111-1111-1111-1111-111111111111', 36, 'BEGINNER', 'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800', 'PUBLIC', 'https://meet.google.com/abc-defg-hij', 'd1111111-1111-1111-1111-111111111111', NOW(), NOW()),
('c2222222-2222-2222-2222-222222222222', 'Kiến trúc Microservices với Spring Cloud, Docker & Resilience4j', 'Xây dựng hệ thống phân tán chịu lỗi cao, cơ chế Circuit Breaker, Service Discovery Eureka, API Gateway và Token Offloading.', '33333333-3333-3333-3333-333333333333', 48, 'INTERMEDIATE', 'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=800', 'PUBLIC', 'https://meet.google.com/xyz-uvwx-rst', 'd2222222-2222-2222-2222-222222222222', NOW(), NOW()),
('c3333333-3333-3333-3333-333333333333', 'Thực hành Cấu trúc dữ liệu & Thuật toán và Luyện Code Sandbox', 'Rèn luyện tư duy thuật toán, giải quyết các bài toán kinh điển từ LeetCode, chấm điểm tự động qua engine Judge0 Sandbox.', '55555555-5555-5555-5555-555555555555', 40, 'ADVANCED', 'https://images.unsplash.com/photo-1515879218367-8466d910aaa4?w=800', 'PUBLIC', 'https://meet.google.com/opq-rstu-vwx', 'd3333333-3333-3333-3333-333333333333', NOW(), NOW());

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
('22222222-2222-2222-2222-222222222222', 'c2222222-2222-2222-2222-222222222222', NOW()),
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
