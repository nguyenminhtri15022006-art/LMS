# Nhật Ký Ghép Nối & Tích Hợp Hệ Thống — EduLearn LMS (integration.md)

Tài liệu này ghi nhận lại toàn bộ các phiên tích hợp liên dịch vụ (Inter-service integration), các lỗi phát sinh trong quá trình ghép nối hệ thống và các biện pháp khắc phục kỹ thuật tương ứng.

---

## Danh Sách Các Phiên Tích Hợp (Integration Sessions)

### [2026-09-27] — Khởi Tạo Khung Tài Liệu & Đồng Bộ Thiết Kế Kiến Trúc
- **Nội dung tích hợp**:
  - Thống nhất ranh giới giao tiếp giữa các phân hệ: Frontend <-> API Gateway <-> 5 Microservices nghiệp vụ.
  - Thống nhất chuẩn dữ liệu trao đổi: Đối tượng bọc `ApiResponse<T>` chung và bảng mã lỗi `ErrorCode`.
  - Thống nhất cơ chế xác thực tập trung: `api-gateway` xác thực JWT và inject các HTTP headers (`X-User-Id`, `X-User-Role`, `X-User-Email`, `X-Gateway-Authenticated`).
- **Lỗi / Xung đột phát hiện**:
  1. *Lỗi cấu hình Docker của project mẫu*: File `docker-compose.yml` ánh xạ thư mục `./docker/mysql/init` không tồn tại, khiến MySQL không tự khởi tạo database khi bật lên lần đầu.
     - *Cách sửa*: Đã lập quyết định kiến trúc [ADR-005](../DECISIONS.md) để chủ động tạo thư mục và script khởi tạo 4 database trong `DoAnCK`.
  2. *Tên package không đồng bộ*: Module `service-registry` trong project mẫu có package `com.n23dccn161.serviceregistry` khác với các service còn lại.
     - *Cách sửa*: Thống nhất quy chuẩn hóa toàn bộ về `com.lms.*` theo [ADR-006](../DECISIONS.md).
- **Trạng thái**: Đồng bộ thiết kế 100%, sẵn sàng cho giai đoạn xây dựng khung dự án.

---

*(Các phiên ghép nối tiếp theo khi tích hợp Feign Client, Gateway Circuit Breaker, Docker Compose và Frontend-Backend sẽ được cập nhật liên tục tại đây)*.
