#!/usr/bin/env node
/**
 * LMS Demo Seeding Script (Phase 5 - Dữ liệu Demo Chuẩn Hóa)
 * Nạp dữ liệu mẫu vào cả MySQL và MongoDB (course_structures, lessons)
 * Có thể chạy lặp lại an toàn (Idempotent: Upsert / Replace).
 *
 * Cách dùng:
 *   node scripts/seed-demo-data.cjs
 */

const { execSync } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');

console.log('=== BẮT ĐẦU NẠP DỮ LIỆU DEMO CHUẨN HÓA (PHASE 5) ===\n');

// 1. Đọc mật khẩu CSDL an toàn từ biến môi trường hoặc .env
function resolveDbPassword() {
    const env = (typeof process !== 'undefined' && process.env) || {};
    if (env.MYSQL_ROOT_PASSWORD) return env.MYSQL_ROOT_PASSWORD;
    if (env.MYSQL_PASSWORD) return env.MYSQL_PASSWORD;
    if (env.SMOKE_DB_PASSWORD) return env.SMOKE_DB_PASSWORD;
    try {
        const envPath = path.resolve(__dirname, '../.env');
        if (fs.existsSync(envPath)) {
            const raw = fs.readFileSync(envPath, 'utf8');
            const match = raw.match(/^MYSQL_ROOT_PASSWORD\s*=\s*(.*)$/m);
            if (match) return match[1].trim();
        }
    } catch {
        // bỏ qua nếu không đọc được file .env
    }
    return '';
}

// 2. Đọc mật khẩu demo an toàn từ biến môi trường (ưu tiên 1) hoặc file .env (ưu tiên 2).
// Tuyệt đối không fallback sang mật khẩu quản trị (BOOTSTRAP_ADMIN_PASSWORDS) hay mật khẩu mặc định cố định.
function resolveDemoPassword() {
    const env = (typeof process !== 'undefined' && process.env) || {};
    if (env.DEMO_USER_PASSWORD && env.DEMO_USER_PASSWORD.trim()) {
        return env.DEMO_USER_PASSWORD.trim();
    }
    try {
        const envPath = path.resolve(__dirname, '../.env');
        if (fs.existsSync(envPath)) {
            const raw = fs.readFileSync(envPath, 'utf8');
            const match = raw.match(/^DEMO_USER_PASSWORD\s*=\s*(.*)$/m);
            if (match && match[1].trim()) {
                return match[1].trim();
            }
        }
    } catch {
        // bỏ qua nếu môi trường test cách ly vô hiệu hóa đọc file .env
    }
    return '';
}

// 3. Nạp BCrypt qua CommonJS; lỗi dependency phải dừng seed, không dùng fallback.
function loadBcrypt() {
    return require('./bcrypt.js');
}

// 4. Sinh hash mật khẩu BCrypt động theo mật khẩu được cung cấp.
// Tuyệt đối không dùng salt cố định hoặc chuỗi hash cố định làm fallback.
function generatePasswordHash(rawPassword) {
    if (!rawPassword || typeof rawPassword !== 'string') {
        return null;
    }
    try {
        const bcrypt = loadBcrypt();
        if (bcrypt && typeof bcrypt.hashSync === 'function' && typeof bcrypt.genSaltSync === 'function') {
            const salt = bcrypt.genSaltSync(10);
            return bcrypt.hashSync(rawPassword, salt);
        }
    } catch {
        // Ghi nhận nếu sinh thất bại, không dùng fallback giả lập
    }
    return null;
}

