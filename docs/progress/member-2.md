# Nháº­t KÃ½ Tiáº¿n Äá»™ Thá»±c Hiá»‡n â€” ThÃ nh ViÃªn 2 (NVCuong3112)

- **NgÆ°á»i thá»±c hiá»‡n**: ThÃ nh viÃªn 2 (`NVCuong3112`)
- **NhÃ¡nh lÃ m viá»‡c**: `feature/member-2`
- **PhÆ°Æ¡ng phÃ¡p**: TDD (Test-Driven Development)
- **Má»¥c tiÃªu**: Äá»£t 1 â€” Khung Microservices & Háº¡ Táº§ng

---

## Tiáº¿n Äá»™ Chi Tiáº¿t Theo Phase

### Phase 1: Parent POM & Háº¡ Táº§ng CÆ¡ Sá»Ÿ Dá»¯ Liá»‡u

- [x] **Task 1.1: Táº¡o Maven Parent POM Monorepo**
  - **File táº¡o**: `backend/pom.xml`
  - **Cáº¥u hÃ¬nh**: Java 21, Spring Boot `3.3.5`, Spring Cloud `2023.0.4`, MapStruct `1.5.5.Final`, JJWT `0.12.5`, Lombok `1.18.30`.
  - **Káº¿t quáº£ thá»±c nghiá»‡m Spring Cloud 2023.0.4**:
    - Cháº¡y lá»‡nh `mvn validate -f backend/pom.xml` thÃ nh cÃ´ng 100% (`BUILD SUCCESS`).
    - ToÃ n bá»™ dependencies BOM cá»§a Spring Cloud `2023.0.4` (gateway, circuitbreaker, eureka, config, etc.) táº£i vá» trÆ¡n tru tá»« Maven Central, khÃ´ng phÃ¡t sinh báº¥t ká»³ xung Ä‘á»™t hay cáº£nh bÃ¡o khÃ´ng tÆ°Æ¡ng thÃ­ch vá»›i Spring Boot `3.3.5`.
    - Tráº¡ng thÃ¡i phiÃªn báº£n: `2023.0.4` hoáº¡t Ä‘á»™ng á»•n Ä‘á»‹nh, sáºµn sÃ ng cho cÃ¡c module con.
  - **Commit dá»± kiáº¿n**: `feat(infra): thiáº¿t láº­p Maven parent pom vÃ  cáº¥u hÃ¬nh dependency management`

- [x] **Task 1.2: Thiáº¿t láº­p Docker Compose CSDL & Script Khá»Ÿi Táº¡o**
  - **File táº¡o**: `docker-compose.db.yml`, `docker/mysql/init/01-init-databases.sql`
  - **Cáº¥u hÃ¬nh**:
    - MySQL 8.4: Port 3306, tá»± Ä‘á»™ng cháº¡y script táº¡o 4 database (`identity_lms`, `course_lms`, `assignment_lms`, `blog_lms`).
    - MongoDB 7.0: Port 27017, user `mongoadmin`.
    - Redis 7.2: Port 6379, requirepass `redisadmin123`.
    - MinIO S3: Port 9000 (API) vÃ  9001 (Console).
  - **Commit dá»± kiáº¿n**: `build(docker): thÃªm docker-compose.db cho MySQL MongoDB Redis vÃ  MinIO`

### Phase 2: Service Registry â€” Netflix Eureka Server (Port 8761)

- [x] **Task 2.1 & 2.2: TDD Eureka Server & Endpoint Verification**
  - **PhÆ°Æ¡ng phÃ¡p**: TDD Red -> Green.
  - **File táº¡o**:
    - `backend/service-registry/pom.xml`
    - `backend/service-registry/src/test/resources/application-test.yaml`
    - `backend/service-registry/src/test/java/com/lms/serviceregistry/ServiceRegistryApplicationTests.java`
    - `backend/service-registry/src/main/resources/application.yaml`
    - `backend/service-registry/src/main/java/com/lms/serviceregistry/ServiceRegistryApplication.java`
  - **Káº¿t quáº£ TDD**:
    - **Red**: Khi chÆ°a cÃ³ class `ServiceRegistryApplication`, kiá»ƒm thá»­ bÃ¡o lá»—i `Unable to find a @SpringBootConfiguration` -> XÃ¡c nháº­n Red chuáº©n xÃ¡c.
    - **Green**: Triá»ƒn khai `ServiceRegistryApplication` (`@EnableEurekaServer`) vÃ  `application.yaml` -> ToÃ n bá»™ **2/2 test cases PASS 100%**:
      1. `contextLoads()`: PASS
      2. `eurekaAppsEndpointReturns200()`: PASS (`GET /eureka/apps` tráº£ vá» HTTP 200 OK)
  - **Commit dá»± kiáº¿n**: `feat(registry): cáº¥u hÃ¬nh Netflix Eureka Server, test khá»Ÿi Ä‘á»™ng vÃ  endpoint /eureka/apps`

