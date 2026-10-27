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
mvn clean verify -f backend/pom.xml '-Dspring.profiles.active=test'

# 2. Test triển khai Docker HTTP Smoke (yêu cầu Docker stack và prerequisites):
mvn test -pl web-app -f backend/pom.xml -Pdocker-smoke '-Dtest=WebAppDockerDeploymentSmokeTest'

# 3. Cài dependencies độc lập của web-app từ lockfile và Chromium:
npm.cmd --prefix backend/web-app ci
npm.cmd --prefix backend/web-app run test:e2e:install

# 4. Browser E2E (cần Docker stack và cấu hình bên dưới):
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
  - *Kịch bản 2*: Dùng cùng object thật để đối chứng fetch có custom header từ origin cho phép và origin trái phép trên port tạm. Browser thực hiện preflight; yêu cầu origin cho phép đọc đúng bytes trước và sau lần bị chặn, đồng thời HTTP OPTIONS của origin bị cấm không có Access-Control-Allow-Origin. NetworkError riêng lẻ không đủ để pass.
  - *Kịch bản 3*: Đăng ký tài khoản mới kèm `fullname` -> Lấy mã OTP 6 số từ Mailpit API (`http://localhost:8025/api/v1/messages`) -> Gửi form xác thực tài khoản -> Đăng nhập bằng tài khoản mới vừa tạo.
- **Google OAuth 2.0**:
  - Cần credentials thật từ Google Cloud Console (`lms.google.client-id`, `lms.google.client-secret`) và màn hình tương tác người dùng (Consent Screen).
  - Không thể tự động hóa headless mà không có credentials; được ghi nhận minh bạch là chưa nghiệm thu E2E do thiếu khóa thật từ nhà cung cấp bên thứ ba (tuân thủ quy định không dùng mock giả mạo).


## Fixture và cleanup của Browser E2E (đính chính 2026-10-27)

Browser runner không dùng tài khoản admin chia sẻ hoặc mật khẩu mặc định. Mỗi luồng
đăng ký tài khoản `e2e_<UUID>` riêng, mật khẩu ngẫu nhiên chỉ nằm trong bộ nhớ;
email có domain `test.lms.invalid` và thư được lấy từ Mailpit của môi trường test.
Playwright, MySQL client và MinIO client được khai báo trong devDependencies của
web-app và khóa version trong package-lock.json; không phụ thuộc frontend/node_modules.

Biến môi trường bắt buộc cho browser runner:

| Biến | Mục đích |
| --- | --- |
| SMOKE_DB_USER, SMOKE_DB_PASSWORD | Kết nối database test identity_lms để xác minh và cleanup fixture |
| SMOKE_MINIO_ACCESS_KEY, SMOKE_MINIO_SECRET_KEY | Lưu object đối chứng, đọc/list/xóa object của test |
| SMOKE_MINIO_BUCKET | Bucket identity thực tế |

Tùy chọn: SMOKE_DB_HOST (127.0.0.1), SMOKE_DB_PORT (3307), SMOKE_WEBAPP_URL
(http://localhost:8088), SMOKE_MAILPIT_URL (http://localhost:8025), SMOKE_MINIO_URL
(http://localhost:9000). Chỉ dùng database/MinIO/Mailpit test. Runner không tự đọc
.env và không dùng credentials mặc định; nạp biến môi trường vào cùng process chạy npm.
Không lưu giá trị secret vào file tracked hoặc báo cáo.

Assertion đăng nhập yêu cầu URL root đúng origin, session cookie và email trên profile
đúng tài khoản. Assertion avatar đối chiếu publicUrl vừa được cấp với src sau reload,
avatar_url trong MySQL, bytes object và khả năng decode ảnh của trình duyệt.

Cleanup cuối suite chạy cả khi test thất bại: xóa object dưới prefix user ID vừa tạo,
refresh_tokens, email_verifications và đúng user có cặp username/email của lần chạy;
xóa đúng IDs email gửi tới fixture. Không gửi Mailpit DELETE với danh sách IDs rỗng
(vì API có thể xóa toàn bộ thư). Kiểm tra lại user/object/email không còn; cleanup lỗi
làm suite fail. Không xóa tài khoản hoặc dữ liệu do các lần chạy cũ/người dùng tạo.
Nếu process bị kill thì hook không chạy: tra định danh e2e của lần bị ngắt và dọn
đúng fixture bằng quyền hợp lệ; không chạy lệnh xóa rộng theo prefix chung.

Khi đăng ký thành công nhưng gửi OTP lỗi, UI thông báo tài khoản đã tạo, chưa gửi
được mã và cho gửi lại ngay. Không yêu cầu đăng ký lại hoặc hiển thị thư đã gửi.
Hướng dẫn API cleanup: https://mailpit.axllent.org/docs/api-v1/ và
https://github.com/minio/minio-js/blob/master/docs/API.md.
