// Explicit live MySQL check; SELECT only, no application data changes.
const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { execSync } = require('node:child_process');
const root = path.resolve(__dirname, '../../../../..');
test('seed MySQL client preserves Vietnamese UTF-8 bytes', () => {
    assert.ok(process.env.MYSQL_ROOT_PASSWORD, 'requires database credentials');
    let mysqlCommand;
    vm.runInNewContext(fs.readFileSync(path.join(root,'scripts/seed-demo-data.cjs'),'utf8'), {
        __dirname:path.join(root,'scripts'),
        process:{env:{DEMO_USER_PASSWORD:'isolated-test-password',MYSQL_ROOT_PASSWORD:process.env.MYSQL_ROOT_PASSWORD}},
        console:{log(){},error(){}},
        require(name) {
            if(name==='node:child_process') return {execSync(cmd){if(cmd.includes('mysql -uroot')) mysqlCommand=cmd;return '';}};
            if(name==='node:fs') return fs;
            if(name==='node:path') return path;
            if(name==='./bcrypt.js') return require(path.join(root,'scripts/bcrypt.js'));
            throw new Error('Unexpected dependency');
        }
    });
    assert.ok(mysqlCommand);
    const container=process.env.SEED_TEST_MYSQL_CONTAINER || 'lms-mysql';
    assert.match(container,/^[a-zA-Z0-9_-]+$/);
    mysqlCommand=mysqlCommand.replace(/\blms-mysql\b/g,container);
    const value='Tiếng Việt: Khóa học';
    const output=execSync(mysqlCommand+' --batch --skip-column-names',{
        input:`SELECT HEX(CONVERT('${value}' USING utf8mb4));`,encoding:'utf8',
        env:{...process.env,MYSQL_PWD:process.env.MYSQL_ROOT_PASSWORD}
    });
    assert.equal(output.trim().toLowerCase(),Buffer.from(value,'utf8').toString('hex'),'MySQL must interpret seed input as UTF-8');
});
