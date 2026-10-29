const { test, describe, before, after } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('fs');
const path = require('path');
const http = require('http');

let chromium;
try {
    chromium = require('D:/LMS-main/frontend/node_modules/@playwright/test').chromium;
} catch (e) {
    chromium = require('@playwright/test').chromium;
}

const WEBAPP_URL = process.env.SMOKE_WEBAPP_URL || 'http://localhost:8088';
const MINIO_URL = process.env.SMOKE_MINIO_URL || 'http://localhost:9000';
const TEST_USER = process.env.SMOKE_TEST_USER || 'admin';
const TEST_PASSWORD = process.env.SMOKE_TEST_PASSWORD || 'admin123';

describe('Browser E2E: Avatar Upload & MinIO CORS Verification', () => {
    let browser;
    let context;
    let unauthorizedServer;
    const unauthorizedPort = 9876;
    const unauthorizedOrigin = `http://127.0.0.1:${unauthorizedPort}`;
    const testImagePath = path.join(__dirname, 'test-avatar-fixture.png');

    before(async () => {
        // Chuẩn bị file ảnh 1x1 PNG hợp lệ
        const pngBuffer = Buffer.from(
            'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==',
            'base64'
        );
        fs.writeFileSync(testImagePath, pngBuffer);

        // Khởi tạo máy chủ HTTP giả lập Unauthorized Origin
        unauthorizedServer = http.createServer((req, res) => {
            res.writeHead(200, { 'Content-Type': 'text/html' });
            res.end(`
                <!DOCTYPE html>
                <html>
                <body>
                    <h1>Unauthorized Origin Test</h1>
                    <script>
                        window.testMinioCors = async function(minioEndpoint) {
                            try {
                                const res = await fetch(minioEndpoint + '/lms-identity-bucket/test.png', {
                                    method: 'OPTIONS',
                                    headers: {
                                        'Access-Control-Request-Method': 'POST'
                                    }
                                });
                                return { success: true, status: res.status };
                            } catch (err) {
                                return { success: false, error: err.message };
                            }
                        };
                    </script>
                </body>
                </html>
            `);
        });

        await new Promise(resolve => unauthorizedServer.listen(unauthorizedPort, '127.0.0.1', resolve));

        browser = await chromium.launch({ headless: true });
    });

    after(async () => {
        if (browser) await browser.close();
        if (unauthorizedServer) {
            await new Promise(resolve => unauthorizedServer.close(resolve));
        }
        if (fs.existsSync(testImagePath)) {
            try { fs.unlinkSync(testImagePath); } catch (e) {}
        }
    });

    test('1. Giao diện Thymeleaf thật: Đăng nhập, chọn ảnh, direct upload MinIO, lưu CSDL và tải lại trang', async () => {
        const testContext = await browser.newContext();
        const page = await testContext.newPage();

        // 1.1 Đăng nhập
        await page.goto(`${WEBAPP_URL}/login`);
        await page.waitForLoadState('networkidle');

        await page.fill('#username', TEST_USER);
        await page.fill('#password', TEST_PASSWORD);

        await Promise.all([
            page.waitForNavigation({ waitUntil: 'networkidle' }),
            page.click('button[type="submit"]')
        ]);

        assert.notEqual(page.url(), `${WEBAPP_URL}/login`, 'Đăng nhập phải chuyển hướng khỏi trang login');

        // 1.2 Mở trang Profile
        await page.goto(`${WEBAPP_URL}/profile`);
        await page.waitForLoadState('networkidle');

        const initialOuterHtml = await page.evaluate(() => document.getElementById('avatarImage')?.outerHTML);
        assert.ok(initialOuterHtml, '#avatarImage phải tồn tại trên trang profile');

        // Đảm bảo fullname có giá trị nếu form rỗng
        const currentFullName = await page.inputValue('#fullname');
        if (!currentFullName || currentFullName.trim().length === 0) {
            await page.fill('#fullname', 'Quản Trị Viên LMS');
        }

        // 1.3 Upload avatar
        let minioUploadStatus = 0;
        page.on('response', res => {
            if (res.url().includes(MINIO_URL) && res.url().includes('lms-identity-bucket') && res.request().method() === 'POST') {
                minioUploadStatus = res.status();
            }
        });

        const navigationPromise = page.waitForNavigation({ waitUntil: 'networkidle' });
        await page.setInputFiles('#avatarFileInput', testImagePath);
        await navigationPromise;

        assert.ok(minioUploadStatus === 204 || minioUploadStatus === 200,
            `MinIO upload phải trả về status 204 hoặc 200 nhưng nhận được: ${minioUploadStatus}`);

        const updatedAvatarSrc = await page.evaluate(() => {
            const img = document.getElementById('avatarImage');
            return img ? img.getAttribute('src') : null;
        });

        assert.ok(updatedAvatarSrc && updatedAvatarSrc.includes('lms-identity-bucket'),
            `Avatar src phải chứa lms-identity-bucket nhưng nhận được: ${updatedAvatarSrc}`);

        // 1.4 Tải lại trang (F5) và xác nhận avatar đã được lưu bền vững vào CSDL
        await page.reload({ waitUntil: 'networkidle' });
        const persistedAvatarSrc = await page.evaluate(() => {
            const img = document.getElementById('avatarImage');
            return img ? img.getAttribute('src') : null;
        });

        assert.equal(persistedAvatarSrc, updatedAvatarSrc,
            'Avatar sau khi tải lại trang phải khớp với avatar đã upload lên MinIO');

        await testContext.close();
    });

    test('2. Trình duyệt tự động chặn CORS khi fetch tới MinIO từ Unauthorized Origin', async () => {
        const testContext = await browser.newContext();
        const page = await testContext.newPage();
        await page.goto(unauthorizedOrigin);
        await page.waitForLoadState('networkidle');

        const corsResult = await page.evaluate(minioEndpoint => window.testMinioCors(minioEndpoint), MINIO_URL);

        assert.equal(corsResult.success, false,
            'Fetch từ Unauthorized Origin tới MinIO không được phép thành công');
        assert.ok(corsResult.error.includes('Failed to fetch') || corsResult.error.includes('NetworkError'),
            `Lỗi ném ra từ trình duyệt phải là lỗi CORS (Failed to fetch), nhận được: ${corsResult.error}`);

        await testContext.close();
    });

    test('3. Chu trình đăng ký tài khoản mới, gửi OTP qua Mailpit thật, kích hoạt và đăng nhập', async () => {
        const mailpitUrl = process.env.SMOKE_MAILPIT_URL || 'http://localhost:8025';
        const testContext = await browser.newContext();
        const page = await testContext.newPage();

        const randomSuffix = Math.random().toString(36).substring(2, 8);
        const newUsername = `student_${randomSuffix}`;
        const newEmail = `student_${randomSuffix}@test.lms.vn`;
        const newPassword = 'Password123!';

        // 3.1 Mở trang Đăng ký và điền form
        await page.goto(`${WEBAPP_URL}/register`);
        await page.waitForLoadState('networkidle');

        const fullnameInput = await page.$('#fullname');
        if (fullnameInput) {
            await page.fill('#fullname', 'Học Viên EduLearn');
        }
        await page.fill('#username', newUsername);
        await page.fill('#email', newEmail);
        await page.fill('#password', newPassword);
        await page.fill('#confirmPassword', newPassword);

        // Submit form đăng ký
        await Promise.all([
            page.waitForNavigation({ waitUntil: 'networkidle' }),
            page.click('button[type="submit"]')
        ]);

        assert.ok(page.url().includes('/verify-email'),
            `Đăng ký thành công phải chuyển hướng sang /verify-email nhưng nhận được: ${page.url()}`);

        // 3.2 Lấy mã OTP từ Mailpit API thật
        let otpCode = null;
        for (let i = 0; i < 15; i++) {
            await new Promise(r => setTimeout(r, 1000));
            const res = await fetch(`${mailpitUrl}/api/v1/messages`);
            const data = await res.json();
            const messages = data.messages || [];

            const msg = messages.find(m => m.To && m.To.some(to => to.Address === newEmail));
            if (msg) {
                const detailRes = await fetch(`${mailpitUrl}/api/v1/message/${msg.ID}`);
                const detailData = await detailRes.json();
                const bodyText = detailData.Text || detailData.HTML || '';
                const match = bodyText.match(/\b\d{6}\b/);
                if (match) {
                    otpCode = match[0];
                    break;
                }
            }
        }

        assert.ok(otpCode, `Không tìm thấy email OTP trong Mailpit gửi đến ${newEmail}`);

        // 3.3 Nhập OTP và xác thực email
        await page.fill('#otp', otpCode);
        await Promise.all([
            page.waitForNavigation({ waitUntil: 'networkidle' }),
            page.click('button[type="submit"]')
        ]);

        assert.ok(page.url().includes('/login'),
            `Xác thực OTP thành công phải chuyển hướng về /login nhưng nhận được: ${page.url()}`);

        // 3.4 Đăng nhập với tài khoản vừa kích hoạt
        await page.fill('#username', newUsername);
        await page.fill('#password', newPassword);
        await Promise.all([
            page.waitForNavigation({ waitUntil: 'networkidle' }),
            page.click('button[type="submit"]')
        ]);

        assert.notEqual(page.url(), `${WEBAPP_URL}/login`, 'Đăng nhập tài khoản mới phải thành công');

        await testContext.close();
    });
});

