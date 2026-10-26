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
# Test cô lập, Docker có thể tắt:
mvn clean verify -pl web-app -f backend/pom.xml '-Dspring.profiles.active=test'

# Chỉ chạy sau khi stack thật và prerequisites đã sẵn sàng:
mvn test -pl web-app -f backend/pom.xml -Pdocker-smoke '-Dtest=WebAppDockerDeploymentSmokeTest'
```

## Điều kiện PASS và dọn dữ liệu

- Gateway: tạo danh mục tên duy nhất bằng token ADMIN, yêu cầu ID hợp lệ rồi kiểm tra
  đúng tên xuất hiện trong HTML BFF. Thiếu token/tạo thất bại phải FAIL; không tìm chữ
  `courses` chung chung. Cuối test xóa đúng ID đã tạo và kiểm tra kết quả cleanup.
- Auth: anonymous bị từ chối, mật khẩu ngẫu nhiên sai bị từ chối, tài khoản cấu hình
  đăng nhập thành công và profile khớp username. Bộ smoke hiện dùng tài khoản chuẩn bị
  trước; helper Mailpit không nằm trong luồng này, không dùng kết quả này để nghiệm thu OTP.
- MinIO: lấy POST policy từ Gateway, upload multipart và đọc lại đúng bytes. Cleanup
  dùng MinIO SDK ký request DELETE, chỉ chấp nhận key dưới `smoke/<UUID-lần-chạy>/`.
  Endpoint/bucket/public URL được đối chiếu trước upload. Cleanup lỗi làm test FAIL.
  Không xóa bucket, không xóa dữ liệu người dùng.
- Nếu Maven bị dừng đột ngột sau upload, kiểm tra object/danh mục của lần chạy theo prefix
  hoặc tên duy nhất và dọn bằng quyền hợp lệ; không xóa rộng cả bucket/database.

Thiếu cấu hình hoặc Docker tắt là prerequisite thất bại, không phải test được bỏ qua
hay PASS. SMTP/OTP thật, Google OAuth và UI upload avatar toàn trình vẫn cần bằng chứng
riêng; bài MinIO ở đây chỉ kiểm tra upload policy, lưu trữ, đọc lại và cleanup.
