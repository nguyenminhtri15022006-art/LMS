const { test } = require('node:test');
const assert = require('node:assert/strict');
const { assertLoginUrl, assertAvatar, assertCorsEvidence, assertFixtureIdentity, requiredEnv } = require('./support/e2e-assertions');

test('login error redirect never counts as a successful login', () => {
    assert.throws(() => assertLoginUrl('http://localhost:8088/login?error', 'http://localhost:8088'));
});
test('login redirect to another origin is rejected', () => {
    assert.throws(() => assertLoginUrl('http://example.org/', 'http://localhost:8088'));
});
test('old avatar after reload does not prove new upload persisted', () => {
    assert.throws(() => assertAvatar('http://localhost/old.png', 'http://localhost/new.png', true));
});
test('network failure without an HTTP response cannot prove CORS rejection', () => {
    assert.throws(() => assertCorsEvidence({ success: true, status: 200 }, { success: false }, null));
});
test('CORS test requires successful allowed-origin control plus denied response', () => {
    assert.doesNotThrow(() => assertCorsEvidence({ success: true, status: 200 }, { success: false },
        { status: 200, allowOrigin: null }));
});
test('fixture cleanup refuses a shared account', () => {
    assert.throws(() => assertFixtureIdentity({ username: 'admin', email: 'admin@example.com' }));
});
test('missing credentials fail before test infrastructure starts', () => {
    assert.throws(() => requiredEnv('SMOKE_DB_PASSWORD', {}), /SMOKE_DB_PASSWORD/);
});
