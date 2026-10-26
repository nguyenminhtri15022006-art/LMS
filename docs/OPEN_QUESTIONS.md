# Vấn đề phát hiện khi đối chiếu project mẫu

## P3-VIDEO-URL — Đã xử lý trong phạm vi web-app

- Nguồn đối chiếu chỉ đọc: `Learning-Management-System-main/frontend/src/features/courses/pages/student/DetailMyCoursePage.jsx`, hàm `VideoPlayer`.
- Mẫu phân loại iframe bằng `includes('youtube.com')`, `includes('vimeo.com')` hoặc `includes('/embed/')`. Cách này không kiểm tra giao thức/hostname; không được sao chép làm quy tắc bảo mật.
- Tái hiện ở web-app trước sửa: `javascript:alert(1)//youtube.com` vẫn xuất hiện trong `iframe src`; hostname giả `youtube.com.evil.example` cũng được coi là nguồn nhúng.
- Xử lý: kiểm tra URI và hostname thực tế; chỉ nhúng YouTube/Vimeo hợp lệ. Nguồn HTTP(S) khác được phát bằng video native. Quyết định chi tiết tại `DECISIONS.md`.
- Không còn mục CẦN XÁC NHẬN chặn bốn lỗi của lần sửa này. Việc phát video từ dịch vụ thật và tích hợp Docker vẫn chưa được nghiệm thu.
