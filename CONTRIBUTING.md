# Quy Định Đóng Góp & Hợp Tác Nhóm (CONTRIBUTING.md)

Tài liệu này quy định chuẩn mực làm việc, cộng tác mã nguồn trên Git & GitHub dành cho các thành viên phát triển dự án `DoAnCK`. Mọi thành viên bắt buộc phải đọc và tuân thủ.

---

## 1. Nguyên Tắc Định Danh & Tài Khoản

1. **Tài khoản cá nhân**: Mỗi thành viên phải sử dụng chính xác tài khoản GitHub cá nhân của mình khi commit và mở Pull Request (PR).
2. **Cấu hình Git trên máy cá nhân**:
   ```bash
   # Cấu hình danh tính chuẩn
   git config --global user.name "Your Full Name"
   git config --global user.email "your.email@example.com"
   ```
3. **Tuyệt đối không sử dụng tài khoản chung** hoặc commit thay thế người khác mà không có sự ủy quyền rõ ràng.

---

## 2. Chiến Lược Phân Nhánh (Branching Strategy)

Dự án áp dụng mô hình phân nhánh chuẩn **Git Flow biến thể**:

```text
main (Release / Stable - chỉ merge từ develop khi chốt kỳ nộp)
  │
develop (Tích hợp trung tâm - các feature merge vào đây)
  ├── feature/member-1/<ten-tinh-nang>
  ├── feature/member-2/<ten-tinh-nang>
  ├── fix/<ten-loi>
  ├── test/<ten-module>
  └── docs/<ten-tai-lieu>
```

### 2.1 Quy chuẩn đặt tên nhánh

| Loại nhánh | Cú pháp đặt tên | Ví dụ |
|---|---|---|
| **Tính năng Thành viên 1** | `feature/member-1/<tên-tính-năng>` | `feature/member-1/auth-jwt-otp`<br>`feature/member-1/course-management` |
| **Tính năng Thành viên 2** | `feature/member-2/<tên-tính-năng>` | `feature/member-2/quiz-system`<br>`feature/member-2/code-judge-engine` |
| **Sửa lỗi phát sinh** | `fix/<tên-lỗi-ngắn-gọn>` | `fix/gateway-route-timeout`<br>`fix/jwt-expiry-format` |
| **Bổ sung kiểm thử** | `test/<tên-module>` | `test/assignment-service`<br>`test/identity-auth` |
| **Cập nhật tài liệu** | `docs/<nội-dung>` | `docs/api-contract-update`<br>`docs/rubric-mapping` |

### 2.2 Quy trình tạo nhánh mới
Luôn tạo nhánh mới từ nhánh `develop` mới nhất:
```bash
git checkout develop
git pull origin develop
git checkout -b feature/member-1/identity-auth
```

---

## 3. Quy Chuẩn Thông Điệp Commit (Conventional Commits)

Mọi commit phải tuân thủ chuẩn **Conventional Commits 1.0.0**:

### Cấu trúc commit:
```text
<type>(<scope>): <mô tả ngắn gọn bằng tiếng Việt hoặc tiếng Anh>

[Mô tả chi tiết tùy chọn: giải thích lý do, quyết định thiết kế]

[Footer: Liên kết Issue hoặc Breaking Changes nếu có]
```

### Các tiền tố (Types) hợp lệ:
- `feat`: Thêm tính năng mới (ví dụ: `feat(quiz): thêm API nộp bài trắc nghiệm`)
- `fix`: Sửa lỗi phát sinh (ví dụ: `fix(auth): sửa lỗi kiểm tra OTP quá hạn`)
- `test`: Thêm hoặc sửa mã kiểm thử (ví dụ: `test(judge): bổ sung testcase cho code runner`)
- `docs`: Thêm hoặc cập nhật tài liệu (ví dụ: `docs(api): cập nhật bảng API Contract`)
- `refactor`: Tái cấu trúc mã nguồn không thay đổi hành vi nghiệp vụ (ví dụ: `refactor(course): tách mapper sang MapStruct`)
- `chore`: Thay đổi cấu hình công cụ, dependencies, build script (ví dụ: `chore(docker): cập nhật docker-compose db init`)
- `build`: Thay đổi hệ thống build Maven / Vite
- `ci`: Thay đổi cấu hình CI/CD GitHub Actions

### Những điều TUYỆT ĐỐI KHÔNG làm:
- Không viết commit mơ hồ: `update`, `fix bug`, `làm tiếp`, `done`, `final`, `final 2`.
- Không gom quá nhiều tính năng không liên quan vào 1 commit khổng lồ.
- Không commit các file tạm, `.env` chứa mật khẩu thật, `node_modules`, thư mục `target`.

---

## 4. Quy Trình Tạo & Duyệt Pull Request (PR)

Các thay đổi lớn hoặc hoàn thành một tính năng bắt buộc phải tạo Pull Request để review trước khi merge vào `develop`.

### 4.1 Tiêu đề & Nội dung PR
- **Tiêu đề**: Rõ ràng, phản ánh đúng nội dung tính năng:
  `[Feature] Triển khai hệ thống Quiz và Quản lý Câu hỏi (Member 2)`
- **Nội dung PR** phải bao gồm:
  - **Mục tiêu**: PR này giải quyết vấn đề gì?
  - **Danh sách thay đổi**: Các module/file đã chỉnh sửa hoặc tạo mới.
  - **Hướng dẫn test**: Các bước chạy thử, lệnh test hoặc kịch bản postman/curl.
  - **Minh chứng**: Ảnh chụp màn hình kết quả chạy test, log build thành công hoặc giao diện UI.

### 4.2 Checklist Review trước khi Merge
Người review (thành viên còn lại) phải tích chọn xác nhận:
- [ ] Mã nguồn đã tuân thủ đúng [CODING_RULES.md](docs/CODING_RULES.md).
- [ ] Không có mật khẩu, token, secret key bị hardcode.
- [ ] Đã chạy thử lệnh build (`mvn clean compile`, `npm run build`) không có lỗi.
- [ ] Các Unit / Integration Tests liên quan đều PASS.
- [ ] API đã được cập nhật vào [API_CONTRACT.md](docs/API_CONTRACT.md).
- [ ] Trạng thái đã được cập nhật trong [FEATURE_PARITY_MATRIX.md](docs/FEATURE_PARITY_MATRIX.md).

### 4.3 Điều kiện Merge vào `develop`
1. Tối thiểu **1 Approved Review** từ thành viên trong nhóm.
2. Tất cả GitHub Actions CI checks (nếu có) phải báo **Xanh (Pass)**.
3. Nhánh không có xung đột (No merge conflicts).
4. Sử dụng phương thức **Squash and Merge** hoặc **Create a merge commit** tùy theo quy mô của nhánh.

---

## 5. Quy Trình Release Lên Nhánh `main`
- Chỉ tạo PR từ `develop` sang `main` khi đã hoàn thành các cột mốc kiểm thử toàn diện (End-to-End Test, Docker Smoke Test).
- Nhánh `main` luôn là bản ổn định nhất, sẵn sàng đóng gói và gắn tag release (ví dụ: `v1.0.0`).
