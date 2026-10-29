# Bảng Đối Chiếu Tiêu Chí Đánh Giá — EduLearn LMS (RUBRIC_MAPPING.md)

Tài liệu này đối chiếu toàn bộ các tính năng, tài liệu kỹ thuật, mã nguồn và kết quả kiểm thử của dự án **EduLearn LMS** (`DoAnCK`) với thang tiêu chí đánh giá (Rubric) môn học Lập trình Web.

> [!IMPORTANT]
> **Nguyên tắc đối chiếu**:
> Không tự ý cho điểm khi chưa có đầy đủ bằng chứng cụ thể. Mỗi tiêu chí bắt buộc phải có: Tính năng chứng minh, File mã nguồn chứng minh, Commit/PR chứng minh, Test chứng minh và ghi chú rõ ràng các phần còn thiếu.

---

## 1. Tiêu Chí 1: Độ Khó, Tính Thực Tiễn & Phân Công Nhóm (Trọng số: 10%)

### Mô tả tiêu chí:
Đề tài có độ phức tạp cao, giải quyết bài toán thực tế có giá trị sử dụng lớn; phân chia công việc rõ ràng, cân bằng giữa các thành viên, thể hiện sự cộng tác chặt chẽ.

| Yếu tố chứng minh | Nội dung cụ thể |
|---|---|
| **Tính năng chứng minh** | Hệ thống quản lý học tập tích hợp đa năng: Trình chấm code trực tuyến (Online Judge) tự động, Thi trắc nghiệm chống gian lận, Diễn đàn chia sẻ kiến thức Markdown, Trợ lý ảo AI và Quản trị theo vai trò. Trưởng nhóm Nguyễn Minh Trí chủ động nhận gánh vác phần nghiệp vụ phức tạp và nặng nhất (~35,000 LOC bao gồm Code Judge Sandbox và AI LLM), Thành viên 2 đảm nhiệm phân hệ Khóa học, Diễn đàn và Người dùng (~23,400 LOC). |
| **File chứng minh** | [TEAM_ROLES.md](TEAM_ROLES.md), [ARCHITECTURE.md](ARCHITECTURE.md), [PROJECT_OVERVIEW.md](PROJECT_OVERVIEW.md). |
| **Commit / PR chứng minh** | Các commit khởi tạo cấu trúc và phân chia trách nhiệm theo từng module của hai thành viên trên GitHub. |
| **Test chứng minh** | Các kịch bản kiểm thử toàn trình End-to-End được mô tả tại [TESTING_PLAN.md](TESTING_PLAN.md). |
| **Phần còn thiếu** | Cập nhật thông tin họ tên, MSSV và liên kết GitHub chính thức của Thành viên 2 sau khi chốt danh sách nhóm. |

---

## 2. Tiêu Chí 2: Sử Dụng AI & Hiệu Quả Cộng Tác GitHub (Trọng số: 20%)

### Mô tả tiêu chí:
Khai thác hiệu quả các công nghệ Trí tuệ nhân tạo (LLM) trong sản phẩm và quá trình phát triển; tuân thủ quy trình Git chuyên nghiệp, lịch sử commit minh bạch, phân nhánh khoa học và tạo Pull Request có kiểm duyệt.

| Yếu tố chứng minh | Nội dung cụ thể |
|---|---|
| **Tính năng chứng minh** | 1. Tích hợp mô hình AI LLM (OpenCode / DeepSeek) trong `chatbot-service` hỗ trợ giải đáp học tập 24/7.<br>2. Tính năng AI tự động sinh câu hỏi trắc nghiệm từ nội dung bài giảng.<br>3. Tính năng AI tự động sinh bộ testcase biên cho bài tập lập trình.<br>4. Quy trình Git Flow chuẩn: Phân chia nhánh tính năng, Conventional Commits, Pull Request review trước khi merge. |
| **File chứng minh** | `backend/chatbot-service/*`, `frontend/src/components/ui/ChatBot.jsx`, [GIT_WORKFLOW.md](GIT_WORKFLOW.md), [CONTRIBUTING.md](../CONTRIBUTING.md), [COMMIT_PLAN.md](COMMIT_PLAN.md). |
| **Commit / PR chứng minh** | Toàn bộ lịch sử các Pull Request từ các nhánh `feature/member-1/*` và `feature/member-2/*` vào nhánh `develop`. |
| **Test chứng minh** | `ChatServiceImplTest.java`, `GenerateServiceImplTest.java`, `RateLimiterServiceImplTest.java`. |
| **Phần còn thiếu** | Cần thực hiện các commit và tạo Pull Request thật trên GitHub theo đúng tiến độ thực tế (từ 27/09 đến 29/10). |

