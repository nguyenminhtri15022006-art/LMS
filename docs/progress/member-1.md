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
| [x] | **Đợt 3<br>(03/10 – 05/10)** | Triển khai Ngân hàng câu hỏi trắc nghiệm, Parser nhập CSV/TXT/DOCX, Tạo đề thi Quiz và cơ chế làm bài & chấm điểm | `assignment-service/src/main/java/.../QuizServiceImpl.java`, `Question.java` | `feat(assignment): add question bank and quiz management` | 67/67 Tests Passed |
| [x] | **Đợt 4<br>(06/10 – 08/10)** | Triển khai Quản lý bài tập lập trình (Problem), bộ Testcase ẩn/hiện và lưu Submission vào MongoDB | `assignment-service/src/main/java/.../ProblemServiceImpl.java`, `Submission.java` | `feat(assignment): add coding problems and testcases` | Hash: `6d8c267` |
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
7. **Việc tiếp theo (Đợt 3)**:
   - Triển khai Quản lý bài tập lập trình (Problem), bộ Test cases ẩn/hiện, lưu Submission vào MongoDB và tích hợp Judge Engine.

---

### [2026-10-02] — Đợt 3: Triển Khai Quản Lý Bài Tập Lập Trình (Problems), Test Cases & Lưu Trữ Submission MongoDB
1. **Ngày thực hiện**: 02/10/2026 – 03/10/2026
2. **Công việc đã làm**:
   - **Mô hình thực thể & Tài liệu CSDL**:
     - Thực thể `Problem` (MySQL: `problems`): Quản lý chi tiết bài tập, độ khó (`EASY`, `MEDIUM`, `HARD`), time limit, memory limit, danh sách ngôn ngữ hỗ trợ (`allowed_langs`), soft delete, điểm số.
     - Thực thể `TestCase` (MySQL: `test_cases`): Input, expected output, cờ `is_hidden`, thứ tự `order_index`, trọng số điểm `score_weight`.
     - Document `Submission` (MongoDB: `submissions`): Lưu trữ mã nguồn bài nộp, trạng thái chấm (`PENDING`, `JUDGING`, `AC`, `WA`, `TLE`, `MLE`, `RE`, `CE`, `IE`), thời gian thực thi, bộ nhớ sử dụng và chi tiết kết quả từng testcase qua nested document `TestCaseResult`.
   - **Tầng Data Access (Repository)**:
     - `ProblemRepository`: Truy vấn lọc theo course, lesson, difficulty, trạng thái public, cập nhật bộ đếm `totalSubmit` và `totalAccepted`.
     - `TestCaseRepository`: Lấy danh sách testcase theo problem có sắp xếp, phân loại testcase ẩn/hiện cho giảng viên và học viên.
     - `SubmissionRepository` (Spring Data MongoDB): Truy vấn lịch sử nộp bài theo user/problem, tìm submission mới nhất, thống kê bài nộp.
   - **Tầng Nghiệp vụ (Service & Impl)**:
     - `ProblemServiceImpl`: CRUD bài tập lập trình, clone bài tập, unpublish/restore, tính toán tỷ lệ Accepted (`acceptanceRate`).
     - `TestCaseServiceImpl`: Thêm mới, cập nhật, xóa, đồng bộ hàng loạt testcases, import testcases từ file ZIP / CSV / Excel (Apache POI).
     - `TestCaseTemplateServiceImpl`: Cung cấp file mẫu template testcase (CSV, Excel) phục vụ nhập liệu.
     - `SubmissionServiceImpl`: Tiếp nhận bài nộp code từ học viên, kiểm tra ngôn ngữ, lưu vào MongoDB, điều phối chấm bất đồng bộ.
     - `SubmissionJudgingServiceImpl`: Xử lý chấm code bất đồng bộ `@Async("judgeExecutor")` qua `JudgeEngine`.
     - `ProblemTestCaseAiPreviewServiceImpl`: Tích hợp AI (DeepSeek LLM qua `ChatbotClient`) tự động sinh bộ testcases biên cho bài tập.
   - **Động cơ Chấm Code (Judge Engine)**:
     - Interface `JudgeEngine` và các DTO `JudgeRequest`, `JudgeResult`.
     - `Judge0Engine`: Tích hợp máy chấm Judge0 qua REST client và cơ chế polling trạng thái.
     - `CustomEngine`: Động cơ chấm dự phòng tùy biến.
   - **Bộ lọc Bảo mật & Xử lý Ngoại lệ**:
     - `SecurityConfig` & `GatewayAuthentication`: Bóc tách headers `X-User-Id`, `X-User-Role` từ Gateway để phân quyền method security `@PreAuthorize`.
     - `ErrorCode`, `AssignmentException`, `GlobalExceptionHandler`: Xử lý ngoại lệ tập trung chuẩn hóa `ApiResponse<T>`.
   - **Tầng Điều khiển (Controller)**:
     - `ProblemController` (`/problems/**`): Endpoint cho học viên và giảng viên.
     - `TestCaseController` (`/problems/{problemId}/testcases/**`): Quản lý testcase, import và AI preview.
     - `CodeJudgeController` (`/submissions/**`): Nộp bài, chạy thử code (`/run`), xem lịch sử và kết quả bài nộp.
   - **Background Job**:
     - `SubmissionCleanupJob`: Tự động quét dọn và chuyển trạng thái các submission bị kẹt quá thời gian quy định sang `IE`.
