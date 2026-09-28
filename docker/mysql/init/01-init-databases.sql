-- ==============================================================================
-- LMS Database Initialization Script
-- Tự động thực thi khi MySQL container khởi chạy lần đầu thông qua entrypoint
-- ==============================================================================

CREATE DATABASE IF NOT EXISTS `identity_lms`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `course_lms`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- Database assignment_lms được chuẩn bị sẵn cho assignment-service (Thành viên 1)
CREATE DATABASE IF NOT EXISTS `assignment_lms`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS `blog_lms`
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
