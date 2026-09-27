# Nhật Ký Tiến Độ Cá Nhân — Thành Viên 2 (member-2.md)

- **Họ và tên**: (Đang cập nhật)
- **MSSV**: (Đang cập nhật)
- **Vai trò**: Thành viên cốt lõi / Fullstack Developer
- **Phụ trách chính**: `identity-service`, `course-service`, `blog-service`, Giao diện Auth/Course/Blog/Settings/Admin, MinIO S3 & Security RBAC.

---

### [2026-09-27] — Khảo Sát Phân Hệ Identity, Course & Blog Từ Project Mẫu

1. **Ngày thực hiện**: 27/09/2026
2. **Công việc đã làm**:
   - Phân tích chi tiết mã nguồn `identity-service` của project mẫu: cơ chế bảo mật JWT, Google OAuth 2.0, OTP và RBAC.
   - Phân tích `course-service`: mô hình lưu trữ cây phân cấp chương mục và bài học trên MongoDB (`CourseStructure`), quy trình ghi danh và theo dõi tiến độ.
   - Khảo sát `blog-service`: quản lý bài viết Markdown, tags, bình luận đa cấp và upvote/downvote.
   - Phối hợp với Thành viên 1 thiết lập các yêu cầu chức năng (`FR-IDENTITY-*`, `FR-COURSE-*`, `FR-BLOG-*`) và ma trận tính năng [docs/FEATURE_PARITY_MATRIX.md](../FEATURE_PARITY_MATRIX.md).
3. **File thay đổi**:
   - Đóng góp nội dung cho [docs/FEATURE_PARITY_MATRIX.md](../FEATURE_PARITY_MATRIX.md) và [docs/DATABASE_DESIGN.md](../DATABASE_DESIGN.md).
   - Khởi tạo [docs/progress/member-2.md](member-2.md).
4. **Test đã chạy**:
   - Rà soát các cấu trúc DTO đầu vào/đầu ra của Identity, Course và Blog trong tài liệu API Reference của project mẫu.
5. **Kết quả**:
   - Nắm rõ 14 tính năng Identity, 15 tính năng Course và 9 tính năng Blog.
   - Xác định được danh sách bảng CSDL MySQL và collections MongoDB cần thiết.
6. **Commit**:
   - Chưa thực hiện commit (chờ người dùng yêu cầu chính thức theo quy định).
7. **Khó khăn & Thách thức**:
   - Tích hợp Judge0 cần môi trường chạy sandbox Docker hoặc API key bên ngoài; đã đề xuất giải pháp dự phòng trong [OPEN_QUESTIONS.md](../OPEN_QUESTIONS.md).
8. **Việc tiếp theo**:
   - Ngày 28–29/09: Hoàn thiện sơ đồ luồng chi tiết cho Quiz Attempt và Code Judge Sandbox, chuẩn bị cho việc khởi tạo khung service vào 30/09.
