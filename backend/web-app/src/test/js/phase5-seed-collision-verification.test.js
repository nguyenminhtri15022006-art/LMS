// Live MySQL checks: explicitly invoked, session-local TEMPORARY tables only.
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const mysql = require('mysql2/promise');
const sql = fs.readFileSync(path.resolve(__dirname, '../../../../../docker/mysql/init/02-demo-seed-data.sql'), 'utf8');
async function connect() {
    assert.ok(process.env.MYSQL_ROOT_PASSWORD, 'requires local environment credentials');
    return mysql.createConnection({ host: '127.0.0.1', port: 3307, user: 'root', password: process.env.MYSQL_ROOT_PASSWORD, database: 'course_lms' });
}

test('category ID collision must fail explicitly or still create every intended category', async () => {
    const db = await connect();
    try {
        await db.query('CREATE TEMPORARY TABLE categories (id BIGINT PRIMARY KEY, name VARCHAR(255), slug VARCHAR(255) UNIQUE, created_at DATETIME)');
        await db.execute('INSERT INTO categories (id, name, slug) VALUES (9001, ?, ?)', ['EXISTING_CATEGORY', 'existing-unrelated-slug']);
        const statement = sql.match(/INSERT INTO `categories`[\s\S]*?;/)[0];
        let rejected = false;
        try { await db.query(statement); } catch { rejected = true; }
        const [original] = await db.query('SELECT name FROM categories WHERE id = 9001');
        assert.equal(original[0].name, 'EXISTING_CATEGORY');
        if (!rejected) {
            const [rows] = await db.execute('SELECT id FROM categories WHERE slug = ?', ['cong-nghe-thong-tin']);
            assert.equal(rows.length, 1, 'successful SQL must not silently omit a required demo category');
        }
    } finally { await db.end(); }
});

test('post seed must not overwrite another authors post sharing its slug', async () => {
    const db = await connect();
    try {
        await db.query('CREATE TEMPORARY TABLE posts (id BIGINT PRIMARY KEY, title VARCHAR(500), slug VARCHAR(500) UNIQUE, summary TEXT, content LONGTEXT, author_id VARCHAR(36), status VARCHAR(32), view_count BIGINT, created_at DATETIME, updated_at DATETIME)');
        const statement = sql.match(/INSERT INTO `posts`[\s\S]*?;/)[0];
        const slug = 'huong-dan-xay-dung-kien-truc-microservices-chuan-doanh-nghiep-voi-spring-boot-3';
        await db.execute('INSERT INTO posts (id, title, slug, content, author_id) VALUES (123, ?, ?, ?, ?)', ['EXISTING_TITLE', slug, 'EXISTING_CONTENT', 'unrelated-author']);
        try { await db.query(statement); } catch { /* Refusing the conflict is safe; still verify preservation. */ }
        const [rows] = await db.query('SELECT title, content, author_id FROM posts WHERE id = 123');
        assert.equal(rows[0].author_id, 'unrelated-author');
        assert.equal(rows[0].title, 'EXISTING_TITLE', 'matching slug does not prove demo ownership');
        assert.equal(rows[0].content, 'EXISTING_CONTENT');
    } finally { await db.end(); }
});
