const { describe, it, beforeEach } = require('node:test');
const assert = require('node:assert');
const { initLearningHub } = require('../../main/resources/static/js/learning-hub.js');

// Mock DOM Helper
class MockClassList {
    constructor() {
        this.classes = new Set();
    }
    add(cls) { this.classes.add(cls); }
    remove(cls) { this.classes.delete(cls); }
    contains(cls) { return this.classes.has(cls); }
}

class MockElement {
    constructor(id = '', tagName = 'div') {
        this.id = id;
        this.tagName = tagName.toUpperCase();
        this.classList = new MockClassList();
        this.innerHTML = '';
        this.textContent = '';
        this.value = '';
        this.disabled = false;
        this.style = {};
        this.attributes = {};
        this.listeners = {};
    }

    setAttribute(name, val) { this.attributes[name] = String(val); }
    getAttribute(name) { return this.attributes[name] || null; }

    addEventListener(event, fn) {
        if (!this.listeners[event]) this.listeners[event] = [];
        this.listeners[event].push(fn);
    }

    async dispatchEvent(event, data) {
        if (this.listeners[event]) {
            for (const fn of this.listeners[event]) {
                await fn(data);
            }
        }
    }

    async click() {
        await this.dispatchEvent('click', {});
    }

    querySelector(selector) {
        return null;
    }
}

class MockDocument {
    constructor() {
        this.elements = new Map();
    }

    register(id, el) {
        this.elements.set(id, el);
        return el;
    }

    getElementById(id) {
        return this.elements.get(id) || null;
    }

    querySelector(selector) {
        if (selector === 'input[name="_csrf"]') {
            return this.elements.get('csrfInput') || null;
        }
        if (selector.startsWith('a[data-lesson-id=')) {
            return this.elements.get('activeLessonLink') || null;
        }
        return null;
    }
}

