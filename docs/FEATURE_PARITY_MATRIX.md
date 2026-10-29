# Ma Trận Đối Chiếu Tính Năng — EduLearn LMS (FEATURE_PARITY_MATRIX.md)

Tài liệu này theo dõi mức độ tương đương giữa **Project Mẫu** và **EduLearn LMS** (`d:\LMS-main`).
Mọi chức năng của hệ thống được kiểm kê chi tiết theo từng module, file frontend, file backend, endpoint API, cơ sở dữ liệu và bài kiểm thử liên quan.

---

## Quy Định Trạng Thái (Status Legend)

Dự án chỉ sử dụng đúng 6 trạng thái kỹ thuật sau:
- `NOT_STARTED`: Chưa bắt đầu triển khai mã nguồn.
- `IN_PROGRESS`: Đang trong quá trình lập trình hoặc cấu hình.
- `BLOCKED`: Đang bị tắc nghẽn do phụ thuộc vào module/hạ tầng khác chưa hoàn thành.
- `IMPLEMENTED`: Đã viết xong mã nguồn nghiệp vụ nhưng chưa có kiểm thử đầy đủ.
- `TESTED`: Đã chạy xong unit/integration test cục bộ hoặc MockMvc nhưng chưa kiểm thử tích hợp toàn diện qua luồng E2E thực tế.
- `VERIFIED`: Đã được kiểm thử hành vi thực tế trên môi trường Docker/Full stack qua bộ kịch bản Playwright E2E theo route controller thật, đạt độ tương đương 100% so với project mẫu và thỏa mãn đủ 13 tiêu chí tại AGENTS.md.

---

## 1. Hạ Tầng & Dịch Vụ Cốt Lõi (Core Infrastructure & Gateway)

| Module | Chức năng | Frontend file | Backend file | API | Database | Test | Trạng thái LMS-main |
|---|---|---|---|---|---|---|:---:|
| `service-registry` | Eureka Service Discovery | — | `ServiceRegistryApplication.java` | `GET :8761/eureka/apps` | In-memory | `ServiceRegistryApplicationTests.java`, Docker live probe | `VERIFIED` |
| `api-gateway` | Định tuyến ngược (Dynamic Reverse Routing) | `services/api.client.js` | `ApiGatewayApplication.java`, `application.yaml` | `ALL :8080/api/**` | — | `ApiGatewayApplicationTests.java`, E2E Flows 1-4 | `VERIFIED` |
| `api-gateway` | Xác thực JWT & Inject Headers (`AuthFilter`) | `context/AuthContext.jsx` | `AuthFilter.java`, `JwtUtils.java` | Pre-filter trên routes private | — | `AuthFilterTest.java`, E2E Flows 1-4 | `VERIFIED` |
| `api-gateway` | Chống giả mạo danh tính (Header Stripping) | — | `application.yaml` (RemoveRequestHeader) | Public endpoints | — | Integration Test (Header strip test); live coverage mới một phần phạm vi dòng, chưa đủ nghiệm thu toàn bộ | `TESTED` |
| `api-gateway` | Khả năng chịu lỗi Circuit Breaker (Resilience4j) | `components/ui/Toast.jsx` | `application.yaml` (circuitbreaker) | Tất cả các tuyến microservices | — | Phase 2 Gateway Fallback Test; live coverage mới một phần phạm vi dòng, chưa đủ nghiệm thu toàn bộ | `TESTED` |
| `api-gateway` | Actuator Health Routing cho Admin Dashboard | `configurations/env.js` | `application.yaml` | `GET :8080/api/*/actuator/**` | — | Docker stack health probe; live coverage mới một phần phạm vi dòng, chưa đủ nghiệm thu toàn bộ | `TESTED` |
| `infrastructure` | Docker Compose Stack (Local & Prod) | `Dockerfile`, `nginx.conf` | 7 Dockerfiles backend | Localhost ports | MySQL, Mongo, Redis, MinIO | Docker smoke test, 11 containers healthy; live coverage mới một phần phạm vi dòng, chưa đủ nghiệm thu toàn bộ | `TESTED` |

---

## 2. Phân Hệ Xác Thực & Người Dùng (Identity Service)

