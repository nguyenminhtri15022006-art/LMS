# Phân Quyền Người Dùng — EduLearn LMS (USER_ROLES.md)

Hệ thống EduLearn LMS thiết kế kiến trúc phân quyền dựa trên vai trò (Role-Based Access Control - RBAC) nghiêm ngặt gồm 4 nhóm người dùng: **Khách (Guest)**, **Học viên (Student)**, **Giảng viên (Instructor)** và **Quản trị viên (Admin)**.

---

## 1. Ma Trận Quyền Hạn Tổng Quát (RBAC Matrix)

| Nhóm chức năng | Guest | Student | Instructor | Admin | Điều kiện & Ràng buộc bổ sung |
|---|:---:|:---:|:---:|:---:|---|
| **Xem Trang chủ & Giới thiệu** | ✅ | ✅ | ✅ | ✅ | Công khai toàn bộ |
| **Đăng ký tài khoản** | ✅ | ❌ | ❌ | ❌ | Chọn vai trò `STUDENT` hoặc `INSTRUCTOR` |
| **Đăng nhập hệ thống (Local & Google)** | ✅ | ❌ | ❌ | ❌ | Đã đăng nhập tự động chuyển về Dashboard |
| **Xem & Cập nhật Hồ sơ cá nhân** | ❌ | ✅ | ✅ | ✅ | Chỉ sửa hồ sơ của chính mình (`user_id = token.sub`) |
| **Đổi mật khẩu** | ❌ | ✅ | ✅ | ✅ | Phải nhập đúng mật khẩu hiện tại |
| **Xem danh sách & Chi tiết Khóa học** | ✅ | ✅ | ✅ | ✅ | Chỉ xem các khóa ở trạng thái `PUBLIC` |
| **Đăng ký (Enroll) khóa học** | ❌ | ✅ | ❌ | ❌ | Khóa học phải ở trạng thái `PUBLIC` và chưa từng ghi danh |
| **Học bài & Đánh dấu tiến độ** | ❌ | ✅ | ❌ | ❌ | Bắt buộc phải có bản ghi ghi danh (`enrollment.status = ACTIVE`) |
| **Tạo mới khóa học** | ❌ | ❌ | ✅ | ❌ | Giảng viên có quyền tạo khóa học |
| **Quản lý nội dung khóa học (Cây bài học)** | ❌ | ❌ | ✅ | ❌ | Chỉ giảng viên tạo khóa (`instructor_id = token.sub`) mới được sửa |
| **Chuyển đổi trạng thái khóa học** | ❌ | ❌ | ✅ | ✅ | Giảng viên: chuyển Private/Public; Admin: có thêm quyền Lock |
| **Quản lý Lịch học & Google Meet** | ❌ | ❌ | ✅ | ❌ | Chỉ giảng viên của khóa học |
| **Tạo Ngân hàng câu hỏi (Question Bank)** | ❌ | ❌ | ✅ | ✅ | Giảng viên tạo câu hỏi theo khóa học |
| **Nhập câu hỏi từ file CSV / TXT** | ❌ | ❌ | ✅ | ❌ | Định dạng file phải hợp lệ theo quy ước |
| **Tạo đề thi trắc nghiệm (Quiz)** | ❌ | ❌ | ✅ | ❌ | Giảng viên của khóa học tương ứng |
| **Làm bài thi trắc nghiệm (Quiz Attempt)** | ❌ | ✅ | ❌ | ❌ | Phải ghi danh vào khóa học và còn lượt thi (`max_attempts`) |
| **Xem kết quả & Lịch sử Quiz** | ❌ | ✅ | ✅ | ✅ | Học viên xem bài của mình; Giảng viên xem bài học sinh lớp mình |
| **Tạo bài tập lập trình (Coding Problem)** | ❌ | ❌ | ✅ | ✅ | Giảng viên & Admin |
| **Quản lý bộ Testcase lập trình** | ❌ | ❌ | ✅ | ✅ | Giảng viên chỉ sửa testcase của bài do mình tạo |
| **Viết code & Nộp bài chấm (Code Judge)**| ❌ | ✅ | ❌ | ❌ | Mã nguồn thực thi an toàn trong Sandbox Judge0 |
| **Xem Bảng xếp hạng (Leaderboard)** | ❌ | ✅ | ✅ | ✅ | Hiển thị thứ hạng giải bài của học viên |
| **Xem Bảng điểm tổng hợp (Gradebook)** | ❌ | ❌ | ✅ | ✅ | Giảng viên xem toàn bộ điểm số học viên khóa mình phụ trách |
| **Đọc bài viết Blog** | ✅ | ✅ | ✅ | ✅ | Chỉ xem bài ở trạng thái `PUBLISHED` |
| **Viết & Chỉnh sửa bài viết Blog** | ❌ | ✅ | ✅ | ❌ | Chỉ tác giả bài viết (`author_id = token.sub`) mới được sửa/xóa |
| **Bình luận & Upvote/Downvote bài viết** | ❌ | ✅ | ✅ | ❌ | Người dùng đã xác thực |
| **Kiểm duyệt & Ẩn bài viết Blog** | ❌ | ❌ | ❌ | ✅ | Admin có quyền chuyển trạng thái bài viết sang `HIDDEN` |
| **Trò chuyện với AI Chatbot** | ❌ | ✅ | ✅ | ❌ | Tuân thủ hạn ngạch Rate Limit (Student: 50, Instructor: 100) |
| **Sử dụng AI sinh Quiz & Testcases** | ❌ | ❌ | ✅ | ❌ | Dành riêng cho Giảng viên khi soạn đề |
| **Giám sát Sức khỏe Service (Health Board)** | ❌ | ❌ | ❌ | ✅ | Kiểm tra trạng thái Eureka & Actuator của 7 microservices |
| **Quản trị người dùng (Khóa/Mở/Cấp quyền)** | ❌ | ❌ | ❌ | ✅ | Quyền hạn tối cao của Admin |
| **Tạo thêm tài khoản Quản trị viên mới** | ❌ | ❌ | ❌ | ✅ | Chỉ Admin hiện tại mới được tạo thêm Admin khác |

