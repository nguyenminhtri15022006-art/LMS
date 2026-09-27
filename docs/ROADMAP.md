# Lộ Trình Phát Triển Dự Án — EduLearn LMS (ROADMAP.md)

Lộ trình phát triển hệ thống EduLearn LMS được phân chia thành 14 cột mốc (Milestones) khoa học, kéo dài từ **27/09/2026** đến **29/10/2026**.

---

## Bảng Lộ Trình Tổng Thể Các Cột Mốc

```mermaid
gantt
    title Lộ trình phát triển EduLearn LMS (27/09 - 29/10/2026)
    dateFormat  YYYY-MM-DD
    section Khởi Tạo & Kiểm Kê
    M01: Khởi tạo tài liệu & Kiểm kê ban đầu      :2026-09-27, 2026-09-27
    M02: Hoàn thiện Ma trận đối chiếu 100%       :2026-09-28, 2026-09-29
    section Xây Dựng Khung & Cốt Lõi Backend
    M03: Khung Microservices, Eureka & Gateway    :2026-09-30, 2026-10-02
    M04: Xác thực JWT/OAuth & Hệ thống Quiz      :2026-10-03, 2026-10-05
    M05: Khóa học, Bài học & Bài tập Lập trình   :2026-10-06, 2026-10-08
    M06: Tiến độ học tập & Trình chấm Code Judge :2026-10-09, 2026-10-11
    M07: Diễn đàn Blog & Bảng điểm Gradebook     :2026-10-12, 2026-10-14
    section Xây Dựng Frontend & Tích Hợp
    M08: Giao diện Auth/Course & Quiz/Judge       :2026-10-15, 2026-10-17
    M09: Admin Quản trị & Dashboard/Chatbot AI    :2026-10-18, 2026-10-20
    section Kiểm Thử, Tích Hợp & Đóng Gói
    M10: Kiểm thử Backend & Frontend chuyên sâu   :2026-10-21, 2026-10-23
    M11: Tích hợp Hệ thống, MinIO, Redis & Gateway:2026-10-24, 2026-10-25
    M12: Docker hóa toàn bộ & Kiểm thử E2E        :2026-10-26, 2026-10-27
    section Bàn Giao & Phát Hành
    M13: Hoàn thiện Hồ sơ, Báo cáo & Video Demo   :2026-10-28, 2026-10-28
    M14: Tổng kiểm tra & Đóng gói Release v1.0.0  :2026-10-29, 2026-10-29
```

---

## Chi Tiết Từng Cột Mốc (Milestones)

### Cột mốc 1: Khởi Tạo Tài Liệu & Kiểm Kê Ban Đầu (27/09)
- **Mục tiêu**: Thiết lập toàn bộ quy định kiến trúc, bảo vệ project mẫu và khởi tạo bộ tài liệu kỹ thuật hoàn chỉnh.
- **Thành viên 1 (Trí)**: Khởi tạo `README.md`, `AGENTS.md`, `CONTRIBUTING.md`, `REQUIREMENTS.md`, `GIT_WORKFLOW.md`, khảo sát Assignment & Chatbot.
- **Thành viên 2**: Khảo sát & kiểm kê phân hệ Identity, Course, Blog, đối chiếu DTO, Entity, Controller của project mẫu.
- **Sản phẩm bàn giao**: Bộ 24 file tài liệu trong `DoAnCK/` và `docs/`.

### Cột mốc 2: Hoàn Thiện Ma Trận Đối Chiếu Tính Năng 100% (28–29/09)
- **Mục tiêu**: Hoàn tất bảng kiểm kê chi tiết 74 chức năng trên toàn hệ thống.
- **Thành viên 1 (Trí)**: Thiết kế chi tiết kiến trúc Code Judge Sandbox, AI LLM DeepSeek và ma trận Quiz (`assignment-service`, `chatbot-service`).
- **Thành viên 2**: Kiểm kê toàn diện `identity-service`, `course-service`, `blog-service` và ánh xạ API Contract.
- **Sản phẩm bàn giao**: `FEATURE_PARITY_MATRIX.md` đầy đủ 74 dòng kiểm soát.

