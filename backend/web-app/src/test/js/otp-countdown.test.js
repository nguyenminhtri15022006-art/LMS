const { test } = require('node:test');
const assert = require('node:assert/strict');
const { JSDOM } = require('jsdom');
const fs = require('node:fs');
const path = require('node:path');
const script = fs.readFileSync(path.join(__dirname, '../../main/resources/static/js/otp-countdown.js'), 'utf8');

test('failed initial OTP delivery enables resend immediately and successful resend starts countdown', async () => {
    const dom = new JSDOM(`<span id="countdownTimer" data-delivery-failed="true">60</span>
        <button id="resendOtpBtn" disabled><span id="resendBtnText">Resend</span></button>
        <input id="email" value="test@example.com"><input name="_csrf" value="test-token">`, { runScripts: 'outside-only' });
    try {
        let calls = 0;
        dom.window.fetch = async () => { calls++; return { ok: true, json: async () => ({ message: 'sent' }) }; };
        dom.window.alert = () => {};
        dom.window.eval(script);
        await new Promise(resolve => dom.window.document.addEventListener('DOMContentLoaded', resolve));
        const button = dom.window.document.getElementById('resendOtpBtn');
        assert.equal(button.disabled, false);
        assert.equal(dom.window.document.getElementById('countdownTimer').textContent, '0');
        button.click();
        await new Promise(resolve => setImmediate(resolve));
        assert.equal(calls, 1);
        assert.equal(button.disabled, true);
        assert.equal(dom.window.document.getElementById('countdownTimer').textContent, '60');
    } finally { dom.window.close(); }
});
