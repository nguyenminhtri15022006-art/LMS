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

### [2026-10-24] — Tích Hợp Đóng Gói Docker Compose & Kiểm Thử Toàn Trình (E2E Integration)
- **Nội dung tích hợp**:
  - Chuẩn hóa kiến trúc Docker multi-container cho toàn bộ 8 microservices backend (`service-registry`, `api-gateway`, `identity-service`, `course-service`, `assignment-service`, `blog-service`, `chatbot-service`, `web-portal`) và 4 dịch vụ hạ tầng (`mysql:8.4`, `mongo:7.0`, `redis:7.2-alpine`, `minio/minio`).
  - Thiết lập Dockerfile tối ưu đa tầng (Multi-stage / Alpine Runtime JRE 21) cho từng microservice với curl healthcheck tích hợp.
  - Cấu hình mạng `lms-network` cầu nối (Bridge network) đảm bảo phân giải tên miền nội bộ giữa các container mà không cần phụ thuộc vào địa chỉ IP tĩnh.
  - Xác thực chuỗi liên phụ thuộc `depends_on` với điều kiện `service_healthy`:
    - `service-registry` khởi động trước -> `api-gateway` và các nghiệp vụ backend chờ Eureka sẵn sàng.
    - `assignment-service` chờ đồng thời `mysql`, `mongodb` và `service-registry`.
    - `chatbot-service` chờ đồng thời `mongodb`, `redis` và `service-registry`.
    - `web-portal` kết nối xuyên suốt qua `api-gateway`.
- **Kịch bản E2E kiểm chứng thành công**:
  1. **Luồng Quiz & Đề thi trắc nghiệm**: Tạo đề -> Import bộ câu hỏi CSV/TXT -> Thí sinh làm bài -> Nộp bài & Hệ thống chấm điểm tự động chuẩn hóa phân số -> Cập nhật Gradebook và Leaderboard thời gian thực.
  2. **Luồng Lập trình Code Judge**: Soạn thảo mã nguồn qua CodeMirror -> Nộp bài chấm qua Judge0 sandbox -> Phân luồng Worker ThreadPool -> Phản hồi Verdict (AC/WA/TLE/MLE/CE/RE) chuẩn xác.
  3. **Luồng Trợ lý ảo AI Chatbot**: Nhận diện bài toán lập trình -> Trả lời giải thích phân tích thuật toán kèm độ phức tạp Big-O -> Giới hạn tốc độ Rate Limiter Redis 20 req/phút -> Hỗ trợ luồng dữ liệu thời gian thực Server-Sent Events (SSE).
- **Trạng thái**: 100% các thành phần sẵn sàng vận hành trên Docker stack.
