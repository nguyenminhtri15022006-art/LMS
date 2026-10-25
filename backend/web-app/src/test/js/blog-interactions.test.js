const { test, beforeEach, afterEach } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { JSDOM } = require('jsdom');
const { initBlogInteractions } = require('../../main/resources/static/js/blog-interactions.js');

const fixtureDir = path.resolve(__dirname, '../../../target/ui-fixtures');
assert.ok(fs.existsSync(path.join(fixtureDir, 'blog-detail.html')),
    'Run mvn test -pl web-app -f backend/pom.xml first to render the Thymeleaf fixture.');
const html = fs.readFileSync(path.join(fixtureDir, 'blog-detail.html'), 'utf8');

let dom, doc, win, calls;
const response = (data, ok = true, status = ok ? 200 : 400) => ({
    ok,
    status,
    json: async () => data
});
const el = id => doc.getElementById(id);

async function until(check) {
    for (let i = 0; i < 50; i++) {
        if (check()) return;
        await new Promise(resolve => setTimeout(resolve, 10));
    }
    assert.ok(check(), 'Expected DOM condition timed out');
}

beforeEach(async () => {
    dom = new JSDOM(html, { url: 'https://lms.test/blog/kien-truc-microservices', runScripts: 'outside-only' });
    win = dom.window;
    doc = win.document;
    if (doc.readyState === 'loading') {
        await new Promise(resolve => doc.addEventListener('DOMContentLoaded', resolve, { once: true }));
    }
    calls = [];
    win.alert = () => {};
    win.confirm = () => true;
    win.prompt = () => 'Bình luận đã cập nhật';
    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        return response({ data: {} });
    };
    initBlogInteractions(doc, win);
});

afterEach(() => dom.window.close());

test('vote post sends POST /blog/posts/{id}/vote with UPVOTE and updates vote counts', async () => {
    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        return response({
            data: {
                upvoteCount: 43,
                downvoteCount: 3,
                currentVote: 'UPVOTE'
            }
        });
    };

    const upvoteBtn = doc.querySelector('.btn-vote-post[data-vote-type="UPVOTE"]');
    assert.ok(upvoteBtn, 'Upvote button should exist');

    upvoteBtn.click();
    await until(() => calls.length > 0);

    assert.equal(calls[0].url, '/blog/posts/50/vote');
    assert.equal(calls[0].options.method, 'POST');
    assert.deepEqual(JSON.parse(calls[0].options.body), { voteType: 'UPVOTE' });

    await until(() => doc.querySelector('.post-upvote-count').textContent === '43');
    assert.equal(doc.querySelector('.post-upvote-count').textContent, '43');
});

test('vote post failure does not crash and re-enables button', async () => {
    let alertMessage = '';
    win.alert = (msg) => { alertMessage = msg; };
    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        return response({ message: 'Bạn chưa đăng nhập' }, false, 401);
    };

    const upvoteBtn = doc.querySelector('.btn-vote-post[data-vote-type="UPVOTE"]');
    upvoteBtn.click();
    await until(() => alertMessage !== '');

    assert.match(alertMessage, /Bạn chưa đăng nhập/);
    assert.equal(upvoteBtn.disabled, false);
});

test('submit root comment sends POST /blog/posts/{id}/comments and appends new card to tree', async () => {
    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        return response({
            data: {
                id: 999,
                content: 'Bình luận mới từ kiểm thử',
                authorName: 'Tester',
                upvoteCount: 0,
                downvoteCount: 0
            }
        });
    };

    const input = el('rootCommentContent');
    input.value = 'Bình luận mới từ kiểm thử';

    el('rootCommentForm').dispatchEvent(new win.Event('submit', { cancelable: true }));
    await until(() => calls.length > 0);

    assert.equal(calls[0].url, '/blog/posts/50/comments');
    assert.equal(calls[0].options.method, 'POST');
    assert.deepEqual(JSON.parse(calls[0].options.body), { content: 'Bình luận mới từ kiểm thử', parentId: null });

    await until(() => doc.querySelector('[data-comment-id="999"]'));
    const newCard = doc.querySelector('[data-comment-id="999"]');
    assert.ok(newCard, 'New comment card should be appended');
    assert.equal(newCard.querySelector('.comment-text').textContent, 'Bình luận mới từ kiểm thử');
    assert.equal(newCard.querySelector('.fw-bold').textContent, 'Tester');
    assert.equal(el('totalCommentsCount').textContent, '4'); // ban đầu là 3 (cha, con, cháu) -> tăng lên 4
});