### Cột mốc 3: Khung Dự Án & Hạ Tầng Cơ Bản (30/09 – 02/10)
- **Mục tiêu**: Xây dựng cấu trúc Maven Parent POM, Eureka Server, API Gateway và khung rỗng của các service.
- **Thành viên 1 (Trí)**: Thiết lập Parent POM, `service-registry`, `api-gateway`, khung `assignment-service` và `chatbot-service` (MongoDB + Redis).
- **Thành viên 2**: Thiết lập khung `identity-service`, `course-service`, `blog-service`, cấu hình kết nối MySQL.
- **Sản phẩm bàn giao**: Các service đăng ký thành công trên Eureka dashboard `:8761`.

### Cột mốc 4: Ngân Hàng Câu Hỏi, Quiz & Xác Thực JWT (03 – 05/10)
- **Mục tiêu**: Hoàn thiện toàn bộ luồng Quiz của Assignment và luồng Auth của Identity.
- **Thành viên 1 (Trí)**: Tạo ngân hàng câu hỏi (Single, Multiple, True/False), parser CSV/TXT, tạo bài Quiz và cơ chế khóa ảo chống spam.
- **Thành viên 2**: Đăng ký, đăng nhập BCrypt, JWT Access/Refresh Token, gửi OTP qua Gmail, Google OAuth.
- **Sản phẩm bàn giao**: Vượt qua các unit test Quiz và kiểm thử xác thực.

### Cột mốc 5: Bài Tập Lập Trình & Quản Lý Khóa Học (06 – 08/10)
- **Mục tiêu**: Triển khai nghiệp vụ bài toán lập trình và khóa học.
- **Thành viên 1 (Trí)**: Problem CRUD, quản lý testcase ẩn/hiện, upload testcases zip, lưu submission vào MongoDB.
- **Thành viên 2**: Course CRUD, Category, Cây cấu trúc bài học MongoDB (`CourseStructure`, `Lesson`).
- **Sản phẩm bàn giao**: API tạo bài toán lập trình và API tạo khóa học hoạt động ổn định.

### Cột mốc 6: Trình Chấm Code Judge0 & Tiến Độ Học Tập (09 – 11/10)
- **Mục tiêu**: Hoàn thiện luồng chấm bài tự động và luồng học tập của học viên.
- **Thành viên 1 (Trí)**: Tích hợp Judge0 API Engine, ThreadPool bất đồng bộ (`judge-executor`), chấm điểm testcases, tính toán verdict.
- **Thành viên 2**: Quy trình Enroll khóa học, cập nhật `lesson_progress`, lịch học Schedule và Google Meet.
- **Sản phẩm bàn giao**: Submit code nhận kết quả chấm tự động (Accepted / Wrong Answer).

### Cột mốc 7: Bảng Điểm Gradebook, Leaderboard & Diễn Đàn Blog (12 – 14/10)
- **Mục tiêu**: Hoàn thiện thống kê kết quả học tập và tính năng tương tác mạng xã hội.
- **Thành viên 1 (Trí)**: Gradebook tổng hợp điểm quiz, Leaderboard xếp hạng giải thuật toán của học viên.
- **Thành viên 2**: Blog Post, Tags, Nested Comments, Upvote/Downvote.
- **Sản phẩm bàn giao**: Hệ sinh thái tương tác và bảng điểm hoạt động chính xác.

### Cột mốc 8: Kết Nối Giao Diện Quiz, Code Judge & Khóa Học (15 – 17/10)
- **Mục tiêu**: Xây dựng UI React kết nối với các API đã phát triển.
- **Thành viên 1 (Trí)**: Trình soạn thảo CodeMirror 6, Giao diện làm bài Quiz đếm ngược, Giao diện xem kết quả thi.
- **Thành viên 2**: Giao diện Đăng nhập/Đăng ký, Trang danh sách và chi tiết khóa học, Giao diện học bài, Blog.
- **Sản phẩm bàn giao**: Frontend gọi API qua Gateway trơn tru, hiển thị đầy đủ Loading/Empty/Toast.

