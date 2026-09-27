# Đặc Tả Giao Diện Lập Trình — EduLearn LMS (API_CONTRACT.md)

Tài liệu này quy định chi tiết toàn bộ các Endpoint API của hệ sinh thái EduLearn LMS.  
Mọi lời gọi từ Frontend bắt buộc phải đi qua **API Gateway** (`http://localhost:8080`) với tiền tố định tuyến `/api/<service>`.

---

## Cấu Trúc Phản Hồi Chuẩn (Standard Response Format)

Mọi API thành công hay thất bại đều trả về cấu trúc đối tượng `ApiResponse<T>` thống nhất:

```json
{
  "code": 1000,
  "message": "Success",
  "result": { ... }
}
```

Mã phản hồi lỗi:
- `1000`: Thành công
- `1001`: Không xác thực / Token không hợp lệ (`UNAUTHENTICATED`)
- `1002`: Không có quyền truy cập (`UNAUTHORIZED`)
- `1003`: Dữ liệu đầu vào không hợp lệ (`INVALID_KEY` / `VALIDATION_FAILED`)
- `1004`: Bản ghi không tồn tại (`USER_NOT_EXISTED`, `COURSE_NOT_FOUND`, etc.)
- `1005`: Bản ghi đã tồn tại (`USER_EXISTED`, `SLUG_EXISTED`, etc.)
- `9999`: Lỗi máy chủ nội bộ (`UNCATEGORIZED_EXCEPTION`)

---

## 1. Phân Hệ Xác Thực & Người Dùng (`identity-service`)

### 1.1 `POST /api/identity/auth/register`
- **Mô tả**: Đăng ký tài khoản người dùng mới.
- **Vai trò cho phép**: `GUEST` (Public).
- **Service xử lý**: `identity-service`.
- **Request Body**:
  ```json
  {
    "username": "student_01",
    "password": "Password123@",
    "fullname": "Nguyen Van A",
    "email": "student01@gmail.com",
    "phone": "0987654321",
    "dob": "2002-05-15",
    "role": "STUDENT"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "code": 1000,
    "message": "User registered successfully",
    "result": {
      "userId": "uuid-string",
      "username": "student_01",
      "email": "student01@gmail.com",
      "role": "STUDENT"
    }
  }
  ```
- **Mã lỗi thường gặp**: 1003 (Validation failed), 1005 (Username/Email already exists).

### 1.2 `POST /api/identity/auth/login`
- **Mô tả**: Đăng nhập hệ thống bằng username và password.
- **Vai trò cho phép**: `GUEST` (Public).
- **Service xử lý**: `identity-service`.
- **Request Body**:
  ```json
  {
    "username": "student_01",
    "password": "Password123@",
    "deviceInfo": "Chrome / Windows 11",
    "ipAddress": "192.168.1.10"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "code": 1000,
    "message": "Login success",
    "result": {
      "accessToken": "eyJhbGciOiJIUzI1...",
      "refreshToken": "7c8e9b...",
      "accessTokenExpiry": "2026-09-27T17:35:00Z",
      "isNewUser": false,
      "user": {
        "userId": "uuid-string",
        "username": "student_01",
        "fullname": "Nguyen Van A",
        "email": "student01@gmail.com",
        "role": "STUDENT",
        "avatarUrl": "https://..."
      }
    }
  }
  ```
- **Mã lỗi thường gặp**: 1001 (Sai thông tin đăng nhập hoặc tài khoản bị khóa).

### 1.3 `POST /api/identity/auth/social-login/{provider}`
- **Mô tả**: Đăng nhập mạng xã hội (Google OAuth 2.0).
- **Path Parameter**: `provider`: `google`.
- **Request Body**: `{ "idToken": "google-oauth-id-token" }`
- **Response (200 OK)**: Cấu trúc tương tự API đăng nhập.

### 1.4 `POST /api/identity/auth/refresh`
- **Mô tả**: Cấp lại Access Token mới từ Refresh Token.
- **Request Body**: `{ "refreshToken": "raw-refresh-token-string" }`
- **Response (200 OK)**: Trả về cặp Access Token và Refresh Token mới (Token Rotation).