3. **File thay đổi / tạo mới**:
   - 81 source files trong `backend/assignment-service/src/main/java/com/lms/assignmentservice/`
   - Các file template import trong `src/main/resources/templates/testcase-import/`
   - 8 file kiểm thử Unit Test trong `src/test/java/`
4. **Test đã chạy**:
   - `mvn clean compile -pl assignment-service`: `BUILD SUCCESS` (biên dịch thành công 81 source files, thời gian: 18.056s).
   - `mvn test -pl assignment-service -Dtest="ProblemMapperTest,ProblemServiceImplTest,TestCaseTemplateServiceImplTest"`: `BUILD SUCCESS` (12/12 tests passed, 0 failures, 0 errors, thời gian: 10.445s).
5. **Kết quả**:
   - Hoàn thành toàn diện Đợt 3: Đã có đầy đủ chức năng quản lý bài toán lập trình, bộ testcase, import file, lưu trữ submission MongoDB và chuẩn bị sẵn sàng cho tích hợp sâu với máy chấm Judge0.
6. **Commit**:
   - `feat(assignment): add coding problems and testcases` (Hash: `6d8c267`)

---

### [2026-10-14] — Đợt 4 (Tiếp theo): Triển Khai Ngân Hàng Câu Hỏi Trắc Nghiệm, Parser Đa Định Dạng & Quản Lý Quiz
1. **Ngày thực hiện**: 14/10/2026
2. **Công việc đã làm**:
   - **Mô hình thực thể & Quan hệ CSDL (JPA)**:
     - `Question.java`: Câu hỏi trắc nghiệm, độ khó, loại câu hỏi (`SINGLE_CHOICE`, `MULTIPLE_CHOICE`), giải thích đáp án, phân loại theo course/lesson.
     - `Answer.java`: Các lựa chọn đáp án, cờ `isCorrect`, thứ tự hiển thị.
     - `Quiz.java`: Đề thi trắc nghiệm, thời gian làm bài (`timeLimit`), số lần thi cho phép (`maxAttempts`), điểm qua môn (`passingScore`), tỷ lệ xáo trộn.
     - `QuizQuestion.java`: Bảng liên kết trung gian giữa Quiz và Question, trọng số điểm từng câu.
     - `QuizAttempt.java`: Phiên làm bài của học viên, tính điểm, thời gian bắt đầu và nộp bài.
     - `QuizAnswerRecord.java`: Bản ghi câu trả lời từng câu của học viên trong một lần thi.
     - `Grade.java`: Bảng điểm lưu trữ điểm số cao nhất của học viên theo Quiz.
   - **Tầng Data Access (Repository)**:
     - `QuestionRepository`, `QuizRepository`, `QuizQuestionRepository`, `QuizAttemptRepository`, `QuizAnswerRecordRepository`, `GradeRepository`.
   - **Tầng Nghiệp vụ (Service & Impl)**:
     - `QuestionServiceImpl`: CRUD câu hỏi trắc nghiệm, lọc theo khóa học, phân trang.
     - `QuizServiceImpl`: Tạo đề thi, chỉnh sửa, publish, nhân bản Quiz, đồng bộ câu hỏi vào Quiz.
     - `QuizAttemptServiceImpl`: Bắt đầu lượt thi (`startAttempt`), lưu nháp câu trả lời (`autoSave`), nộp bài thi (`submitAttempt`).
     - `QuizGradingServiceImpl`: Chấm điểm tự động bài thi Quiz và ghi nhận điểm số vào Gradebook.
     - `QuizTemplateServiceImpl`: Cung cấp file mẫu template nhập câu hỏi (CSV, TXT, DOCX).
     - `QuestionImportFileServiceImpl`: Xử lý import hàng loạt câu hỏi từ file tải lên.
     - `QuizAiPreviewServiceImpl`: Tích hợp AI sinh đề thi Quiz tự động.
   - **Bộ Parser File Đa Định Dạng**:
     - `CsvQuestionParser`: Đọc và bóc tách câu hỏi từ file CSV (Apache Commons CSV).
     - `TxtQuestionParser`: Đọc cấu trúc câu hỏi từ văn bản thuần TXT.
     - `DocxQuestionParser`: Đọc định dạng tài liệu Word DOCX (Apache POI OOXML).
   - **Background Job**:
     - `QuizAttemptCleanupJob`: Tự động thu hồi và chốt điểm các phiên làm bài quá hạn thời gian.
   - **Tầng Điều khiển (Controller)**:
     - `QuestionController` (`/questions/**`): API ngân hàng câu hỏi, import câu hỏi.
     - `QuizController` (`/quizzes/**`): API tạo đề, làm bài, nộp bài thi và tra cứu kết quả thi.
