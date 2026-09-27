# Đặc Tả Yêu Cầu Hệ Thống — EduLearn LMS (REQUIREMENTS.md)

Tài liệu này xác định toàn bộ yêu cầu chức năng (Functional Requirements - FR) và phi chức năng (Non-Functional Requirements - NFR) của hệ sinh thái EduLearn LMS.

---

## 1. Yêu Cầu Chức Năng (Functional Requirements)

### 1.1 Phân Hệ Xác Thực & Quản Lý Người Dùng (Identity Service)

| Mã yêu cầu | Tên chức năng | Mô tả chi tiết | Vai trò |
|---|---|---|:---:|
| `FR-IDENTITY-001` | Đăng ký tài khoản cục bộ | Cho phép người dùng đăng ký tài khoản với username, email, password, họ tên, ngày sinh và vai trò (STUDENT/INSTRUCTOR). Mã hóa mật khẩu bằng BCrypt. | Guest |
| `FR-IDENTITY-002` | Xác thực Email qua OTP | Gửi mã OTP xác thực (6 số) có thời hạn 15 phút tới email người dùng khi đăng ký hoặc quên mật khẩu; giới hạn tối đa 5 lần thử. | Guest / All |
| `FR-IDENTITY-003` | Đăng nhập hệ thống | Xác thực username/password, cấp phát cặp Access Token (JWT 30 phút) và Refresh Token (7 ngày). | All |
| `FR-IDENTITY-004` | Đăng nhập Google OAuth 2.0 | Tích hợp xác thực Single Sign-On (SSO) qua Google ID Token; tự động tạo tài khoản hoặc liên kết tài khoản nếu đã tồn tại email. | Guest |
| `FR-IDENTITY-005` | Làm mới Token (Refresh Token) | Cho phép client gửi Refresh Token hợp lệ để nhận Access Token mới mà không cần đăng nhập lại; hỗ trợ thu hồi token khi đăng xuất. | Student, Instructor, Admin |
| `FR-IDENTITY-006` | Quên mật khẩu & Đặt lại mật khẩu | Gửi mã xác minh đặt lại mật khẩu qua email; người dùng xác thực mã và thiết lập mật khẩu mới an toàn. | Guest |
| `FR-IDENTITY-007` | Xem & Cập nhật Hồ sơ cá nhân | Xem thông tin tài khoản, cập nhật họ tên, tiểu sử (bio), số điện thoại, ngày sinh và upload ảnh đại diện (Avatar) lên MinIO S3. | Student, Instructor, Admin |
| `FR-IDENTITY-008` | Đổi mật khẩu | Cho phép người dùng đã đăng nhập thay đổi mật khẩu hiện tại bằng cách nhập mật khẩu cũ và xác nhận mật khẩu mới. | Student, Instructor, Admin |
| `FR-IDENTITY-009` | Quản lý người dùng hệ thống | Cho phép quản trị viên xem danh sách người dùng phân trang, lọc theo vai trò, tìm kiếm theo tên/email, khóa/mở khóa tài khoản (toggle active status). | Admin |
| `FR-IDENTITY-010` | Tạo tài khoản Quản trị viên mới | Cho phép Admin tạo thêm tài khoản với quyền ADMIN trực tiếp từ trang quản trị. | Admin |

---

### 1.2 Phân Hệ Quản Lý Khóa Học & Nội Dung Học Tập (Course Service)

