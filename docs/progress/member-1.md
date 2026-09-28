# Kế Hoạch & Nhật Ký Tiến Độ Cá Nhân — Thành Viên 1 (member-1.md)

> [!NOTE]
> File này dành riêng cho **Thành viên 1 — Nguyễn Minh Trí** (Trưởng nhóm). Chứa lộ trình 12 đợt commit, bảng checklist kiểm soát chất lượng và nhật ký thực hiện chi tiết theo chuẩn Conventional Commits và Rubric HCMUTE.

---

## 1. Thông Tin Thành Viên
- **Họ và tên**: Nguyễn Minh Trí
- **MSSV**: 23110340
- **Vai trò**: Trưởng nhóm / Fullstack Developer (Chủ trì Khối Nghiệp vụ Phức tạp & Công nghệ Cốt lõi)
- **Nhánh Git làm việc**: `feature/member-1` (tạo từ `develop`)
- **Phân hệ phụ trách chính**:
  1. `backend/service-registry` (Eureka Server 8761 khám phá dịch vụ)
  2. `backend/api-gateway` (Spring Cloud Gateway 8080, Token Offloading, Circuit Breaker, CORS)
  3. `backend/assignment-service` (8083: Quiz, Question Bank CSV/TXT, Coding Problem, Testcase, Sandbox Judge0, Gradebook, Leaderboard)
  4. `backend/chatbot-service` (8085: Trợ lý AI DeepSeek/OpenCode LLM streaming WebFlux SSE, Rate Limiter Redis)
  5. Giao diện Frontend tương ứng (`features/assignment/code-judge`, `features/assignment/quiz`, `components/ui/ChatBot`, `features/dashboard`)

---

## 2. Bảng Theo Dõi & Danh Sách Các Đợt Commit Của Thành Viên 1

Bạn hãy chuyển `[ ]` thành `[x]` sau khi hoàn thành và commit thành công mỗi đợt:

