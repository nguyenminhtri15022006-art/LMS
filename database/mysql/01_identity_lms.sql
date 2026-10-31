SET NAMES utf8mb4;
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
('00000000-0000-0000-0000-000000000000', 'admin', 'admin@lms.edu.vn', '$2a$10$MID31l2qtrnyKUi.kqctwepMx5h5L8L7laiUNqt125BF2WGLAr0qi', 'Quản Trị Viên Hệ Thống', 'ADMIN', 1, 1, 1, 'LOCAL', NOW(), NOW()),
('11111111-1111-1111-1111-111111111111', 'instructor_demo', 'instructor_demo@lms.edu.vn', '$2a$10$MID31l2qtrnyKUi.kqctwepMx5h5L8L7laiUNqt125BF2WGLAr0qi', 'TS. Nguyễn Văn Hướng', 'INSTRUCTOR', 1, 1, 1, 'LOCAL', NOW(), NOW()),
('33333333-3333-3333-3333-333333333333', 'nguyenminhtri', 'nguyenminhtri@lms.edu.vn', '$2a$10$MID31l2qtrnyKUi.kqctwepMx5h5L8L7laiUNqt125BF2WGLAr0qi', 'ThS. Nguyễn Minh Trí', 'INSTRUCTOR', 1, 1, 1, 'LOCAL', NOW(), NOW()),
('55555555-5555-5555-5555-555555555555', 'cuongnv', 'cuongnv@lms.edu.vn', '$2a$10$MID31l2qtrnyKUi.kqctwepMx5h5L8L7laiUNqt125BF2WGLAr0qi', 'Nông Văn Cường', 'INSTRUCTOR', 1, 1, 1, 'LOCAL', NOW(), NOW()),
('44444444-4444-4444-4444-444444444444', 'lethimailinh', 'mailinh@lms.edu.vn', '$2a$10$MID31l2qtrnyKUi.kqctwepMx5h5L8L7laiUNqt125BF2WGLAr0qi', 'TS. Lê Thị Mai Linh', 'INSTRUCTOR', 1, 1, 1, 'LOCAL', NOW(), NOW()),
('22222222-2222-2222-2222-222222222222', 'student_demo', 'student_demo@lms.edu.vn', '$2a$10$MID31l2qtrnyKUi.kqctwepMx5h5L8L7laiUNqt125BF2WGLAr0qi', 'Lê Hoàng Nam', 'STUDENT', 1, 1, 1, 'LOCAL', NOW(), NOW()),
('22222222-2222-2222-2222-222222222223', 'tranvantuan', 'tuan.tran@lms.edu.vn', '$2a$10$MID31l2qtrnyKUi.kqctwepMx5h5L8L7laiUNqt125BF2WGLAr0qi', 'Trần Văn Tuấn', 'STUDENT', 1, 1, 1, 'LOCAL', NOW(), NOW()),
('22222222-2222-2222-2222-222222222224', 'hoangthimai', 'mai.hoang@lms.edu.vn', '$2a$10$MID31l2qtrnyKUi.kqctwepMx5h5L8L7laiUNqt125BF2WGLAr0qi', 'Hoàng Thị Mai', 'STUDENT', 1, 1, 1, 'LOCAL', NOW(), NOW()),
('22222222-2222-2222-2222-222222222225', 'nguyenvanan', 'an.nguyen@lms.edu.vn', '$2a$10$MID31l2qtrnyKUi.kqctwepMx5h5L8L7laiUNqt125BF2WGLAr0qi', 'Nguyễn Văn An', 'STUDENT', 1, 1, 1, 'LOCAL', NOW(), NOW());
