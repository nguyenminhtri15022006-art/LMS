# Đặc Tả Giao Diện Lập Trình — EduLearn LMS (API_CONTRACT.md)

Tài liệu này quy định chi tiết toàn bộ các Endpoint API của hệ sinh thái EduLearn LMS.  
Mọi lời gọi từ Frontend bắt buộc phải đi qua **API Gateway** (`http://localhost:8080`) với tiền tố định tuyến `/api/<service>`.

---

## Cấu Trúc Phản Hồi Chuẩn (Standard Response Format)

Mọi API thành công hay thất bại đều trả về cấu trúc đối tượng `ApiResponse<T>` thống nhất:

```json
{
  "code": 1000,
  "message": "Success",
  "data": { ... }
}
```

Mã phản hồi lỗi:
- `1000`: Thành công
- `1001`: Không xác thực / Token không hợp lệ (`UNAUTHENTICATED`)
- `1002`: Không có quyền truy cập (`UNAUTHORIZED`)
- `1003`: Dữ liệu đầu vào không hợp lệ (`INVALID_KEY` / `VALIDATION_FAILED`)
- `1004`: Bản ghi không tồn tại (`USER_NOT_EXISTED`, `COURSE_NOT_FOUND`, etc.)
- `1005`: Bản ghi đã tồn tại (`USER_EXISTED`, `SLUG_EXISTED`, etc.)
- `9999`: Lỗi máy chủ nội bộ (`UNCATEGORIZED_EXCEPTION`)

---

## 1. Phân Hệ Xác Thực & Người Dùng (`identity-service` — 24 Endpoints Thông Thường + 4 Endpoints Nội Bộ = 28 Endpoints)

### 1.1 Quản Lý Xác Thực (`AuthenticationController` — `/auth`)
- `POST /api/identity/auth/register` (Public)
  - In: `UserCreationRequest` (`username`, `password`, `email`, `name`, `role`)
  - Out: `ApiResponse<UserCreationResponse>`
- `POST /api/identity/auth/login` (Public)
  - In: `AuthenticationRequest` (`identifier`, `password`)
  - Out: `ApiResponse<AuthenticationResponse>` (accessToken, refreshToken, authenticated)
- `POST /api/identity/auth/social-login/{provider}` (Public)
  - In: `SocialLoginRequest` (`authCode`), Path: `provider` (ví dụ `google`)
  - Out: `ApiResponse<AuthenticationResponse>`
- `POST /api/identity/auth/introspect` (Public)
  - In: `IntrospectRequest` (`token`)
  - Out: `ApiResponse<IntrospectResponse>` (`valid`)
- `POST /api/identity/auth/refresh` (Public)
  - In: `RefreshTokenRequest` (`refreshToken`)
  - Out: `ApiResponse<AuthenticationResponse>`
- `POST /api/identity/auth/logout` (Public)
  - In: `LogoutRequest` (`refreshToken`)
  - Out: `ApiResponse<Void>`

### 1.2 Xác Thực Email & Quên/Đặt Lại Mật Khẩu (`EmailVerificationController` — `/auth`)
- `POST /api/identity/auth/verify-email/send` (Public)
  - In: `SendEmailVerificationRequest` (`email`)
  - Out: `ApiResponse<Void>`
- `POST /api/identity/auth/verify-email` (Public)
  - In: `VerifyEmailRequest` (`email`, `otp`)
  - Out: `ApiResponse<Void>`
- `POST /api/identity/auth/forgot-password` (Public)
  - In: `ForgotPasswordRequest` (`email`)
  - Out: `ApiResponse<Void>` (Gửi OTP 6 số qua email nếu tài khoản tồn tại)
- `POST /api/identity/auth/reset-password` (Public)
  - In: `ResetPasswordRequest` (`email`, `otp`, `newPassword`)
  - Out: `ApiResponse<Void>`

### 1.3 Hồ Sơ Cá Nhân Người Dùng (`UserController` — `/users`)
- `GET /api/identity/users/myinfo` (Authenticated — Student, Instructor, Admin)
  - Header: Injected `X-User-Id` bởi Gateway `AuthFilter`
  - Out: `ApiResponse<UserFullResponse>`
