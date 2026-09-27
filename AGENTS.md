# AGENTS.md — Quy Định Bắt Buộc Dành Cho AI Coding Agent

Tài liệu này xác định các nguyên tắc, ràng buộc kỹ thuật và quy trình chuẩn mà AI Agent (Antigravity / Coding Assistant) phải tuân thủ nghiêm ngặt trong suốt quá trình phát triển dự án `DoAnCK`.

---

## 1. Phân Vùng Quyền Hạn Trong Workspace

Hệ thống có hai phân vùng dự án rõ ràng:

### 1.1 Thư mục chỉ được đọc (READ-ONLY)
- **Đường dẫn**: `D:\UTE\Nam_3_2026_2027_1\nam3\LTW\DACK\Learning-Management-System-main`
- **Mục đích**: Là nguồn tham chiếu chuẩn duy nhất về mặt kiến trúc, hành vi chức năng, entity, controller, API contracts, cấu hình và luồng xử lý.
- **Ràng buộc**:
  - **TUYỆT ĐỐI KHÔNG** sửa, thêm, xóa, format hoặc đổi tên bất kỳ file/thư mục nào trong project mẫu.
  - **KHÔNG** chạy các lệnh thay đổi dữ liệu hoặc trạng thái trong project mẫu (như `git checkout`, `git clean`, `npm install`, build modifying files).
  - Nếu phát hiện lỗi hoặc thiếu sót trong project mẫu: Ghi nhận vào [docs/OPEN_QUESTIONS.md](docs/OPEN_QUESTIONS.md) và [docs/DECISIONS.md](docs/DECISIONS.md), **không sao chép lỗi** sang `DoAnCK`.

### 1.2 Thư mục được phép làm việc (READ-WRITE)
- **Đường dẫn**: `D:\UTE\Nam_3_2026_2027_1\nam3\LTW\DACK\DoAnCK`
- **Mục đích**: Chứa toàn bộ mã nguồn, tài liệu, cấu hình và kiểm thử của dự án cần phát triển.
- **Ràng buộc**:
  - Mọi thao tác tạo file, sửa code, xóa file, cấu hình docker, kiểm thử và commit chỉ được thực hiện trong phạm vi `DoAnCK`.
  - Mọi đường dẫn tương đối phải lấy gốc từ `DoAnCK`.

---

## 2. Quy Trình Làm Việc Tiêu Chuẩn Của Agent

Trước khi thực hiện bất kỳ thay đổi nào trong mã nguồn:

### Bước 1: Khảo sát & Đọc hiểu
- Đọc kỹ yêu cầu người dùng và đối chiếu với mã nguồn liên quan trong project mẫu.
- Xác định rõ luồng dữ liệu giữa Frontend -> API Gateway -> Microservice -> Database / Cache / Storage.

### Bước 2: Liệt kê trước khi sửa (Mandatory Pre-flight)
Trước mỗi lần viết hoặc sửa code, Agent phải trình bày rõ trong suy luận hoặc tài liệu:
1. **Mục tiêu**: Chức năng hoặc thành phần cần hoàn thành.
2. **File sẽ tạo**: Danh sách file mới kèm đường dẫn tuyệt đối hoặc tương đối chuẩn.
3. **File sẽ sửa**: Danh sách file hiện có cần can thiệp.
4. **Input**: Dữ liệu đầu vào, DTO, Request Body, Query params hoặc props.
5. **Output**: Dữ liệu đầu ra, Response format, HTTP status codes, UI state.
6. **Cách kiểm thử**: Lệnh build, unit test, integration test hoặc kịch bản kiểm thử API/UI.

### Bước 3: Triển khai mã nguồn
- Viết mã sạch, tuân thủ nguyên tắc SOLID, Clean Architecture và tài liệu [docs/CODING_RULES.md](docs/CODING_RULES.md).
- Không copy-paste mù quáng; điều chỉnh cấu trúc package, biến môi trường và tối ưu hóa xử lý lỗi.
- Đảm bảo có đầy đủ validation ở cả frontend (client-side) và backend (Jakarta Validation).

