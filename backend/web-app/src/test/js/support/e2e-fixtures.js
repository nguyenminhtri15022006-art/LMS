const assert = require('node:assert/strict');
const { randomUUID, randomBytes } = require('node:crypto');
const mysql = require('mysql2/promise');
const { Client } = require('minio');
const { execSync } = require('node:child_process');
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
        this.courseBucket = process.env.SMOKE_MINIO_COURSE_BUCKET || 'lms-course-bucket';
        this.blogBucket = process.env.SMOKE_MINIO_BLOG_BUCKET || 'lms-blog-bucket';
        this.mailpit = process.env.SMOKE_MAILPIT_URL || 'http://localhost:8025';
        this.minioUrl = process.env.SMOKE_MINIO_URL || 'http://localhost:9000';
        this.gatewayUrl = process.env.SMOKE_GATEWAY_URL || 'http://localhost:8080';
        this.webappUrl = process.env.SMOKE_WEBAPP_URL || 'http://localhost:8088';

        const url = new URL(this.minioUrl);
        this.minio = new Client({
            endPoint: url.hostname,
            port: Number(url.port || (url.protocol === 'https:' ? 443 : 80)),
            useSSL: url.protocol === 'https:',
            region: 'us-east-1',
            accessKey: requiredEnv('SMOKE_MINIO_ACCESS_KEY'),
            secretKey: requiredEnv('SMOKE_MINIO_SECRET_KEY')
        });

        this.dbConfig = {
            host: process.env.SMOKE_DB_HOST || '127.0.0.1',
            port: Number(process.env.SMOKE_DB_PORT || 3307),
            user: requiredEnv('SMOKE_DB_USER'),
            password: requiredEnv('SMOKE_DB_PASSWORD'),
            database: 'identity_lms',
            connectTimeout: 10000
        };

        this.accounts = [];
        this.courses = [];
        this.categories = [];
        this.tags = [];
        this.posts = [];
        this.minioObjects = [];
        this.corsKey = `smoke/${randomUUID()}/cors.png`;
    }

    async start() {
        this.db = await mysql.createConnection(this.dbConfig);
        this.courseDb = await mysql.createConnection({ ...this.dbConfig, database: 'course_lms' });
        this.blogDb = await mysql.createConnection({ ...this.dbConfig, database: 'blog_lms' });
        await this.minio.putObject(this.bucket, this.corsKey, png, png.length, { 'Content-Type': 'image/png' });
    }

    async account() {
        const username = `e2e_${randomUUID().replaceAll('-', '')}`;
        const account = {
            username,
            email: `${username}@test.lms.invalid`,
            password: randomBytes(24).toString('base64url'),
            role: 'STUDENT'
        };
        assertFixtureIdentity(account);
        const [rows] = await this.db.execute('SELECT user_id FROM users WHERE user_name = ? OR email = ?', [account.username, account.email]);
        assert.equal(rows.length, 0, 'Fixture identity must not exist before this run');
        this.accounts.push(account);
        return account;
    }

    async instructorAccount() {
        const username = `e2e_${randomUUID().replaceAll('-', '')}`;
        const account = {
            username,
            email: `${username}@test.lms.invalid`,
            password: 'InstructorPass123!',
            fullname: 'Giang Vien EtoE',
            role: 'INSTRUCTOR'
        };
        assertFixtureIdentity(account);
        const [rows] = await this.db.execute('SELECT user_id FROM users WHERE user_name = ? OR email = ?', [account.username, account.email]);
        assert.equal(rows.length, 0, 'Fixture identity must not exist before this run');

        // Register instructor directly via Gateway
        const regRes = await fetch(`${this.gatewayUrl}/api/identity/auth/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                username: account.username,
                email: account.email,
                password: account.password,
                fullname: account.fullname,
                role: 'INSTRUCTOR'
            })
        });
        assert.equal(regRes.status, 200, `Instructor registration failed: ${regRes.status}`);

        // Trigger OTP delivery
        const sendRes = await fetch(`${this.gatewayUrl}/api/identity/auth/verify-email/send`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: account.email })
        });
        assert.equal(sendRes.status, 200, `OTP send failed: ${sendRes.status}`);

        // Verify with received OTP
        const otp = await this.otp(account);
        const verRes = await fetch(`${this.gatewayUrl}/api/identity/auth/verify-email`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: account.email, otp })
        });
        assert.equal(verRes.status, 200, `Instructor verify failed: ${verRes.status}`);

        this.accounts.push(account);
        return account;
    }

    async user(account) {
        assertFixtureIdentity(account);
        const [rows] = await this.db.execute('SELECT user_id, user_name, email, role, avatar_url, is_verified, is_active FROM users WHERE user_name = ? AND email = ?',
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

    async removeObjects(prefix, bucket = this.bucket) {
        assert.ok(prefix.startsWith('avatars/') || prefix.startsWith('thumbnails/') || prefix === this.corsKey || prefix.startsWith('smoke/') || prefix.startsWith('e2e/'));
        for await (const object of this.minio.listObjectsV2(bucket, prefix, true)) {
            assert.ok(object.name.startsWith(prefix));
            await this.minio.removeObject(bucket, object.name);
        }
        const remaining = [];
        for await (const object of this.minio.listObjectsV2(bucket, prefix, true)) remaining.push(object);
        assert.equal(remaining.length, 0, 'Fixture objects must be removed');
    }

    trackCourse(courseId) {
        if (courseId && !this.courses.includes(courseId)) {
            this.courses.push(courseId);
        }
    }

    trackCategory(categoryId) {
        if (categoryId && !this.categories.includes(categoryId)) {
            this.categories.push(categoryId);
        }
    }

    trackTag(tagId) {
        if (tagId && !this.tags.includes(tagId)) {
            this.tags.push(tagId);
        }
    }

    trackPost(postId) {
        if (postId && !this.posts.includes(postId)) {
            this.posts.push(postId);
        }
    }

    trackMinioObject(bucket, objectKey) {
        this.minioObjects.push({ bucket, objectKey });
    }

    mongoExec(evalStr) {
        const sanitized = evalStr.replace(/\r?\n/g, ' ').replace(/"/g, '\\"');
        const cmd = `docker exec lms-mongodb mongosh --quiet --eval "${sanitized}"`;
        const out = execSync(cmd, { stdio: ['pipe', 'pipe', 'pipe'], timeout: 10000 });
        return out.toString().trim();
    }

    mongoStructure(courseId) {
        try {
            const out = this.mongoExec(`JSON.stringify(db.getSiblingDB('course_lms').course_structures.findOne({ courseId: '${courseId}' }))`);
            return JSON.parse(out);
        } catch {
            return null;
        }
    }

    async cleanupCourse(courseId) {
        if (!courseId) return;
        // Clean MongoDB
        try {
            this.mongoExec(`db.getSiblingDB('course_lms').course_structures.deleteMany({ courseId: '${courseId}' })`);
            this.mongoExec(`db.getSiblingDB('course_lms').lessons.deleteMany({ courseId: '${courseId}' })`);
        } catch (e) {
            console.warn(`Mongo cleanup error for course ${courseId}:`, e.message);
        }

        // Clean MySQL
        if (this.courseDb) {
            const [cRows] = await this.courseDb.execute('SELECT thumbnail_url FROM courses WHERE id = ?', [courseId]);
            if (cRows.length && cRows[0].thumbnail_url) {
                try {
                    const u = new URL(cRows[0].thumbnail_url);
                    const path = u.pathname.replace(/^\/lms-course-bucket\//, '');
                    await this.minio.removeObject(this.courseBucket, path);
                } catch (ignored) {}
            }
            await this.courseDb.execute('DELETE FROM lesson_progress WHERE course_id = ?', [courseId]);
            await this.courseDb.execute('DELETE FROM enrollments WHERE course_id = ?', [courseId]);
            await this.courseDb.execute('DELETE FROM announcements WHERE course_id = ?', [courseId]);
            await this.courseDb.execute('DELETE FROM schedules WHERE course_id = ?', [courseId]);
            await this.courseDb.execute('DELETE FROM course_categories WHERE course_id = ?', [courseId]);
            await this.courseDb.execute('DELETE FROM course_learning_points WHERE course_id = ?', [courseId]);
            await this.courseDb.execute('DELETE FROM course_requirements WHERE course_id = ?', [courseId]);
            await this.courseDb.execute('DELETE FROM courses WHERE id = ?', [courseId]);
        }
    }

    async cleanupPost(postId) {
        if (!postId || !this.blogDb) return;
        const [pRows] = await this.blogDb.execute('SELECT thumbnail FROM posts WHERE id = ?', [postId]);
        if (pRows.length && pRows[0].thumbnail) {
            try {
                const u = new URL(pRows[0].thumbnail);
                const path = u.pathname.replace(/^\/lms-blog-bucket\//, '');
                await this.minio.removeObject(this.blogBucket, path);
            } catch (ignored) {}
        }
        await this.blogDb.execute('DELETE FROM votes WHERE target_id = ? AND target_type = "POST"', [postId]);
        await this.blogDb.execute('DELETE FROM comments WHERE post_id = ?', [postId]);
        await this.blogDb.execute('DELETE FROM post_tags WHERE post_id = ?', [postId]);
        await this.blogDb.execute('DELETE FROM posts WHERE id = ?', [postId]);
    }

    async cleanupCategory(categoryId) {
        if (!categoryId || !this.courseDb) return;
        await this.courseDb.execute('DELETE FROM course_categories WHERE category_id = ?', [categoryId]);
        await this.courseDb.execute('DELETE FROM categories WHERE id = ?', [categoryId]);
    }

    async cleanupTag(tagId) {
        if (!tagId || !this.blogDb) return;
        await this.blogDb.execute('DELETE FROM post_tags WHERE tag_id = ?', [tagId]);
        await this.blogDb.execute('DELETE FROM tags WHERE id = ?', [tagId]);
    }

    async cleanupAccount(account) {
        assertFixtureIdentity(account);
        assert.ok(this.accounts.includes(account), 'Only identities allocated by this run may be cleaned');
        const [rows] = await this.db.execute('SELECT user_id FROM users WHERE user_name = ? AND email = ?', [account.username, account.email]);
        assert.ok(rows.length <= 1);
        if (rows.length) {
            const id = rows[0].user_id;
            assert.match(id, /^[a-f0-9-]{36}$/i);
            try { await this.removeObjects(`avatars/${id}-`, this.bucket); } catch (ignored) {}
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
        if (ids.length) {
            await request(`${this.mailpit}/api/v1/messages`, {
                method: 'DELETE',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ IDs: ids })
            });
        }
        assert.equal((await this.messages(account)).length, 0, 'Fixture email must be removed');
        const [remaining] = await this.db.execute('SELECT user_id FROM users WHERE user_name = ? OR email = ?', [account.username, account.email]);
        assert.equal(remaining.length, 0, 'Fixture user must be removed');
    }

    async close() {
        const failures = [];

        // 1. Clean tracked posts
        for (const postId of this.posts) {
            try { await this.cleanupPost(postId); } catch (err) { failures.push(err); }
        }

        // 2. Clean tracked courses
        for (const courseId of this.courses) {
            try { await this.cleanupCourse(courseId); } catch (err) { failures.push(err); }
        }

        // 3. Clean tracked categories
        for (const categoryId of this.categories) {
            try { await this.cleanupCategory(categoryId); } catch (err) { failures.push(err); }
        }

        // 4. Clean tracked tags
        for (const tagId of this.tags) {
            try { await this.cleanupTag(tagId); } catch (err) { failures.push(err); }
        }

        // 5. Clean tracked minio objects
        for (const { bucket, objectKey } of this.minioObjects) {
            try { await this.minio.removeObject(bucket, objectKey); } catch (err) { failures.push(err); }
        }

        // 6. Clean tracked accounts
        if (this.db) {
            for (const account of this.accounts) {
                try { await this.cleanupAccount(account); } catch (error) { failures.push(error); }
            }
            try { await this.removeObjects(this.corsKey); } catch (error) { failures.push(error); }
            await this.db.end();
        }

        if (this.courseDb) {
            try { await this.courseDb.end(); } catch (err) { failures.push(err); }
        }

        if (this.blogDb) {
            try { await this.blogDb.end(); } catch (err) { failures.push(err); }
        }

        if (failures.length) throw new AggregateError(failures, 'E2E fixture cleanup failed');
    }
}

module.exports = { Fixtures, png };