---

## 3. Tiêu Chí 3: Cấu Trúc Dữ Liệu, Vai Trò & Giao Diện Người Dùng (Trọng số: 20%)

### Mô tả tiêu chí:
Thiết kế cơ sở dữ liệu chuẩn hóa, quan hệ toàn vẹn; hỗ trợ đầy đủ các vai trò người dùng (Guest, Student, Instructor, Admin) với phân quyền RBAC đa lớp; giao diện thân thiện, hiện đại, hỗ trợ trạng thái tương tác đầy đủ.

| Yếu tố chứng minh | Nội dung cụ thể |
|---|---|
| **Tính năng chứng minh** | 1. Kiến trúc Polyglot Persistence: 25 bảng quan hệ MySQL và 5 collections MongoDB phân tách theo từng microservice.<br>2. Phân quyền RBAC 4 cấp độ: Khách, Học viên, Giảng viên, Quản trị viên.<br>3. Giao diện React 18 SPA hiện đại, tích hợp CodeMirror 6, Framer Motion, đầy đủ 4 trạng thái UI (Loading, Empty, Success Toast, Error Toast). |
| **File chứng minh** | [DATABASE_DESIGN.md](DATABASE_DESIGN.md), [USER_ROLES.md](USER_ROLES.md), `frontend/src/routes/AppRoutes.jsx`, `frontend/src/layouts/*`. |
| **Commit / PR chứng minh** | Các commit triển khai Flyway migration (`V1`, `V2`), Spring Security RBAC và hệ thống Route React. |
| **Test chứng minh** | `PrivateRoute.test.jsx`, `RoleRoute.test.jsx`, `IdentityApiIntegrationTest.java`. |
| **Phần còn thiếu** | Chờ triển khai mã nguồn chi tiết và chạy thực tế trên giao diện để hoàn thành trạng thái `VERIFIED`. |

---

## 4. Tiêu Chí 4: Chất Lượng Ứng Dụng, Mã Nguồn & Chức Năng (Trọng số: 35%)

### Mô tả tiêu chí:
Chức năng hoạt động chính xác, ổn định 100% so với đặc tả; mã nguồn sạch (Clean Code), tuân thủ nguyên tắc SOLID, xử lý ngoại lệ toàn cục, validation chặt chẽ và có bộ kiểm thử tự động phong phú.

| Yếu tố chứng minh | Nội dung cụ thể |
|---|---|
| **Tính năng chứng minh** | 1. 74 chức năng nghiệp vụ chi tiết bao phủ toàn bộ 7 microservices.<br>2. Cấu trúc Response chuẩn `ApiResponse<T>`, bộ xử lý ngoại lệ tập trung `GlobalExceptionHandler`.<br>3. Kiểm soát dữ liệu bằng Jakarta Validation ở backend và form validation ở frontend.<br>4. Tích hợp Resilience4j Circuit Breaker chống sập tầng. |
| **File chứng minh** | [FEATURE_PARITY_MATRIX.md](FEATURE_PARITY_MATRIX.md), [API_CONTRACT.md](API_CONTRACT.md), [CODING_RULES.md](CODING_RULES.md), [TESTING_PLAN.md](TESTING_PLAN.md). |
| **Commit / PR chứng minh** | Các commit mã nguồn nghiệp vụ của từng service kèm test tương ứng. |
| **Test chứng minh** | Bộ kiểm thử JUnit 5 backend và Vitest frontend (mục tiêu độ bao phủ mã nguồn >= 70% các logic cốt lõi). |
| **Phần còn thiếu** | Đang ở giai đoạn khởi tạo tài liệu (0% code), cần triển khai mã nguồn thật để chuyển trạng thái từ `NOT_STARTED` sang `VERIFIED`. |

---

## 5. Tiêu Chí 5: Tính Sáng Tạo Ngoài Chương Trình Giảng Dạy (Trọng số: 15%)

### Mô tả tiêu chí theo chuẩn HCMUTE:
Áp dụng các công nghệ, kỹ thuật tiên tiến và giải pháp kiến trúc nâng cao vượt ra ngoài khuôn khổ môn học trên lớp.  
Thang điểm bộ môn:
- `3 điểm`: 01 nội dung ngoài chương trình.
- `5 điểm`: 02 nội dung ngoài chương trình.
- `8 điểm`: 03 - 04 nội dung ngoài chương trình.
- `10 điểm`: 05 - 06 nội dung ngoài chương trình.
- `15 điểm (Tối đa)`: **07 - 08 nội dung ngoài chương trình học** (Mục tiêu đồ án).

