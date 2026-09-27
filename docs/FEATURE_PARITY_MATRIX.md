# Ma Trận Đối Chiếu Tính Năng — EduLearn LMS (FEATURE_PARITY_MATRIX.md)

Tài liệu này theo dõi mức độ tương đương 100% giữa **Project Mẫu** và **DoAnCK**.  
Mọi chức năng của hệ thống được kiểm kê chi tiết theo từng module, file frontend, file backend, endpoint API, cơ sở dữ liệu và bài kiểm thử liên quan.

---

## Quy Định Trạng Thái (Status Legend)

Dự án chỉ sử dụng đúng 6 trạng thái kỹ thuật sau:
- `NOT_STARTED`: Chưa bắt đầu triển khai mã nguồn.
- `IN_PROGRESS`: Đang trong quá trình lập trình hoặc cấu hình.
- `BLOCKED`: Đang bị tắc nghẽn do phụ thuộc vào module/hạ tầng khác chưa hoàn thành.
- `IMPLEMENTED`: Đã viết xong mã nguồn nghiệp vụ nhưng chưa có kiểm thử đầy đủ.
- `TESTED`: Đã chạy xong unit/integration test cục bộ nhưng chưa kiểm thử tích hợp toàn diện.
- `VERIFIED`: Đã được kiểm thử hành vi thực tế trên môi trường Docker/Full stack, đạt độ tương đương 100% so với project mẫu.

---

## 1. Hạ Tầng & Dịch Vụ Cốt Lõi (Core Infrastructure & Gateway)

| Module | Chức năng | Frontend file | Backend file | API | Database | Test | Trạng thái DoAnCK |
|---|---|---|---|---|---|---|:---:|
| `service-registry` | Eureka Service Discovery | — | `ServiceRegistryApplication.java` | `GET :8761/eureka/apps` | In-memory | `ServiceRegistryApplicationTests.java` | `NOT_STARTED` |
| `api-gateway` | Định tuyến ngược (Dynamic Reverse Routing) | `services/api.client.js` | `ApiGatewayApplication.java`, `application.yaml` | `ALL :8080/api/**` | — | `ApiGatewayApplicationTests.java` | `NOT_STARTED` |
| `api-gateway` | Xác thực JWT & Inject Headers (`AuthFilter`) | `context/AuthContext.jsx` | `AuthFilter.java`, `JwtUtils.java` | Pre-filter trên routes private | — | `AuthFilterTest.java` | `NOT_STARTED` |
| `api-gateway` | Chống giả mạo danh tính (Header Stripping) | — | `application.yaml` (RemoveRequestHeader) | Public endpoints | — | Integration Test | `NOT_STARTED` |
| `api-gateway` | Khả năng chịu lỗi Circuit Breaker (Resilience4j) | `components/ui/Toast.jsx` | `application.yaml` (circuitbreaker) | Tất cả các tuyến microservices | — | Load / Fault test | `NOT_STARTED` |
| `api-gateway` | Actuator Health Routing cho Admin Dashboard | `configurations/env.js` | `application.yaml` | `GET :8080/api/*/actuator/**` | — | Health check test | `NOT_STARTED` |
| `infrastructure` | Docker Compose Stack (Local & Prod) | `Dockerfile`, `nginx.conf` | 7 Dockerfiles backend | Localhost ports | MySQL, Mongo, Redis, MinIO | Docker smoke test | `NOT_STARTED` |

---

## 2. Phân Hệ Xác Thực & Người Dùng (Identity Service)

