# Ma Trận Đối Chiếu Tính Năng — EduLearn LMS (FEATURE_PARITY_MATRIX.md)

Tài liệu này theo dõi mức độ tương đương 100% giữa **Project Mẫu** và **EduLearn LMS** (`d:\LMS-main`).  
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

| Module | Chức năng | Frontend file | Backend file | API | Database | Test | Trạng thái LMS-main |
|---|---|---|---|---|---|---|:---:|
| `service-registry` | Eureka Service Discovery | — | `ServiceRegistryApplication.java` | `GET :8761/eureka/apps` | In-memory | `ServiceRegistryApplicationTests.java` | `VERIFIED` |
| `api-gateway` | Định tuyến ngược (Dynamic Reverse Routing) | `services/api.client.js` | `ApiGatewayApplication.java`, `application.yaml` | `ALL :8080/api/**` | — | `ApiGatewayApplicationTests.java` | `VERIFIED` |
| `api-gateway` | Xác thực JWT & Inject Headers (`AuthFilter`) | `context/AuthContext.jsx` | `AuthFilter.java`, `JwtUtils.java` | Pre-filter trên routes private | — | `AuthFilterTest.java` | `VERIFIED` |
| `api-gateway` | Chống giả mạo danh tính (Header Stripping) | — | `application.yaml` (RemoveRequestHeader) | Public endpoints | — | Integration Test | `VERIFIED` |
| `api-gateway` | Khả năng chịu lỗi Circuit Breaker (Resilience4j) | `components/ui/Toast.jsx` | `application.yaml` (circuitbreaker) | Tất cả các tuyến microservices | — | Load / Fault test | `VERIFIED` |
| `api-gateway` | Actuator Health Routing cho Admin Dashboard | `configurations/env.js` | `application.yaml` | `GET :8080/api/*/actuator/**` | — | Health check test | `VERIFIED` |
| `infrastructure` | Docker Compose Stack (Local & Prod) | `Dockerfile`, `nginx.conf` | 7 Dockerfiles backend | Localhost ports | MySQL, Mongo, Redis, MinIO | Docker smoke test, 11 containers healthy | `VERIFIED` |

---

## 2. Phân Hệ Xác Thực & Người Dùng (Identity Service)