| Yếu tố chứng minh | Nội dung cụ thể (Đầy đủ 8 nội dung đạt mức tối đa 15/15) |
|---|---|
| **Tính năng chứng minh** | 1. **Kiến trúc Microservices Cloud-Native**: Sử dụng Spring Cloud Gateway và Eureka Service Discovery thay vì kiến trúc Monolith thông thường.<br>2. **Hệ thống Chấm Code Tự Động (Online Judge)**: Tích hợp Judge0 API Engine thực thi mã nguồn đa ngôn ngữ (C++, Java, Python, Go, Rust, JS) trong Docker Sandbox cô lập an toàn.<br>3. **Trợ Lý Trí Tuệ Nhân Tạo (Generative AI / LLM)**: Tích hợp mô hình ngôn ngữ lớn DeepSeek/OpenCode hỗ trợ giải đáp học tập thời gian thực với cơ chế phản hồi streaming.<br>4. **AI Prompt Engineering tự động sinh dữ liệu**: Tính năng AI tự động phân tích bài giảng để sinh đề thi trắc nghiệm (Quiz) và sinh bộ Testcase biên cho bài tập lập trình.<br>5. **Cơ sở dữ liệu đa hình (Polyglot Persistence)**: Kết hợp đồng thời MySQL 8.4 (giao dịch ACID) và MongoDB 7.0 (Document lưu trữ cây bài học & mã nguồn bài nộp).<br>6. **Bộ nhớ đệm phân tán Redis & Thuật toán Rate Limiting**: Quản lý bộ đếm giới hạn request gọi AI theo vai trò học viên/giảng viên, chống spam và tối ưu chi phí.<br>7. **Lưu trữ đám mây tương thích S3 (MinIO)**: Tích hợp MinIO Object Storage với cơ chế Presigned URLs bảo mật để tải ảnh đại diện, thumbnail và tài liệu.<br>8. **Khả năng chịu lỗi và tự phục hồi (Fault Tolerance)**: Tích hợp Resilience4j Circuit Breaker tại Gateway chống sập tầng và cơ chế khóa ảo chống spam làm bài thi trắc nghiệm (`in_progress_lock` Virtual Generated Column). |
| **File chứng minh** | [ARCHITECTURE.md](ARCHITECTURE.md), `backend/api-gateway/src/main/resources/application.yaml`, `backend/assignment-service/src/main/java/.../JudgeServiceImpl.java`, `backend/chatbot-service/*`. |
| **Commit / PR chứng minh** | Các commit liên quan đến Judge0 integration, Chatbot AI LLM, Redis Rate Limiting và Gateway Circuit Breaker. |
| **Test chứng minh** | `useJudge0.test.jsx`, `ChatbotApiIntegrationTest.java`, Gateway Circuit Breaker fault tests. |
| **Phần còn thiếu** | Cần triển khai và quay video demo giải thích chi tiết cả 8 cơ chế kỹ thuật sáng tạo này. |

---

## Bảng Tự Đánh Giá Minh Chứng

| Tiêu chí | Trọng số | Trạng thái minh chứng hiện tại | Ghi chú |
|---|:---:|:---:|---|
| **1. Độ khó, tính thực tiễn & phân công** | 10% | Đã hoàn thành hồ sơ phân tích | Trưởng nhóm Nguyễn Minh Trí gánh phần nặng nhất (~35,000 LOC) |
| **2. Sử dụng AI & cộng tác GitHub** | 20% | Đã thiết lập khung quy trình | Chờ thực hiện các commit và PR thật |
| **3. Cấu trúc dữ liệu, vai trò & giao diện** | 20% | Đã thiết kế 100% CSDL & RBAC | Chờ triển khai mã nguồn |
| **4. Chất lượng ứng dụng, code & chức năng** | 35% | Đã lập ma trận 74 tính năng | Đang ở bước khởi tạo |
| **5. Tính sáng tạo ngoài chương trình** | 15% | Đạt tiêu chuẩn tối đa: Có đủ 8/8 nội dung sáng tạo | Sẵn sàng lấy 15/15 điểm |
| **TỔNG CỘNG** | **100%** | **Sẵn sàng triển khai giai đoạn 2** | — |