---

## 2. Chi Tiết Phân Quyền Theo Từng Vai Trò

### 2.1 Vai Trò: KHÁCH (GUEST)
- **Trang được truy cập**:
  - `/`, `/about-us`, `/list-course`, `/list-course/detail-course/:courseId`
  - `/blog`, `/blog/:id`
  - `/login`, `/register`, `/forgot-password`, `/authenticate`
- **API được gọi**:
  - `POST /api/identity/auth/register`, `POST /api/identity/auth/login`
  - `POST /api/identity/auth/social-login/{provider}`
  - `POST /api/identity/auth/send-otp`, `POST /api/identity/auth/verify-otp`
  - `GET /api/course/categories`, `GET /api/course/courses`, `GET /api/course/courses/{id}/public`
  - `GET /api/blog/posts`, `GET /api/blog/posts/{slugOrId}`, `GET /api/blog/tags`
- **Dữ liệu được xem**: Danh sách khóa học mở, bài viết công khai, thẻ tag, danh mục.
- **Dữ liệu được tạo/sửa/xóa**: Không có quyền ghi vào CSDL (ngoại trừ tạo bản ghi User mới khi đăng ký).

### 2.2 Vai Trò: HỌC VIÊN (STUDENT)
- **Trang được truy cập**:
  - Toàn bộ các trang công khai.
  - `/dashboard`, `/my-courses`, `/my-courses/detail`, `/course-hub`
  - `/exercises`, `/exercises/code/:slug`, `/exercises/quizzes/:courseId`
  - `/exercises/challenge/:slugOrId`, `/exercises/quiz/:id/overview`
  - `/exercises/quiz/:id`, `/exercises/quiz/:id/result`, `/exercises/quiz/:id/history/:attemptId`
  - `/exercises/leaderboard/:slug`, `/settings`
  - `/blog/create`, `/blog/edit/:id`, `/blog/my-posts`
- **Dữ liệu được tạo**:
  - Bản ghi `enrollments` (ghi danh).
  - Bản ghi `lesson_progress` (hoàn thành bài học).
  - Bản ghi `quiz_attempts`, `quiz_answer_records` (làm bài quiz).
  - Bản ghi `submissions` (nộp mã nguồn bài tập lập trình).
  - Bản ghi `posts`, `comments`, `votes` (tương tác blog).
  - Bản ghi `chat_messages` (chat với AI).
