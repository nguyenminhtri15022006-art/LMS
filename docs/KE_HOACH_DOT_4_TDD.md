# Kế Hoạch Triển Khai Đợt 4 — Tích Hợp Đa Dịch Vụ, Chuẩn Bị Hạ Tầng & Kiểm Thử Toàn Trình (E2E) Theo TDD

- **Dự án**: EduLearn LMS (`LMS-main`)
- **Người thực hiện**: Thành viên 2 (`NVCuong3112`)
- **Nhánh làm việc chuyên biệt**: `feature/member-2-dot-4-integration`
- **Phương pháp chủ đạo**: **TDD (Test-Driven Development)** và **Test Isolation** áp dụng cho Contract Test, Resiliency Test, Docker Smoke và Toàn trình (End-to-End).
- **Tiêu chuẩn chất lượng**: Đánh giá trung thực từng tính năng theo **13 tiêu chí nghiệm thu nghiêm ngặt** quy định tại [AGENTS.md](file:///D:/LMS-main/AGENTS.md), không tự động tick `VERIFIED` trong [docs/FEATURE_PARITY_MATRIX.md](file:///D:/LMS-main/docs/FEATURE_PARITY_MATRIX.md) khi chưa đủ bằng chứng thực thi.
- **Ràng buộc lần này**: **Chỉ khảo sát chỉ đọc và cập nhật tài liệu kế hoạch**. Không viết code, không triển khai phase, không thay đổi hạ tầng, không commit/push/merge/tag. Giữ nguyên mọi thay đổi ngoài phạm vi.

---

## I. Bước 0 — Đối Chiếu Thực Tế & Khảo Sát Mã Nguồn (Code Evidence)

### 1. Nguồn tài liệu quy định và tham chiếu đối chiếu
- [AGENTS.md](file:///D:/LMS-main/AGENTS.md): Phân vùng quyền hạn (READ-ONLY với `Learning-Management-System-main/`, READ-WRITE với `D:\LMS-main`), 13 tiêu chí nghiệm thu VERIFIED, cấm giả lập dữ liệu (mocking) để báo hoàn thành.
- [docs/TEAM_ROLES.md](file:///D:/LMS-main/docs/TEAM_ROLES.md): Phân định trách nhiệm — Member 2 phụ trách `identity-service`, `course-service`, `blog-service`, `web-app` BFF, và phần dùng chung `api-gateway`, `service-registry`. Member 1 (Trí) phụ trách `assignment-service`, `chatbot-service`.
- [docs/GIT_WORKFLOW.md](file:///D:/LMS-main/docs/GIT_WORKFLOW.md): Quy trình phân nhánh, commit Conventional Commits tiếng Việt có dấu, cấm force-push, cấm commit mã nguồn khi test còn fail.
- [docs/CODING_RULES.md](file:///D:/LMS-main/docs/CODING_RULES.md): Chuẩn kiến trúc Clean Architecture, DTO validation, xử lý exception tập trung.
- [docs/API_CONTRACT.md](file:///D:/LMS-main/docs/API_CONTRACT.md): Hợp đồng API công khai và nội bộ.
- [docs/FEATURE_PARITY_MATRIX.md](file:///D:/LMS-main/docs/FEATURE_PARITY_MATRIX.md): Ma trận đối chiếu tính năng giữa dự án mẫu và `LMS-main`.
- [docs/progress/member-2.md](file:///D:/LMS-main/docs/progress/member-2.md): Nhật ký tiến độ thực tế đã ghi nhận.

### 2. Mốc tham chiếu số liệu kiểm thử Phase 7 (Báo cáo lịch sử)
Theo báo cáo nghiệm thu Phase 7 được ghi nhận tại [docs/progress/member-2.md](file:///D:/LMS-main/docs/progress/member-2.md#L74-L81) và [docs/DOCKER_SMOKE_TESTS.md](file:///D:/LMS-main/docs/DOCKER_SMOKE_TESTS.md):
- **Java Test Cases**: **284 test cases** trong một lần chạy Maven reactor (`mvn test -f backend/pom.xml`).
- **JavaScript Test Cases**: **53 test cases** của `web-app` (logic countdown OTP, form validation, DOM assertions).
- **Docker HTTP Smoke Test Cases**: **4 test cases** (Actuator health, Login JWT, Profile avatar presigned URL, Public courses).
- **Browser Playwright E2E Test Cases**: **3 test cases** (kiểm thử trình duyệt Chromium với direct upload MinIO CORS).

*(Ghi chú bắt buộc: Đây là mốc tham chiếu kết quả lịch sử đã được báo cáo nghiệm thu tại Phase 7; tài liệu Đợt 4 không tuyên bố đã chạy lại toàn bộ mốc này trong bước chỉnh sửa tài liệu).*

### 3. Bảng bằng chứng đối chiếu phân hệ, cổng và tuyến bảo mật thực tế

| Phân hệ / Dịch vụ | Cổng Container & Host Thực Tế | Tuyến Gateway & Bộ Lọc | Whitelist / Security Trong Service | Bằng chứng File / Class / Method | Trạng thái Thực Tế Tại LMS-main |
|---|:---:|---|---|---|:---:|
| `service-registry` | `8761:8761` | Không qua Gateway | Cho phép kết nối đăng ký service nội bộ | [ServiceRegistryApplication.java](file:///D:/LMS-main/backend/service-registry/src/main/java/com/lms/serviceregistry/ServiceRegistryApplication.java) | `VERIFIED` (Đang chạy healthy) |
| `api-gateway` | `8080:8080` / `/api` | Định tuyến ngược, `AuthFilter` (inject `X-User-*`), CircuitBreaker Resilience4j | Không có route riêng cho `/internal/**` | [application.yaml](file:///D:/LMS-main/backend/api-gateway/src/main/resources/application.yaml), [AuthFilter.java:54](file:///D:/LMS-main/backend/api-gateway/src/main/java/com/lms/apigateway/configuration/AuthFilter.java#L54) | `TESTED` (**Chưa có GatewayFallbackController.java**) |
| `identity-service` | `8081:8081` / `/identity` | Whitelist `/auth/**`, Private `/users/**`, `/admin/**` | Whitelist `"/internal/**"` (`SecurityConfig:56`) | [InternalUserController.java](file:///D:/LMS-main/backend/identity-service/src/main/java/com/lms/identityservice/controller/InternalUserController.java), [SecurityConfig.java:56](file:///D:/LMS-main/backend/identity-service/src/main/java/com/lms/identityservice/configuration/SecurityConfig.java#L56) | `TESTED` (Đang chạy healthy) |
| `course-service` | `8082:8082` / `/course` | Whitelist `/courses` (GET public), Private `/courses/**` | Whitelist `"/internal/**"` (`SecurityConfig:43`) | [InternalCourseController.java](file:///D:/LMS-main/backend/course-service/src/main/java/com/lms/courseservice/controller/InternalCourseController.java), [SecurityConfig.java:43](file:///D:/LMS-main/backend/course-service/src/main/java/com/lms/courseservice/configuration/SecurityConfig.java#L43) | `TESTED` (Đang chạy healthy) |
| `blog-service` | `8084:8084` / `/blog` | Whitelist `/posts/public/**`, Private `/posts/**` | Whitelist `"/internal/**"` (`SecurityConfig:43`) | [InternalBlogController.java](file:///D:/LMS-main/backend/blog-service/src/main/java/com/lms/blogservice/controller/InternalBlogController.java), [SecurityConfig.java:43](file:///D:/LMS-main/backend/blog-service/src/main/java/com/lms/blogservice/configuration/SecurityConfig.java#L43) | `TESTED` (Đang chạy healthy) |
| `web-app` (BFF) | Container `8088`, Host `8088` (`8088:8088`) | Giao diện người dùng Thymeleaf MVC, gọi API qua Gateway :8080 | Session cookie, CSRF token, Spring Security Form Login | [application.yml:2](file:///D:/LMS-main/backend/web-app/src/main/resources/application.yml#L2) (`server.port: 8088`), [docker-compose.yml:318](file:///D:/LMS-main/docker-compose.yml#L318), [GatewayClient.java](file:///D:/LMS-main/backend/web-app/src/main/java/com/lms/webapp/client/GatewayClient.java), [AuthController.java:56](file:///D:/LMS-main/backend/web-app/src/main/java/com/lms/webapp/controller/AuthController.java#L56) | `TESTED` (Đang chạy healthy trên cổng 8088) |
| `mysql` | Container `3306`, Host mặc định `3307` (`${MYSQL_PORT:-3307}:3306`) | CSDL quan hệ nội bộ cho Identity, Course, Blog | Tài khoản root / mật khẩu môi trường | [docker-compose.yml:21](file:///D:/LMS-main/docker-compose.yml#L21) (`${MYSQL_PORT:-3307}:3306`) | `VERIFIED` (Đang chạy healthy) |
| `assignment-service` *(Trí)* | Container `8083:8083` / `/assignment` | Định tuyến qua Gateway `id: assignment-service` | Whitelist `/internal/**` trong sample | [InternalController.java (sample)](file:///D:/LMS-main/Learning-Management-System-main/backend/assignment-service/src/main/java/com/lms/assignmentservice/controller/InternalController.java) | `NOT_STARTED` trong `D:\LMS-main` *(Phụ thuộc Trí)* |
| `chatbot-service` *(Trí)* | Container `8086:8086` / `/chatbot` | Định tuyến qua Gateway `id: chatbot-service` | Whitelist `/internal/**` trong sample | [InternalController.java (sample)](file:///D:/LMS-main/Learning-Management-System-main/backend/chatbot-service/src/main/java/com/lms/chatbotservice/controller/InternalController.java) | `NOT_STARTED` trong `D:\LMS-main` *(Phụ thuộc Trí)* |

---

## II. Kiểm Kê Thực Tế & Thống Nhất Số Lượng Internal APIs

### 1. Phân loại 16 Internal Endpoints thực tế
- **Tổng số endpoint nội bộ có sẵn trong controller của Member 2**: **14 endpoints** (4 Identity + 8 Course + 2 Blog).
- **Số endpoint đã xác nhận có Consumer thực tế đang gọi**: **8 endpoints** (5 endpoints được `assignment-service` và `chatbot-service` của Trí gọi + 3 endpoints được nội bộ `course-service` và `blog-service` gọi).
- **Số endpoint có sẵn trong code Member 2 nhưng chưa xác nhận nhu cầu từ Trí**: **6 endpoints** (1 Identity + 3 Course + 2 Blog).
- **Số endpoint do Trí cung cấp mà Member 2 tiêu thụ**: **2 endpoints** (`course-service` gọi `assignment-service` qua `AssignmentClient`).

### 2. Bảng kiểm kê chi tiết từng Internal Endpoint

| STT | Service Cung Cấp | HTTP Method & Đường Dẫn (Path) | Request / Param DTO | Response Model | Quyền Truy Cập (Auth) | Service Tiêu Thụ Thực Tế (Consumer) | Vị Trí Mã Nguồn Cung Cấp | Trạng Thái Hiện Tại |
|:---:|---|---|---|---|---|---|---|:---:|
| **1** | `identity-service` | `GET /internal/user/{id}` | Path: `id` (String) | `UserFullResponse` (Direct DTO) | Whitelist (`SecurityConfig:56`) | `course-service` ([IdentityClient:22](file:///D:/LMS-main/backend/course-service/src/main/java/com/lms/courseservice/configuration/IdentityClient.java#L22)), `assignment-service` ([IdentityClient:26](file:///D:/LMS-main/Learning-Management-System-main/backend/assignment-service/src/main/java/com/lms/assignmentservice/repository/httpclient/IdentityClient.java#L26)) | [InternalUserController.java:23](file:///D:/LMS-main/backend/identity-service/src/main/java/com/lms/identityservice/controller/InternalUserController.java#L23) | **Đã có sẵn — Đã xác nhận consumer** |
| **2** | `identity-service` | `POST /internal/users/batch` | Body: `Set<String>` userIds | `List<UserFullResponse>` (Direct DTO) | Whitelist (`SecurityConfig:56`) | `assignment-service` ([IdentityClient:30](file:///D:/LMS-main/Learning-Management-System-main/backend/assignment-service/src/main/java/com/lms/assignmentservice/repository/httpclient/IdentityClient.java#L30) — Leaderboard) | [InternalUserController.java:28](file:///D:/LMS-main/backend/identity-service/src/main/java/com/lms/identityservice/controller/InternalUserController.java#L28) | **Đã có sẵn — Đã xác nhận consumer** |
| **3** | `identity-service` | `GET /internal/users/{userId}/exists` | Path: `userId` (String) | `boolean` (Primitive) | Whitelist (`SecurityConfig:56`) | `course-service` ([IdentityClient:19](file:///D:/LMS-main/backend/course-service/src/main/java/com/lms/courseservice/configuration/IdentityClient.java#L19)), `blog-service` ([IdentityClient:17](file:///D:/LMS-main/backend/blog-service/src/main/java/com/lms/blogservice/configuration/IdentityClient.java#L17)) | [InternalUserController.java:33](file:///D:/LMS-main/backend/identity-service/src/main/java/com/lms/identityservice/controller/InternalUserController.java#L33) | **Đã có sẵn — Đã xác nhận consumer** |
| **4** | `identity-service` | `GET /internal/users/search/by-email` | Query: `email` (String) | `UserFullResponse` (Direct DTO) | Whitelist (`SecurityConfig:56`) | `course-service` ([IdentityClient:25](file:///D:/LMS-main/backend/course-service/src/main/java/com/lms/courseservice/configuration/IdentityClient.java#L25)). Trí chưa gọi | [InternalUserController.java:38](file:///D:/LMS-main/backend/identity-service/src/main/java/com/lms/identityservice/controller/InternalUserController.java#L38) | **Đã có sẵn — Chưa xác nhận nhu cầu từ Trí** |
| **5** | `course-service` | `GET /internal/courses/{courseId}` | Path: `courseId` (String) | `InternalCourseResponse` (Direct DTO) | Whitelist (`SecurityConfig:43`) | `assignment-service` ([CourseClient:20](file:///D:/LMS-main/Learning-Management-System-main/backend/assignment-service/src/main/java/com/lms/assignmentservice/repository/httpclient/CourseClient.java#L20) — Validate khóa học) | [InternalCourseController.java:23](file:///D:/LMS-main/backend/course-service/src/main/java/com/lms/courseservice/controller/InternalCourseController.java#L23) | **Đã có sẵn — Đã xác nhận consumer** |
| **6** | `course-service` | `GET /internal/courses/{courseId}/lessons/{lessonId}` | Path: `courseId`, `lessonId` (String) | `InternalLessonResponse` (Direct DTO) | Whitelist (`SecurityConfig:43`) | Trí chưa khai báo trong `CourseClient` | [InternalCourseController.java:28](file:///D:/LMS-main/backend/course-service/src/main/java/com/lms/courseservice/controller/InternalCourseController.java#L28) | **Đã có sẵn — Chưa xác nhận nhu cầu từ Trí** |
| **7** | `course-service` | `GET /internal/courses/{courseId}/enrollments/{userId}` | Path: `courseId`, `userId` (String) | `Boolean` (Wrapper) | Whitelist (`SecurityConfig:43`) | `assignment-service` ([CourseClient:27](file:///D:/LMS-main/Learning-Management-System-main/backend/assignment-service/src/main/java/com/lms/assignmentservice/repository/httpclient/CourseClient.java#L27) — Kiểm tra quyền nộp bài) | [InternalCourseController.java:33](file:///D:/LMS-main/backend/course-service/src/main/java/com/lms/courseservice/controller/InternalCourseController.java#L33) | **Đã có sẵn — Đã xác nhận consumer** |
| **8** | `course-service` | `GET /internal/courses/{courseId}/students/count` | Path: `courseId` (String) | `Long` (Wrapper) | Whitelist (`SecurityConfig:43`) | `assignment-service` ([CourseClient:31](file:///D:/LMS-main/Learning-Management-System-main/backend/assignment-service/src/main/java/com/lms/assignmentservice/repository/httpclient/CourseClient.java#L31) — Thống kê) | [InternalCourseController.java:38](file:///D:/LMS-main/backend/course-service/src/main/java/com/lms/courseservice/controller/InternalCourseController.java#L38) | **Đã có sẵn — Đã xác nhận consumer** |
| **9** | `course-service` | `GET /internal/courses/{courseId}/exists` | Path: `courseId` (String) | `Boolean` (Wrapper) | Whitelist (`SecurityConfig:43`) | Trí dùng `getCourse` bắt 404 thay vì gọi API này | [InternalCourseController.java:43](file:///D:/LMS-main/backend/course-service/src/main/java/com/lms/courseservice/controller/InternalCourseController.java#L43) | **Đã có sẵn — Chưa xác nhận nhu cầu từ Trí** |
| **10** | `course-service` | `GET /internal/courses/{courseId}/lessons/{lessonId}/exists` | Path: `courseId`, `lessonId` (String) | `Boolean` (Wrapper) | Whitelist (`SecurityConfig:43`) | Trí chưa khai báo gọi | [InternalCourseController.java:48](file:///D:/LMS-main/backend/course-service/src/main/java/com/lms/courseservice/controller/InternalCourseController.java#L48) | **Đã có sẵn — Chưa xác nhận nhu cầu từ Trí** |
| **11** | `course-service` | `GET /internal/courses` | Không có | `ApiResponse<List<ChatbotCourseResponse>>` | Whitelist (`SecurityConfig:43`) | `chatbot-service` ([CourseClient:23](file:///D:/LMS-main/Learning-Management-System-main/backend/chatbot-service/src/main/java/com/lms/chatbotservice/feign/CourseClient.java#L23) — RAG Context) | [InternalCourseController.java:53](file:///D:/LMS-main/backend/course-service/src/main/java/com/lms/courseservice/controller/InternalCourseController.java#L53) | **Đã có sẵn — Đã xác nhận consumer** |
| **12** | `course-service` | `GET /internal/courses/enrolled` | Query: `userId` (String) | `ApiResponse<List<ChatbotCourseResponse>>` | Whitelist (`SecurityConfig:43`) | `chatbot-service` ([CourseClient:19](file:///D:/LMS-main/Learning-Management-System-main/backend/chatbot-service/src/main/java/com/lms/chatbotservice/feign/CourseClient.java#L19) — RAG Context) | [InternalCourseController.java:60](file:///D:/LMS-main/backend/course-service/src/main/java/com/lms/courseservice/controller/InternalCourseController.java#L60) | **Đã có sẵn — Đã xác nhận consumer** |
| **13** | `blog-service` | `GET /internal/posts/{postId}` | Path: `postId` (Long) | `ApiResponse<InternalPostResponse>` | Whitelist (`SecurityConfig:43`) | Chưa có Feign Client nào trong codebase của Trí gọi | [InternalBlogController.java:22](file:///D:/LMS-main/backend/blog-service/src/main/java/com/lms/blogservice/controller/InternalBlogController.java#L22) | **Đã có sẵn — Chưa xác nhận nhu cầu từ Trí** |
| **14** | `blog-service` | `GET /internal/posts/{postId}/exists` | Path: `postId` (Long) | `ApiResponse<Boolean>` | Whitelist (`SecurityConfig:43`) | Chưa có Feign Client nào trong codebase của Trí gọi | [InternalBlogController.java:30](file:///D:/LMS-main/backend/blog-service/src/main/java/com/lms/blogservice/controller/InternalBlogController.java#L30) | **Đã có sẵn — Chưa xác nhận nhu cầu từ Trí** |
| **15** | `assignment-service` *(Trí)* | `GET /internal/courses/{courseId}/exercise-count` | Path: `courseId` (String) | `ApiResponse<Long>` | Whitelist internal | `course-service` ([AssignmentClient:21](file:///D:/LMS-main/backend/course-service/src/main/java/com/lms/courseservice/configuration/AssignmentClient.java#L21)) | [InternalController.java:35 (sample)](file:///D:/LMS-main/Learning-Management-System-main/backend/assignment-service/src/main/java/com/lms/assignmentservice/controller/InternalController.java#L35) | **Cần kiểm chứng Fallback (Trí chưa chạy)** |
| **16** | `assignment-service` *(Trí)* | `POST /internal/courses/exercise-counts` | Body: `List<String>` courseIds | `ApiResponse<Map<String, Long>>` | Whitelist internal | `course-service` ([AssignmentClient:24](file:///D:/LMS-main/backend/course-service/src/main/java/com/lms/courseservice/configuration/AssignmentClient.java#L24)) | [InternalController.java:42 (sample)](file:///D:/LMS-main/Learning-Management-System-main/backend/assignment-service/src/main/java/com/lms/assignmentservice/controller/InternalController.java#L42) | **Cần kiểm chứng Fallback (Trí chưa chạy)** |

---

## III. Phân Tích & Làm Rõ Bảo Mật API Nội Bộ (Service-to-Service Security)

### 1. Cơ chế định tuyến và luồng gọi thực tế
- **Feign gọi trực tiếp service, KHÔNG qua API Gateway**:
  * Tại [course-service/src/main/resources/application.yaml:51-54](file:///D:/LMS-main/backend/course-service/src/main/resources/application.yaml#L51-L54) và [blog-service/src/main/resources/application.yaml:60](file:///D:/LMS-main/backend/blog-service/src/main/resources/application.yaml#L60), URL của Feign Client trỏ trực tiếp vào hostname/cổng của service đích (ví dụ `${IDENTITY_SERVICE_URL:http://localhost:8081/identity}`).
  * Tại [api-gateway/src/main/resources/application.yaml](file:///D:/LMS-main/backend/api-gateway/src/main/resources/application.yaml), không có route riêng cho `/internal/**`.

### 2. Phân tích rủi ro khi có Route Rộng (Wildcard Routes) tại Gateway
- **Rà soát tuyến wildcard**: Gateway có cấu hình route rộng:
  * `id: course-service`: `Path=${app.prefix-api}/course/**` với filter `StripPrefix=1` và `AuthFilter`.
  * `id: blog-service`: `Path=${app.prefix-api}/blog/**` với filter `StripPrefix=1` và `AuthFilter`.
- **Hành vi chuyển tiếp khi gọi đường dẫn có tiền tố service**:
  * Khi client gọi `GET /api/course/internal/courses` hoặc `GET /api/blog/internal/posts/1`:
    1. Gateway match predicate `/api/course/**`.
    2. Filter `StripPrefix=1` loại bỏ `/api`, chuyển đường dẫn thành `/course/internal/courses`.
    3. `AuthFilter` kiểm tra JWT: Nếu không có JWT (anonymous), trả về HTTP 401 Unauthorized.
    4. **Nếu client gửi kèm JWT hợp lệ của một người dùng thông thường (ROLE_STUDENT)**: `AuthFilter` inject các headers `X-User-*` và forward request xuống downstream `course-service`.
    5. Tại `course-service`: `context-path` là `/course`, controller match `/internal/courses`. Vì `SecurityConfig.java:43` đang khai báo `permitAll("/internal/**")`, request từ người dùng thông thường này **có nguy cơ lọt qua và được xử lý thành công**!
  * **Kết luận**: Khẳng định "Gateway không route internal, mọi request đều nhận 404" là **chưa chính xác**. Đường dẫn dạng `/internal/**` không có prefix service sẽ nhận 404, nhưng đường dẫn dạng `/api/course/internal/**` có thể bị route wildcard chuyển tiếp xuống downstream.

### 3. Kịch bản kiểm thử bảo mật API nội bộ dự kiến (Security Test Matrix)

| STT | Loại Yêu Cầu (Caller Type) | Đường Dẫn Thử Nghiệm (Path) | Headers Kèm Theo | Trạng Thái Mong Đợi Theo Chính Sách An Toàn | Kết Quả Runtime Thực Tế |
|:---:|---|---|---|:---:|:---:|
| 1 | Anonymous (Chưa đăng nhập) | `GET :8080/internal/user/1` | Không | **HTTP 404 Not Found** (Gateway không match route) | Cần kiểm chứng tại Phase 1 |
| 2 | Anonymous (Chưa đăng nhập) | `GET :8080/api/course/internal/courses` | Không | **HTTP 401 Unauthorized** (Bị `AuthFilter` Gateway chặn) | Cần kiểm chứng tại Phase 1 |
| 3 | User thường (`ROLE_STUDENT`) | `GET :8080/api/course/internal/courses` | Bearer Token hợp lệ | **Phải bị từ chối: HTTP 403 Forbidden hoặc 404** (Cần chính sách chặn gọi endpoint internal từ client) | Cần kiểm chứng tại Phase 1 |
| 4 | Lời gọi nội bộ hợp lệ (Feign) | `GET :8081/identity/internal/user/{id}` | Internal Direct Call | **HTTP 200 OK** (Dữ liệu trả về đúng schema DTO) | Cần kiểm chứng tại Phase 1 |

### 4. Đề xuất phương án bảo mật nội bộ & Mục CẦN XÁC NHẬN
- **Network Isolation**: Chuyển các container nội bộ (`identity`, `course`, `blog`, `assignment`, `chatbot`, MySQL, MongoDB, Redis) sang `expose:` thay vì `ports:` trong `docker-compose.yml`, chỉ duy nhất `api-gateway` (:8080) và `web-app` (:8088) mở cổng ra host. *(Lưu ý: Network isolation chỉ hạn chế truy cập mạng vật lý, không thay thế xác thực danh tính giữa các dịch vụ)*.
- **Service Authentication**: Đề xuất bổ sung xác thực giữa các service (ví dụ Header bí mật `X-Internal-Secret: ${INTERNAL_SECRET}`).
- **CẦN XÁC NHẬN**: Thống nhất với Thành viên 1 phương án bảo mật liên dịch vụ chính thức; tuyệt đối **không mở whitelist `/internal/**` tại API Gateway** chỉ để phục vụ kiểm thử.

---

## IV. Tách Biệt Phụ Thuộc Vào Thành Viên 1 (Trí)

Theo quy định tại [docs/TEAM_ROLES.md](file:///D:/LMS-main/docs/TEAM_ROLES.md) và [AGENTS.md](file:///D:/LMS-main/AGENTS.md):

```mermaid
flowchart TD
    subgraph M2_Scope ["Phạm Vi Độc Lập — Thành Viên 2 (Tự Thực Hiện 100%)"]
        P_ID["identity-service (Cung cấp 4 Internal APIs)"]
        P_CS["course-service (Cung cấp 8 Internal APIs)"]
        P_BS["blog-service (Cung cấp 2 Internal APIs)"]
        P_GW["api-gateway (Resilience4j & Fallback Controller)"]
        P_WA["web-app BFF (Giao diện Thymeleaf, 4 Luồng E2E)"]
        P_FB["AssignmentClient Fallback (Trả về 0L khi Trí vắng mặt)"]
    end

    subgraph M1_Scope ["Phạm Vi Phụ Thuộc — Thành Viên 1 (Trí)"]
        S_AS["assignment-service (Quiz, Code Judge0, Leaderboard)"]
        S_CB["chatbot-service (Trợ lý AI LLM Streaming)"]
        S_CONS["Consumer Feign Calls thực tế từ Quiz/Chatbot"]
    end

    M2_Scope -.->|"Provider Contract Test (MockMvc/Isolated H2)"| M2_Scope
    S_CONS -->|"Tích hợp thật (Khi Trí bàn giao container)"| M2_Scope
```

### Nguyên tắc chất lượng liên nhóm:
1. **Phần Thành viên 2 tự chủ thực hiện**:
   - 14 Provider Contract Tests độc lập cho các endpoint nội bộ của Member 2.
   - Kiểm chứng Fallback an toàn của `AssignmentClient` trong `course-service` (trả về 0L hoặc empty map).
   - Phát triển `GatewayFallbackController` và kiểm thử Circuit Breaker Resilience4j.
   - 4 Luồng E2E độc lập của các phân hệ Học viên, Giảng viên, Blog, Admin (theo route controller thật).
2. **Phần phụ thuộc Thành viên 1**:
   - Mã nguồn thực tế của `assignment-service` và `chatbot-service`.
   - Tích hợp thật có nộp bài code (Judge0), làm quiz trắc nghiệm và trợ lý AI chatbot.
3. **Quy tắc nghiệm thu**:
   - **Mock không chứng minh tích hợp thật đã PASS**: Provider test chỉ chứng minh Member 2 sẵn sàng. Tích hợp thật chỉ nghiệm thu khi container của Trí chạy thật.
   - **Không sửa code thuộc Trí**: Bất kỳ sai khác hợp đồng nào đều ghi vào mục phối hợp liên nhóm.

---

## V. Chuẩn Hóa Phương Pháp TDD & Test Isolation

### 1. Chu trình TDD theo từng loại kịch bản

| Loại Kịch Bản | Bước Red 1 | Bước Red 2 | Bước Green |
|---|---|---|---|
| **Class / Method Mới** (Ví dụ: `GatewayFallbackController`) | Viết test `GatewayFallbackControllerTest`. Test báo **Lỗi Compile** hoặc **404 Not Found** do chưa tồn tại class/controller. | Tạo stub class và method nhưng trả về empty/chưa đúng body. Test chạy được nhưng **Assertion Fail** (so sánh status 503, JSON body code 1010). | Viết logic tối thiểu (trả về `ResponseEntity<Map<String, Object>>` đúng format). Test báo **PASS 100%**. |
| **Sửa Lỗi / Hành Vi Thiếu Sót Hiện Hữu** | Viết **Regression Test** tái hiện lỗi chính xác. Test phải FAIL rõ ràng. | Không áp dụng (chỉ có Red regression). | Sửa mã nguồn nghiệp vụ để khắc phục đúng nguyên nhân gốc rễ. Regression test chuyển sang **PASS 100%**. |
| **Chức Năng Hiện Hữu Đã Đúng Sẵn** (Ví dụ: 14 internal endpoints) | Viết test kiểm chứng hợp đồng (Contract Test). Test **PASS ngay từ lần chạy đầu tiên**. | Ghi nhận trung thực: **Kiểm chứng hành vi hiện hữu (Baseline Verification)**. | **Tuyệt đối không cố tình sửa sai code** để giả lập Red; không bịa đặt bằng chứng TDD. |

### 2. Quy tắc cô lập kiểm thử (Test Isolation)
- **Chứng minh `mvn test` chạy độc lập khi Docker tắt hoàn toàn**:
  * Không tự ý tắt stack Docker trong lần sửa tài liệu này để tránh gián đoạn dịch vụ đang chạy.
  * Tính độc lập được chứng minh bằng cấu hình mã nguồn trong [application-test.yaml](file:///D:/LMS-main/backend/identity-service/src/test/resources/application-test.yaml):
    - `spring.datasource.url: jdbc:h2:mem:...` (chạy hoàn toàn trên RAM JVM, không kết nối port MySQL 3306/3307).
    - `eureka.client.enabled: false`, `register-with-eureka: false`, `fetch-registry: false` (không gửi heartbeat ra Eureka :8761).
    - Bộ test chỉ khởi tạo Spring Context nội bộ và MockMvc, hoàn toàn không gọi Docker API (`/var/run/docker.sock` hoặc cổng 2375).
- **Tách biệt kiểm thử ngoài**: Docker Smoke Test, Fault Test và Browser E2E Playwright chạy bằng profile/lệnh riêng biệt và luôn có Prerequisite Check.

---

## VI. Cấu Trúc Chi Tiết 5 Phase Triển Khai Của Đợt 4

Thứ tự triển khai: **Chuẩn bị hạ tầng & Fixtures (Phase 3) được đưa lên trước Bộ kiểm thử E2E (Phase 4)** để đảm bảo môi trường sẵn sàng trước khi chạy kịch bản toàn trình.

```mermaid
gantt
    title Kế Hoạch 5 Phase Đợt 4 (Thành Viên 2 — TDD)
    dateFormat  YYYY-MM-DD
    section Phase 1
    Internal Feign Contracts & Provider Verification TDD (17 Tests) :active, p1, 2026-10-28, 2d
    section Phase 2
    Gateway Resiliency & Distributed Consistency TDD               :p2, after p1, 2d
    section Phase 3
    Docker Environment Readiness & Isolated Test Fixtures Setup    :p3, after p2, 2d
    section Phase 4
    Comprehensive 4-Flow End-to-End Test Suite (E2E TDD)           :p4, after p3, 3d
    section Phase 5
    Docker Automation, Seeding Data & 13-Criteria VERIFIED Audit   :p5, after p4, 2d
```

---

### Phase 1: Kiểm Thử Hợp Đồng Feign Nội Bộ & Khả Năng Tương Thích Nhà Cung Cấp (Internal Feign Contracts & Provider Verification TDD)

- **Mục tiêu**: Kiểm chứng 14 endpoints nội bộ của Member 2 sẵn sàng phục vụ theo hợp đồng DTO, mã lỗi và serialization; kiểm chứng Fallback an toàn của `AssignmentClient` qua 3 cấp độ:
  1. *Cấp độ Fallback Bean*: Kiểm chứng hợp đồng phương thức của lớp `AssignmentClient.Fallback` (trả về `0L` và `Map.of()`).
  2. *Cấp độ Service Resilience*: Kiểm chứng `CourseServiceImpl` bắt ngoại lệ an toàn qua khối `try/catch` khi client gặp sự cố.
  3. *Cấp độ Feign Proxy Auto-Fallback*: Kiểm chứng Spring Cloud OpenFeign proxy thực sự tự động kích hoạt Fallback khi downstream gặp lỗi HTTP 500/kết nối thông qua `spring-cloud-starter-circuitbreaker-resilience4j`.

- **Phân loại rõ ràng các nhóm test trong Phase 1**:
  * **17 test cases thuộc danh sách kế hoạch ban đầu**: Gồm 15 Provider Contract Tests (bao phủ 14 endpoints của Member 2) + 2 Unit Fallback Contract Tests.
  * **1 test bổ sung cấp Service**: `CourseServiceImplTest#getCourseByIdGracefullyHandlesAssignmentServiceFailure` (kiểm chứng try/catch ở tầng nghiệp vụ).
  * **3 test bổ sung cấp Feign Proxy**: `AssignmentClientProxyIntegrationTest` (chạy qua Feign proxy thật kết nối HTTP server cục bộ cổng động theo test lifecycle).

#### 1. Danh sách 17 test cases Phase 1 theo kế hoạch ban đầu:

| STT | Endpoint / Hành Vi Kiểm Thử | Dữ Liệu Input Thử Nghiệm | Điều Kiện Tiên Quyết / Môi Trường Cô Lập | Assertions Chi Tiết (Status & Body) | Phân Loại TDD | Lệnh Thực Thi Kiểm Thử Thực Tế (Từ `D:\LMS-main`) | Kết Quả Thực Tế |
|:---:|---|---|---|---|:---:|---|:---:|
| 1 | `GET /internal/user/{id}` | Path: `user-1` | `@WebMvcTest`, Mock `InternalUserService.getUserById("user-1")` | HTTP 200, JSON schema `UserFullResponse`: `userId: "user-1"`, `username: "student01"`, `email`, `fullname`, `role: "STUDENT"` | Tái sử dụng (Baseline) | `mvn test -f backend/pom.xml -pl identity-service -Dtest=IdentityApiIntegrationTest#internalGetUserHttpContractReturns200AndUserDetails` | **PASS** |
| 2 | `GET /internal/user/{id}` (404) | Path: `user-not-found` | `@WebMvcTest`, Mock `InternalUserService.getUserById` ném `IdentityException(USER_NOT_EXISTED)` | HTTP 404 Not Found, JSON body `code: 1003` (`USER_NOT_EXISTED`) | Tái sử dụng (Baseline Verification) — Bổ sung kiểm chứng hành vi 404 hiện hữu | `mvn test -f backend/pom.xml -pl identity-service -Dtest=IdentityApiIntegrationTest#internalGetUserNotFoundHttpContractReturns404` | **PASS** |
| 3 | `POST /internal/users/batch` | Body: `["user-1", "user-2"]` | `@WebMvcTest`, Mock `InternalUserService.getUsersByIds` trả về danh sách 2 users | HTTP 200, JSON Array kích thước 2, `$[0].userId: "user-1"`, `$[1].userId: "user-2"` | Tái sử dụng (Baseline) | `mvn test -f backend/pom.xml -pl identity-service -Dtest=IdentityApiIntegrationTest#internalPostUsersBatchHttpContractReturns200` | **PASS** |
| 4 | `GET /internal/users/{userId}/exists` | Path: `user-1`, `user-999` | `@WebMvcTest`, Mock `InternalUserService.existsById` trả về `true`/`false` | HTTP 200, Response Body là `"true"` / `"false"` (primitive boolean) | Tái sử dụng (Baseline) | `mvn test -f backend/pom.xml -pl identity-service -Dtest=IdentityApiIntegrationTest#internalUserExistsHttpContractReturnsBoolean` | **PASS** |
| 5 | `GET /internal/users/search/by-email` | Query: `email=student01@example.com` | `@WebMvcTest`, Mock `InternalUserService.findByEmail` | HTTP 200, JSON schema `UserFullResponse`: `userId: "user-1"`, `email: "student01@example.com"` | Tái sử dụng (Baseline) | `mvn test -f backend/pom.xml -pl identity-service -Dtest=IdentityApiIntegrationTest#internalSearchUserByEmailHttpContractReturnsUser` | **PASS** |
| 6 | `GET /internal/courses/{courseId}` | Path: `course-test-123` | `@WebMvcTest`, Mock `InternalCourseService.getCourse` | HTTP 200, JSON schema `InternalCourseResponse`: `id`, `title`, `instructorId`, `status: "PUBLIC"` | Tái sử dụng (Baseline) | `mvn test -f backend/pom.xml -pl course-service -Dtest=InternalCourseApiIntegrationTest#internalGetCourseHttpContractReturns200` | **PASS** |
| 7 | `GET /internal/courses/{courseId}/lessons/{lessonId}` | Path: `course-test-123`, `lesson-test-456` | `@WebMvcTest`, Mock `InternalCourseService.getLesson` | HTTP 200, JSON schema `InternalLessonResponse`: `id`, `lessonType: "video"`, `courseId` | Tái sử dụng (Baseline) | `mvn test -f backend/pom.xml -pl course-service -Dtest=InternalCourseApiIntegrationTest#internalGetLessonHttpContractReturns200` | **PASS** |
| 8 | `GET /internal/courses/{courseId}/enrollments/{userId}` | Path: `course-test-123`, `user-test-789` | `@WebMvcTest`, Mock `InternalCourseService.existsEnrollment` | HTTP 200, Response Body là `"true"` (Boolean wrapper) | Tái sử dụng (Baseline) | `mvn test -f backend/pom.xml -pl course-service -Dtest=InternalCourseApiIntegrationTest#internalCheckEnrollmentHttpContractReturnsBoolean` | **PASS** |
| 9 | `GET /internal/courses/{courseId}/students/count` | Path: `course-test-123` | `@WebMvcTest`, Mock `InternalCourseService.countStudents` | HTTP 200, Response Body là `"42"` (Long wrapper) | Tái sử dụng (Baseline) | `mvn test -f backend/pom.xml -pl course-service -Dtest=InternalCourseApiIntegrationTest#internalCountStudentsHttpContractReturnsLong` | **PASS** |
| 10 | `GET /internal/courses/{courseId}/exists` | Path: `course-test-123` | `@WebMvcTest`, Mock `InternalCourseService.existsCourse` | HTTP 200, Response Body là `"true"` (Boolean wrapper) | Tái sử dụng (Baseline) | `mvn test -f backend/pom.xml -pl course-service -Dtest=InternalCourseApiIntegrationTest#internalCourseExistsHttpContractReturnsBoolean` | **PASS** |
| 11 | `GET /internal/courses/{courseId}/lessons/{lessonId}/exists` | Path: `course-test-123`, `lesson-test-456` | `@WebMvcTest`, Mock `InternalCourseService.existsLesson` | HTTP 200, Response Body là `"true"` (Boolean wrapper) | Tái sử dụng (Baseline) | `mvn test -f backend/pom.xml -pl course-service -Dtest=InternalCourseApiIntegrationTest#internalLessonExistsHttpContractReturnsBoolean` | **PASS** |
| 12 | `GET /internal/courses` | Không có param | `@WebMvcTest`, Mock `InternalCourseService.getAllPublishedCourses` | HTTP 200, `ApiResponse<List<ChatbotCourseResponse>>`: `code: 1000`, `data[0].id`, `data[0].title` | Tái sử dụng (Baseline) | `mvn test -f backend/pom.xml -pl course-service -Dtest=InternalCourseApiIntegrationTest#internalGetAllCoursesHttpContractReturnsList` | **PASS** |
| 13 | `GET /internal/courses/enrolled` | Query: `userId=user-test-789` | `@WebMvcTest`, Mock `InternalCourseService.getEnrolledCourses` | HTTP 200, `ApiResponse<List<ChatbotCourseResponse>>`: `code: 1000`, `data[0].id`, `data[0].title` | Tái sử dụng (Baseline) | `mvn test -f backend/pom.xml -pl course-service -Dtest=InternalCourseApiIntegrationTest#internalGetEnrolledCoursesHttpContractReturnsList` | **PASS** |
| 14 | `GET /internal/posts/{postId}` | Path: `1` | `@WebMvcTest`, Mock `InternalBlogService.getPost(1L)` | HTTP 200, `ApiResponse<InternalPostResponse>`: `code: 1000`, `data.id: 1`, `data.title`, `data.status: "PUBLISHED"` | Tái sử dụng (Baseline) | `mvn test -f backend/pom.xml -pl blog-service -Dtest=BlogApiIntegrationTest#internalGetPostHttpContractReturns200` | **PASS** |
| 15 | `GET /internal/posts/{postId}/exists` | Path: `1` | `@WebMvcTest`, Mock `InternalBlogService.existsPost(1L)` | HTTP 200, `ApiResponse<Boolean>`: `code: 1000`, `data: true` | Tái sử dụng (Baseline) | `mvn test -f backend/pom.xml -pl blog-service -Dtest=BlogApiIntegrationTest#internalPostExistsHttpContractReturnsBoolean` | **PASS** |
| 16 | `course-service` -> `AssignmentClient.Fallback` (Single Count) | Method Call: `fallback.getExerciseCount("course-123")` | Khởi tạo bean fallback trong ngữ cảnh test cô lập | Kiểm chứng hợp đồng Fallback: trả về `ApiResponse<Long>` với `data = 0L` | Bổ sung mới (Baseline Contract) | `mvn test -f backend/pom.xml -pl course-service -Dtest=AssignmentClientFallbackTest#assignmentClientFallbackReturnsZeroExerciseCountWhenInvoked` | **PASS** |
| 17 | `course-service` -> `AssignmentClient.Fallback` (Batch Count) | Method Call: `fallback.getExerciseCountBatch(["c-1", "c-2"])` | Khởi tạo bean fallback trong ngữ cảnh test cô lập | Kiểm chứng hợp đồng Fallback: trả về `ApiResponse<Map<String, Long>>` với `data = Map.of()` rỗng | Bổ sung mới (Baseline Contract) | `mvn test -f backend/pom.xml -pl course-service -Dtest=AssignmentClientFallbackTest#assignmentClientFallbackReturnsEmptyMapForBatchWhenInvoked` | **PASS** |

#### 2. Danh sách 3 test cases Feign Proxy Auto-Fallback (Kiểm chứng qua HTTP Server cục bộ):

| STT | Kịch Bản Kiểm Thử Proxy | Input & Trạng Thái Downstream | Môi Trường Test Cô Lập | Assertions Kiểm Chứng | Phân Loại TDD | Lệnh Thực Thi (Từ `D:\LMS-main`) | Kết Quả |
|:---:|---|---|---|---|:---:|---|:---:|
| 18 | Feign Proxy Gọi Thành Công | Downstream trả HTTP 200, `{"code":1000,"data":42}` | `HttpServer` cục bộ cổng động, `@SpringBootTest` | `response.getData() == 42L` (khác fallback 0L); xác nhận request tới server test | TDD Verification | `mvn test -f backend/pom.xml -pl course-service -Dtest=AssignmentClientProxyIntegrationTest#exerciseCountReturnsRealDataWhenDownstreamSucceeds` | **PASS** |
| 19 | Feign Proxy Tự Động Fallback (Single Count) | Downstream trả HTTP 500 `Internal Server Error` | `HttpServer` cục bộ cổng động, `@SpringBootTest` | Proxy bắt lỗi 500, kích hoạt fallback trả về `ApiResponse<Long> = 0L`, không ném FeignException | TDD Red -> Green | `mvn test -f backend/pom.xml -pl course-service -Dtest=AssignmentClientProxyIntegrationTest#exerciseCountProxyTriggersFallbackWhenDownstreamFails` | **PASS** |
| 20 | Feign Proxy Tự Động Fallback (Batch Count) | Downstream trả HTTP 500 `Internal Server Error` | `HttpServer` cục bộ cổng động, `@SpringBootTest` | Proxy bắt lỗi 500, kích hoạt fallback trả về `ApiResponse<Map<String, Long>> = Map.of()`, không ném FeignException | TDD Red -> Green | `mvn test -f backend/pom.xml -pl course-service -Dtest=AssignmentClientProxyIntegrationTest#exerciseCountBatchProxyTriggersFallbackWhenDownstreamFails` | **PASS** |

- **Tiêu chuẩn hoàn thành Phase 1**:
  * Đầy đủ 17/17 test cases kế hoạch + 3 test Feign proxy + 1 test service resilience chạy PASS 100% trên môi trường cô lập, hoàn toàn độc lập với Docker và CSDL ngoài.
  * Báo cáo kết quả thực tế, **dừng lại chờ người dùng nghiệm thu** trước khi chuyển Phase 2.
  * Commit: `test(feign): kiểm chứng hợp đồng api nội bộ và cơ chế auto-fallback feign proxy`.

---

### Phase 2: Cơ Chế Chịu Lỗi Gateway & Tính Toàn Vẹn Dữ Liệu Phân Tán (Gateway Resiliency & Distributed Consistency TDD)

- **Mục tiêu**: Bổ sung `GatewayFallbackController` còn thiếu trong `api-gateway` theo đúng TDD; kiểm chứng hành vi Circuit Breaker và Timeout theo cấu hình thực tế; kiểm chứng quy tắc phân tán khi tài khoản hoặc khóa học bị khóa.
- **Khảo sát cấu hình thực tế**:
  * Circuit Breaker: `sliding-window-size: 10`, `minimum-number-of-calls: 5`, `failure-rate-threshold: 50%`, `wait-duration-in-open-state: 10s`.
  * Timelimiter: default 10s, `courseServiceCircuitBreaker` 15s, `assignmentServiceCircuitBreaker` 15s, `assignmentAiServiceCircuitBreaker` 130s, `chatbotServiceCircuitBreaker` 130s.
  * **Hợp đồng lỗi Fallback**: Trả về **HTTP 503 Service Unavailable**, JSON body: `status: 503`, `code: 1010`, `message: "Dịch vụ tạm thời không khả dụng"`, `service: "{serviceName}"`.
  * **Cách BFF (`web-app`) xử lý**: `GatewayClient` nhận HTTP 503 / code 1010, chuyển thành thông báo lỗi thân thiện trên giao diện hoặc view `error/503.html`.
  * **Cơ chế khóa User**: Mã nguồn hiện có enum `ErrorCode.USER_INACTIVE` (code 1006, HTTP 403 Forbidden). Khi Admin khóa user (`isActive = false`), hệ thống chặn đăng nhập mới và chặn cấp refresh token. Access token cũ còn hạn (tối đa 15 phút) là stateless JWT; ghi nhận đề xuất thu hồi token qua Redis.
  * **Cơ chế khóa Khóa học**: Khóa học có `CourseStatus.LOCKED`, biến mất khỏi trang tìm kiếm public.
- **Tiêu chuẩn hoàn thành Phase 2**:
  * `GatewayFallbackControllerTest` và các test cascading state PASS 100%.
  * Báo cáo kết quả thực tế, **dừng lại chờ người dùng nghiệm thu** trước khi chuyển Phase 3.
  * Commit dự kiến: `feat(gateway): bổ sung fallback controller cho circuit breaker và kiểm chứng tính nhất quán phân tán`.

---

### Phase 3: Chuẩn Bị Môi Trường Docker Tối Thiểu, Quản Trị Cấu Hình & Quản Lý Test Fixtures (Docker Environment Readiness & Isolated Test Fixtures Setup)

- **Mục tiêu**: Xác thực sức khỏe của stack Docker Thành viên 2 (11 container hiện có); chuẩn hóa cấu hình môi trường test; xây dựng cơ chế quản lý dữ liệu kiểm thử (Test Fixtures) có định danh riêng biệt và tự động dọn dẹp (cleanup).
- **Quy tắc an toàn trong Fault Testing**:
  * **Bước phục hồi service bắt buộc nằm trong khối `finally` hoặc cleanup hook** (ví dụ `afterEach` / `afterAll`), để dù assertion kiểm thử thất bại thì bước khởi động lại service (`docker compose start <service>`) vẫn luôn được thực thi.
  * Sau khi phục hồi, **bắt buộc phải có bước kiểm tra sức khỏe (Health Check)** xác nhận container đã trở lại trạng thái `healthy` hoặc HTTP 200 trước khi kết thúc test.
  * **Tuyệt đối không tuyên bố "đã khôi phục"** nếu bước khởi động lại hoặc health check thất bại.
  * Thao tác ngắt service chỉ được thực hiện trên môi trường test cục bộ, không ảnh hưởng môi trường dùng chung.
- **Quản lý dữ liệu Fixture**:
  * Tách biệt: **Dữ liệu Demo** (tài khoản mẫu nạp ở Phase 5) và **Dữ liệu Fixture Test** (sinh động trong test: `e2e_student_<timestamp>@test.lms`).
  * Cleanup bắt buộc trong `finally`: chỉ xóa dữ liệu do test tạo ra. Tuyệt đối **không xóa volume Docker**, không chạy `DROP DATABASE` hoặc xóa dữ liệu dùng chung.
- **Commit dự kiến**: `chore(test): chuẩn hóa fixture lifecycle cô lập và bộ kiểm tra điều kiện tiên quyết docker`.

---

### Phase 4: Bộ Kiểm Thử Toàn Trình 4 Luồng Nghiệp Vụ E2E TDD (Comprehensive End-to-End Test Suite)

- **Mục tiêu**: Xây dựng và thực thi 4 kịch bản kiểm thử E2E liên hoàn, kiểm tra từ giao diện Web-App BFF (:8088) qua API Gateway (:8080) tới CSDL thật (MySQL container :3306, host :3307; MongoDB :27017; MinIO :9000).
- **Đối chiếu Route Controller Thật Trong Web-App**:
  * Trang xem bài học sinh viên: **`/learn/{courseId}`** và **`/learn/{courseId}/lessons/{lessonId}`** ([CourseStudentController.java:208, 228](file:///D:/LMS-main/backend/web-app/src/main/java/com/lms/webapp/controller/CourseStudentController.java#L208-L228)).
  * Trang quản lý cấu trúc khóa học giảng viên: **`/instructor/courses/{id}/manage`** và **`/instructor/courses/{courseId}/structure/nodes`** ([CourseInstructorController.java:246, 298](file:///D:/LMS-main/backend/web-app/src/main/java/com/lms/webapp/controller/CourseInstructorController.java#L246-L298)).
  * Trang tạo bài viết blog: **`/blog/new`** ([BlogController.java:169, 181](file:///D:/LMS-main/backend/web-app/src/main/java/com/lms/webapp/controller/BlogController.java#L169-L181)).
  * Trang quản trị Admin: **`POST /admin/users/{id}/toggle-active`** (khóa/mở khóa user, [AdminController.java:116](file:///D:/LMS-main/backend/web-app/src/main/java/com/lms/webapp/controller/AdminController.java#L116)), **`POST /admin/users/admins`** (tạo admin mới, [AdminController.java:164](file:///D:/LMS-main/backend/web-app/src/main/java/com/lms/webapp/controller/AdminController.java#L164)), **`POST /admin/courses/{id}/lock`** (khóa khóa học, [AdminController.java:240](file:///D:/LMS-main/backend/web-app/src/main/java/com/lms/webapp/controller/AdminController.java#L240)), **`POST /admin/categories`** ([AdminController.java:299](file:///D:/LMS-main/backend/web-app/src/main/java/com/lms/webapp/controller/AdminController.java#L299)), **`POST /admin/tags`** ([AdminController.java:381](file:///D:/LMS-main/backend/web-app/src/main/java/com/lms/webapp/controller/AdminController.java#L381)).

#### Chi tiết 4 Luồng Nghiệp Vụ E2E:

| Tên Luồng | Vai Trò | Điều Kiện Đầu Vào | Các Service Tham Gia | Thao Tác UI / API Thực Tế (Route Thật) | Assertions Bắt Buộc (Nghiệp Vụ + CSDL Lưu Trữ + RBAC) | Cơ Chế Cleanup | Ranh Giới Phụ Thuộc Trí |
|---|---|---|---|---|---|---|:---:|
| **E2E Luồng 1** (Học viên) | `ROLE_STUDENT` | Mailpit :8025 chạy, có ít nhất 1 khóa học public | `web-app` (:8088), `api-gateway` (:8080), `identity-service`, `course-service`, `mailpit`, MySQL, MongoDB | `GET /register` -> `POST /register` -> Đọc OTP Mailpit API -> `POST /verify-email` -> `POST /login` -> `GET /courses` -> `POST /courses/{id}/enroll` -> **`GET /learn/{courseId}`** -> **`POST /learn/{courseId}/lessons/{lessonId}/complete`** | - HTTP status 200/302, session cookie sạch.<br>- MySQL `users.is_active = true`.<br>- MySQL `enrollments` có bản ghi.<br>- MySQL `lesson_progress` tiến độ tăng %.<br>- MongoDB: bài học kế tiếp mở khóa. | Xóa user test, enrollment và progress tương ứng trong khối `finally` | Độc lập (Làm Quiz/Code cần Trí) |
| **E2E Luồng 2** (Giảng viên) | `ROLE_TEACHER` | Tài khoản giảng viên test sẵn sàng | `web-app` (:8088), `api-gateway` (:8080), `course-service`, `minio`, MySQL, MongoDB | `POST /login` -> `GET /instructor/courses/new` -> `POST /instructor/courses/new` (MinIO S3 thumbnail) -> **`GET /instructor/courses/{id}/manage`** -> **`POST /instructor/courses/{courseId}/structure/nodes`** (MongoDB cây đề cương) -> `POST /instructor/courses/{id}/edit` (status `PUBLIC`) | - MySQL `courses.status = 'PUBLIC'`.<br>- MinIO `lms-course-bucket` có thumbnail hợp lệ.<br>- MongoDB `course_structures` lưu đúng cấu trúc cây.<br>- Khóa học hiển thị trên trang danh sách public. | Xóa khóa học test, xóa thumbnail MinIO, xóa cây đề cương Mongo trong khối `finally` | Độc lập (AI sinh Quiz cần Trí) |
| **E2E Luồng 3** (Blog & Forum) | Tác giả & Độc giả | Tài khoản người dùng đã đăng nhập | `web-app` (:8088), `api-gateway` (:8080), `blog-service`, `minio`, MySQL | **`GET /blog/new`** -> **`POST /blog/new`** (Markdown + thumbnail) -> `GET /blog/{slug}` (độc giả đọc bài) -> `POST /blog/posts/{postId}/comments` (bình luận cấp 1) -> `POST /blog/posts/{postId}/comments` kèm `parentId` (cấp 2) -> Thử gửi bình luận cấp 3 -> `POST /blog/posts/{postId}/vote` | - MySQL `posts` lưu bài viết, `view_count` tăng atomic.<br>- MySQL `comments` lưu comment cấp 1 và cấp 2 (có `parent_id`).<br>- Bình luận cấp 3 bị từ chối với lỗi nghiệp vụ rõ ràng.<br>- Bảng `post_votes` ghi nhận vote chính xác. | Xóa bài viết, comment, votes và thumbnail test trong khối `finally` | Độc lập 100% |
| **E2E Luồng 4** (Quản trị Admin) | `ROLE_ADMIN` | Tài khoản Admin test sẵn sàng | `web-app` (:8088), `api-gateway` (:8080), `identity-service`, `course-service`, `blog-service`, MySQL | `POST /login` -> `GET /admin/dashboard` (xem KPI) -> **`POST /admin/users/{id}/toggle-active`** (khóa user vi phạm) -> **`POST /admin/courses/{id}/lock`** (khóa khóa học) -> **`POST /admin/users/admins`** (tạo Admin mới) -> **`POST /admin/categories`** & **`POST /admin/tags`** | - MySQL `users.is_active = false`; user bị khóa nhận lỗi `USER_INACTIVE` (HTTP 403, code 1006) khi đăng nhập.<br>- MySQL `courses.status = 'LOCKED'`; biến mất khỏi danh sách public.<br>- RBAC: Học viên cố gọi API Admin nhận **403 Forbidden**.<br>- Category & Tag mới lưu thành công. | Mở khóa hoặc dọn dẹp user/course/category test trong khối `finally` | Độc lập (Thống kê bài tập cần Trí) |

- **Tiêu chuẩn hoàn thành Phase 4**:
  * 4/4 Luồng E2E chạy thực tế PASS 100%.
  * Báo cáo kết quả thực tế, **dừng lại chờ người dùng nghiệm thu** trước khi chuyển Phase 5.
  * Commit dự kiến: `test(e2e): hoàn thành 4 luồng kiểm thử toàn trình học viên giảng viên blog admin theo route controller thật`.

---

### Phase 5: Tự Động Hóa Docker Hoàn Thiện, Seeding Dữ Liệu Demo Chuẩn Hóa & Rà Soát 13 Tiêu Chí VERIFIED

- **Mục tiêu**: Hoàn thiện khả năng tái lập của hệ thống (1 lệnh khởi động), nạp dữ liệu mẫu phong phú phục vụ demo/thuyết trình, và rà soát nghiêm ngặt từng tính năng theo 13 tiêu chí tại [AGENTS.md](file:///D:/LMS-main/AGENTS.md).
- **Rà soát 13 Tiêu chí Nghiệm thu VERIFIED**:
  * Đánh giá từng tính năng theo đủ 13 tiêu chí trong `AGENTS.md`.
  * **Bỏ việc tick sẵn toàn bộ `[x]`**: Chuyển thành mục chờ kiểm chứng; chỉ đánh dấu hoàn thành khi có bằng chứng thực tế tại Phase 5.
  * **Google OAuth 2.0**: Giữ nguyên trạng thái **`TESTED`**, ghi rõ điều kiện cần để nghiệm thu `VERIFIED` (cần tài khoản Google Cloud Console thật và domain redirect được cấp phép).
  * **Không tự ý tạo Git tag `v1.0.0`**: Release toàn dự án cần thống nhất với Trí và có chỉ đạo của người dùng.
- **Tiêu chuẩn hoàn thành Phase 5**:
  * Stack khởi động ổn định kèm dữ liệu demo.
  * Bảng đối chiếu 13 tiêu chí hoàn thành với bằng chứng cụ thể.
  * Báo cáo tổng kết Đợt 4 và dừng lại chờ nghiệm thu toàn diện từ người dùng.
  * Commit dự kiến: `docs(release): rà soát 13 tiêu chí verified và hoàn tất tài liệu bàn giao đợt 4`.

---

## VII. Bảng Tổng Hợp 5 Phase & Kế Hoạch Kiểm Thử

| Phase | Tên Giai Đoạn | Phương Pháp Kiểm Thử | Điều Kiện Tiên Quyết | Sản Phẩm Đầu Ra Cụ Thể | Trạng Thái Dự Kiến |
| :---: | :--- | :---: | :--- | :--- | :---: |
| **Phase 1** | Internal Feign Contracts & Provider Verification TDD (16 Tests) | MockMvc / Contract Test (Profile `test`, Isolated H2) | Mã nguồn `identity`, `course`, `blog` hiện có | 14 Provider Contracts PASS + 2 Fallback an toàn | **Sẵn sàng bắt đầu ngay** |
| **Phase 2** | Gateway Resiliency & Distributed Consistency TDD | Fault / WebTestClient TDD | Phase 1 hoàn tất | `GatewayFallbackController` hoạt động + Cascading rules PASS | Kế tiếp |
| **Phase 3** | Docker Environment Readiness & Isolated Fixtures Setup | Healthcheck Probe & Fixture Lifecycle Test | Docker Engine running, 11 container healthy | Stack Docker sẵn sàng + Fixture cleanup tự động | Kế tiếp |
| **Phase 4** | Comprehensive 4-Flow End-to-End Test Suite (E2E TDD) | Playwright Browser E2E & API E2E (Route Thật) | Phase 3 hoàn tất (Docker stack & Fixtures sẵn sàng) | 4 Luồng E2E nghiệp vụ PASS 100% | Kế tiếp |
| **Phase 5** | Docker Automation, Seeding & Parity Matrix Audit | Full Regression Audit & 13 Tiêu chí AGENTS.md | Phase 4 hoàn tất | Seeding demo chuẩn, Ma trận Parity cập nhật minh bạch | Nghiệm thu cuối |

---

## VIII. Quy Trình Quản Lý Git & Xử Lý Hiện Trạng Workspace

### 1. Hiện trạng Workspace & Đối chiếu Commit Phase 7
Trước khi triển khai mã nguồn, tiến hành kiểm tra `git status` trên nhánh `feature/member-2-dot-4-integration`:
- **Commit Phase 7 trên nhánh `feature/member-2-thymeleaf-web-app`**:
  * Mã commit: `634ffa9` — *fix(web-app): siết kiểm thử e2e và xử lý lỗi gửi otp*.
- **Các file unstaged trong workspace hiện tại**:
  * Các file thuộc `backend/web-app/` (`AuthController.java`, `otp-countdown.js`, `verify-email.html`, `AuthControllerTest.java`, `browser-avatar-cors.e2e.test.js`, `support/`, etc.) và tài liệu `docs/DOCKER_SMOKE_TESTS.md`, `docs/progress/member-2.md` **trùng khớp chính xác** với bản sửa lỗi của commit `634ffa9`.
  * Các file khác: `.gitignore`, `frontend/e2e/auth-flow.spec.js`, `frontend/src/context/AuthContext.jsx`, `frontend/src/features/auth/pages/SocialCallbackPage.jsx`.
- **Nguyên tắc bảo vệ workspace**:
  * **TUYỆT ĐỐI KHÔNG** chạy `git reset --hard`, `git checkout -- .`, `git clean -fd` để tránh làm mất các cải tiến đã nghiệm thu của Phase 7.
  * **KHÔNG** tự động cherry-pick đè file khi chưa đánh giá trạng thái. Giữ nguyên working tree an toàn.

### 2. Quy trình làm việc chuẩn sau khi kế hoạch được duyệt
Mọi thao tác trong quá trình triển khai Đợt 4 phải tuân thủ nghiêm ngặt:
1. Triển khai tuần tự từng Phase (Phase 1 -> Phase 2 -> Phase 3 -> Phase 4 -> Phase 5).
2. Chạy kiểm thử bắt buộc (compile không lỗi, unit test/contract test PASS, Docker smoke PASS nếu thuộc phase Docker).
3. Cập nhật kết quả kiểm thử thực tế vào [docs/progress/member-2.md](file:///D:/LMS-main/docs/progress/member-2.md) và các tài liệu liên quan.
4. Tạo commit theo chuẩn **Conventional Commits** với nội dung tiếng Việt có dấu rõ nghĩa.
5. Push commit lên nhánh duy nhất của Đợt 4: `feature/member-2-dot-4-integration`.
6. **Không tạo nhánh con theo từng phase**, không dùng cờ `--force`, không merge vào `dev`/`develop`/`main`.
7. **Sau mỗi Phase: Dừng lại, báo cáo kết quả chi tiết và chờ người dùng nghiệm thu trước khi bắt đầu Phase tiếp theo.**

---

## IX. Danh Sách Các Điểm "CẦN XÁC NHẬN" & Phối Hợp Liên Nhóm

1. **CẦN XÁC NHẬN 1 (Bảo mật mạng liên dịch vụ & Chặn route internal từ Gateway)**:
   - Thống nhất cơ chế chặn caller bên ngoài gọi vào `/api/*/internal/**` qua API Gateway; và thống nhất phương án Network Isolation (`expose`) hoặc Header bí mật `X-Internal-Secret` giữa các service.
2. **CẦN XÁC NHẬN 2 (Chuẩn hóa Schema Response cho Internal APIs)**:
   - Hiện tại 4 endpoint của `identity-service` trả về trực tiếp DTO không bọc `ApiResponse`, khớp với `IdentityClient` trong mã nguồn mẫu của Trí. 2 endpoints Chatbot của `course-service` và 2 endpoints của `blog-service` trả về `ApiResponse<T>`. Cần xác nhận duy trì cấu trúc này hay chuẩn hóa toàn bộ về `ApiResponse<T>`.
3. **CẦN XÁC NHẬN 3 (Nhu cầu thực tế đối với 6 endpoints nội bộ nhóm B)**:
   - Xác nhận với Trí xem `assignment-service` và `chatbot-service` có dự kiến gọi 6 endpoints: `search/by-email`, `lessons/{lessonId}`, `courses/{id}/exists`, `lessons/{id}/exists`, `posts/{id}`, `posts/{id}/exists` hay không để quyết định duy trì hợp đồng hay tinh giản.
4. **CẦN XÁC NHẬN 4 (Quy mô Container Stack và Giới Hạn Tài Nguyên)**:
   - Khi Thành viên 1 tích hợp `assignment-service`, `chatbot-service` và Judge0 Engine, số lượng container sẽ tăng từ 11 lên 14-16 containers. Cần thống nhất cấu hình RAM limits và phân bổ compose file hợp lý.
5. **CẦN XÁC NHẬN 5 (Thông tin xác thực Google OAuth 2.0 Thật)**:
   - Để nghiệm thu `VERIFIED` cho tính năng Google SSO, người dùng cần cung cấp Google OAuth Client ID & Secret staging hợp lệ cùng domain redirect được cấp phép trên Google Cloud Console.

---

## X. Kết Luận Khảo Sát & Đánh Giá Mức Độ Sẵn Sàng (Readiness)

- **Phase 1 (Internal Feign Contracts & Provider Verification TDD — 16 Tests)**: **ĐÃ ĐỦ ĐIỀU KIỆN ĐỂ BẮT ĐẦU NGAY**.
  * Có thể bắt đầu triển khai sau khi hoàn tất đối chiếu hợp đồng, chốt 16 test cases cụ thể và kiểm tra trạng thái Git; không cần chờ Trí hoàn thành toàn bộ.
- **Phase 2 (Gateway Resiliency & Distributed Consistency TDD)**: **CẦN KIỂM CHỨNG ĐIỀU KIỆN KHI ĐẾN LƯỢT**.
  * Cần Phase 1 hoàn tất và kiểm tra runtime Gateway trước khi triển khai `GatewayFallbackController`.
- **Phase 3 (Docker Environment Readiness & Isolated Test Fixtures)**: **CẦN KIỂM CHỨNG ĐIỀU KIỆN KHI ĐẾN LƯỢT**.
  * Cần xác thực sức khỏe của 11 container Member 2 tại thời điểm chạy và kiểm tra fixture cleanup.
- **Phase 4 (4 Luồng E2E TDD Theo Route Controller Thật)**: **CẦN KIỂM CHỨNG ĐIỀU KIỆN KHI ĐẾN LƯỢT**.
  * Cần Phase 3 hoàn tất (stack và fixture sẵn sàng) trước khi thực thi kịch bản trình duyệt.
- **Phase 5 (Docker Automation & 13-Criteria VERIFIED Audit)**: **CHỜ KẾT QUẢ CỦA CÁC PHASE TRƯỚC**.
  * Cần kết quả chạy thật của cả 4 Phase trước và sự thống nhất phạm vi phát hành cùng Thành viên 1.