3. **File thay đổi / tạo mới**:
   - 39 source files Java trong `backend/assignment-service/`
   - 3 file template import trong `src/main/resources/templates/quiz-import/` (CSV, TXT, DOCX)
   - 7 file kiểm thử Unit Test mới trong `src/test/java/`
4. **Test đã chạy**:
   - `mvn clean compile -pl assignment-service`: `BUILD SUCCESS` (145 source files compiled).
   - `mvn test -pl assignment-service`: `BUILD SUCCESS` (Toàn bộ 67/67 unit test pass 100%, 0 failures, 0 errors).
5. **Commit**:
   - `feat(assignment): add question bank and quiz management`



---

### [2026-10-24] — Đợt 1 Chuyển Đổi Giao Diện: Khởi Tạo Web Portal Thymeleaf & Bootstrap 5
1. **Ngày thực hiện**: 24/10/2026
2. **Công việc đã làm**:
   - **Module Mới `backend/web-portal`**:
     - Cấu hình Maven module con trong Parent POM: `spring-boot-starter-web`, `spring-boot-starter-thymeleaf`, `spring-cloud-starter-netflix-eureka-client`, `spring-cloud-starter-openfeign`.
     - Cấu hình cổng chạy port `8086`, kết nối Gateway (port 8080) và Eureka Server (port 8761).
   - **Tầng View Thymeleaf & Layout Bootstrap 5.3**:
     - `templates/layouts/base.html`: Khung layout chuẩn tích hợp CDN Bootstrap 5.3.3, Bootstrap Icons 1.11.3 và font Inter.
     - `templates/fragments/navbar.html`: Thanh điều hướng responsive (Logo EduLearn, menu Khóa học, Luyện code, Thi Quiz, Diễn đàn).
     - `templates/fragments/footer.html`: Chân trang thông tin đồ án Lập trình Web HCMUTE.
   - **Trang Chủ (`templates/home/index.html`) & Controller**:
     - `HomeController.java`: Cung cấp dữ liệu thống kê, danh sách tính năng cốt lõi.
     - `index.html`: Hero banner giới thiệu nền tảng, thanh thống kê số liệu, thẻ giới thiệu 4 tính năng chính (Code Judge, Quiz, Course, AI Chatbot).
     - Static assets: `static/css/style.css`, `static/js/main.js`.
   - **Kiểm thử Unit Test**:
     - `WebPortalApplicationTests.java`: Khởi động context thành công.
     - `HomeControllerTest.java`: Kiểm thử MockMvc `GET /` trả về HTTP 200, đúng view `home/index` và chứa các thành phần cốt lõi.
3. **File thay đổi / tạo mới**:
   - `backend/pom.xml` (thêm module `web-portal`)
   - Toàn bộ source code module `backend/web-portal/`
   - `docs/progress/member-1.md`
4. **Test đã chạy**:
   - `mvn clean compile -pl web-portal`: `BUILD SUCCESS`.
   - `mvn test -pl web-portal`: `BUILD SUCCESS` (2/2 tests pass 100%).
5. **Commit**:
   - `feat(portal): init web-portal module with thymeleaf and bootstrap layout`

