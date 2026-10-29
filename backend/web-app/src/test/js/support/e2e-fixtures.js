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
        this.bucket = process.env.SMOKE_MINIO_BUCKET || process.env.MINIO_IDENTITY_BUCKET || 'lms-identity-bucket';
        this.courseBucket = process.env.SMOKE_MINIO_COURSE_BUCKET || process.env.MINIO_BUCKET || 'lms-course-bucket';
        this.blogBucket = process.env.SMOKE_MINIO_BLOG_BUCKET || process.env.MINIO_BLOG_BUCKET || 'lms-blog-bucket';
        this.mailpit = process.env.SMOKE_MAILPIT_URL || 'http://localhost:8025';
        this.minioUrl = process.env.SMOKE_MINIO_URL || process.env.MINIO_PUBLIC_URL || 'http://localhost:9000';
        this.gatewayUrl = process.env.SMOKE_GATEWAY_URL || 'http://localhost:8080';
        this.webappUrl = process.env.SMOKE_WEBAPP_URL || 'http://localhost:8088';

        const minioAccessKey = process.env.SMOKE_MINIO_ACCESS_KEY || process.env.MINIO_ACCESS_KEY || process.env.MINIO_ROOT_USER;
        const minioSecretKey = process.env.SMOKE_MINIO_SECRET_KEY || process.env.MINIO_SECRET_KEY || process.env.MINIO_ROOT_PASSWORD;
        assert.ok(minioAccessKey, 'MinIO access key must be provided via SMOKE_MINIO_ACCESS_KEY or MINIO_ACCESS_KEY/MINIO_ROOT_USER');
        assert.ok(minioSecretKey, 'MinIO secret key must be provided via SMOKE_MINIO_SECRET_KEY or MINIO_SECRET_KEY/MINIO_ROOT_PASSWORD');

        const url = new URL(this.minioUrl);
        this.minio = new Client({
            endPoint: url.hostname,
            port: Number(url.port || (url.protocol === 'https:' ? 443 : 80)),
            useSSL: url.protocol === 'https:',
            region: 'us-east-1',
            accessKey: minioAccessKey,
            secretKey: minioSecretKey
        });

        const dbUser = process.env.SMOKE_DB_USER || process.env.MYSQL_ROOT_USERNAME || 'root';
        const dbPassword = process.env.SMOKE_DB_PASSWORD || process.env.MYSQL_ROOT_PASSWORD;
        assert.ok(dbPassword, 'MySQL root password must be provided via SMOKE_DB_PASSWORD or MYSQL_ROOT_PASSWORD');

        this.dbConfig = {
            host: process.env.SMOKE_DB_HOST || '127.0.0.1',
            port: Number(process.env.SMOKE_DB_PORT || 3307),
            user: dbUser,
            password: dbPassword,
            database: 'identity_lms',
            connectTimeout: 10000
        };

        this.accounts = [];
        this.courses = [];
        this.categories = [];
        this.tags = [];
        this.posts = [];
        this.minioObjects = [];
        this.cleanedCourses = new Set();
        this.cleanedCategories = new Set();
        this.cleanedTags = new Set();
        this.cleanedPosts = new Set();
        this.cleanedAccounts = new Set();
        this.allowedPrefixes = new Set();
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
            password: randomBytes(24).toString('base64url') + 'A1!',
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

        // Track immediately so cleanup finds account even if OTP delivery or verification fails
        this.accounts.push(account);

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

    isOwnedPrefix(prefix) {
        if (!prefix || typeof prefix !== 'string') return false;
        const broadPrefixes = ['avatars/', 'avatars', 'thumbnails/', 'thumbnails', 'smoke/', 'smoke', 'e2e/', 'e2e', '/', ''];
        if (broadPrefixes.includes(prefix)) return false;
        if (this.corsKey && prefix === this.corsKey) return true;
        if (this.allowedPrefixes && this.allowedPrefixes.has(prefix)) return true;
        if (this.minioObjects && this.minioObjects.some(o => o.objectKey === prefix)) return true;
        return false;
    }

    async removeObjects(prefix, bucket = this.bucket) {
        assert.ok(typeof prefix === 'string' && prefix.length > 0, 'Prefix must be a non-empty string');
        const broadPrefixes = ['avatars/', 'avatars', 'thumbnails/', 'thumbnails', 'smoke/', 'smoke', 'e2e/', 'e2e', '/', ''];
        assert.ok(!broadPrefixes.includes(prefix), `Broad prefix "${prefix}" is forbidden`);
        assert.ok(this.isOwnedPrefix(prefix), `Prefix "${prefix}" is not owned by this fixture run`);
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
        assert.ok(this.courses.includes(courseId), `Course ${courseId} is not owned by this fixture`);
        if (this.cleanedCourses.has(courseId)) return;

        const errors = [];

        // 1. Clean MongoDB
        try {
            this.mongoExec(`db.getSiblingDB('course_lms').course_structures.deleteMany({ courseId: '${courseId}' })`);
            this.mongoExec(`db.getSiblingDB('course_lms').lessons.deleteMany({ courseId: '${courseId}' })`);
        } catch (e) {
            errors.push(e);
        }

        // 2. Clean MinIO thumbnail & MySQL
        if (this.courseDb) {
            try {
                const [cRows] = await this.courseDb.execute('SELECT thumbnail_url FROM courses WHERE id = ?', [courseId]);
                if (cRows.length && cRows[0].thumbnail_url) {
                    try {
                        const u = new URL(cRows[0].thumbnail_url);
                        const path = u.pathname.replace(/^\/lms-course-bucket\//, '');
                        await this.minio.removeObject(this.courseBucket, path);
                    } catch (minioErr) {
                        errors.push(minioErr);
                    }
                }
            } catch (dbErr) {
                errors.push(dbErr);
            }

            try {
                await this.courseDb.execute('DELETE FROM lesson_progress WHERE course_id = ?', [courseId]);
                await this.courseDb.execute('DELETE FROM enrollments WHERE course_id = ?', [courseId]);
                await this.courseDb.execute('DELETE FROM announcements WHERE course_id = ?', [courseId]);
                await this.courseDb.execute('DELETE FROM schedules WHERE course_id = ?', [courseId]);
                await this.courseDb.execute('DELETE FROM course_categories WHERE course_id = ?', [courseId]);
                await this.courseDb.execute('DELETE FROM course_learning_points WHERE course_id = ?', [courseId]);
                await this.courseDb.execute('DELETE FROM course_requirements WHERE course_id = ?', [courseId]);
                await this.courseDb.execute('DELETE FROM courses WHERE id = ?', [courseId]);
            } catch (dbErr) {
                errors.push(dbErr);
            }
        }

        if (errors.length) {
            throw errors.length === 1 ? errors[0] : new AggregateError(errors, `Course cleanup failed for ${courseId}`);
        }

        this.cleanedCourses.add(courseId);
    }

    async cleanupPost(postId) {
        if (!postId) return;
        assert.ok(this.posts.includes(postId), `Post ${postId} is not owned by this fixture`);
        if (this.cleanedPosts.has(postId)) return;

        const errors = [];

        if (this.blogDb) {
            try {
                const [pRows] = await this.blogDb.execute('SELECT thumbnail FROM posts WHERE id = ?', [postId]);
                if (pRows.length && pRows[0].thumbnail) {
                    try {
                        const u = new URL(pRows[0].thumbnail);
                        const path = u.pathname.replace(/^\/lms-blog-bucket\//, '');
                        await this.minio.removeObject(this.blogBucket, path);
                    } catch (minioErr) {
                        errors.push(minioErr);
                    }
                }
            } catch (dbErr) {
                errors.push(dbErr);
            }

            try {
                await this.blogDb.execute('DELETE FROM votes WHERE target_id = ? AND target_type = "POST"', [postId]);
                await this.blogDb.execute('DELETE FROM comments WHERE post_id = ?', [postId]);
                await this.blogDb.execute('DELETE FROM post_tags WHERE post_id = ?', [postId]);
                await this.blogDb.execute('DELETE FROM posts WHERE id = ?', [postId]);
            } catch (dbErr) {
                errors.push(dbErr);
            }
        }

        if (errors.length) {
            throw errors.length === 1 ? errors[0] : new AggregateError(errors, `Post cleanup failed for ${postId}`);
        }

        this.cleanedPosts.add(postId);
    }

    async cleanupCategory(categoryId) {
        if (!categoryId) return;
        assert.ok(this.categories.includes(categoryId), `Category ${categoryId} is not owned by this fixture`);
        if (this.cleanedCategories.has(categoryId)) return;

        const errors = [];
        if (this.courseDb) {
            try {
                await this.courseDb.execute('DELETE FROM course_categories WHERE category_id = ?', [categoryId]);
                await this.courseDb.execute('DELETE FROM categories WHERE id = ?', [categoryId]);
            } catch (dbErr) {
                errors.push(dbErr);
            }
        }

        if (errors.length) {
            throw errors.length === 1 ? errors[0] : new AggregateError(errors, `Category cleanup failed for ${categoryId}`);
        }

        this.cleanedCategories.add(categoryId);
    }

    async cleanupTag(tagId) {
        if (!tagId) return;
        assert.ok(this.tags.includes(tagId), `Tag ${tagId} is not owned by this fixture`);
        if (this.cleanedTags.has(tagId)) return;

        const errors = [];
        if (this.blogDb) {
            try {
                await this.blogDb.execute('DELETE FROM post_tags WHERE tag_id = ?', [tagId]);
                await this.blogDb.execute('DELETE FROM tags WHERE id = ?', [tagId]);
            } catch (dbErr) {
                errors.push(dbErr);
            }
        }

        if (errors.length) {
            throw errors.length === 1 ? errors[0] : new AggregateError(errors, `Tag cleanup failed for ${tagId}`);
        }

        this.cleanedTags.add(tagId);
    }

    async cleanupAccount(account) {
        assertFixtureIdentity(account);
        assert.ok(this.accounts.includes(account), 'Only identities allocated by this run may be cleaned');
        if (this.cleanedAccounts.has(account.username)) return;

        const errors = [];
        let userId = null;

        if (this.db) {
            try {
                const [rows] = await this.db.execute('SELECT user_id FROM users WHERE user_name = ? AND email = ?', [account.username, account.email]);
                assert.ok(rows.length <= 1);
                if (rows.length) {
                    userId = rows[0].user_id;
                    assert.match(userId, /^[a-f0-9-]{36}$/i);
                    const avatarPrefix = `avatars/${userId}-`;
                    if (!this.allowedPrefixes) this.allowedPrefixes = new Set();
                    this.allowedPrefixes.add(avatarPrefix);
                    try {
                        await this.removeObjects(avatarPrefix, this.bucket);
                    } catch (minioErr) {
                        errors.push(minioErr);
                    }

                    await this.db.beginTransaction();
                    try {
                        await this.db.execute('DELETE FROM refresh_tokens WHERE user_id = ?', [userId]);
                        await this.db.execute('DELETE FROM email_verifications WHERE user_id = ?', [userId]);
                        const [deleted] = await this.db.execute('DELETE FROM users WHERE user_id = ? AND user_name = ? AND email = ?', [userId, account.username, account.email]);
                        assert.equal(deleted.affectedRows, 1);
                        await this.db.commit();
                    } catch (txErr) {
                        await this.db.rollback();
                        errors.push(txErr);
                    }
                }
            } catch (dbErr) {
                errors.push(dbErr);
            }
        }

        try {
            const ids = [...new Set((await this.messages(account)).map(m => m.ID))];
            if (ids.length) {
                await request(`${this.mailpit}/api/v1/messages`, {
                    method: 'DELETE',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ IDs: ids })
                });
            }
            assert.equal((await this.messages(account)).length, 0, 'Fixture email must be removed');
        } catch (mailErr) {
            errors.push(mailErr);
        }

        if (this.db && userId) {
            try {
                const [remaining] = await this.db.execute('SELECT user_id FROM users WHERE user_name = ? OR email = ?', [account.username, account.email]);
                assert.equal(remaining.length, 0, 'Fixture user must be removed');
            } catch (dbErr) {
                errors.push(dbErr);
            }
        }

        if (errors.length) {
            throw errors.length === 1 ? errors[0] : new AggregateError(errors, `Account cleanup failed for ${account.username}`);
        }

        this.cleanedAccounts.add(account.username);
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
