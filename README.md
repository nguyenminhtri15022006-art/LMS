# EduLearn LMS - Hệ Thống Quản Lý Học Tập Trực Tuyến Hỗ Trợ AI & Code Judge

EduLearn LMS là nền tảng quản lý học tập và thực hành lập trình trực tuyến hiện đại theo kiến trúc Microservices, hỗ trợ ba nhóm người dùng chính: **Học viên (STUDENT)**, **Giảng viên (INSTRUCTOR)** và **Quản trị viên (ADMIN)**. Hệ thống tích hợp đầy đủ quy trình học tập từ lý thuyết (video, tài liệu), kiểm tra trắc nghiệm (Quiz, Question Bank), thực hành lập trình tự động chấm điểm (Code Judge), mạng xã hội học tập (Blog & Thảo luận) đến trợ lý học tập thông minh (Chatbot AI dựa trên LLM).

---

## 1. Thông Tin Đề Tài & Nhóm Phát Triển

- **Môn học**: Lập trình Web / Đồ án Cuối kỳ
- **Học kỳ**: Học kỳ 1 (2026 - 2027)
- **Đơn vị đào tạo**: Trường Đại học Sư phạm Kỹ thuật TP.HCM (HCMUTE)
- **Đề tài**: Xây dựng hệ thống EduLearn LMS theo kiến trúc Microservices với React & Spring Boot

### Thành viên nhóm

| STT | Họ và tên | MSSV | Vai trò | Trách nhiệm chính |
|:---:|---|:---:|---|---|
| 1 | **Nguyễn Minh Trí** | 23110340 | Trưởng nhóm / Fullstack | Assignment Service (Quiz & Code Judge), Chatbot Service (AI LLM), Dashboard, Leaderboard, Code Runner Sandbox |
| 2 | *(Thành viên 2)* | *(Cập nhật)* | Thành viên / Fullstack | Identity Service, Course Service, Blog Service, Frontend Auth/Course/Blog/Admin, MinIO & Security |

*(Chi tiết phân công xem tại [docs/TEAM_ROLES.md](docs/TEAM_ROLES.md))*.

---

## 2. Các Tính Năng Nổi Bật

- **Kiến trúc Microservices chuyên nghiệp**: Chia tách dịch vụ độc lập với Spring Boot, Spring Cloud Gateway, Eureka Service Discovery, OpenFeign, Resilience4j Circuit Breaker.
- **Xác thực & Phân quyền bảo mật (RBAC)**: Xác thực JWT phân cấp (Access/Refresh Token), đăng nhập mạng xã hội Google OAuth 2.0, mã xác thực Email OTP, bảo vệ route nhiều lớp tại Gateway và Frontend.
- **Quản lý khóa học linh hoạt**: Quản lý đa cấp danh mục, khóa học, chương mục, bài học video/tài liệu, lịch học tích hợp Google Meet, theo dõi tiến độ học tập chi tiết từng học viên.
- **Hệ thống Quiz & Ngân hàng câu hỏi nâng cao**: Tạo quiz thủ công hoặc tự động sinh từ Question Bank theo chủ đề; hỗ trợ câu hỏi đơn, nhiều lựa chọn, đúng/sai; chấm điểm tự động, giới hạn thời gian làm bài, lưu lịch sử chi tiết từng attempt.
- **Hệ thống Code Judge tự động**: Trình biên dịch và chấm bài tự động nhiều ngôn ngữ (C++, Java, Python, Go, Rust, JavaScript) tích hợp Judge0 API; quản lý testcase ẩn/công khai, giới hạn memory/time, thống kê xếp hạng (Leaderboard) theo thời gian thực.
- **Diễn đàn Blog & Tương tác cộng tác**: Đăng bài viết định dạng Markdown, hệ thống Tag/Category, bình luận phân cấp, bình chọn (Upvote/Downvote), upload ảnh thumbnail lên MinIO.
- **Trợ lý AI Chatbot & Tự sinh nội dung**: Chatbot hỗ trợ học tập thông minh sử dụng OpenCode/DeepSeek LLM, lưu trữ ngữ cảnh phiên chat trên Redis & MongoDB, tự động sinh đề thi trắc nghiệm và testcase bài tập lập trình từ mô tả.
- **Hạ tầng Cloud-Native**: Dockerized toàn bộ dịch vụ, Nginx reverse proxy, Object Storage MinIO (tương thích AWS S3), cơ sở dữ liệu phân tán (MySQL cho quan hệ, MongoDB cho document, Redis cho cache & rate limit).

---

## 3. Kiến Trúc Tổng Thể

Hệ thống được tổ chức theo mô hình **Monorepo**:

```text
DoAnCK/
├── backend/                       # Backend Microservices (Spring Boot 3.3.5, Java 21)
│   ├── service-registry/          # Eureka Server (Port 8761)
│   ├── api-gateway/               # Spring Cloud Gateway (Port 8080)
│   ├── identity-service/          # Auth, RBAC, OAuth, OTP, User Management (Port 8081)
│   ├── course-service/            # Course, Category, Lesson, Schedule (Port 8082)
│   ├── assignment-service/        # Quiz, Code Judge, Submission, Leaderboard (Port 8083)
│   ├── blog-service/              # Post, Comment, Tag, Vote (Port 8084)
│   └── chatbot-service/           # AI Assistant, Chat Session, AI Generation (Port 8085)
├── frontend/                      # Frontend Application (React 18, Vite 5, Port 5173)
├── docker/                        # Cấu hình container hạ tầng & DB init scripts
├── docs/                          # Hệ thống tài liệu kỹ thuật toàn diện
├── docker-compose.yml             # Triển khai toàn bộ hệ thống (Full stack)
├── docker-compose.db.yml          # Triển khai tầng dữ liệu (MySQL, MongoDB, Redis, MinIO)
├── docker-compose.prod.yml        # Triển khai Production tối ưu
└── .env.example                   # Biến môi trường mẫu
```

### Bản đồ phân bổ Service & Cổng mạng (Ports)

| Service | Host Port | Database / Storage | Mô tả chức năng |
|---|:---:|---|---|
| `frontend` | `5173` | — | Giao diện người dùng React 18 SPA (Nginx serve trong Docker) |
| `api-gateway` | `8080` | — | Single Entry Point, JWT verification, Rate limiting, Routing, Circuit Breaker |
| `service-registry` | `8761` | — | Eureka Service Discovery quản lý định danh và liveness của service |
| `identity-service` | `8081` | MySQL (`identity_lms`), MinIO | Quản lý người dùng, hồ sơ cá nhân, đăng nhập/đăng ký, OTP, Refresh Token |
| `course-service` | `8082` | MySQL (`course_lms`), MongoDB (`course_lms_mongo`), MinIO | Khóa học, cây nội dung bài học, tiến độ, lịch học Google Meet |
| `assignment-service`| `8083` | MySQL (`assignment_lms`), MongoDB (`assignment_lms_mongo`), MinIO | Quản lý Quiz, Question Bank, Bài tập lập trình, Lịch sử nộp bài & Chấm Code |
| `blog-service` | `8084` | MySQL (`blog_lms`), MinIO | Bài viết công nghệ, thảo luận, tag, bình luận, upvote/downvote |
| `chatbot-service` | `8085` | MongoDB (`chatbot_db`), Redis (`6379`) | Chatbot AI hỗ trợ học tập, sinh câu hỏi & testcase tự động qua LLM |
| `mysql-db` | `3306` | MySQL 8.4 | Lưu trữ dữ liệu quan hệ ACID cho 4 service chính |
| `mongo-db` | `27017` | MongoDB 7.0 | Lưu trữ tài liệu phi cấu trúc (Cây khóa học, Submission code, Lịch sử Chat) |
| `redis-cache` | `6379` | Redis 7.2 | Cache dữ liệu tra cứu nhanh, quản lý rate limit chatbot |
| `minio-storage` | `9000` / `9001` | MinIO S3 API / Console | Lưu trữ file ảnh đại diện, thumbnail khóa học/blog, tài liệu học tập |

---

## 4. Công Nghệ Sử Dụng

### Backend
- **Ngôn ngữ**: Java 21 (LTS)
- **Framework**: Spring Boot 3.3.5, Spring Cloud 2023.0.1
- **Discovery & Gateway**: Netflix Eureka Server, Spring Cloud Gateway
- **Bảo mật**: Spring Security 6, JJWT (0.12.5), Google OAuth Client
- **Truy xuất dữ liệu**: Spring Data JPA (Hibernate), Spring Data MongoDB, Flyway Migration
- **Giao tiếp liên dịch vụ**: Spring Cloud OpenFeign, Spring WebFlux (WebClient)
- **Khả năng chịu lỗi**: Resilience4j (Circuit Breaker, TimeLimiter)
- **Tiện ích**: Lombok, MapStruct 1.5.5.Final

### Frontend
- **Framework**: React 18, Vite 5
- **Định tuyến & Gọi API**: React Router DOM 6, Axios Interceptors
- **Trình soạn thảo Code**: CodeMirror 6 (hỗ trợ C++, Java, Python, JavaScript, Go, Rust)
- **Giao diện & Hiệu ứng**: Framer Motion, FontAwesome Icons, Pure CSS Modules
- **Markdown & Code Highlight**: React Markdown