| Mã yêu cầu | Tên chức năng | Mô tả chi tiết | Vai trò |
|---|---|---|:---:|
| `FR-COURSE-001` | Quản lý danh mục khóa học | CRUD danh mục khóa học (Categories); tự động tạo slug chuẩn SEO; gắn nhiều danh mục cho một khóa học. | Admin, Instructor |
| `FR-COURSE-002` | Tạo mới & Chỉnh sửa khóa học | Giảng viên tạo khóa học với tiêu đề, mô tả, cấp độ (Beginner/Intermediate/Advanced), thời lượng, ảnh thumbnail tải lên MinIO. | Instructor |
| `FR-COURSE-003` | Quản lý trạng thái khóa học | Chuyển đổi trạng thái khóa học giữa `PRIVATE` (chỉ giảng viên thấy), `PUBLIC` (công khai cho học viên đăng ký) và `LOCKED` (khóa). | Instructor, Admin |
| `FR-COURSE-004` | Quản lý cấu trúc cây bài học | Lưu trữ cây phân cấp chương mục (Chapters) và bài học (Lessons) dưới dạng Document trên MongoDB; hỗ trợ kéo thả/sắp xếp thứ tự bài học linh hoạt. | Instructor |
| `FR-COURSE-005` | Quản lý bài học đa định dạng | Hỗ trợ hai loại bài học: Bài học Video (lưu URL video/upload) và Bài học Tài liệu (nội dung Markdown, tài liệu đính kèm). | Instructor |
| `FR-COURSE-006` | Đăng ký ghi danh khóa học | Học viên đăng ký tham gia khóa học công khai; hệ thống tạo bản ghi `enrollments` theo dõi tiến trình. | Student |
| `FR-COURSE-007` | Theo dõi tiến độ học tập | Đánh dấu bài học đã hoàn thành (`lesson_progress`); tự động tính toán tỷ lệ % hoàn thành toàn khóa học cho từng học viên. | Student |
| `FR-COURSE-008` | Lên lịch học trực tuyến (Schedule) | Giảng viên tạo lịch học có thời gian bắt đầu, kết thúc, ghi chú; hỗ trợ tích hợp link phòng học Google Meet. | Instructor |
| `FR-COURSE-009` | Quản lý thông báo khóa học | Giảng viên đăng tải thông báo (Announcements) trong khóa học; học viên đã ghi danh có thể xem thông báo cập nhật. | Instructor, Student |
| `FR-COURSE-010` | Danh sách khóa học của tôi | Học viên xem danh sách các khóa học đã ghi danh kèm tiến độ; Giảng viên xem danh sách các khóa học do mình sáng lập. | Student, Instructor |

---

### 1.3 Phân Hệ Đánh Giá & Thực Hành Lập Trình (Assignment Service)

| Mã yêu cầu | Tên chức năng | Mô tả chi tiết | Vai trò |
|---|---|---|:---:|
| `FR-ASSIGNMENT-001`| Quản lý ngân hàng câu hỏi | Tạo, sửa, xóa câu hỏi trắc nghiệm thuộc nhiều chủ đề; hỗ trợ 3 định dạng: Chọn 1 đáp án (Single), Chọn nhiều đáp án (Multiple), Đúng/Sai (True/False). | Instructor, Admin |
| `FR-ASSIGNMENT-002`| Nhập câu hỏi từ file | Hỗ trợ nhập danh sách câu hỏi hàng loạt từ file CSV hoặc TXT theo cấu trúc quy ước chuẩn. | Instructor |
| `FR-ASSIGNMENT-003`| Tạo bài thi trắc nghiệm (Quiz) | Tạo bài kiểm tra từ ngân hàng câu hỏi hoặc tạo mới; thiết lập thời gian làm bài, điểm đậu, số lần làm tối đa, thời gian mở/đóng đề. | Instructor |
| `FR-ASSIGNMENT-004`| Chế độ hiển thị kết quả Quiz | Tùy chọn hiển thị kết quả kiểm tra: Ngay lập tức (`IMMEDIATELY`), Sau khi nộp bài (`AFTER_SUBMIT`), hoặc Sau khi hết hạn đề (`AFTER_DEADLINE`). | Instructor |
| `FR-ASSIGNMENT-005`| Quy trình thi trắc nghiệm | Học viên bắt đầu lượt thi (tạo attempt); hệ thống đếm ngược thời gian; hỗ trợ xáo trộn thứ tự câu hỏi và đáp án; tự động thu bài khi hết giờ. | Student |
| `FR-ASSIGNMENT-006`| Chấm điểm & Xem lịch sử Quiz | Hệ thống tự động chấm điểm bài trắc nghiệm; lưu trữ lịch sử từng lần thi, đáp án đã chọn, giải thích chi tiết và cập nhật bảng điểm (Gradebook). | Student, Instructor |
| `FR-ASSIGNMENT-007`| Quản lý bài tập lập trình | Giảng viên tạo bài toán lập trình (Coding Problem), thiết lập tiêu đề, slug, mô tả, độ khó (Easy, Medium, Hard), giới hạn bộ nhớ/thời gian và ngôn ngữ cho phép. | Instructor, Admin |
| `FR-ASSIGNMENT-008`| Quản lý bộ Testcase lập trình | Tạo và quản lý testcase cho bài tập lập trình: phân chia testcase công khai (hiển thị cho học viên) và testcase ẩn (dùng để chấm điểm tuyệt đối). | Instructor |
| `FR-ASSIGNMENT-009`| Trình soạn thảo & Chấm code | Tích hợp CodeMirror 6; hỗ trợ biên dịch và thực thi mã nguồn đa ngôn ngữ (C++, Java, Python, Go, Rust, JS) qua Judge0 API; trả về kết quả từng testcase. | Student |
| `FR-ASSIGNMENT-010`| Lưu trữ Submission & Bảng xếp hạng | Lưu lại toàn bộ mã nguồn các lần nộp bài của học viên trên MongoDB; tự động cập nhật thống kê điểm số và Bảng xếp hạng (Leaderboard) theo thời gian thực. | Student, Instructor |

