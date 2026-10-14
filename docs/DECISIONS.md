# Biên Bản Quyết Định Kỹ Thuật — EduLearn LMS (DECISIONS.md)

Tài liệu này ghi nhận các Quyết định Kiến trúc Phần mềm (Architecture Decision Records - ADR) và các biện pháp khắc phục sai sót kỹ thuật phát hiện từ project mẫu được áp dụng vào dự án **DoAnCK**.

---

## Danh Sách Quyết Định Kiến Trúc (ADRs)

### ADR-001: Áp Dụng Mô Hình Monorepo Cho Toàn Bộ Hệ Thống Microservices
- **Trạng thái**: Đã phê duyệt (Accepted).
- **Bối cảnh**: Hệ thống bao gồm 7 microservices backend, 1 frontend SPA, cấu hình hạ tầng Docker và tài liệu.
- **Quyết định**: Quản lý toàn bộ trong một repository duy nhất (`DoAnCK`).
- **Hệ quả**:
  - Dễ dàng quản lý phiên bản, chia sẻ biến môi trường `.env` chung.
  - Một lệnh `docker compose up` có thể build và chạy toàn bộ hệ thống.
  - Phân chia nhánh rõ ràng theo vai trò thành viên để tránh xung đột mã nguồn.

---

### ADR-002: Kiến Trúc Lưu Trữ Đa Hình (Polyglot Persistence)
- **Trạng thái**: Đã phê duyệt (Accepted).
- **Bối cảnh**: Hệ thống có cả dữ liệu giao dịch quan hệ (Users, Khóa học, Điểm số, Bảng điểm) và dữ liệu tài liệu linh hoạt (Cây bài học nhiều tầng, Mã nguồn nộp bài, Lịch sử hội thoại AI).
- **Quyết định**:
  - **MySQL 8.4**: Đảm bảo tính toàn vẹn ACID cho `identity_lms`, `course_lms`, `assignment_lms`, `blog_lms`.
  - **MongoDB 7.0**: Lưu trữ Document phi cấu trúc cho `course_structures`, `lessons`, `submissions`, `chat_sessions`, `chat_messages`.
  - **Redis 7.2**: Lưu trữ bộ nhớ đệm cache và đếm số lượng request kiểm soát rate limit.
  - **MinIO**: Lưu trữ tệp tin nhị phân lớn (ảnh, tài liệu, file zip) tương thích giao thức S3.
- **Hệ quả**: Tối ưu hóa hiệu năng đọc ghi cho từng nghiệp vụ chuyên biệt.

---

### ADR-003: Xác Thực Tập Trung Tại API Gateway & Inject Header Nội Bộ
- **Trạng thái**: Đã phê duyệt (Accepted).
- **Bối cảnh**: Không muốn mỗi microservice downstream phải gọi ngược lại `identity-service` hoặc tự giải mã JWT gây lặp mã và tăng độ trễ mạng.
- **Quyết định**:
  - `api-gateway` sử dụng bộ lọc `AuthFilter` thẩm định chữ ký HMAC-256 của token JWT.
  - Tự động inject các headers: `X-User-Id`, `X-User-Role`, `X-User-Email`, `X-Gateway-Authenticated: true`.
  - Đối với các request public, Gateway chủ động xóa các header trên để chống tấn công giả mạo (Header Spoofing).
  - Downstream services chỉ việc trích xuất `@RequestHeader("X-User-Id")` để biết danh tính người gọi.
- **Hệ quả**: Hiệu năng xử lý nội bộ tăng đáng kể, giảm tải cho `identity-service`.

---

### ADR-004: Tích Hợp Judge0 Engine Cho Trình Chấm Code Tự Động
- **Trạng thái**: Đã phê duyệt (Accepted).
- **Bối cảnh**: Chức năng thực hành lập trình đòi hỏi môi trường thực thi an toàn, hỗ trợ nhiều ngôn ngữ (C++, Java, Python, Go, Rust, JS) và đo lường chính xác CPU time, Memory.
- **Quyết định**: Sử dụng **Judge0 API Engine** chạy trong môi trường Sandbox cô lập. `assignment-service` gửi mã nguồn sang Judge0 qua REST client và xử lý kết quả bằng cơ chế bất đồng bộ (ThreadPool Executor).
- **Hệ quả**: Bảo đảm an toàn tuyệt đối cho hệ thống máy chủ, tránh nguy cơ thực thi mã độc từ phía người dùng.

---

### ADR-005: Khắc Phục Sai Sót Thiếu Thư Mục `docker/mysql/init` Của Project Mẫu
- **Trạng thái**: Đã phê duyệt (Accepted - Bugfix từ project mẫu).
- **Bối cảnh**: Project mẫu khai báo volume `- ./docker/mysql/init:/docker-entrypoint-initdb.d:ro` trong `docker-compose.yml` nhưng không có thư mục `docker` trên thực tế, dẫn đến nguy cơ thiếu dữ liệu khởi tạo.
- **Quyết định**: Trong `DoAnCK`, chủ động tạo thư mục `docker/mysql/init/` và tạo file `01-create-databases.sql`:
  ```sql
  CREATE DATABASE IF NOT EXISTS `identity_lms` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE DATABASE IF NOT EXISTS `course_lms` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE DATABASE IF NOT EXISTS `assignment_lms` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  CREATE DATABASE IF NOT EXISTS `blog_lms` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
  GRANT ALL PRIVILEGES ON *.* TO 'root'@'%';
  FLUSH PRIVILEGES;
  ```
- **Hệ quả**: MySQL container khi khởi chạy lần đầu tiên sẽ tự động chuẩn bị đầy đủ 4 database, giúp các microservices kết nối thành công 100% ngay từ lần đầu khởi động.

---

### ADR-006: Chuẩn Hóa Tên Package Backend Về Định Dạng `com.lms.*`
- **Trạng thái**: Đã phê duyệt (Accepted).
- **Bối cảnh**: Trong project mẫu, `service-registry` sử dụng package `com.n23dccn161.serviceregistry` khác biệt với toàn bộ các module còn lại (`com.lms.*`).
- **Quyết định**: Thống nhất chuyển toàn bộ package name trong `DoAnCK` về dạng:
  - `com.lms.serviceregistry`
  - `com.lms.apigateway`
  - `com.lms.identityservice`
  - `com.lms.courseservice`
  - `com.lms.assignmentservice`
  - `com.lms.blogservice`
  - `com.lms.chatbotservice`
- **Hệ quả**: Mã nguồn đồng nhất, sạch sẽ và chuyên nghiệp.

---

### ADR-007: Quản Lý Di Trú Lược Đồ Bằng Flyway Migration Tuần Tự
- **Trạng thái**: Đã phê duyệt (Accepted).
- **Bối cảnh**: Cần kiểm soát chặt chẽ việc thay đổi cấu trúc bảng theo thời gian thực mà không làm mất dữ liệu.
- **Quyết định**: Duy trì cấu trúc di trú theo 2 bước:
  - `V1__initial_schema.sql`: Tạo toàn bộ cấu trúc bảng và khóa ngoại nguyên bản.
  - `V2__add_indexes.sql`: Thêm các index hiệu năng cao phục vụ truy vấn tối ưu.
- **Hệ quả**: Dữ liệu có thể nâng cấp mượt mà trong quá trình CI/CD.