### Cơ sở dữ liệu & Hạ tầng
- **Database**: MySQL 8.4, MongoDB 7.0
- **Cache & Rate Limit**: Redis 7.2 Alpine
- **Storage**: MinIO Object Storage
- **Containerization**: Docker, Docker Compose, Nginx Alpine

---

## 5. Vai Trò Người Dùng Trong Hệ Thống

| Vai trò | Quyền hạn chính |
|---|---|
| **Khách (GUEST)** | Xem trang chủ, danh sách khóa học công khai, chi tiết khóa học, danh sách bài viết blog, đăng ký/đăng nhập. |
| **Học viên (STUDENT)** | Đăng ký khóa học, học bài theo cấu trúc, đánh dấu hoàn thành, làm bài trắc nghiệm, nộp bài lập trình, xem bảng xếp hạng (Leaderboard), viết blog cá nhân, bình luận, chat với AI Assistant. |
| **Giảng viên (INSTRUCTOR)**| Tạo và quản lý khóa học, thiết lập cây nội dung bài học, tạo lịch học Google Meet, tạo ngân hàng câu hỏi, tạo bài kiểm tra Quiz, tạo bài tập lập trình & bộ testcase (có AI hỗ trợ sinh đề), xem bảng điểm và kết quả học viên. |
| **Quản trị viên (ADMIN)** | Giám sát trạng thái vận hành của các microservices (Health Dashboard), quản lý người dùng (khóa/mở tài khoản, phân quyền), phê duyệt/quản lý toàn bộ khóa học, bài tập và bài viết blog trên hệ thống. |

---

## 6. Yêu Cầu Môi Trường Cài Đặt

Trước khi khởi chạy hệ thống, đảm bảo máy tính đã cài đặt các phần mềm sau:

1. **Docker Desktop** (phiên bản 24.0+ khuyến nghị) & **Docker Compose v2**
2. **Java Development Kit (JDK)**: OpenJDK 21 trở lên (để chạy backend không qua docker)
3. **Apache Maven**: Phiên bản 3.9+
4. **Node.js**: Phiên bản 20 LTS (hoặc 18 LTS trở lên) & **npm** 10+
5. **Git**: Phiên bản 2.40+

---

## 7. Hướng Dẫn Cài Đặt & Khởi Chạy

### Cách 1: Chạy toàn bộ hệ thống bằng Docker Compose (Khuyên dùng)

1. **Sao chép cấu hình môi trường**:
   ```bash
   cp .env.example .env
   ```
   *(Trên Windows PowerShell: `Copy-Item .env.example .env`)*

2. **Khởi chạy toàn bộ hệ thống**:
   ```bash
   docker compose up --build -d
   ```

3. **Kiểm tra trạng thái container**:
   ```bash
   docker compose ps
   ```

4. **Dừng toàn bộ hệ thống**:
   ```bash
   docker compose down
   ```

---

### Cách 2: Chạy Database trên Docker & Chạy Code trên IDE / Terminal

Dành cho quá trình phát triển (development) từng service:

1. **Bật riêng tầng dữ liệu (MySQL, MongoDB, Redis, MinIO)**:
   ```bash
   docker compose -f docker-compose.db.yml up -d
   ```

2. **Khởi chạy Backend (theo đúng thứ tự phụ thuộc)**:
   - Terminal 1 (Eureka):
     ```bash
     mvn spring-boot:run -f backend/pom.xml -pl service-registry
     ```
   - Terminal 2 (API Gateway):
     ```bash
     mvn spring-boot:run -f backend/pom.xml -pl api-gateway
     ```
   - Terminal 3 (Identity Service):
     ```bash
     mvn spring-boot:run -f backend/pom.xml -pl identity-service
     ```
   - Terminal 4 (Course Service):
     ```bash
     mvn spring-boot:run -f backend/pom.xml -pl course-service
     ```
   - Terminal 5 (Assignment Service):
     ```bash
     mvn spring-boot:run -f backend/pom.xml -pl assignment-service
     ```
   - Terminal 6 (Blog Service):
     ```bash
     mvn spring-boot:run -f backend/pom.xml -pl blog-service
     ```
   - Terminal 7 (Chatbot Service):
     ```bash
     mvn spring-boot:run -f backend/pom.xml -pl chatbot-service
     ```

3. **Khởi chạy Frontend**:
   ```bash
   cd frontend
   npm install
   npm run dev
   ```

---

## 8. Các Địa Chỉ Truy Cập Mạng (URLs)

