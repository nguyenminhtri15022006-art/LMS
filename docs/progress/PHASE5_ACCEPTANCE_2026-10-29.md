# Biên bản nghiệm thu Phase 5 — 2026-10-29

**Kết luận: PASS trong phạm vi seed demo, khởi tạo stack local từ volume mới và hồi quy các luồng Member 2 đã liệt kê.** Không đồng nghĩa toàn bộ dự án đạt parity 100% hoặc mọi chức năng đủ 13 tiêu chí VERIFIED.

## Phạm vi và bảo toàn dữ liệu

- Nhánh: `feature/member-2-dot-4-integration`. Nghiệm thu trên working tree, gồm các sửa Phase 7 có sẵn; không khẳng định chỉ riêng commit Phase 5 chứa toàn bộ code được chạy.
- Build lại sáu image backend từ working tree bằng `docker compose build`.
- Stack nghiệm thu có tên `lms-phase5-acceptance`, container/network/volume riêng. Stack thường tạm dừng để nhường cổng; không xóa/reset volume của stack thường.
- MySQL chỉ mount `01-init-databases.sql`. Microservice khởi tạo schema trước khi script seed được gọi chủ động.
- Seed sử dụng nguyên script nghiệp vụ, chỉ chuyển tên container MySQL/MongoDB qua wrapper tới database thật của stack cô lập. Không mock phản hồi storage, không tạo dữ liệu mong đợi để lách assertion.
- Mật khẩu demo ngẫu nhiên được lưu cục bộ trong thư mục loại trừ khỏi Git; không đưa vào biên bản hay commit.
- Project mẫu chỉ được đọc. Không thay đổi phân hệ Assignment/Chatbot của Trí.

## Kết quả thực thi

| Hạng mục | Kết quả | Thời gian |
|---|---|---|
| `mvn clean verify -f backend/pom.xml -Dspring.profiles.active=test` | BUILD SUCCESS, 337 test, 0 fail/error/skip | 4 phút 47 giây |
| Chi tiết Java | registry 2; gateway 21; identity 48; course 67; blog 27; web-app 172 | Đọc tổng từ Surefire XML sau clean verify |
| Test mật khẩu seed | 4/4 PASS | 1.217s |
| Test seed/collision/encoding | 6/6 PASS, có SELECT/bảng TEMPORARY trên MySQL thật | 1.008s |
| JavaScript suite mặc định | 61/61 PASS | 3.903s ở lần kiểm tra trước; không sửa mã JS giao diện sau đó |
| Fresh Compose `up -d --wait` | 11 dịch vụ healthy; job tạo bucket exit 0 | Không đo riêng tổng thời gian |
| `DockerStackReadinessIntegrationTest` với `-Pdocker-smoke` | 1/1 PASS, 11 dịch vụ sẵn sàng | Maven 4.852s |
| `four-flows.e2e.test.js` trên image mới/stack cô lập | 4/4 PASS | 29.431s |
| `browser-avatar-cors.e2e.test.js` | 3/3 PASS: avatar đúng bytes, CORS, OTP Mailpit thật | 6.687s |
| Seed và trình duyệt demo | PASS các assertions bên dưới | Không đo riêng |

## Assertions demo đã chạy

1. Schema thật đã tồn tại trước seed. Chạy seed hai lần; lần thứ hai cung cấp mật khẩu khác để chứng minh không reset tài khoản hiện có.
2. Số bản ghi tất cả bảng trong ba schema không đổi sau lần seed thứ hai. Hash mật khẩu/role của hai tài khoản demo không đổi. Danh mục sentinel không thuộc seed được giữ nguyên.
3. Có đúng 2 course structures, 5 lessons; mọi lesson được node tham chiếu thuộc đúng courseId.
4. Đăng nhập UI bằng tài khoản học viên, giảng viên, admin. Catalog hiển thị 2 khóa học, mở đủ 5 bài học và xác nhận Markdown có nội dung; 2 bài blog render được.
5. Studio giảng viên hiển thị đúng 2 khóa học sở hữu; admin vào dashboard thành công. Có ảnh chụp kiểm chứng, Studio đã được xem trực tiếp để kiểm tra tiếng Việt.
6. Audit sau 4-flow E2E theo manifest tài nguyên của chính lượt chạy: users/courses/enrollments/lessonProgress/categories/posts/comments/tags/structures/lessons/minioObjects/emails còn lại đều 0. Dữ liệu demo không nằm trong tập fixture cần xóa.

