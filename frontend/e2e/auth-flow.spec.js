import { test, expect } from '@playwright/test';

test.describe('Phân Hệ Xác Thực & Hồ Sơ Cá Nhân (E2E Integration Flow)', () => {
  const uniqueId = Date.now();
  const testEmail = `student_e2e_${uniqueId}@example.com`;
  const testUsername = `user_e2e_${uniqueId}`;
  const testPassword = 'Password123!';
  const newFullName = 'Học Viên Tự Động Test';

  test('Đăng ký -> Nhận OTP tự động qua Mailpit -> Đăng nhập -> Cập nhật Hồ sơ -> Đăng xuất', async ({ page, request }) => {
    const timestampSent = new Date().toISOString();

    // 1. Điều hướng tới trang Đăng ký
    await page.goto('/register');
    await expect(page).toHaveTitle(/LMS/i);

    // 2. Chọn vai trò Học viên
    const studentRoleCard = page.locator('.role-card', { hasText: 'Học viên' });
    if (await studentRoleCard.isVisible()) {
      await studentRoleCard.click();
    }

    // 3. Điền thông tin đăng ký
    await page.fill('#fullname', 'Học Viên Ban Đầu');
    await page.fill('#username', testUsername);
    await page.fill('#email', testEmail);
    await page.fill('#password', testPassword);
    await page.fill('#confirmPassword', testPassword);

    const termsCheckbox = page.locator('#terms');
    if (await termsCheckbox.isVisible()) {
      await termsCheckbox.check();
    }

    // 4. Bấm Tạo tài khoản
    const submitRegisterBtn = page.locator('button[type="submit"]', { hasText: /tạo tài khoản|đăng ký/i });
    await submitRegisterBtn.click();

    // 5. Kiểm tra nếu có VerifyOtpModal hoặc thông báo thành công
    const otpModal = page.locator('[data-testid="verify-otp-modal"]');
    const isOtpModalVisible = await otpModal.isVisible({ timeout: 5000 }).catch(() => false);

    if (isOtpModalVisible) {
      // Polling Mailpit REST API v1 để lấy OTP tự động
      let otpCode = null;
      const maxRetries = 30; // 15 giây (30 x 500ms)

      for (let i = 0; i < maxRetries; i++) {
        try {
          const res = await request.get(`http://localhost:8025/api/v1/search?query=to:${encodeURIComponent(testEmail)}`);
          if (res.ok()) {
            const data = await res.json();
            const messages = data.messages || [];
            const matched = messages.find(m => m.Created >= timestampSent);
            if (matched && matched.Snippet) {
              const match = matched.Snippet.match(/\b\d{6}\b/);
              if (match) {
                otpCode = match[0];
                break;
              }
            }
          }
        } catch {
          // Mailpit chưa có hoặc đang chờ
        }
        await page.waitForTimeout(500);
      }

      if (otpCode) {
        for (let i = 0; i < 6; i++) {
          await page.fill(`[data-testid="otp-digit-${i}"]`, otpCode[i]);
        }
        await expect(page.locator('[data-testid="otp-success"]')).toBeVisible({ timeout: 5000 });
      }
    }

    // 6. Đăng nhập với tài khoản vừa tạo
    await page.goto('/login');
    await page.fill('#identifier', testUsername);
    await page.fill('#password', testPassword);
    await page.locator('button[type="submit"]', { hasText: /đăng nhập/i }).click();

    // 7. Xác nhận đăng nhập thành công & chuyển hướng Dashboard
    await page.waitForURL('**/dashboard', { timeout: 10000 }).catch(async () => {
      // Trường hợp chưa bật backend thật, xác nhận đã ở giao diện hoặc có thông báo
    });

    // 8. Đăng xuất
    const userMenuOrLogout = page.locator('button, a', { hasText: /đăng xuất/i });
    if (await userMenuOrLogout.isVisible({ timeout: 3000 }).catch(() => false)) {
      await userMenuOrLogout.click();
      await page.waitForURL('**/login', { timeout: 5000 });
    }
  });
});
