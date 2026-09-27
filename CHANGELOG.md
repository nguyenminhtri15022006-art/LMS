# CHANGELOG.md — Nhật Ký Thay Đổi Dự Án

Tất cả các thay đổi đáng chú ý của dự án `DoAnCK` sẽ được ghi lại trong tài liệu này theo đúng tiến độ thực tế.  
Định dạng nhật ký được phân loại theo: `Added`, `Changed`, `Fixed`, `Tested`, `Documented`.

---

## [Unreleased] — Khởi Tạo Dự Án & Hoàn Thiện Bộ Tài Liệu Kỹ Thuật (2026-09-27)

### Documented
- Khởi tạo tài liệu gốc [README.md](README.md) mô tả toàn cảnh hệ thống EduLearn LMS, kiến trúc microservices, thông tin nhóm, hướng dẫn chạy và các đường dẫn tài liệu.
- Thiết lập quy định hoạt động nghiêm ngặt cho Coding Agent trong [AGENTS.md](AGENTS.md) nhằm bảo vệ project mẫu, quy trình an toàn dữ liệu và chuẩn mực kiểm thử.
- Thiết lập quy định cộng tác và phân nhánh trong [CONTRIBUTING.md](CONTRIBUTING.md).
- Xây dựng tài liệu kiến trúc tổng thể [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) với sơ đồ luồng dữ liệu microservices, API gateway và eureka.
- Xây dựng bản đặc tả thiết kế cơ sở dữ liệu chi tiết [docs/DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md) bao phủ toàn bộ 25 bảng quan hệ MySQL và 5 collections MongoDB.
- Lập bảng đặc tả giao diện lập trình [docs/API_CONTRACT.md](docs/API_CONTRACT.md) cho tất cả các endpoint của 5 services nghiệp vụ.
- Thiết lập ma trận phân quyền chi tiết cho 4 vai trò (Guest, Student, Instructor, Admin) trong [docs/USER_ROLES.md](docs/USER_ROLES.md).
- Phân chia trách nhiệm thành viên chi tiết theo từng module trong [docs/TEAM_ROLES.md](docs/TEAM_ROLES.md).
- Lập lộ trình phát triển chi tiết từ 27/09 đến 29/10 trong [docs/ROADMAP.md](docs/ROADMAP.md) và [docs/COMMIT_PLAN.md](docs/COMMIT_PLAN.md).
- Xây dựng bộ quy tắc viết code [docs/CODING_RULES.md](docs/CODING_RULES.md) cho Java 21, Spring Boot và React 18.
- Xây dựng kế hoạch kiểm thử [docs/TESTING_PLAN.md](docs/TESTING_PLAN.md) và hướng dẫn triển khai [docs/DEPLOYMENT_GUIDE.md](docs/DEPLOYMENT_GUIDE.md).
- Đối chiếu các tiêu chí chấm điểm theo thang rubric môn học trong [docs/RUBRIC_MAPPING.md](docs/RUBRIC_MAPPING.md).
- Ghi nhận các vấn đề kỹ thuật và quyết định thiết kế trong [docs/OPEN_QUESTIONS.md](docs/OPEN_QUESTIONS.md) và [docs/DECISIONS.md](docs/DECISIONS.md).
- Khởi tạo khung nhật ký tiến độ thực tế trong `docs/progress/`.

### Added
- Khởi tạo file mẫu môi trường sanitized [.env.example](.env.example) bao phủ toàn bộ biến cấu hình cho 7 microservices backend, frontend, cơ sở dữ liệu và lưu trữ MinIO.

---

*(Các thay đổi tiếp theo về Code, Test và Fix lỗi sẽ được cập nhật liên tục khi thực hiện thật theo từng giai đoạn)*.