- `PUT /api/identity/users/myinfo` (Authenticated)
  - In: `UserUpdateRequest` (`name`, `phone`, `bio`, `avatar`, `dob`)
  - Out: `ApiResponse<UserFullResponse>`
- `PATCH /api/identity/users/myinfo/password` (Authenticated)
  - In: `UserUpdatePasswordRequest` (`oldPassword`, `newPassword`, `confirmPassword`)
  - Out: `ApiResponse<UserFullResponse>`
- `PATCH /api/identity/users/myinfo/role` (Authenticated)
  - In: `UserRoleUpdateRequest` (`role`)
  - Out: `ApiResponse<UserFullResponse>`
- `GET /api/identity/users/{userId}/profile` (Public)
  - Out: `ApiResponse<UserPublicProfileResponse>`

### 1.4 Quản Trị Người Dùng Admin (`AdminUserController` — `/admin/users`)
- `GET /api/identity/admin/users` (ADMIN)
  - Query: `pageable` (size 20), `role` (optional), `keyword` (optional)
  - Out: `ApiResponse<Page<UserSummaryResponse>>`
- `GET /api/identity/admin/users/stats` (ADMIN)
  - Out: `ApiResponse<UserDashboardResponse>` (thống kê tổng user, phân bố role, active)
- `GET /api/identity/admin/users/{id}` (ADMIN)
  - Out: `ApiResponse<UserFullResponse>`
- `PATCH /api/identity/admin/users/{id}/active` (ADMIN)
  - Out: `ApiResponse<UserFullResponse>` (Toggle khóa/mở khóa tài khoản)
- `POST /api/identity/admin/users/admins` (ADMIN)
  - In: `AdminCreateAdminRequest`
  - Out: `ApiResponse<UserFullResponse>`
- `PUT /api/identity/admin/users/{id}` (ADMIN)
  - In: `UserUpdateRequest`
  - Out: `ApiResponse<UserFullResponse>`
- `PATCH /api/identity/admin/users/{id}/password` (ADMIN)
  - In: `AdminResetPasswordRequest`
  - Out: `ApiResponse<UserFullResponse>`
- `PATCH /api/identity/admin/users/{id}/admin-active` (ADMIN)
  - Out: `ApiResponse<UserFullResponse>`

### 1.5 Lưu Trữ Avatar MinIO (`StorageController` — `/storage`)
- `POST /api/identity/storage/presigned-url` (Authenticated)
  - In: `PresignedUrlRequest` (`fileName`, `fileType`, `folder`)
  - Out: `ApiResponse<PresignedUrlResponse>` (`uploadUrl`, `formFields`, `publicUrl`)
  - **Đặc tả luồng upload tương thích Frontend (`useFileUpload.js`)**:
    * **Cơ chế**: S3 Post Policy (Browser-based POST Upload qua `FormData`).
    * **`uploadUrl`**: URL gốc của Bucket MinIO (`${minioPublicUrl}/${bucketName}`, ví dụ: `http://localhost:9000/lms-identity-bucket`).
    * **`formFields`**: Map chứa chữ ký chính sách S3 PostPolicy (`key`, `policy`, `x-amz-algorithm`, `x-amz-credential`, `x-amz-date`, `x-amz-signature`).
    * **`publicUrl`**: URL CDN/MinIO công khai để truy cập ảnh đại diện sau khi upload (`${minioPublicUrl}/${bucketName}/${objectName}`).
    * **Yêu cầu gọi từ Client**:
      - Client gửi: HTTP `POST` đến `uploadUrl` (Bucket URL).
      - Header: `Content-Type: multipart/form-data; boundary=...` (trình duyệt tự sinh).
      - Body: `FormData` đính kèm các trường trong `formFields`, `Content-Type: <fileType>`, và `file: <Blob/File>`.
    * *(Lưu ý: Không dùng Presigned PUT URL cho Avatar Identity vì frontend `useFileUpload.js` gửi `POST FormData`, chỉ chấp nhận endpoint Bucket S3 PostPolicy).*

