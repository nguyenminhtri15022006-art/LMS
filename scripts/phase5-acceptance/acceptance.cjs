const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const { randomBytes } = require('node:crypto');
const root = process.cwd();
const artifactDir = path.join(root, '.codex-report-review/phase5');
fs.mkdirSync(artifactDir, { recursive:true });
const mysql = require(path.join(root, 'backend/web-app/node_modules/mysql2/promise'));
const { chromium } = require(path.join(root, 'backend/web-app/node_modules/@playwright/test'));
const bcrypt = require(path.join(root,'scripts/bcrypt.js'));
const passwordPath = path.join(artifactDir,'demo-password');
const password = fs.existsSync(passwordPath) ? fs.readFileSync(passwordPath,'utf8') : randomBytes(24).toString('base64url');
fs.writeFileSync(passwordPath,password);
const cfg = JSON.parse(fs.readFileSync(path.join(artifactDir,'compose.json'),'utf8'));
const admin = cfg.services['identity-service'].environment;
function mongo(expr) { return JSON.parse(execFileSync('docker',['exec','lms-phase5-acceptance-mongodb','mongosh','--quiet','--eval',`const d=db.getSiblingDB('course_lms'); print(JSON.stringify(${expr}))`],{encoding:'utf8'}).trim()); }
function seed(pass) {
    const output = execFileSync(process.execPath,['--require',path.join(__dirname,'redirect-seed.cjs'),'scripts/seed-demo-data.cjs'], { cwd:root, env:{...process.env,DEMO_USER_PASSWORD:pass}, encoding:'utf8' });
    assert.ok(output.includes('HOÀN TẤT NẠP DỮ LIỆU DEMO'));
}
async function main() {
    assert.equal(execFileSync('docker',['inspect','lms-phase5-acceptance-mysql','--format','{{.State.Running}}'],{encoding:'utf8'}).trim(),'true');
    const db = await mysql.createConnection({ host:'127.0.0.1',port:3307,user:'root',password:process.env.MYSQL_ROOT_PASSWORD });
    let browser;
    const report = { coldStart:'fresh isolated volumes', seedRuns:0, ui:[], checks:[] };
    try {
        const [tables] = await db.query("SELECT TABLE_SCHEMA,TABLE_NAME FROM information_schema.tables WHERE TABLE_SCHEMA IN ('identity_lms','course_lms','blog_lms') AND TABLE_TYPE='BASE TABLE' ORDER BY TABLE_SCHEMA,TABLE_NAME");
        assert.ok(tables.length > 15, 'microservice migrations must have created schema before seed');
        await db.execute("INSERT INTO course_lms.categories (name,slug,created_at) SELECT 'P5 sentinel preserve','p5-sentinel-preserve',NOW() WHERE NOT EXISTS (SELECT 1 FROM course_lms.categories WHERE slug='p5-sentinel-preserve')");
        seed(password); report.seedRuns++;
        async function snapshot() {
            const counts = {};
            for (const t of tables) {
                const [rows] = await db.query(`SELECT COUNT(*) n FROM \`${t.TABLE_SCHEMA}\`.\`${t.TABLE_NAME}\``);
                counts[`${t.TABLE_SCHEMA}.${t.TABLE_NAME}`] = rows[0].n;
            }
            const [users] = await db.query("SELECT user_id,user_name,password_hash,role FROM identity_lms.users WHERE user_name IN ('instructor_demo','student_demo') ORDER BY user_name");
            return { counts, users };
        }
        const first = await snapshot();
        assert.equal(first.users.length,2);
        for (const u of first.users) assert.ok(bcrypt.compareSync(password,u.password_hash),'demo hash matches configured password');
        seed(randomBytes(24).toString('base64url')); report.seedRuns++;
        const second = await snapshot();
        assert.deepEqual(second, first, 'second seed must not duplicate rows or reset existing password/role');
        const [sentinel] = await db.query("SELECT name FROM course_lms.categories WHERE slug='p5-sentinel-preserve'");
        assert.equal(sentinel[0].name,'P5 sentinel preserve');
        report.checks.push('repeat seed preserves counts/passwords/roles/sentinel');
        const structures = mongo('d.course_structures.find({}).toArray()');
        const lessons = mongo('d.lessons.find({}).toArray()');
        assert.equal(structures.length,2); assert.equal(lessons.length,5);
        for (const s of structures) for (const node of s.nodes.filter(n=>n.lessonId)) assert.equal(lessons.find(l=>l._id===node.lessonId)?.courseId,s.courseId);
        report.checks.push('2 structures / 5 lessons / all course references valid');
        const [courses] = await db.query('SELECT id,title FROM course_lms.courses ORDER BY id');
        const [posts] = await db.query('SELECT id,slug,title FROM blog_lms.posts ORDER BY id');
        assert.equal(courses.length,2); assert.equal(posts.length,2);
        browser = await chromium.launch({ headless:true });
        async function login(username,pass) {
            console.log('Browser login: '+username);
            const ctx=await browser.newContext(); const page=await ctx.newPage(); page.setDefaultTimeout(20000);
            await page.goto('http://localhost:8088/login');
            await page.fill('#username',username); await page.fill('#password',pass);
            try { await Promise.all([page.waitForURL(u=>u.pathname==='/' && !u.search),page.click('button[type=submit]')]); }
            catch(error) { await page.screenshot({path:path.join(artifactDir,'login-failure.png')}); throw new Error('Login UI failed for '+username+': '+(await page.locator('body').innerText()).slice(-700)); }
            return {ctx,page};
        }
        const student = await login('student_demo',password);
        try {
            const page=student.page;
            await page.goto('http://localhost:8088/courses');
            for(const c of courses) assert.ok((await page.locator('body').innerText()).includes(c.title));
            for(const c of courses) {
                await page.goto(`http://localhost:8088/courses/${c.id}`);
                const enroll=page.locator('form[action$="/enroll"] button[type=submit]');
                if(await enroll.count()) await Promise.all([page.waitForURL(u=>u.pathname.startsWith('/learn/')),enroll.click()]);
                for(const l of lessons.filter(l=>l.courseId===c.id)) {
                    const response=await page.goto(`http://localhost:8088/learn/${c.id}/lessons/${l._id}`);
                    assert.equal(response.status(),200);
                    const node=structures.find(s=>s.courseId===c.id).nodes.find(n=>n.lessonId===l._id);
                    assert.ok((await page.locator('body').innerText()).includes(node.title),`structure lesson title ${l._id}`);
                    assert.ok((await page.locator('.markdown-body').innerText()).length>25,`real markdown ${l._id}`);
                }
            }
            report.ui.push('student login/catalog/2 courses/5 rendered lessons');
            for(const post of posts) {
                await page.goto(`http://localhost:8088/blog/${post.slug}`);
                assert.ok((await page.locator('body').innerText()).includes(post.title));
            }
            await page.screenshot({path:path.join(artifactDir,'blog.png'),fullPage:true});
            report.ui.push('2 blog posts rendered');
        } finally { await student.ctx.close(); }
        const teacher=await login('instructor_demo',password);
        try {
            const response=await teacher.page.goto('http://localhost:8088/instructor/courses');
            assert.equal(response.status(),200);
            for(const c of courses) assert.ok((await teacher.page.locator('body').innerText()).includes(c.title));
            await teacher.page.screenshot({path:path.join(artifactDir,'instructor.png'),fullPage:true});
            report.ui.push('instructor login/studio shows both owned courses');
        } finally { await teacher.ctx.close(); }
        const manager=await login(admin.BOOTSTRAP_ADMIN_USERNAMES.split(',')[0],admin.BOOTSTRAP_ADMIN_PASSWORDS.split(',')[0]);
        try {
            const response=await manager.page.goto('http://localhost:8088/admin');
            assert.equal(response.status(),200); assert.ok(new URL(manager.page.url()).pathname.startsWith('/admin'));
            await manager.page.screenshot({path:path.join(artifactDir,'admin.png'),fullPage:true});
            report.ui.push('admin login/dashboard');
        } finally { await manager.ctx.close(); }
        report.status='PASS';
        fs.writeFileSync(path.join(artifactDir,'result.json'),JSON.stringify(report,null,2));
        console.log(JSON.stringify(report));
    } finally { if(browser) await browser.close(); await db.end(); }
}
main().catch(e=>{console.error(e.message);process.exitCode=1;});
