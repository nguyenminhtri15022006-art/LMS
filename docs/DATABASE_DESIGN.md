# Thiết Kế Cơ Sở Dữ Liệu Chi Tiết — EduLearn LMS (DATABASE_DESIGN.md)

Tài liệu này đặc tả toàn bộ lược đồ cơ sở dữ liệu quan hệ (MySQL 8.4) và phi quan hệ (MongoDB 7.0) của hệ thống EduLearn LMS. Các bảng và collection được đối chiếu chính xác 100% từ mã nguồn và các file Flyway Migration của project mẫu, tuyệt đối không tự thêm bảng giả định.

---

## 1. Phân Hệ Xác Thực & Người Dùng (`identity_lms` - MySQL)

### 1.1 Bảng `users`
Lưu trữ thông tin tài khoản người dùng trên toàn hệ thống.
- **Khóa chính**: `user_id` (CHAR(36) - UUID v4)
- **Cột**:
  - `user_name`: VARCHAR(55), NOT NULL, UNIQUE
  - `email`: VARCHAR(255), NOT NULL, UNIQUE
  - `password_hash`: VARCHAR(255), NOT NULL (BCrypt hash)
  - `full_name`: VARCHAR(255), NOT NULL
  - `dob`: DATE, NULL
  - `avatar_url`: VARCHAR(500), NULL (URL MinIO)
  - `bio`: TEXT, NULL
  - `phone`: VARCHAR(20), NULL
  - `role`: ENUM('ADMIN', 'INSTRUCTOR', 'STUDENT'), NOT NULL
  - `role_selected`: TINYINT(1), NOT NULL DEFAULT 0
  - `is_active`: TINYINT(1), NOT NULL DEFAULT 1
  - `is_verified`: TINYINT(1), NOT NULL DEFAULT 0
  - `auth_provider`: ENUM('LOCAL', 'GOOGLE'), NOT NULL DEFAULT 'LOCAL'
  - `provider_id`: VARCHAR(100), NULL
  - `last_login_at`: DATETIME, NULL
  - `created_at`: DATETIME, NOT NULL DEFAULT CURRENT_TIMESTAMP
  - `updated_at`: DATETIME, NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
- **Indexes**:
  - `idx_users_active_verified`: (`is_active`, `is_verified`)
  - `idx_users_phone`: (`phone`)
  - `idx_users_role_created_at`: (`role`, `created_at`)

### 1.2 Bảng `refresh_tokens`
Quản lý phiên đăng nhập và vòng đời của Refresh Token.
- **Khóa chính**: `token_id` (CHAR(36))
- **Khóa ngoại**: `user_id` REFERENCES `users(user_id)` ON DELETE RESTRICT
- **Cột**:
  - `token_hash`: VARCHAR(64), NOT NULL, UNIQUE (SHA-256 hash của raw token)
  - `device_info`: VARCHAR(255), NULL
  - `ip_address`: VARCHAR(45), NULL
  - `expires_at`: DATETIME, NOT NULL
  - `is_revoked`: TINYINT(1), NOT NULL DEFAULT 0
  - `created_at`: DATETIME, NOT NULL DEFAULT CURRENT_TIMESTAMP
- **Indexes**:
  - `idx_refresh_tokens_user_revoked`: (`user_id`, `is_revoked`)
  - `idx_refresh_tokens_expires_at`: (`expires_at`)

### 1.3 Bảng `email_verifications`
Quản lý mã xác thực OTP gửi qua email.
- **Khóa chính**: `id` (BIGINT AUTO_INCREMENT)
- **Khóa ngoại**: `user_id` REFERENCES `users(user_id)` ON DELETE CASCADE
- **Cột**:
  - `token`: VARCHAR(255), NOT NULL, UNIQUE (mã OTP hoặc token xác thực)
  - `type`: ENUM('VERIFY_EMAIL', 'RESET_PASSWORD'), NOT NULL
  - `expires_at`: DATETIME, NOT NULL
  - `used_at`: DATETIME, NULL
  - `attempts`: TINYINT, NOT NULL DEFAULT 0 (Tối đa 5 lần)
  - `created_at`: DATETIME, NOT NULL DEFAULT CURRENT_TIMESTAMP