---

### 1.4 Phân Hệ Diễn Đàn & Tương Tác Học Tập (Blog Service)

| Mã yêu cầu | Tên chức năng | Mô tả chi tiết | Vai trò |
|---|---|---|:---:|
| `FR-BLOG-001` | Soạn thảo & Đăng bài viết | Người dùng viết bài chia sẻ kiến thức định dạng Markdown, tải ảnh thumbnail, gắn thẻ phân loại (Tags); hỗ trợ lưu bản nháp (Draft) hoặc xuất bản (Published). | Student, Instructor |
| `FR-BLOG-002` | Quản lý danh mục & Thẻ bài viết | Hệ thống tự động quản lý Tags; hiển thị danh sách bài viết theo từng thẻ hoặc tìm kiếm theo tiêu đề. | All |
| `FR-BLOG-003` | Bình luận đa cấp (Comments) | Học viên và giảng viên có thể để lại bình luận dưới bài viết; hỗ trợ trả lời bình luận (Reply đa tầng). | Student, Instructor |
| `FR-BLOG-004` | Bình chọn bài viết & Bình luận | Hỗ trợ tính năng Upvote và Downvote cho bài viết và bình luận; mỗi người dùng chỉ được thực hiện 1 thao tác bình chọn trên 1 đối tượng. | Student, Instructor |
| `FR-BLOG-005` | Quản trị nội dung Blog | Quản trị viên có quyền ẩn các bài viết vi phạm tiêu chuẩn cộng đồng (`HIDDEN`) hoặc xóa bài viết. | Admin |

---

### 1.5 Phân Hệ Trợ Lý Học Tập Thông Minh (Chatbot Service)