| Module | Chức năng | Frontend file | Backend file | API | Database | Test | Trạng thái DoAnCK |
|---|---|---|---|---|---|---|:---:|
| `identity-service` | Đăng ký tài khoản mới (Username/Password/Role) | `features/auth/pages/AuthPage.jsx` | `AuthenticationController.java`, `UserServiceImpl.java` | `POST /auth/register` | Bảng `users` | `UserServiceImplTest.java` | `NOT_STARTED` |
| `identity-service` | Đăng nhập cục bộ & cấp JWT (Access + Refresh) | `features/auth/pages/AuthPage.jsx`, `services/auth.api.js` | `AuthenticationController.java`, `AuthenticationServiceImpl.java` | `POST /auth/login` | Bảng `users`, `refresh_tokens` | `AuthenticationServiceImplTest.java` | `NOT_STARTED` |
| `identity-service` | Đăng nhập Google OAuth 2.0 (SSO) | `features/auth/pages/SocialCallbackPage.jsx` | `AuthenticationController.java`, `GoogleAuthService.java` | `POST /auth/social-login/{provider}` | Bảng `users`, `refresh_tokens` | `IdentityApiIntegrationTest.java` | `NOT_STARTED` |
| `identity-service` | Gửi & Xác thực mã OTP qua Email | `features/auth/components/VerifyOtpModal.jsx` | `EmailVerificationController.java`, `EmailVerificationServiceImpl.java` | `POST /auth/send-otp`, `POST /auth/verify-otp` | Bảng `email_verifications` | `EmailVerificationServiceImplTest.java` | `NOT_STARTED` |
| `identity-service` | Làm mới Token (Refresh Token Rotation) | `services/api.client.js` (Interceptors) | `AuthenticationController.java`, `TokenServiceImpl.java` | `POST /auth/refresh` | Bảng `refresh_tokens` | `AuthenticationServiceImplTest.java` | `NOT_STARTED` |
| `identity-service` | Đăng xuất & Thu hồi Refresh Token | `layouts/StudentLayout/components/Header.jsx` | `AuthenticationController.java`, `TokenServiceImpl.java` | `POST /auth/logout` | Bảng `refresh_tokens` | `AuthenticationServiceImplTest.java` | `NOT_STARTED` |
| `identity-service` | Quên mật khẩu & Đặt lại mật khẩu | `features/auth/pages/ForgotPasswordPage.jsx` | `AuthenticationController.java`, `UserServiceImpl.java` | `POST /auth/forgot-password`, `POST /auth/reset-password` | Bảng `email_verifications`, `users` | `IdentityFunctionalTest.java` | `NOT_STARTED` |
| `identity-service` | Xem & Cập nhật Hồ sơ cá nhân | `features/settings/pages/SettingsPage.jsx` | `UserController.java`, `UserServiceImpl.java` | `GET /users/me`, `PUT /users/me` | Bảng `users` | `UserServiceImplTest.java` | `NOT_STARTED` |
| `identity-service` | Tải lên ảnh đại diện cá nhân (Avatar) | `hooks/useFileUpload.js` | `UserController.java`, `MinioStorageService.java` | `POST /users/me/avatar` | MinIO bucket `lms-identity-bucket` | `useFileUpload.test.jsx` | `NOT_STARTED` |
| `identity-service` | Đổi mật khẩu tài khoản | `features/settings/pages/SettingsPage.jsx` | `UserController.java`, `UserServiceImpl.java` | `PUT /users/me/change-password` | Bảng `users` | `UserServiceImplTest.java` | `NOT_STARTED` |
| `identity-service` | Xem hồ sơ công khai của người dùng khác | `features/courses/pages/student/DetailCoursePage.jsx` | `UserController.java`, `UserServiceImpl.java` | `GET /users/{userId}/profile` | Bảng `users` | `UserServiceImplTest.java` | `NOT_STARTED` |
| `identity-service` | Quản trị danh sách người dùng & Trạng thái | `features/dashboard/components/admin/Management/UserManagement.jsx` | `AdminUserController.java`, `UserServiceImpl.java` | `GET /admin/users`, `PATCH /admin/users/{userId}/status` | Bảng `users` | `IdentityApiIntegrationTest.java` | `NOT_STARTED` |
| `identity-service` | Quản trị viên tạo tài khoản Admin mới | `features/dashboard/components/admin/Management/AdminManagement.jsx` | `AdminUserController.java`, `UserServiceImpl.java` | `POST /admin/users` | Bảng `users` | `IdentityApiIntegrationTest.java` | `NOT_STARTED` |
| `identity-service` | API nội bộ cung cấp thông tin User cho service khác | — | `InternalUserController.java` | `GET /internal/users/{id}`, `POST /internal/users/batch` | Bảng `users` | `IdentityApiIntegrationTest.java` | `NOT_STARTED` |

---

## 3. Phân Hệ Quản Lý Khóa Học & Nội Dung (Course Service)