| Module | Chức năng | Frontend file | Backend file | API | Database | Test | Trạng thái LMS-main |
|---|---|---|---|---|---|---|:---:|
| `identity-service` | Đăng ký tài khoản mới (Username/Password/Role) | `templates/auth/register.html` | `AuthenticationController.java`, `UserServiceImpl.java` | `POST /auth/register` | Bảng `users` | `UserServiceImplTest.java`, E2E Flow 1 | `VERIFIED` |
| `identity-service` | Đăng nhập cục bộ & cấp JWT (Access + Refresh) | `templates/auth/login.html` | `AuthenticationController.java`, `AuthenticationServiceImpl.java` | `POST /auth/login` | Bảng `users`, `refresh_tokens` | `AuthenticationServiceImplTest.java`, E2E Flows 1-4 | `VERIFIED` |
| `identity-service` | Đăng nhập Google OAuth 2.0 (SSO) | `features/auth/pages/SocialCallbackPage.jsx` | `AuthenticationController.java`, `GoogleAuthService.java` | `POST /auth/social-login/{provider}` | Bảng `users`, `refresh_tokens` | `AuthenticationServiceImplTest.java` (Cần Google Client ID/Secret thật từ Cloud Console để nghiệm thu VERIFIED) | `TESTED` |
| `identity-service` | Gửi & Xác thực mã OTP qua Email | `templates/auth/verify-email.html` | `EmailVerificationController.java`, `EmailVerificationServiceImpl.java` | `POST /auth/verify-email/send`, `POST /auth/verify-email` | Bảng `email_verifications` | `otp-countdown.test.js`, E2E Flow 1 (Mailpit API) | `VERIFIED` |
| `identity-service` | Làm mới Token (Refresh Token Rotation) | `services/api.client.js` (Interceptors) | `AuthenticationController.java`, `AuthenticationServiceImpl.java` | `POST /auth/refresh` | Bảng `refresh_tokens` | `AuthenticationServiceImplTest.java` (Unit Test backend) | `TESTED` |
| `identity-service` | Đăng xuất & Thu hồi Refresh Token | `layouts/StudentLayout/components/Header.jsx` | `AuthenticationController.java`, `AuthenticationServiceImpl.java` | `POST /auth/logout` | Bảng `refresh_tokens` | `AuthenticationServiceImplTest.java` (Unit Test backend) | `TESTED` |
| `identity-service` | Quên mật khẩu & Đặt lại mật khẩu | `templates/auth/forgot-password.html`, `templates/auth/reset-password.html` | `EmailVerificationController.java`, `EmailVerificationServiceImpl.java` | `POST /auth/forgot-password`, `POST /auth/reset-password` | Bảng `email_verifications`, `users` | `IdentityFunctionalTest.java`, `AuthControllerTest.java` (MockMvc) | `TESTED` |
| `identity-service` | Xem & Cập nhật Hồ sơ cá nhân | `templates/profile/index.html` | `UserController.java`, `UserServiceImpl.java` | `GET /users/myinfo`, `PUT /users/myinfo` | Bảng `users` | `browser-avatar-cors.e2e.test.js`; live coverage mới một phần phạm vi dòng, chưa đủ nghiệm thu toàn bộ | `TESTED` |
| `identity-service` | Tải lên ảnh đại diện cá nhân (Presigned URL) | `templates/profile/index.html` | `StorageController.java`, `StorageServiceImpl.java` | `POST /storage/presigned-url` | MinIO bucket `lms-identity-bucket` | `browser-avatar-cors.e2e.test.js` (Playwright upload MinIO) | `VERIFIED` |
| `identity-service` | Đổi mật khẩu tài khoản | `templates/profile/index.html` | `UserController.java`, `UserServiceImpl.java` | `PATCH /users/myinfo/password` | Bảng `users` | `UserServiceImplTest.java`, `ProfileControllerTest.java` (MockMvc) | `TESTED` |
| `identity-service` | Đổi vai trò tài khoản (Role Request) | `templates/profile/index.html` | `UserController.java`, `UserServiceImpl.java` | `PATCH /users/myinfo/role` | Bảng `users` | `UserServiceImplTest.java`, `ProfileControllerTest.java` (MockMvc) | `TESTED` |
| `identity-service` | Xem hồ sơ công khai của người dùng khác | `templates/blog/detail.html`, `templates/courses/student/detail.html` | `UserController.java`, `UserServiceImpl.java` | `GET /users/{userId}/profile` | Bảng `users` | `UserServiceImplTest.java`, `IdentityApiIntegrationTest.java` | `TESTED` |
| `identity-service` | Quản trị danh sách người dùng & Trạng thái | `templates/admin/users/list.html` | `AdminUserController.java`, `AdminUserServiceImpl.java` | `GET /admin/users`, `PATCH /admin/users/{id}/active` | Bảng `users` | E2E Flow 4 (Khóa tài khoản và chặn đăng nhập) | `VERIFIED` |
| `identity-service` | Quản trị viên tạo tài khoản Admin mới | `templates/admin/users/list.html` | `AdminUserController.java`, `AdminUserServiceImpl.java` | `POST /admin/users/admins` | Bảng `users` | E2E Flow 4 (Tạo Admin mới & Đăng nhập thành công) | `VERIFIED` |
| `identity-service` | API nội bộ cung cấp thông tin User cho service khác | — | `InternalUserController.java` | `GET /internal/user/{id}`, `POST /internal/users/batch`, `GET /internal/users/{userId}/exists` | Bảng `users` | Phase 1 Contract Tests (xem biên bản Phase 1; không phải E2E) | `TESTED` |

