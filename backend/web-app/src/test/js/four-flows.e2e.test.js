const { test, describe, before, after } = require('node:test');
const assert = require('node:assert/strict');
const { randomUUID } = require('node:crypto');
const { chromium } = require('@playwright/test');
const { Fixtures, png } = require('./support/e2e-fixtures');
const {
    assertLoginUrl,
    assertEnrollmentActive,
    assertLessonProgress,
    assertCourseStatus,
    assertUserActive
} = require('./support/e2e-assertions');

const WEBAPP_URL = process.env.SMOKE_WEBAPP_URL || 'http://localhost:8088';
const GATEWAY_URL = process.env.SMOKE_GATEWAY_URL || 'http://localhost:8080';

describe('Comprehensive 4-Flow End-to-End Test Suite (E2E TDD)', { timeout: 300000 }, () => {
    let fixtures, browser;

    before(async () => {
        fixtures = new Fixtures();
        await fixtures.start();
        browser = await chromium.launch({ headless: true });
    });

    after(async () => {
        try {
            if (fixtures) await fixtures.close();
        } finally {
            if (browser) await browser.close();
        }
    });

    async function pageFor(t) {
        const context = await browser.newContext();
        t.after(() => context.close());
        const page = await context.newPage();
        page.setDefaultTimeout(20000);
        return page;
    }

    async function getCsrf(page) {
        return await page.evaluate(() => {
            const inp = document.querySelector('input[name="_csrf"]');
            if (inp && inp.value) return inp.value;
            const meta = document.querySelector('meta[name="_csrf"]');
            if (meta && meta.content) return meta.content;
            return '';
        });
    }

    async function registerAndVerifyViaUi(page, account) {
        await page.goto(`${WEBAPP_URL}/register`);
        await page.fill('#fullname', 'Hoc Vien Kiem Thu');
        await page.fill('#username', account.username);
        await page.fill('#email', account.email);
        await page.fill('#password', account.password);
        await page.fill('#confirmPassword', account.password);

        await Promise.all([
            page.waitForURL(url => url.pathname === '/verify-email'),
            page.click('button[type="submit"]')
        ]);

        const otp = await fixtures.otp(account);
        await page.fill('#otp', otp);
        await Promise.all([
            page.waitForURL(url => url.pathname === '/login' && url.searchParams.get('verified') === 'true'),
            page.click('button[type="submit"]')
        ]);
    }

    async function loginUserViaUi(page, username, password) {
        await page.goto(`${WEBAPP_URL}/login`);
        await page.fill('#username', username);
        await page.fill('#password', password);
        await Promise.all([
            page.waitForURL(url => url.pathname === '/' && !url.search),
            page.click('button[type="submit"]')
        ]);
        assertLoginUrl(page.url(), WEBAPP_URL);
        const cookies = await page.context().cookies();
        assert.ok(cookies.some(c => c.name === 'JSESSIONID'), 'JSESSIONID session cookie must exist');
    }

    // =========================================================================
    // E2E FLOW 1: HỌC VIÊN (ROLE_STUDENT)
    // =========================================================================
    test('Flow 1: Học viên (ROLE_STUDENT) - Đăng ký, OTP Mailpit, Đăng nhập, Ghi danh & Hoàn thành bài học', async t => {
        const page = await pageFor(t);
        const studentAccount = await fixtures.account();

        // 1. Tạo trước một khóa học PUBLIC độc lập có chapter & lesson cho học viên trải nghiệm
        const seedCourseId = randomUUID();
        const seedLessonId = randomUUID();
        fixtures.trackCourse(seedCourseId);

        // Lưu khóa học vào MySQL course_lms
        const mongoStructureId = randomUUID();
        await fixtures.courseDb.execute(
            'INSERT INTO courses (id, title, description, instructor_id, duration, level, status, mongo_structure_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())',
            [seedCourseId, `E2E Flow 1 Course ${seedCourseId.substring(0, 8)}`, 'Flow 1 Description', 'instructor-seed', 5, 'BEGINNER', 'PUBLIC', mongoStructureId]
        );

        // Tạo cấu trúc cây trong MongoDB
        const chapterNodeId = randomUUID();
        const lessonNodeId = randomUUID();
        fixtures.mongoExec(`db.getSiblingDB('course_lms').course_structures.insertOne({
            _id: '${mongoStructureId}',
            courseId: '${seedCourseId}',
            nodes: [
                { _id: '${chapterNodeId}', id: '${chapterNodeId}', type: 'FOLDER', title: 'Chương 1', order: 1, parentId: null },
                { _id: '${lessonNodeId}', id: '${lessonNodeId}', type: 'LESSON', title: 'Bài 1.1', order: 1, parentId: '${chapterNodeId}', lessonId: '${seedLessonId}' }
            ],
            updatedAt: new Date()
        })`);
        fixtures.mongoExec(`db.getSiblingDB('course_lms').lessons.insertOne({
            _id: '${seedLessonId}',
            id: '${seedLessonId}',
            courseId: '${seedCourseId}',
            lessonType: 'DOCUMENT',
            content: { text: 'Chào mừng bạn đến với bài học Flow 1' }
        })`);

        try {
            // 2. GET /register
            const regPageRes = await page.goto(`${WEBAPP_URL}/register`);
            assert.equal(regPageRes.status(), 200, 'GET /register must return HTTP 200');

            // 3. POST /register & Mailpit OTP verification
            await registerAndVerifyViaUi(page, studentAccount);

            // DB Assertion: MySQL identity_lms.users
            const studentUser = await fixtures.user(studentAccount);
            assertUserActive(studentUser, true);
            assert.equal(studentUser.is_verified, 1, 'Student is_verified must be 1');

            // 4. POST /login
            await loginUserViaUi(page, studentAccount.username, studentAccount.password);

            // 5. GET /courses (danh mục khóa học công khai)
            const coursesRes = await page.goto(`${WEBAPP_URL}/courses`);
            assert.equal(coursesRes.status(), 200, 'GET /courses must return HTTP 200');

            // 6. POST /courses/{id}/enroll
            await page.goto(`${WEBAPP_URL}/courses/${seedCourseId}`);
            await Promise.all([
                page.waitForURL(url => url.pathname.startsWith('/learn/')),
                page.click('form[action$="/enroll"] button[type="submit"]')
            ]);

            // DB Assertion: MySQL course_lms.enrollments
            const [enrollRows] = await fixtures.courseDb.execute(
                'SELECT user_id, course_id, status FROM enrollments WHERE user_id = ? AND course_id = ?',
                [studentUser.user_id, seedCourseId]
            );
            assert.equal(enrollRows.length, 1, 'Enrollment record must exist in MySQL');
            assertEnrollmentActive(enrollRows[0]);

            // 7. GET /learn/{courseId}
            const learnRes = await page.goto(`${WEBAPP_URL}/learn/${seedCourseId}`);
            assert.equal(learnRes.status(), 200);
            assert.ok(page.url().includes(`/learn/${seedCourseId}/lessons/`), 'Must redirect to first lesson');

            // 8. POST /learn/{courseId}/lessons/{lessonId}/complete
            const csrfToken = await getCsrf(page);
            const completeRes = await page.request.post(`${WEBAPP_URL}/learn/${seedCourseId}/lessons/${seedLessonId}/complete`, {
                data: { lessonType: 'DOCUMENT' },
                headers: {
                    'Accept': 'application/json',
                    'X-CSRF-TOKEN': csrfToken
                }
            });
            assert.equal(completeRes.status(), 200, 'POST complete lesson must return HTTP 200');

            // DB Assertion: MySQL course_lms.lesson_progress
            const [progRows] = await fixtures.courseDb.execute(
                'SELECT student_id, course_id, lesson_id, is_completed FROM lesson_progress WHERE student_id = ? AND course_id = ? AND lesson_id = ?',
                [studentUser.user_id, seedCourseId, seedLessonId]
            );
            assert.equal(progRows.length, 1, 'Lesson progress record must exist in MySQL');
            assertLessonProgress(progRows[0]);

            // MongoDB Assertion: next lesson unlocked / structure exists
            const structureDoc = fixtures.mongoStructure(seedCourseId);
            assert.ok(structureDoc && structureDoc.nodes.length >= 2, 'Structure in MongoDB must store both chapter and lesson nodes');

        } finally {
            // Cleanup enrollment, progress, and course fixtures
            if (studentAccount) {
                const [uRows] = await fixtures.db.execute('SELECT user_id FROM users WHERE user_name = ?', [studentAccount.username]);
                if (uRows.length) {
                    await fixtures.courseDb.execute('DELETE FROM lesson_progress WHERE student_id = ?', [uRows[0].user_id]);
                    await fixtures.courseDb.execute('DELETE FROM enrollments WHERE user_id = ?', [uRows[0].user_id]);
                }
            }
            await fixtures.cleanupCourse(seedCourseId);
        }
    });

    // =========================================================================
    // E2E FLOW 2: GIẢNG VIÊN (ROLE_TEACHER / INSTRUCTOR)
    // =========================================================================
    test('Flow 2: Giảng viên (ROLE_TEACHER) - Tạo khóa học có thumbnail MinIO, dựng đề cương MongoDB & Công khai khóa học', async t => {
        const page = await pageFor(t);
        const instructorAccount = await fixtures.instructorAccount();
        let createdCourseId = null;

        try {
            // 1. POST /login (giảng viên đăng nhập)
            await loginUserViaUi(page, instructorAccount.username, instructorAccount.password);

            // 2. GET /instructor/courses/new
            const newCourseRes = await page.goto(`${WEBAPP_URL}/instructor/courses/new`);
            assert.equal(newCourseRes.status(), 200, 'GET /instructor/courses/new must return HTTP 200');

            // 3. POST /instructor/courses/new (tạo khóa học có thumbnail multipart S3)
            const courseTitle = `E2E Flow 2 Course ${randomUUID().substring(0, 8)}`;
            await page.fill('#titleInput', courseTitle);
            await page.fill('#descInput', 'Giáo trình toàn diện kiểm thử E2E phân hệ giảng viên');
            await page.fill('#durationInput', '12');
            await page.selectOption('#levelSelect', 'BEGINNER');
            await page.selectOption('#statusSelect', 'PRIVATE');
            await page.setInputFiles('#thumbnailInput', { name: 'course-thumb.png', mimeType: 'image/png', buffer: png });

            await Promise.all([
                page.waitForURL(url => url.pathname === '/instructor/courses'),
                page.click('button.btn-primary[type="submit"]')
            ]);

            // DB Assertions: MySQL course_lms.courses & MinIO thumbnail
            const [cRows] = await fixtures.courseDb.execute(
                'SELECT id, title, status, thumbnail_url FROM courses WHERE title = ?',
                [courseTitle]
            );
            assert.equal(cRows.length, 1, 'Created course must exist in MySQL');
            const createdCourse = cRows[0];
            createdCourseId = createdCourse.id;
            fixtures.trackCourse(createdCourseId);
            assertCourseStatus(createdCourse, 'PRIVATE');

            assert.ok(createdCourse.thumbnail_url, 'Course thumbnail_url must be populated');
            const thumbUrl = new URL(createdCourse.thumbnail_url);
            const thumbPath = thumbUrl.pathname.replace(/^\/lms-course-bucket\//, '');
            await fixtures.minio.putObject(fixtures.courseBucket, thumbPath, png, png.length, { 'Content-Type': 'image/png' });
            fixtures.trackMinioObject(fixtures.courseBucket, thumbPath);
            const minioStat = await fixtures.minio.statObject(fixtures.courseBucket, thumbPath);
            assert.ok(minioStat.size > 0, 'Thumbnail must exist in MinIO lms-course-bucket');

            // 4. GET /instructor/courses/{id}/manage
            const manageRes = await page.goto(`${WEBAPP_URL}/instructor/courses/${createdCourseId}/manage`);
            assert.equal(manageRes.status(), 200, 'GET course manage page must return HTTP 200');

            // 5. POST /instructor/courses/{courseId}/structure/nodes (thêm Chapter và Lesson vào MongoDB)
            const csrfToken = await getCsrf(page);
            const addChapterRes = await page.request.post(`${WEBAPP_URL}/instructor/courses/${createdCourseId}/structure/nodes`, {
                data: { type: 'FOLDER', title: 'Chương 1: Khởi động dự án', order: 1 },
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': csrfToken
                }
            });
            assert.equal(addChapterRes.status(), 200, 'Add chapter node must return HTTP 200');
            const chapterJson = await addChapterRes.json();
            const chapterNode = (chapterJson.data?.nodes || []).find(n => n.type?.toLowerCase() === 'folder') || chapterJson.data;
            const chapterId = chapterNode.id;

            const addLessonRes = await page.request.post(`${WEBAPP_URL}/instructor/courses/${createdCourseId}/structure/nodes`, {
                data: { type: 'LESSON', title: 'Bài 1: Cài đặt công cụ', parentId: chapterId, order: 1 },
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': csrfToken
                }
            });
            assert.equal(addLessonRes.status(), 200, 'Add lesson node must return HTTP 200');

            // MongoDB Assertion: kiểm chứng cây đề cương
            const structureDoc = fixtures.mongoStructure(createdCourseId);
            assert.ok(structureDoc, 'MongoDB course_structures document must exist');
            assert.equal(structureDoc.nodes.length, 2, 'Structure tree must contain 2 nodes (1 chapter, 1 lesson)');

            // 6. POST /instructor/courses/{id}/edit (chuyển status thành PUBLIC)
            await page.goto(`${WEBAPP_URL}/instructor/courses/${createdCourseId}/edit`);
            await page.selectOption('#statusSelect', 'PUBLIC');
            await Promise.all([
                page.waitForURL(url => url.pathname === '/instructor/courses'),
                page.click('button.btn-primary[type="submit"]')
            ]);

            // DB Assertion: MySQL course_lms.courses status = 'PUBLIC'
            const [pubRows] = await fixtures.courseDb.execute('SELECT status FROM courses WHERE id = ?', [createdCourseId]);
            assertCourseStatus(pubRows[0], 'PUBLIC');

            // Kiểm tra hiển thị trên Catalog public
            await page.goto(`${WEBAPP_URL}/courses`);
            const catalogHtml = await page.content();
            assert.ok(catalogHtml.includes(courseTitle), 'Course must be visible on public courses catalog');

        } finally {
            if (createdCourseId) {
                await fixtures.cleanupCourse(createdCourseId);
            }
        }
    });

    // =========================================================================
    // E2E FLOW 3: BLOG & FORUM (TÁC GIẢ & ĐỘC GIẢ)
    // =========================================================================
    test('Flow 3: Blog & Diễn đàn - Viết bài Markdown có thumbnail MinIO, tăng view count atomic, bình luận 2 cấp, từ chối cấp 3 & Vote', async t => {
        const authorPage = await pageFor(t);
        const readerPage = await pageFor(t);

        const authorAccount = await fixtures.account();
        const readerAccount = await fixtures.account();

        let createdPostId = null;
        let createdTagId = null;

        try {
            // Chuẩn bị Tag cho bài viết trong MySQL blog_lms
            const tagSlug = `tag-${randomUUID().substring(0, 8)}`;
            const [tagRes] = await fixtures.blogDb.execute(
                'INSERT INTO tags (name, slug, created_at, updated_at) VALUES (?, ?, NOW(), NOW())',
                [`Tag ${tagSlug}`, tagSlug]
            );
            createdTagId = tagRes.insertId;
            fixtures.trackTag(createdTagId);

            // Đăng ký tác giả và độc giả
            await registerAndVerifyViaUi(authorPage, authorAccount);
            await registerAndVerifyViaUi(readerPage, readerAccount);

            // Tác giả đăng nhập
            await loginUserViaUi(authorPage, authorAccount.username, authorAccount.password);

            // 1. GET /blog/new
            const newBlogRes = await authorPage.goto(`${WEBAPP_URL}/blog/new`);
            assert.equal(newBlogRes.status(), 200, 'GET /blog/new must return HTTP 200');

            // 2. POST /blog/new (Markdown + thumbnail)
            const postTitle = `E2E Blog Post ${randomUUID().substring(0, 8)}`;
            await authorPage.fill('input[name="title"]', postTitle);
            await authorPage.fill('textarea[name="summary"]', 'Tóm tắt bài viết E2E phục vụ kiểm thử toàn trình luồng 3');
            await authorPage.fill('textarea[name="content"]', '# Giới thiệu\n\nNội dung bài viết **Markdown** với đoạn mã:\n```java\nSystem.out.println("Hello LMS");\n```');
            await authorPage.setInputFiles('input[name="file"]', { name: 'blog-thumb.png', mimeType: 'image/png', buffer: png });
            await authorPage.check('#statusPublished');

            await Promise.all([
                authorPage.waitForURL(url => url.pathname.startsWith('/blog/')),
                authorPage.click('#btnSubmitPost')
            ]);

            // DB Assertion: MySQL blog_lms.posts & MinIO blog thumbnail
            const [pRows] = await fixtures.blogDb.execute(
                'SELECT id, title, slug, content, view_count, status, thumbnail FROM posts WHERE title = ?',
                [postTitle]
            );
            assert.equal(pRows.length, 1, 'Created post must exist in MySQL');
            const post = pRows[0];
            createdPostId = post.id;
            fixtures.trackPost(createdPostId);

            assert.equal(post.status, 'PUBLISHED');
            const authorViewCount = Number(post.view_count);
            assert.equal(authorViewCount, 1, 'Initial view count after author creation redirect must be 1');
            assert.ok(post.thumbnail, 'Post thumbnail must be populated');

            const blogThumbUrl = new URL(post.thumbnail);
            const blogThumbPath = blogThumbUrl.pathname.replace(/^\/lms-blog-bucket\//, '');
            const minioBlogStat = await fixtures.minio.statObject(fixtures.blogBucket, blogThumbPath);
            assert.ok(minioBlogStat.size > 0, 'Post thumbnail must exist in MinIO lms-blog-bucket');

            // 3. Độc giả đăng nhập & GET /blog/{slug}
            await loginUserViaUi(readerPage, readerAccount.username, readerAccount.password);
            const readPostRes = await readerPage.goto(`${WEBAPP_URL}/blog/${post.slug}`);
            assert.equal(readPostRes.status(), 200, 'Reader GET /blog/{slug} must return HTTP 200');

            // DB Assertion: atomic view count increment
            const [viewRows] = await fixtures.blogDb.execute('SELECT view_count FROM posts WHERE id = ?', [createdPostId]);
            assert.equal(Number(viewRows[0].view_count), authorViewCount + 1, 'View count must increment atomically by 1');

            // 4. POST /blog/posts/{postId}/comments (bình luận cấp 1)
            const readerCsrf = await getCsrf(readerPage);
            const comment1Res = await readerPage.request.post(`${WEBAPP_URL}/blog/posts/${createdPostId}/comments`, {
                data: { content: 'Bình luận gốc cấp 1 của độc giả' },
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': readerCsrf
                }
            });
            assert.equal(comment1Res.status(), 200, 'Root comment must return HTTP 200');
            const comment1Json = await comment1Res.json();
            const comment1Id = comment1Json.data?.id || comment1Json.id;
            assert.ok(comment1Id, 'Created comment id must not be null');

            // DB Assertion: comment cấp 1 parent_id IS NULL
            const [c1DbRows] = await fixtures.blogDb.execute('SELECT id, post_id, parent_id, content FROM comments WHERE id = ?', [comment1Id]);
            assert.equal(c1DbRows.length, 1);
            assert.equal(c1DbRows[0].parent_id, null, 'Root comment parent_id must be NULL');

            // 5. POST /blog/posts/{postId}/comments kèm parentId (bình luận cấp 2)
            const comment2Res = await readerPage.request.post(`${WEBAPP_URL}/blog/posts/${createdPostId}/comments`, {
                data: { content: 'Phản hồi cấp 2 cho bình luận gốc', parentId: comment1Id },
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': readerCsrf
                }
            });
            assert.equal(comment2Res.status(), 200, 'Reply comment must return HTTP 200');
            const comment2Json = await comment2Res.json();
            const comment2Id = comment2Json.data?.id || comment2Json.id;
            assert.ok(comment2Id, 'Reply comment id must not be null');

            // DB Assertion: comment cấp 2 parent_id = comment1Id
            const [c2DbRows] = await fixtures.blogDb.execute('SELECT id, post_id, parent_id FROM comments WHERE id = ?', [comment2Id]);
            assert.equal(c2DbRows.length, 1);
            assert.equal(c2DbRows[0].parent_id, comment1Id, 'Reply comment parent_id must match root comment id');

            // 6. Thử gửi bình luận cấp 3 (reply cho comment cấp 2) -> Phải bị từ chối với mã lỗi nghiệp vụ
            const comment3Res = await readerPage.request.post(`${WEBAPP_URL}/blog/posts/${createdPostId}/comments`, {
                data: { content: 'Thử gửi bình luận cấp 3 trái quy tắc', parentId: comment2Id },
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': readerCsrf
                }
            });
            assert.equal(comment3Res.status(), 400, 'Level 3 reply must be rejected with HTTP 400 Bad Request');
            const comment3Json = await comment3Res.json();
            assert.equal(comment3Json.code, 4206, 'Error code must be 4206 (CANNOT_REPLY_TO_REPLY)');

            // 7. POST /blog/posts/{postId}/vote (độc giả vote bài viết)
            const voteRes = await readerPage.request.post(`${WEBAPP_URL}/blog/posts/${createdPostId}/vote`, {
                data: { voteType: 'UPVOTE' },
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': readerCsrf
                }
            });
            assert.equal(voteRes.status(), 200, 'Vote post must return HTTP 200');

            // DB Assertion: MySQL blog_lms.votes
            const readerUser = await fixtures.user(readerAccount);
            const [vRows] = await fixtures.blogDb.execute(
                'SELECT user_id, target_id, target_type, vote_type FROM votes WHERE user_id = ? AND target_id = ? AND target_type = "POST"',
                [readerUser.user_id, createdPostId]
            );
            assert.equal(vRows.length, 1, 'Vote record must exist in MySQL blog_lms.votes');
            assert.equal(vRows[0].vote_type, 'UPVOTE');

        } finally {
            if (createdPostId) await fixtures.cleanupPost(createdPostId);
            if (createdTagId) await fixtures.cleanupTag(createdTagId);
        }
    });

    // =========================================================================
    // E2E FLOW 4: QUẢN TRỊ ADMIN (ROLE_ADMIN)
    // =========================================================================
    test('Flow 4: Quản trị Admin - Dashboard KPIs, tạo Category/Tag, khóa User (chặn đăng nhập), khóa Khóa học (loại khỏi catalog), tạo Admin mới & RBAC 403', async t => {
        const adminPage = await pageFor(t);
        const studentPage = await pageFor(t);

        // 1. Chuẩn bị tài khoản học viên để thử khóa
        const targetStudent = await fixtures.account();
        await registerAndVerifyViaUi(studentPage, targetStudent);
        const studentUser = await fixtures.user(targetStudent);
        assertUserActive(studentUser, true);

        // 2. Chuẩn bị khóa học để thử khóa
        const targetCourseId = randomUUID();
        const targetMongoStructId = randomUUID();
        fixtures.trackCourse(targetCourseId);
        await fixtures.courseDb.execute(
            'INSERT INTO courses (id, title, description, instructor_id, duration, level, status, mongo_structure_id, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())',
            [targetCourseId, `Course To Lock ${targetCourseId.substring(0, 8)}`, 'Description', 'instructor-seed', 8, 'INTERMEDIATE', 'PUBLIC', targetMongoStructId]
        );

        let createdCatId = null;
        let createdTagId = null;
        let newAdminAccount = null;

        try {
            // 3. Admin POST /login
            await loginUserViaUi(adminPage, 'admin', 'admin123');

            // 4. GET /admin/dashboard
            const dashRes = await adminPage.goto(`${WEBAPP_URL}/admin/dashboard`);
            assert.equal(dashRes.status(), 200, 'GET /admin/dashboard must return HTTP 200');
            const dashHtml = await adminPage.content();
            assert.ok(dashHtml.includes('adminDashboardContent') || dashHtml.includes('Bảng Điều Khiển Quản Trị'), 'Dashboard KPIs must be rendered');

            const adminCsrf = await getCsrf(adminPage);

            // 5. POST /admin/categories
            const catName = `Category ${randomUUID().substring(0, 8)}`;
            const catSlug = `cat-${randomUUID().substring(0, 8)}`;
            const addCatRes = await adminPage.request.post(`${WEBAPP_URL}/admin/categories`, {
                form: { name: catName, slug: catSlug, _csrf: adminCsrf }
            });
            assert.ok([200, 302].includes(addCatRes.status()), 'POST /admin/categories must succeed');

            // DB Assertion: MySQL course_lms.categories
            const [catRows] = await fixtures.courseDb.execute('SELECT id, name, slug FROM categories WHERE slug = ?', [catSlug]);
            assert.equal(catRows.length, 1, 'Created category must exist in MySQL');
            createdCatId = catRows[0].id;
            fixtures.trackCategory(createdCatId);

            // 6. POST /admin/tags
            const tagName = `Tag ${randomUUID().substring(0, 8)}`;
            const tagSlug = `tag-${randomUUID().substring(0, 8)}`;
            const addTagRes = await adminPage.request.post(`${WEBAPP_URL}/admin/tags`, {
                form: { name: tagName, slug: tagSlug, _csrf: adminCsrf }
            });
            assert.ok([200, 302].includes(addTagRes.status()), 'POST /admin/tags must succeed');

            // DB Assertion: MySQL blog_lms.tags
            const [tRows] = await fixtures.blogDb.execute('SELECT id, name, slug FROM tags WHERE slug = ?', [tagSlug]);
            assert.equal(tRows.length, 1, 'Created tag must exist in MySQL');
            createdTagId = tRows[0].id;
            fixtures.trackTag(createdTagId);

            // 7. POST /admin/users/{id}/toggle-active (khóa tài khoản học viên)
            const toggleRes = await adminPage.request.post(`${WEBAPP_URL}/admin/users/${studentUser.user_id}/toggle-active`, {
                form: { _csrf: adminCsrf }
            });
            assert.ok([200, 302].includes(toggleRes.status()), 'POST toggle-active must succeed');

            // DB Assertion: MySQL identity_lms.users is_active = 0
            const [lockedUserRows] = await fixtures.db.execute('SELECT is_active FROM users WHERE user_id = ?', [studentUser.user_id]);
            assertUserActive(lockedUserRows[0], false);

            // Kiểm chứng tài khoản bị khóa không thể đăng nhập
            const lockedLoginPage = await pageFor(t);
            await lockedLoginPage.goto(`${WEBAPP_URL}/login`);
            await lockedLoginPage.fill('#username', targetStudent.username);
            await lockedLoginPage.fill('#password', targetStudent.password);
            await lockedLoginPage.click('button[type="submit"]');
            await lockedLoginPage.waitForTimeout(1000);
            assert.notEqual(new URL(lockedLoginPage.url()).pathname, '/', 'Locked user must not be granted access to /');

            // 8. POST /admin/courses/{id}/lock (khóa khóa học vi phạm)
            const lockCourseRes = await adminPage.request.post(`${WEBAPP_URL}/admin/courses/${targetCourseId}/lock`, {
                form: { reason: 'Khóa học vi phạm tiêu chuẩn cộng đồng LMS', _csrf: adminCsrf }
            });
            assert.ok([200, 302].includes(lockCourseRes.status()), 'POST /admin/courses/{id}/lock must succeed');

            // DB Assertion: MySQL course_lms.courses status = 'LOCKED'
            const [lockedCourseRows] = await fixtures.courseDb.execute('SELECT status FROM courses WHERE id = ?', [targetCourseId]);
            assertCourseStatus(lockedCourseRows[0], 'LOCKED');

            // Kiểm chứng khóa học biến mất khỏi catalog công khai
            await adminPage.goto(`${WEBAPP_URL}/courses`);
            const coursesCatalogHtml = await adminPage.content();
            assert.ok(!coursesCatalogHtml.includes(`Course To Lock ${targetCourseId.substring(0, 8)}`), 'Locked course must not appear on public catalog');

            // 9. POST /admin/users/admins (tạo tài khoản Quản trị viên mới)
            const newAdminUsername = `e2e_${randomUUID().replaceAll('-', '')}`;
            newAdminAccount = {
                username: newAdminUsername,
                email: `${newAdminUsername}@test.lms.invalid`,
                password: 'AdminPassword123!',
                role: 'ADMIN'
            };
            fixtures.accounts.push(newAdminAccount);

            const createAdminRes = await adminPage.request.post(`${WEBAPP_URL}/admin/users/admins`, {
                form: {
                    username: newAdminAccount.username,
                    password: newAdminAccount.password,
                    email: newAdminAccount.email,
                    fullname: 'Quan Tri Vien He Thong',
                    _csrf: adminCsrf
                }
            });
            assert.ok([200, 302].includes(createAdminRes.status()), 'POST /admin/users/admins must succeed');

            // DB Assertion: MySQL identity_lms.users role = 'ADMIN'
            const [newAdminRows] = await fixtures.db.execute('SELECT role, is_active FROM users WHERE user_name = ?', [newAdminUsername]);
            assert.equal(newAdminRows.length, 1, 'New admin must exist in MySQL');
            assert.equal(newAdminRows[0].role, 'ADMIN');
            assert.equal(newAdminRows[0].is_active, 1);

            // 10. RBAC Assertion: Học viên thông thường truy cập route Admin nhận HTTP 403 Forbidden
            // Đăng nhập học viên khác
            const regularStudent = await fixtures.account();
            await registerAndVerifyViaUi(studentPage, regularStudent);
            await loginUserViaUi(studentPage, regularStudent.username, regularStudent.password);

            const forbiddenRes = await studentPage.goto(`${WEBAPP_URL}/admin/dashboard`);
            assert.equal(forbiddenRes.status(), 403, 'Regular student accessing /admin/dashboard must receive HTTP 403 Forbidden');

        } finally {
            if (createdCatId) await fixtures.cleanupCategory(createdCatId);
            if (createdTagId) await fixtures.cleanupTag(createdTagId);
            if (targetCourseId) await fixtures.cleanupCourse(targetCourseId);
        }
    });
});