test('reply comment opens reply box and sends parentId', async () => {
    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        return response({
            data: {
                id: 1001,
                content: 'Câu trả lời lồng nhau',
                authorName: 'Sub-Tester',
                upvoteCount: 0,
                downvoteCount: 0
            }
        });
    };

    const commentCard = doc.querySelector('[data-comment-id="501"]');
    assert.ok(commentCard);

    // 1. Nhấn nút Trả lời
    commentCard.querySelector('.btn-reply-comment').click();
    const replyBox = commentCard.querySelector('.reply-box-container');
    assert.equal(replyBox.classList.contains('d-none'), false);

    // 2. Nhập nội dung câu trả lời và submit
    const replyInput = replyBox.querySelector('.reply-input');
    replyInput.value = 'Câu trả lời lồng nhau';
    replyBox.querySelector('.btn-submit-reply').click();

    await until(() => calls.length > 0);
    assert.equal(calls[0].url, '/blog/posts/50/comments');
    assert.equal(calls[0].options.method, 'POST');
    assert.deepEqual(JSON.parse(calls[0].options.body), {
        content: 'Câu trả lời lồng nhau',
        parentId: '501'
    });

    await until(() => doc.querySelector('[data-comment-id="1001"]'));
    const replyCard = doc.querySelector('[data-comment-id="1001"]');
    assert.ok(replyCard);
    assert.equal(replyCard.querySelector('.comment-text').textContent, 'Câu trả lời lồng nhau');
    assert.equal(replyBox.classList.contains('d-none'), true);
});

test('edit comment prompts new text and sends PUT', async () => {
    win.prompt = () => 'Nội dung bình luận sau khi sửa';
    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        return response({
            data: {
                id: 501,
                content: 'Nội dung bình luận sau khi sửa'
            }
        });
    };

    const editBtn = doc.querySelector('[data-comment-id="501"] .btn-edit-comment');
    editBtn.click();

    await until(() => calls.length > 0);
    assert.equal(calls[0].url, '/blog/posts/50/comments/501');
    assert.equal(calls[0].options.method, 'PUT');
    assert.deepEqual(JSON.parse(calls[0].options.body), { content: 'Nội dung bình luận sau khi sửa' });

    await until(() => doc.querySelector('[data-comment-id="501"] .comment-text').textContent === 'Nội dung bình luận sau khi sửa');
    assert.equal(doc.querySelector('[data-comment-id="501"] .comment-text').textContent, 'Nội dung bình luận sau khi sửa');
});

test('delete comment confirms and sends DELETE, then removes element from DOM', async () => {
    win.confirm = () => true;
    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        return response({ message: 'Xóa bình luận thành công' });
    };

    const deleteBtn = doc.querySelector('[data-comment-id="501"] .btn-delete-comment');
    deleteBtn.click();

    await until(() => calls.length > 0);
    assert.equal(calls[0].url, '/blog/posts/50/comments/501');
    assert.equal(calls[0].options.method, 'DELETE');

    await until(() => doc.querySelector('[data-comment-id="501"]') === null);
    assert.equal(doc.querySelector('[data-comment-id="501"]'), null);
    assert.equal(el('totalCommentsCount').textContent, '2'); // ban đầu là 3 -> giảm còn 2
});

test('vote comment sends POST to comment vote URL and updates count', async () => {
    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        return response({
            data: {
                upvoteCount: 6,
                downvoteCount: 0,
                currentVote: 'UPVOTE'
            }
        });
    };

    const commentUpvoteBtn = doc.querySelector('[data-comment-id="501"] .btn-vote-comment[data-vote-type="UPVOTE"]');
    commentUpvoteBtn.click();

    await until(() => calls.length > 0);
    assert.equal(calls[0].url, '/blog/posts/50/comments/501/vote');
    assert.equal(calls[0].options.method, 'POST');
    assert.deepEqual(JSON.parse(calls[0].options.body), { voteType: 'UPVOTE' });

    await until(() => doc.querySelector('[data-comment-id="501"] .comment-upvote-count').textContent === '6');
    assert.equal(doc.querySelector('[data-comment-id="501"] .comment-upvote-count').textContent, '6');
});