### 1.6 Giao Tiếp Nội Bộ (`InternalUserController` — `/internal`)
- `GET /internal/user/{id}` (Internal Feign) -> Out: `UserFullResponse`
- `POST /internal/users/batch` (Internal Feign) -> In: `Set<String> userIds`, Out: `List<UserFullResponse>`
- `GET /internal/users/{userId}/exists` (Internal Feign) -> Out: `boolean`
- `GET /internal/users/search/by-email` (Internal Feign) -> In: `email`, Out: `UserFullResponse`

---

## 2. Phân Hệ Quản Lý Khóa Học (`course-service`)

### 2.1 Danh Mục Khóa Học (`CategoryController` — `/categories`)
- `GET /api/course/categories` (Public) -> Out: `ApiResponse<List<CategoryResponse>>`
- `POST /api/course/categories` (ADMIN) -> In: `CreateCategoryRequest`, Out: `ApiResponse<CategoryResponse>`
- `PUT /api/course/categories/{id}` (ADMIN) -> In: `UpdateCategoryRequest`, Out: `ApiResponse<CategoryResponse>`
- `DELETE /api/course/categories/{id}` (ADMIN) -> Out: `ApiResponse<CategoryResponse>`

### 2.2 Quản Lý Khóa Học & Ghi Danh (`CourseController` — `/courses`)
- `GET /api/course/courses` (Public)
  - Query: `pageable`, `status`, `categoryId`, `keyword`, `level`
  - Out: `ApiResponse<Page<CourseResponse>>`
- `GET /api/course/courses/{id}` (Public) -> Out: `ApiResponse<CourseResponse>`
- `POST /api/course/courses` (INSTRUCTOR, ADMIN)
  - In: `@ModelAttribute @Valid CreateCourseRequest` (multipart/form-data)
  - Out: `ApiResponse<CourseResponse>`
- `PUT /api/course/courses/{id}` (INSTRUCTOR, ADMIN)
  - In: `@ModelAttribute @Valid UpdateCourseRequest`
  - Out: `ApiResponse<CourseResponse>`
- `DELETE /api/course/courses/{id}` (INSTRUCTOR, ADMIN) -> Out: `ApiResponse<String>`
- `GET /api/course/courses/my` (INSTRUCTOR, ADMIN) -> Query: `pageable`, Out: `ApiResponse<Page<CourseResponse>>`
- `GET /api/course/courses/enrolled` (Authenticated) -> Out: `ApiResponse<List<CourseResponse>>`
- `POST /api/course/courses/{id}/enroll` (Authenticated) -> Out: `ApiResponse<EnrollmentResponse>`
- `POST /api/course/courses/{id}/invite` (INSTRUCTOR, ADMIN)
  - In: `InviteStudentRequest` (`email`), Out: `ApiResponse<InviteResultResponse>`
- `PATCH /api/course/courses/{id}/lock` (ADMIN)
  - In: `LockCourseRequest` (`reason`), Out: `ApiResponse<CourseResponse>`
- `PATCH /api/course/courses/{id}/unlock` (ADMIN) -> Out: `ApiResponse<CourseResponse>`
- `GET /api/course/courses/my/stats` (INSTRUCTOR, ADMIN) -> Out: `ApiResponse<InstructorStatsResponse>`
- `GET /api/course/courses/my/recent-enrollments` (INSTRUCTOR, ADMIN) -> Out: `ApiResponse<List<RecentEnrollmentResponse>>`
- `GET /api/course/courses/{id}/students/count` (Public) -> Out: `ApiResponse<Long>`
- `GET /api/course/courses/{id}/lessons/count` (Public) -> Out: `ApiResponse<Long>`
- `GET /api/course/courses/{id}/students` (INSTRUCTOR, ADMIN) -> Query: `pageable`, Out: `ApiResponse<Page<CourseStudentResponse>>`
- `DELETE /api/course/courses/{id}/students/{userId}` (INSTRUCTOR, ADMIN) -> Out: `ApiResponse<String>`
- `GET /api/course/courses/users/search` (INSTRUCTOR, ADMIN) -> Query: `email`, Out: `ApiResponse<UserInfoDto>`

