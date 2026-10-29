SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ==============================================================================
-- LMS Seed Data Script - Đợt 12 Release v1.0.0
-- Dữ liệu mẫu khởi tạo cho Assignment Service (Quiz, Questions, Coding Problems, Testcases)
-- ==============================================================================

USE `lms_assignment`;

-- 1. Dữ liệu mẫu bài tập lập trình (Coding Problems)
INSERT INTO `problems` (`problem_id`, `title`, `slug`, `description`, `difficulty`, `time_limit`, `memory_limit`, `created_by`, `course_id`, `created_at`, `updated_at`)
VALUES
(1, 'Hai Số Có Tổng Bằng Target (Two Sum)', 'two-sum', 'Cho một mảng các số nguyên `nums` và một số nguyên `target`, hãy trả về chỉ số của hai số sao cho tổng của chúng bằng `target`.', 'EASY', 1000, 256, 'instructor-1', 'course-1', NOW(), NOW()),
(2, 'Đảo Ngược Chuỗi Ký Tự (Reverse String)', 'reverse-string', 'Viết hàm đảo ngược chuỗi các ký tự đầu vào mà không sử dụng mảng phụ.', 'EASY', 1000, 128, 'instructor-1', 'course-1', NOW(), NOW()),
(3, 'Dãy Con Tăng Dài Nhất (Longest Increasing Subsequence)', 'longest-increasing-subsequence', 'Tìm độ dài của dãy con tăng nghiêm ngặt dài nhất từ một dãy số nguyên cho trước.', 'MEDIUM', 2000, 256, 'instructor-1', 'course-1', NOW(), NOW())
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);

-- 2. Dữ liệu mẫu Testcases cho Problem 1 (Two Sum)
INSERT INTO `test_cases` (`test_id`, `problem_id`, `input`, `expected_output`, `score_weight`, `is_hidden`, `created_at`)
VALUES
(1, 1, '4\n2 7 11 15\n9', '0 1', 25.0, 0, NOW()),
(2, 1, '3\n3 2 4\n6', '1 2', 25.0, 0, NOW()),
(3, 1, '2\n3 3\n6', '0 1', 25.0, 1, NOW()),
(4, 1, '5\n1 5 3 7 9\n12', '1 3', 25.0, 1, NOW())
ON DUPLICATE KEY UPDATE `score_weight` = VALUES(`score_weight`);

-- 3. Dữ liệu mẫu Bài thi trắc nghiệm (Quizzes)
INSERT INTO `quizzes` (`quiz_id`, `title`, `description`, `course_id`, `time_limit`, `pass_score`, `total_score`, `max_attempts`, `status`, `created_by`, `created_at`, `updated_at`)
VALUES
(1, 'Kiểm Tra Kiến Thức Cơ Bản Java & Spring Boot', 'Đề kiểm tra trắc nghiệm 15 phút đánh giá kiến thức cơ bản về Java OOP, Spring IoC/DI và Microservices.', 'course-1', 15, 60, 100, 3, 'PUBLISHED', 'instructor-1', NOW(), NOW())
ON DUPLICATE KEY UPDATE `title` = VALUES(`title`);

-- 4. Dữ liệu mẫu Câu hỏi trắc nghiệm (Questions)
INSERT INTO `questions` (`question_id`, `content`, `type`, `score`, `course_id`, `created_by`, `created_at`, `updated_at`)
VALUES
(1, 'Trong ngôn ngữ Java, từ khóa nào sau đây dùng để kế thừa một lớp cha?', 'SINGLE', 25, 'course-1', 'instructor-1', NOW(), NOW()),
(2, 'Annotation nào trong Spring Boot đánh dấu một bean dịch vụ xử lý logic nghiệp vụ?', 'SINGLE', 25, 'course-1', 'instructor-1', NOW(), NOW()),
(3, 'Những thành phần nào sau đây thuộc hệ sinh thái Spring Cloud?', 'MULTIPLE', 25, 'course-1', 'instructor-1', NOW(), NOW()),
(4, 'Mô hình kiến trúc Microservices giao tiếp giữa các service thông qua giao thức nào phổ biến nhất?', 'SINGLE', 25, 'course-1', 'instructor-1', NOW(), NOW())
ON DUPLICATE KEY UPDATE `content` = VALUES(`content`);

-- 5. Đáp án câu hỏi (Answers)
INSERT INTO `answers` (`answer_id`, `question_id`, `content`, `is_correct`, `order_index`)
VALUES
(1, 1, 'implements', 0, 0),
(2, 1, 'extends', 1, 1),
(3, 1, 'inherits', 0, 2),
(4, 1, 'instanceof', 0, 3),

(5, 2, '@Repository', 0, 0),
(6, 2, '@Controller', 0, 1),
(7, 2, '@Service', 1, 2),
(8, 2, '@Configuration', 0, 3),

(9, 3, 'Eureka Service Registry', 1, 0),
(10, 3, 'Spring Cloud Gateway', 1, 1),
(11, 3, 'Hibernate ORM', 0, 2),
(12, 3, 'OpenFeign', 1, 3),

(13, 4, 'REST API qua HTTP/JSON', 1, 0),
(14, 4, 'Trực tiếp truy cập chung Database bảng users', 0, 1),
(15, 4, 'Chia sẻ file trên đĩa cứng cục bộ', 0, 2),
(16, 4, 'Chỉ dùng Bluetooth', 0, 3)
ON DUPLICATE KEY UPDATE `content` = VALUES(`content`);

-- 6. Gán câu hỏi vào Quiz (Quiz_Questions)
INSERT INTO `quiz_questions` (`quiz_id`, `question_id`, `order_index`, `override_score`)
VALUES
(1, 1, 1, 25),
(1, 2, 2, 25),
(1, 3, 3, 25),
(1, 4, 4, 25)
ON DUPLICATE KEY UPDATE `order_index` = VALUES(`order_index`);
