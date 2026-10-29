# Kiểm thử web-app

Yêu cầu: JDK 21, Maven và Node.js tương thích jsdom trong lockfile (đã kiểm tra với Node 24.19.0; nhánh Node 24 cần từ 24.15.0).

Chạy tại gốc repo:

```powershell
npm --prefix backend/web-app ci
mvn clean verify -pl web-app -f backend/pom.xml "-Dspring.profiles.active=test"
npm --prefix backend/web-app test
```

`CourseStudentRenderingTest` tạo HTML từ template thực tế và lấy Bootstrap 5.3.3 từ WebJar vào `backend/web-app/target/ui-fixtures/`. Test Node đọc fixture này, chạy Bootstrap và thao tác click trên DOM jsdom. Luôn chạy Maven trước Node sau khi sửa template hoặc chạy `clean`; thiếu fixture phải báo lỗi, không tự tạo DOM thay thế.

Test tự động sử dụng profile `test`, H2 in-memory và mock HTTP tại ranh giới Gateway/fetch. Không yêu cầu MySQL, MongoDB, Redis, MinIO hoặc Gateway đang chạy. Không có dữ liệu mock trong mã nguồn sản phẩm.

Phạm vi bổ sung Phase 3:

- 5 test Spring MVC kiểm tra modal trong HTML cuối cùng, thứ tự/cây nhiều cấp ở hai trang, URL nguy hiểm và hostname giả.
- 8 test `VideoSource` kiểm tra URL, hostname, media trực tiếp, chữ ký query, các trường video và chuẩn hóa YouTube/Vimeo.
- 8 test Node thực thi complete lesson, nút mở modal Bootstrap, phân trang Next/Previous, loading/empty/error/retry và dữ liệu chèn HTML/URL nguy hiểm.

Đây là kiểm thử cô lập, chưa thay thế kiểm thử trình duyệt và hệ thống thật. SMTP, OAuth, MinIO, phát video thật và Docker smoke/E2E vẫn thuộc nghiệm thu tích hợp.
