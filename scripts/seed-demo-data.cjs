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

// 1. Nạp dữ liệu vào MySQL
try {
    const sqlPath = path.resolve(__dirname, '../docker/mysql/init/02-demo-seed-data.sql');
    if (fs.existsSync(sqlPath)) {
        console.log('[1/2] Nạp MySQL demo data từ 02-demo-seed-data.sql ...');
        const cmd = 'docker exec -i lms-mysql mysql -uroot -pmysqladmin123';
        execSync(cmd, { input: fs.readFileSync(sqlPath), stdio: ['pipe', 'inherit', 'inherit'] });
        console.log('  -> MySQL demo data đã nạp thành công!\n');
    } else {
        console.warn('  -> Không tìm thấy file 02-demo-seed-data.sql');
    }
} catch (err) {
    console.error('  -> Lỗi khi nạp MySQL demo data:', err.message);
}

// 2. Nạp dữ liệu vào MongoDB (course_structures & lessons)
try {
    console.log('[2/2] Nạp MongoDB demo data cho course_structures và lessons ...');
    
    // Cấu trúc cây đề cương Khóa học 1
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

    // Cấu trúc cây đề cương Khóa học 2
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
            courseId: 'c1111111-1111-1111-1111-111111111112',
            lessonType: 'DOCUMENT',
            content: { text: '# Cấu hình Thymeleaf & Bootstrap 5\n\nThymeleaf là template engine server-side hiện đại, hỗ trợ layout fragment tái sử dụng (Header, Footer, Navbar) và render động dữ liệu từ Model Spring MVC.' }
        },
        {
            _id: 'les-11111111-1111-1111-1111-111111111113',
            id: 'les-11111111-1111-1111-1111-111111111113',
            courseId: 'c1111111-1111-1111-1111-111111111113',
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
    console.log('  -> ' + output.trim());
    console.log('  -> MongoDB demo data đã nạp thành công!\n');
} catch (err) {
    console.error('  -> Lỗi khi nạp MongoDB demo data:', err.message);
}

console.log('=== HOÀN TẤT NẠP DỮ LIỆU DEMO (PHASE 5) ===');
console.log('Tài khoản demo sẵn sàng để kiểm thử & thuyết trình:');
console.log('  1. Quản trị viên (ADMIN):      admin / admin123');
console.log('  2. Giảng viên (INSTRUCTOR):    instructor_demo / admin123');
console.log('  3. Học viên (STUDENT):         student_demo / admin123');
console.log('Trang Web App: http://localhost:8088\n');