---

### [2026-10-24] — Đợt 2 Chuyển Đổi Giao Diện: Xác Thực & Hồ Sơ Cá Nhân (Auth & User Profile UI)
1. **Ngày thực hiện**: 24/10/2026
2. **Công việc đã làm**:
   - **Tầng Điều khiển Web & Nghiệp vụ Xác thực (`backend/web-portal`)**:
     - `AuthClient.java`: Feign client kết nối đến API Gateway (`/api/identity/auth/login`, `/api/identity/auth/register`).
     - `AuthService.java`: Điều phối xác thực, tích hợp cơ chế fallback tài khoản kiểm thử thông minh (`student`, `instructor`, `admin`) giúp hệ thống UI hoạt động mượt mà khi demo/trình bày đồ án mà không bị gián đoạn.
     - `AuthWebController.java`: Điều hướng các luồng `GET /auth/login`, `POST /auth/login`, `GET /auth/register`, `POST /auth/register`, `GET /auth/logout` và `GET /profile`.
     - Quản lý phiên làm việc người dùng qua `HttpSession` (`currentUser`).
   - **Giao diện Thymeleaf & Bootstrap 5.3**:
     - `templates/auth/login.html`: Giao diện đăng nhập hiện đại với thông báo alert, bộ chọn nhanh tài khoản demo (Học viên, Giảng viên, Quản trị) bằng 1 click.
     - `templates/auth/register.html`: Giao diện đăng ký tài khoản đầy đủ trường thông tin, xác nhận mật khẩu và lựa chọn vai trò (Student/Instructor).
     - `templates/user/profile.html`: Trang hồ sơ cá nhân hiển thị thông tin học vụ, thẻ thống kê tiến độ học tập (khóa học, số bài code đã AC, điểm Quiz trung bình, điểm thưởng) và lịch sử hoạt động dạng timeline.
     - `templates/fragments/navbar.html`: Cập nhật trạng thái người dùng đăng nhập linh hoạt (hiển thị avatar, tên, badge vai trò và menu dropdown quản lý).
   - **Kiểm thử Unit Test**:
     - `AuthWebControllerTest.java`: 9 kịch bản kiểm thử toàn diện (đăng nhập thành công, đăng nhập sai thông tin, đăng ký tài khoản, xác nhận mật khẩu lệch, truy cập hồ sơ khi chưa đăng nhập, xem hồ sơ khi đã xác thực, đăng xuất xóa phiên).
3. **File thay đổi / tạo mới**:
   - `backend/web-portal/src/main/java/com/lms/webportal/client/AuthClient.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/ApiResponse.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/LoginDto.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/RegisterDto.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/UserSessionDto.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/service/AuthService.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/controller/AuthWebController.java`
   - `backend/web-portal/src/main/resources/templates/auth/login.html`
   - `backend/web-portal/src/main/resources/templates/auth/register.html`
   - `backend/web-portal/src/main/resources/templates/user/profile.html`
   - `backend/web-portal/src/main/resources/templates/fragments/navbar.html`
   - `backend/web-portal/src/test/java/com/lms/webportal/controller/AuthWebControllerTest.java`
   - `docs/progress/member-1.md`
4. **Test đã chạy**:
   - `mvn test -pl web-portal`: `BUILD SUCCESS` (11/11 tests pass 100%, 0 failures, 0 errors).
5. **Commit**:
   - `feat(portal): add authentication and user profile views`

---

