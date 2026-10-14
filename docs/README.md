# Hệ Thống Tài Liệu Kỹ Thuật — EduLearn LMS

Thư mục `docs/` chứa toàn bộ tài liệu kiến trúc, yêu cầu nghiệp vụ, thiết kế cơ sở dữ liệu, đặc tả API, kế hoạch phát triển và nhật ký kiểm thử của dự án **EduLearn LMS** (`d:\LMS-main`).

---

## Danh Mục Tài Liệu

### 1. Phân Tích & Đặc Tả Nghiệp Vụ
- [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md): Mô tả bài toán, mục tiêu hệ thống, người dùng và luồng vận hành cốt lõi.
- [REQUIREMENTS.md](REQUIREMENTS.md): Đặc tả chi tiết các yêu cầu chức năng (FR) và phi chức năng (NFR) kèm mã định danh chuẩn.
- [FEATURE_PARITY_MATRIX.md](FEATURE_PARITY_MATRIX.md): Ma trận đối chiếu tính năng 100% giữa project mẫu và `DoAnCK` với 6 trạng thái kiểm soát nghiêm ngặt.
- [USER_ROLES.md](USER_ROLES.md): Ma trận phân quyền chi tiết (RBAC) cho 4 nhóm người dùng (Guest, Student, Instructor, Admin).

### 2. Thiết Kế Hệ Thống & Kỹ Thuật
- [ARCHITECTURE.md](ARCHITECTURE.md): Kiến trúc Monorepo Microservices, Spring Cloud Gateway, Eureka Discovery, luồng xác thực JWT/OAuth, Redis, MinIO và sơ đồ Mermaid.
- [DATABASE_DESIGN.md](DATABASE_DESIGN.md): Chi tiết 25 bảng quan hệ MySQL và 5 collections MongoDB, khóa chính, khóa ngoại, indexes và enums.
- [API_CONTRACT.md](API_CONTRACT.md): Toàn bộ tài liệu endpoint, HTTP methods, headers, request/response body, mã lỗi cho 5 microservices nghiệp vụ.
- [DECISIONS.md](DECISIONS.md): Biên bản ghi nhận quyết định kiến trúc (Architecture Decision Records - ADR) và các lỗi ghi nhận từ project mẫu.
- [OPEN_QUESTIONS.md](OPEN_QUESTIONS.md): Các vấn đề kỹ thuật còn mở, giả định và câu hỏi cần làm rõ.

### 3. Quy Trình Làm Việc & Quản Lý Dự Án
- [TEAM_ROLES.md](TEAM_ROLES.md): Phân công trách nhiệm chi tiết cho 2 thành viên theo từng module và luồng chức năng.
- [ROADMAP.md](ROADMAP.md): Lộ trình phát triển tổng thể theo các mốc thời gian từ 27/09 đến 29/10.
- [COMMIT_PLAN.md](COMMIT_PLAN.md): Kế hoạch commit chi tiết theo từng ngày, thành viên, phạm vi mã nguồn và thông điệp chuẩn.
- [GIT_WORKFLOW.md](GIT_WORKFLOW.md): Quy định phân nhánh, quy trình tạo Pull Request và checklist kiểm duyệt.
- [CODING_RULES.md](CODING_RULES.md): Chuẩn mực viết code Java 21, Spring Boot, React 18, đặt tên biến, DTO, Entity, Exception và CSS.
- [RUBRIC_MAPPING.md](RUBRIC_MAPPING.md): Bảng đối chiếu minh chứng đồ án với thang điểm Rubric môn học (100%).

### 4. Kiểm Thử & Vận Hành
- [TESTING_PLAN.md](TESTING_PLAN.md): Kế hoạch kiểm thử toàn diện (Unit Test, Integration Test, Security, Role, E2E, Docker Smoke Test).
- [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md): Sổ tay hướng dẫn cài đặt môi trường, chạy Docker Compose, chạy IDE và xử lý sự cố *(Lưu ý: Chưa khởi tạo trong workspace - CẦN BỔ SUNG)*.

### 5. Nhật Ký Tiến Độ Thực Tế (`docs/progress/`)
- [progress/README.md](progress/README.md): Hướng dẫn ghi chép nhật ký công việc.
- [progress/member-1.md](progress/member-1.md): Nhật ký công việc thực tế của Thành viên 1 (Nguyễn Minh Trí).
- [progress/member-2.md](progress/member-2.md): Nhật ký công việc thực tế của Thành viên 2.
- [progress/integration.md](progress/integration.md): Nhật ký ghép nối tích hợp hệ thống và giải quyết xung đột.