describe('Kiểm thử thực thi JavaScript learning-hub.js trong môi trường thực', () => {
    let doc;
    let win;
    let fetchCalls;

    beforeEach(() => {
        fetchCalls = [];
        doc = new MockDocument();

        // Cài đặt metadata DOM
        const courseIdInput = new MockElement('courseId', 'input');
        courseIdInput.value = 'c-101';
        doc.register('courseId', courseIdInput);

        const lessonIdInput = new MockElement('lessonId', 'input');
        lessonIdInput.value = 'l-202';
        doc.register('lessonId', lessonIdInput);

        const lessonTypeInput = new MockElement('lessonType', 'input');
        lessonTypeInput.value = 'VIDEO';
        doc.register('lessonType', lessonTypeInput);

        const csrfInput = new MockElement('csrfInput', 'input');
        csrfInput.value = 'mock-csrf-token-xyz';
        doc.register('csrfInput', csrfInput);

        // Nút và badge hoàn thành
        const completeBtn = new MockElement('completeLessonBtn', 'button');
        doc.register('completeLessonBtn', completeBtn);

        const completeBtnText = new MockElement('completeBtnText', 'span');
        completeBtnText.innerHTML = 'Đánh Dấu Hoàn Thành';
        doc.register('completeBtnText', completeBtnText);

        const completedBadge = new MockElement('completedBadge', 'span');
        completedBadge.classList.add('d-none');
        doc.register('completedBadge', completedBadge);

        const completeAlert = new MockElement('completeAlert', 'div');
        completeAlert.classList.add('d-none');
        doc.register('completeAlert', completeAlert);

        const completeAlertMsg = new MockElement('completeAlertMsg', 'span');
        doc.register('completeAlertMsg', completeAlertMsg);

        const completeSuccessAlert = new MockElement('completeSuccessAlert', 'div');
        completeSuccessAlert.classList.add('d-none');
        doc.register('completeSuccessAlert', completeSuccessAlert);

        // Thanh tiến độ
        const courseProgressBar = new MockElement('courseProgressBar', 'div');
        courseProgressBar.style.width = '25%';
        courseProgressBar.setAttribute('aria-valuenow', '25');
        doc.register('courseProgressBar', courseProgressBar);

        const progressPercentText = new MockElement('progressPercentText', 'strong');
        progressPercentText.textContent = '25';
        doc.register('progressPercentText', progressPercentText);

        const completedLessonsCount = new MockElement('completedLessonsCount', 'span');
        completedLessonsCount.textContent = '1';
        doc.register('completedLessonsCount', completedLessonsCount);

        // Active Lesson Link trong sidebar
        const checkContainer = new MockElement('checkContainer', 'span');
        const activeLessonLink = new MockElement('activeLessonLink', 'a');
        activeLessonLink.querySelector = (sel) => sel === '.lesson-check-icon' ? checkContainer : null;
        doc.register('activeLessonLink', activeLessonLink);

        // Schedules Modal Elements
        doc.register('schedulesModal', new MockElement('schedulesModal'));
        doc.register('schedulesLoading', new MockElement('schedulesLoading'));
        doc.register('schedulesError', new MockElement('schedulesError'));
        doc.register('schedulesErrorMsg', new MockElement('schedulesErrorMsg'));
        doc.register('schedulesEmpty', new MockElement('schedulesEmpty'));
        doc.register('schedulesContent', new MockElement('schedulesContent'));
        doc.register('schedulesRetryBtn', new MockElement('schedulesRetryBtn'));

        // Announcements Modal Elements
        doc.register('announcementsModal', new MockElement('announcementsModal'));
        doc.register('announcementsLoading', new MockElement('announcementsLoading'));
        doc.register('announcementsError', new MockElement('announcementsError'));
        doc.register('announcementsErrorMsg', new MockElement('announcementsErrorMsg'));
        doc.register('announcementsEmpty', new MockElement('announcementsEmpty'));
        doc.register('announcementsContent', new MockElement('announcementsContent'));
        doc.register('announcementsRetryBtn', new MockElement('announcementsRetryBtn'));
        doc.register('announcementsPagination', new MockElement('announcementsPagination'));
        doc.register('announcementsPageInfo', new MockElement('announcementsPageInfo'));
        doc.register('announcementsPrevBtn', new MockElement('announcementsPrevBtn'));
        doc.register('announcementsNextBtn', new MockElement('announcementsNextBtn'));

        win = {
            fetch: async (url, options) => {
                fetchCalls.push({ url, options });
                return {
                    ok: true,
                    status: 200,
                    json: async () => ({})
                };
            }
        };
    });

    it('Complete Lesson thành công: Gửi đúng payload, cập nhật tiến độ backend, hiển thị badge và checkmark', async () => {
        // Cấu hình mock fetch trả về 200 kèm tiến độ cập nhật 75% và 3 bài hoàn thành
        win.fetch = async (url, options) => {
            fetchCalls.push({ url, options });
            return {
                ok: true,
                status: 200,
                json: async () => ({
                    code: 1000,
                    message: 'Đã hoàn thành',
                    data: {
                        percentComplete: 75.0,
                        completedLessons: 3,
                        totalLessons: 4
                    }
                })
            };
        };

        initLearningHub(doc, win);

        const completeBtn = doc.getElementById('completeLessonBtn');
        await completeBtn.click();

        // 1. Kiểm tra gọi fetch đúng endpoint, method POST, header CSRF và payload lessonType VIDEO
        assert.strictEqual(fetchCalls.length, 1);
        assert.strictEqual(fetchCalls[0].url, '/learn/c-101/lessons/l-202/complete');
        assert.strictEqual(fetchCalls[0].options.method, 'POST');
        assert.strictEqual(fetchCalls[0].options.headers['X-CSRF-TOKEN'], 'mock-csrf-token-xyz');
        assert.deepStrictEqual(JSON.parse(fetchCalls[0].options.body), { lessonType: 'VIDEO' });

        // 2. Xác nhận cập nhật DOM
        assert.strictEqual(completeBtn.classList.contains('d-none'), true);
        assert.strictEqual(doc.getElementById('completedBadge').classList.contains('d-none'), false);
        assert.strictEqual(doc.getElementById('completeSuccessAlert').classList.contains('d-none'), false);

        // 3. Xác nhận cập nhật tiến độ học tập từ dữ liệu backend
        assert.strictEqual(doc.getElementById('courseProgressBar').style.width, '75%');
        assert.strictEqual(doc.getElementById('courseProgressBar').getAttribute('aria-valuenow'), '75');
        assert.strictEqual(doc.getElementById('progressPercentText').textContent, 75);
        assert.strictEqual(doc.getElementById('completedLessonsCount').textContent, 3);
    });

    it('Complete Lesson thất bại (lỗi 400 hoặc server): Bật lại button, hiển thị lỗi, KHÔNG tăng tiến độ', async () => {
        win.fetch = async (url, options) => {
            fetchCalls.push({ url, options });
            return {
                ok: false,
                status: 400,
                json: async () => ({
                    code: 400,
                    message: 'Lỗi tiến độ từ máy chủ'
                })
            };
        };

        initLearningHub(doc, win);

        const completeBtn = doc.getElementById('completeLessonBtn');
        await completeBtn.click();

        // Nút không bị ẩn, button được kích hoạt lại (disabled = false)
        assert.strictEqual(completeBtn.classList.contains('d-none'), false);
        assert.strictEqual(completeBtn.disabled, false);

        // Alert lỗi hiển thị thông điệp từ server
        assert.strictEqual(doc.getElementById('completeAlert').classList.contains('d-none'), false);
        assert.strictEqual(doc.getElementById('completeAlertMsg').textContent, 'Lỗi tiến độ từ máy chủ');

        // Tiến độ giữ nguyên giá trị ban đầu (25%), không bị tăng
        assert.strictEqual(doc.getElementById('courseProgressBar').style.width, '25%');
        assert.strictEqual(doc.getElementById('progressPercentText').textContent, '25');
        assert.strictEqual(doc.getElementById('completedLessonsCount').textContent, '1');
    });

    it('Modal Lịch học: Xử lý 4 trạng thái Loading -> Success render dữ liệu / Empty / Error', async () => {
        // Test trường hợp Success có dữ liệu
        win.fetch = async (url) => {
            fetchCalls.push({ url });
            return {
                ok: true,
                status: 200,
                json: async () => ({
                    data: [
                        { title: 'Buổi 1: Khai giảng', note: 'Tham gia đúng giờ', meetingUrl: 'https://meet.google.com/abc' }
                    ]
                })
            };
        };

        const hub = initLearningHub(doc, win);
        await hub.loadSchedules();

        assert.strictEqual(fetchCalls[0].url, '/courses/c-101/schedules');
        assert.strictEqual(doc.getElementById('schedulesLoading').classList.contains('d-none'), true);
        assert.strictEqual(doc.getElementById('schedulesContent').classList.contains('d-none'), false);
        assert.match(doc.getElementById('schedulesContent').innerHTML, /Buổi 1: Khai giảng/);
        assert.match(doc.getElementById('schedulesContent').innerHTML, /Tham gia phòng học/);

        // Test trường hợp Empty
        win.fetch = async () => ({
            ok: true,
            status: 200,
            json: async () => ({ data: [] })
        });
        await hub.loadSchedules();
        assert.strictEqual(doc.getElementById('schedulesEmpty').classList.contains('d-none'), false);

        // Test trường hợp Error
        win.fetch = async () => ({
            ok: false,
            status: 500,
            json: async () => ({})
        });
        await hub.loadSchedules();
        assert.strictEqual(doc.getElementById('schedulesError').classList.contains('d-none'), false);
    });

    it('Modal Thông báo: Xử lý phân trang, nút Next/Prev và hiển thị danh sách', async () => {
        win.fetch = async (url) => {
            fetchCalls.push({ url });
            return {
                ok: true,
                status: 200,
                json: async () => ({
                    data: {
                        content: [
                            { title: 'Thông báo nộp bài tập', content: 'Hạn chót ngày mai', createdAt: '2026-10-25' }
                        ],
                        totalPages: 3,
                        number: 0
                    }
                })
            };
        };

        const hub = initLearningHub(doc, win);
        await hub.loadAnnouncements(0);

        // Kiểm tra gọi endpoint kèm tham số page=0 và size=10
        assert.match(fetchCalls[0].url, /\/courses\/c-101\/announcements\?page=0&size=10/);
        assert.strictEqual(doc.getElementById('announcementsLoading').classList.contains('d-none'), true);
        assert.strictEqual(doc.getElementById('announcementsContent').classList.contains('d-none'), false);
        assert.match(doc.getElementById('announcementsContent').innerHTML, /Thông báo nộp bài tập/);

        // Kiểm tra phân trang
        assert.strictEqual(doc.getElementById('announcementsPagination').classList.contains('d-none'), false);
        assert.strictEqual(doc.getElementById('announcementsPageInfo').textContent, 'Trang 1 / 3');
        assert.strictEqual(doc.getElementById('announcementsPrevBtn').disabled, true);
        assert.strictEqual(doc.getElementById('announcementsNextBtn').disabled, false);
    });
});