| Module | Chức năng | Frontend file | Backend file | API | Database | Test | Trạng thái DoAnCK |
|---|---|---|---|---|---|---|:---:|
| `course-service` | Quản lý danh mục khóa học (Category CRUD) | `features/dashboard/components/admin/Management/WebContentMgmt.jsx` | `CategoryController.java`, `CategoryServiceImpl.java` | `GET /categories`, `POST /categories`, `PUT /categories/{id}` | Bảng `categories` | `CategoryServiceImplTest.java` | `NOT_STARTED` |
| `course-service` | Danh sách khóa học công khai & Tìm kiếm/Lọc | `features/courses/pages/student/ListCoursePage.jsx` | `CourseController.java`, `CourseServiceImpl.java` | `GET /courses` (Page, Category, Search) | Bảng `courses`, `course_categories` | `CourseServiceImplTest.java` | `NOT_STARTED` |
| `course-service` | Chi tiết khóa học (Overview & Preview) | `features/courses/pages/student/DetailCoursePage.jsx` | `CourseController.java`, `CourseServiceImpl.java` | `GET /courses/{id}`, `GET /courses/{id}/public` | Bảng `courses`, `course_learning_points`, `course_requirements` | `CourseApiIntegrationTest.java` | `NOT_STARTED` |
| `course-service` | Giảng viên tạo mới khóa học | `features/courses/pages/teacher/CreateCoursePage.jsx` | `CourseController.java`, `CourseServiceImpl.java` | `POST /courses` | Bảng `courses`, MongoDB `CourseStructure` | `CourseServiceImplTest.java` | `NOT_STARTED` |
| `course-service` | Giảng viên chỉnh sửa khóa học & Đổi trạng thái | `features/courses/pages/teacher/EditCoursePage.jsx` | `CourseController.java`, `CourseServiceImpl.java` | `PUT /courses/{id}`, `PATCH /courses/{id}/status` | Bảng `courses` | `CourseServiceImplTest.java` | `NOT_STARTED` |
| `course-service` | Quản lý cây bài học (Chapters/Lessons Structure) | `features/courses/pages/teacher/CourseManagementCenter.jsx` | `CourseStructureController.java`, `CourseStructureServiceImpl.java` | `GET /courses/{id}/structure`, `PUT /courses/{id}/structure` | MongoDB `course_structures`, `lessons` | `CourseStructureServiceImplTest.java` | `NOT_STARTED` |
| `course-service` | Quản lý nội dung bài học (Video & Document) | `features/courses/pages/student/CourseHubPage.jsx` | `LessonController.java`, `LessonServiceImpl.java` | `GET /courses/{cId}/lessons/{lId}`, `POST /courses/{cId}/lessons` | MongoDB `lessons` | `CourseFunctionalTest.java` | `NOT_STARTED` |
| `course-service` | Upload thumbnail khóa học & file bài giảng | `hooks/useFileUpload.js` | `StorageController.java`, `StorageServiceImpl.java` | `POST /storage/upload` | MinIO bucket `lms-course-bucket` | `StorageServiceImplTest.java` | `NOT_STARTED` |
| `course-service` | Học viên đăng ký ghi danh khóa học | `features/courses/pages/student/DetailCoursePage.jsx` | `CourseController.java`, `CourseServiceImpl.java` | `POST /courses/{id}/enroll` | Bảng `enrollments` | `CourseServiceImplTest.java` | `NOT_STARTED` |
| `course-service` | Danh sách khóa học đã ghi danh của học viên | `features/courses/pages/student/MyCoursePage.jsx` | `CourseController.java`, `CourseServiceImpl.java` | `GET /courses/my-courses` | Bảng `enrollments`, `courses` | `CourseServiceImplTest.java` | `NOT_STARTED` |
| `course-service` | Đánh dấu bài học hoàn thành & Theo dõi tiến độ | `features/courses/pages/student/CourseHubPage.jsx` | `LessonProgressController.java`, `LessonProgressServiceImpl.java` | `GET /courses/{id}/progress`, `POST /courses/{id}/lessons/{lId}/progress` | Bảng `lesson_progress` | `LessonProgressServiceImplTest.java` | `NOT_STARTED` |
| `course-service` | Quản lý lịch học trực tuyến & Google Meet | `features/courses/pages/teacher/CourseManagementCenter.jsx` | `ScheduleController.java`, `ScheduleServiceImpl.java` | `GET /courses/{cId}/schedules`, `POST /courses/{cId}/schedules` | Bảng `schedules` | `ScheduleServiceImplTest.java` | `NOT_STARTED` |
| `course-service` | Quản lý thông báo trong khóa học | `features/courses/pages/student/DetailMyCoursePage.jsx` | `AnnouncementController.java`, `AnnouncementServiceImpl.java` | `GET /courses/{cId}/announcements`, `POST /courses/{cId}/announcements` | Bảng `announcements` | `AnnouncementServiceImplTest.java` | `NOT_STARTED` |
| `course-service` | Quản trị viên duyệt và quản lý toàn bộ khóa học | `features/dashboard/components/admin/Management/CourseManagement.jsx` | `CourseController.java`, `CourseServiceImpl.java` | `GET /admin/courses`, `PATCH /courses/{id}/status` | Bảng `courses` | `CourseApiIntegrationTest.java` | `NOT_STARTED` |
| `course-service` | API nội bộ cung cấp thông tin khóa học | — | `InternalCourseController.java` | `GET /internal/courses/{id}`, `GET /internal/courses/{id}/summary` | Bảng `courses` | `InternalCourseServiceImplTest.java` | `NOT_STARTED` |