| Module | Chức năng | Frontend file | Backend file | API | Database | Test | Trạng thái LMS-main |
|---|---|---|---|---|---|---|:---:|
| `identity-service` | Đăng ký tài khoản mới (Username/Password/Role) | `templates/auth/register.html` | `AuthenticationController.java`, `UserServiceImpl.java` | `POST /auth/register` | Bảng `users` | `UserServiceImplTest.java`, E2E Flow 1 | `VERIFIED` |
| `identity-service` | Đăng nhập cục bộ & cấp JWT (Access + Refresh) | `templates/auth/login.html` | `AuthenticationController.java`, `AuthenticationServiceImpl.java` | `POST /auth/login` | Bảng `users`, `refresh_tokens` | `AuthenticationServiceImplTest.java`, E2E Flow 1-4 | `VERIFIED` |
| `identity-service` | Đăng nhập Google OAuth 2.0 (SSO) | `features/auth/pages/SocialCallbackPage.jsx` | `AuthenticationController.java`, `GoogleAuthService.java` | `POST /auth/social-login/{provider}` | Bảng `users`, `refresh_tokens` | `AuthenticationServiceImplTest.java` (Cần Google Client ID/Secret thật để nghiệm thu VERIFIED) | `TESTED` |
| `identity-service` | Gửi & Xác thực mã OTP qua Email | `templates/auth/verify-email.html` | `EmailVerificationController.java`, `EmailVerificationServiceImpl.java` | `POST /auth/verify-email/send`, `POST /auth/verify-email` | Bảng `email_verifications` | `EmailVerificationServiceImplTest.java`, `otp-countdown.test.js`, E2E Flow 1 | `VERIFIED` |
| `identity-service` | Làm mới Token (Refresh Token Rotation) | `services/api.client.js` (Interceptors) | `AuthenticationController.java`, `TokenServiceImpl.java` | `POST /auth/refresh` | Bảng `refresh_tokens` | `AuthenticationServiceImplTest.java`, `TokenServiceImplTest.java` | `VERIFIED` |
| `identity-service` | Đăng xuất & Thu hồi Refresh Token | `layouts/StudentLayout/components/Header.jsx` | `AuthenticationController.java`, `TokenServiceImpl.java` | `POST /auth/logout` | Bảng `refresh_tokens` | `AuthenticationServiceImplTest.java` | `VERIFIED` |
| `identity-service` | Quên mật khẩu & Đặt lại mật khẩu | `templates/auth/forgot-password.html` | `EmailVerificationController.java`, `EmailVerificationServiceImpl.java` | `POST /auth/forgot-password`, `POST /auth/reset-password` | Bảng `email_verifications`, `users` | `IdentityFunctionalTest.java` | `VERIFIED` |
| `identity-service` | Xem & Cập nhật Hồ sơ cá nhân | `templates/profile.html` | `UserController.java`, `UserServiceImpl.java` | `GET /users/myinfo`, `PUT /users/myinfo` | Bảng `users` | `UserServiceImplTest.java`, `ProfileControllerTest.java` | `VERIFIED` |
| `identity-service` | Tải lên ảnh đại diện cá nhân (Presigned URL) | `templates/profile.html` | `StorageController.java`, `StorageServiceImpl.java` | `POST /storage/presigned-url` | MinIO bucket `lms-identity-bucket` | `StorageServiceImplTest.java`, `browser-avatar-cors.e2e.test.js` | `VERIFIED` |
| `identity-service` | Đổi mật khẩu tài khoản | `templates/profile.html` | `UserController.java`, `UserServiceImpl.java` | `PATCH /users/myinfo/password` | Bảng `users` | `UserServiceImplTest.java` | `VERIFIED` |
| `identity-service` | Đổi vai trò tài khoản (Role Request) | `templates/profile.html` | `UserController.java`, `UserServiceImpl.java` | `PATCH /users/myinfo/role` | Bảng `users` | `UserServiceImplTest.java` | `VERIFIED` |
| `identity-service` | Xem hồ sơ công khai của người dùng khác | `templates/instructor/profile.html` | `UserController.java`, `UserServiceImpl.java` | `GET /users/{userId}/profile` | Bảng `users` | `UserServiceImplTest.java`, `IdentityApiIntegrationTest.java` | `VERIFIED` |
| `identity-service` | Quản trị danh sách người dùng & Trạng thái | `templates/admin/users.html` | `AdminUserController.java`, `AdminUserServiceImpl.java` | `GET /admin/users`, `PATCH /admin/users/{id}/active` | Bảng `users` | `UserServiceImplTest.java`, E2E Flow 4 | `VERIFIED` |
| `identity-service` | Quản trị viên tạo tài khoản Admin mới | `templates/admin/users.html` | `AdminUserController.java`, `AdminUserServiceImpl.java` | `POST /admin/users/admins` | Bảng `users` | `UserServiceImplTest.java`, E2E Flow 4 | `VERIFIED` |
| `identity-service` | API nội bộ cung cấp thông tin User cho service khác | — | `InternalUserController.java` | `GET /internal/user/{id}`, `POST /internal/users/batch`, `GET /internal/users/{userId}/exists` | Bảng `users` | Phase 1 Contract Tests (16/16 PASS) | `VERIFIED` |

---

## 3. Phân Hệ Quản Lý Khóa Học & Nội Dung (Course Service)