---

## 3. Phân Hệ Quản Lý Khóa Học & Nội Dung (Course Service)

| Module | Chức năng | Frontend file | Backend file | API | Database | Test | Trạng thái LMS-main |
|---|---|---|---|---|---|---|:---:|
| `course-service` | Quản lý danh mục khóa học (Category CRUD) | `templates/admin/categories/list.html` | `CategoryController.java`, `CategoryServiceImpl.java` | `GET /categories`, `POST /categories`, `PUT /categories/{id}`, `DELETE /categories/{id}` | Bảng `categories` | E2E Flow 4; live coverage mới một phần phạm vi dòng, chưa đủ nghiệm thu toàn bộ | `TESTED` |
| `course-service` | Danh sách khóa học công khai & Tìm kiếm/Lọc | `templates/courses/student/catalog.html` | `CourseController.java`, `CourseServiceImpl.java` | `GET /courses` (Pageable, status, categoryId, keyword, level) | Bảng `courses` | E2E Flow 1; live coverage mới một phần phạm vi dòng, chưa đủ nghiệm thu toàn bộ | `TESTED` |
| `course-service` | Chi tiết khóa học (Overview & Preview) | `templates/courses/student/detail.html` | `CourseController.java`, `CourseServiceImpl.java` | `GET /courses/{id}` | Bảng `courses` | E2E Flow 1 | `VERIFIED` |
| `course-service` | Giảng viên tạo mới khóa học | `templates/courses/instructor/course-form.html` | `CourseController.java`, `CourseServiceImpl.java` | `POST /courses` (multipart `CreateCourseRequest`) | Bảng `courses`, MongoDB `CourseStructure` | E2E Flow 2 | `VERIFIED` |
| `course-service` | Giảng viên chỉnh sửa khóa học & Đổi trạng thái | `templates/courses/instructor/course-form.html` | `CourseController.java`, `CourseServiceImpl.java` | `PUT /courses/{id}`, `PATCH /courses/{id}/lock`, `PATCH /courses/{id}/unlock` | Bảng `courses` | E2E Flow 2; live coverage mới một phần phạm vi dòng, chưa đủ nghiệm thu toàn bộ | `TESTED` |
| `course-service` | Quản lý cây bài học (Chapters/Lessons Structure) | `templates/courses/instructor/management-center.html` | `CourseStructureController.java`, `CourseStructureServiceImpl.java` | `GET /courses/{courseId}/structure`, `POST /structure/nodes`, `PUT /structure/reorder` | MongoDB `course_structures`, `lessons` | `curriculum-builder.test.js`, E2E Flow 2; live coverage mới một phần phạm vi dòng, chưa đủ nghiệm thu toàn bộ | `TESTED` |
| `course-service` | Quản lý nội dung bài học (Video & Document) | `templates/courses/student/learning-hub.html` | `LessonController.java`, `LessonServiceImpl.java` | `GET /courses/{cId}/lessons/{lId}`, `PUT /courses/{cId}/lessons/{lId}` | MongoDB `lessons` | E2E Flow 1-2; live coverage mới một phần phạm vi dòng, chưa đủ nghiệm thu toàn bộ | `TESTED` |
| `course-service` | Upload thumbnail khóa học & file bài giảng | `templates/courses/instructor/course-form.html` | `StorageController.java`, `StorageServiceImpl.java`, `MinioServiceImpl.java` | `POST /storage/presigned-url`, `POST /storage/multipart/initiate` | MinIO bucket `lms-course-bucket` | E2E Flow 2 (S3 multipart upload MinIO); live coverage mới một phần phạm vi dòng, chưa đủ nghiệm thu toàn bộ | `TESTED` |
| `course-service` | Học viên đăng ký ghi danh khóa học | `templates/courses/student/detail.html` | `CourseController.java`, `CourseServiceImpl.java` | `POST /courses/{id}/enroll` | Bảng `enrollments` | E2E Flow 1 | `VERIFIED` |
| `course-service` | Danh sách khóa học của tôi (Tạo & Ghi danh) | `templates/courses/student/my-courses.html` | `CourseController.java`, `CourseServiceImpl.java` | `GET /courses/my`, `GET /courses/enrolled` | Bảng `enrollments`, `courses` | E2E Flow 1 | `VERIFIED` |
| `course-service` | Đánh dấu bài học hoàn thành & Theo dõi tiến độ | `templates/courses/student/learning-hub.html` | `LessonProgressController.java`, `LessonProgressServiceImpl.java` | `POST /courses/{courseId}/lessons/{lessonId}/complete`, `GET /courses/{courseId}/progress` | Bảng `lesson_progress` | E2E Flow 1 (Tiến độ hoàn thành 100%) | `VERIFIED` |
| `course-service` | Quản lý lịch học trực tuyến & Google Meet | `templates/courses/student/detail.html`, `templates/courses/instructor/management-center.html` | `ScheduleController.java`, `ScheduleServiceImpl.java` | `GET /courses/{cId}/schedules`, `POST /schedules`, `PUT /schedules/{id}`, `DELETE /schedules/{id}` | Bảng `schedules` | `ScheduleServiceImplTest.java`, `learning-hub.test.js` | `TESTED` |
| `course-service` | Quản lý thông báo trong khóa học | `templates/courses/student/detail.html`, `templates/courses/instructor/management-center.html` | `AnnouncementController.java`, `AnnouncementServiceImpl.java` | `GET /courses/{cId}/announcements`, `POST /announcements`, `DELETE /announcements/{id}` | Bảng `announcements` | `AnnouncementServiceImplTest.java`, `learning-hub.test.js` | `TESTED` |
| `course-service` | Quản trị viên duyệt và quản lý toàn bộ khóa học | `templates/admin/courses/list.html` | `CourseController.java`, `CourseServiceImpl.java` | `GET /courses/my/stats`, `PATCH /courses/{id}/lock`, `PATCH /courses/{id}/unlock` | Bảng `courses` | E2E Flow 4 (Khóa khóa học và loại khỏi catalog) | `VERIFIED` |
| `course-service` | API nội bộ cung cấp thông tin khóa học (Feign) | — | `InternalCourseController.java` | `GET /internal/courses/{courseId}`, `GET /internal/courses/{courseId}/enrollments/{userId}`, `GET /internal/courses` | Bảng `courses` | Phase 1 Contract Tests (xem biên bản Phase 1; không phải E2E) | `TESTED` |