| Trạng thái | Đợt / Thời gian | Nội dung công việc cần làm | File liên quan dự kiến | Commit Message gợi ý (chuẩn dev) | Ghi chú / Hash |
|:---:|:---:|---|---|---|---|
| [x] | **Đợt 1<br>(27/09)** | Khởi tạo cấu trúc tài liệu kỹ thuật gốc, khảo sát sâu Assignment & Chatbot LLM từ mẫu | `README.md`, `AGENTS.md`, `CONTRIBUTING.md`, `docs/*` | `docs: hoan thien ho so ky thuat rubric HCMUTE va phan cong Tri chu tri assignment chatbot` | Hash: `7a69d45` (Đã hoàn thành) |
| [x] | **Đợt 2<br>(30/09 – 02/10)** | Thiết lập Maven Parent POM, Service Registry (Eureka), API Gateway, khung Assignment & Chatbot | `backend/pom.xml`, `backend/service-registry/*`, `backend/api-gateway/*`, `backend/assignment-service/*`, `backend/chatbot-service/*`, `docker-compose.db.yml` | `feat(infra): thiet lap eureka api gateway va khung assignment chatbot` | Hash: `a02474a` (Build & Test Passed) |
| [ ] | **Đợt 3<br>(03/10 – 05/10)** | Triển khai Ngân hàng câu hỏi trắc nghiệm, Parser nhập CSV/TXT, Tạo đề thi Quiz và cơ chế khóa chống spam | `assignment-service/src/main/java/.../QuizServiceImpl.java`, `Question.java` | `feat(assignment): triển khai ngân hàng câu hỏi và tạo quiz` | Kế hoạch tiếp theo |
| [ ] | **Đợt 4<br>(06/10 – 08/10)** | Triển khai Quản lý bài tập lập trình (Problem), bộ Testcase ẩn/hiện và lưu Submission vào MongoDB | `assignment-service/src/main/java/.../ProblemServiceImpl.java`, `Submission.java` | `feat(assignment): thêm quản lý bài tập lập trình và test cases` | |
| [ ] | **Đợt 5<br>(09/10 – 11/10)** | Tích hợp Judge0 API Engine chấm code tự động đa ngôn ngữ, xử lý ThreadPool bất đồng bộ | `assignment-service/src/main/java/.../JudgeServiceImpl.java`, `SubmissionServiceImpl.java` | `feat(judge): tích hợp trình chấm code tự động qua judge0` | |
| [ ] | **Đợt 6<br>(12/10 – 14/10)** | Triển khai Bảng điểm tổng hợp (Gradebook), Lịch sử thi Quiz, Thống kê Leaderboard thời gian thực | `assignment-service/src/main/java/.../GradeServiceImpl.java`, `LeaderboardServiceImpl.java` | `feat(assignment): triển khai bảng điểm gradebook và leaderboard` | |
| [ ] | **Đợt 7<br>(15/10 – 17/10)** | Xây dựng giao diện Trình soạn thảo CodeMirror 6, Giao diện làm bài Quiz đếm ngược và kết quả | `frontend/src/features/assignment/code-judge/*`, `frontend/src/features/assignment/quiz/*` | `feat(frontend): kết nối giao diện quiz và trình soạn thảo code judge` | |
| [ ] | **Đợt 8<br>(18/10 – 20/10)** | Tích hợp Trợ lý ảo AI Chatbot (OpenCode/DeepSeek LLM WebClient SSE), Rate Limiter Redis, Dashboard | `chatbot-service/src/main/java/.../ChatServiceImpl.java`, `frontend/src/components/ui/ChatBot.jsx` | `feat(chatbot): tích hợp trợ lý ai và dashboard thống kê` | |
| [ ] | **Đợt 9<br>(21/10 – 23/10)** | Bổ sung kiểm thử phủ kín cho Assignment, Code Judge, Chatbot; kiểm thử các trường hợp biên của Parser và Scoring | `assignment-service/src/test/...`, `chatbot-service/src/test/...` | `test(backend): bổ sung kiểm thử assignment code judge và chatbot` | |
| [ ] | **Đợt 10<br>(24/10 – 25/10)** | Tinh chỉnh cấu hình Circuit Breaker, Gateway Timeouts, bộ lọc chống giả mạo header, xử lý CORS | `api-gateway/src/main/resources/application.yaml`, `AuthFilter.java` | `fix(gateway): tinh chỉnh timeout circuit breaker và bảo mật header` | |
| [ ] | **Đợt 11<br>(26/10 – 27/10)** | Thực hiện kịch bản kiểm thử tích hợp toàn diện End-to-End (E2E) trên toàn bộ hệ thống Docker | `docs/TESTING_PLAN.md`, `docs/progress/integration.md` | `test(e2e): kiểm thử end to end toàn hệ thống trên môi trường docker` | |
| [ ] | **Đợt 12<br>(28/10 – 29/10)** | Chuẩn bị dữ liệu mẫu (Seeding), kiểm tra chốt chặn cuối cùng, merge `develop` -> `main`, phát hành v1.0.0 | `docker/mysql/init/*`, `README.md`, Release v1.0.0 | `chore(release): phát hành phiên bản chính thức v1.0.0` | |

---

## 3. Quy Trình Làm Việc Tiêu Chuẩn
1. **Trước mỗi đợt**: Rà soát yêu cầu trong `COMMIT_PLAN.md`, kiểm tra branch `feature/member-1`.
2. **Khởi động Database**:
   ```bash
   docker compose -f docker-compose.db.yml up -d
   ```
3. **Kiểm thử bắt buộc**: Chạy `mvn clean compile` hoặc `mvn test -pl <module>` đảm bảo không có lỗi.
4. **Cập nhật tiến độ**: Đánh dấu `[x]` vào đợt đã hoàn thành trên file này, ghi nhận hash commit và kết quả kiểm thử.

---

## 4. Nhật Ký Chi Tiết Từng Ngày