| Module | Chức năng | Frontend file | Backend file | API | Database | Test | Trạng thái LMS-main |
|---|---|---|---|---|---|---|:---:|
| `course-service` | Quản lý danh mục khóa học (Category CRUD) | `templates/admin/categories.html` | `CategoryController.java`, `CategoryServiceImpl.java` | `GET /categories`, `POST /categories`, `PUT /categories/{id}`, `DELETE /categories/{id}` | Bảng `categories` | `CategoryServiceImplTest.java`, E2E Flow 4 | `VERIFIED` |
| `course-service` | Danh sách khóa học công khai & Tìm kiếm/Lọc | `templates/courses/index.html` | `CourseController.java`, `CourseServiceImpl.java` | `GET /courses` (Pageable, status, categoryId, keyword, level) | Bảng `courses` | `CourseServiceImplTest.java`, E2E Flow 1 | `VERIFIED` |
| `course-service` | Chi tiết khóa học (Overview & Preview) | `templates/courses/detail.html` | `CourseController.java`, `CourseServiceImpl.java` | `GET /courses/{id}` | Bảng `courses` | `CourseApiIntegrationTest.java`, E2E Flow 1 | `VERIFIED` |
| `course-service` | Giảng viên tạo mới khóa học | `templates/instructor/courses/create.html` | `CourseController.java`, `CourseServiceImpl.java` | `POST /courses` (multipart `CreateCourseRequest`) | Bảng `courses`, MongoDB `CourseStructure` | `CourseServiceImplTest.java`, E2E Flow 2 | `VERIFIED` |
| `course-service` | Giảng viên chỉnh sửa khóa học & Đổi trạng thái | `templates/instructor/courses/edit.html` | `CourseController.java`, `CourseServiceImpl.java` | `PUT /courses/{id}`, `PATCH /courses/{id}/lock`, `PATCH /courses/{id}/unlock` | Bảng `courses` | `CourseServiceImplTest.java`, E2E Flow 2 | `VERIFIED` |
| `course-service` | Quản lý cây bài học (Chapters/Lessons Structure) | `templates/instructor/courses/manage.html` | `CourseStructureController.java`, `CourseStructureServiceImpl.java` | `GET /courses/{courseId}/structure`, `POST /structure/nodes`, `PUT /structure/reorder` | MongoDB `course_structures`, `lessons` | `curriculum-builder.test.js`, E2E Flow 2 | `VERIFIED` |
| `course-service` | Quản lý nội dung bài học (Video & Document) | `templates/courses/learn.html` | `LessonController.java`, `LessonServiceImpl.java` | `GET /courses/{cId}/lessons/{lId}`, `PUT /courses/{cId}/lessons/{lId}` | MongoDB `lessons` | `LessonServiceImplTest.java`, E2E Flow 1-2 | `VERIFIED` |
| `course-service` | Upload thumbnail khóa học & file bài giảng | `templates/instructor/courses/create.html` | `StorageController.java`, `StorageServiceImpl.java`, `MinioServiceImpl.java` | `POST /storage/presigned-url`, `POST /storage/multipart/initiate` | MinIO bucket `lms-course-bucket` | `StorageServiceImplTest.java`, E2E Flow 2 | `VERIFIED` |
| `course-service` | Học viên đăng ký ghi danh khóa học | `templates/courses/detail.html` | `CourseController.java`, `CourseServiceImpl.java` | `POST /courses/{id}/enroll` | Bảng `enrollments` | `EnrollmentConcurrencyTest.java`, E2E Flow 1 | `VERIFIED` |
| `course-service` | Danh sách khóa học của tôi (Tạo & Ghi danh) | `templates/courses/my-courses.html` | `CourseController.java`, `CourseServiceImpl.java` | `GET /courses/my`, `GET /courses/enrolled` | Bảng `enrollments`, `courses` | `CourseServiceImplTest.java`, E2E Flow 1 | `VERIFIED` |
| `course-service` | Đánh dấu bài học hoàn thành & Theo dõi tiến độ | `templates/courses/learn.html` | `LessonProgressController.java`, `LessonProgressServiceImpl.java` | `POST /courses/{courseId}/lessons/{lessonId}/complete`, `GET /courses/{courseId}/progress` | Bảng `lesson_progress` | `LessonProgressServiceImplTest.java`, E2E Flow 1 | `VERIFIED` |
| `course-service` | Quản lý lịch học trực tuyến & Google Meet | `templates/courses/detail.html` | `ScheduleController.java`, `ScheduleServiceImpl.java` | `GET /courses/{cId}/schedules`, `POST /schedules`, `PUT /schedules/{id}`, `DELETE /schedules/{id}` | Bảng `schedules` | `ScheduleServiceImplTest.java`, `learning-hub.test.js` | `VERIFIED` |
| `course-service` | Quản lý thông báo trong khóa học | `templates/courses/detail.html` | `AnnouncementController.java`, `AnnouncementServiceImpl.java` | `GET /courses/{cId}/announcements`, `POST /announcements`, `DELETE /announcements/{id}` | Bảng `announcements` | `AnnouncementServiceImplTest.java`, `learning-hub.test.js` | `VERIFIED` |
| `course-service` | Quản trị viên duyệt và quản lý toàn bộ khóa học | `templates/admin/courses.html` | `CourseController.java`, `CourseServiceImpl.java` | `GET /courses/my/stats`, `PATCH /courses/{id}/lock`, `PATCH /courses/{id}/unlock` | Bảng `courses` | `admin-actions.test.js`, E2E Flow 4 | `VERIFIED` |
| `course-service` | API nội bộ cung cấp thông tin khóa học (Feign) | — | `InternalCourseController.java` | `GET /internal/courses/{courseId}`, `GET /internal/courses/{courseId}/enrollments/{userId}`, `GET /internal/courses` | Bảng `courses` | Phase 1 Contract Tests (16/16 PASS) | `VERIFIED` |

---

## 4. Phân Hệ Bài Tập, Trắc Nghiệm & Chấm Code (Assignment Service)

| Module | Chức năng | Frontend file | Backend file | API | Database | Test | Trạng thái LMS-main |
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