### Bước 4: Kiểm thử bắt buộc (Verification)
- Không tuyên bố một chức năng đã hoàn thành nếu chưa chạy lệnh build hoặc test thực tế.
- Bắt buộc kiểm tra lỗi compile, linting và runtime errors.
- Tuyệt đối không được bỏ qua (ignore) các lỗi build hoặc test đang fail.

---

## 3. Quy Tắc Bảo Mật & Toàn Vẹn Dữ Liệu

1. **Không rò rỉ Secrets**:
   - Tuyệt đối không hardcode mật khẩu, Private Key, JWT Secret, Mail App Password, Google OAuth Secret, LLM API Key, Judge0 Key vào mã nguồn hoặc tài liệu markdown.
   - Luôn sử dụng biến môi trường thông qua `.env` và Spring `@Value` / `@ConfigurationProperties`.
2. **Không sao chép file nhạy cảm từ project mẫu**:
   - Không copy `.env` thật, credentials thật, file build `target/`, thư mục `node_modules/`, file log hoặc file `.git`.
   - File `.env.example` phải dùng giá trị placeholder an toàn (`replace-with-...`).
3. **Không dùng dữ liệu giả (Mock) để che giấu lỗi**:
   - Dữ liệu trả về từ API backend phải được truy vấn thật từ MySQL, MongoDB hoặc Redis.
   - Không được hardcode JSON cố định trong controller để giả lập chức năng đã hoạt động.

---

## 4. Quy Tắc Quản Lý Mã Nguồn (Git & Commits)

1. **Không tự động commit/push**:
   - Agent **KHÔNG ĐƯỢC TỰ Ý** chạy `git commit`, `git push` hoặc `git merge` trừ khi có yêu cầu rõ ràng từ người dùng.
2. **Không tạo commit giả hoặc commit vô nghĩa**:
   - Không tạo các commit kiểu: `update`, `fix`, `done`, `edit file`.
   - Mọi commit phải theo chuẩn **Conventional Commits** (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`).
3. **Không can thiệp lịch sử hoặc backdate**:
   - Tuyệt đối không chỉnh sửa ngày giờ commit (`GIT_AUTHOR_DATE`, `GIT_COMMITTER_DATE`) để gian lận lịch sử.
   - Chỉ commit khi công việc tương ứng đã được thực hiện thật, build thành công và vượt qua kiểm thử.
4. **Phân chia nhánh**:
   - Tuân thủ quy định nhánh tại [docs/GIT_WORKFLOW.md](docs/GIT_WORKFLOW.md).
   - Không phát triển trực tiếp trên nhánh `main`.

---

## 5. Quy Chuẩn Đánh Giá Hoàn Thành 100%

Một chức năng chỉ được đánh dấu là `VERIFIED` trong [docs/FEATURE_PARITY_MATRIX.md](docs/FEATURE_PARITY_MATRIX.md) khi thỏa mãn đủ 13 tiêu chí:
1. Có giao diện người dùng (UI) hoàn chỉnh với trạng thái Loading, Empty, Success, Error.
2. Có API endpoint backend tương ứng được định tuyến qua API Gateway.
3. Có validation dữ liệu đầu vào chặt chẽ.
4. Có phân quyền theo vai trò (RBAC) chính xác.
5. Có xử lý ngoại lệ và trả về ApiResponse chuẩn.
6. Có lưu trữ và cập nhật trạng thái trong cơ sở dữ liệu thật.
7. Có kiểm thử tự động (Unit Test / Integration Test) vượt qua thành công.
8. Biên dịch và đóng gói thành công (Maven / Vite build không lỗi).
9. Hoạt động ổn định trong môi trường Docker Compose.
10. Ghi nhận tài liệu trong API Contract.
11. Đã có commit giải quyết nhóm chức năng đó.
12. Có hướng dẫn sử dụng và kiểm thử rõ ràng.
13. Hành vi nghiệp vụ tương đương 100% so với project mẫu.
