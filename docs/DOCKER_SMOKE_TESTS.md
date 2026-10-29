# Chạy smoke test Phase 7

Các test dưới profile `docker-smoke` gọi HTTP thật. Test mặc định không cần Docker;
HttpServer trong unit test chỉ kiểm chứng helper, không thay thế bằng chứng E2E.

## Điều kiện chạy

1. `docker info` thành công. Nếu WSL báo virtualization chưa bật, kiểm tra
   Virtual Machine Platform và virtualization trong BIOS/UEFI, khởi động lại Windows
   sau khi bật tính năng. Không có distro không đồng nghĩa Docker hỏng; không bắt buộc
   cài Ubuntu riêng. Tham khảo [Docker WSL](https://docs.docker.com/desktop/features/wsl/).
2. Cấu hình Compose bằng secrets riêng của môi trường test, chạy `docker compose up -d --build`,
   kiểm tra `docker compose ps -a` và log nếu service/init container lỗi.
3. Chuẩn bị một tài khoản test đang active, đã xác thực email và một access token ADMIN
   còn hạn. Không mặc định tồn tại `admin/admin123`. Có thể dùng tài khoản đã chuẩn bị
   hoặc bootstrap trong môi trường test với `BOOTSTRAP_ADMIN_ENABLED=true`,
   `BOOTSTRAP_ADMIN_USERNAMES`, `BOOTSTRAP_ADMIN_PASSWORDS`, `BOOTSTRAP_ADMIN_EMAILS`
   trước khi khởi động identity-service. Bootstrap bỏ qua username đã tồn tại;
   thay đổi mật khẩu trong biến môi trường không cập nhật tài khoản cũ.
4. Lấy access token qua `POST /api/identity/auth/login`, JSON `username`, `password`;
   dùng `data.accessToken` của tài khoản ADMIN. Không in/lưu token vào log hoặc Git.

## Cấu hình cho Maven

Đặt các biến môi trường trong cùng phiên terminal chạy Maven (không ghi secrets vào
command line `-D`, tài liệu hoặc file tracked):

| Biến môi trường bắt buộc | Ý nghĩa |
| --- | --- |
| `SMOKE_TEST_USER` | Username tài khoản đã chuẩn bị để đăng nhập BFF |
| `SMOKE_TEST_PASSWORD` | Mật khẩu thật của tài khoản test |
| `SMOKE_ADMIN_TOKEN` | Access token ADMIN còn hạn; tạo/xóa danh mục và cấp upload policy |
| `SMOKE_MINIO_ACCESS_KEY` | Access key của principal được phép xóa object test |
| `SMOKE_MINIO_SECRET_KEY` | Secret key tương ứng |
| `SMOKE_MINIO_BUCKET` | Bucket identity thực tế, mặc định Compose là `lms-identity-bucket` |

Maven không tự đọc `.env` của Compose. Các biến `SMOKE_*` phải được nạp vào process
chạy Maven. Tài khoản cleanup cần quyền `s3:DeleteObject` trong bucket trên, prefix
`smoke/*`; không bật anonymous write/delete để làm test pass.

URL mặc định: BFF `http://localhost:8088`, Gateway `http://localhost:8080`, MinIO
`http://localhost:9000`. Có thể đổi qua `-Dsmoke.webapp.url`, `-Dsmoke.gateway.url`,
`-Dsmoke.minio.url`; `-Dsmoke.minio.region` mặc định `us-east-1`.
MinIO public URL do identity trả về phải khớp endpoint host dùng trong test.

```powershell
# 1. Test cô lập, Docker có thể tắt:
mvn clean test -pl web-app -f backend/pom.xml '-Dspring.profiles.active=test'

# 2. Test triển khai Docker HTTP Smoke (yêu cầu Docker stack và prerequisites):
mvn test -pl web-app -f backend/pom.xml -Pdocker-smoke '-Dtest=WebAppDockerDeploymentSmokeTest'

# 3. Test trình duyệt E2E giao diện Thymeleaf (yêu cầu Docker stack + Mailpit + MinIO):
npm.cmd --prefix backend/web-app run test:e2e
```

## Điều kiện PASS và dọn dữ liệu

- **Gateway**: tạo danh mục tên duy nhất bằng token ADMIN, yêu cầu ID hợp lệ rồi kiểm tra
  đúng tên xuất hiện trong HTML BFF. Thiếu token/tạo thất bại phải FAIL; không tìm chữ
  `courses` chung chung. Cuối test xóa đúng ID đã tạo và kiểm tra kết quả cleanup.
- **Auth Session & Chuyển Hướng**: anonymous bị từ chối, mật khẩu ngẫu nhiên sai bị từ chối,
  tài khoản cấu hình đăng nhập thành công và profile khớp username. Kiểm tra URL chuyển hướng
  bằng phương thức `isValidLoginRedirect(location, baseUrl)` kiểm tra nghiêm ngặt scheme, host,
  port và root path, từ chối tuyệt đối open redirect.
- **MinIO S3 POST Policy & CORS**:
  - CORS MinIO được cấu hình chặt chẽ trong `docker-compose.yml`: `MINIO_API_CORS_ALLOW_ORIGIN: "http://localhost:8088,http://127.0.0.1:8088,http://localhost:3000"`, không dùng wildcard `*`.
  - Kiểm tra upload multipart nhị phân thật qua Presigned POST Policy (yêu cầu trường `Content-Type: image/png` trước file part) và đọc lại đúng bytes.
  - Cleanup dùng MinIO SDK ký request DELETE, chỉ chấp nhận key dưới `smoke/<UUID-lần-chạy>/`.
- **Trình Duyệt E2E (Playwright - `test:e2e`)**:
  - *Kịch bản 1*: Đăng nhập UI Thymeleaf -> Chọn file ảnh avatar -> Direct Upload MinIO -> Cập nhật CSDL MySQL -> Tải lại trang và xác thực URL ảnh đại diện hiển thị đúng.
  - *Kịch bản 2*: Trình duyệt gọi trực tiếp tới MinIO từ origin trái phép (`http://127.0.0.1:9876`), kiểm tra trình duyệt tự động chặn kết nối qua cơ chế CORS (`Failed to fetch`).
  - *Kịch bản 3*: Đăng ký tài khoản mới kèm `fullname` -> Lấy mã OTP 6 số từ Mailpit API (`http://localhost:8025/api/v1/messages`) -> Gửi form xác thực tài khoản -> Đăng nhập bằng tài khoản mới vừa tạo.
- **Google OAuth 2.0**:
  - Cần credentials thật từ Google Cloud Console (`lms.google.client-id`, `lms.google.client-secret`) và màn hình tương tác người dùng (Consent Screen).
  - Không thể tự động hóa headless mà không có credentials; được ghi nhận minh bạch là chưa nghiệm thu E2E do thiếu khóa thật từ nhà cung cấp bên thứ ba (tuân thủ quy định không dùng mock giả mạo).