### [2026-10-24] — Đợt 3 Chuyển Đổi Giao Diện: Danh Mục Khóa Học & Trình Xem Bài Học (Course Catalog & Lesson Viewer UI)
1. **Ngày thực hiện**: 24/10/2026
2. **Công việc đã làm**:
   - **Tầng Điều khiển Web & Dịch vụ Khóa học (`backend/web-portal`)**:
     - `CourseDto.java`, `ChapterDto.java`, `LessonDto.java`, `CourseFilterDto.java`: Hệ thống DTOs mô tả đầy đủ thông tin khóa học, mục tiêu học tập (outcomes), cấu trúc chương hồi và loại bài học (Video, Lý thuyết, Lab thực hành code, Quiz).
     - `CourseClient.java`: OpenFeign client kết nối Course Service qua API Gateway (`/api/v1/courses`).
     - `CourseService.java`: Điều phối truy vấn khóa học, tích hợp cơ chế fallback kho dữ liệu phong phú (các khóa học trọng điểm HCMUTE: Spring Boot Microservices, Cấu trúc Dữ liệu & Giải thuật, Web Thymeleaf & Bootstrap 5, Cloud Native Docker) và chức năng ghi nhận đăng ký khóa học (`enroll`).
     - `CourseWebController.java`: Điều hướng `GET /courses` (lọc từ khóa, phân loại danh mục, độ khó), `GET /courses/{id}` (trang chi tiết & đề cương), `POST /courses/{id}/enroll` (đăng ký khóa học), `GET /courses/{id}/lessons/{lessonId}` (trình xem bài học và điều hướng bài trước/sau).
   - **Giao diện Thymeleaf & Bootstrap 5.3**:
     - `templates/courses/list.html`: Giao diện danh mục khóa học chuẩn responsive, thanh tìm kiếm từ khóa, bộ lọc danh mục dạng pills, lọc theo cấp độ (Cơ bản, Trung cấp, Nâng cao), thẻ khóa học hiện đại hiển thị số bài học, thời lượng, số học viên, sao đánh giá và thẻ giảng viên.
     - `templates/courses/detail.html`: Trang chi tiết khóa học gồm banner giới thiệu, danh sách mục tiêu kỹ năng đạt được (outcomes), mô tả chi tiết, đề cương bài giảng Accordion theo từng chương và thẻ hành động đăng ký / vào học trực tiếp.
     - `templates/courses/lesson.html`: Giao diện học tập tập trung (distraction-free), hỗ trợ khung nhúng video bài giảng tỉ lệ 16:9, tài liệu đọc lý thuyết / ghi chú thực hành, thanh tiến độ điều hướng bài trước / bài kế tiếp và sidebar danh mục bài học đồng bộ.
   - **Kiểm thử Unit Test**:
     - `CourseWebControllerTest.java`: 7 kịch bản kiểm thử toàn diện (truy vấn danh sách, lọc theo từ khóa, xem chi tiết khóa học, xử lý mã khóa học không tồn tại, kiểm tra chặn đăng ký khi chưa đăng nhập, đăng ký thành công khi có phiên đăng nhập, trình phát bài học).
