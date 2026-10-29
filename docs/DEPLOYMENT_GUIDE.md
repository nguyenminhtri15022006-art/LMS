# Hướng Dẫn Cài Đặt & Triển Khai Hệ Thống — EduLearn LMS (DEPLOYMENT_GUIDE.md)

Tài liệu này cung cấp hướng dẫn từng bước để thiết lập môi trường, cấu hình biến, khởi chạy hệ thống cục bộ (Local Development) và triển khai đóng gói hoàn chỉnh bằng Docker Compose.

---

## 1. Yêu Cầu Phần Cứng & Môi Trường Cài Đặt

### Cấu hình máy tính khuyến nghị:
- **Hệ điều hành**: Windows 10/11, macOS, hoặc Linux (Ubuntu 22.04+).
- **RAM**: Tối thiểu 16GB (khuyến nghị 32GB để chạy đồng thời toàn bộ 11 Docker containers mượt mà).
- **Ổ cứng**: Tối thiểu 20GB dung lượng trống.

### Các phần mềm bắt buộc phải cài đặt trước:
1. **Docker Desktop** (bản 24.0+ trở lên) kèm **Docker Compose v2**.
2. **Java Development Kit (JDK)**: OpenJDK 21 LTS.
3. **Apache Maven**: Phiên bản 3.9 trở lên.
4. **Node.js**: Phiên bản 20 LTS (hoặc tối thiểu 18 LTS) kèm **npm** 10+.
5. **Git**: Phiên bản 2.40 trở lên.

---

## 2. Chuẩn Bị File Cấu Hình Môi Trường (`.env`)

Mọi thông số kết nối cơ sở dữ liệu, cổng dịch vụ, khóa bảo mật đều được quản lý tập trung qua file `.env` tại thư mục gốc của dự án.

### Bước thực hiện:
1. Tạo file `.env` từ file mẫu `.env.example`:
   ```bash
   # Trên Linux / macOS:
   cp .env.example .env

   # Trên Windows PowerShell:
   Copy-Item .env.example .env
   ```
2. Mở file `.env` và rà soát/cập nhật các thông số cần thiết:
   - `JWT_SIGNER_KEY`: Thay đổi chuỗi secret key có độ dài tối thiểu 256 bits.
   - `MAIL_USERNAME` & `MAIL_PASSWORD`: Cấu hình tài khoản Gmail và Mật khẩu ứng dụng (App Password) để gửi OTP thực tế.
   - `GOOGLE_CLIENT_ID` & `GOOGLE_CLIENT_SECRET`: Khóa Google OAuth nếu muốn thử nghiệm đăng nhập Google SSO.
   - `LLM_API_KEY`: Khóa API của nhà cung cấp LLM (OpenCode / DeepSeek) để sử dụng trợ lý ảo Chatbot.
   - `JUDGE0_API_KEY`: Khóa API Judge0 để chấm bài lập trình.

---

## 3. Cách 1: Triển Khai Toàn Bộ Bằng Docker Compose (Khuyên dùng)

Đây là phương thức nhanh chóng và đồng bộ nhất để chạy toàn bộ hệ sinh thái mà không cần cài đặt cục bộ Java hay Node.js trên máy tính.

### 3.1 Khởi động hệ thống
Mở terminal tại thư mục gốc `DoAnCK/` và chạy:
```bash
docker compose up --build -d
```

### 3.2 Kiểm tra trạng thái các container
```bash
docker compose ps
```
Xác nhận tất cả 11 services đều hiển thị trạng thái `healthy` hoặc `Up`.

### 3.3 Xem log vận hành
```bash
# Xem log tổng hợp:
docker compose logs -f

# Xem log của một service cụ thể:
docker compose logs -f api-gateway
docker compose logs -f assignment-service
docker compose logs -f chatbot-service
```

### 3.4 Dừng hệ thống
```bash
# Dừng các container nhưng giữ lại dữ liệu trong volumes:
docker compose down

# Dừng và xóa toàn bộ dữ liệu volumes (Làm sạch hoàn toàn):
docker compose down -v
```

---

## 4. Cách 2: Chạy Tầng Dữ Liệu Bằng Docker & Chạy Code Bằng IDE / Terminal

Phương thức này tối ưu cho quá trình phát triển (Development) khi lập trình viên cần chỉnh sửa code liên tục và debug bằng IntelliJ IDEA hoặc VS Code.

### Bước 1: Khởi động riêng các cơ sở dữ liệu và lưu trữ
Chạy file docker compose thu nhỏ dành cho hạ tầng:
```bash
docker compose -f docker-compose.db.yml up -d
```
Lệnh này sẽ khởi chạy:
- MySQL (`localhost:3306`)
- MongoDB (`localhost:27017`)
- Redis (`localhost:6379`)
- MinIO (`localhost:9000` / Console `localhost:9001`)