### 2.3 Cấu Trúc Khóa Học MongoDB (`CourseStructureController` — `/courses/{courseId}/structure`)
- `GET /api/course/courses/{courseId}/structure` (Public / Enrolled) -> Out: `ApiResponse<CourseStructureResponse>`
- `POST /api/course/courses/{courseId}/structure/nodes` (INSTRUCTOR, ADMIN)
  - In: `AddNodeRequest` (`title`, `parentId`, `type`), Out: `ApiResponse<CourseStructureResponse>`
- `POST /api/course/courses/{courseId}/structure/nodes/batch` (INSTRUCTOR, ADMIN)
  - In: `AddMultipleNodesRequest`, Out: `ApiResponse<CourseStructureResponse>`
- `PUT /api/course/courses/{courseId}/structure/reorder` (INSTRUCTOR, ADMIN)
  - In: `ReorderNodesRequest` (`nodeIds`), Out: `ApiResponse<CourseStructureResponse>`
- `GET /api/course/courses/{courseId}/structure/lessons` (Public) -> Out: `ApiResponse<List<StructureNodeResponse>>`
- `PATCH /api/course/courses/{courseId}/structure/nodes/{nodeId}` (INSTRUCTOR, ADMIN)
  - In: `UpdateNodeTitleRequest` (`title`), Out: `ApiResponse<CourseStructureResponse>`
- `DELETE /api/course/courses/{courseId}/structure/nodes/{nodeId}` (INSTRUCTOR, ADMIN) -> Out: `ApiResponse<String>`

### 2.4 Bài Học MongoDB (`LessonController` — `/courses/{courseId}/lessons`)
- `GET /api/course/courses/{courseId}/lessons/{lessonId}` (Authenticated) -> Out: `ApiResponse<LessonResponse>`
- `PUT /api/course/courses/{courseId}/lessons/{lessonId}` (INSTRUCTOR, ADMIN)
  - In: `UpdateLessonRequest` (`title`, `content`, `videoUrl`, `durationSeconds`), Out: `ApiResponse<LessonResponse>`

### 2.5 Tiến Độ Bài Học (`LessonProgressController` — `/courses/{courseId}`)
- `POST /api/course/courses/{courseId}/lessons/{lessonId}/complete` (Authenticated)
  - In: `CompleteLessonRequest` (`watchedSeconds`, `isCompleted`), Out: `ApiResponse<Void>`
- `GET /api/course/courses/{courseId}/progress` (Authenticated) -> Out: `ApiResponse<CourseProgressResponse>`
- `GET /api/course/courses/{courseId}/progress/average` (INSTRUCTOR, ADMIN) -> Out: `ApiResponse<Double>`

### 2.6 Lịch Học Google Meet (`ScheduleController` — `/courses/{courseId}/schedules`)
- `GET /api/course/courses/{courseId}/schedules` (Authenticated) -> Out: `ApiResponse<List<ScheduleResponse>>`
- `POST /api/course/courses/{courseId}/schedules` (INSTRUCTOR, ADMIN)
  - In: `CreateScheduleRequest` (`title`, `startTime`, `endTime`, `meetUrl`), Out: `ApiResponse<ScheduleResponse>`
- `PUT /api/course/courses/{courseId}/schedules/{scheduleId}` (INSTRUCTOR, ADMIN)
  - In: `UpdateScheduleRequest`, Out: `ApiResponse<ScheduleResponse>`
- `DELETE /api/course/courses/{courseId}/schedules/{scheduleId}` (INSTRUCTOR, ADMIN) -> Out: `ApiResponse<String>`

### 2.7 Bảng Tin Thông Báo (`AnnouncementController` — `/courses/{courseId}/announcements`)
- `GET /api/course/courses/{courseId}/announcements` (Authenticated) -> Query: `pageable`, Out: `ApiResponse<Page<AnnouncementResponse>>`
- `POST /api/course/courses/{courseId}/announcements` (INSTRUCTOR, ADMIN)
  - In: `CreateAnnouncementRequest` (`title`, `content`), Out: `ApiResponse<AnnouncementResponse>`
- `DELETE /api/course/courses/{courseId}/announcements/{announcementId}` (INSTRUCTOR, ADMIN) -> Out: `ApiResponse<String>`

