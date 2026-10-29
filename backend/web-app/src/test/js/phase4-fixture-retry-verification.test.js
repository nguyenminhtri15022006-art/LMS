const { test } = require('node:test');
const assert = require('node:assert/strict');
const { Fixtures } = require('./support/e2e-fixtures');

// Fully initialized in-memory state; never touches a real service.
function fixture() {
    return Object.assign(Object.create(Fixtures.prototype), {
        accounts: [], courses: ['owned-course'], categories: [], tags: [], posts: [], minioObjects: [],
        cleanedAccounts: new Set(), cleanedCourses: new Set(), cleanedCategories: new Set(),
        cleanedTags: new Set(), cleanedPosts: new Set(),
        bucket: 'test-only', corsKey: 'smoke/this-run/cors.png'
    });
}

test('MongoDB error must be the actual cleanup rejection, not fixture initialization TypeError', async () => {
    const f = fixture();
    const expected = new Error('SIMULATED_MONGO_FAILURE');
    f.mongoExec = () => { throw expected; };
    await assert.rejects(() => f.cleanupCourse('owned-course'), error => error === expected);
});

test('failed cleanup must retry pending MongoDB work instead of marking course cleaned', async () => {
    const f = fixture();
    let attempts = 0;
    f.mongoExec = () => {
        attempts++;
        if (attempts === 1) throw new Error('TRANSIENT_MONGO_FAILURE');
        return '';
    };
    await assert.rejects(() => f.cleanupCourse('owned-course'), /TRANSIENT_MONGO_FAILURE/);
    await f.cleanupCourse('owned-course');
    assert.equal(attempts, 3, 'retry must execute both pending MongoDB deletions');
});

test('long prefix belonging to another run must be rejected before object deletion', async () => {
    const f = fixture();
    const foreignPrefix = 'smoke/11111111-2222-3333-4444-555555555555/';
    let removed = 0;
    f.minio = {
        async *listObjectsV2() { if (!removed) yield { name: foreignPrefix + 'other.png' }; },
        async removeObject() { removed++; }
    };
    let error;
    try { await f.removeObjects(foreignPrefix); } catch (caught) { error = caught; }
    assert.equal(removed, 0, 'prefix length does not prove ownership');
    assert.ok(error, 'foreign run prefix must be rejected');
});
