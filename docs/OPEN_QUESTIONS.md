# Danh Sách Câu Hỏi Mở & Vấn Đề Kỹ Thuật — EduLearn LMS (OPEN_QUESTIONS.md)

Tài liệu này ghi nhận các điểm chưa rõ, các giả định kỹ thuật và các câu hỏi cần thống nhất với người dùng/giảng viên hướng dẫn trong quá trình phát triển dự án `DoAnCK`.

---

## 1. Thông Tin Thành Viên Nhóm
- **Câu hỏi**: Họ tên, Mã số sinh viên (MSSV) và tài khoản GitHub chính thức của Thành viên 2 là gì?
- **Tình trạng hiện tại**: Đang tạm để trống trong [README.md](../README.md) và [TEAM_ROLES.md](TEAM_ROLES.md).
- **Đề xuất**: Cần cập nhật sớm trước khi tạo nhánh `feature/member-2/*` và mở Pull Request đầu tiên.

---

## 2. Dịch Vụ Gửi Email Xác Thực OTP
- **Câu hỏi**: Quá trình kiểm thử local nên dùng máy chủ SMTP thật (Gmail SMTP) hay sử dụng máy chủ giả lập SMTP local (như MailHog / GreenMail container)?
- **Phân tích**: Gmail SMTP yêu cầu bật xác thực 2 bước và sinh App Password, có thể bị chặn bởi bộ lọc bảo mật hoặc giới hạn gửi thư nếu test liên tục.
- **Đề xuất**: Hỗ trợ cả hai chế độ: Cấu hình mặc định trong Docker Compose có thể bổ sung container `mailhog` (port 1025/8025) để xem email thử nghiệm tức thì trên giao diện web mà không lo lỗi xác thực Gmail.

---

## 3. Trình Chấm Mã Nguồn Tự Động (Judge0 Engine)
- **Câu hỏi**: Môi trường kiểm thử chấm code sẽ kết nối tới Judge0 như thế nào?
  1. Dùng Judge0 Cloud (RapidAPI - có giới hạn số lượt request miễn phí mỗi ngày)?
  2. Hay bổ sung dịch vụ Judge0 Server trực tiếp vào file `docker-compose.yml` (chạy hoàn toàn offline trên máy)?
- **Đề xuất**: Bổ sung profile `docker-compose.judge0.yml` để khi cần test chấm code offline, nhóm có thể bật Judge0 local tại port 2358.

---

## 4. Nhà Cung Cấp Mô Hình Ngôn Ngữ Lớn (LLM Provider cho Chatbot)
- **Câu hỏi**: Khóa `LLM_API_KEY` hiện tại trong file mẫu trỏ tới OpenCode / DeepSeek (`https://opencode.ai/zen/v1`). Nếu không có key thật thì hệ thống hoạt động ra sao?
- **Đề xuất**: Trong `chatbot-service`, cài đặt một cơ chế Fallback (hoặc Mock Assistant) tự động phát hiện nếu `LLM_API_KEY` chưa được thiết lập thì trả về câu trả lời hướng dẫn mẫu có cấu trúc, giúp giao diện không bị treo hoặc sập khi người dùng trải nghiệm mà chưa có API key.

---

## 5. Lỗi Thiếu Thư Mục `docker/mysql/init` Trong Project Mẫu
- **Phát hiện**: Trong file `docker-compose.yml` của project mẫu có khai báo:
  ```yaml
  volumes:
    - ./docker/mysql/init:/docker-entrypoint-initdb.d:ro
  ```
  Tuy nhiên, thư mục `docker` thực tế không tồn tại trong thư mục gốc của project mẫu. Khi chạy `docker compose up`, MySQL không có file SQL khởi tạo tự động các database `identity_lms`, `course_lms`, `assignment_lms`, `blog_lms`, dẫn đến lỗi kết nối JDBC nếu không có tham số `createDatabaseIfNotExist=true`.
- **Giải pháp trong DoAnCK**: Tạo thư mục `docker/mysql/init/01-init-databases.sql` trong `DoAnCK` để tự động khởi tạo sẵn cả 4 database quan hệ và cấp quyền cho user `root`. Ghi nhận chi tiết vào [DECISIONS.md](DECISIONS.md).

---

## 6. Tính Nhất Quán Của Tên Package Trong Backend
- **Phát hiện**: Trong project mẫu, module `service-registry` sử dụng package name `com.n23dccn161.serviceregistry`, trong khi tất cả các service còn lại đều tuân thủ chuẩn `com.lms.<servicename>`.
- **Đề xuất**: Chuẩn hóa toàn bộ backend trong `DoAnCK` về chung prefix `com.lms.*` (`com.lms.serviceregistry`, `com.lms.apigateway`, `com.lms.identityservice`, ...) để đảm bảo tính đồng bộ và chuyên nghiệp cao nhất.
