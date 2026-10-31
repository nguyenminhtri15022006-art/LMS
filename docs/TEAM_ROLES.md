# Phân Công Trách Nhiệm Nhóm — EduLearn LMS (TEAM_ROLES.md)

Tài liệu này xác định chi tiết sự phân công công việc, ranh giới trách nhiệm và khối lượng nghiệp vụ giữa hai thành viên trong dự án **EduLearn LMS** (`LMS-main`).

---

## 1. Thành Viên 1 — Nguyễn Minh Trí (MSSV: 24110359)

**Vai trò**: Trưởng nhóm / Fullstack Developer

### 1.1 Khối Backend Phụ Trách
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
   - Scheduled Jobs dọn dẹp bài thi bỏ dở và xử lý submission bị kẹt trạng thái.
2. **`chatbot-service`**:
   - Tích hợp mô hình ngôn ngữ lớn (OpenCode / DeepSeek LLM) qua WebClient WebFlux streaming (Server-Sent Events).
   - Quản lý phiên trò chuyện (`ChatSession`) và lịch sử tin nhắn (`ChatMessage`) trên MongoDB.
   - Tích hợp Redis Cache và thuật toán Rate Limiting (giới hạn số lượt gọi AI theo vai trò).
   - Tính năng AI tự động sinh câu hỏi Quiz từ văn bản bài giảng.
   - Tính năng AI tự động sinh bộ Testcases biên cho bài tập lập trình.
   - Scheduled Job lưu trữ (archive) các phiên trò chuyện cũ.

### 1.2 Khối Frontend Phụ Trách
- **Giao diện Thi trắc nghiệm & Ngân hàng câu hỏi**:
  - Phía học viên: Danh sách bài quiz, Tổng quan trước khi thi (`QuizOverviewPage`), Giao diện làm bài đếm ngược thời gian (`ExerciseQuizPage`), Xem kết quả thi (`ExerciseQuizResultPage`), Xem lại lịch sử bài làm chi tiết (`QuizHistoryDetailPage`).
  - Phía giảng viên: Tạo bài quiz mới (`CreateQuizPage`), Chỉnh sửa bài quiz (`EditQuizPage`), Nhập câu hỏi từ file.
- **Giao diện Chấm code lập trình**:
  - Trình soạn thảo mã nguồn CodeMirror 6 đa ngôn ngữ (`ExerciseCodePage`), nộp bài và nhận phản hồi thời gian thực.
  - Xem kết quả bài nộp lập trình chi tiết từng testcase (`ExerciseCodeResultPage`).
  - Phía giảng viên: Tạo bài toán lập trình (`CreateCodingPage`), Quản lý testcase, Xem trước bộ testcase nhập từ zip (`PreviewImportPage`), Bảng điểm và kết quả sinh viên (`ExerciseResultsPage`).
- **Giao diện Bảng xếp hạng**: Trang Leaderboard thi đua lập trình (`LeaderboardPage`).
- **Giao diện Dashboard**:
  - Dashboard Học viên (`DashboardStudent`): Thống kê tiến độ học, điểm số, bài tập hoàn thành.
  - Dashboard Giảng viên (`HomePageInstructor`): Thống kê số lượng sinh viên, bài tập cần chấm, khóa học active.
- **Giao diện Trợ lý ảo AI**: Component `ChatBot.jsx` cửa sổ nổi góc màn hình, hỗ trợ hiển thị Markdown, danh sách lịch sử phiên chat và gửi câu hỏi.

### 1.3 Khối Kiểm Thử & Tài Liệu Phụ Trách
- Viết Unit Tests & Integration Tests cho `assignment-service`, `chatbot-service`.
- Viết Frontend Tests cho `useJudge0.js`, `quizScoring.js`, `codeChallengeFormatters.js`.
- Phụ trách tài liệu kiến trúc AI và engine chấm code: [docs/ARCHITECTURE.md](ARCHITECTURE.md), [docs/TESTING_PLAN.md](TESTING_PLAN.md).

---

## 2. Thành Viên 2 — Nông Văn Cường (24110176)

**Vai trò**: Thành viên cốt lõi / Fullstack Developer

### 2.1 Khối Backend Phụ Trách
1. **`identity-service`**:
   - Kiến trúc xác thực JWT, cơ chế Refresh Token Rotation.
   - Tích hợp Google OAuth 2.0 Single Sign-On (SSO).
   - Module gửi mã OTP xác thực qua Email (JavaMailSender).
   - Quản lý tài khoản người dùng, đổi mật khẩu, quên mật khẩu, cập nhật hồ sơ cá nhân.
   - Tích hợp MinIO lưu trữ ảnh đại diện (Avatar).
   - Quản trị người dùng dành cho Admin (khóa/mở tài khoản, cấp quyền Admin).
   - Các API nội bộ (`/internal/users/**`) phục vụ cho các microservices khác.
2. **`course-service`**:
   - Quản lý danh mục khóa học (Categories).
   - Quản lý khóa học (Courses) và trạng thái (Private/Public/Locked).
   - Quản lý cây cấu trúc chương mục và bài học lưu trữ trên MongoDB (`CourseStructure`, `Lesson`).
   - Tải lên thumbnail khóa học và tài liệu học tập qua MinIO Presigned URLs.
   - Quy trình ghi danh (Enrollment) và theo dõi tiến độ hoàn thành bài học (`LessonProgress`).
   - Lên lịch học trực tuyến (Schedule) và tích hợp Google Meet.
   - Thông báo khóa học (Announcements).
