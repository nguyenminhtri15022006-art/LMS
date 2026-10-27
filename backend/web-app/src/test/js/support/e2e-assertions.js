const assert = require('node:assert/strict');

function assertLoginUrl(actual, base) {
    const url = new URL(actual);
    assert.equal(url.origin, new URL(base).origin);
    assert.equal(url.pathname, '/');
    assert.equal(url.search, '');
}

function assertAvatar(actual, expected, decoded) {
    assert.ok(expected, 'Expected newly uploaded object URL');
    assert.equal(actual, expected, 'Avatar must match this upload, not an older image');
    assert.equal(decoded, true, 'Browser must decode the uploaded image');
}

function assertCorsEvidence(allowed, denied, preflight) {
    assert.equal(allowed.success, true, 'Allowed origin control must succeed');
    assert.equal(allowed.status, 200);
    assert.equal(denied.success, false);
    assert.ok(preflight, 'HTTP preflight evidence is required; network failure alone proves nothing');
    assert.ok([200, 204].includes(preflight.status));
    assert.equal(preflight.allowOrigin, null, 'Forbidden origin must not receive an allow-origin header');
}

function assertFixtureIdentity(fixture) {
    assert.match(fixture.username, /^e2e_[a-f0-9]{32}$/);
    assert.equal(fixture.email, `${fixture.username}@test.lms.invalid`);
}

function requiredEnv(name, env = process.env) {
    assert.ok(env[name] && env[name].trim(), `Missing required ${name}`);
    return env[name];
}

module.exports = { assertLoginUrl, assertAvatar, assertCorsEvidence, assertFixtureIdentity, requiredEnv };
