# HƯỚNG DẪN NẠP VÀ SỬ DỤNG CƠ SỞ DỮ LIỆU EDULEARN LMS

Tài liệu này hướng dẫn chi tiết cách nạp cơ sở dữ liệu hoàn chỉnh của dự án **EduLearn LMS** phục vụ công tác chấm điểm, nghiệm thu và chạy thử nghiệm đồ án.

---

## 1. Tổng quan hệ thống Cơ sở dữ liệu

Dự án áp dụng mô hình lưu trữ đa hệ quản trị (**Polyglot Persistence**) chuẩn Microservices:

| Hệ quản trị CSDL | Tên Database | Mục đích sử dụng |
|---|---|---|
| **MySQL 8.4** | `identity_lms` | Quản lý người dùng, phân quyền RBAC, JWT Refresh Token Rotation, mã OTP. |
| **MySQL 8.4** | `course_lms` | Danh mục, thông tin khóa học, lịch học Google Meet, ghi danh, tiến độ học viên. |
| **MySQL 8.4** | `assignment_lms` | Bài tập lập trình, bộ testcases chấm tự động Judge0, đề thi trắc nghiệm, bảng điểm. |
| **MySQL 8.4** | `blog_lms` | Diễn đàn trao đổi sinh viên, bài viết Markdown, hệ thống thẻ tags, bình luận, vote. |
| **MongoDB 7.0** | `learning_lms` | Cấu trúc cây đề cương bài học phân cấp (Course Structure) và bài nộp code chi tiết. |

---

## 2. Danh sách Tài khoản & Mật khẩu mẫu để Đăng nhập thử nghiệm

> **Mật khẩu dùng chung cho tất cả tài khoản mẫu:** `Password123@`

| STT | Tên đăng nhập | Email | Vai trò | Họ tên hiển thị | Quyền hạn thử nghiệm |
|:---:|---|---|:---:|---|---|
| 1 | **`admin`** | `admin@lms.edu.vn` | **ADMIN** | Quản Trị Viên Hệ Thống | Toàn quyền quản trị hệ thống, quản lý người dùng, khóa học, phê duyệt danh mục và thẻ tag. |
| 2 | **`instructor_demo`** | `instructor_demo@lms.edu.vn` | **INSTRUCTOR** | TS. Nguyễn Văn Hướng | Giảng viên mẫu, biên soạn bài giảng, tạo bài thi trắc nghiệm, cấu hình lịch học Google Meet. |
| 3 | **`nguyenminhtri`** | `nguyenminhtri@lms.edu.vn` | **INSTRUCTOR** | ThS. Nguyễn Minh Trí | Giảng viên phụ trách môn Lập trình Web và Kiến trúc Microservices. |
| 4 | **`cuongnv`** | `cuongnv@lms.edu.vn` | **INSTRUCTOR** | Nông Văn Cường | Giảng viên phụ trách mảng Thuật toán và Luyện code Sandbox. |
| 5 | **`student_demo`** | `student_demo@lms.edu.vn` | **STUDENT** | Lê Hoàng Nam | Học viên mẫu, đã ghi danh đầy đủ 3 khóa học, có nhật ký học tập, làm quiz và nộp code. |
| 6 | **`tranvantuan`** | `tuan.tran@lms.edu.vn` | **STUDENT** | Trần Văn Tuấn | Học viên tham gia diễn đàn và làm bài tập. |

---

## 3. Cách 1: Nạp 1-Click bằng MySQL Workbench / DBeaver / HeidiSQL (Khuyên dùng)

1. Mở công cụ quản trị CSDL của Thầy/Cô (ví dụ: **MySQL Workbench**, **HeidiSQL**, **DBeaver**, **Navicat** hoặc **phpMyAdmin**).
2. Kết nối đến MySQL Server (Localhost cổng `3306`, tài khoản `root`).
3. Mở tệp SQL tổng hợp duy nhất:
   ```text
   database/edulearn_lms_full_backup.sql
   ```
4. Nhấn nút **Execute** (hoặc tổ hợp phím `Ctrl + Shift + Enter`).
5. **Kết quả:** Hệ thống sẽ tự động tạo đủ 4 database (`identity_lms`, `course_lms`, `assignment_lms`, `blog_lms`), tạo đầy đủ bảng, khóa ngoại, chỉ mục hiệu năng và nạp 100% dữ liệu mẫu tiếng Việt chuẩn mã `utf8mb4`.

---

## 4. Cách 2: Nạp bằng dòng lệnh Command Prompt / Terminal

Nếu máy tính đã cài sẵn MySQL Client trong biến môi trường:

```bash
# Mở terminal tại thư mục chứa file SQL và chạy:
mysql -u root -p < database/edulearn_lms_full_backup.sql
```
*(Nhập mật khẩu root MySQL của máy khi được yêu cầu)*.

Nếu Thầy/Cô muốn nạp riêng lẻ từng dịch vụ, các file thành phần nằm tại thư mục `database/mysql/`:
- `database/mysql/01_identity_lms.sql`
- `database/mysql/02_course_lms.sql`
- `database/mysql/03_assignment_lms.sql`
- `database/mysql/04_blog_lms.sql`

---

## 5. Cách 3: Tự động khởi chạy bằng Docker Compose

Trong dự án đã cấu hình sẵn kịch bản tự động nạp CSDL khi chạy Docker:

```bash
# Khởi động cụm CSDL (MySQL 8.4, MongoDB, Redis, MinIO):
docker compose -f docker-compose.db.yml up -d
```
Tập tin `docker-compose.db.yml` đã được liên kết với thư mục khởi tạo `docker/mysql/init/` để tự động thiết lập và sẵn sàng nhận kết nối ngay lập tức.

---

## 6. Hướng dẫn nạp dữ liệu MongoDB (Tùy chọn)

Nếu muốn nạp thêm dữ liệu cấu trúc đề cương phân cấp vào MongoDB:
1. Mở **MongoDB Compass** hoặc terminal **mongosh**.
2. Kết nối đến `mongodb://localhost:27017`.
3. Chạy tập lệnh:
   ```bash
   mongosh "mongodb://localhost:27017/learning_lms" < database/mongodb/init_mongodb_learning_lms.js
   ```