| Mã yêu cầu | Tên chức năng | Mô tả chi tiết | Vai trò |
|---|---|---|:---:|
| `FR-CHATBOT-001` | Trò chuyện với Trợ lý AI | Hỗ trợ giao diện Chatbot giải đáp thắc mắc về khóa học, bài học và kiến thức lập trình; tích hợp mô hình ngôn ngữ lớn (OpenCode / DeepSeek). | Student, Instructor |
| `FR-CHATBOT-002` | Quản lý phiên hội thoại | Lưu trữ lịch sử tin nhắn trong MongoDB; phân chia phiên chat (Chat Sessions); tự động đặt tiêu đề cho phiên dựa trên nội dung ban đầu. | Student, Instructor |
| `FR-CHATBOT-003` | Tự động sinh câu hỏi Quiz bằng AI | Giảng viên cung cấp đoạn nội dung bài giảng, AI tự động phân tích và sinh danh sách câu hỏi trắc nghiệm kèm đáp án đúng và lời giải thích. | Instructor |
| `FR-CHATBOT-004` | Tự động sinh Testcase bài tập bằng AI| Giảng viên cung cấp yêu cầu bài toán lập trình, AI tự động sinh bộ dữ liệu đầu vào (Input) và đầu ra kỳ vọng (Expected Output) bao gồm các trường hợp biên. | Instructor |
| `FR-CHATBOT-005` | Giới hạn hạn ngạch gọi AI (Rate Limit)| Áp dụng bộ đếm trên Redis nhằm hạn chế số lượt gọi AI trong ngày (50 lượt/ngày cho Học viên, 100 lượt/ngày cho Giảng viên) nhằm chống lãng phí tài nguyên. | Student, Instructor |

---

## 2. Yêu Cầu Phi Chức Năng (Non-Functional Requirements)

### 2.1 Bảo Mật Hệ Thống (Security)
- `NFR-SECURITY-001`: Mật khẩu người dùng bắt buộc phải được băm bằng thuật toán **BCrypt** với độ phức tạp tối thiểu (strength 10).
- `NFR-SECURITY-002`: Toàn bộ kết nối API từ bên ngoài vào hệ thống đều phải đi qua **API Gateway**; các cổng trực tiếp của microservice nghiệp vụ được đóng hoặc chỉ nhận traffic nội bộ mạng Docker (`lms` bridge network).
- `NFR-SECURITY-003`: Xác thực phiên làm việc thông qua **JSON Web Token (JWT)** được ký bằng thuật toán HMAC-SHA256 với secret key tối thiểu 256 bits. Access token có hiệu lực ngắn (30 phút); Refresh token được mã hóa băm SHA-256 trước khi lưu trong cơ sở dữ liệu.
- `NFR-SECURITY-004`: API Gateway thực hiện xóa bỏ các header người dùng do client tự tạo (`X-User-Id`, `X-User-Role`, `X-User-Email`) trước khi inject thông tin giải mã từ JWT hợp lệ nhằm ngăn chặn hoàn toàn tấn công giả mạo danh tính (Header Spoofing).
- `NFR-SECURITY-005`: Dữ liệu tải lên (Avatar, Thumbnail, Assignment Files) phải được kiểm tra định dạng MIME type và giới hạn kích thước tối đa (5MB - 20MB) trước khi đẩy lên MinIO S3.

### 2.2 Hiệu Năng & Khả Năng Mở Rộng (Performance & Scalability)
- `NFR-PERF-001`: Thời gian phản hồi (Response Time) cho các API truy vấn cơ bản (danh sách khóa học, xem bài học, xem bài viết) không vượt quá **500ms** trong điều kiện mạng nội bộ.
- `NFR-PERF-002`: Hệ thống sử dụng **Redis Cache** để lưu trữ thông tin phiên chat, kết quả đếm rate limit và các dữ liệu đọc nhiều, giảm tải áp lực truy vấn cho cơ sở dữ liệu quan hệ.
- `NFR-PERF-003`: Tác vụ chấm code lập trình (Code Judge) và gọi LLM AI phải được thực thi **bất đồng bộ (Asynchronous)** qua ThreadPool Executor chuyên dụng (`judge-executor`) để không làm nghẽn luồng xử lý HTTP request chính.
- `NFR-PERF-004`: Áp dụng cơ chế phân trang chuẩn (Spring Pageable) cho tất cả các endpoint trả về danh sách dữ liệu (mặc định pageSize = 10 hoặc 20), ngăn chặn tình trạng tràn bộ nhớ (Out Of Memory).

