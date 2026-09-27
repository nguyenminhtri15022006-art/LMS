# Quy Chuẩn Lập Trình — EduLearn LMS (CODING_RULES.md)

Tài liệu này xác định các quy tắc đặt tên, tổ chức mã nguồn, kiến trúc phân tầng, quản lý lỗi và tiêu chuẩn thẩm mỹ code dành cho Java (Spring Boot) và JavaScript (React 18).

---

## 1. Chuẩn Mực Mã Nguồn Backend (Java 21 & Spring Boot)

### 1.1 Quy Chuẩn Đặt Tên (Naming Conventions)
- **Tên Package**: Toàn bộ chữ thường, không dấu gạch dưới hay ký tự đặc biệt:
  `com.lms.identityservice.controller`, `com.lms.courseservice.repository`
- **Tên Class / Interface**: Tuân thủ chuẩn `PascalCase`:
  `UserController`, `CourseServiceImpl`, `QuizRepository`
- **Tên Method & Biến**: Tuân thủ chuẩn `camelCase`:
  `getUserById()`, `calculatePassingGrade()`, `totalSubmittedCount`
- **Hằng số (Constants / Enums)**: Toàn bộ chữ hoa phân cách bằng dấu gạch dưới (`UPPER_SNAKE_CASE`):
  `MAX_ATTEMPTS`, `ROLE_STUDENT`, `IN_PROGRESS`

### 1.2 Kiến Trúc Phân Tầng & Phân Định Trách Nhiệm (Layered Architecture)

#### 1. Tầng Thực Thể (Entity Layer)
- Tên class đại diện trực tiếp cho bảng MySQL hoặc MongoDB collection:
  `User`, `Course`, `Problem`, `Submission`.