### Bước 2: Khởi động Backend (Bắt buộc theo thứ tự phụ thuộc)
Mở các terminal riêng biệt tại thư mục gốc và chạy theo thứ tự:

1. **Service Registry (Eureka)**:
   ```bash
   mvn spring-boot:run -f backend/pom.xml -pl service-registry
   ```
   *(Chờ khoảng 10 giây để Eureka khởi động hoàn tất tại port 8761)*.

2. **API Gateway**:
   ```bash
   mvn spring-boot:run -f backend/pom.xml -pl api-gateway
   ```

3. **Identity Service**:
   ```bash
   mvn spring-boot:run -f backend/pom.xml -pl identity-service
   ```

4. **Course Service**:
   ```bash
   mvn spring-boot:run -f backend/pom.xml -pl course-service
   ```

5. **Assignment Service**:
   ```bash
   mvn spring-boot:run -f backend/pom.xml -pl assignment-service
   ```

6. **Blog Service**:
   ```bash
   mvn spring-boot:run -f backend/pom.xml -pl blog-service
   ```

7. **Chatbot Service**:
   ```bash
   mvn spring-boot:run -f backend/pom.xml -pl chatbot-service
   ```

### Bước 3: Khởi động Giao diện Frontend
Mở một terminal mới:
```bash
cd frontend
npm install
npm run dev
```
Trình duyệt sẽ tự động mở ứng dụng tại: `http://localhost:5173`.

---

## 5. Danh Mục Các Địa Chỉ Mạng (URLs) Khi Hệ Thống Vận Hành

| Thành phần | Địa chỉ truy cập | Ghi chú tài khoản |
|---|---|---|
| **Frontend Web App** | [http://localhost:5173](http://localhost:5173) | Đăng ký tài khoản mới |
| **API Gateway** | [http://localhost:8080](http://localhost:8080) | Cổng gọi API chung |
| **Eureka Server Dashboard** | [http://localhost:8761](http://localhost:8761) | Xem danh sách service online |
| **MinIO Web Console** | [http://localhost:9001](http://localhost:9001) | `minioadmin` / `minioadmin123` |
| **Tài khoản Quản trị viên** | Trên Web Frontend | `admin` / `admin` |

### Các URL Kiểm Tra Trạng Thái Sức Khỏe (Health Checks):
- Gateway: `http://localhost:8080/actuator/health/liveness`
- Identity: `http://localhost:8081/identity/actuator/health`
- Course: `http://localhost:8082/course/actuator/health/liveness`
- Assignment: `http://localhost:8083/assignment/actuator/health/liveness`
- Blog: `http://localhost:8084/blog/actuator/health/liveness`
- Chatbot: `http://localhost:8085/chatbot/actuator/health/liveness`

---

## 6. Xử Lý Các Sự Cố Phổ Biến (Troubleshooting)

### 6.1 Xung đột hoặc bị chiếm dụng Cổng (Port Conflict)
Khi khởi động, nếu gặp lỗi `Port already in use` (ví dụ port 8080, 5173, 3306):

#### Trên Windows PowerShell:
1. Xem tiến trình nào đang chiếm cổng (ví dụ cổng 8080):
   ```powershell
   Get-NetTCPConnection -State Listen | Where-Object { $_.LocalPort -eq 8080 } | Select-Object LocalAddress,LocalPort,OwningProcess
   ```
2. Xem tên ứng dụng đang chạy PID đó:
   ```powershell
   Get-Process -Id <PID>
   ```
3. Dừng tiến trình bị treo:
   ```powershell
   Stop-Process -Id <PID> -Force
   ```

#### Trên Linux / macOS:
```bash
# Tìm PID:
lsof -i :8080
# Tắt tiến trình:
kill -9 <PID>
```

### 6.2 Lỗi Gateway không định tuyến được tới Service (`503 Service Unavailable`)
- **Nguyên nhân**: Service chưa đăng ký kịp thời lên Eureka hoặc tên service trong `application.yaml` không khớp với `spring.application.name`.
- **Cách khắc phục**:
  1. Truy cập `http://localhost:8761` kiểm tra xem tên service có xuất hiện trong bảng "Instances currently registered with Eureka" hay chưa.
  2. Đảm bảo Eureka Server được khởi động trước tất cả các service khác tối thiểu 10 giây.

### 6.3 Lỗi kết nối Cơ sở dữ liệu MySQL (`Access denied` hoặc `Database not found`)
- Chuỗi kết nối JDBC trong `.env` đã có tham số `createDatabaseIfNotExist=true`. Nếu MySQL báo lỗi quyền, hãy đảm bảo username/password là `root` và `mysqladmin123`.
