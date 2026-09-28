# Nhật Ký Tiến Độ Thực Hiện — Thành Viên 2 (NVCuong3112)

- **Người thực hiện**: Thành viên 2 (`NVCuong3112`)
- **Nhánh làm việc**: `feature/member-2`
- **Phương pháp**: TDD (Test-Driven Development)
- **Mục tiêu**: Đợt 1 — Khung Microservices & Hạ Tầng

---

## Tiến Độ Chi Tiết Theo Phase

### Phase 1: Parent POM & Hạ Tầng Cơ Sở Dữ Liệu

- [x] **Task 1.1: Tạo Maven Parent POM Monorepo**
  - **File tạo**: `backend/pom.xml`
  - **Cấu hình**: Java 21, Spring Boot `3.3.5`, Spring Cloud `2023.0.4`, MapStruct `1.5.5.Final`, JJWT `0.12.5`, Lombok `1.18.30`.
  - **Kết quả thực nghiệm Spring Cloud 2023.0.4**:
    - Chạy lệnh `mvn validate -f backend/pom.xml` thành công 100% (`BUILD SUCCESS`).
    - Toàn bộ dependencies BOM của Spring Cloud `2023.0.4` (gateway, circuitbreaker, eureka, config, etc.) tải về trơn tru từ Maven Central, không phát sinh bất kỳ xung đột hay cảnh báo không tương thích với Spring Boot `3.3.5`.
    - Trạng thái phiên bản: `2023.0.4` hoạt động ổn định, sẵn sàng cho các module con.
  - **Commit dự kiến**: `feat(infra): thiết lập Maven parent pom và cấu hình dependency management`

- [x] **Task 1.2: Thiết lập Docker Compose CSDL & Script Khởi Tạo**
  - **File tạo**: `docker-compose.db.yml`, `docker/mysql/init/01-init-databases.sql`
  - **Cấu hình**:
    - MySQL 8.4: Port 3306, tự động chạy script tạo 4 database (`identity_lms`, `course_lms`, `assignment_lms`, `blog_lms`).
    - MongoDB 7.0: Port 27017, user `mongoadmin`.
    - Redis 7.2: Port 6379, requirepass `redisadmin123`.
    - MinIO S3: Port 9000 (API) và 9001 (Console).
  - **Commit dự kiến**: `build(docker): thêm docker-compose.db cho MySQL MongoDB Redis và MinIO`

### Phase 2: Service Registry — Netflix Eureka Server (Port 8761)

- [x] **Task 2.1 & 2.2: TDD Eureka Server & Endpoint Verification**
  - **Phương pháp**: TDD Red -> Green.
  - **File tạo**:
    - `backend/service-registry/pom.xml`
    - `backend/service-registry/src/test/resources/application-test.yaml`
    - `backend/service-registry/src/test/java/com/lms/serviceregistry/ServiceRegistryApplicationTests.java`
    - `backend/service-registry/src/main/resources/application.yaml`
    - `backend/service-registry/src/main/java/com/lms/serviceregistry/ServiceRegistryApplication.java`
  - **Kết quả TDD**:
    - **Red**: Khi chưa có class `ServiceRegistryApplication`, kiểm thử báo lỗi `Unable to find a @SpringBootConfiguration` -> Xác nhận Red chuẩn xác.
    - **Green**: Triển khai `ServiceRegistryApplication` (`@EnableEurekaServer`) và `application.yaml` -> Toàn bộ **2/2 test cases PASS 100%**:
      1. `contextLoads()`: PASS
      2. `eurekaAppsEndpointReturns200()`: PASS (`GET /eureka/apps` trả về HTTP 200 OK)
  - **Commit dự kiến**: `feat(registry): cấu hình Netflix Eureka Server, test khởi động và endpoint /eureka/apps`

---

### Kế Hoạch Tiếp Theo (Phase 3)
- Phase 3: Spring Cloud Gateway (`api-gateway`) — Viết test trước (TDD Red: 15 test cases) cho `JwtUtil`, `AuthFilter`, `GatewayRouteConfiguration` rồi triển khai mã nguồn (Green).