### 2.8 Lưu Trữ MinIO (`StorageController` — `/storage`)
- `POST /api/course/storage/presigned-url` (Authenticated) -> In: `PresignedUrlRequest`, Out: `ApiResponse<PresignedUrlResponse>`
- `DELETE /api/course/storage/file` (Authenticated) -> Query: `url`, Out: `ApiResponse<Void>`
- `POST /api/course/storage/multipart/initiate` (INSTRUCTOR, ADMIN) -> In: `InitiateMultipartRequest`, Out: `ApiResponse<InitiateMultipartResponse>`
- `POST /api/course/storage/multipart/complete` (INSTRUCTOR, ADMIN) -> In: `CompleteMultipartRequest`, Out: `ApiResponse<CompleteMultipartResponse>`

### 2.9 Giao Tiếp Nội Bộ (`InternalCourseController` — `/internal`)
- `GET /internal/courses/{courseId}` -> Out: `InternalCourseResponse`
- `GET /internal/courses/{courseId}/lessons/{lessonId}` -> Out: `InternalLessonResponse`
- `GET /internal/courses/{courseId}/enrollments/{userId}` -> Out: `Boolean`
- `GET /internal/courses/{courseId}/students/count` -> Out: `Long`
- `GET /internal/courses/{courseId}/exists` -> Out: `Boolean`
- `GET /internal/courses/{courseId}/lessons/{lessonId}/exists` -> Out: `Boolean`
- `GET /internal/courses` -> Out: `ApiResponse<List<ChatbotCourseResponse>>`
- `GET /internal/courses/enrolled?userId={userId}` -> Out: `ApiResponse<List<ChatbotCourseResponse>>`

---

## 3. Phân Hệ Bài Tập & Đánh Giá (`assignment-service`)

### 3.1 `GET /api/assignment/questions` & `POST /api/assignment/questions`
- **Mô tả**: Quản lý ngân hàng câu hỏi trắc nghiệm (Question Bank).
- **Vai trò cho phép**: `INSTRUCTOR`, `ADMIN`.
- **Request Body tạo câu hỏi**:
  ```json
  {
    "courseId": "course-uuid",
    "content": "Giao thức nào được sử dụng để khám phá dịch vụ trong Spring Cloud?",
    "type": "SINGLE",
    "topic": "Microservices",
    "explanation": "Eureka là giải pháp Service Discovery phổ biến của Netflix OSS.",
    "score": 10,
    "answers": [
      { "content": "Netflix Eureka", "isCorrect": true, "orderIndex": 1 },
      { "content": "Spring Security", "isCorrect": false, "orderIndex": 2 },
      { "content": "MySQL", "isCorrect": false, "orderIndex": 3 },
      { "content": "Redis", "isCorrect": false, "orderIndex": 4 }
    ]
  }
  ```

### 3.2 `POST /api/assignment/questions/import`
- **Mô tả**: Nhập danh sách câu hỏi hàng loạt từ file CSV hoặc TXT.
- **Content-Type**: `multipart/form-data`.
- **Vai trò cho phép**: `INSTRUCTOR`.

### 3.3 `POST /api/assignment/quizzes`
- **Mô tả**: Giảng viên tạo mới một bài kiểm tra Quiz.
- **Request Body**:
  ```json
  {
    "courseId": "course-uuid",
    "title": "Kiểm tra giữa kỳ: Kiến trúc Microservices",
    "duration": 45,
    "passScore": 50,
    "maxAttempts": 2,
    "shuffleQuestions": true,
    "shuffleAnswers": true,
    "showResult": "AFTER_SUBMIT",
    "questionIds": [101, 102, 103, 104]
  }
  ```

### 3.4 `POST /api/assignment/quizzes/{quizId}/attempts`
- **Mô tả**: Học viên bắt đầu một lượt thi trắc nghiệm.
- **Vai trò cho phép**: `STUDENT`.
- **Response**: Trả về `attemptId`, thời gian bắt đầu, thời gian hết hạn (`expiresAt`), danh sách câu hỏi và các lựa chọn (đáp án đúng bị ẩn).

