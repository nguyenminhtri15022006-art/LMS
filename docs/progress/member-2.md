# Kế Hoạch & Nhật Ký Tiến Độ Cá Nhân — Thành Viên 2 (member-2.md)

> [!NOTE]
> File này dành riêng cho **Thành viên 2**. Bạn có toàn quyền chỉnh sửa thông tin cá nhân, cập nhật tiến độ, đánh dấu checkbox `[x]` khi hoàn thành từng đợt commit hoặc điều chỉnh ghi chú theo tình hình thực tế.

---

## 1. Thông Tin Thành Viên
- **Họ và tên**: *(Bạn hãy điền họ và tên vào đây)*
- **MSSV**: *(Bạn hãy điền MSSV vào đây)*
- **Vai trò**: Fullstack Developer (Chủ trì Khối Xác thực, Quản lý Khóa học & Diễn đàn Blog)
- **Nhánh Git làm việc**: `feature/member-2` (Tạo từ `develop`)
- **Phân hệ phụ trách chính**:
  1. `backend/identity-service` (Spring Security, JWT, OAuth2 Google, User Profile, Quản trị Admin)
  2. `backend/course-service` (Khóa học, Cây bài giảng MongoDB, Ghi danh, MinIO S3 upload video/thumbnail, Lịch học)
  3. `backend/blog-service` (Bài viết Markdown, Bình luận đa cấp, Tags, Upvote/Downvote)
  4. Giao diện Frontend tương ứng (`frontend/src/features/auth`, `courses`, `blog`, `dashboard/admin`)

---

## 2. Bảng Theo Dõi & Danh Sách Các Đợt Commit Của Thành Viên 2

Bạn hãy chuyển `[ ]` thành `[x]` sau khi hoàn thành và commit thành công mỗi đợt:

| Trạng thái | Đợt / Thời gian | Nội dung công việc cần làm | File liên quan dự kiến | Commit Message gợi ý (chuẩn dev) | Ghi chú cá nhân |
|:---:|:---:|---|---|---|---|
| [x] | **Đợt 1<br>(27/09)** | Khảo sát phân hệ Identity, Course, Blog từ mẫu; thống nhất đặc tả | `docs/FEATURE_PARITY_MATRIX.md`, `docs/DATABASE_DESIGN.md` | `docs: khao sat phan he identity course va blog` | Đã xong tài liệu |
| [ ] | **Đợt 2<br>(30/09 – 02/10)** | Khởi tạo khung 3 service (`identity`, `course`, `blog`), cấu hình pom.xml và kết nối MySQL | `backend/identity-service/*`, `backend/course-service/*`, `backend/blog-service/*` | `feat: init identity course and blog services` | Chờ làm |
| [ ] | **Đợt 3<br>(03/10 – 05/10)** | Đăng ký, đăng nhập, mã hóa BCrypt, phát hành JWT Access/Refresh Token, gửi mã OTP qua Email | `identity-service/src/main/java/.../service/impl/AuthenticationServiceImpl.java` | `feat(auth): add jwt login register and email otp` | |
| [ ] | **Đợt 4<br>(06/10 – 08/10)** | CRUD Khóa học, Danh mục, Cây cấu trúc chương mục và bài học lưu trên MongoDB | `course-service/src/main/java/.../CourseServiceImpl.java`, `CourseStructure.java` | `feat(course): add course management and mongo lesson structure` | |
| [ ] | **Đợt 5<br>(09/10 – 11/10)** | Quy trình Ghi danh (Enrollment), Cập nhật tiến độ học tập (`LessonProgress`), Lịch học Google Meet | `course-service/src/main/java/.../LessonProgressServiceImpl.java`, `ScheduleServiceImpl.java` | `feat(course): implement enrollment and meet schedule` | |
| [ ] | **Đợt 6<br>(12/10 – 14/10)** | Diễn đàn Blog: Đăng bài Markdown, quản lý Tags, Bình luận đa cấp và Upvote/Downvote | `blog-service/src/main/java/.../PostServiceImpl.java`, `CommentServiceImpl.java` | `feat(blog): add community posts comments and voting` | |
| [ ] | **Đợt 7<br>(15/10 – 17/10)** | Xây dựng giao diện React cho luồng Đăng nhập, Đăng ký, Quản lý hồ sơ, Danh sách khóa học và Blog | `frontend/src/features/auth/*`, `frontend/src/features/courses/*`, `frontend/src/features/blog/*` | `feat(ui): add auth pages course view and blog feed` | |
| [ ] | **Đợt 8<br>(18/10 – 20/10)** | Giao diện Quản trị Admin: Quản lý người dùng (Khóa/Mở tài khoản), Quản lý duyệt bài giảng & khóa học | `frontend/src/features/dashboard/components/admin/*`, `AdminUserController.java` | `feat(admin): add user management and course moderation` | |
| [ ] | **Đợt 9<br>(21/10 – 23/10)** | Viết Unit Test & Integration Test cho Identity, Course, Blog; kiểm tra phân quyền RBAC | `identity-service/src/test/...`, `course-service/src/test/...`, `blog-service/src/test/...` | `test: add unit tests for identity course and blog` | |
| [ ] | **Đợt 10<br>(24/10 – 25/10)** | Tối ưu hóa lưu trữ MinIO S3 (upload video, avatar, thumbnail), tối ưu index MySQL (`V2`) | `docker/minio/*`, `resources/db/migration/V2__add_indexes.sql` | `perf(db): optimize mysql indexes and minio storage` | |
| [ ] | **Đợt 11<br>(26/10 – 27/10)** | Hoàn thiện Dockerfile đa tầng cho các service của mình, kiểm tra kết nối với Docker Compose | `backend/identity-service/Dockerfile`, `course-service/Dockerfile`, `blog-service/Dockerfile` | `build(docker): finalize service dockerfiles` | |
| [ ] | **Đợt 12<br>(28/10 – 29/10)** | Cùng Trí kiểm tra chốt chặn cuối cùng, đồng bộ tài liệu, mở PR merge `develop` -> `main` | `README.md`, PR reviews | `docs: finalize final report and api documentation` | |

---

## 3. Hướng Dẫn Bắt Đầu Dành Cho Thành Viên 2

1. **Lấy code mới nhất & tạo nhánh riêng**:
   ```bash
   git checkout develop
   git pull origin develop
   git checkout -b feature/member-2
   ```
2. **Khởi động Database môi trường dev**:
   ```bash
   docker compose -f docker-compose.db.yml up -d
   ```
3. **Mỗi khi làm xong 1 đợt**:
   - Chạy test đảm bảo không có lỗi: `mvn clean compile` hoặc `mvn test`.
   - Đánh dấu `[x]` vào đợt tương ứng trong bảng trên file này.
   - Commit code theo thông điệp gợi ý ngắn gọn.
   - Push lên nhánh riêng của bạn: `git push origin feature/member-2`.
   - Tạo Pull Request (PR) vào nhánh `develop` để Trí review và merge.

---

## 4. Nhật Ký Chi Tiết Từng Ngày

### [2026-09-27] — Khảo Sát Phân Hệ Identity, Course & Blog Từ Project Mẫu
- **Nội dung**: Phân tích DTO, Entity, kiến trúc xác thực JWT/OAuth2 và cấu trúc bài học MongoDB từ project mẫu.
- **Kết quả**: Thống nhất ma trận tính năng và cấu trúc database.
