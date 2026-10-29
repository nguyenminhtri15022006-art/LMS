-- ==============================================================================
-- LMS Database Initialization Script
-- Tự động thực thi khi MySQL container khởi chạy lần đầu thông qua entrypoint
-- ==============================================================================

-- 1. Tạo các Database theo cả 2 tiền tố/hậu tố để tương thích 100% các service
CREATE DATABASE IF NOT EXISTS `lms_identity` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `lms_course` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `lms_assignment` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `lms_blog` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `identity_lms` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `course_lms` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `assignment_lms` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `blog_lms` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. Tạo User ứng dụng chuyên dụng và cấp quyền
CREATE USER IF NOT EXISTS 'lms_user'@'%' IDENTIFIED BY 'lmspassword123';
GRANT ALL PRIVILEGES ON *.* TO 'lms_user'@'%';
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%';

FLUSH PRIVILEGES;