- **Indexes**:
  - `idx_email_verif_user_type_expires`: (`user_id`, `type`, `expires_at`)
  - `idx_email_verif_expires_at`: (`expires_at`)

---

## 2. Phân Hệ Quản Lý Khóa Học (`course_lms` - MySQL & MongoDB)

### 2.1 Bảng `categories` (MySQL)
- **Khóa chính**: `id` (BIGINT AUTO_INCREMENT)
- **Cột**: `name` (VARCHAR(200) UNIQUE), `slug` (VARCHAR(200) UNIQUE), `created_at` (DATETIME)

### 2.2 Bảng `courses` (MySQL)
- **Khóa chính**: `id` (VARCHAR(36))
- **Cột**:
  - `title`: VARCHAR(500), NOT NULL
  - `description`: TEXT, NULL
  - `instructor_id`: VARCHAR(36), NOT NULL (Tham chiếu logic sang `identity_service.users.user_id`)
  - `duration`: INT, NULL (tổng số phút)
  - `level`: ENUM('BEGINNER', 'INTERMEDIATE', 'ADVANCED'), NULL
  - `thumbnail_url`: VARCHAR(1000), NULL
  - `status`: ENUM('PRIVATE', 'PUBLIC', 'LOCKED'), NOT NULL DEFAULT 'PRIVATE'
  - `meeting_url`: VARCHAR(500), NULL (Google Meet link)
  - `google_event_id`: VARCHAR(200), NULL
  - `mongo_structure_id`: VARCHAR(36), NOT NULL, UNIQUE (Tham chiếu Document MongoDB)
  - `created_at`: DATETIME, NOT NULL
  - `updated_at`: DATETIME, NOT NULL
- **Indexes**:
  - `idx_courses_instructor`: (`instructor_id`)
  - `idx_courses_status_created`: (`status`, `created_at`)

### 2.3 Bảng `course_categories` (MySQL)
Bảng liên kết Nhiều-Nhiều (N-N) giữa Khóa học và Danh mục.
- **Khóa chính**: (`course_id`, `category_id`)
- **Khóa ngoại**:
  - `course_id` REFERENCES `courses(id)` ON DELETE CASCADE
  - `category_id` REFERENCES `categories(id)` ON DELETE CASCADE

### 2.4 Bảng `course_learning_points` (MySQL)
Danh sách các mục tiêu/điểm học tập đạt được trong khóa học.
- **Khóa chính**: (`course_id`, `point_order`)
- **Khóa ngoại**: `course_id` REFERENCES `courses(id)` ON DELETE CASCADE
- **Cột**: `point` (TEXT), `point_order` (INT)

### 2.5 Bảng `course_requirements` (MySQL)
Danh sách các yêu cầu tiên quyết khi tham gia khóa học.
- **Khóa chính**: (`course_id`, `requirement_order`)
- **Khóa ngoại**: `course_id` REFERENCES `courses(id)` ON DELETE CASCADE
- **Cột**: `requirement` (TEXT), `requirement_order` (INT)

### 2.6 Bảng `enrollments` (MySQL)
Ghi nhận học viên đăng ký tham gia khóa học.
- **Khóa chính**: `id` (BIGINT AUTO_INCREMENT)
- **Khóa ngoại**: `course_id` REFERENCES `courses(id)` ON DELETE CASCADE
- **Unique Constraint**: (`user_id`, `course_id`)
- **Cột**: `user_id` (VARCHAR(36)), `status` (ENUM('ACTIVE', 'CANCELLED') DEFAULT 'ACTIVE'), `enrolled_at` (DATETIME), `updated_at` (DATETIME)
- **Indexes**: `idx_enrollments_user`: (`user_id`)

### 2.7 Bảng `schedules` (MySQL)
Lịch học trực tuyến của khóa học.
- **Khóa chính**: `id` (BIGINT AUTO_INCREMENT)
- **Khóa ngoại**: `course_id` REFERENCES `courses(id)` ON DELETE CASCADE
- **Cột**: `title` (VARCHAR(500)), `start_time` (DATETIME), `end_time` (DATETIME), `note` (TEXT), `created_at` (DATETIME)