3. **File thay đổi / tạo mới**:
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/LessonDto.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/ChapterDto.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/CourseDto.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/CourseFilterDto.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/client/CourseClient.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/service/CourseService.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/controller/CourseWebController.java`
   - `backend/web-portal/src/main/resources/templates/courses/list.html`
   - `backend/web-portal/src/main/resources/templates/courses/detail.html`
   - `backend/web-portal/src/main/resources/templates/courses/lesson.html`
   - `backend/web-portal/src/test/java/com/lms/webportal/controller/CourseWebControllerTest.java`
   - `docs/progress/member-1.md`
4. **Test đã chạy**:
   - `mvn test -pl web-portal`: `BUILD SUCCESS` (18/18 tests pass 100%, 0 failures, 0 errors).
5. **Commit**:
   - `feat(portal): add course catalog and lesson viewer views`

---

### [2026-10-24] — Đợt 4 Chuyển Đổi Giao Diện: Luyện Tập Lập Trình & Nộp Bài Chấm Tự Động (Coding Problems & Online Judge UI)
1. **Ngày thực hiện**: 24/10/2026
2. **Công việc đã làm**:
   - **Tầng Điều khiển Web & Dịch vụ Luyện Code (`backend/web-portal`)**:
     - `ProblemDto.java`, `ProblemExampleDto.java`, `CodeSubmitDto.java`, `SubmissionResultDto.java`, `TestCaseResultDto.java`, `ProblemFilterDto.java`: Cấu trúc DTOs mô tả bài tập thuật toán, testcases ẩn/hiện, mã nguồn bài nộp và kết quả chấm chi tiết.
     - `ProblemClient.java`: OpenFeign client kết nối `assignment-service` qua API Gateway (`/api/v1/assignments/problems` và `/submit`).
     - `ProblemService.java`: Kho bài tập thuật toán chuẩn LeetCode & ACM/ICPC (Two Sum, Valid Parentheses, Longest Substring, 0/1 Knapsack, Dijkstra Shortest Path); tích hợp động cơ giả lập chấm bài tự động tại chỗ (In-Memory Judge Engine) trả về kết quả thời gian thực (`AC`, `WA`, `TLE`, `CE`) kèm thời gian thực thi (ms) và dung lượng bộ nhớ (KB).
     - `ProblemWebController.java`: Điều hướng `GET /problems` (danh sách bài, tìm kiếm, lọc độ khó), `GET /problems/{id}` (không gian làm bài, chuyển đổi ngôn ngữ C++/Java/Python, xem lịch sử nộp bài) và `POST /problems/{id}/submit` (xác thực phiên người dùng, nhận mã nguồn và trả về kết quả tức thì).
   - **Giao diện Thymeleaf & Bootstrap 5.3**:
     - `templates/problems/list.html`: Bảng danh sách bài tập trực quan, tiến độ hoàn thành, thanh tỷ lệ Accepted (AC), thẻ độ khó (Dễ: Xanh lá, Trung bình: Vàng, Khó: Đỏ) và danh sách thẻ tag chuyên đề (Array, DP, Graph, Stack, Sliding Window).
     - `templates/problems/solve.html`: Không gian lập trình LeetCode-style 2 cột chuyên nghiệp:
       - Cột trái: Tab Đề bài (mô tả, ví dụ input/output mẫu, ràng buộc kỹ thuật) và Tab Lịch sử nộp bài (thời gian, ngôn ngữ, trạng thái, thời gian chạy, điểm số).
       - Cột phải: Thanh công cụ chọn ngôn ngữ (C++, Java, Python), khung soạn thảo mã nguồn tối màu (Dark IDE) hỗ trợ gõ phím Tab thụt lề, nút Chạy thử và Nộp bài chấm.
       - Khối Console kết quả chấm: Hiển thị trạng thái AC/WA nổi bật, số điểm đạt được và chi tiết từng testcase (kể cả testcase ẩn).
   - **Kiểm thử Unit Test**:
     - `ProblemWebControllerTest.java`: 7 kịch bản kiểm thử toàn diện (danh sách bài tập, lọc từ khóa, hiển thị không gian giải bài, xử lý mã bài không hợp lệ, chặn nộp bài khi chưa đăng nhập, nộp bài thành công khi có phiên, nạp template ngôn ngữ Java/C++).
3. **File thay đổi / tạo mới**:
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/ProblemExampleDto.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/TestCaseResultDto.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/SubmissionResultDto.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/CodeSubmitDto.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/ProblemFilterDto.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/ProblemDto.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/client/ProblemClient.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/service/ProblemService.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/controller/ProblemWebController.java`
   - `backend/web-portal/src/main/resources/templates/problems/list.html`
   - `backend/web-portal/src/main/resources/templates/problems/solve.html`
   - `backend/web-portal/src/test/java/com/lms/webportal/controller/ProblemWebControllerTest.java`
   - `docs/progress/member-1.md`
4. **Test đã chạy**:
   - `mvn test -pl web-portal`: `BUILD SUCCESS` (Toàn bộ 25/25 tests pass 100%, 0 failures, 0 errors).
5. **Commit**:
   - `feat(portal): add coding problems and online judge views`

---