| Module | Chức năng | Frontend file | Backend file | API | Database | Test | Trạng thái LMS-main |
|---|---|---|---|---|---|---|:---:|
| `blog-service` | Quản lý Thẻ bài viết (Tags CRUD) | `templates/admin/tags.html` | `TagController.java`, `TagServiceImpl.java` | `GET /tags`, `POST /tags`, `PUT /tags/{id}`, `DELETE /tags/{id}` | Bảng `tags` | `TagServiceImplTest.java`, E2E Flow 4 | `VERIFIED` |
| `blog-service` | Danh sách bài viết Blog & Lọc theo Tag/Tìm kiếm | `templates/blog/index.html` | `PostController.java`, `PostServiceImpl.java` | `GET /posts/public` (Pageable, tagId, keyword) | Bảng `posts`, `post_tags`, `tags` | `PostServiceImplTest.java`, E2E Flow 3 | `VERIFIED` |
| `blog-service` | Xem chi tiết bài viết Blog (Markdown Preview) | `templates/blog/detail.html` | `PostController.java`, `PostServiceImpl.java` | `GET /posts/public/{slug}`, `GET /posts/{id}` | Bảng `posts` (tăng `viewCount` atomic) | `BlogFunctionalTest.java`, E2E Flow 3 | `VERIFIED` |
| `blog-service` | Viết bài mới & Tải ảnh bìa (Thumbnail) | `templates/blog/create.html` | `PostController.java`, `PostServiceImpl.java`, `MinioStorageService.java` | `POST /posts` (multipart `CreatePostRequest`) | Bảng `posts`, `post_tags`, MinIO `lms-blog-bucket` | `PostServiceImplTest.java`, E2E Flow 3 | `VERIFIED` |
| `blog-service` | Chỉnh sửa bài viết cá nhân & Xóa bài | `templates/blog/edit.html` | `PostController.java`, `PostServiceImpl.java` | `PUT /posts/{id}`, `DELETE /posts/{id}` | Bảng `posts`, `post_tags` | `PostServiceImplTest.java`, `blog-interactions.test.js` | `VERIFIED` |
| `blog-service` | Quản lý danh sách bài viết cá nhân | `templates/blog/my-posts.html` | `PostController.java`, `PostServiceImpl.java` | `GET /posts/me` | Bảng `posts` | `PostServiceImplTest.java` | `VERIFIED` |
| `blog-service` | Bình luận bài viết & Trả lời bình luận (Nested) | `templates/blog/detail.html` | `CommentController.java`, `CommentServiceImpl.java` | `GET /posts/{pId}/comments`, `POST /posts/{pId}/comments`, `DELETE /posts/{pId}/comments/{cId}` | Bảng `comments` | `CommentServiceImplTest.java`, E2E Flow 3 | `VERIFIED` |
| `blog-service` | Bình chọn Upvote / Downvote bài viết & Comment | `templates/blog/detail.html` | `VoteController.java`, `VoteServiceImpl.java` | `POST /posts/{pId}/vote`, `POST /posts/{pId}/comments/{cId}/vote` | Bảng `votes` | `VoteServiceImplTest.java`, E2E Flow 3 | `VERIFIED` |
| `blog-service` | Quản trị bài viết Blog (Admin kiểm duyệt/ẩn) | `templates/admin/blogs.html` | `PostController.java`, `PostServiceImpl.java` | `GET /posts/admin` | Bảng `posts` | `BlogApiIntegrationTest.java`, `admin-actions.test.js` | `VERIFIED` |
| `blog-service` | API nội bộ cung cấp thông tin bài viết (Feign) | — | `InternalBlogController.java` | `GET /internal/posts/{postId}`, `GET /internal/posts/{postId}/exists` | Bảng `posts` | Phase 1 Contract Tests (16/16 PASS) | `VERIFIED` |

---

## 6. Phân Hệ Trợ Lý Ảo & Tự Sinh Nội Dung (Chatbot Service)

| Module | Chức năng | Frontend file | Backend file | API | Database | Test | Trạng thái LMS-main |
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