### 2.8 Bảng `announcements` (MySQL)
Thông báo của giảng viên trong khóa học.
- **Khóa chính**: `id` (BIGINT AUTO_INCREMENT)
- **Khóa ngoại**: `course_id` REFERENCES `courses(id)` ON DELETE CASCADE
- **Cột**: `title` (VARCHAR(500)), `content` (TEXT), `created_at` (DATETIME)

### 2.9 Bảng `lesson_progress` (MySQL)
Theo dõi trạng thái hoàn thành bài học của học viên.
- **Khóa chính**: `id` (BIGINT AUTO_INCREMENT)
- **Unique Constraint**: (`student_id`, `lesson_id`)
- **Cột**:
  - `student_id`: VARCHAR(36), NOT NULL
  - `course_id`: VARCHAR(36), NOT NULL
  - `lesson_id`: VARCHAR(36), NOT NULL (Tham chiếu `mongo.lessons.id`)
  - `lesson_type`: ENUM('VIDEO', 'DOCUMENT'), NOT NULL
  - `is_completed`: BOOLEAN DEFAULT FALSE
  - `last_accessed`: DATETIME, NULL
- **Indexes**: `idx_progress_student_course`: (`student_id`, `course_id`)

### 2.10 Collection `course_structures` (MongoDB)
Lưu trữ cây phân cấp chương mục và danh sách ID bài học.
- **Document Model**:
  ```json
  {
    "_id": "uuid-string",
    "course_id": "course-uuid",
    "chapters": [
      {
        "id": "chapter-uuid",
        "title": "Chương 1: Giới thiệu",
        "order_index": 1,
        "lesson_ids": ["lesson-uuid-1", "lesson-uuid-2"]
      }
    ],
    "created_at": "ISODate",
    "updated_at": "ISODate"
  }
  ```

### 2.11 Collection `lessons` (MongoDB)
Lưu trữ nội dung chi tiết bài học đa định dạng.
- **Document Model**:
  ```json
  {
    "_id": "lesson-uuid",
    "course_id": "course-uuid",
    "title": "Cài đặt môi trường Spring Boot",
    "type": "VIDEO", // hoặc "DOCUMENT"
    "duration_minutes": 15,
    "video_url": "https://minio.../video.mp4",
    "content_markdown": "# Hướng dẫn cài đặt...",
    "attachments": [
      { "name": "slides.pdf", "url": "https://..." }
    ],
    "created_at": "ISODate"
  }
  ```

---

## 3. Phân Hệ Bài Tập & Đánh Giá (`assignment_lms` - MySQL & MongoDB)

### 3.1 Bảng `quizzes` (MySQL)
Quản lý các bài thi trắc nghiệm.
- **Khóa chính**: `quiz_id` (INT AUTO_INCREMENT)
- **Cột**:
  - `course_id`: CHAR(36), NOT NULL
  - `lesson_id`: CHAR(36), NULL
  - `title`: VARCHAR(300), NOT NULL
  - `description`: TEXT, NULL
  - `duration`: INT, NULL (thời lượng phút)
  - `total_score`: SMALLINT, NOT NULL DEFAULT 100
  - `pass_score`: TINYINT, NOT NULL DEFAULT 50
  - `max_attempts`: TINYINT, NOT NULL DEFAULT 0 (0 = không giới hạn)
  - `shuffle_questions`: TINYINT(1), NOT NULL DEFAULT 1
  - `shuffle_answers`: TINYINT(1), NOT NULL DEFAULT 1
  - `show_result`: ENUM('IMMEDIATELY', 'AFTER_SUBMIT', 'AFTER_DEADLINE'), NOT NULL DEFAULT 'AFTER_SUBMIT'
  - `start_time`: DATETIME, NULL
  - `end_time`: DATETIME, NULL
  - `created_by`: CHAR(36), NOT NULL
  - `is_published`: TINYINT(1), NOT NULL DEFAULT 0
  - `is_deleted`: TINYINT(1), NOT NULL DEFAULT 0
  - `created_at`, `updated_at`: DATETIME

