const { test, describe, before, after } = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const { chromium } = require('@playwright/test');
const { Fixtures, png } = require('./support/e2e-fixtures');
const { assertLoginUrl, assertAvatar, assertCorsEvidence } = require('./support/e2e-assertions');
const WEBAPP_URL = process.env.SMOKE_WEBAPP_URL || 'http://localhost:8088';

describe('Browser E2E: isolated users, avatar persistence, CORS and real OTP', { timeout: 180000 }, () => {
    let fixtures, browser, unauthorizedServer, unauthorizedOrigin;
    before(async () => {
        fixtures = new Fixtures();
        await fixtures.start();
        unauthorizedServer = http.createServer((req, res) => {
            res.writeHead(200, { 'Content-Type': 'text/html' });
            res.end('<!doctype html><title>Untrusted origin</title>');
        });
        await new Promise((resolve, reject) => {
            unauthorizedServer.once('error', reject);
            unauthorizedServer.listen(0, '127.0.0.1', resolve);
        });
        unauthorizedOrigin = `http://127.0.0.1:${unauthorizedServer.address().port}`;
        browser = await chromium.launch({ headless: true });
    });
    after(async () => {
        try { if (fixtures) await fixtures.close(); }
        finally {
            if (browser) await browser.close();
            if (unauthorizedServer?.listening) await new Promise(resolve => unauthorizedServer.close(resolve));
        }
    });
    async function pageFor(t) {
        const context = await browser.newContext();
        t.after(() => context.close());
        const page = await context.newPage();
        page.setDefaultTimeout(15000);
        return page;
    }
    async function registerAndVerify(page, account) {
        await page.goto(`${WEBAPP_URL}/register`);
        await page.fill('#fullname', 'Test Student');
        await page.fill('#username', account.username);
        await page.fill('#email', account.email);
        await page.fill('#password', account.password);
        await page.fill('#confirmPassword', account.password);
        await Promise.all([page.waitForURL(url => url.pathname === '/verify-email'), page.click('button[type="submit"]')]);
        assert.equal(new URL(page.url()).searchParams.get('email'), account.email);
        assert.equal((await fixtures.user(account)).is_verified, 0);
        await page.fill('#otp', await fixtures.otp(account));
        await Promise.all([page.waitForURL(url => url.pathname === '/login' && url.searchParams.get('verified') === 'true'),
            page.click('button[type="submit"]')]);
        assert.equal((await fixtures.user(account)).is_verified, 1);
    }
    async function loginAndCheckProfile(page, account) {
        await page.goto(`${WEBAPP_URL}/login`);
        await page.fill('#username', account.username);
        await page.fill('#password', account.password);
        await Promise.all([page.waitForURL(url => url.pathname === '/' && !url.search), page.click('button[type="submit"]')]);
        assertLoginUrl(page.url(), WEBAPP_URL);
        const response = await page.goto(`${WEBAPP_URL}/profile`);
        assert.equal(response.status(), 200);
        assert.equal(new URL(page.url()).pathname, '/profile');
        assert.equal(await page.getByText(account.email, { exact: true }).count(), 1);
        assert.ok((await page.context().cookies()).some(cookie => cookie.name === 'JSESSIONID'));
    }
    test('avatar UI uploads a new object, persists exact URL in MySQL and renders identical bytes', async t => {
        const page = await pageFor(t);
        const account = await fixtures.account();
        await registerAndVerify(page, account);
        await loginAndCheckProfile(page, account);
        const oldUrl = await page.locator('#avatarImage').getAttribute('src');
        const uploadResponse = page.waitForResponse(response => response.request().method() === 'POST'
            && response.url() === `${fixtures.minioUrl}/${fixtures.bucket}`);
        const policyResponse = page.waitForResponse(response => response.url().endsWith('/profile/avatar/presigned-url'));
        const saved = page.waitForURL(url => url.pathname === '/profile' && url.searchParams.get('updated') === 'true');
        await page.setInputFiles('#avatarFileInput', { name: 'fixture.png', mimeType: 'image/png', buffer: png });
        const policyHttp = await policyResponse;
        assert.equal(policyHttp.status(), 200);
        const policy = await policyHttp.json();
        const expected = policy.publicUrl;
        assert.notEqual(expected, oldUrl);
        assert.ok([200, 204].includes((await uploadResponse).status()));
        await saved;
        assert.equal((await fixtures.user(account)).avatar_url, expected);
        await page.reload();
        const image = page.locator('#avatarImage');
        const decoded = await image.evaluate(async img => { try { await img.decode(); return img.naturalWidth > 0; } catch { return false; } });
        assertAvatar(await image.getAttribute('src'), expected, decoded);
        const bytes = await page.request.get(expected);
        assert.equal(bytes.status(), 200);
        assert.deepEqual(await bytes.body(), png);
    });
    test('CORS denies an untrusted browser origin while identical allowed-origin requests succeed', async t => {
        const allowedPage = await pageFor(t);
        const deniedPage = await pageFor(t);
        await allowedPage.goto(`${WEBAPP_URL}/login`);
        await deniedPage.goto(unauthorizedOrigin);
        const objectUrl = `${fixtures.minioUrl}/${fixtures.bucket}/${fixtures.corsKey}`;
        const fetchInBrowser = page => page.evaluate(async url => {
            try {
                const response = await fetch(url, { headers: { 'X-Smoke-Probe': 'cors' } });
                return { success: true, status: response.status, bytes: Array.from(new Uint8Array(await response.arrayBuffer())) };
            } catch { return { success: false }; }
        }, objectUrl);
        const allowed = await fetchInBrowser(allowedPage);
        assert.deepEqual(allowed.bytes, Array.from(png));
        const denied = await fetchInBrowser(deniedPage);
        const preflight = await fetch(objectUrl, { method: 'OPTIONS', signal: AbortSignal.timeout(10000), headers: {
            Origin: unauthorizedOrigin, 'Access-Control-Request-Method': 'GET', 'Access-Control-Request-Headers': 'x-smoke-probe'
        } });
        assertCorsEvidence(allowed, denied, { status: preflight.status, allowOrigin: preflight.headers.get('access-control-allow-origin') });
        const controlAfter = await fetchInBrowser(allowedPage);
        assert.equal(controlAfter.status, 200);
        assert.deepEqual(controlAfter.bytes, Array.from(png));
    });
    test('registration sends real Mailpit OTP, verifies database state and logs into the correct profile', async t => {
        const page = await pageFor(t);
        const account = await fixtures.account();
        await registerAndVerify(page, account);
        await loginAndCheckProfile(page, account);
    });
});
