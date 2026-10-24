const { test, beforeEach, afterEach } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { JSDOM } = require('jsdom');
const { initLearningHub } = require('../../main/resources/static/js/learning-hub.js');

const fixtureDir = path.resolve(__dirname, '../../../target/ui-fixtures');
assert.ok(fs.existsSync(path.join(fixtureDir, 'learning-hub.html')),
    'Run mvn test -pl web-app -f backend/pom.xml first to render the Thymeleaf fixture.');
const html = fs.readFileSync(path.join(fixtureDir, 'learning-hub.html'), 'utf8');
const bootstrap = fs.readFileSync(path.join(fixtureDir, 'bootstrap.bundle.min.js'), 'utf8');
let dom, doc, win, hub, calls;
const response = (data, ok = true) => ({ ok, status: ok ? 200 : 400, json: async () => data });
const el = id => doc.getElementById(id);
const visible = id => !el(id).classList.contains('d-none');
async function until(check) {
    for (let i = 0; i < 50; i++) {
        if (check()) return;
        await new Promise(resolve => setTimeout(resolve, 10));
    }
    assert.ok(check(), 'Expected DOM update after event');
}

beforeEach(async () => {
    dom = new JSDOM(html, { url: 'https://lms.test/learn/c-101/lessons/l-deep', runScripts: 'outside-only' });
    win = dom.window;
    doc = win.document;
    if (doc.readyState === 'loading') await new Promise(resolve => doc.addEventListener('DOMContentLoaded', resolve, { once: true }));
    calls = [];
    win.fetch = async (url, options) => { calls.push({ url, options }); return response({ data: [] }); };
    win.eval(bootstrap);
    hub = initLearningHub(doc, win);
});
afterEach(() => dom.window.close());

test('complete click sends VIDEO and CSRF, then updates actual rendered progress and deep lesson checkmark', async () => {
    win.fetch = async (url, options) => {
        calls.push({ url, options });
        return response({ data: { percentComplete: 75, completedLessons: 3 } });
    };
    el('completeLessonBtn').click();
    await until(() => visible('completedBadge'));
    assert.equal(calls[0].url, '/learn/c-101/lessons/l-deep/complete');
    assert.equal(calls[0].options.method, 'POST');
    assert.equal(calls[0].options.headers['X-CSRF-TOKEN'], doc.querySelector('input[name="_csrf"]').value);
    assert.deepEqual(JSON.parse(calls[0].options.body), { lessonType: 'VIDEO' });
    assert.equal(el('courseProgressBar').style.width, '75%');
    assert.equal(el('progressPercentText').textContent, '75');
    assert.equal(el('completedLessonsCount').textContent, '3');
    assert.ok(doc.querySelector('a[data-lesson-id="l-deep"] .bi-check-circle-fill'));
});

test('complete failure preserves progress and shows server error as text', async () => {
    win.fetch = async () => response({ message: '<img src=x onerror=alert(1)>' }, false);
    el('completeLessonBtn').click();
    await until(() => visible('completeAlert'));
    assert.equal(el('completeLessonBtn').disabled, false);
    assert.equal(el('courseProgressBar').style.width, '25%');
    assert.equal(el('completedLessonsCount').textContent, '1');
    assert.equal(el('completeAlertMsg').querySelector('img'), null);
    assert.equal(el('completeAlertMsg').textContent, '<img src=x onerror=alert(1)>');
});

test('real Bootstrap trigger opens rendered schedules modal and loads the API', async () => {
    win.fetch = async url => { calls.push({ url }); return response({ data: [{ title: 'Class one', meetingUrl: 'https://meet.google.com/abc' }] }); };
    doc.querySelector('[data-bs-target="#schedulesModal"]').click();
    await until(() => el('schedulesModal')?.classList.contains('show') && calls.length === 1);
    assert.equal(calls[0].url, '/courses/c-101/schedules');
    assert.match(el('schedulesContent').textContent, /Class one/);
    assert.equal(el('schedulesContent').querySelector('a').href, 'https://meet.google.com/abc');
});