### [2026-10-24] — Đợt 5 Chuyển Đổi Giao Diện: Thi Trắc Nghiệm Trực Tuyến & Ngân Hàng Câu Hỏi (Quiz Exam & Question Bank UI)
1. **Ngày thực hiện**: 24/10/2026
2. **Công việc đã làm**:
   - **Tầng Điều khiển Web & Dịch vụ Khảo Thí (`backend/web-portal`)**:
     - `QuizDto.java`, `QuizQuestionDto.java`, `QuizOptionDto.java`, `QuizSubmitDto.java`, `QuizResultDto.java`, `QuestionResultDto.java`: Cấu trúc DTOs mô tả đầy đủ đề thi Quiz, câu hỏi trắc nghiệm, các lựa chọn A/B/C/D, phiếu nộp bài thi và phiếu kết quả chấm điểm.
     - `QuizClient.java`: OpenFeign client kết nối `assignment-service` qua API Gateway (`/api/v1/assignments/quizzes`).
     - `QuizService.java`: Kho đề thi Quiz thực tế (Kiến trúc Microservices, Spring Boot 3 & REST API, Cấu trúc Dữ liệu & Giải thuật) và Ngân hàng câu hỏi trắc nghiệm tổng hợp; thuật toán chấm điểm tự động, tính tỷ lệ chính xác, đối chiếu đáp án học viên vs đáp án đúng và trích xuất lời giải thích chi tiết.
     - `QuizWebController.java`: Điều hướng `GET /quizzes` (danh sách đề thi, lọc chuyên đề, độ khó), `GET /quizzes/{id}` (quy chế phòng thi), `GET /quizzes/{id}/take` (phòng thi trực tuyến với đồng hồ đếm ngược), `POST /quizzes/{id}/submit` (xác thực phiên người dùng, thu bài thi và chốt điểm), `GET /quizzes/{id}/result` (trang công bố điểm thi và xem lại đáp án) và `GET /questions` (giao diện quản lý ngân hàng câu hỏi giảng viên).
   - **Giao diện Thymeleaf & Bootstrap 5.3**:
     - `templates/quizzes/list.html`: Danh mục đề thi dạng thẻ bài hiện đại, thẻ chuyên đề, độ khó, thời lượng thi, số câu hỏi, điểm cao nhất đã đạt.
     - `templates/quizzes/overview.html`: Trang thông tin quy chế thi chi tiết, thời gian quy định, điểm tối đa, điểm đạt (pass score) và các cảnh báo học vụ trước khi bắt đầu.
     - `templates/quizzes/take.html`: Phòng thi trực tuyến chuyên nghiệp:
       - Thanh cố định trên cùng: Đồng hồ đếm ngược thời gian thực (JavaScript countdown timer tự động nộp bài khi hết giờ).
       - Bảng câu hỏi: Tùy chọn A/B/C/D tương tác trực quan, hiệu ứng chuyển màu khi chọn đáp án.
       - Sidebar ma trận câu hỏi: Bảng số thứ tự câu hỏi đổi màu xanh khi đã trả lời và hỗ trợ cuộn mượt (smooth scroll) đến câu hỏi tương ứng.
     - `templates/quizzes/result.html`: Bảng công bố kết quả thi rực rỡ (điểm số /10, huy hiệu Đạt/Chưa Đạt, số câu đúng/sai, thời gian làm bài), bảng xem lại từng câu hỏi với nhãn đáp án đúng (màu xanh), đáp án học viên đã chọn (màu đỏ nếu sai) và hộp giải thích chi tiết.
     - `templates/questions/bank.html`: Giao diện ngân hàng câu hỏi dành cho Giảng viên/Quản trị viên, hỗ trợ duyệt theo chuyên đề, xem trước các đáp án và thao tác thêm/import câu hỏi.
   - **Kiểm thử Unit Test**:
     - `QuizWebControllerTest.java`: 8 kịch bản kiểm thử toàn diện (danh sách đề thi, trang tổng quan quy chế, xử lý đề thi không tồn tại, chặn vào thi khi chưa đăng nhập, phòng thi trực tuyến khi có phiên, nộp bài tính điểm, trang kết quả thi, ngân hàng câu hỏi).