---

## 4. Phân Hệ Bài Tập, Trắc Nghiệm & Chấm Code (Assignment Service — Phụ trách: Thành viên 1)

> [!IMPORTANT]
> Toàn bộ phân hệ Assignment Service do **Thành viên 1 (Trí)** phụ trách theo phân công dự án. Thành viên 2 không tự ý triển khai hoặc đánh dấu hoàn tất trước khi có mã nguồn và kiểm chứng thực tế từ Thành viên 1.

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
| `blog-service` | Quản lý Thẻ bài viết (Tags CRUD) | `templates/admin/tags/list.html` | `TagController.java`, `TagServiceImpl.java` | `GET /tags`, `POST /tags`, `PUT /tags/{id}`, `DELETE /tags/{id}` | Bảng `tags` | E2E Flow 4; live coverage mới một phần phạm vi dòng, chưa đủ nghiệm thu toàn bộ | `TESTED` |
| `blog-service` | Danh sách bài viết Blog & Lọc theo Tag/Tìm kiếm | `templates/blog/index.html` | `PostController.java`, `PostServiceImpl.java` | `GET /posts/public` (Pageable, tagId, keyword) | Bảng `posts`, `post_tags`, `tags` | E2E Flow 3; live coverage mới một phần phạm vi dòng, chưa đủ nghiệm thu toàn bộ | `TESTED` |
| `blog-service` | Xem chi tiết bài viết Blog (Markdown Preview) | `templates/blog/detail.html` | `PostController.java`, `PostServiceImpl.java` | `GET /posts/public/{slug}`, `GET /posts/{id}` | Bảng `posts` (tăng `viewCount` atomic) | E2E Flow 3 | `VERIFIED` |
| `blog-service` | Viết bài mới & Tải ảnh bìa (Thumbnail) | `templates/blog/post-form.html` | `PostController.java`, `PostServiceImpl.java`, `MinioStorageService.java` | `POST /posts` (multipart `CreatePostRequest`) | Bảng `posts`, `post_tags`, MinIO `lms-blog-bucket` | E2E Flow 3 | `VERIFIED` |
| `blog-service` | Chỉnh sửa bài viết cá nhân & Xóa bài | `templates/blog/post-form.html` | `PostController.java`, `PostServiceImpl.java` | `PUT /posts/{id}`, `DELETE /posts/{id}` | Bảng `posts`, `post_tags` | `PostServiceImplTest.java`, `blog-interactions.test.js` | `TESTED` |
| `blog-service` | Quản lý danh sách bài viết cá nhân | `templates/blog/my-posts.html` | `PostController.java`, `PostServiceImpl.java` | `GET /posts/me` | Bảng `posts` | `PostServiceImplTest.java` (Unit Test) | `TESTED` |
| `blog-service` | Bình luận bài viết & Trả lời bình luận (Nested 2 cấp) | `templates/blog/detail.html` | `CommentController.java`, `CommentServiceImpl.java` | `GET /posts/{pId}/comments`, `POST /posts/{pId}/comments`, `DELETE /posts/{pId}/comments/{cId}` | Bảng `comments` | E2E Flow 3 (Bình luận gốc, reply cấp 2, từ chối cấp 3); live coverage mới một phần phạm vi dòng, chưa đủ nghiệm thu toàn bộ | `TESTED` |
| `blog-service` | Bình chọn Upvote / Downvote bài viết & Comment | `templates/blog/detail.html` | `VoteController.java`, `VoteServiceImpl.java` | `POST /posts/{pId}/vote`, `POST /posts/{pId}/comments/{cId}/vote` | Bảng `votes` | E2E Flow 3 (Tăng vote count atomic); live coverage mới một phần phạm vi dòng, chưa đủ nghiệm thu toàn bộ | `TESTED` |
| `blog-service` | Quản trị bài viết Blog (Admin kiểm duyệt/ẩn) | `templates/admin/blogs.html` | `PostController.java`, `PostServiceImpl.java` | `GET /posts/admin` | Bảng `posts` | `BlogApiIntegrationTest.java`, `admin-actions.test.js` | `TESTED` |
| `blog-service` | API nội bộ cung cấp thông tin bài viết (Feign) | — | `InternalBlogController.java` | `GET /internal/posts/{postId}`, `GET /internal/posts/{postId}/exists` | Bảng `posts` | Phase 1 Contract Tests (xem biên bản Phase 1; không phải E2E) | `TESTED` |

