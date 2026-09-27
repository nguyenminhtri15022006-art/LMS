# Phân Công Trách Nhiệm Nhóm — EduLearn LMS (TEAM_ROLES.md)

Tài liệu này xác định chi tiết sự phân công công việc, ranh giới trách nhiệm và khối lượng nghiệp vụ giữa hai thành viên trong dự án **EduLearn LMS** (`DoAnCK`).

---

## 1. Thành Viên 1 — Nguyễn Minh Trí (MSSV: 23110340)

**Vai trò**: Trưởng nhóm / Fullstack Developer (Chủ trì khối Nghiệp vụ Phức tạp & Công nghệ Cốt lõi)

### 1.1 Khối Backend Phụ Trách (~18,100 LOC)
1. **`assignment-service` (13,496 LOC - Module lớn nhất toàn dự án)**:
   - Hệ thống Ngân hàng câu hỏi (Question Bank) theo chủ đề, độ khó, điểm số.
   - Bộ phân tích (Parser) nhập câu hỏi hàng loạt từ file CSV / TXT.
   - Quản lý đề thi trắc nghiệm (Quiz CRUD), cấu hình xáo trộn câu hỏi, xáo trộn đáp án, thời gian làm bài, điểm đậu, số lần thi tối đa.
   - Vòng đời lượt thi trắc nghiệm (Quiz Attempt), cơ chế khóa ảo chống spam (`in_progress_lock` Virtual Generated Column), tính điểm tự động.
   - Quản lý bảng điểm tổng hợp (Gradebook) và lưu trữ lịch sử thi chi tiết.
   - Quản lý bài tập lập trình (Coding Problems) và bộ testcase (ẩn/hiện, trọng số điểm, memory/time limit).
   - Tích hợp **Judge0 API Engine** chấm mã nguồn đa ngôn ngữ (C++, Java, Python, Go, Rust, JS) trong Docker Sandbox cô lập an toàn.
   - Xử lý bất đồng bộ đa luồng (`ThreadPoolExecutor`: `judge-executor`) để chấm code không gây nghẽn Gateway.
   - Lưu trữ mã nguồn nộp bài và kết quả chi tiết từng testcase trên MongoDB (`submissions`).
   - Thống kê xếp hạng và sinh Bảng xếp hạng (Leaderboard) theo thời gian thực.
   - Scheduled Jobs dọn dẹp bài thi bỏ dở và xử lý submission bị kẹt trạng thái.
2. **`chatbot-service` (4,600 LOC - Module Trí tuệ Nhân tạo AI)**:
   - Tích hợp mô hình ngôn ngữ lớn (OpenCode / DeepSeek LLM) qua Spring WebClient phản hồi streaming.
   - Quản lý phiên trò chuyện (`ChatSession`) và lịch sử tin nhắn (`ChatMessage`) trên MongoDB.
   - Tích hợp Redis Cache và thuật toán Rate Limiting (giới hạn số lượt gọi AI theo vai trò).
   - Tính năng **AI tự động sinh câu hỏi Quiz** từ văn bản bài giảng.
   - Tính năng **AI tự động sinh bộ Testcases biên** cho bài tập lập trình.
   - Scheduled Job tự động lưu trữ (archive) các phiên trò chuyện cũ.

### 1.2 Khối Frontend Phụ Trách (~16,200 LOC)
- **Giao diện Thi trắc nghiệm & Ngân hàng câu hỏi**:
  - Phía học viên: Danh sách bài quiz, Tổng quan trước khi thi (`QuizOverviewPage`), Giao diện làm bài đếm ngược thời gian (`ExerciseQuizPage`), Xem kết quả thi (`ExerciseQuizResultPage`), Xem lại lịch sử bài làm chi tiết (`QuizHistoryDetailPage`).
  - Phía giảng viên: Tạo bài quiz mới (`CreateQuizPage`), Chỉnh sửa bài quiz (`EditQuizPage`), Nhập câu hỏi từ file.
- **Giao diện Chấm code lập trình**:
  - Trình soạn thảo mã nguồn **CodeMirror 6** đa ngôn ngữ (`ExerciseCodePage`), nộp bài và nhận phản hồi thời gian thực.
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
- Chịu trách nhiệm biên soạn [API_CONTRACT.md](API_CONTRACT.md), [TESTING_PLAN.md](TESTING_PLAN.md), [ARCHITECTURE.md](ARCHITECTURE.md).

---

## 2. Thành Viên 2 — (Cập nhật Họ tên & MSSV)

**Vai trò**: Thành viên cốt lõi / Fullstack Developer

### 2.1 Khối Backend Phụ Trách (~12,900 LOC)
1. **`identity-service` (4,011 LOC)**:
   - Kiến trúc xác thực JWT, cơ chế Refresh Token Rotation.
   - Tích hợp Google OAuth 2.0 Single Sign-On (SSO).
   - Module gửi mã OTP xác thực qua Email (JavaMailSender).
   - Quản lý tài khoản người dùng, đổi mật khẩu, quên mật khẩu, cập nhật hồ sơ cá nhân.
   - Tích hợp MinIO lưu trữ ảnh đại diện (Avatar).
   - Quản trị người dùng dành cho Admin (khóa/mở tài khoản, cấp quyền Admin).
   - Các API nội bộ (`/internal/users/**`) phục vụ cho các microservices khác.
