const { test, beforeEach } = require('node:test');
const assert = require('node:assert/strict');
const { JSDOM } = require('jsdom');
const {
    generateSlug,
    initAdminActions,
    prepareCategoryModal,
    prepareTagModal
} = require('../../main/resources/static/js/admin-actions.js');

let dom, doc, win;

const sampleHtml = `
<!DOCTYPE html>
<html>
<body>
    <!-- Category Form Elements -->
    <input type="text" id="catInputName" />
    <input type="text" id="catInputSlug" />
    <form id="categoryForm" action="/admin/categories">
        <h5 id="categoryModalLabel">Thêm Thể Loại Mới</h5>
        <button id="btnSubmitCategory" type="submit">Lưu Thể Loại</button>
    </form>

    <!-- Tag Form Elements -->
    <input type="text" id="tagInputName" />
    <input type="text" id="tagInputSlug" />
    <form id="tagForm" action="/admin/tags">
        <h5 id="tagModalLabel">Thêm Thẻ Tag Mới</h5>
        <button id="btnSubmitTag" type="submit">Lưu Thẻ Tag</button>
    </form>

    <!-- Reset Password Elements -->
    <button class="btn-open-reset-pwd" data-user-id="u-888" data-username="john_doe">Reset</button>
    <form id="formResetPassword" action="/admin/users/placeholder/reset-password">
        <span id="resetTargetUsername"></span>
    </form>
    <div id="resetPasswordModal"></div>

    <!-- Lock Course Elements -->
    <button class="btn-open-lock-course" data-course-id="c-999" data-course-title="Học Lập Trình Go">Lock</button>
    <form id="formLockCourse" action="/admin/courses/placeholder/lock">
        <span id="lockCourseTitle"></span>
    </form>
    <div id="lockCourseModal"></div>

    <!-- Edit Category Elements -->
    <button class="btn-edit-category" data-cat-id="12" data-cat-name="Khoa Học Dữ Liệu" data-cat-slug="khoa-hoc-du-lieu">Edit Cat</button>
    <div id="categoryModal"></div>

    <!-- Edit Tag Elements -->
    <button class="btn-edit-tag" data-tag-id="45" data-tag-name="Trí Tuệ Nhân Tạo" data-tag-slug="tri-tue-nhan-tao">Edit Tag</button>
    <div id="tagModal"></div>
</body>
</html>
`;

beforeEach(() => {
    dom = new JSDOM(sampleHtml, { url: 'https://lms.test/admin' });
    win = dom.window;
    doc = win.document;
    global.document = doc;
    global.window = win;

    // Mock bootstrap Modal
    win.bootstrap = {
        Modal: {
            getOrCreateInstance: () => ({
                show: () => {}
            })
        }
    };
});

test('1. generateSlug correctly formats Vietnamese string into slug', () => {
    assert.strictEqual(generateSlug('Lập Trình Web Full-Stack 2026!'), 'lap-trinh-web-full-stack-2026');
    assert.strictEqual(generateSlug('Đồ Án Tốt Nghiệp & AI'), 'do-an-tot-nghiep-ai');
    assert.strictEqual(generateSlug(''), '');
    assert.strictEqual(generateSlug(null), '');
});

test('2. Cat and Tag auto slug generates when typing in name input', () => {
    initAdminActions();

    const catName = doc.getElementById('catInputName');
    const catSlug = doc.getElementById('catInputSlug');
    catName.value = 'Học Máy & Deep Learning';
    catName.dispatchEvent(new win.Event('input'));

    assert.strictEqual(catSlug.value, 'hoc-may-deep-learning');

    const tagName = doc.getElementById('tagInputName');
    const tagSlug = doc.getElementById('tagInputSlug');
    tagName.value = 'Spring Boot 3';
    tagName.dispatchEvent(new win.Event('input'));

    assert.strictEqual(tagSlug.value, 'spring-boot-3');
});

test('3. Reset password button updates form action URL and displays target username', () => {
    initAdminActions();

    const btn = doc.querySelector('.btn-open-reset-pwd');
    btn.click();

    const form = doc.getElementById('formResetPassword');
    const targetUsername = doc.getElementById('resetTargetUsername');

    assert.strictEqual(form.getAttribute('action'), '/admin/users/u-888/reset-password');
    assert.strictEqual(targetUsername.textContent, '@john_doe');
});

test('4. Lock course button updates form action URL and displays target course title', () => {
    initAdminActions();

    const btn = doc.querySelector('.btn-open-lock-course');
    btn.click();

    const form = doc.getElementById('formLockCourse');
    const title = doc.getElementById('lockCourseTitle');

    assert.strictEqual(form.getAttribute('action'), '/admin/courses/c-999/lock');
    assert.strictEqual(title.textContent, 'Học Lập Trình Go');
});

test('5. Edit Category prepares category modal for update', () => {
    initAdminActions();

    const btn = doc.querySelector('.btn-edit-category');
    btn.click();

    const form = doc.getElementById('categoryForm');
    const inputName = doc.getElementById('catInputName');
    const inputSlug = doc.getElementById('catInputSlug');

    assert.strictEqual(form.getAttribute('action'), '/admin/categories/12');
    assert.strictEqual(inputName.value, 'Khoa Học Dữ Liệu');
    assert.strictEqual(inputSlug.value, 'khoa-hoc-du-lieu');
});

test('6. Edit Tag prepares tag modal for update', () => {
    initAdminActions();

    const btn = doc.querySelector('.btn-edit-tag');
    btn.click();

    const form = doc.getElementById('tagForm');
    const inputName = doc.getElementById('tagInputName');
    const inputSlug = doc.getElementById('tagInputSlug');

    assert.strictEqual(form.getAttribute('action'), '/admin/tags/45');
    assert.strictEqual(inputName.value, 'Trí Tuệ Nhân Tạo');
    assert.strictEqual(inputSlug.value, 'tri-tue-nhan-tao');
});