| Module | Chức năng | Frontend file | Backend file | API | Database | Test | Trạng thái LMS-main |
|---|---|---|---|---|---|---|:---:|
| `frontend` | Trang chủ hệ thống & Giới thiệu khóa học nổi bật | `templates/home.html` | `HomeController.java`, `CourseController.java` | `GET /` | Bảng `courses` | `HomeControllerTest.java` | `VERIFIED` |
| `frontend` | Dashboard Học viên (Tổng quan tiến độ & Điểm số) | `features/dashboard/pages/student/DashboardStudent.jsx` | `services/dashboard.api.js` | Tích hợp Identity + Course + Assignment | — | UI Smoke Test | `NOT_STARTED` |
| `frontend` | Dashboard Giảng viên (Khóa học, Thống kê sinh viên) | `features/dashboard/pages/teacher/HomePageInstructor.jsx` | `services/instructorDashboard.api.js` | Tích hợp Course + Assignment | — | UI Smoke Test | `NOT_STARTED` |
| `frontend` | Admin Service Health Monitor Dashboard | `features/dashboard/pages/admin/HomePageAdmin.jsx` | Actuator liveness endpoints | `GET /api/*/actuator/health/liveness` | — | UI Smoke Test | `NOT_STARTED` |
| `frontend` | Phân quyền truy cập & Bảo vệ định tuyến (RBAC) | `templates/admin/**` | `SecurityConfig.java`, `AuthFilter.java` | Kiểm tra JWT role ADMIN/INSTRUCTOR | Session / Cookie | E2E Flow 4 RBAC 403 test | `VERIFIED` |
| `frontend` | Xử lý lỗi toàn cục và trang báo lỗi (HTML & JSON) | `templates/error/*.html` | `GatewayExceptionHandler.java`, `GlobalExceptionHandler.java` | Các tuyến bắt lỗi 403, 404, 500, 503 | — | Phase 2 BffVerificationTest (MockMvc) | `VERIFIED` |

---

## 8. Bảng Tổng Hợp Trạng Thái Tiến Độ

| Phân hệ / Module | Tổng số chức năng | NOT_STARTED | IN_PROGRESS | IMPLEMENTED | TESTED | VERIFIED | % TESTED | % VERIFIED | % Đã kiểm thử (TESTED + VERIFIED) |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| Core & Infrastructure | 7 | 0 | 0 | 0 | 0 | 7 | 0,00% | 100,00% | 100,00% |
| Identity Service | 15 | 0 | 0 | 0 | 1 | 14 | 6,67% | 93,33% | 100,00% |
| Course Service | 15 | 0 | 0 | 0 | 0 | 15 | 0,00% | 100,00% | 100,00% |
| Assignment Service | 16 | 16 | 0 | 0 | 0 | 0 | 0,00% | 0,00% | 0,00% |
| Blog Service | 10 | 0 | 0 | 0 | 0 | 10 | 0,00% | 100,00% | 100,00% |
| Chatbot Service | 7 | 7 | 0 | 0 | 0 | 0 | 0,00% | 0,00% | 0,00% |
| Frontend & Dashboard | 6 | 3 | 0 | 0 | 0 | 3 | 0,00% | 50,00% | 50,00% |
| **TỔNG CỘNG** | **76** | **26** | **0** | **0** | **1** | **49** | **1,32%** | **64,47%** | **65,79%** |

> [!NOTE]
> **Giải thích tỷ lệ tiến độ chuẩn hóa**:
> - **Tỷ lệ TESTED**: **1 / 76 = 1,32%** (Duy nhất chức năng Google OAuth 2.0 cần tài khoản Google Cloud Console và redirect URI thật để nghiệm thu VERIFIED).
> - **Tỷ lệ VERIFIED**: **49 / 76 = 64,47%** (Toàn bộ 46/47 chức năng thuộc phạm vi Thành viên 2 đã đạt đủ 13/13 tiêu chí tại AGENTS.md qua kiểm thử E2E Playwright trên Docker stack thật, xác thực CSDL MySQL/MongoDB/MinIO và Zero-leak).
> - **Tổng số tính năng đã qua kiểm thử (TESTED + VERIFIED)**: **50 / 76 = 65,79%**.
> - **Số tính năng chưa bắt đầu (NOT_STARTED)**: **26 / 76 = 34,21%** (16 chức năng Assignment Service + 7 chức năng Chatbot Service + 3 chức năng Dashboard thuộc phân hệ của Thành viên 1).

*(Bảng này được cập nhật chuyển đổi trạng thái liên tục theo từng commit và kịch bản test thực tế)*.



### Bổ sung kiểm chứng UI Thymeleaf Phase 3 — 2026-10-25

Bốn lỗi modal không render, cây nhiều cấp, chèn HTML từ dữ liệu API và URL iframe video đã được sửa trong web-app. Kiểm chứng: 58 test Java và 8 test JavaScript trên HTML Thymeleaf/Bootstrap thật trong jsdom; JDK 21 build/package thành công. Chi tiết ở `progress/member-2.md` và `../backend/web-app/TESTING.md`.

Đây là bằng chứng kiểm thử cô lập của UI mới; không nâng bất kỳ mục nào lên VERIFIED và không tính lại tỷ lệ full-stack từ số lượng test. Phát media và tích hợp Docker thật vẫn cần nghiệm thu riêng.