### Cột mốc 9: Trợ Lý Ảo Chatbot AI, Dashboard & Admin Quản Trị (18 – 20/10)
- **Mục tiêu**: Triển khai các tính năng nâng cao AI và Bảng điều khiển quản trị.
- **Thành viên 1 (Trí)**: Cửa sổ Chatbot AI hỗ trợ học tập, Giới hạn Rate limit Redis, Tính năng AI sinh Quiz/Testcase, Dashboard Học viên & Giảng viên.
- **Thành viên 2**: Admin User Management, Admin Course Management, Khóa/mở tài khoản.
- **Sản phẩm bàn giao**: Chatbot AI phản hồi mượt mà và Admin giám sát hệ thống.

### Cột mốc 10: Kiểm Thử Toàn Diện Backend & Frontend (21 – 23/10)
- **Mục tiêu**: Bổ sung kiểm thử phủ kín các luồng biên, bảo mật và ngoại lệ.
- **Thành viên 1 (Trí)**: Viết test cho Assignment, Code Judge, Chatbot; Vitest frontend test cho scoring & parsers.
- **Thành viên 2**: Viết test cho Identity, Course, Blog; Vitest frontend test cho hooks và context.
- **Sản phẩm bàn giao**: `mvn clean test` PASS 100%, frontend coverage đạt yêu cầu.

### Cột mốc 11: Tích Hợp Hệ Thống & Tối Ưu Hóa Hạ Tầng (24 – 25/10)
- **Mục tiêu**: Tinh chỉnh giao tiếp liên dịch vụ, MinIO Object Storage, Redis Cache và Circuit Breaker.
- **Thành viên 1 (Trí)**: Hoàn thiện cấu hình Circuit Breaker, Gateway Timeouts, xử lý timeout gọi LLM AI.
- **Thành viên 2**: Tối ưu hóa lưu trữ MinIO S3, cấu hình MySQL Flyway indexes (`V2`), dọn dẹp biến môi trường.
- **Sản phẩm bàn giao**: Hệ thống hoạt động liền mạch không lỗi kết nối.

### Cột mốc 12: Đóng Gói Docker & Kiểm Thử End-to-End (26 – 27/10)
- **Mục tiêu**: Đóng gói toàn bộ hệ thống vào Docker Compose và chạy thử nghiệm thực tế.
- **Thành viên 1 (Trí)**: Thực hiện kịch bản kiểm thử tích hợp toàn diện End-to-End (E2E) trên toàn bộ hệ thống Docker.
- **Thành viên 2**: Hoàn thiện Dockerfile đa tầng cho các service, viết kịch bản docker-compose khởi động tự động.
- **Sản phẩm bàn giao**: Toàn bộ hệ thống chạy ổn định chỉ với một lệnh Docker Compose.

### Cột mốc 13: Hoàn Thiện Hồ Sơ & Video Demo (28/10)
- **Mục tiêu**: Chuẩn bị đầy đủ tài liệu nộp bài, dữ liệu demo mẫu và video thuyết minh.
- **Thành viên 1 (Trí)**: Chuẩn bị bộ dữ liệu mẫu (Seeding Data), quay video demo hoàn chỉnh các tính năng hệ thống.
- **Thành viên 2**: Hoàn thiện README gốc, rà soát API Contract, tài liệu báo cáo.
- **Sản phẩm bàn giao**: Bộ hồ sơ đồ án hoàn chỉnh.

### Cột mốc 14: Tổng Kiểm Tra & Phát Hành Release v1.0.0 (29/10)
- **Mục tiêu**: Kiểm tra chốt chặn cuối cùng và đóng gói phiên bản chính thức.
- **Cả 2 thành viên**: Tạo Pull Request cuối cùng từ `develop` sang `main`, kiểm tra lint, test, gắn Git tag `v1.0.0`.
- **Sản phẩm bàn giao**: Mã nguồn nhánh `main` đạt trạng thái ổn định tuyệt đối.