- **Dữ liệu được sửa**: Hồ sơ cá nhân của mình, bài viết blog của mình (nếu đang ở trạng thái DRAFT), đáp án của attempt khi chưa nộp bài.
- **Dữ liệu bị cấm**: Không thể xem đáp án đúng của Quiz khi đang làm bài hoặc trước deadline; không thể xem các testcase ẩn của bài tập lập trình; không thể sửa xóa nội dung khóa học.

### 2.3 Vai Trò: GIẢNG VIÊN (INSTRUCTOR)
- **Trang được truy cập**:
  - `/instructor/home`, `/manage/courses`, `/manage/courses/create`, `/manage/courses/edit/:courseId`, `/manage/courses/center/:courseId`
  - `/instructor/exercises`, `/instructor/exercises/:courseId`, `/instructor/exercises/:courseId/create-quiz`, `/instructor/exercises/:courseId/manual-quiz`
  - `/instructor/exercises/:courseId/quiz/edit/:quizId`, `/instructor/exercises/:courseId/create-coding`
  - `/instructor/exercises/:courseId/code-judge/edit/:slugOrId`, `/instructor/exercises/:courseId/code-judge/preview`
  - `/instructor/exercises/:courseId/results`
  - Các trang Blog và Cài đặt hồ sơ cá nhân.
- **Dữ liệu được tạo**: Khóa học mới, chương mục, bài học video/tài liệu, câu hỏi ngân hàng, đề thi trắc nghiệm, bài tập lập trình, bộ testcase ẩn/hiện, lịch học Google Meet.
- **Dữ liệu được xem**: Toàn bộ điểm số, lịch sử thi và bài nộp code của tất cả học viên đã ghi danh vào các khóa học do mình phụ trách.
- **Dữ liệu được sửa**: Toàn bộ nội dung khóa học và bài tập do mình tạo.

### 2.4 Vai Trò: QUẢN TRỊ VIÊN (ADMIN)
- **Trang được truy cập**:
  - `/admin/home` (Bảng điều khiển giám sát sức khỏe Microservices)
  - `/admin/users` (Quản trị người dùng)
  - `/admin/admins` (Quản trị tài khoản quản trị)
  - `/admin/all-courses`, `/admin/all-courses/:courseId` (Quản lý toàn bộ khóa học)
  - `/admin/web-content` (Quản lý danh mục khóa học, nội dung hệ thống)
  - `/admin/judge` (Quản lý bài toán lập trình)
  - `/admin/blog`, `/admin/blog/:id` (Kiểm duyệt bài viết blog)
  - `/admin/settings`, `/admin/error`
- **Dữ liệu được tạo/sửa/xóa**:
  - Khóa tài khoản (`is_active = false`) hoặc mở khóa người dùng.
  - Tạo tài khoản Admin mới.
  - Khóa hoặc duyệt trạng thái mọi khóa học (`status = LOCKED / PUBLIC`).
  - Ẩn các bài viết blog vi phạm (`status = HIDDEN`).
  - Xem thông số Actuator Health, Metrics của toàn bộ hạ tầng backend.

---

## 3. Điều Kiện Phân Quyền Kỹ Thuật (Security Implementation)

1. **Phân quyền tại API Gateway**:
   - `AuthFilter` đọc trường `scope` trong token JWT.
   - Nếu endpoint yêu cầu vai trò Admin (`/api/identity/admin/**`, `/api/course/admin/**`), gateway kiểm tra `scope.contains("ADMIN")`. Nếu không thỏa mãn, trả về HTTP 403 Forbidden ngay tại Gateway.
2. **Phân quyền tại Microservice nghiệp vụ**:
   - Sử dụng chú thích Spring Security: `@PreAuthorize("hasRole('INSTRUCTOR')")` hoặc kiểm tra logic chủ sở hữu (Resource Ownership Check):
     ```java
     if (!course.getInstructorId().equals(currentUserId) && !currentUserRole.equals("ADMIN")) {
         throw new AppException(ErrorCode.UNAUTHORIZED);
     }
     ```
3. **Phân quyền tại Giao diện Frontend**:
   - Sử dụng component `<RoleRoute allowedRoles={[ROLES.INSTRUCTOR]} />` bao bọc route để ngăn người dùng không có thẩm quyền truy cập URL; tự động chuyển hướng về trang Fallback/Login.