### 2.3 Khả Năng Sẵn Sàng & Chịu Lỗi (Availability & Fault Tolerance)
- `NFR-AVAIL-001`: Tích hợp **Resilience4j Circuit Breaker** tại API Gateway cho từng tuyến dịch vụ; tự động ngắt mạch (Open state) khi tỷ lệ lỗi vượt ngưỡng 50% trong sliding window 10 lần gọi để bảo vệ hạ tầng downstream.
- `NFR-AVAIL-002`: Đăng ký và giám sát sức khỏe dịch vụ qua **Netflix Eureka Server**; tự động phát hiện và loại bỏ các instance service gặp sự cố khỏi danh sách định tuyến.
- `NFR-AVAIL-003`: Cung cấp các endpoint Actuator Health (`/actuator/health/liveness`) trên từng service để phục vụ Docker healthcheck và trang giám sát trạng thái của Quản trị viên.

### 2.4 Khả Năng Sử Dụng (Usability & User Experience)
- `NFR-UI-001`: Giao diện người dùng phải hiển thị đầy đủ 4 trạng thái tương tác: Đang tải dữ liệu (Loading Spinner / Skeleton), Dữ liệu rỗng (Empty State), Thông báo thành công (Success Toast) và Thông báo lỗi chi tiết (Error Toast).
- `NFR-UI-002`: Trình soạn thảo lập trình (Code Editor) phải hỗ trợ tính năng làm nổi bật cú pháp (Syntax Highlighting) tương ứng với từng ngôn ngữ lập trình được chọn, hiển thị số dòng và hỗ trợ tự động thụt đầu dòng.
- `NFR-UI-003`: Hệ thống hỗ trợ hiển thị nội dung định dạng Markdown chuẩn (tiêu đề, in đậm, danh sách, khối code, bảng biểu) cho bài giảng, bài viết blog và tin nhắn trợ lý ảo AI.

### 2.5 Sao Lưu, Phục Hồi & Bảo Trì (Backup & Maintainability)
- `NFR-MAINT-001`: Quản lý tiến hóa lược đồ cơ sở dữ liệu thông qua công cụ di trú **Flyway**; mọi thay đổi bảng biểu phải được viết thành các migration script phiên bản tuần tự (`V1__...sql`, `V2__...sql`).
- `NFR-MAINT-002`: Hệ thống cung cấp cơ chế dọn dẹp tự động (Scheduled Jobs) định kỳ hàng ngày để xóa bỏ các token xác thực đã hết hạn, các bài thi trắc nghiệm bị bỏ dở quá 7 ngày, và các submission bị kẹt trạng thái quá 10 phút.
- `NFR-MAINT-003`: Dữ liệu phân tán được lưu trữ trên các Docker Named Volumes độc lập (`mysql-data`, `mongo-data`, `redis-data`, `minio-data`), đảm bảo dữ liệu không bị mất khi khởi động lại hoặc nâng cấp container.

### 2.6 Logging & Xử Lý Ngoại Lệ (Logging & Error Handling)
- `NFR-LOG-001`: Tất cả các service backend phải chuẩn hóa cấu trúc phản hồi lỗi theo mẫu đối tượng `ApiResponse<T>` thống nhất: chứa mã trạng thái (code), thông điệp rõ nghĩa (message) và dữ liệu (result nếu có).
- `NFR-LOG-002`: Triển khai bộ xử lý ngoại lệ tập trung (`@ControllerAdvice` / `GlobalExceptionHandler`) ở từng microservice để bắt và chuyển đổi toàn bộ `MethodArgumentNotValidException`, `AppException` và `Exception` hệ thống thành HTTP response chuẩn.
- `NFR-LOG-003`: Ghi log chi tiết (SLF4J/Logback) cho các luồng xử lý quan trọng: Đăng nhập thất bại, Lỗi kết nối liên dịch vụ Feign Client, Lỗi chấm bài Code Judge, Lỗi gọi API LLM kèm theo Correlation ID / Trace ID tương ứng.