### [2026-09-27] — Đợt 1: Khảo Sát Dự Án & Thiết Lập Bộ Tài Liệu Kỹ Thuật Ban Đầu
1. **Ngày thực hiện**: 27/09/2026
2. **Công việc đã làm**:
   - Khảo sát toàn diện kiến trúc Monorepo Microservices của project mẫu: phân tích `pom.xml`, `docker-compose.yml`, các controllers, entities và routes frontend.
   - Xác nhận sự cô lập và tính toàn vẹn của thư mục project mẫu (chỉ đọc) và thư mục phát triển `DoAnCK`.
   - Kiểm tra trạng thái Git ban đầu của dự án.
   - Soạn thảo và khởi tạo toàn bộ bộ tài liệu kỹ thuật cốt lõi: `README.md`, `AGENTS.md`, `CONTRIBUTING.md`, `CHANGELOG.md`, `.env.example`, `ARCHITECTURE.md`, `REQUIREMENTS.md`, `USER_ROLES.md`, `TEAM_ROLES.md`, `ROADMAP.md`, `COMMIT_PLAN.md`, `GIT_WORKFLOW.md`, `CODING_RULES.md`, `TESTING_PLAN.md`, `DEPLOYMENT_GUIDE.md`, `RUBRIC_MAPPING.md`, `OPEN_QUESTIONS.md`, `DECISIONS.md`.
3. **File thay đổi**:
   - `README.md`, `AGENTS.md`, `CONTRIBUTING.md`, `CHANGELOG.md`, `.env.example`
   - `docs/*` (24 tài liệu kỹ thuật)
4. **Test đã chạy**:
   - Kiểm tra liên kết tệp tin và cú pháp sơ đồ Mermaid trong tất cả các file tài liệu.
   - Chạy lệnh kiểm tra `git status` tại thư mục `DoAnCK`.
5. **Kết quả**:
   - 24 tài liệu được tạo mới thành công, không có lỗi định dạng markdown.
   - Xác nhận `DoAnCK` sẵn sàng bước vào giai đoạn thiết lập khung mã nguồn.
6. **Commit**:
   - `docs: hoan thien ho so ky thuat rubric HCMUTE va phan cong Tri chu tri assignment chatbot` (Hash: `7a69d45`)

---

### [2026-09-27] — Đợt 2: Thiết Lập Hạ Tầng Backend Monorepo & Khung Phân Hệ Assignment, Chatbot
1. **Ngày thực hiện**: 27/09/2026 – 28/09/2026
2. **Công việc đã làm**:
   - **Tập tin quản lý Git**: Khởi tạo `.gitignore` toàn diện cho hệ thống Monorepo (loại trừ `target/`, `node_modules/`, `.env`, keystore, logs).
   - **Hạ tầng cơ sở dữ liệu**:
     - Viết `docker/mysql/init/01-create-databases.sql` tạo 4 cơ sở dữ liệu tự động (`lms_identity`, `lms_course`, `lms_assignment`, `lms_blog`) với bộ mã `utf8mb4_unicode_ci` (khắc phục ADR-005).
     - Viết `docker-compose.db.yml` quản lý tập trung 4 container dịch vụ dữ liệu: MySQL 8.4 LTS, MongoDB 7.0, Redis 7.2 Alpine, MinIO S3 Object Storage.
   - **Maven Parent POM (`backend/pom.xml`)**:
     - Quản lý tập trung Spring Boot 3.3.5, Spring Cloud 2023.0.1, Java 21 LTS.
     - Dependency Management cho Lombok, MapStruct 1.5.5, JJWT 0.12.5, Resilience4j 2.2.0, SpringDoc OpenAPI.
     - Khai báo 4 module con: `service-registry`, `api-gateway`, `assignment-service`, `chatbot-service`.
   - **Khởi tạo Service Registry (`service-registry`)**:
     - Cổng dịch vụ: 8761.
     - Kích hoạt `@EnableEurekaServer` với cấu hình standalone local.
     - Khắc phục lỗi tổ chức package ADR-006 sang chuẩn `com.lms.serviceregistry`.
   - **Khởi tạo API Gateway (`api-gateway`)**:
     - Cổng dịch vụ: 8080.
     - Kích hoạt Spring Cloud Gateway kết hợp Eureka Client.
     - Cấu hình định tuyến đầy đủ: `/api/v1/auth/**`, `/api/v1/courses/**`, `/api/v1/assignments/**`, `/api/v1/chat/**`, `/api/v1/posts/**`.
     - Tích hợp `AuthFilter` thực hiện Token Offloading (xác thực JWT tập trung, gán headers `X-User-Id`, `X-User-Role` chuyển tiếp cho downstream services).
     - Tích hợp Resilience4j Circuit Breaker fallback (`/fallback/**`) và bộ xử lý ngoại lệ phản hồi JSON `GlobalErrorExceptionHandler`.
   - **Khởi tạo Khung Phân Hệ Assignment Service (`assignment-service`)**:
     - Cổng dịch vụ: 8083.
     - Cấu hình đa nguồn dữ liệu: JPA MySQL 8.4 + Spring Data MongoDB.
     - Tích hợp Flyway migration: `V1__initial_schema.sql` (bảng `questions`, `quizzes`, `quiz_questions`, `quiz_attempts`, `quiz_attempt_answers`, `coding_problems`, `test_cases`) và `V2__add_indexes.sql` (tối ưu hóa chỉ mục tìm kiếm và khóa ảo chống spam bài thi).
   - **Khởi tạo Khung Phân Hệ Chatbot AI Service (`chatbot-service`)**:
     - Cổng dịch vụ: 8085.
     - Cấu hình Spring Boot WebFlux phản hồi Server-Sent Events (SSE).
     - Kết nối MongoDB lưu trữ `ChatSession` và `ChatMessage`.
     - Cấu hình Redis Cache phục vụ Rate Limiting.
     - Khởi tạo `@ConfigurationProperties` `GeminiProperties` hỗ trợ tích hợp API OpenCode / DeepSeek LLM.