---

## 4. Phân Hệ Bài Tập, Trắc Nghiệm & Chấm Code (Assignment Service)

| Module | Chức năng | Frontend file | Backend file | API | Database | Test | Trạng thái DoAnCK |
|---|---|---|---|---|---|---|:---:|
| `assignment-service` | Ngân hàng câu hỏi trắc nghiệm (Question Bank) | `features/assignment/quiz/pages/teacher/EditQuizPage.jsx` | `QuestionController.java`, `QuestionServiceImpl.java` | `GET /questions`, `POST /questions`, `PUT /questions/{id}` | Bảng `questions`, `answers` | `QuestionServiceImplTest.java` | `NOT_STARTED` |
| `assignment-service` | Nhập câu hỏi từ file CSV / TXT | `features/assignment/quiz/components/ImportQuestionModal.jsx` | `QuestionController.java`, `CsvQuestionParser.java`, `TxtQuestionParser.java` | `POST /questions/import` | Bảng `questions`, `answers` | `CsvQuestionParserTest.java`, `TxtQuestionParserTest.java` | `NOT_STARTED` |
| `assignment-service` | Tạo & Cấu hình bài kiểm tra Quiz | `features/assignment/quiz/pages/teacher/CreateQuizPage.jsx` | `QuizController.java`, `QuizServiceImpl.java` | `POST /quizzes`, `PUT /quizzes/{id}` | Bảng `quizzes`, `quiz_questions` | `QuizServiceImplTest.java` | `NOT_STARTED` |
| `assignment-service` | Danh sách Quiz theo khóa học | `features/assignment/quiz/pages/student/QuizListingPage.jsx` | `QuizController.java`, `QuizServiceImpl.java` | `GET /quizzes/course/{courseId}` | Bảng `quizzes` | `QuizServiceImplTest.java` | `NOT_STARTED` |
| `assignment-service` | Bắt đầu lượt thi trắc nghiệm (Attempt) | `features/assignment/quiz/pages/student/QuizOverviewPage.jsx` | `QuizAttemptController.java`, `QuizAttemptServiceImpl.java` | `POST /quizzes/{quizId}/attempts` | Bảng `quiz_attempts`, `quiz_answer_records` | `QuizAttemptServiceImplTest.java` | `NOT_STARTED` |
| `assignment-service` | Lưu câu trả lời & Nộp bài thi trắc nghiệm | `features/assignment/quiz/pages/student/ExerciseQuizPage.jsx` | `QuizAttemptController.java`, `QuizAttemptServiceImpl.java` | `PUT /quizzes/{qId}/attempts/{aId}/answers`, `POST /quizzes/{qId}/attempts/{aId}/submit` | Bảng `quiz_attempts`, `quiz_answer_records`, `grades` | `QuizAttemptServiceImplTest.java`, `quizScoring.test.js` | `NOT_STARTED` |
| `assignment-service` | Xem kết quả bài thi & Lịch sử chi tiết | `features/assignment/quiz/pages/student/ExerciseQuizResultPage.jsx`, `QuizHistoryDetailPage.jsx` | `QuizAttemptController.java`, `QuizAttemptServiceImpl.java` | `GET /quizzes/{qId}/attempts/{aId}`, `GET /quizzes/{qId}/attempts` | Bảng `quiz_attempts`, `quiz_answer_records` | `QuizAttemptServiceImplTest.java` | `NOT_STARTED` |
| `assignment-service` | Tạo bài toán lập trình (Coding Problem) | `features/assignment/code-judge/pages/teacher/CreateCodingPage.jsx` | `ProblemController.java`, `ProblemServiceImpl.java` | `POST /problems`, `PUT /problems/{id}` | Bảng `problems` | `ProblemServiceImplTest.java` | `NOT_STARTED` |
| `assignment-service` | Quản lý bộ Testcase (Ẩn / Hiện) | `features/assignment/code-judge/pages/teacher/CreateCodingPage.jsx` | `TestCaseController.java`, `TestCaseServiceImpl.java` | `GET /problems/{id}/testcases`, `POST /problems/{id}/testcases` | Bảng `test_cases` | `TestCaseServiceImplTest.java` | `NOT_STARTED` |
| `assignment-service` | Nhập xuất bộ Testcase theo định dạng nén/file | `features/assignment/code-judge/pages/teacher/PreviewImportPage.jsx` | `TestCaseController.java`, `TestCaseTemplateServiceImpl.java` | `POST /problems/{id}/testcases/import-zip`, `GET /problems/{id}/testcases/template` | Bảng `test_cases` | `TestCaseTemplateServiceImplTest.java` | `NOT_STARTED` |
| `assignment-service` | Trình soạn thảo Code & Thực thi chấm điểm | `features/assignment/code-judge/pages/student/ExerciseCodePage.jsx` | `CodeJudgeController.java`, `JudgeServiceImpl.java` | `POST /submissions`, `GET /submissions/{id}` | MongoDB `submissions`, Judge0 Engine | `SubmissionServiceImplTest.java`, `useJudge0.test.jsx` | `NOT_STARTED` |
| `assignment-service` | Xem kết quả chấm bài lập trình chi tiết | `features/assignment/code-judge/pages/student/ExerciseCodeResultPage.jsx` | `CodeJudgeController.java`, `SubmissionServiceImpl.java` | `GET /submissions/{id}` | MongoDB `submissions` | `SubmissionServiceImplTest.java` | `NOT_STARTED` |
| `assignment-service` | Lịch sử nộp bài cá nhân & Giảng viên xem bài nộp | `features/assignment/code-judge/pages/student/ExerciseCodePage.jsx`, `ExerciseResultsPage.jsx` | `CodeJudgeController.java`, `SubmissionServiceImpl.java` | `GET /submissions/problem/{problemId}`, `GET /submissions/my-history` | MongoDB `submissions` | `SubmissionServiceImplTest.java` | `NOT_STARTED` |
| `assignment-service` | Bảng xếp hạng lập trình (Leaderboard) | `features/assignment/leaderboard/pages/student/LeaderboardPage.jsx` | `CodeJudgeController.java`, `LeaderboardServiceImpl.java` | `GET /submissions/leaderboard/{problemId}` | Bảng `problems`, MongoDB `submissions` | `SubmissionServiceImplTest.java` | `NOT_STARTED` |
| `assignment-service` | Bảng điểm tổng hợp (Gradebook) | `features/assignment/Exercise/pages/teacher/ExerciseResultsPage.jsx` | `QuizController.java`, `GradeServiceImpl.java` | `GET /quizzes/course/{cId}/gradebook` | Bảng `grades`, `quizzes` | `QuizServiceImplTest.java` | `NOT_STARTED` |
| `assignment-service` | Scheduled job dọn dẹp bài thi kẹt & đồng bộ điểm | — | `QuizCleanupJob.java`, `SubmissionCleanupJob.java` | Cron background jobs | Bảng `quiz_attempts`, MongoDB `submissions` | `QuizAttemptServiceImplTest.java` | `NOT_STARTED` |