### 1.5 `POST /api/identity/auth/send-otp` & `POST /api/identity/auth/verify-otp`
- **Mô tả**: Gửi mã xác thực qua Email và kiểm tra tính hợp lệ của mã OTP.
- **Request Body send-otp**: `{ "email": "user@gmail.com", "type": "VERIFY_EMAIL" }`
- **Request Body verify-otp**: `{ "email": "user@gmail.com", "otp": "123456", "type": "VERIFY_EMAIL" }`

### 1.6 `GET /api/identity/users/me` & `PUT /api/identity/users/me`
- **Mô tả**: Xem và cập nhật thông tin hồ sơ của chính mình.
- **Header**: `Authorization: Bearer <accessToken>`.
- **Vai trò cho phép**: `STUDENT`, `INSTRUCTOR`, `ADMIN`.

### 1.7 `GET /api/identity/admin/users`
- **Mô tả**: Quản trị viên lấy danh sách toàn bộ người dùng phân trang và lọc theo vai trò.
- **Header**: `Authorization: Bearer <accessToken>`.
- **Vai trò cho phép**: `ADMIN`.
- **Query Parameters**: `page` (int), `size` (int), `role` (String), `search` (String).

---

## 2. Phân Hệ Quản Lý Khóa Học (`course-service`)

### 2.1 `GET /api/course/categories`
- **Mô tả**: Lấy danh sách tất cả các danh mục khóa học.
- **Vai trò cho phép**: Public (`GUEST`, `STUDENT`, `INSTRUCTOR`, `ADMIN`).

### 2.2 `GET /api/course/courses`
- **Mô tả**: Lấy danh sách khóa học công khai có phân trang và bộ lọc.
- **Query Parameters**: `page` (default 0), `size` (default 10), `categoryId` (optional), `level` (optional), `search` (optional).

### 2.3 `GET /api/course/courses/{id}` & `GET /api/course/courses/{id}/public`
- **Mô tả**: Xem thông tin chi tiết của một khóa học.

### 2.4 `POST /api/course/courses`
- **Mô tả**: Giảng viên tạo mới một khóa học.
- **Vai trò cho phép**: `INSTRUCTOR`.
- **Header**: `Authorization: Bearer <accessToken>` (Gateway inject `X-User-Id`).
- **Request Body**:
  ```json
  {
    "title": "Lập trình Spring Boot Microservices từ cơ bản tới nâng cao",
    "description": "Khóa học chi tiết về kiến trúc hệ thống phân tán...",
    "level": "INTERMEDIATE",
    "duration": 1800,
    "categoryIds": [1, 3],
    "learningPoints": ["Hiểu kiến trúc Microservices", "Thành thạo Spring Cloud"],
    "requirements": ["Đã học Java căn bản"]
  }
  ```

### 2.5 `GET /api/course/courses/{id}/structure` & `PUT /api/course/courses/{id}/structure`
- **Mô tả**: Xem hoặc cập nhật cây chương mục và danh sách bài học của khóa học (lưu trên MongoDB).
- **Vai trò cho phép**: `STUDENT` (chỉ xem nếu đã ghi danh), `INSTRUCTOR` (sửa nếu là chủ sở hữu), `ADMIN`.

### 2.6 `POST /api/course/courses/{id}/enroll`
- **Mô tả**: Học viên đăng ký ghi danh vào một khóa học.
- **Vai trò cho phép**: `STUDENT`.
- **Response (200 OK)**: Trả về trạng thái ghi danh `ACTIVE`.

### 2.7 `POST /api/course/courses/{courseId}/lessons/{lessonId}/progress`
- **Mô tả**: Đánh dấu học viên đã hoàn thành một bài học cụ thể.
- **Vai trò cho phép**: `STUDENT`.

### 2.8 `GET /api/course/courses/{courseId}/schedules` & `POST /api/course/courses/{courseId}/schedules`
- **Mô tả**: Xem và tạo lịch học trực tuyến kèm link Google Meet.
- **Vai trò tạo lịch**: `INSTRUCTOR`.