3. **File thay đổi / tạo mới**:
   - `.gitignore`
   - `docker/mysql/init/01-create-databases.sql`
   - `docker-compose.db.yml`
   - `backend/pom.xml`
   - `backend/service-registry/pom.xml`, `application.yaml`, `ServiceRegistryApplication.java`, `ServiceRegistryApplicationTests.java`
   - `backend/api-gateway/pom.xml`, `application.yaml`, `ApiGatewayApplication.java`, `AuthFilter.java`, `JwtUtil.java`, `GatewayFallbackController.java`, `ErrorCode.java`, `GlobalErrorExceptionHandler.java`, `ApiGatewayApplicationTests.java`
   - `backend/assignment-service/pom.xml`, `application.yaml`, `AssignmentServiceApplication.java`, `V1__initial_schema.sql`, `V2__add_indexes.sql`, `AssignmentServiceApplicationTests.java`
   - `backend/chatbot-service/pom.xml`, `application.yaml`, `ChatbotServiceApplication.java`, `GeminiProperties.java`, `ChatbotServiceApplicationTests.java`
   - `docs/progress/member-1.md`
4. **Test đã chạy**:
   - Chạy `mvn clean compile -DskipTests` toàn bộ 4 module backend con: `BUILD SUCCESS` (10.682s, JDK 21.0.12).
   - Chạy `mvn test -pl assignment-service`: `BUILD SUCCESS` (1/1 test passed, time: 21.564s).
   - Chạy `mvn test -pl chatbot-service`: `BUILD SUCCESS` (1/1 test passed, time: 14.272s).
5. **Kết quả**:
   - Hoàn thành 100% mục tiêu Đợt 2: Toàn bộ nền tảng hạ tầng Monorepo và khung 2 microservice chủ trì của Thành viên 1 đã sẵn sàng để triển khai tầng nghiệp vụ (Business Logic).
6. **Commit**:
   - `feat(infra): thiet lap eureka api gateway va khung assignment chatbot` (Hash: `a02474a`)
7. **Việc tiếp theo (Đợt 3 — 03/10 đến 05/10)**:
   - Triển khai Ngân hàng câu hỏi trắc nghiệm (`Question`), Parser nhập CSV/TXT (`CsvQuestionParser`), Tạo đề thi Quiz (`QuizServiceImpl`) và cơ chế khóa ảo chống spam làm bài.
