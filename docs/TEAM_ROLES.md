# Phân Công Trách Nhiệm Nhóm — EduLearn LMS (TEAM_ROLES.md)

Tài liệu này xác định chi tiết sự phân công công việc, ranh giới trách nhiệm và khối lượng nghiệp vụ giữa hai thành viên trong dự án **EduLearn LMS** (`Nhóm 19`).

---

## 1. Thành Viên 1 — Nguyễn Minh Trí (MSSV: 24110359)

**Vai trò**: Trưởng nhóm / Fullstack Developer  
**Trọng tâm phụ trách**: Kiến trúc nền tảng, Xác thực & Phân quyền, Khóa học (Course Service), Tiến độ học tập CSDL thật, Studio Giảng viên, Blog & Quản trị Hệ thống (Admin).

### 1.1 Khối Backend Phụ Trách
1. **`identity-service`**:
   - Kiến trúc xác thực JWT phân quyền (RBAC: `ADMIN`, `INSTRUCTOR`, `STUDENT`), cơ chế Refresh Token Rotation.
   - Tích hợp Google OAuth 2.0 Single Sign-On (SSO).
   - Module gửi mã OTP xác thực qua Email (JavaMailSender).
   - Quản lý tài khoản người dùng, đổi mật khẩu, quên mật khẩu, cập nhật hồ sơ cá nhân.
   - Tích hợp MinIO lưu trữ ảnh đại diện (Avatar).
   - Quản trị người dùng dành cho Admin (khóa/mở tài khoản, cấp quyền).
   - Các API nội bộ (`/internal/users/**`) phục vụ cho các microservices khác tra cứu.
2. **`course-service`**:
   - Quản lý danh mục khóa học (Categories).
   - Quản lý khóa học (Courses) và 3 trạng thái (`PRIVATE`, `PUBLIC`, `LOCKED`).
   - Quản lý cây cấu trúc chương mục và bài học lưu trữ trên MongoDB (`CourseStructure`, `Lesson`).
   - Tải lên thumbnail khóa học và tài liệu học tập qua MinIO Storage.
   - Quy trình ghi danh (Enrollment) và theo dõi tiến độ hoàn thành bài học realtime (`lesson_progress` trong MySQL).
   - Lên lịch học trực tuyến (Schedule) tích hợp Google Meet và Bảng tin thông báo khóa học (Announcements).
3. **`blog-service`**:
   - Quản lý bài viết công nghệ định dạng Markdown (Post CRUD).
   - Quản lý thẻ bài viết (Tags).
   - Hệ thống bình luận đa cấp (Nested Comments).
   - Hệ thống bình chọn tương tác (Upvote / Downvote).
   - Tải ảnh bìa bài viết lên MinIO.

### 1.2 Khối Frontend Phụ Trách
- **Giao diện Xác thực**: Trang đăng nhập, đăng ký, quên mật khẩu OTP, tiếp nhận callback Google OAuth.
- **Giao diện Khóa học**:
  - Phía học viên: Danh sách khóa học, Chi tiết khóa học, Khóa học của tôi (`/profile`), Banner thông báo & Lịch học Google Meet, Trang học bài (`lesson.html`) xem video và tích hoàn thành bài học realtime.
  - Phía giảng viên: Quản lý khóa học, Tạo mới khóa học, Studio quản lý đề cương bài giảng cây MongoDB, Tab Bảng tin thông báo & Lịch Google Meet.
- **Giao diện Blog**: Trang diễn đàn cộng đồng, Chi tiết bài viết Markdown, Soạn thảo bài viết mới, Bình luận và Upvote.
- **Giao diện Quản trị**: Admin Dashboard thống kê toàn hệ thống, quản lý người dùng, duyệt/khóa mở khóa học, quản lý danh mục và tags.

---

## 2. Thành Viên 2 — Nông Văn Cường (MSSV: 24110176)