- Sử dụng Lombok để giảm mã nguồn lặp lại:
  `@Getter`, `@Setter`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@FieldDefaults(level = AccessLevel.PRIVATE)`.
- Không sử dụng Lombok `@Data` trên JPA Entity để tránh các vấn đề tiềm ẩn liên quan đến `equals()`, `hashCode()` và vòng lặp vô tận trong quan hệ hai chiều.

#### 2. Tầng Truy Xuất Dữ Liệu (Repository Layer)
- Tên interface kết thúc bằng `Repository`: `UserRepository`, `QuizRepository`.
- Kế thừa `JpaRepository<Entity, ID>` cho MySQL hoặc `MongoRepository<Document, String>` cho MongoDB.
- Tận dụng quy ước đặt tên method truy vấn (Derived Query Methods):
  `findByUserIdAndCourseId(String userId, String courseId)`.

#### 3. Tầng Nghiệp Vụ (Service Layer)
- Phải tách biệt rõ ràng giữa interface và class cài đặt:
  - Interface: `UserService`, `QuizService`.
  - Class cài đặt: `UserServiceImpl`, `QuizServiceImpl`.
- Đánh dấu `@Service` trên class cài đặt; sử dụng `@Transactional` cho các nghiệp vụ cập nhật nhiều bảng/document.
- Sử dụng Constructor Injection (khuyến nghị dùng `@RequiredArgsConstructor` của Lombok) thay cho `@Autowired` trên field.

#### 4. Tầng DTO & Ánh Xạ Dữ Liệu (DTO & Mapper)
- Phân chia DTO rõ ràng thành Request và Response:
  - `UserCreationRequest`, `CourseUpdateRequest`.
  - `UserResponse`, `QuizDetailResponse`.
- Chuyển đổi dữ liệu giữa Entity và DTO bằng **MapStruct**:
  `UserMapper.java` với `@Mapper(componentModel = "spring")`.

#### 5. Tầng Điều Khiển (Controller Layer)
- Tên class kết thúc bằng `Controller`: `AuthenticationController`, `CourseController`.
- Áp dụng `@RestController` và định nghĩa prefix chuẩn: `@RequestMapping("/courses")`.
- Tất cả phương thức public phải trả về đối tượng bọc chuẩn `ApiResponse<T>`.
- Tuyệt đối không viết logic nghiệp vụ phức tạp trực tiếp trong Controller; chỉ làm nhiệm vụ tiếp nhận request, validate và ủy quyền cho Service.

### 1.3 Validation & Xử Lý Ngoại Lệ (Validation & Exceptions)
- Kiểm tra hợp lệ đầu vào bằng Jakarta Validation:
  `@NotNull`, `@NotBlank`, `@Size(min = 5)`, `@Email`, `@Min`, `@Max`.
- Bắt buộc đặt `@Valid` trước các tham số `@RequestBody`.
- Định nghĩa enum lỗi tập trung `ErrorCode`:
  ```java
  public enum ErrorCode {
      UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
      USER_EXISTED(1005, "User already existed", HttpStatus.BAD_REQUEST),
      UNAUTHENTICATED(1001, "Unauthenticated", HttpStatus.UNAUTHORIZED),
      UNAUTHORIZED(1002, "You do not have permission", HttpStatus.FORBIDDEN);
      ...
  }
  ```
- Ném ngoại lệ nghiệp vụ thống nhất: `throw new AppException(ErrorCode.COURSE_NOT_FOUND);`.
- Xử lý toàn cục bằng `@ControllerAdvice` / `@RestControllerAdvice` (`GlobalExceptionHandler`).

### 1.4 Ghi Log (Logging Rules)
- Sử dụng `@Slf4j` từ Lombok.
- Không sử dụng `System.out.println()` trong mã nguồn sản phẩm.
- Chọn cấp độ log chuẩn mực:
  - `log.debug(...)`: Dùng khi debug luồng nội bộ (payload, vòng lặp).
  - `log.info(...)`: Ghi nhận sự kiện quan trọng (User đăng ký thành công, Bắt đầu chấm code, Khởi động service).
  - `log.warn(...)`: Cảnh báo không làm sập ứng dụng (Rate limit vượt ngưỡng, Token gần hết hạn).
  - `log.error(...)`: Ngoại lệ hệ thống nghiêm trọng, ghi kèm exception stacktrace.

---

## 2. Chuẩn Mực Mã Nguồn Frontend (React 18 & JavaScript)

### 2.1 Quy Chuẩn Đặt Tên
- **Tên Component & File JSX**: Tuân thủ `PascalCase`:
  `Header.jsx`, `DetailCoursePage.jsx`, `ChatBot.jsx`.
- **Tên Hooks**: Bắt đầu bằng tiền tố `use`:
  `useAuth.js`, `useFileUpload.js`, `useJudge0.js`.
- **Tên Utils & Services API**: Tuân thủ `camelCase`:
  `dateTime.js`, `course.api.js`, `api.client.js`.
- **Tên Hằng Số (Constants)**: `UPPER_SNAKE_CASE`:
  `ROLES`, `ENDPOINTS`, `API_TIMEOUT_MS`.

### 2.2 Kiến Trúc Component & State Management
- Ưu tiên sử dụng **Functional Components** kết hợp React Hooks chuẩn (`useState`, `useEffect`, `useCallback`, `useMemo`, `useRef`).
- Tách biệt logic và giao diện: Các thao tác gọi API phức tạp hoặc quản lý state dài nên được bọc trong Custom Hook riêng biệt.
- Sử dụng React Context (`AuthContext`) cho trạng thái dùng chung toàn ứng dụng (thông tin người dùng hiện tại, trạng thái đăng nhập, theme).
- Hạn chế re-render thừa thãi bằng việc chia nhỏ component UI.

### 2.3 Quản Lý Gọi API & Axios Client
- Toàn bộ kết nối API phải đi qua Axios instance duy nhất tại `src/services/api.client.js`.
- Cấu hình Interceptors:
  - **Request Interceptor**: Tự động đính kèm `Authorization: Bearer <accessToken>` từ AuthContext hoặc LocalStorage nếu có.
  - **Response Interceptor**: Bắt mã lỗi 401 để tự động gọi endpoint `/auth/refresh` lấy token mới; nếu thất bại thì điều hướng về `/login`.
- Không gọi trực tiếp `axios.get()` lẻ loi trong component mà phải thông qua các hàm dịch vụ trong thư mục `src/services/*.api.js`.

### 2.4 Tiêu Chuẩn Giao Diện & CSS
- Tổ chức CSS theo cấu trúc module hoặc CSS đi kèm component trong thư mục `styles/`.
- Định nghĩa bảng màu (Color Palette), Typography và khoảng cách đồng bộ thông qua CSS Variables trong `index.css`:
  `--primary-color`, `--secondary-color`, `--bg-dark`, `--text-main`.
- Đảm bảo tính tương thích giao diện (Responsive Design) trên các kích thước màn hình máy tính bảng và desktop.
- Mọi thao tác người dùng có độ trễ mạng đều phải có hiển thị chỉ báo tải (`LoadingSpinner`, `Skeleton`) và phản hồi kết quả bằng `Toast` thông báo.
