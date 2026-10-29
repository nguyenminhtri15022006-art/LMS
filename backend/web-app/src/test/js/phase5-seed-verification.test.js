const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const mysql = require('mysql2/promise');

const root = path.resolve(__dirname, '../../../../..');
const scriptPath = path.join(root, 'scripts/seed-demo-data.cjs');
const sqlPath = path.join(root, 'docker/mysql/init/02-demo-seed-data.sql');
const source = fs.readFileSync(scriptPath, 'utf8');

function runSeedWithFakeDocker(execSync) {
    const processState = {
        env: { DEMO_USER_PASSWORD: 'isolated-test-password', MYSQL_ROOT_PASSWORD: 'isolated-placeholder' },
        exitCode: 0,
        exit(code) { this.exitCode = code; throw new Error('EXIT'); }
    };
    const context = {
        __dirname: path.dirname(scriptPath), process: processState,
        console: { log() {}, warn() {}, error() {} },
        require(name) {
            if (name === 'node:child_process') return { execSync };
            if (name === 'node:fs') return fs;
            if (name === 'node:path') return path;
            if (name === './bcrypt.js') return require(path.join(root, 'scripts/bcrypt.js'));
            throw new Error('Unexpected dependency');
        }
    };
    try { vm.runInNewContext(source, context); }
    catch (error) { if (error.message !== 'EXIT') throw error; }
    return processState;
}

test('seed must signal nonzero exit when database commands fail', () => {
    let calls = 0;
    const state = runSeedWithFakeDocker(() => { calls++; throw new Error('SIMULATED_DATABASE_FAILURE'); });
    assert.ok(calls > 0, 'must reach database commands, not fail on missing password or dependency');
    assert.notEqual(state.exitCode, 0, 'failed seed must not signal success to shell or CI');
});

test('each seeded lesson must belong to the course whose structure references it', () => {
    let mongoScript;
    runSeedWithFakeDocker((command, options) => {
        if (command.includes('mongosh')) mongoScript = options.input;
        return '';
    });
    assert.ok(mongoScript, 'must capture generated Mongo script without running Docker');
    const structures = [], lessons = [];
    vm.runInNewContext(mongoScript, {
        db: { getSiblingDB() { return {
            course_structures: { replaceOne(filter, doc) { structures.push(doc); } },
            lessons: { replaceOne(filter, doc) { lessons.push(doc); } }
        }; } }, print() {}
    });
    const mismatches = [];
    for (const structure of structures) {
        for (const node of structure.nodes.filter(n => n.lessonId)) {
            const lesson = lessons.find(l => l._id === node.lessonId);
            if (!lesson || lesson.courseId !== structure.courseId) {
                mismatches.push({ lesson: node.lessonId, expected: structure.courseId, actual: lesson?.courseId });
            }
        }
    }
    assert.deepEqual(mismatches, [], 'every referenced lesson must have the correct courseId');
});

test('category seed must preserve an unrelated existing category with the same numeric ID', async () => {
    // TEMPORARY table shadows only this connection; production table is never touched.
    assert.ok(process.env.MYSQL_ROOT_PASSWORD, 'requires local environment credentials');
    const db = await mysql.createConnection({
        host: '127.0.0.1', port: 3307, user: 'root',
        password: process.env.MYSQL_ROOT_PASSWORD, database: 'course_lms'
    });
    try {
        await db.execute('CREATE TEMPORARY TABLE categories (id BIGINT PRIMARY KEY, name VARCHAR(255), slug VARCHAR(255) UNIQUE, created_at DATETIME)');
        await db.execute('INSERT INTO categories (id, name, slug) VALUES (1, ?, ?)', ['UNCHANGED_EXISTING_CATEGORY', 'unrelated-existing-category']);
        const sql = fs.readFileSync(sqlPath, 'utf8');
        const statement = sql.match(/INSERT INTO `categories`[\s\S]*?;/)?.[0];
        assert.ok(statement, 'expected category seed statement');
        await db.query(statement);
        const [rows] = await db.execute('SELECT name, slug FROM categories WHERE id = 1');
        assert.equal(rows[0].name, 'UNCHANGED_EXISTING_CATEGORY', 'seed must not overwrite an unrelated row by numeric ID');
    } finally { await db.end(); }
});