function runSeed() {
    const demoPassword = resolveDemoPassword();
    if (!demoPassword) {
        console.error('[LỖI BẢO MẬT] Chưa cung cấp mật khẩu demo. Vui lòng đặt biến môi trường DEMO_USER_PASSWORD hoặc cấu hình trong .env.');
        if (typeof process !== 'undefined') {
            process.exitCode = 1;
            if (typeof process.exit === 'function') {
                process.exit(1);
            }
        }
        return;
    }

    const demoPasswordHash = generatePasswordHash(demoPassword);
    if (!demoPasswordHash) {
        console.error('[LỖI] Không thể sinh hash BCrypt hợp lệ cho mật khẩu demo.');
        if (typeof process !== 'undefined') {
            process.exitCode = 1;
            if (typeof process.exit === 'function') {
                process.exit(1);
            }
        }
        return;
    }

    const dbPassword = resolveDbPassword();

const errors = [];
const summary = { mysql: false, mongodb: false };

// 5. Nạp dữ liệu vào MySQL
try {
    const sqlPath = path.resolve(__dirname, '../docker/mysql/init/02-demo-seed-data.sql');
    if (!fs.existsSync(sqlPath)) {
        throw new Error(`Không tìm thấy file SQL tại ${sqlPath}`);
    }
    console.log('[1/2] Nạp MySQL demo data từ 02-demo-seed-data.sql ...');

    // Đọc file SQL và thay thế placeholder hash động
    let sqlContent = fs.readFileSync(sqlPath, 'utf8');
    sqlContent = sqlContent.replaceAll('__DEMO_PASSWORD_HASH__', demoPasswordHash);
    sqlContent = 'SET NAMES utf8mb4;\nSET CHARACTER SET utf8mb4;\n' + sqlContent;

    const childEnv = { ...((typeof process !== 'undefined' && process.env) || {}), MYSQL_PWD: dbPassword };
    const cmd = 'docker exec -i -e MYSQL_PWD lms-mysql mysql -uroot --default-character-set=utf8mb4';
    execSync(cmd, {
        input: sqlContent,
        env: childEnv,
        stdio: ['pipe', 'pipe', 'pipe']
    });
    summary.mysql = true;
    console.log('  -> MySQL demo data đã nạp thành công!\n');
} catch (err) {
    errors.push({ step: 'MySQL', error: err.message });
    console.error('  -> Lỗi khi nạp MySQL demo data:', err.message);
}

// 6. Nạp dữ liệu vào MongoDB (course_structures & lessons)
try {
    console.log('[2/2] Nạp MongoDB demo data cho course_structures và lessons ...');

    // Cấu trúc cây đề cương Khóa học 1 (courseId: c1111111-1111-1111-1111-111111111111)
    const structCourse1 = {
        _id: 'd1111111-1111-1111-1111-111111111111',
        courseId: 'c1111111-1111-1111-1111-111111111111',
        nodes: [
            { id: 'node-ch1', _id: 'node-ch1', type: 'FOLDER', title: 'Chương 1: Giới thiệu & Cài đặt môi trường', order: 1, parentId: null },
            { id: 'node-les1', _id: 'node-les1', type: 'LESSON', title: 'Bài 1.1: Tổng quan Spring Boot 3 & Kiến trúc Monolith BFF', order: 1, parentId: 'node-ch1', lessonId: 'les-11111111-1111-1111-1111-111111111111' },
            { id: 'node-les2', _id: 'node-les2', type: 'LESSON', title: 'Bài 1.2: Cấu hình Maven, Thymeleaf & Bootstrap 5', order: 2, parentId: 'node-ch1', lessonId: 'les-11111111-1111-1111-1111-111111111112' },
            { id: 'node-ch2', _id: 'node-ch2', type: 'FOLDER', title: 'Chương 2: Xây dựng REST API & Kết nối CSDL', order: 2, parentId: null },
            { id: 'node-les3', _id: 'node-les3', type: 'LESSON', title: 'Bài 2.1: Spring Data JPA, Validation & Exception Handling', order: 1, parentId: 'node-ch2', lessonId: 'les-11111111-1111-1111-1111-111111111113' }
        ],
        updatedAt: new Date()
    };

    // Cấu trúc cây đề cương Khóa học 2 (courseId: c2222222-2222-2222-2222-222222222222)
    const structCourse2 = {
        _id: 'd2222222-2222-2222-2222-222222222222',
        courseId: 'c2222222-2222-2222-2222-222222222222',
        nodes: [
            { id: 'node-c2-ch1', _id: 'node-c2-ch1', type: 'FOLDER', title: 'Chương 1: Tổng quan Kiến trúc Phân tán', order: 1, parentId: null },
            { id: 'node-c2-les1', _id: 'node-c2-les1', type: 'LESSON', title: 'Bài 1.1: Service Discovery với Netflix Eureka Server', order: 1, parentId: 'node-c2-ch1', lessonId: 'les-22222222-2222-2222-2222-222222222221' },
            { id: 'node-c2-les2', _id: 'node-c2-les2', type: 'LESSON', title: 'Bài 1.2: API Gateway định tuyến động & Chống giả mạo danh tính', order: 2, parentId: 'node-c2-ch1', lessonId: 'les-22222222-2222-2222-2222-222222222222' }
        ],
        updatedAt: new Date()
    };

    // Các bài học demo thuộc đúng courseId của cấu trúc cây đề cương
    const lessons = [
        {
            _id: 'les-11111111-1111-1111-1111-111111111111',
            id: 'les-11111111-1111-1111-1111-111111111111',
            courseId: 'c1111111-1111-1111-1111-111111111111',
            lessonType: 'DOCUMENT',
            content: { text: '# Chào mừng bạn đến với Spring Boot 3!\n\nTrong bài học này, chúng ta sẽ tìm hiểu kiến trúc tổng quan của dự án LMS, cách Spring Boot đóng gói backend và kết nối với Thymeleaf UI.' }
        },
        {
            _id: 'les-11111111-1111-1111-1111-111111111112',
            id: 'les-11111111-1111-1111-1111-111111111112',
            courseId: 'c1111111-1111-1111-1111-111111111111',
            lessonType: 'DOCUMENT',
            content: { text: '# Cấu hình Thymeleaf & Bootstrap 5\n\nThymeleaf là template engine server-side hiện đại, hỗ trợ layout fragment tái sử dụng (Header, Footer, Navbar) và render động dữ liệu từ Model Spring MVC.' }
        },
        {
            _id: 'les-11111111-1111-1111-1111-111111111113',
            id: 'les-11111111-1111-1111-1111-111111111113',
            courseId: 'c1111111-1111-1111-1111-111111111111',
            lessonType: 'DOCUMENT',
            content: { text: '# Spring Data JPA & Validation\n\nSử dụng Jakarta Validation (`@Valid`, `@NotBlank`, `@Size`) để bảo đảm tính toàn vẹn dữ liệu đầu vào trước khi lưu trữ vào MySQL.' }
        },
        {
            _id: 'les-22222222-2222-2222-2222-222222222221',
            id: 'les-22222222-2222-2222-2222-222222222221',
            courseId: 'c2222222-2222-2222-2222-222222222222',
            lessonType: 'DOCUMENT',
            content: { text: '# Service Discovery với Eureka\n\nEureka Server hoạt động tại port 8761, nhận heartbeat từ các microservices và cung cấp registry địa chỉ cho API Gateway định tuyến.' }
        },
        {
            _id: 'les-22222222-2222-2222-2222-222222222222',
            id: 'les-22222222-2222-2222-2222-222222222222',
            courseId: 'c2222222-2222-2222-2222-222222222222',
            lessonType: 'DOCUMENT',
            content: { text: '# API Gateway & Circuit Breaker Resilience4j\n\nGateway chặn header giả mạo, xác thực JWT, định tuyến request và áp dụng Circuit Breaker với Fallback Controller khi downstream service gặp sự cố.' }
        }
    ];

    const mongoScript = [
        "const dbCourse = db.getSiblingDB('course_lms');",
        `dbCourse.course_structures.replaceOne({ _id: '${structCourse1._id}' }, ${JSON.stringify(structCourse1)}, { upsert: true });`,
        `dbCourse.course_structures.replaceOne({ _id: '${structCourse2._id}' }, ${JSON.stringify(structCourse2)}, { upsert: true });`,
        ...lessons.map(l => `dbCourse.lessons.replaceOne({ _id: '${l._id}' }, ${JSON.stringify(l)}, { upsert: true });`),
        `print('MongoDB demo data upserted: 2 course_structures, ' + ${lessons.length} + ' lessons.');`
    ].join('\n');

    const output = execSync('docker exec -i lms-mongodb mongosh --quiet', {
        input: mongoScript,
        encoding: 'utf8'
    });
    summary.mongodb = true;
    if (output && output.trim()) {
        console.log('  -> ' + output.trim());
    }
    console.log('  -> MongoDB demo data đã nạp thành công!\n');
} catch (err) {
    errors.push({ step: 'MongoDB', error: err.message });
    console.error('  -> Lỗi khi nạp MongoDB demo data:', err.message);
}

// 7. Báo cáo kết quả và kết thúc tiến trình
if (errors.length > 0) {
    console.error('\n[THẤT BẠI] Quá trình nạp dữ liệu demo không hoàn tất do có lỗi:');
    for (const item of errors) {
        console.error(` - Bước ${item.step}: THẤT BẠI (${item.error})`);
    }
    console.error(`Trạng thái: MySQL = ${summary.mysql ? 'THÀNH CÔNG' : 'THẤT BẠI'}, MongoDB = ${summary.mongodb ? 'THÀNH CÔNG' : 'THẤT BẠI'}`);
    console.error('Lưu ý: Dữ liệu giữa MySQL và MongoDB không dùng transaction xuyên hệ thống. Vui lòng khắc phục lỗi và chạy lại script.\n');
    if (typeof process !== 'undefined') {
        process.exitCode = 1;
        if (typeof process.exit === 'function') {
            process.exit(1);
        }
    }
} else {
    console.log('=== HOÀN TẤT NẠP DỮ LIỆU DEMO (PHASE 5) ===');
    console.log('Tài khoản demo sẵn sàng để kiểm thử & thuyết trình:');
    console.log('  1. Quản trị viên (ADMIN):      admin (mật khẩu cấu hình qua BOOTSTRAP_ADMIN_PASSWORDS)');
    console.log('  2. Giảng viên (INSTRUCTOR):    instructor_demo (mật khẩu băm động từ biến môi trường)');
    console.log('  3. Học viên (STUDENT):         student_demo (mật khẩu băm động từ biến môi trường)');
    console.log('Trang Web App: http://localhost:8088\n');
}
}

runSeed();