## Lỗi phát hiện và xử lý trong nghiệm thu

- **BCrypt fallback**: test lỗi module load RED (3 PASS/1 FAIL), sau khi bỏ `new Function`, shim và `Math.random`, GREEN 4/4. Harness VM được cấp dependency thật và mật khẩu test rõ ràng.
- **Charset SQL seed**: trên MySQL thật, SELECT HEX chứng minh tiếng Việt bị đọc sai charset (RED). Bổ sung `--default-character-set=utf8mb4` vào mysql client, test GREEN; dựng lại riêng volume nghiệm thu và chạy seed/UI lại thành công. Không tự sửa dữ liệu cũ của người dùng.
- Lượt E2E đầu trên container cũ đang khởi động có 1 timeout ở thao tác ghi danh; cleanup sạch. Sau build, chờ toàn stack mới healthy và chạy lại: 4/4 PASS. Không dùng kết quả lần đầu làm bằng chứng đạt.
- Harness demo ban đầu đọc tiêu đề từ document Lesson không có trường title; sửa assertion lấy tiêu đề từ node CourseStructure đúng hợp đồng. Đây là sửa harness, không phải sửa API để khớp một kỳ vọng sai.

## Lệnh tái kiểm tra và artifacts

Các script `scripts/phase5-acceptance/{prepare,redirect-seed,acceptance}.cjs` giữ harness đã dùng để kiểm tra. Chạy từ repository root. `prepare.cjs` tạo cấu hình resolved ở `.codex-report-review/phase5`, tự loại thư mục này khỏi Git vì có credentials. Không công bố file compose resolved hoặc demo-password.

```powershell
node scripts/phase5-acceptance/prepare.cjs
# Dừng stack thường trước khi dùng cùng ports; không dùng down -v cho stack thường.
docker compose stop
docker compose -f .codex-report-review/phase5/compose.json up -d --wait --wait-timeout 240
node --env-file=.env scripts/phase5-acceptance/acceptance.cjs
node --env-file=.env --require=./scripts/phase5-acceptance/redirect-seed.cjs --test backend/web-app/src/test/js/four-flows.e2e.test.js
node --env-file=.env --require=./scripts/phase5-acceptance/redirect-seed.cjs --test backend/web-app/src/test/js/browser-avatar-cors.e2e.test.js
docker compose -f .codex-report-review/phase5/compose.json stop
docker compose up -d --wait --wait-timeout 240
```

Chỉ claim cold-start khi volume `lms-phase5-acceptance_*` mới; chạy lại trên volume cũ không phải bằng chứng cold-start mới. Harness tạo sentinel và dữ liệu demo trong stack cô lập, không chủ động xóa chúng. Kết quả JSON/ảnh được giữ ở `.codex-report-review/phase5` cục bộ.

## Giới hạn và ma trận

- Google OAuth thật chưa nghiệm thu; không giả lập credentials.
- Chưa chứng nhận triển khai production, stress/concurrency toàn hệ thống, Assignment/Chatbot của Trí hoặc mọi trường hợp CRUD trong các tính năng tổng hợp.
- Ma trận được hạ các dòng có phạm vi rộng hơn bằng chứng xuống TESTED: hiện 18 VERIFIED / 32 TESTED / 26 NOT_STARTED trên nhánh này. Các VERIFIED giữ lại theo hồ sơ trước đó; biên bản này không nâng chức năng mới lên VERIFIED.
- Không phát hành tag v1.0.0, không merge vào dev/develop/main.


**Kiểm tra sau khôi phục stack thường:** `docker compose up -d --wait` thành công, 11 dịch vụ healthy. Chạy lại 4-flow E2E trên stack thường: **4/4 PASS, 39.963s**. Audit độc lập theo manifest của lượt chạy xác nhận toàn bộ 13 nhóm bản ghi/object/email còn lại bằng 0. Stack nghiệm thu riêng đã dừng; volume riêng được giữ để tái kiểm tra, không dùng làm dữ liệu ứng dụng thường.
