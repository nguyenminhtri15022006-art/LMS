const { test, beforeEach, afterEach } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { JSDOM } = require('jsdom');
const { initInstructorManagement } = require('../../main/resources/static/js/instructor-management.js');

const fixtureDir = path.resolve(__dirname, '../../../target/ui-fixtures');
assert.ok(fs.existsSync(path.join(fixtureDir, 'management-center.html')),
    'Run mvn test -pl web-app -f backend/pom.xml first to render the Thymeleaf fixture.');
const html = fs.readFileSync(path.join(fixtureDir, 'management-center.html'), 'utf8');
const bootstrap = fs.readFileSync(path.join(fixtureDir, 'bootstrap.bundle.min.js'), 'utf8');

let dom, doc, win, calls;
const response = (data, ok = true, status = 200) => ({
    ok,
    status,
    json: async () => data
});

const el = id => doc.getElementById(id);
const visible = id => el(id) && !el(id).classList.contains('d-none');

async function until(check, timeoutMs = 800) {
    const start = Date.now();
    while (Date.now() - start < timeoutMs) {
        if (check()) return;
        await new Promise(resolve => setTimeout(resolve, 15));
    }
    assert.ok(check(), 'Expected DOM condition timed out');
}

beforeEach(async () => {
    dom = new JSDOM(html, { url: 'https://lms.test/instructor/courses/c-200/manage', runScripts: 'outside-only' });
    win = dom.window;
    doc = win.document;
    calls = [];
    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        return response({ data: [] });
    };
    win.eval(bootstrap);
    win.scrollTo = () => {};
    win.confirm = () => true;
    win.alert = () => {};
});

afterEach(() => {
    if (dom && dom.window) dom.window.close();
});

test('students tab loads and handles pagination Next and Previous clicks', async () => {
    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        const parsed = new URL(url, 'https://lms.test');
        const page = Number(parsed.searchParams.get('page') || 0);
        return response({
            data: {
                content: [
                    { userId: `u-${page}`, username: `student_${page}`, fullname: `Nguyễn Văn ${page}`, email: `st${page}@test.com`, enrolledAt: '2026-10-25T10:00:00Z' }
                ],
                totalPages: 2,
                totalElements: 12,
                number: page
            }
        });
    };

    initInstructorManagement(doc, win);
    el('students-tab').dispatchEvent(new win.Event('shown.bs.tab'));

    await until(() => el('studentsPageInfo')?.textContent.includes('Trang 1 / 2'));

    assert.equal(el('studentsPrevBtn').disabled, true);
    assert.equal(el('studentsNextBtn').disabled, false);

    // Click Next
    el('studentsNextBtn').click();
    await until(() => el('studentsPageInfo')?.textContent.includes('Trang 2 / 2'));

    assert.equal(el('studentsNextBtn').disabled, true);
    assert.equal(el('studentsPrevBtn').disabled, false);

    // Click Previous
    el('studentsPrevBtn').click();
    await until(() => el('studentsPageInfo')?.textContent.includes('Trang 1 / 2'));

    assert.deepEqual(calls.map(c => c.url), [
        '/instructor/courses/c-200/students?page=0&size=10',
        '/instructor/courses/c-200/students?page=1&size=10',
        '/instructor/courses/c-200/students?page=0&size=10'
    ]);
});

test('delete student calls DELETE /instructor/courses/{id}/students/{userId}', async () => {
    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        if (options.method === 'DELETE') {
            return response({ code: 1000 });
        }
        return response({
            data: {
                content: [{ userId: 'u-99', username: 'student_99', email: 'st99@test.com' }],
                totalPages: 1,
                totalElements: 1
            }
        });
    };

    initInstructorManagement(doc, win);
    el('students-tab').dispatchEvent(new win.Event('shown.bs.tab'));

    await until(() => doc.querySelector('.btn-remove-student'));
    doc.querySelector('.btn-remove-student').click();

    await until(() => calls.some(c => c.options.method === 'DELETE'));
    const delCall = calls.find(c => c.options.method === 'DELETE');
    assert.equal(delCall.url, '/instructor/courses/c-200/students/u-99');
});

test('invite student classifies successEmails, alreadyEnrolledEmails, and notFoundEmails', async () => {
    let mode = 'success';
    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        if (options.method === 'POST') {
            if (mode === 'success') {
                return response({ data: { successEmails: ['sv.new@test.com'], alreadyEnrolledEmails: [], notFoundEmails: [] } });
            } else if (mode === 'already') {
                return response({ data: { successEmails: [], alreadyEnrolledEmails: ['sv.exist@test.com'], notFoundEmails: [] } });
            } else if (mode === 'notFound') {
                return response({ data: { successEmails: [], alreadyEnrolledEmails: [], notFoundEmails: ['sv.ghost@test.com'] } });
            }
        }
        return response({ data: { content: [], totalPages: 1, totalElements: 0 } });
    };

    initInstructorManagement(doc, win);

    // 1. Success case
    mode = 'success';
    el('inviteEmailInput').value = 'sv.new@test.com';
    el('btnSendInvite').click();
    await until(() => el('alertMessageContainer')?.querySelector('.alert-success'));
    assert.match(el('alertMessageContainer').textContent, /Đã ghi danh tài khoản thành công: sv.new@test.com/);
    assert.equal(el('inviteEmailInput').value, '');

    // 2. Already enrolled case
    mode = 'already';
    el('inviteEmailInput').value = 'sv.exist@test.com';
    el('btnSendInvite').click();
    await until(() => el('alertMessageContainer')?.querySelector('.alert-warning'));
    assert.match(el('alertMessageContainer').textContent, /đã tham gia khóa học/);
    assert.equal(el('inviteEmailInput').value, 'sv.exist@test.com'); // Giữ nguyên form

    // 3. Not found case
    mode = 'notFound';
    el('inviteEmailInput').value = 'sv.ghost@test.com';
    el('btnSendInvite').click();
    await until(() => el('alertMessageContainer')?.querySelector('.alert-danger'));
    assert.match(el('alertMessageContainer').textContent, /Không tìm thấy tài khoản/);
    assert.equal(el('inviteEmailInput').value, 'sv.ghost@test.com'); // Giữ nguyên form
});