### 3.2 Bảng `questions` (MySQL)
Ngân hàng câu hỏi trắc nghiệm (Question Bank).
- **Khóa chính**: `question_id` (INT AUTO_INCREMENT)
- **Cột**:
  - `course_id`: CHAR(36), NOT NULL
  - `content`: TEXT, NOT NULL
  - `type`: ENUM('SINGLE', 'MULTIPLE', 'TRUE_FALSE'), NOT NULL
  - `topic`: VARCHAR(100), NULL
  - `explanation`: TEXT, NULL
  - `score`: TINYINT, NOT NULL DEFAULT 10
  - `image_url`: VARCHAR(500), NULL
  - `created_by`: CHAR(36), NOT NULL
  - `created_at`, `updated_at`: DATETIME

### 3.3 Bảng `answers` (MySQL)
Đáp án của từng câu hỏi.
- **Khóa chính**: `answer_id` (INT AUTO_INCREMENT)
- **Khóa ngoại**: `question_id` REFERENCES `questions(question_id)` ON DELETE CASCADE
- **Cột**: `content` (TEXT), `is_correct` (TINYINT(1) DEFAULT 0), `order_index` (SMALLINT DEFAULT 0)

### 3.4 Bảng `quiz_questions` (MySQL)
Bảng liên kết câu hỏi được gắn vào một bài thi Quiz cụ thể.
- **Khóa chính**: `id` (BIGINT AUTO_INCREMENT)
- **Khóa ngoại**:
  - `quiz_id` REFERENCES `quizzes(quiz_id)` ON DELETE CASCADE
  - `question_id` REFERENCES `questions(question_id)`
- **Unique Constraint**: (`quiz_id`, `question_id`)
- **Cột**: `override_score` (SMALLINT NULL), `order_index` (SMALLINT DEFAULT 0)

### 3.5 Bảng `quiz_attempts` (MySQL)
Ghi nhận từng lần làm bài thi của học viên.
- **Khóa chính**: `attempt_id` (BIGINT AUTO_INCREMENT)
- **Khóa ngoại**: `quiz_id` REFERENCES `quizzes(quiz_id)`
- **Cột**:
  - `user_id`: CHAR(36), NOT NULL
  - `score`: FLOAT, NOT NULL DEFAULT 0
  - `total_score`: DECIMAL(10,2), NOT NULL
  - `is_passed`: TINYINT(1), NOT NULL DEFAULT 0
  - `time_spent_s`: INT, NULL
  - `status`: ENUM('IN_PROGRESS', 'SUBMITTED', 'EXPIRED'), NOT NULL DEFAULT 'IN_PROGRESS'
  - `started_at`: DATETIME, NOT NULL
  - `submitted_at`: DATETIME, NULL
  - `expires_at`: DATETIME, NULL
  - `attempt_number`: TINYINT, NOT NULL DEFAULT 1
  - `in_progress_lock`: VARCHAR(100) GENERATED ALWAYS AS (IF(status = 'IN_PROGRESS', CONCAT(user_id, ':', quiz_id), NULL)) VIRTUAL
  - `version`: BIGINT, NOT NULL DEFAULT 0 (Optimistic Lock)
- **Indexes**:
  - Unique index trên `in_progress_lock` để chống spam mở nhiều attempt cùng lúc.
  - `idx_attempts_user_quiz`: (`user_id`, `quiz_id`, `started_at`)

### 3.6 Bảng `quiz_answer_records` (MySQL)
Ghi nhận đáp án thực tế học viên đã chọn trong một lần thi.
- **Khóa chính**: `id` (BIGINT AUTO_INCREMENT)
- **Khóa ngoại**:
  - `attempt_id` REFERENCES `quiz_attempts(attempt_id)` ON DELETE CASCADE
  - `question_id` REFERENCES `questions(question_id)`
- **Unique Constraint**: (`attempt_id`, `question_id`)
- **Cột**: `selected_answer_ids` (JSON, NOT NULL), `earned_score` (DECIMAL(10,2) DEFAULT 0)