| Thành phần | Địa chỉ truy cập | Tài khoản mặc định |
|---|---|---|
| **Frontend Web App** | [http://localhost:5173](http://localhost:5173) | Đăng ký hoặc dùng Admin |
| **API Gateway** | [http://localhost:8080](http://localhost:8080) | Cổng API chung |
| **Eureka Discovery** | [http://localhost:8761](http://localhost:8761) | Xem danh sách service đăng ký |
| **MinIO Console** | [http://localhost:9001](http://localhost:9001) | `minioadmin` / `minioadmin123` |
| **Tài khoản Admin bootstrap** | Giao diện Web | `admin` / `admin` |

### Health Check URLs

- **API Gateway**: `http://localhost:8080/actuator/health/liveness`
- **Identity Service**: `http://localhost:8081/identity/actuator/health`
- **Course Service**: `http://localhost:8082/course/actuator/health/liveness`
- **Assignment Service**: `http://localhost:8083/assignment/actuator/health/liveness`
- **Blog Service**: `http://localhost:8084/blog/actuator/health/liveness`
- **Chatbot Service**: `http://localhost:8085/chatbot/actuator/health/liveness`

---

## 9. Hướng Dẫn Kiểm Thử (Testing)

### Backend Tests
```bash
# Chạy toàn bộ Unit & Integration tests cho tất cả module:
mvn clean test -f backend/pom.xml

# Chạy test cho một service cụ thể:
mvn test -f backend/pom.xml -pl identity-service
mvn test -f backend/pom.xml -pl assignment-service
```

### Frontend Tests
```bash
cd frontend
# Chạy Vitest suite
npm run test

# Chạy kiểm tra độ bao phủ mã nguồn (Coverage)
npm run test:coverage:core
```

---

## 10. Tiến Độ Tổng Quát & Liên Kết Tài Liệu Kỹ Thuật

Dự án được triển khai theo các mốc kỹ thuật từ khởi tạo kiến trúc đến hoàn thiện:

- 📋 [Tổng quan dự án (PROJECT_OVERVIEW.md)](docs/PROJECT_OVERVIEW.md)
- 📌 [Đặc tả yêu cầu chức năng & phi chức năng (REQUIREMENTS.md)](docs/REQUIREMENTS.md)
- 📊 [Ma trận đối chiếu tính năng 100% (FEATURE_PARITY_MATRIX.md)](docs/FEATURE_PARITY_MATRIX.md)
- 🏛️ [Tài liệu Kiến trúc hệ thống (ARCHITECTURE.md)](docs/ARCHITECTURE.md)
- 🗄️ [Thiết kế Cơ sở dữ liệu chi tiết (DATABASE_DESIGN.md)](docs/DATABASE_DESIGN.md)
- 🔌 [Đặc tả toàn bộ API Contract (API_CONTRACT.md)](docs/API_CONTRACT.md)
- 👥 [Phân quyền vai trò người dùng RBAC (USER_ROLES.md)](docs/USER_ROLES.md)
- 🤝 [Phân công trách nhiệm nhóm (TEAM_ROLES.md)](docs/TEAM_ROLES.md)
- 🗺️ [Lộ trình phát triển Roadmap (ROADMAP.md)](docs/ROADMAP.md)
- 📅 [Kế hoạch Commit chi tiết (COMMIT_PLAN.md)](docs/COMMIT_PLAN.md)
- 🌿 [Quy trình làm việc Git (GIT_WORKFLOW.md)](docs/GIT_WORKFLOW.md)
- 📏 [Bộ quy tắc chuẩn viết mã Coding Rules (CODING_RULES.md)](docs/CODING_RULES.md)
- 🧪 [Kế hoạch kiểm thử toàn diện (TESTING_PLAN.md)](docs/TESTING_PLAN.md)
- 🚀 [Hướng dẫn cài đặt & Triển khai (DEPLOYMENT_GUIDE.md)](docs/DEPLOYMENT_GUIDE.md)
- 🎯 [Bảng đối chiếu Rubric đánh giá đồ án (RUBRIC_MAPPING.md)](docs/RUBRIC_MAPPING.md)
- ❓ [Danh sách câu hỏi mở & Vấn đề tồn đọng (OPEN_QUESTIONS.md)](docs/OPEN_QUESTIONS.md)
- 💡 [Biên bản quyết định kiến trúc ADR (DECISIONS.md)](docs/DECISIONS.md)
- 📝 [Nhật ký tiến độ thành viên 1 (progress/member-1.md)](docs/progress/member-1.md)
- 📝 [Nhật ký tiến độ thành viên 2 (progress/member-2.md)](docs/progress/member-2.md)
- 🔗 [Nhật ký tích hợp hệ thống (progress/integration.md)](docs/progress/integration.md)