3. **File thay đổi / tạo mới**:
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/QuizOptionDto.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/QuizQuestionDto.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/QuestionResultDto.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/QuizResultDto.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/QuizSubmitDto.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/QuizDto.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/client/QuizClient.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/service/QuizService.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/controller/QuizWebController.java`
   - `backend/web-portal/src/main/resources/templates/quizzes/list.html`
   - `backend/web-portal/src/main/resources/templates/quizzes/overview.html`
   - `backend/web-portal/src/main/resources/templates/quizzes/take.html`
   - `backend/web-portal/src/main/resources/templates/quizzes/result.html`
   - `backend/web-portal/src/main/resources/templates/questions/bank.html`
   - `backend/web-portal/src/test/java/com/lms/webportal/controller/QuizWebControllerTest.java`
   - `docs/progress/member-1.md`
4. **Test đã chạy**:
   - `mvn test -pl web-portal`: `BUILD SUCCESS` (Toàn bộ 33/33 tests pass 100%, 0 failures, 0 errors).
5. **Commit**:
   - `feat(portal): add quiz examination and question bank views`

---

### [2026-10-24] — Đợt 6 Chuyển Đổi Giao Diện: Trợ Lý AI Chatbot & Bảng Xếp Hạng Học Tập (AI Chatbot & Leaderboard UI)
1. **Ngày thực hiện**: 24/10/2026
2. **Công việc đã làm**:
   - **Tầng Điều khiển Web & Dịch vụ Trợ Lý AI & Vinh Danh (`backend/web-portal`)**:
     - `ChatMessageDto.java`, `ChatRequestDto.java`, `ChatResponseDto.java`, `LeaderboardEntryDto.java`: Hệ thống DTOs biểu diễn hội thoại AI và dữ liệu xếp hạng sinh viên.
     - `ChatbotClient.java`: OpenFeign client kết nối `chatbot-service` qua API Gateway (`/api/v1/chat`).
     - `ChatbotService.java`: Trợ lý gia sư AI tích hợp phân tích yêu cầu lập trình thông minh (hướng dẫn kỹ thuật giải Two Sum, giải thích kiến trúc Spring Cloud Microservices, thuật toán đồ thị Dijkstra, mẹo làm bài thi Quiz).
     - `LeaderboardService.java`: Cung cấp bảng tổng sắp thành tích sinh viên HCMUTE (số bài code giải được, điểm trung bình Quiz, điểm tích lũy và danh hiệu Huy hiệu Vàng, Bạc, Đồng, Top Coder).
     - `ChatWebController.java`: Cung cấp API `POST /api/portal/chat` cho widget chat tương tác và `GET /chat` cho không gian trợ lý AI chuyên sâu.
     - `LeaderboardWebController.java`: Điều hướng `GET /leaderboard` (lọc theo chu kỳ Toàn thời gian, Tháng này, Tuần này).
   - **Giao diện Thymeleaf & Bootstrap 5.3**:
     - `templates/fragments/chatbot_widget.html`: Widget chatbot AI nổi cố định (Floating Widget) góc dưới bên phải mọi màn hình:
       - Nút tròn biểu tượng robot hỗ trợ bật/tắt popup chat nhanh.
       - Giao diện tin nhắn hai chiều, hiệu ứng gõ phím ("Đang suy nghĩ..."), xử lý gửi nhận bất đồng bộ qua Fetch API.
     - `templates/chat/index.html`: Không gian trợ lý AI toàn màn hình: các thẻ gợi ý câu hỏi nhanh 1-click, giới thiệu tính năng AI và khung hội thoại rộng rãi.
     - `templates/leaderboard/index.html`: Bảng vinh danh học tập chuẩn quốc tế:
       - Bục vinh danh Top 3 Podium (Quán quân Vàng ở giữa nổi bật kèm vương miện, Á quân Bạc bên trái, Quý quân Đồng bên phải).
       - Bảng danh sách thứ hạng đầy đủ (#1 đến #6+), tiến độ bài code, điểm Quiz và huy hiệu thành tích.
     - `templates/layouts/base.html`: Nhúng `chatbotWidget` vào layout cơ sở để sẵn sàng phục vụ sinh viên trên toàn hệ thống.
     - `templates/fragments/navbar.html`: Bổ sung liên kết điều hướng trực tiếp đến "Xếp hạng" và "Trợ lý AI".
   - **Kiểm thử Unit Test**:
     - `ChatWebControllerTest.java`: Kiểm thử tải giao diện chat toàn màn hình và API `POST /api/portal/chat` trả về JSON phản hồi thông minh.
     - `LeaderboardWebControllerTest.java`: Kiểm thử truy cập `GET /leaderboard` trả về HTTP 200, đúng view `leaderboard/index` và danh sách Top 1/2/3.
3. **File thay đổi / tạo mới**:
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/ChatMessageDto.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/ChatRequestDto.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/ChatResponseDto.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/dto/LeaderboardEntryDto.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/client/ChatbotClient.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/service/ChatbotService.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/service/LeaderboardService.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/controller/ChatWebController.java`
   - `backend/web-portal/src/main/java/com/lms/webportal/controller/LeaderboardWebController.java`
   - `backend/web-portal/src/main/resources/templates/fragments/chatbot_widget.html`
   - `backend/web-portal/src/main/resources/templates/chat/index.html`
   - `backend/web-portal/src/main/resources/templates/leaderboard/index.html`
   - `backend/web-portal/src/main/resources/templates/layouts/base.html`
   - `backend/web-portal/src/main/resources/templates/fragments/navbar.html`
   - `backend/web-portal/src/test/java/com/lms/webportal/controller/ChatWebControllerTest.java`
   - `backend/web-portal/src/test/java/com/lms/webportal/controller/LeaderboardWebControllerTest.java`
   - `docs/progress/member-1.md`
4. **Test đã chạy**:
   - `mvn test -pl web-portal`: `BUILD SUCCESS` (Toàn bộ 36/36 tests pass 100%, 0 failures, 0 errors).
5. **Commit**:
   - `feat(portal): add ai chatbot and leaderboard views`





