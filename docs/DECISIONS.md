# Quyết định kỹ thuật

## P3-UI-SECURITY — Hiển thị nội dung Learning Hub an toàn

- Mục tiêu: khắc phục bốn lỗi Phase 3 được người dùng yêu cầu, giữ phạm vi Member 2 và nhánh `feature/member-2-thymeleaf-web-app`.
- Dữ liệu tiêu đề, ghi chú, nội dung và thời gian từ API được gán bằng `textContent`; không ghép vào `innerHTML`. Nội dung thông báo là văn bản, giữ xuống dòng bằng CSS.
- Link phòng học chỉ chấp nhận URL HTTP(S) tuyệt đối, không credentials/ký tự điều khiển/backslash; tạo thuộc tính bằng DOM, dùng `rel="noopener noreferrer"`.
- `VideoSource` là dữ liệu trình bày đã kiểm tra: YouTube/Vimeo được xác định bằng hostname chính xác và đường dẫn video hợp lệ; link xem thông thường được chuẩn hóa sang URL nhúng HTTPS. URL HTTP(S) khác và đường dẫn media bắt đầu bằng `/` được dùng với `<video>`; URL nguy hiểm/không hợp lệ hiển thị trạng thái chưa sẵn sàng. Giữ nguyên query của URL media ký sẵn.
- Không sao chép cách kiểm tra `includes` không an toàn từ project mẫu; phát hiện được ghi tại `OPEN_QUESTIONS.md`.
- Dùng chung fragment Thymeleaf đệ quy cho cây ở trang chi tiết và Learning Hub. Modal phải thuộc content fragment của layout.
- Test UI dùng HTML do Spring MVC/Thymeleaf render, Bootstrap thật từ WebJar và jsdom. Chỉ mock phản hồi HTTP tại ranh giới Gateway/fetch; không tự dựng sẵn modal để che lỗi template.
- Giới hạn: jsdom không chứng minh phát media, bố cục hay tích hợp trình duyệt/Docker thật; không nâng trạng thái thành VERIFIED chỉ từ các test này.