---

## 6. Phân Hệ Trợ Lý Ảo & Tự Sinh Nội Dung (Chatbot Service — Phụ trách: Thành viên 1)

> [!IMPORTANT]
> Toàn bộ phân hệ Chatbot Service do **Thành viên 1 (Trí)** phụ trách theo phân công dự án.

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
| `frontend` | Trang chủ hệ thống & Giới thiệu khóa học nổi bật | `templates/home.html` | `HomeController.java`, `CourseController.java` | `GET /` | Bảng `courses` | `HomeControllerTest.java` (MockMvc) | `TESTED` |
| `frontend` | Dashboard Học viên (Tổng quan tiến độ & Điểm số — Member 1) | `features/dashboard/pages/student/DashboardStudent.jsx` | `services/dashboard.api.js` | Tích hợp Identity + Course + Assignment | — | UI Smoke Test | `NOT_STARTED` |
| `frontend` | Dashboard Giảng viên (Khóa học, Thống kê sinh viên — Member 1) | `features/dashboard/pages/teacher/HomePageInstructor.jsx` | `services/instructorDashboard.api.js` | Tích hợp Course + Assignment | — | UI Smoke Test | `NOT_STARTED` |
| `frontend` | Admin Service Health Monitor Dashboard (Member 1) | `features/dashboard/pages/admin/HomePageAdmin.jsx` | Actuator liveness endpoints | `GET /api/*/actuator/health/liveness` | — | UI Smoke Test | `NOT_STARTED` |
| `frontend` | Phân quyền truy cập & Bảo vệ định tuyến (RBAC) | `templates/admin/**` | `SecurityConfig.java`, `AuthFilter.java` | Kiểm tra JWT role ADMIN/INSTRUCTOR | Session / Cookie | E2E Flow 4 RBAC 403 test | `VERIFIED` |
| `frontend` | Xử lý lỗi toàn cục và trang báo lỗi (HTML & JSON) | `templates/error/*.html` | `GatewayExceptionHandler.java`, `GlobalExceptionHandler.java` | Các tuyến bắt lỗi 403, 404, 500, 503 | — | Phase 2 BffVerificationTest (MockMvc) | `TESTED` |