---

## 5. Phân Hệ Diễn Đàn & Blog (Blog Service)

| Module | Chức năng | Frontend file | Backend file | API | Database | Test | Trạng thái DoAnCK |
|---|---|---|---|---|---|---|:---:|
| `blog-service` | Quản lý Thẻ bài viết (Tags CRUD) | `features/blog/pages/student/BlogHubPage.jsx` | `TagController.java`, `TagServiceImpl.java` | `GET /tags`, `POST /tags`, `PUT /tags/{id}` | Bảng `tags` | `TagServiceImplTest.java` | `NOT_STARTED` |
| `blog-service` | Danh sách bài viết Blog & Lọc theo Tag/Tìm kiếm | `features/blog/pages/student/BlogHubPage.jsx` | `PostController.java`, `PostServiceImpl.java` | `GET /posts` (Page, Tag, Keyword) | Bảng `posts`, `post_tags`, `tags` | `PostServiceImplTest.java` | `NOT_STARTED` |
| `blog-service` | Xem chi tiết bài viết Blog (Markdown Preview) | `features/blog/pages/student/BlogDetailPage.jsx` | `PostController.java`, `PostServiceImpl.java` | `GET /posts/{slugOrId}` | Bảng `posts` (tăng `view_count`) | `BlogFunctionalTest.java` | `NOT_STARTED` |
| `blog-service` | Viết bài mới & Tải ảnh bìa (Thumbnail) | `features/blog/pages/student/CreateBlogPostPage.jsx` | `PostController.java`, `PostServiceImpl.java`, `MinioStorageService.java` | `POST /posts`, `POST /posts/thumbnail` | Bảng `posts`, `post_tags`, MinIO `lms-blog-bucket` | `PostServiceImplTest.java` | `NOT_STARTED` |
| `blog-service` | Chỉnh sửa bài viết cá nhân & Xóa bài | `features/blog/pages/student/EditBlogPostPage.jsx` | `PostController.java`, `PostServiceImpl.java` | `PUT /posts/{id}`, `DELETE /posts/{id}` | Bảng `posts`, `post_tags` | `PostServiceImplTest.java` | `NOT_STARTED` |
| `blog-service` | Quản lý danh sách bài viết cá nhân | `features/blog/pages/student/MyBlogPostsPage.jsx` | `PostController.java`, `PostServiceImpl.java` | `GET /posts/my-posts` | Bảng `posts` | `PostServiceImplTest.java` | `NOT_STARTED` |
| `blog-service` | Bình luận bài viết & Trả lời bình luận (Nested) | `features/blog/components/CommentSection.jsx` | `CommentController.java`, `CommentServiceImpl.java` | `GET /posts/{pId}/comments`, `POST /posts/{pId}/comments` | Bảng `comments` | `CommentServiceImplTest.java` | `NOT_STARTED` |
| `blog-service` | Bình chọn Upvote / Downvote bài viết & Comment | `features/blog/components/VoteButtons.jsx` | `VoteController.java`, `VoteServiceImpl.java` | `POST /posts/{pId}/vote`, `POST /comments/{cId}/vote` | Bảng `votes` | `VoteServiceImplTest.java` | `NOT_STARTED` |
| `blog-service` | Quản trị bài viết Blog (Admin kiểm duyệt/ẩn) | `features/dashboard/components/admin/Management/BlogMgmt.jsx` | `PostController.java`, `PostServiceImpl.java` | `GET /admin/posts`, `PATCH /posts/{id}/status` | Bảng `posts` | `BlogApiIntegrationTest.java` | `NOT_STARTED` |

