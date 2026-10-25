const { test, beforeEach, afterEach } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { JSDOM } = require('jsdom');
const { initCurriculumBuilder } = require('../../main/resources/static/js/curriculum-builder.js');

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
        return response({ data: { nodes: [] } });
    };
    win.eval(bootstrap);
    win.scrollTo = () => {};
    win.prompt = () => 'Tiêu đề cập nhật';
    win.confirm = () => true;
    win.alert = () => {};
});

afterEach(() => {
    if (dom && dom.window) dom.window.close();
});

test('curriculum builder loads directly from /structure endpoint and renders empty state when nodes empty', async () => {
    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        return response({ data: { nodes: [] } });
    };

    initCurriculumBuilder(doc, win);
    await until(() => visible('curriculumEmptyState'));

    assert.equal(calls.length, 1);
    assert.equal(calls[0].url, '/instructor/courses/c-200/structure');
    assert.equal(visible('curriculumLoading'), false);
    assert.equal(visible('curriculumErrorState'), false);
    assert.equal(visible('curriculumEmptyState'), true);
});

test('curriculum builder displays error state on API failure and retries on button click', async () => {
    let fail = true;
    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        if (fail) return response({ message: 'Forbidden' }, false, 403);
        return response({ data: { nodes: [{ id: 'n1', type: 'folder', title: 'Chương 1', order: 1 }] } });
    };

    initCurriculumBuilder(doc, win);
    await until(() => visible('curriculumErrorState'));

    assert.equal(visible('curriculumLoading'), false);
    assert.equal(visible('curriculumEmptyState'), false);
    assert.equal(visible('curriculumErrorState'), true);
    assert.match(el('curriculumErrorMessage').textContent, /Mã phản hồi HTTP: 403/);

    // Click retry button
    fail = false;
    el('curriculumRetryBtn').click();
    await until(() => doc.querySelector('[data-node-id="n1"]'));

    assert.equal(visible('curriculumErrorState'), false);
    assert.ok(doc.querySelector('[data-node-id="n1"]'));
});

test('renders hierarchical tree with lowercase NodeType and orders siblings correctly', async () => {
    const nodes = [
        { id: 'c2', type: 'folder', title: 'Chương 2', order: 2, parentId: null },
        { id: 'c1', type: 'folder', title: 'Chương 1', order: 1, parentId: null },
        { id: 'l1', type: 'lesson', title: 'Bài 1.1', order: 1, parentId: 'c1', lessonType: 'video', lessonId: 'les-101' },
        { id: 'l2', type: 'lesson', title: 'Bài 1.2', order: 2, parentId: 'c1', lessonType: 'document', lessonId: 'les-102' }
    ];

    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        return response({ data: { nodes } });
    };

    initCurriculumBuilder(doc, win);
    await until(() => doc.querySelector('[data-node-id="c1"]'));

    const renderedChapters = Array.from(doc.querySelectorAll('#curriculumTreeList > [data-node-id]'));
    assert.equal(renderedChapters.length, 2);
    assert.equal(renderedChapters[0].getAttribute('data-node-id'), 'c1');
    assert.equal(renderedChapters[1].getAttribute('data-node-id'), 'c2');

    // Child lessons inside c1
    const childLessons = Array.from(renderedChapters[0].querySelectorAll('.child-lessons-container [data-node-id]'));
    assert.equal(childLessons.length, 2);
    assert.equal(childLessons[0].getAttribute('data-node-id'), 'l1');
    assert.equal(childLessons[1].getAttribute('data-node-id'), 'l2');
    assert.equal(childLessons[0].getAttribute('data-lesson-id'), 'les-101');
});

test('reorder Move Down sends PUT /structure/reorder with ReorderNodesRequest', async () => {
    const nodes = [
        { id: 'c1', type: 'folder', title: 'Chương 1', order: 1, parentId: null },
        { id: 'c2', type: 'folder', title: 'Chương 2', order: 2, parentId: null }
    ];

    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        if (options.method === 'PUT') {
            return response({ code: 1000 });
        }
        return response({ data: { nodes } });
    };

    initCurriculumBuilder(doc, win);
    await until(() => doc.querySelector('[data-node-id="c1"] .btn-move-down'));

    doc.querySelector('[data-node-id="c1"] .btn-move-down').click();
    await until(() => calls.some(c => c.options.method === 'PUT'));

    const putCall = calls.find(c => c.options.method === 'PUT');
    assert.equal(putCall.url, '/instructor/courses/c-200/structure/reorder');
    const body = JSON.parse(putCall.options.body);
    assert.deepEqual(body, {
        nodes: [
            { nodeId: 'c2', parentId: null, order: 1 },
            { nodeId: 'c1', parentId: null, order: 2 }
        ]
    });
});

test('reorder failure triggers rollback and shows danger alert', async () => {
    const nodes = [
        { id: 'c1', type: 'folder', title: 'Chương 1', order: 1, parentId: null },
        { id: 'c2', type: 'folder', title: 'Chương 2', order: 2, parentId: null }
    ];

    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        if (options.method === 'PUT') {
            return response({ message: 'Lỗi thứ tự không hợp lệ' }, false, 400);
        }
        return response({ data: { nodes } });
    };

    initCurriculumBuilder(doc, win);
    await until(() => doc.querySelector('[data-node-id="c1"] .btn-move-down'));

    doc.querySelector('[data-node-id="c1"] .btn-move-down').click();
    await until(() => el('alertMessageContainer')?.querySelector('.alert-danger'));

    const alert = el('alertMessageContainer').querySelector('.alert-danger');
    assert.match(alert.textContent, /Không thể sắp xếp lại/);
});

test('add chapter failure 403 does not close modal and retains input', async () => {
    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        if (options.method === 'POST') {
            return response({ code: 403, message: 'Bạn không có quyền giảng viên' }, false, 403);
        }
        return response({ data: { nodes: [] } });
    };

    initCurriculumBuilder(doc, win);
    await until(() => visible('curriculumEmptyState'));

    el('btnAddChapter').click();
    el('nodeTitleInput').value = 'Chương Mới Thử Nghiệm';
    el('btnSaveNode').click();

    await until(() => el('alertMessageContainer')?.querySelector('.alert-danger'));

    assert.equal(el('nodeTitleInput').value, 'Chương Mới Thử Nghiệm');
    const alert = el('alertMessageContainer').querySelector('.alert-danger');
    assert.match(alert.textContent, /Bạn không có quyền giảng viên/);
    assert.equal(el('alertMessageContainer').querySelector('.alert-success'), null);
});

test('curriculum node title is rendered safely without XSS execution', async () => {
    const maliciousTitle = '<img src=x onerror=alert(1)>';
    const nodes = [
        { id: 'c-xss', type: 'folder', title: maliciousTitle, order: 1, parentId: null }
    ];

    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        return response({ data: { nodes } });
    };

    initCurriculumBuilder(doc, win);
    await until(() => doc.querySelector('[data-node-id="c-xss"]'));

    const nodeEl = doc.querySelector('[data-node-id="c-xss"]');
    assert.equal(nodeEl.querySelector('img'), null);
    assert.equal(nodeEl.querySelector('.fw-bold').textContent, maliciousTitle);
});
