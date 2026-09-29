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

### Phase 3: API Gateway — Spring Cloud Gateway (Port 8080)

- [x] **Task 3.1 & 3.2: TDD Dynamic Routing & AuthFilter (15 Test Cases)**
  - **Phương pháp**: TDD Red 1 (Compilation Error) -> Stubs -> Red 2 (Assertion Failures) -> Green.
  - **File tạo**:
    - `backend/api-gateway/pom.xml`
    - `backend/api-gateway/src/test/resources/application-test.yaml`
    - `backend/api-gateway/src/test/java/com/lms/apigateway/ApiGatewayApplicationTests.java` (1 test case)
    - `backend/api-gateway/src/test/java/com/lms/apigateway/configuration/JwtUtilTest.java` (3 test cases)
    - `backend/api-gateway/src/test/java/com/lms/apigateway/configuration/AuthFilterTest.java` (6 test cases)
    - `backend/api-gateway/src/test/java/com/lms/apigateway/configuration/GatewayRouteConfigurationTest.java` (5 test cases)
    - `backend/api-gateway/src/main/resources/application.yaml`
    - `backend/api-gateway/src/main/java/com/lms/apigateway/ApiGatewayApplication.java`
    - `backend/api-gateway/src/main/java/com/lms/apigateway/exception/ErrorCode.java`
    - `backend/api-gateway/src/main/java/com/lms/apigateway/configuration/JwtUtil.java`
    - `backend/api-gateway/src/main/java/com/lms/apigateway/configuration/AuthFilter.java`
  - **Kết quả TDD**:
    - **Red 1**: Kiểm thử ban đầu lỗi biên dịch do thiếu class `JwtUtil`, `AuthFilter`, `ErrorCode`.
    - **Red 2**: Sau khi tạo class stubs rỗng, kiểm thử thất bại chính xác tại các behavioral assertions (chưa có logic verify token và whitelist).
    - **Green**: Triển khai đầy đủ logic xác thực HMAC SHA, bóc tách claims, token offloading (inject 4 headers `X-*`, strip `Authorization`), whitelist phân quyền HTTP Method -> Toàn bộ **15/15 test cases PASS 100%**:
      1. `ApiGatewayApplicationTests`: 1 test case PASS (`contextLoads`).
      2. `JwtUtilTest`: 3 test cases PASS (`extractToken`, `verify` hợp lệ, `verify` từ chối refresh/malformed/expired/tampered).
      3. `AuthFilterTest`: 6 test cases PASS (Header Injection, 401 khi thiếu token, 401 token hết hạn, 401 sai chữ ký, Header Stripping 5 headers giả mạo, Whitelist HTTP Method).
      4. `GatewayRouteConfigurationTest`: 5 test cases PASS (Định tuyến Identity, Course, Blog, Shortcuts, Sensitive Route Assertions).
  - **Commit dự kiến**: `feat(gateway): triển khai Spring Cloud Gateway dynamic routes và AuthFilter`

---

### Phase 4: Thiết Lập Khung 3 Microservices Của Thành Viên 2

- [x] **Task 4.1: Khung identity-service (Port 8081)**
  - **Phương pháp**: TDD Red -> Green.
  - **File tạo**:
    - `backend/identity-service/pom.xml`
    - `backend/identity-service/src/test/resources/application-test.yaml`
    - `backend/identity-service/src/test/java/com/lms/identityservice/IdentityServiceApplicationTests.java`
    - `backend/identity-service/src/main/resources/application.yaml`
    - `backend/identity-service/src/main/java/com/lms/identityservice/IdentityServiceApplication.java`
  - **Kết quả TDD**:
    - **Red**: Chạy `mvn test -pl identity-service` khi chưa có class cấu hình chính -> `IllegalStateException: Unable to find a @SpringBootConfiguration` -> Xác nhận Red chuẩn xác.
    - **Green**: Triển khai `IdentityServiceApplication` (`@SpringBootApplication`, `@EnableDiscoveryClient`), `application.yaml` (MySQL `identity_lms`, context-path `/identity`, port `8081`), `application-test.yaml` (H2 in-memory `identity_test`, Eureka disabled) -> **1/1 test case PASS 100%**:
      1. `contextLoads()`: PASS (Khởi tạo EntityManagerFactory và Spring Context trên H2).
  - **Commit dự kiến**: `feat(identity): thiết lập khung dự án identity-service và cấu hình database`

- [x] **Task 4.2: Khung course-service (Port 8082)**
  - **Phương pháp**: TDD Red -> Green.
  - **File tạo**:
    - `backend/course-service/pom.xml`
    - `backend/course-service/src/test/resources/application-test.yaml`
    - `backend/course-service/src/test/java/com/lms/courseservice/CourseServiceApplicationTests.java`
    - `backend/course-service/src/main/resources/application.yaml`
    - `backend/course-service/src/main/java/com/lms/courseservice/CourseServiceApplication.java`
  - **Kết quả TDD**:
    - **Red**: Chạy `mvn test -pl course-service` khi chưa có class cấu hình chính -> `IllegalStateException: Unable to find a @SpringBootConfiguration` -> Xác nhận Red chuẩn xác.
    - **Green**: Triển khai `CourseServiceApplication` (`@SpringBootApplication`, `@EnableDiscoveryClient`), `application.yaml` (MySQL `course_lms`, MongoDB `course_lms_mongo`, context-path `/course`, port `8082`), `application-test.yaml` (H2 in-memory `course_test`, exclude Mongo auto-config, Eureka disabled) -> **1/1 test case PASS 100%**:
      1. `contextLoads()`: PASS (Khởi tạo EntityManagerFactory và Spring Data JPA/MongoDB strict mode trên H2).
  - **Commit dự kiến**: `feat(course): thiết lập khung dự án course-service kết nối MySQL và MongoDB`

- [x] **Task 4.3: Khung blog-service (Port 8084)**
  - **Phương pháp**: TDD Red -> Green.
  - **File tạo**:
    - `backend/blog-service/pom.xml`
    - `backend/blog-service/src/test/resources/application-test.yaml`
    - `backend/blog-service/src/test/java/com/lms/blogservice/BlogServiceApplicationTests.java`
    - `backend/blog-service/src/main/resources/application.yaml`
    - `backend/blog-service/src/main/java/com/lms/blogservice/BlogServiceApplication.java`
  - **Kết quả TDD**:
    - **Red**: Chạy `mvn test -pl blog-service` khi chưa có class cấu hình chính -> `IllegalStateException: Unable to find a @SpringBootConfiguration` -> Xác nhận Red chuẩn xác.
    - **Green**: Triển khai `BlogServiceApplication` (`@SpringBootApplication`, `@EnableDiscoveryClient`), `application.yaml` (MySQL `blog_lms`, context-path `/blog`, port `8084`), `application-test.yaml` (H2 in-memory `blog_test`, Eureka disabled) -> **1/1 test case PASS 100%**:
      1. `contextLoads()`: PASS (Khởi tạo EntityManagerFactory và Spring Data JPA trên H2).
  - **Commit dự kiến**: `feat(blog): thiết lập khung dự án blog-service và cấu hình MySQL`

---

### Kế Hoạch Tiếp Theo (Phase 5)
- Phase 5: Tổng kiểm tra monorepo và nghiệm thu toàn bộ Đợt 1.




