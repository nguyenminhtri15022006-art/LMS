# Nhật Ký Tiến Độ Cá Nhân — Thành Viên 1 (member-1.md)

- **Họ và tên**: Nguyễn Minh Trí
- **MSSV**: 23110340
- **Vai trò**: Trưởng nhóm / Fullstack Developer (Chủ trì khối Nghiệp vụ Phức tạp & Công nghệ Cốt lõi)
- **Phụ trách chính**: `assignment-service` (Quiz, Question Bank, Code Judge), `chatbot-service` (LLM AI), Dashboard, Leaderboard, Gradebook, Trình chấm code Sandbox Judge0.

---

### [2026-09-27] — Khảo Sát Dự Án & Thiết Lập Bộ Tài Liệu Kỹ Thuật Ban Đầu

1. **Ngày thực hiện**: 27/09/2026
2. **Công việc đã làm**:
   - Khảo sát toàn diện kiến trúc Monorepo Microservices của project mẫu: phân tích `pom.xml`, `docker-compose.yml`, các controllers, entities và routes frontend.
   - Xác nhận sự cô lập và tính toàn vẹn của thư mục project mẫu (chỉ đọc) và thư mục phát triển `DoAnCK`.
   - Kiểm tra trạng thái Git ban đầu của dự án.
   - Soạn thảo và khởi tạo toàn bộ bộ tài liệu kỹ thuật cốt lõi: `README.md`, `AGENTS.md`, `CONTRIBUTING.md`, `CHANGELOG.md`, `.env.example`, `ARCHITECTURE.md`, `REQUIREMENTS.md`, `USER_ROLES.md`, `TEAM_ROLES.md`, `ROADMAP.md`, `COMMIT_PLAN.md`, `GIT_WORKFLOW.md`, `CODING_RULES.md`, `TESTING_PLAN.md`, `DEPLOYMENT_GUIDE.md`, `RUBRIC_MAPPING.md`, `OPEN_QUESTIONS.md`, `DECISIONS.md`.
3. **File thay đổi**:
   - `README.md`
   - `AGENTS.md`
   - `CONTRIBUTING.md`
   - `CHANGELOG.md`
   - `.env.example`
   - `docs/README.md`
   - `docs/PROJECT_OVERVIEW.md`
   - `docs/REQUIREMENTS.md`
   - `docs/FEATURE_PARITY_MATRIX.md`
   - `docs/ARCHITECTURE.md`
   - `docs/DATABASE_DESIGN.md`
   - `docs/API_CONTRACT.md`
   - `docs/USER_ROLES.md`
   - `docs/TEAM_ROLES.md`
   - `docs/ROADMAP.md`
   - `docs/COMMIT_PLAN.md`
   - `docs/GIT_WORKFLOW.md`
   - `docs/CODING_RULES.md`
   - `docs/TESTING_PLAN.md`
   - `docs/DEPLOYMENT_GUIDE.md`
   - `docs/RUBRIC_MAPPING.md`
   - `docs/OPEN_QUESTIONS.md`
   - `docs/DECISIONS.md`
   - `docs/progress/README.md`
   - `docs/progress/member-1.md`
   - `docs/progress/member-2.md`
   - `docs/progress/integration.md`
4. **Test đã chạy**:
   - Kiểm tra liên kết tệp tin và cú pháp sơ đồ Mermaid trong tất cả các file tài liệu.
   - Chạy lệnh kiểm tra `git status` tại thư mục `DoAnCK`.
5. **Kết quả**:
   - 24 tài liệu được tạo mới thành công, không có lỗi định dạng markdown.
   - Xác nhận `DoAnCK` sẵn sàng bước vào giai đoạn thiết lập khung mã nguồn.
6. **Commit**:
   - `docs: hoan thien ho so ky thuat rubric HCMUTE va phan cong Tri chu tri assignment chatbot` (Hash: `7a69d45`)
7. **Khó khăn & Thách thức**:
   - Phát hiện project mẫu thiếu thư mục `docker/mysql/init` dù có khai báo volume trong `docker-compose.yml`, đã kịp thời ghi nhận quyết định tạo thư mục bù đắp tại `DECISIONS.md`.
8. **Việc tiếp theo**:
   - Khởi tạo Maven Parent POM, Eureka Server, Spring Cloud Gateway, khung Assignment Service & Chatbot Service.

---

### [2026-09-27] — Thiết Lập Hạ Tầng Backend Monorepo & Khung Phân Hệ Assignment, Chatbot

1. **Ngày thực hiện**: 27/09/2026
2. **Công việc đã làm**:
   - Tạo file `.gitignore` toàn diện cho hệ thống Monorepo (bỏ qua artifacts compile, node_modules, IDE, file môi trường bí mật).
   - Thiết lập hạ tầng Docker: `docker/mysql/init/01-create-databases.sql` khởi tạo tự động 4 CSDL (`lms_identity`, `lms_course`, `lms_assignment`, `lms_blog`) khắc phục triệt để lỗi ADR-005 của project mẫu.
   - Viết `docker-compose.db.yml` quản lý 4 container dịch vụ dữ liệu: MySQL 8.4, MongoDB 7.0, Redis 7.2, MinIO S3.
   - Thiết lập Maven Parent `backend/pom.xml` quản lý đồng bộ phiên bản: Spring Boot 3.3.5, Spring Cloud 2023.0.1, Java 21, Lombok, MapStruct, JJWT.
   - Khởi tạo module `service-registry` (Eureka Server 8761) với package chuẩn `com.lms.serviceregistry` (khắc phục ADR-006).
   - Khởi tạo module `api-gateway` (Spring Cloud Gateway 8080) đầy đủ cấu hình định tuyến microservices, `AuthFilter` thực hiện Token Offloading, `JwtUtil`, Circuit Breaker fallback và `GlobalErrorExceptionHandler`.
   - Khởi tạo khung `assignment-service` (cổng 8083, Spring Boot 3.3.5, JPA MySQL, MongoDB, Resilience4j, MinIO, Flyway migration V1 & V2).
   - Khởi tạo khung `chatbot-service` (cổng 8085, Spring Boot 3.3.5, WebFlux SSE, MongoDB, Redis, GeminiProperties cấu hình LLM DeepSeek/OpenCode).
3. **File thay đổi / tạo mới**:
   - `.gitignore`
   - `docker/mysql/init/01-create-databases.sql`
   - `docker-compose.db.yml`
   - `backend/pom.xml`
   - `backend/service-registry/*`
   - `backend/api-gateway/*`
   - `backend/assignment-service/*`
   - `backend/chatbot-service/*`
   - `docs/progress/member-1.md`
4. **Test đã chạy**:
   - Chạy `mvn clean compile -DskipTests` toàn bộ 4 module backend con.
   - Kết quả: `BUILD SUCCESS` hoàn hảo trong 49.167s với JDK 21.0.12.
5. **Kết quả**:
   - Toàn bộ khung hạ tầng backend Monorepo và 2 phân hệ cốt lõi của Thành viên 1 đã sẵn sàng để triển khai tầng nghiệp vụ (Business Logic).
6. **Commit**:
   - `feat(infra): thiết lập eureka api gateway và khung assignment chatbot`