2. **`course-service` (5,738 LOC)**:
   - Quản lý danh mục khóa học (Categories).
   - Quản lý khóa học (Courses) và trạng thái (Private/Public/Locked).
   - Quản lý cây cấu trúc chương mục và bài học lưu trữ trên MongoDB (`CourseStructure`, `Lesson`).
   - Tải lên thumbnail khóa học và tài liệu học tập qua MinIO Presigned URLs.
   - Quy trình ghi danh (Enrollment) và theo dõi tiến độ hoàn thành bài học (`LessonProgress`).
   - Lên lịch học trực tuyến (Schedule) và tích hợp Google Meet.
   - Thông báo khóa học (Announcements).
3. **`blog-service` (3,146 LOC)**:
   - Quản lý bài viết công nghệ định dạng Markdown (Post CRUD).
   - Quản lý thẻ bài viết (Tags).
   - Hệ thống bình luận đa cấp (Nested Comments).
   - Hệ thống bình chọn tương tác (Upvote / Downvote).
   - Tải ảnh bìa bài viết lên MinIO.

### 2.2 Khối Frontend Phụ Trách (~10,500 LOC)
- **Giao diện Xác thực**: Trang đăng nhập, đăng ký, quên mật khẩu, màn hình chờ xác thực mạng xã hội (`SocialCallbackPage`).
- **Giao diện Khóa học**:
  - Phía học viên: Danh sách khóa học, Chi tiết khóa học, Khóa học của tôi, Trang học bài (`CourseHubPage`) xem video và tài liệu.
  - Phía giảng viên: Quản lý khóa học, Tạo mới khóa học, Chỉnh sửa khóa học, Trung tâm quản lý nội dung (`CourseManagementCenter`).
- **Giao diện Blog**: Trang diễn đàn cộng đồng (`BlogHubPage`), Chi tiết bài viết Markdown, Soạn thảo bài viết mới (`CreateBlogPostPage`), Quản lý bài viết của tôi.
- **Giao diện Cài đặt**: Trang hồ sơ cá nhân (`SettingsPage`), đổi mật khẩu, cập nhật avatar.
- **Giao diện Quản trị**: Bảng quản lý người dùng (`UserManagement`), Quản trị viên (`AdminManagement`), Quản lý khóa học toàn hệ thống (`CourseManagement`), Quản lý nội dung (`WebContentMgmt`).

### 2.3 Khối Kiểm Thử & Tài Liệu Phụ Trách
- Viết Unit Tests & Integration Tests cho `identity-service`, `course-service`, `blog-service`.
- Viết Frontend Tests cho `AuthContext.test.jsx`, `useFileUpload.test.jsx`, `PrivateRoute.test.jsx`, `RoleRoute.test.jsx`.
- Chịu trách nhiệm biên soạn [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md), [CODING_RULES.md](CODING_RULES.md).

---

## 3. Công Việc Phối Hợp Chung (Collaborative Tasks)

Cả hai thành viên cùng phối hợp thực hiện và rà soát:
1. **Kiến trúc nền tảng**: Thiết lập `service-registry` (Eureka) và cấu hình định tuyến, Circuit Breaker của `api-gateway`.
2. **Hạ tầng Docker**: Xây dựng `docker-compose.yml`, `docker-compose.db.yml`, tối ưu hóa Dockerfile đa tầng (Multi-stage build) và cấu hình Nginx.
3. **Cơ sở dữ liệu**: Rà soát các tệp Flyway SQL migration (`V1`, `V2`), cấu trúc MongoDB Document và Redis Cache.
4. **Bảo mật & Phân quyền**: Đảm bảo đồng bộ thông tin JWT Claims và kiểm thử phân quyền Role-Based Access Control tại Gateway và Controller.
5. **Tích hợp hệ thống**: Ghép nối các luồng nghiệp vụ liên dịch vụ (Course <-> Assignment, Assignment <-> Chatbot AI), xử lý lỗi CORS và timeouts.
6. **Kiểm thử toàn diện**: Thực hiện kiểm thử đầu cuối (End-to-End Test) và Docker Smoke Test trước khi bàn giao.
7. **Báo cáo & Video Demo**: Soạn thảo slide báo cáo tổng kết và quay video kịch bản demo hệ thống.

---

## 4. Đánh Giá Phân Bổ Khối Lượng Công Việc

Qua khảo sát và đo đạc chi tiết từng dòng code thực tế (Lines of Code - LOC):
- **Thành viên 1 — Nguyễn Minh Trí (Trưởng nhóm)**: Chủ động nhận gánh vác phần việc **nặng nhất và khó nhất** của toàn bộ dự án, chiếm khoảng **~34,300 LOC (gần 60% toàn bộ hệ thống)**. Khối lượng tập trung vào 2 công nghệ sáng tạo 15% rubric môn học (Trình chấm code Judge0 Sandbox thực thi mã nguồn an toàn & Trợ lý ảo AI LLM DeepSeek sinh đề/testcase tự động), cùng với bài toán chống gian lận thi trắc nghiệm và trình soạn thảo CodeMirror 6.
- **Thành viên 2**: Phụ trách phần nghiệp vụ nền tảng người dùng, quản trị phân quyền RBAC, cây cấu trúc khóa học đa tầng và diễn đàn blog công nghệ, chiếm khoảng **~23,400 LOC (khoảng 40% hệ thống)**.

**Kết luận**: Phân công thể hiện rõ nét vai trò dẫn dắt (Leadership) của Trưởng nhóm Nguyễn Minh Trí khi trực tiếp gánh vác các bài toán kỹ thuật hóc búa nhất, đồng thời tạo điều kiện để Thành viên 2 tập trung hoàn thành trọn vẹn luồng nghiệp vụ quản lý khóa học và người dùng.