test('reply nested child comment 502 opens its reply box and sends parentId 502', async () => {
    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        return response({
            data: {
                id: 1002,
                content: 'Câu trả lời cho bình luận con',
                authorName: 'Nest-Tester',
                upvoteCount: 0,
                downvoteCount: 0
            }
        });
    };

    const childCard = doc.querySelector('[data-comment-id="502"]');
    assert.ok(childCard, 'Child comment 502 should exist in nested tree');
    assert.equal(childCard.getAttribute('data-parent-id'), '501');

    childCard.querySelector('.btn-reply-comment').click();
    const replyBox = childCard.querySelector('.reply-box-container');
    assert.equal(replyBox.classList.contains('d-none'), false);

    const replyInput = replyBox.querySelector('.reply-input');
    replyInput.value = 'Câu trả lời cho bình luận con';
    replyBox.querySelector('.btn-submit-reply').click();

    await until(() => calls.length > 0);
    assert.equal(calls[0].url, '/blog/posts/50/comments');
    assert.equal(calls[0].options.method, 'POST');
    assert.deepEqual(JSON.parse(calls[0].options.body), {
        content: 'Câu trả lời cho bình luận con',
        parentId: '502'
    });

    await until(() => doc.querySelector('[data-comment-id="1002"]'));
    const nestedReply = doc.querySelector('[data-comment-id="1002"]');
    assert.ok(nestedReply);
    assert.equal(nestedReply.querySelector('.comment-text').textContent, 'Câu trả lời cho bình luận con');
    assert.equal(el('totalCommentsCount').textContent, '4');
});

test('edit nested child comment 502 updates text on 502 without modifying parent or grandchild', async () => {
    win.prompt = () => 'Nội dung bình luận con đã cập nhật';
    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        return response({
            data: {
                id: 502,
                content: 'Nội dung bình luận con đã cập nhật'
            }
        });
    };

    const editBtn = doc.querySelector('[data-comment-id="502"] .btn-edit-comment');
    assert.ok(editBtn);
    editBtn.click();

    await until(() => calls.length > 0);
    assert.equal(calls[0].url, '/blog/posts/50/comments/502');
    assert.equal(calls[0].options.method, 'PUT');
    assert.deepEqual(JSON.parse(calls[0].options.body), { content: 'Nội dung bình luận con đã cập nhật' });

    await until(() => doc.querySelector('[data-comment-id="502"] .comment-text').textContent === 'Nội dung bình luận con đã cập nhật');
    assert.equal(doc.querySelector('[data-comment-id="502"] .comment-text').textContent, 'Nội dung bình luận con đã cập nhật');
    // Kiểm tra comment cha (501) và cháu (503) không bị ảnh hưởng
    assert.equal(doc.querySelector('[data-comment-id="501"] .comment-text').textContent, 'Bài viết rất hay và chi tiết!');
    assert.equal(doc.querySelector('[data-comment-id="503"] .comment-text').textContent, 'Bình luận cháu đồng ý!');
});

test('delete nested grandchild comment 503 removes 503 from DOM while keeping parent 501 and child 502 intact', async () => {
    win.confirm = () => true;
    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        return response({ message: 'Xóa bình luận thành công' });
    };

    const deleteBtn = doc.querySelector('[data-comment-id="503"] .btn-delete-comment');
    assert.ok(deleteBtn);
    deleteBtn.click();

    await until(() => calls.length > 0);
    assert.equal(calls[0].url, '/blog/posts/50/comments/503');
    assert.equal(calls[0].options.method, 'DELETE');

    await until(() => doc.querySelector('[data-comment-id="503"]') === null);
    assert.equal(doc.querySelector('[data-comment-id="503"]'), null);
    // Cha và con vẫn còn nguyên vẹn trong DOM
    assert.ok(doc.querySelector('[data-comment-id="501"]'));
    assert.ok(doc.querySelector('[data-comment-id="502"]'));
    assert.equal(el('totalCommentsCount').textContent, '2');
});

test('vote nested child comment 502 updates vote counts exclusively on 502', async () => {
    win.fetch = async (url, options = {}) => {
        calls.push({ url, options });
        return response({
            data: {
                upvoteCount: 15,
                downvoteCount: 0,
                currentVote: 'UPVOTE'
            }
        });
    };

    const commentUpvoteBtn = doc.querySelector('[data-comment-id="502"] .btn-vote-comment[data-vote-type="UPVOTE"]');
    assert.ok(commentUpvoteBtn);
    commentUpvoteBtn.click();

    await until(() => calls.length > 0);
    assert.equal(calls[0].url, '/blog/posts/50/comments/502/vote');
    assert.equal(calls[0].options.method, 'POST');
    assert.deepEqual(JSON.parse(calls[0].options.body), { voteType: 'UPVOTE' });

    await until(() => doc.querySelector('[data-comment-id="502"] .comment-upvote-count').textContent === '15');
    assert.equal(doc.querySelector('[data-comment-id="502"] .comment-upvote-count').textContent, '15');
    // Vote của cha 501 không thay đổi
    assert.equal(doc.querySelector('[data-comment-id="501"] .comment-upvote-count').textContent, '5');
});