3. **`blog-service`**:
   - Quản lý bài viết công nghệ định dạng Markdown (Post CRUD).
   - Quản lý thẻ bài viết (Tags).
   - Hệ thống bình luận đa cấp (Nested Comments).
   - Hệ thống bình chọn tương tác (Upvote / Downvote).
   - Tải ảnh bìa bài viết lên MinIO.

### 2.2 Khối Frontend Phụ Trách
- **Giao diện Xác thực**: Trang đăng nhập, đăng ký, quên mật khẩu, màn hình chờ xác thực mạng xã hội (`SocialCallbackPage`).
- **Giao diện Khóa học**:
  - Phía học viên: Danh sách khóa học, Chi tiết khóa học, Khóa học của tôi, Trang học bài (`CourseHubPage`) xem video và tài liệu.
  - Phía giảng viên: Quản lý khóa học, Tạo mới khóa học, Chỉnh sửa khóa học, Trung tâm quản lý nội dung (`CourseManagementCenter`).
- **Giao diện Blog**: Trang diễn đàn cộng đồng (`BlogHubPage`), Chi tiết bài viết Markdown, Soạn thảo bài viết mới (`CreateBlogPostPage`), Quản lý bài viết của tôi.
- **Giao diện Cài đặt**: Trang hồ sơ cá nhân (`SettingsPage`), đổi mật khẩu, cập nhật avatar.
- **Giao diện Quản trị**: Bảng quản lý người dùng (`UserManagement`), Quản trị viên (`AdminManagement`), Quản lý khóa học toàn hệ thống (`CourseManagement`), Quản lý nội dung (`WebContentMgmt`).

### 2.3 Khối Kiểm Thử & Tài Liệu Phụ Trách
- Viết Unit Tests & Integration Tests cho `identity-service`, `course-service`, `blog-service`.
- Viết Frontend Tests cho các hooks, context xác thực và utilities liên quan.
- Chịu trách nhiệm biên soạn [docs/CODING_RULES.md](CODING_RULES.md), [docs/API_CONTRACT.md](API_CONTRACT.md), [docs/progress/member-2.md](progress/member-2.md).
- *(Lưu ý: Tài liệu DEPLOYMENT_GUIDE.md hiện chưa được khởi tạo trong kho lưu trữ, sẽ được biên soạn trong giai đoạn đóng gói triển khai)*.

---

## 3. Công Việc Phối Hợp Chung (Collaborative Tasks)

Cả hai thành viên cùng phối hợp thực hiện và rà soát:
1. **Kiến trúc nền tảng**: Thiết lập Parent POM Monorepo, `service-registry` (Eureka) và cấu hình định tuyến, Circuit Breaker của `api-gateway`.
2. **Hạ tầng Docker**: Xây dựng `docker-compose.yml`, `docker-compose.db.yml`, tối ưu hóa Dockerfile đa tầng (Multi-stage build) và cấu hình Nginx.
3. **Cơ sở dữ liệu**: Rà soát các tệp Flyway SQL migration (`V1`, `V2`), cấu trúc MongoDB Document và Redis Cache.
4. **Bảo mật & Phân quyền**: Đảm bảo đồng bộ thông tin JWT Claims và kiểm thử phân quyền Role-Based Access Control tại Gateway và Controller.
5. **Tích hợp hệ thống**: Ghép nối các luồng nghiệp vụ liên dịch vụ (Course <-> Assignment, Assignment <-> Chatbot AI), xử lý lỗi CORS và timeouts.
6. **Kiểm thử toàn diện**: Thực hiện kiểm thử đầu cuối (End-to-End Test) và Docker Smoke Test trước khi bàn giao.
7. **Báo cáo & Video Demo**: Soạn thảo slide báo cáo tổng kết và quay video kịch bản demo hệ thống.

---

## 4. Đánh Giá Độ Cân Bằng Khối Lượng Công Việc

Qua khảo sát chi tiết project mẫu:
- **Thành viên 2 (Nông Văn Cường (24110176))**: 3 microservices nghiệp vụ (`identity`, `course`, `blog`), 14 bảng MySQL, 2 MongoDB Collections, 12 màn hình giao diện. Khối lượng tập trung vào luồng xác thực bảo mật, quản lý cây phân cấp bài học và diễn đàn thảo luận.
- **Nguyễn Minh Trí**: 2 microservices nghiệp vụ nặng về thuật toán (`assignment`, `chatbot`), 11 bảng MySQL, 3 MongoDB Collections, 12 màn hình giao diện. Khối lượng tập trung vào bộ máy chấm code tự động (Judge0 Sandbox), tính năng thi trắc nghiệm chống gian lận và tích hợp Trí tuệ nhân tạo LLM.

**Kết luận**: Phân công đạt độ cân bằng cao cả về số dòng code (LOC), độ phức tạp nghiệp vụ và công nghệ áp dụng.