### 3.5 `POST /api/assignment/quizzes/{quizId}/attempts/{attemptId}/submit`
- **Mô tả**: Nộp bài thi trắc nghiệm và nhận kết quả chấm điểm.
- **Request Body**:
  ```json
  {
    "answers": [
      { "questionId": 101, "selectedAnswerIds": [1] },
      { "questionId": 102, "selectedAnswerIds": [4, 5] }
    ]
  }
  ```

### 3.6 `POST /api/assignment/problems`
- **Mô tả**: Tạo bài toán lập trình mới (Coding Problem).
- **Vai trò cho phép**: `INSTRUCTOR`, `ADMIN`.
- **Request Body**:
  ```json
  {
    "courseId": "course-uuid",
    "lessonId": "lesson-uuid",
    "title": "Tìm kiếm đường đi ngắn nhất (Dijkstra)",
    "slug": "dijkstra-shortest-path",
    "description": "Cho đồ thị có hướng có trọng số...",
    "difficulty": "MEDIUM",
    "timeLimitMs": 1000,
    "memoryLimitMb": 256,
    "allowedLangs": ["CPP", "JAVA", "PYTHON"]
  }
  ```

### 3.7 `POST /api/assignment/problems/{problemId}/testcases`
- **Mô tả**: Thêm testcase cho bài tập lập trình (phân loại ẩn hoặc công khai).

### 3.8 `POST /api/assignment/submissions`
- **Mô tả**: Học viên nộp mã nguồn giải bài tập lập trình để chấm tự động qua Judge0.
- **Vai trò cho phép**: `STUDENT`.
- **Request Body**:
  ```json
  {
    "problemId": 12,
    "language": "CPP",
    "sourceCode": "#include <iostream>\nusing namespace std;\nint main() { ... }"
  }
  ```
- **Response (200 OK)**: Trả về `submissionId` ở trạng thái `PENDING`.

### 3.9 `GET /api/assignment/submissions/{submissionId}`
- **Mô tả**: Kiểm tra trạng thái và kết quả chi tiết từng testcase của bài nộp.

### 3.10 `GET /api/assignment/submissions/leaderboard/{problemId}`
- **Mô tả**: Xem bảng xếp hạng top học viên giải bài toán nhanh và tối ưu nhất.

---

## 4. Phân Hệ Diễn Đàn & Blog (`blog-service`)

### 4.1 Bài Viết Blog (`PostController` — `/posts`)
- `GET /api/blog/posts/public` (Public)
  - Query: `pageable`, `tagId`, `keyword`
  - Out: `ApiResponse<Page<PostSummaryResponse>>`
- `GET /api/blog/posts/public/{slug}` (Public)
  - Out: `ApiResponse<PostResponse>` (tự động tăng `viewCount`)
- `GET /api/blog/posts/admin` (ADMIN)
  - Query: `pageable`, `status`, `tagId`, `keyword`
  - Out: `ApiResponse<Page<PostSummaryResponse>>`
- `GET /api/blog/posts/{id}` (Authenticated) -> Out: `ApiResponse<PostResponse>`
- `GET /api/blog/posts/me` (Authenticated)
  - Query: `pageable`, `status`
  - Out: `ApiResponse<Page<PostSummaryResponse>>`
- `POST /api/blog/posts` (Authenticated)
  - In: `@ModelAttribute @Valid CreatePostRequest` (`title`, `content`, `tagIds`, `thumbnail` multipart)
  - Out: `ApiResponse<PostResponse>`
- `PUT /api/blog/posts/{id}` (Authenticated)
  - In: `@ModelAttribute @Valid UpdatePostRequest`
  - Out: `ApiResponse<PostResponse>`
- `DELETE /api/blog/posts/{id}` (Authenticated) -> Out: `ApiResponse<String>`