### 3.7 Bảng `grades` (MySQL)
Lưu kết quả điểm số cao nhất của học viên theo từng bài Quiz.
- **Khóa chính**: `id` (BIGINT AUTO_INCREMENT)
- **Khóa ngoại**: `quiz_id` REFERENCES `quizzes(quiz_id)`
- **Unique Constraint**: (`user_id`, `quiz_id`)
- **Cột**: `user_id` (CHAR(36)), `course_id` (CHAR(36)), `best_score` (FLOAT), `attempts_count` (INT), `last_attempt_at` (DATETIME)

### 3.8 Bảng `problems` (MySQL)
Bài tập thực hành lập trình.
- **Khóa chính**: `problem_id` (INT AUTO_INCREMENT)
- **Cột**:
  - `course_id`: CHAR(36), NOT NULL
  - `lesson_id`: CHAR(36), NOT NULL
  - `title`: VARCHAR(300), NOT NULL
  - `slug`: VARCHAR(350), NOT NULL, UNIQUE
  - `description`: LONGTEXT, NOT NULL
  - `difficulty`: ENUM('EASY', 'MEDIUM', 'HARD'), NOT NULL
  - `time_limit_ms`: INT, NOT NULL DEFAULT 2000
  - `memory_limit_mb`: INT, NOT NULL DEFAULT 256
  - `allowed_langs`: JSON, NOT NULL (Ví dụ: `["CPP", "JAVA", "PYTHON"]`)
  - `score`: SMALLINT, NOT NULL DEFAULT 100
  - `is_public`: TINYINT(1), NOT NULL DEFAULT 0
  - `is_deleted`: TINYINT(1), NOT NULL DEFAULT 0
  - `total_submit`: INT, NOT NULL DEFAULT 0
  - `total_accepted`: INT, NOT NULL DEFAULT 0
  - `created_by`: CHAR(36), NOT NULL
  - `created_at`, `updated_at`: DATETIME

### 3.9 Bảng `test_cases` (MySQL)
Bộ dữ liệu kiểm thử của bài toán lập trình.
- **Khóa chính**: `test_id` (BIGINT AUTO_INCREMENT)
- **Khóa ngoại**: `problem_id` REFERENCES `problems(problem_id)` ON DELETE CASCADE
- **Cột**:
  - `input`: LONGTEXT, NOT NULL
  - `expected_output`: LONGTEXT, NOT NULL
  - `is_hidden`: TINYINT(1), NOT NULL DEFAULT 1
  - `order_index`: SMALLINT, NOT NULL DEFAULT 0
  - `score_weight`: FLOAT, NOT NULL DEFAULT 1.0
  - `created_at`: DATETIME, NOT NULL DEFAULT CURRENT_TIMESTAMP

### 3.10 Collection `submissions` (MongoDB)
Lưu trữ toàn bộ mã nguồn các lần nộp bài lập trình và kết quả chi tiết từng testcase.
- **Document Model**:
  ```json
  {
    "_id": "submission-object-id",
    "user_id": "user-uuid",
    "problem_id": 101,
    "language": "CPP",
    "source_code": "#include <iostream>...",
    "status": "ACCEPTED", // PENDING, ACCEPTED, WRONG_ANSWER, TIME_LIMIT_EXCEEDED, COMPILATION_ERROR
    "passed_test_cases": 10,
    "total_test_cases": 10,
    "score": 100,
    "execution_time_ms": 45,
    "memory_used_kb": 2048,
    "compile_output": "",
    "test_case_results": [
      {
        "test_id": 1,
        "status": "ACCEPTED",
        "time_ms": 12,
        "memory_kb": 1024,
        "is_hidden": false
      }
    ],
    "submitted_at": "ISODate"
  }
  ```
- **Indexes**:
  - `idx_user_problem_submitted`: (`user_id`: 1, `problem_id`: 1, `submitted_at`: -1)
  - `idx_problem_submitted`: (`problem_id`: 1, `submitted_at`: -1)
  - `idx_status_submitted`: (`status`: 1, `submitted_at`: 1)

---

## 4. Phân Hệ Diễn Đàn & Blog (`blog_lms` - MySQL)

