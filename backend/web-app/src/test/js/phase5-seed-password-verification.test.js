// Runs the real seed entrypoint with Docker intercepted; no database writes.
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '../../../../..');
if (process.argv.includes('--seed-password-probe')) {
    const cp = require('node:child_process');
    const originalExists = fs.existsSync;
    const originalRead = fs.readFileSync;
    const envPath = path.join(root, '.env');
    fs.existsSync = function (p) { return path.resolve(String(p)) === envPath ? false : originalExists.apply(this, arguments); };
    fs.readFileSync = function (p) { if (path.resolve(String(p)) === envPath) throw new Error('local env disabled for isolated test'); return originalRead.apply(this, arguments); };
    let sql = '', calls = 0;
    cp.execSync = function (command, options) { calls++; if (command.includes('mysql -uroot')) sql = String(options.input); return ''; };
    console.log = console.error = console.warn = () => {};
    const Module = require('node:module');
    const originalLoad = Module._load;
    if (process.argv.includes('--fail-bcrypt-load')) {
        Module._load = function (request, parent, isMain) {
            if (request === './bcrypt.js' && parent?.filename === path.join(root, 'scripts/seed-demo-data.cjs')) {
                throw new Error('SIMULATED_BCRYPT_LOAD_FAILURE');
            }
            return originalLoad.apply(this, arguments);
        };
    }
    process.on('exit', () => {
        const bcrypt = require(path.join(root, 'scripts/bcrypt.js'));
        const hash = sql.match(/\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}/)?.[0];
        const matches = !!hash && !!process.env.DEMO_USER_PASSWORD && bcrypt.compareSync(process.env.DEMO_USER_PASSWORD, hash);
        process.stdout.write(JSON.stringify({ calls, matches }));
    });
    require(path.join(root, 'scripts/seed-demo-data.cjs'));
} else {
    const { test } = require('node:test');
    const assert = require('node:assert/strict');
    const { spawnSync } = require('node:child_process');
    function probe(password, extraArgs = []) {
        const env = { ...process.env, MYSQL_ROOT_PASSWORD: 'isolated-placeholder' };
        delete env.DEMO_USER_PASSWORD;
        delete env.BOOTSTRAP_ADMIN_PASSWORDS;
        if (password) env.DEMO_USER_PASSWORD = password;
        const result = spawnSync(process.execPath, [__filename, '--seed-password-probe', ...extraArgs], { env, cwd: root, encoding: 'utf8' });
        assert.equal(result.error, undefined);
        return { status: result.status, ...JSON.parse(result.stdout) };
    }
    test('real seed entrypoint must hash the supplied demo password', () => {
        const result = probe(require('node:crypto').randomBytes(24).toString('base64url'));
        assert.equal(result.status, 0);
        assert.equal(result.matches, true, 'SQL password hash must match the supplied environment password');
    });
    test('missing demo password must fail before invoking Docker', () => {
        const result = probe();
        assert.notEqual(result.status, 0, 'missing password must not silently use a built-in credential');
        assert.equal(result.calls, 0);
    });
    test('bcrypt module load failure must stop before Docker without a fallback', () => {
        const result = probe(require('node:crypto').randomBytes(24).toString('base64url'), ['--fail-bcrypt-load']);
        assert.notEqual(result.status, 0, 'dependency load failure must stop seeding');
        assert.equal(result.calls, 0, 'no database commands may run after bcrypt load failure');
    });
    test('admin password must not be silently used when demo password is missing', () => {
        const env = { ...process.env, MYSQL_ROOT_PASSWORD: 'isolated-placeholder', BOOTSTRAP_ADMIN_PASSWORDS: 'admin_password_never_used' };
        delete env.DEMO_USER_PASSWORD;
        const result = spawnSync(process.execPath, [__filename, '--seed-password-probe'], { env, cwd: root, encoding: 'utf8' });
        assert.equal(result.error, undefined);
        const parsed = JSON.parse(result.stdout);
        assert.notEqual(result.status, 0, 'presence of admin credentials must not bypass missing demo password');
        assert.equal(parsed.calls, 0, 'Docker must not be invoked when demo password is missing');
    });
}