test('actual announcements modal Next and Previous clicks fetch the requested page', async () => {
    win.fetch = async url => {
        calls.push({ url });
        const page = Number(new URL(url, 'https://lms.test').searchParams.get('page'));
        return response({ data: { content: [{ title: `Page ${page}`, content: 'Message' }], totalPages: 2 } });
    };
    doc.querySelector('[data-bs-target="#announcementsModal"]').click();
    await until(() => el('announcementsPageInfo')?.textContent === 'Trang 1 / 2');
    assert.equal(el('announcementsPrevBtn').disabled, true);
    el('announcementsNextBtn').click();
    await until(() => el('announcementsPageInfo').textContent === 'Trang 2 / 2');
    assert.equal(el('announcementsNextBtn').disabled, true);
    el('announcementsPrevBtn').click();
    await until(() => el('announcementsPageInfo').textContent === 'Trang 1 / 2');
    assert.deepEqual(calls.map(c => c.url), [0, 1, 0].map(page => `/courses/c-101/announcements?page=${page}&size=10`));
});

test('announcement API text never becomes executable markup', async () => {
    const payload = '<img src=x onerror="alert(1)">';
    win.fetch = async () => response({ data: { content: [{ title: payload, content: payload, createdAt: payload }], totalPages: 1 } });
    await hub.loadAnnouncements(0);
    assert.ok(el('announcementsContent'), 'Modal must exist in the actual rendered page');
    assert.equal(el('announcementsContent').querySelector('img,script,iframe,[onerror]'), null);
    assert.equal(el('announcementsContent').textContent.split(payload).length - 1, 3);
});

test('schedule text is literal and meeting URL cannot inject attributes', async () => {
    const payload = '<svg onload="alert(1)">';
    win.fetch = async () => response({ data: [{ title: payload, note: payload, meetingUrl: 'https://meet.google.com/abc" onclick="alert(1)' }] });
    await hub.loadSchedules();
    assert.ok(el('schedulesContent'), 'Modal must exist in the actual rendered page');
    assert.equal(el('schedulesContent').querySelector('svg,[onload],[onclick]'), null);
    assert.equal(el('schedulesContent').textContent.split(payload).length - 1, 2);
});

test('meeting links reject executable, relative, credentialed and malformed URLs', async () => {
    for (const url of ['javascript:alert(1)', 'data:text/html,test', '//evil.example', '/internal', 'https://user@evil.example', 'https://meet.google.com\\@evil.example', 'https://meet.google.com/\nabc']) {
        win.fetch = async () => response({ data: [{ title: 'Class', meetingUrl: url }] });
        await hub.loadSchedules();
        assert.ok(el('schedulesContent'), 'Modal must exist in the actual rendered page');
        assert.equal(el('schedulesContent').querySelector('a'), null, url);
    }
});

test('schedule loading, empty, error and retry states work on rendered DOM', async () => {
    let resolveFetch;
    win.fetch = () => new Promise(resolve => { resolveFetch = resolve; });
    const pending = hub.loadSchedules();
    assert.ok(el('schedulesLoading'), 'Modal must exist in the actual rendered page');
    assert.equal(visible('schedulesLoading'), true);
    resolveFetch(response({ data: [] }));
    await pending;
    assert.equal(visible('schedulesEmpty'), true);
    assert.equal(visible('schedulesLoading'), false);
    win.fetch = async () => { throw new Error('offline'); };
    await hub.loadSchedules();
    assert.equal(visible('schedulesError'), true);
    win.fetch = async () => response({ data: [{ title: 'Recovered' }] });
    el('schedulesRetryBtn').click();
    await until(() => visible('schedulesContent'));
    assert.equal(visible('schedulesError'), false);
    assert.match(el('schedulesContent').textContent, /Recovered/);
});