### 4.2 Thẻ Gắn Blog (`TagController` — `/tags`)
- `GET /api/blog/tags` (Public) -> Out: `ApiResponse<List<TagResponse>>`
- `GET /api/blog/tags/{id}` (Public) -> Out: `ApiResponse<TagResponse>`
- `GET /api/blog/tags/search` (Public) -> Query: `keyword`, Out: `ApiResponse<List<TagResponse>>`
- `POST /api/blog/tags` (ADMIN) -> In: `CreateTagRequest`, Out: `ApiResponse<TagResponse>`
- `PUT /api/blog/tags/{id}` (ADMIN) -> In: `UpdateTagRequest`, Out: `ApiResponse<TagResponse>`
- `DELETE /api/blog/tags/{id}` (ADMIN) -> Out: `ApiResponse<String>`

### 4.3 Bình Luận Bài Viết (`CommentController` — `/posts/{postId}/comments`)
- `GET /api/blog/posts/{postId}/comments` (Public)
  - Query: `pageable`, Out: `ApiResponse<Page<CommentResponse>>`
- `POST /api/blog/posts/{postId}/comments` (Authenticated)
  - In: `CreateCommentRequest` (`content`, `parentId`), Out: `ApiResponse<CommentResponse>`
- `PUT /api/blog/posts/{postId}/comments/{commentId}` (Authenticated)
  - In: `UpdateCommentRequest` (`content`), Out: `ApiResponse<CommentResponse>`
- `DELETE /api/blog/posts/{postId}/comments/{commentId}` (Authenticated) -> Out: `ApiResponse<String>`

### 4.4 Bình Chọn Bài Viết & Bình Luận (`VoteController`)
- `POST /api/blog/posts/{postId}/vote` (Authenticated)
  - In: `VoteRequest` (`voteType`: `UP`, `DOWN`), Out: `ApiResponse<VoteResponse>`
- `POST /api/blog/posts/{postId}/comments/{commentId}/vote` (Authenticated)
  - In: `VoteRequest` (`voteType`: `UP`, `DOWN`), Out: `ApiResponse<VoteResponse>`

### 4.5 Giao Tiếp Nội Bộ (`InternalBlogController` — `/internal`)
- `GET /internal/posts/{postId}` -> Out: `ApiResponse<InternalPostResponse>`
- `GET /internal/posts/{postId}/exists` -> Out: `ApiResponse<Boolean>`

---

## 5. Phân Hệ Trợ Lý Ảo & AI (`chatbot-service`)

### 5.1 `POST /api/chatbot/chat`
- **Mô tả**: Trò chuyện với trợ lý học tập AI.
- **Vai trò cho phép**: `STUDENT`, `INSTRUCTOR`.
- **Header**: `Authorization: Bearer <accessToken>`.
- **Request Body**:
  ```json
  {
    "sessionId": "session-uuid",
    "content": "Giải thích giúp em cơ chế hoạt động của Circuit Breaker trong Microservices?",
    "contextType": "COURSE",
    "contextRefId": "course-uuid"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "code": 1000,
    "message": "Success",
    "result": {
      "sessionId": "session-uuid",
      "reply": "Circuit Breaker là một mẫu thiết kế quan trọng...",
      "tokenUsed": 320
    }
  }
  ```

### 5.2 `GET /api/chatbot/sessions` & `POST /api/chatbot/sessions`
- **Mô tả**: Lấy danh sách lịch sử các phiên hội thoại hoặc tạo một cuộc trò chuyện mới.

### 5.3 `POST /api/chatbot/generate/quiz`
- **Mô tả**: Yêu cầu AI tự động sinh câu hỏi trắc nghiệm từ văn bản bài giảng.
- **Vai trò cho phép**: `INSTRUCTOR`.
- **Request Body**:
  ```json
  {
    "sourceText": "Spring Cloud Gateway cung cấp cơ chế định tuyến...",
    "numQuestions": 5,
    "difficulty": "MEDIUM"
  }
  ```

### 5.4 `POST /api/chatbot/generate/testcases`
- **Mô tả**: Yêu cầu AI tự động sinh bộ testcases cho bài tập lập trình từ mô tả bài toán.
- **Vai trò cho phép**: `INSTRUCTOR`.
- **Request Body**:
  ```json
  {
    "problemDescription": "Cho hai số nguyên a và b (-10^9 <= a, b <= 10^9). Tính tổng a + b.",
    "numCases": 10
  }
  ```
