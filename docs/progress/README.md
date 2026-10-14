# Hướng Dẫn Ghi Nhật Ký Tiến Độ Dự Án (`docs/progress/`)

Thư mục `docs/progress/` là nơi lưu trữ bằng chứng làm việc thực tế hàng ngày của từng thành viên và nhật ký tích hợp toàn hệ thống.

---

## 1. Cấu Trúc Các Tệp Nhật Ký

```text
docs/progress/
├── README.md           # Tài liệu hướng dẫn ghi chép này
├── member-1.md         # Nhật ký công việc thực tế của Thành viên 1 (Nguyễn Minh Trí)
├── member-2.md         # Nhật ký công việc thực tế của Thành viên 2
└── integration.md      # Nhật ký các phiên tích hợp, ghép nối module và xử lý xung đột
```

---

## 2. Quy Chuẩn Ghi Nhật Ký Thành Viên

Mỗi mục nhật ký hàng ngày bắt buộc phải tuân thủ đúng cấu trúc 8 mục:

```markdown
### [YYYY-MM-DD] — Tên Nhóm Công Việc Hoàn Thành

1. **Ngày thực hiện**: DD/MM/YYYY
2. **Công việc đã làm**: Mô tả ngắn gọn, súc tích công việc thực tế đã triển khai.
3. **File thay đổi**: Liệt kê đường dẫn các file đã tạo mới hoặc chỉnh sửa.
4. **Test đã chạy**: Lệnh kiểm thử cụ thể đã chạy (Maven test, Vitest, Postman/curl).
5. **Kết quả**: Trạng thái vượt qua (PASS / FAIL / 0 error).
6. **Commit**: Hash commit hoặc thông điệp commit tương ứng trên Git (nếu đã commit).
7. **Khó khăn & Thách thức**: Các lỗi gặp phải trong quá trình làm và cách khắc phục.
8. **Việc tiếp theo**: Nhiệm vụ dự kiến cần làm trong ngày làm việc tiếp theo.
```

> [!CAUTION]
> **Quy tắc trung thực**:
> Tuyệt đối không ghi nhật ký trước ngày hoặc ghi những việc chưa làm xong. Chỉ ghi nhận khi đã có mã nguồn hoặc kết quả kiểm thử thực tế.
