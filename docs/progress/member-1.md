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
   - Chưa thực hiện commit (chờ người dùng yêu cầu chính thức theo quy định).
7. **Khó khăn & Thách thức**:
   - Phát hiện project mẫu thiếu thư mục `docker/mysql/init` dù có khai báo volume trong `docker-compose.yml`, đã kịp thời ghi nhận quyết định tạo thư mục bù đắp tại `DECISIONS.md`.
8. **Việc tiếp theo**:
   - Ngày 28–29/09: Cùng Thành viên 2 hoàn thiện bảng ma trận tính năng chi tiết và kiểm kê toàn bộ DTO/API của Identity và Course Service.
