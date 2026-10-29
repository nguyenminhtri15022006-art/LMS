const { test } = require('node:test');
const assert = require('node:assert/strict');
const { Fixtures } = require('./support/e2e-fixtures');

// In-memory verification only: no database, Docker or MinIO access.
function fixture() {
    return Object.assign(Object.create(Fixtures.prototype), {
        accounts: [], courses: [], categories: [], tags: [], posts: [], minioObjects: [],
        bucket: 'test-identity', courseBucket: 'test-course', blogBucket: 'test-blog',
        corsKey: 'smoke/this-run/cors.png',
        mongoExec() { return ''; },
        minio: { async removeObject() {} }
    });
}

test('course cleanup must report MongoDB deletion failure', async t => {
    const f = fixture();
    f.courses.push('owned-course');
    f.mongoExec = () => { throw new Error('SIMULATED_MONGO_FAILURE'); };
    t.mock.method(console, 'warn', () => {});
    await assert.rejects(() => f.cleanupCourse('owned-course'),
        'cleanup cannot succeed when MongoDB deletion failed');
});

test('post cleanup must report MinIO deletion failure', async () => {
    const f = fixture();
    f.posts.push(123);
    f.blogDb = {
        async execute(sql) {
            return sql.startsWith('SELECT')
                ? [[{ thumbnail: 'http://localhost:9000/lms-blog-bucket/owned.png' }]]
                : [{ affectedRows: 1 }];
        }
    };
    f.minio.removeObject = async () => { throw new Error('SIMULATED_MINIO_FAILURE'); };
    await assert.rejects(() => f.cleanupPost(123),
        'cleanup cannot succeed when thumbnail deletion failed');
});

test('cleanup must reject course not owned by this fixture before deletion', async () => {
    const f = fixture();
    let attempts = 0;
    f.mongoExec = () => { attempts++; return ''; };
    let caught;
    try { await f.cleanupCourse('untracked-other-run-course'); } catch (error) { caught = error; }
    assert.equal(attempts, 0, 'untracked course must never reach deletion callback');
    assert.ok(caught, 'untracked course must be rejected');
});

test('removeObjects must reject broad shared avatars prefix', async () => {
    const f = fixture();
    let removed = 0;
    f.minio = {
        async *listObjectsV2() { if (!removed) yield { name: 'avatars/unrelated-user.png' }; },
        async removeObject() { removed++; }
    };
    let caught;
    try { await f.removeObjects('avatars/'); } catch (error) { caught = error; }
    assert.equal(removed, 0, 'shared prefix must not delete another user object');
    assert.ok(caught, 'broad prefix must be rejected');
});

test('instructor created before OTP failure must remain tracked for cleanup', async t => {
    const f = fixture();
    f.gatewayUrl = 'http://unused.invalid';
    f.db = { async execute() { return [[]]; } };
    let calls = 0;
    t.mock.method(global, 'fetch', async () => ({ status: ++calls === 1 ? 200 : 503 }));
    await assert.rejects(() => f.instructorAccount(), /OTP send failed/);
    assert.equal(calls, 2);
    assert.equal(f.accounts.length, 1, 'successfully registered account must be tracked even if OTP fails');
});