### 4.1 Bảng `tags`
Thẻ phân loại bài viết.
- **Khóa chính**: `id` (BIGINT AUTO_INCREMENT)
- **Cột**: `name` (VARCHAR(100) UNIQUE), `slug` (VARCHAR(100) UNIQUE), `created_at`, `updated_at`

### 4.2 Bảng `posts`
Bài viết công nghệ trên diễn đàn.
- **Khóa chính**: `id` (BIGINT AUTO_INCREMENT)
- **Cột**:
  - `title`: VARCHAR(500), NOT NULL
  - `slug`: VARCHAR(500), NOT NULL, UNIQUE
  - `summary`: VARCHAR(1000), NULL
  - `content`: LONGTEXT, NOT NULL (Markdown)
  - `thumbnail`: VARCHAR(1000), NULL
  - `author_id`: VARCHAR(36), NOT NULL
  - `status`: ENUM('DRAFT', 'PUBLISHED', 'HIDDEN'), NOT NULL DEFAULT 'DRAFT'
  - `view_count`: BIGINT, NOT NULL DEFAULT 0
  - `created_at`, `updated_at`: DATETIME
- **Indexes**: `idx_posts_author_status`: (`author_id`, `status`), `idx_posts_status_created`: (`status`, `created_at`)

### 4.3 Bảng `post_tags`
Liên kết N-N giữa Bài viết và Thẻ Tags.
- **Khóa chính**: (`post_id`, `tag_id`)
- **Khóa ngoại**:
  - `post_id` REFERENCES `posts(id)` ON DELETE CASCADE
  - `tag_id` REFERENCES `tags(id)` ON DELETE CASCADE

### 4.4 Bảng `comments`
Bình luận phân cấp dưới bài viết.
- **Khóa chính**: `id` (BIGINT AUTO_INCREMENT)
- **Khóa ngoại**:
  - `post_id` REFERENCES `posts(id)` ON DELETE CASCADE
  - `parent_id` REFERENCES `comments(id)` ON DELETE CASCADE
- **Cột**: `user_id` (VARCHAR(36)), `content` (TEXT), `created_at`, `updated_at`

### 4.5 Bảng `votes`
Bình chọn Upvote / Downvote cho bài viết và bình luận.
- **Khóa chính**: `id` (BIGINT AUTO_INCREMENT)
- **Unique Constraint**: (`user_id`, `target_id`, `target_type`)
- **Cột**: `user_id` (VARCHAR(36)), `target_id` (BIGINT), `target_type` (ENUM('POST', 'COMMENT')), `vote_type` (ENUM('UPVOTE', 'DOWNVOTE')), `created_at` (DATETIME)

---

## 5. Phân Hệ Trợ Lý Ảo & AI (`chatbot_db` - MongoDB)

### 5.1 Collection `chat_sessions`
Lưu trữ thông tin phiên trò chuyện.
- **Document Model**:
  ```json
  {
    "_id": "session-id-string",
    "user_id": "user-uuid",
    "title": "Giải thích thuật toán Dijkstra",
    "context_type": "GENERAL", // COURSE, LESSON, PROBLEM, GENERAL
    "context_ref_id": "course-uuid-or-problem-id",
    "status": "ACTIVE", // ACTIVE, ARCHIVED
    "last_message_at": "ISODate",
    "created_at": "ISODate"
  }
  ```
- **Compound Indexes**:
  - `idx_active_session_lookup`: `{ "user_id": 1, "context_type": 1, "context_ref_id": 1, "status": 1 }`
  - `idx_user_last_message`: `{ "user_id": 1, "last_message_at": -1 }`
  - `idx_status_last`: `{ "status": 1, "last_message_at": 1 }`

### 5.2 Collection `chat_messages`
Lưu trữ từng lượt tin nhắn thoại giữa người dùng và AI Assistant.
- **Document Model**:
  ```json
  {
    "_id": "message-id-string",
    "session_id": "session-id-string",
    "user_id": "user-uuid",
    "role": "USER", // USER hoặc MODEL
    "content": "Làm thế nào để khởi tạo project Spring Boot?",
    "token_count": 28,
    "created_at": "ISODate"
  }
  ```
- **Compound Index**:
  - `idx_session_created`: `{ "session_id": 1, "created_at": -1 }`
