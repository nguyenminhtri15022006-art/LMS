SET NAMES utf8mb4;
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