### Phase 3: API Gateway â€” Spring Cloud Gateway (Port 8080)

- [x] **Task 3.1 & 3.2: TDD Dynamic Routing & AuthFilter (15 Test Cases)**
  - **PhÆ°Æ¡ng phÃ¡p**: TDD Red 1 (Compilation Error) -> Stubs -> Red 2 (Assertion Failures) -> Green.
  - **File táº¡o**:
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
  - **Káº¿t quáº£ TDD**:
    - **Red 1**: Kiá»ƒm thá»­ ban Ä‘áº§u lá»—i biÃªn dá»‹ch do thiáº¿u class `JwtUtil`, `AuthFilter`, `ErrorCode`.
    - **Red 2**: Sau khi táº¡o class stubs rá»—ng, kiá»ƒm thá»­ tháº¥t báº¡i chÃ­nh xÃ¡c táº¡i cÃ¡c behavioral assertions (chÆ°a cÃ³ logic verify token vÃ  whitelist).
    - **Green**: Triá»ƒn khai Ä‘áº§y Ä‘á»§ logic xÃ¡c thá»±c HMAC SHA, bÃ³c tÃ¡ch claims, token offloading (inject 4 headers `X-*`, strip `Authorization`), whitelist phÃ¢n quyá»n HTTP Method -> ToÃ n bá»™ **15/15 test cases PASS 100%**:
      1. `ApiGatewayApplicationTests`: 1 test case PASS (`contextLoads`).
      2. `JwtUtilTest`: 3 test cases PASS (`extractToken`, `verify` há»£p lá»‡, `verify` tá»« chá»‘i refresh/malformed/expired/tampered).
      3. `AuthFilterTest`: 6 test cases PASS (Header Injection, 401 khi thiáº¿u token, 401 token háº¿t háº¡n, 401 sai chá»¯ kÃ½, Header Stripping 5 headers giáº£ máº¡o, Whitelist HTTP Method).
      4. `GatewayRouteConfigurationTest`: 5 test cases PASS (Äá»‹nh tuyáº¿n Identity, Course, Blog, Shortcuts, Sensitive Route Assertions).
  - **Commit dá»± kiáº¿n**: `feat(gateway): triá»ƒn khai Spring Cloud Gateway dynamic routes vÃ  AuthFilter`

---

### Phase 4: Thiáº¿t Láº­p Khung 3 Microservices Cá»§a ThÃ nh ViÃªn 2

- [x] **Task 4.1: Khung identity-service (Port 8081)**
  - **PhÆ°Æ¡ng phÃ¡p**: TDD Red -> Green.
  - **File táº¡o**:
    - `backend/identity-service/pom.xml`
    - `backend/identity-service/src/test/resources/application-test.yaml`
    - `backend/identity-service/src/test/java/com/lms/identityservice/IdentityServiceApplicationTests.java`
    - `backend/identity-service/src/main/resources/application.yaml`
    - `backend/identity-service/src/main/java/com/lms/identityservice/IdentityServiceApplication.java`
  - **Káº¿t quáº£ TDD**:
    - **Red**: Cháº¡y `mvn test -pl identity-service` khi chÆ°a cÃ³ class cáº¥u hÃ¬nh chÃ­nh -> `IllegalStateException: Unable to find a @SpringBootConfiguration` -> XÃ¡c nháº­n Red chuáº©n xÃ¡c.
    - **Green**: Triá»ƒn khai `IdentityServiceApplication` (`@SpringBootApplication`, `@EnableDiscoveryClient`), `application.yaml` (MySQL `identity_lms`, context-path `/identity`, port `8081`), `application-test.yaml` (H2 in-memory `identity_test`, Eureka disabled) -> **1/1 test case PASS 100%**:
      1. `contextLoads()`: PASS (Khá»Ÿi táº¡o EntityManagerFactory vÃ  Spring Context trÃªn H2).
  - **Commit dá»± kiáº¿n**: `feat(identity): thiáº¿t láº­p khung dá»± Ã¡n identity-service vÃ  cáº¥u hÃ¬nh database`