---

## 8. Bảng Tổng Hợp Trạng Thái Tiến Độ

| Phân hệ / Module | Tổng số chức năng | NOT_STARTED | IN_PROGRESS | IMPLEMENTED | TESTED | VERIFIED | % TESTED | % VERIFIED | % Đã kiểm thử |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|
| Core & Infrastructure | 7 | 0 | 0 | 0 | 4 | 3 | 57.14% | 42.86% | 100.00% |
| Identity Service | 15 | 0 | 0 | 0 | 9 | 6 | 60.00% | 40.00% | 100.00% |
| Course Service | 15 | 0 | 0 | 0 | 9 | 6 | 60.00% | 40.00% | 100.00% |
| Assignment Service (Trí) | 16 | 16 | 0 | 0 | 0 | 0 | 0.00% | 0.00% | 0.00% |
| Blog Service | 10 | 0 | 0 | 0 | 8 | 2 | 80.00% | 20.00% | 100.00% |
| Chatbot Service (Trí) | 7 | 7 | 0 | 0 | 0 | 0 | 0.00% | 0.00% | 0.00% |
| Frontend & Dashboard | 6 | 3 | 0 | 0 | 2 | 1 | 33.33% | 16.67% | 50.00% |
| **TỔNG CỘNG** | **76** | **26** | **0** | **0** | **32** | **18** | 42.11% | 23.68% | 65.79% |

> Đính chính 2026-10-29: hạ các dòng có phạm vi rộng hơn bằng chứng E2E xuống TESTED (ví dụ CRUD mới kiểm chứng tạo, vote mới kiểm chứng upvote bài viết, upload file bài giảng chưa có E2E). Các trạng thái VERIFIED còn lại được giữ theo hồ sơ trước đó; lượt Phase 5 này không chứng nhận lại toàn bộ 13 tiêu chí cho từng chức năng.
> 26 mục của Thành viên 1 giữ trạng thái trên nhánh hiện tại; không suy luận rằng Trí chưa làm trên nhánh riêng.
> Xem biên bản `docs/progress/PHASE5_ACCEPTANCE_2026-10-29.md` cho kết quả chạy thực tế và giới hạn nghiệm thu.
