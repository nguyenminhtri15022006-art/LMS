SET NAMES utf8mb4;
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