---

## 3. Phân Hệ Bài Tập & Đánh Giá (`assignment-service`)

### 3.1 `GET /api/assignment/questions` & `POST /api/assignment/questions`
- **Mô tả**: Quản lý ngân hàng câu hỏi trắc nghiệm (Question Bank).
- **Vai trò cho phép**: `INSTRUCTOR`, `ADMIN`.
- **Request Body tạo câu hỏi**:
  ```json
  {
    "courseId": "course-uuid",
    "content": "Giao thức nào được sử dụng để khám phá dịch vụ trong Spring Cloud?",
    "type": "SINGLE",
    "topic": "Microservices",
    "explanation": "Eureka là giải pháp Service Discovery phổ biến của Netflix OSS.",
    "score": 10,
    "answers": [
      { "content": "Netflix Eureka", "isCorrect": true, "orderIndex": 1 },
      { "content": "Spring Security", "isCorrect": false, "orderIndex": 2 },
      { "content": "MySQL", "isCorrect": false, "orderIndex": 3 },
      { "content": "Redis", "isCorrect": false, "orderIndex": 4 }
    ]
  }
  ```

### 3.2 `POST /api/assignment/questions/import`
- **Mô tả**: Nhập danh sách câu hỏi hàng loạt từ file CSV hoặc TXT.
- **Content-Type**: `multipart/form-data`.
- **Vai trò cho phép**: `INSTRUCTOR`.

### 3.3 `POST /api/assignment/quizzes`
- **Mô tả**: Giảng viên tạo mới một bài kiểm tra Quiz.
- **Request Body**:
  ```json
  {
    "courseId": "course-uuid",
    "title": "Kiểm tra giữa kỳ: Kiến trúc Microservices",
    "duration": 45,
    "passScore": 50,
    "maxAttempts": 2,
    "shuffleQuestions": true,
    "shuffleAnswers": true,
    "showResult": "AFTER_SUBMIT",
    "questionIds": [101, 102, 103, 104]
  }
  ```

### 3.4 `POST /api/assignment/quizzes/{quizId}/attempts`
- **Mô tả**: Học viên bắt đầu một lượt thi trắc nghiệm.
- **Vai trò cho phép**: `STUDENT`.
- **Response**: Trả về `attemptId`, thời gian bắt đầu, thời gian hết hạn (`expiresAt`), danh sách câu hỏi và các lựa chọn (đáp án đúng bị ẩn).

### 3.5 `POST /api/assignment/quizzes/{quizId}/attempts/{attemptId}/submit`
- **Mô tả**: Nộp bài thi trắc nghiệm và nhận kết quả chấm điểm.
- **Request Body**:
  ```json
  {
    "answers": [
      { "questionId": 101, "selectedAnswerIds": [1] },
      { "questionId": 102, "selectedAnswerIds": [4, 5] }
    ]
  }
  ```

### 3.6 `POST /api/assignment/problems`
- **Mô tả**: Tạo bài toán lập trình mới (Coding Problem).
- **Vai trò cho phép**: `INSTRUCTOR`, `ADMIN`.
- **Request Body**:
  ```json
  {
    "courseId": "course-uuid",
    "lessonId": "lesson-uuid",
    "title": "Tìm kiếm đường đi ngắn nhất (Dijkstra)",
    "slug": "dijkstra-shortest-path",
    "description": "Cho đồ thị có hướng có trọng số...",
    "difficulty": "MEDIUM",
    "timeLimitMs": 1000,
    "memoryLimitMb": 256,
    "allowedLangs": ["CPP", "JAVA", "PYTHON"]
  }
  ```

### 3.7 `POST /api/assignment/problems/{problemId}/testcases`
- **Mô tả**: Thêm testcase cho bài tập lập trình (phân loại ẩn hoặc công khai).

### 3.8 `POST /api/assignment/submissions`
- **Mô tả**: Học viên nộp mã nguồn giải bài tập lập trình để chấm tự động qua Judge0.
- **Vai trò cho phép**: `STUDENT`.
- **Request Body**:
  ```json
  {
    "problemId": 12,
    "language": "CPP",
    "sourceCode": "#include <iostream>\nusing namespace std;\nint main() { ... }"
  }
  ```