**Vai trò**: Thành viên cốt lõi / Fullstack Developer  
**Trọng tâm phụ trách**: Đánh giá Năng lực, Chấm Code tự động (Judge0 Sandbox), Hệ thống thi trắc nghiệm (Quiz Engine), Trợ lý Trí tuệ Nhân tạo (Chatbot AI) & Bảng xếp hạng (Leaderboard).

### 2.1 Khối Backend Phụ Trách
1. **`assignment-service`**:
   - Ngân hàng câu hỏi trắc nghiệm (Question Bank) theo chủ đề và độ khó.
   - Parser nhập câu hỏi hàng loạt từ file CSV / TXT.
   - Quản lý bài kiểm tra trắc nghiệm (Quiz CRUD), cấu hình xáo trộn đề, thời gian làm bài, điểm đậu.
   - Vòng đời lượt thi trắc nghiệm (Quiz Attempt), cơ chế khóa chống spam (`in_progress_lock`), chấm điểm tự động.
   - Quản lý bảng điểm tổng hợp (Gradebook) và lịch sử thi chi tiết.
   - Quản lý bài toán lập trình (Coding Problems) và bộ testcase (ẩn/hiện, trọng số điểm, memory/time limit).
   - Tích hợp Judge0 API Engine để chấm mã nguồn đa ngôn ngữ (C++, Java, Python, Go, Rust, JS) trong Docker Sandbox.
   - Lưu trữ mã nguồn và kết quả testcase trên MongoDB (`submissions`).
   - Thống kê và sinh bảng xếp hạng (Leaderboard) theo thời gian thực.
2. **`chatbot-service`**:
   - Tích hợp mô hình ngôn ngữ lớn (OpenCode / DeepSeek LLM) qua WebClient WebFlux streaming (Server-Sent Events).
   - Quản lý phiên trò chuyện (`ChatSession`) và lịch sử tin nhắn (`ChatMessage`) trên MongoDB & Redis.
   - Tính năng AI tự động sinh câu hỏi Quiz từ văn bản bài giảng.
   - Tính năng AI tự động sinh bộ Testcases biên cho bài tập lập trình.
   - Rate Limiting giới hạn số lượt gọi AI theo vai trò người dùng.

### 2.2 Khối Frontend Phụ Trách
- **Giao diện Thi trắc nghiệm & Ngân hàng câu hỏi**:
  - Phía học viên: Danh sách bài quiz, màn hình làm bài đếm ngược thời gian, nộp bài tính điểm tự động, xem giải thích đáp án.
  - Phía giảng viên: Tạo bài quiz mới, chỉnh sửa quiz, nhập câu hỏi từ file.
- **Giao diện Chấm code lập trình**:
  - Trình soạn thảo mã nguồn CodeMirror 6 đa ngôn ngữ, nộp bài và nhận phản hồi thời gian thực từ Judge0 sandbox.
  - Xem kết quả bài nộp lập trình chi tiết từng testcase (AC, WA, TLE, MLE).
  - Phía giảng viên: Tạo bài toán lập trình, quản lý testcase, xem bảng điểm sinh viên.
- **Giao diện Bảng xếp hạng**: Trang Leaderboard vinh danh học viên có thành tích code cao nhất.
- **Giao diện Trợ lý ảo AI**: Widget ChatBot nổi ở góc màn hình hỗ trợ giải đáp thắc mắc học tập 24/7.

---

## 3. Công Việc Phối Hợp Chung (Collaborative Tasks)

Cả hai thành viên cùng phối hợp thực hiện và rà soát:
1. **Kiến trúc nền tảng**: Thiết lập Parent POM Monorepo, `service-registry` (Eureka) và cấu hình định tuyến, Circuit Breaker của `api-gateway`.
2. **Hạ tầng Docker**: Thiết lập và duy trì hoạt động các container MySQL, MongoDB, Redis, MinIO và MailHog.
3. **Kiểm thử E2E & Tích hợp**: Rà soát luồng nghiệp vụ liên microservices và hoàn thiện tài liệu báo cáo đồ án.
