const assert = require('node:assert/strict');
const { randomUUID, randomBytes } = require('node:crypto');
const mysql = require('mysql2/promise');
const { Client } = require('minio');
const { requiredEnv, assertFixtureIdentity } = require('./e2e-assertions');

const png = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=', 'base64');
async function request(url, options = {}) {
    const response = await fetch(url, { ...options, signal: AbortSignal.timeout(10000) });
    assert.ok(response.ok, `Fixture HTTP request failed: ${response.status}`);
    return response;
}

class Fixtures {
    constructor() {
        this.bucket = requiredEnv('SMOKE_MINIO_BUCKET');
        this.mailpit = process.env.SMOKE_MAILPIT_URL || 'http://localhost:8025';
        this.minioUrl = process.env.SMOKE_MINIO_URL || 'http://localhost:9000';
        const url = new URL(this.minioUrl);
        this.minio = new Client({ endPoint: url.hostname, port: Number(url.port || (url.protocol === 'https:' ? 443 : 80)),
            useSSL: url.protocol === 'https:', region: 'us-east-1',
            accessKey: requiredEnv('SMOKE_MINIO_ACCESS_KEY'), secretKey: requiredEnv('SMOKE_MINIO_SECRET_KEY') });
        this.dbConfig = { host: process.env.SMOKE_DB_HOST || '127.0.0.1',
            port: Number(process.env.SMOKE_DB_PORT || 3307), user: requiredEnv('SMOKE_DB_USER'),
            password: requiredEnv('SMOKE_DB_PASSWORD'), database: 'identity_lms', connectTimeout: 10000 };
        this.accounts = [];
        this.corsKey = `smoke/${randomUUID()}/cors.png`;
    }
    async start() {
        this.db = await mysql.createConnection(this.dbConfig);
        await this.minio.putObject(this.bucket, this.corsKey, png, png.length, { 'Content-Type': 'image/png' });
    }
    async account() {
        const username = `e2e_${randomUUID().replaceAll('-', '')}`;
        const account = { username, email: `${username}@test.lms.invalid`, password: randomBytes(24).toString('base64url') };
        assertFixtureIdentity(account);
        const [rows] = await this.db.execute('SELECT user_id FROM users WHERE user_name = ? OR email = ?', [account.username, account.email]);
        assert.equal(rows.length, 0, 'Fixture identity must not exist before this run');
        this.accounts.push(account);
        return account;
    }
    async user(account) {
        assertFixtureIdentity(account);
        const [rows] = await this.db.execute('SELECT user_id, avatar_url, is_verified FROM users WHERE user_name = ? AND email = ?',
            [account.username, account.email]);
        assert.equal(rows.length, 1, 'Exactly one account created by this run must exist');
        return rows[0];
    }
    async messages(account) {
        assertFixtureIdentity(account);
        const found = [];
        for (let start = 0; ; start += 100) {
            const data = await (await request(`${this.mailpit}/api/v1/messages?start=${start}&limit=100`)).json();
            const messages = data.messages || [];
            found.push(...messages.filter(m => m.To?.some(to => to.Address === account.email)));
            if (start + messages.length >= data.total || messages.length < 100) break;
        }
        return found;
    }
    async otp(account) {
        const deadline = Date.now() + 20000;
        do {
            for (const message of await this.messages(account)) {
                const detail = await (await request(`${this.mailpit}/api/v1/message/${encodeURIComponent(message.ID)}`)).json();
                const match = (detail.Text || detail.HTML || '').match(/\b\d{6}\b/);
                if (match) return match[0];
            }
            await new Promise(resolve => setTimeout(resolve, 400));
        } while (Date.now() < deadline);
        throw new Error('OTP email for this fixture did not arrive');
    }
    async removeObjects(prefix) {
        assert.ok(prefix.startsWith('avatars/') || prefix === this.corsKey);
        for await (const object of this.minio.listObjectsV2(this.bucket, prefix, true)) {
            assert.ok(object.name.startsWith(prefix));
            await this.minio.removeObject(this.bucket, object.name);
        }
        const remaining = [];
        for await (const object of this.minio.listObjectsV2(this.bucket, prefix, true)) remaining.push(object);
        assert.equal(remaining.length, 0, 'Fixture objects must be removed');
    }
    async cleanupAccount(account) {
        assertFixtureIdentity(account);
        assert.ok(this.accounts.includes(account), 'Only identities allocated by this run may be cleaned');
        const [rows] = await this.db.execute('SELECT user_id FROM users WHERE user_name = ? AND email = ?', [account.username, account.email]);
        assert.ok(rows.length <= 1);
        if (rows.length) {
            const id = rows[0].user_id;
            assert.match(id, /^[a-f0-9-]{36}$/i);
            await this.removeObjects(`avatars/${id}-`);
            await this.db.beginTransaction();
            try {
                await this.db.execute('DELETE FROM refresh_tokens WHERE user_id = ?', [id]);
                await this.db.execute('DELETE FROM email_verifications WHERE user_id = ?', [id]);
                const [deleted] = await this.db.execute('DELETE FROM users WHERE user_id = ? AND user_name = ? AND email = ?', [id, account.username, account.email]);
                assert.equal(deleted.affectedRows, 1);
                await this.db.commit();
            } catch (error) { await this.db.rollback(); throw error; }
        }
        const ids = [...new Set((await this.messages(account)).map(m => m.ID))];
        // Mailpit deletes ALL mail if IDs is empty: never send an empty selection.
        if (ids.length) await request(`${this.mailpit}/api/v1/messages`, {
            method: 'DELETE', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ IDs: ids }) });
        assert.equal((await this.messages(account)).length, 0, 'Fixture email must be removed');
        const [remaining] = await this.db.execute('SELECT user_id FROM users WHERE user_name = ? OR email = ?', [account.username, account.email]);
        assert.equal(remaining.length, 0, 'Fixture user must be removed');
    }
    async close() {
        const failures = [];
        if (this.db) {
            for (const account of this.accounts) {
                try { await this.cleanupAccount(account); } catch (error) { failures.push(error); }
            }
            try { await this.removeObjects(this.corsKey); } catch (error) { failures.push(error); }
            await this.db.end();
        }
        if (failures.length) throw new AggregateError(failures, 'E2E fixture cleanup failed');
    }
}
module.exports = { Fixtures, png };