- **Response (200 OK)**: Trả về `submissionId` ở trạng thái `PENDING`.

### 3.9 `GET /api/assignment/submissions/{submissionId}`
- **Mô tả**: Kiểm tra trạng thái và kết quả chi tiết từng testcase của bài nộp.

### 3.10 `GET /api/assignment/submissions/leaderboard/{problemId}`
- **Mô tả**: Xem bảng xếp hạng top học viên giải bài toán nhanh và tối ưu nhất.

---

## 4. Phân Hệ Diễn Đàn & Blog (`blog-service`)

### 4.1 `GET /api/blog/posts`
- **Mô tả**: Xem danh sách bài viết blog công khai có phân trang, lọc theo tag và từ khóa tìm kiếm.
- **Vai trò cho phép**: Public (`GUEST`, `STUDENT`, `INSTRUCTOR`, `ADMIN`).

### 4.2 `GET /api/blog/posts/{slugOrId}`
- **Mô tả**: Đọc chi tiết bài viết blog, tự động tăng lượt xem (`view_count`).

### 4.3 `POST /api/blog/posts`
- **Mô tả**: Tạo bài viết mới (hỗ trợ Markdown, tải ảnh thumbnail).
- **Vai trò cho phép**: `STUDENT`, `INSTRUCTOR`.

### 4.4 `POST /api/blog/posts/{postId}/comments`
- **Mô tả**: Gửi bình luận hoặc trả lời bình luận (Reply) dưới bài viết.
- **Request Body**: `{ "content": "Bài viết giải thích rất dễ hiểu!", "parentId": null }`

### 4.5 `POST /api/blog/posts/{postId}/vote`
- **Mô tả**: Bình chọn Upvote hoặc Downvote cho bài viết.
- **Request Body**: `{ "voteType": "UPVOTE" }`

---

## 5. Phân Hệ Trợ Lý Ảo & AI (`chatbot-service`)

### 5.1 `POST /api/chatbot/chat`
- **Mô tả**: Trò chuyện với trợ lý học tập AI.
- **Vai trò cho phép**: `STUDENT`, `INSTRUCTOR`.
- **Header**: `Authorization: Bearer <accessToken>`.
- **Request Body**:
  ```json
  {
    "sessionId": "session-uuid",
    "content": "Giải thích giúp em cơ chế hoạt động của Circuit Breaker trong Microservices?",
    "contextType": "COURSE",
    "contextRefId": "course-uuid"
  }
  ```
- **Response (200 OK)**:
  ```json
  {
    "code": 1000,
    "message": "Success",
    "result": {
      "sessionId": "session-uuid",
      "reply": "Circuit Breaker là một mẫu thiết kế quan trọng...",
      "tokenUsed": 320
    }
  }
  ```

### 5.2 `GET /api/chatbot/sessions` & `POST /api/chatbot/sessions`
- **Mô tả**: Lấy danh sách lịch sử các phiên hội thoại hoặc tạo một cuộc trò chuyện mới.

### 5.3 `POST /api/chatbot/generate/quiz`
- **Mô tả**: Yêu cầu AI tự động sinh câu hỏi trắc nghiệm từ văn bản bài giảng.
- **Vai trò cho phép**: `INSTRUCTOR`.
- **Request Body**:
  ```json
  {
    "sourceText": "Spring Cloud Gateway cung cấp cơ chế định tuyến...",
    "numQuestions": 5,
    "difficulty": "MEDIUM"
  }
  ```

### 5.4 `POST /api/chatbot/generate/testcases`
- **Mô tả**: Yêu cầu AI tự động sinh bộ testcases cho bài tập lập trình từ mô tả bài toán.
- **Vai trò cho phép**: `INSTRUCTOR`.
- **Request Body**:
  ```json
  {
    "problemDescription": "Cho hai số nguyên a và b (-10^9 <= a, b <= 10^9). Tính tổng a + b.",
    "numCases": 10
  }
  ```
