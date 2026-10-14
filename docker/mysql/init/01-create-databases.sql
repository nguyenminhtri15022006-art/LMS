-- ==============================================================================
-- EduLearn LMS - Script khởi tạo tự động các CSDL Microservices
-- Tự động chạy khi MySQL container khởi động lần đầu thông qua /docker-entrypoint-initdb.d
-- ==============================================================================

-- 1. Tạo các Database độc lập cho từng Microservice
CREATE DATABASE IF NOT EXISTS `lms_identity` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `lms_course` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `lms_assignment` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS `lms_blog` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 2. Tạo User ứng dụng chuyên dụng và cấp quyền
CREATE USER IF NOT EXISTS 'lms_user'@'%' IDENTIFIED BY 'lmspassword123';
GRANT ALL PRIVILEGES ON `lms_identity`.* TO 'lms_user'@'%';
GRANT ALL PRIVILEGES ON `lms_course`.* TO 'lms_user'@'%';
GRANT ALL PRIVILEGES ON `lms_assignment`.* TO 'lms_user'@'%';
GRANT ALL PRIVILEGES ON `lms_blog`.* TO 'lms_user'@'%';

-- Đảm bảo quyền truy cập từ xa cho root trong môi trường dev
GRANT ALL PRIVILEGES ON *.* TO 'root'@'%';

FLUSH PRIVILEGES;