test('schedules tab supports edit schedule with PUT', async () => {
    const schedules = [
        { id: 10, title: 'Buổi ôn tập', startTime: '2026-11-01T09:00:00Z', endTime: '2026-11-01T11:00:00Z', note: 'Ôn tập chương 1' }
    ];

    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        if (options.method === 'PUT') {
            return response({ code: 1000 });
        }
        return response({ data: schedules });
    };

    initInstructorManagement(doc, win);
    el('schedules-tab').dispatchEvent(new win.Event('shown.bs.tab'));

    await until(() => doc.querySelector('.btn-edit-schedule'));

    // Click Sửa lịch học
    doc.querySelector('.btn-edit-schedule').click();
    assert.equal(el('scheduleIdInput').value, '10');
    assert.equal(el('scheduleTitleInput').value, 'Buổi ôn tập');
    assert.equal(el('scheduleModalTitle').textContent, 'Chỉnh Sửa Lịch Học Trực Tuyến');

    // Chỉnh sửa tiêu đề và bấm lưu
    el('scheduleTitleInput').value = 'Buổi ôn tập nâng cao';
    el('btnSaveSchedule').click();

    await until(() => calls.some(c => c.options.method === 'PUT'));
    const putCall = calls.find(c => c.options.method === 'PUT');
    assert.equal(putCall.url, '/instructor/courses/c-200/schedules/10');
    const body = JSON.parse(putCall.options.body);
    assert.equal(body.title, 'Buổi ôn tập nâng cao');
});

test('disarms javascript: in meetingUrl into plain text without clickable link', async () => {
    const schedules = [
        { id: 1, title: 'Lịch học độc hại', startTime: '2026-11-01T09:00:00Z', endTime: '2026-11-01T10:00:00Z', meetingUrl: 'javascript:alert(1)' },
        { id: 2, title: 'Lịch học an toàn', startTime: '2026-11-01T14:00:00Z', endTime: '2026-11-01T15:00:00Z', meetingUrl: 'https://meet.google.com/abc-xyz' }
    ];

    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        return response({ data: schedules });
    };

    initInstructorManagement(doc, win);
    el('schedules-tab').dispatchEvent(new win.Event('shown.bs.tab'));

    await until(() => doc.querySelectorAll('#schedulesListContainer .card').length === 2);

    const cards = Array.from(doc.querySelectorAll('#schedulesListContainer .card'));

    // Card 1: URL độc hại javascript:alert(1) KHÔNG được tạo thẻ <a>
    const maliciousCard = cards[0];
    assert.equal(maliciousCard.querySelector('a'), null);
    assert.match(maliciousCard.textContent, /javascript:alert\(1\)/);

    // Card 2: URL an toàn https://meet.google.com/abc-xyz ĐƯỢC tạo thẻ <a>
    const safeCard = cards[1];
    const safeLink = safeCard.querySelector('a');
    assert.ok(safeLink);
    assert.equal(safeLink.href, 'https://meet.google.com/abc-xyz');
});

test('announcements tab renders without executing HTML and supports posting', async () => {
    const maliciousPayload = '<img src=x onerror=alert(1)>';
    const announcements = [
        { id: 5, title: maliciousPayload, content: maliciousPayload, createdAt: '2026-10-25T12:00:00Z' }
    ];

    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        if (options.method === 'POST') {
            return response({ code: 1000 });
        }
        return response({
            data: {
                content: announcements,
                totalPages: 1,
                totalElements: 1
            }
        });
    };

    initInstructorManagement(doc, win);
    el('announcements-tab').dispatchEvent(new win.Event('shown.bs.tab'));

    await until(() => doc.querySelector('.btn-delete-announcement'));

    const listContainer = el('announcementsListContainer');
    assert.equal(listContainer.querySelector('img'), null);
    assert.match(listContainer.textContent, /<img src=x onerror=alert\(1\)>/);

    // Post announcement
    el('announcementTitleInput').value = 'Thông báo tuần mới';
    el('announcementContentInput').value = 'Nội dung thông báo tuần mới';
    el('announcementForm').dispatchEvent(new win.Event('submit', { cancelable: true }));

    await until(() => calls.some(c => c.options.method === 'POST'));
    const postCall = calls.find(c => c.options.method === 'POST');
    assert.equal(postCall.url, '/instructor/courses/c-200/announcements');
    const body = JSON.parse(postCall.options.body);
    assert.equal(body.title, 'Thông báo tuần mới');
    assert.equal(body.content, 'Nội dung thông báo tuần mới');
});