---

## 6. Phân Hệ Trợ Lý Ảo & Tự Sinh Nội Dung (Chatbot Service)

| Module | Chức năng | Frontend file | Backend file | API | Database | Test | Trạng thái DoAnCK |
|---|---|---|---|---|---|---|:---:|
| `chatbot-service` | Giao diện Chatbot học tập tích hợp đa trang | `components/ui/ChatBot.jsx` | `ChatController.java`, `ChatServiceImpl.java` | `POST /chat` | MongoDB `chat_messages`, Redis Cache | `ChatServiceImplTest.java` | `NOT_STARTED` |
| `chatbot-service` | Quản lý danh sách phiên hội thoại & Đổi phiên | `components/ui/ChatBot.jsx` | `ChatController.java`, `ChatSessionServiceImpl.java` | `GET /sessions`, `POST /sessions` | MongoDB `chat_sessions` | `ChatSessionServiceImplTest.java` | `NOT_STARTED` |
| `chatbot-service` | Xem lịch sử tin nhắn của một phiên | `components/ui/ChatBot.jsx` | `ChatController.java`, `ChatMessageServiceImpl.java` | `GET /sessions/{id}/messages` | MongoDB `chat_messages` | `ChatMessageServiceImplTest.java` | `NOT_STARTED` |
| `chatbot-service` | Kiểm soát hạn ngạch tin nhắn AI (Rate Limiter) | `components/ui/ChatBot.jsx` | `RateLimiterServiceImpl.java` | Header / Interceptor kiểm tra | Redis (counter per user/day) | `RateLimiterServiceImplTest.java` | `NOT_STARTED` |
| `chatbot-service` | Tự động sinh câu hỏi Quiz từ văn bản bài giảng | `features/assignment/quiz/pages/teacher/CreateQuizPage.jsx` | `GenerateController.java`, `GenerateServiceImpl.java` | `POST /generate/quiz` | External LLM API | `GenerateServiceImplTest.java` | `NOT_STARTED` |
| `chatbot-service` | Tự động sinh Testcase lập trình từ mô tả bài | `features/assignment/code-judge/pages/teacher/CreateCodingPage.jsx` | `GenerateController.java`, `GenerateServiceImpl.java` | `POST /generate/testcases` | External LLM API | `GenerateServiceImplTest.java` | `NOT_STARTED` |
| `chatbot-service` | Background job lưu trữ/archive phiên chat cũ | — | `SessionArchiveJob.java` | Cron job định kỳ | MongoDB `chat_sessions` | `ChatSessionServiceImplTest.java` | `NOT_STARTED` |