- [x] **Task 4.2: Khung course-service (Port 8082)**
  - **PhÆ°Æ¡ng phÃ¡p**: TDD Red -> Green.
  - **File táº¡o**:
    - `backend/course-service/pom.xml`
    - `backend/course-service/src/test/resources/application-test.yaml`
    - `backend/course-service/src/test/java/com/lms/courseservice/CourseServiceApplicationTests.java`
    - `backend/course-service/src/main/resources/application.yaml`
    - `backend/course-service/src/main/java/com/lms/courseservice/CourseServiceApplication.java`
  - **Káº¿t quáº£ TDD**:
    - **Red**: Cháº¡y `mvn test -pl course-service` khi chÆ°a cÃ³ class cáº¥u hÃ¬nh chÃ­nh -> `IllegalStateException: Unable to find a @SpringBootConfiguration` -> XÃ¡c nháº­n Red chuáº©n xÃ¡c.
    - **Green**: Triá»ƒn khai `CourseServiceApplication` (`@SpringBootApplication`, `@EnableDiscoveryClient`), `application.yaml` (MySQL `course_lms`, MongoDB `course_lms_mongo`, context-path `/course`, port `8082`), `application-test.yaml` (H2 in-memory `course_test`, exclude Mongo auto-config, Eureka disabled) -> **1/1 test case PASS 100%**:
      1. `contextLoads()`: PASS (Khá»Ÿi táº¡o EntityManagerFactory vÃ  Spring Data JPA/MongoDB strict mode trÃªn H2).
  - **Commit dá»± kiáº¿n**: `feat(course): thiáº¿t láº­p khung dá»± Ã¡n course-service káº¿t ná»‘i MySQL vÃ  MongoDB`

- [x] **Task 4.3: Khung blog-service (Port 8084)**
  - **PhÆ°Æ¡ng phÃ¡p**: TDD Red -> Green.
  - **File táº¡o**:
    - `backend/blog-service/pom.xml`
    - `backend/blog-service/src/test/resources/application-test.yaml`
    - `backend/blog-service/src/test/java/com/lms/blogservice/BlogServiceApplicationTests.java`
    - `backend/blog-service/src/main/resources/application.yaml`
    - `backend/blog-service/src/main/java/com/lms/blogservice/BlogServiceApplication.java`
  - **Káº¿t quáº£ TDD**:
    - **Red**: Cháº¡y `mvn test -pl blog-service` khi chÆ°a cÃ³ class cáº¥u hÃ¬nh chÃ­nh -> `IllegalStateException: Unable to find a @SpringBootConfiguration` -> XÃ¡c nháº­n Red chuáº©n xÃ¡c.
    - **Green**: Triá»ƒn khai `BlogServiceApplication` (`@SpringBootApplication`, `@EnableDiscoveryClient`), `application.yaml` (MySQL `blog_lms`, context-path `/blog`, port `8084`), `application-test.yaml` (H2 in-memory `blog_test`, Eureka disabled) -> **1/1 test case PASS 100%**:
      1. `contextLoads()`: PASS (Khá»Ÿi táº¡o EntityManagerFactory vÃ  Spring Data JPA trÃªn H2).
  - **Commit dá»± kiáº¿n**: `feat(blog): thiáº¿t láº­p khung dá»± Ã¡n blog-service vÃ  cáº¥u hÃ¬nh MySQL`

---

### Phase 5: Tổng Kiểm Tra Monorepo & Nghiệm Thu Đợt 1

- [x] **Task 5.1: Tổng kiểm thử toàn bộ Backend Monorepo**
  - **Lệnh thực thi**: `mvn clean test -f backend/pom.xml`
  - **Kết quả**: **BUILD SUCCESS 100%** trên toàn bộ 6/6 module:
    1. `lms-backend` (Parent POM): SUCCESS [0.165 s]
    2. `service-registry` (Eureka Server): SUCCESS [14.116 s]
    3. `api-gateway` (Spring Cloud Gateway & AuthFilter): SUCCESS [8.472 s]
    4. `identity-service` (Identity Service): SUCCESS [8.163 s]
    5. `course-service` (Course Service): SUCCESS [7.478 s]
    6. `blog-service` (Blog Service): SUCCESS [7.518 s]
    - Tổng thời gian: 46.4s, 0 Failures, 0 Errors.
  - **Tối ưu hóa**: Thêm cấu hình `maven-compiler-plugin` override (`combine.self="override"`) cho `service-registry/pom.xml` đồng bộ với các module khác để tương thích hoàn toàn trên JDK 21 - 24.

- [x] **Task 5.2: Nghiệm thu toàn bộ Đợt 1**
  - Hoàn tất khung hạ tầng CSDL Docker Compose (MySQL, MongoDB, Redis, MinIO).
  - Hoàn tất Eureka Service Registry, Spring Cloud Gateway (JWT AuthFilter + Dynamic Routes).
  - Hoàn tất khung 3 Microservices chính (Identity, Course, Blog) với cấu hình port, database và H2 test profile độc lập.
  - 8 commit chuẩn Conventional Commits trên nhánh `feature/member-2`.