---

## 7. Giao Diện & Bảng Điều Khiển (Dashboard & UI)

| Module | Chức năng | Frontend file | Backend file | API | Database | Test | Trạng thái DoAnCK |
|---|---|---|---|---|---|---|:---:|
| `frontend` | Trang chủ hệ thống & Giới thiệu khóa học nổi bật | `features/home/pages/HomePage.jsx` | `CourseController.java` | `GET /courses/featured` | Bảng `courses` | UI Smoke Test | `NOT_STARTED` |
| `frontend` | Dashboard Học viên (Tổng quan tiến độ & Điểm số) | `features/dashboard/pages/student/DashboardStudent.jsx` | `services/dashboard.api.js` | Tích hợp Identity + Course + Assignment | — | UI Smoke Test | `NOT_STARTED` |
| `frontend` | Dashboard Giảng viên (Khóa học, Thống kê sinh viên) | `features/dashboard/pages/teacher/HomePageInstructor.jsx` | `services/instructorDashboard.api.js` | Tích hợp Course + Assignment | — | UI Smoke Test | `NOT_STARTED` |
| `frontend` | Admin Service Health Monitor Dashboard | `features/dashboard/pages/admin/HomePageAdmin.jsx` | Actuator liveness endpoints | `GET /api/*/actuator/health/liveness` | — | UI Smoke Test | `NOT_STARTED` |
| `frontend` | Bảo vệ định tuyến phía Client (`PrivateRoute`, `RoleRoute`)| `routes/PrivateRoute.jsx`, `routes/RoleRoute.jsx` | — | Kiểm tra token & role trong AuthContext | LocalStorage | `PrivateRoute.test.jsx`, `RoleRoute.test.jsx` | `NOT_STARTED` |
| `frontend` | Xử lý lỗi toàn cục và trang báo lỗi | `features/errors/pages/AppErrorPage.jsx`, `routes/FallbackRoute.jsx` | — | — | — | `appError.test.js` | `NOT_STARTED` |

---

## 8. Bảng Tổng Hợp Trạng Thái Tiến Độ

| Phân hệ / Module | Tổng số chức năng | NOT_STARTED | IN_PROGRESS | IMPLEMENTED | TESTED | VERIFIED | % Hoàn thành thực tế |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| Core & Infrastructure | 7 | 7 | 0 | 0 | 0 | 0 | 0% |
| Identity Service | 14 | 14 | 0 | 0 | 0 | 0 | 0% |
| Course Service | 15 | 15 | 0 | 0 | 0 | 0 | 0% |
| Assignment Service | 16 | 16 | 0 | 0 | 0 | 0 | 0% |
| Blog Service | 9 | 9 | 0 | 0 | 0 | 0 | 0% |
| Chatbot Service | 7 | 7 | 0 | 0 | 0 | 0 | 0% |
| Frontend & Dashboard | 6 | 6 | 0 | 0 | 0 | 0 | 0% |
| **TỔNG CỘNG** | **74** | **74** | **0** | **0** | **0** | **0** | **0%** |

*(Bảng này sẽ được cập nhật chuyển đổi trạng thái liên tục theo từng commit và kịch bản test thực tế)*.
