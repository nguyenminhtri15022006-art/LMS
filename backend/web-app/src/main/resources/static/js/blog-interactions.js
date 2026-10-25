/**
 * Blog Interactions JS — Xử lý bình luận lồng nhau và tương tác Vote
 * Hỗ trợ Ajax, chống XSS, bảo vệ CSRF và phản hồi tức thì trên UI.
 */
(function (global) {
    function initBlogInteractions(doc, win) {
        doc = doc || document;
        win = win || window;

        const postContainer = doc.querySelector('[data-post-id]');
        if (!postContainer) return;

        const postId = postContainer.getAttribute('data-post-id');
        if (!postId || postId === '0') return;

        // Lấy CSRF token từ meta hoặc input
        function getCsrfToken() {
            const csrfInput = doc.querySelector('input[name="_csrf"]');
            if (csrfInput) return csrfInput.value;
            const metaCsrf = doc.querySelector('meta[name="_csrf"]');
            if (metaCsrf) return metaCsrf.getAttribute('content');
            return '';
        }

        function getCsrfHeader() {
            const metaHeader = doc.querySelector('meta[name="_csrf_header"]');
            if (metaHeader) return metaHeader.getAttribute('content');
            return 'X-CSRF-TOKEN';
        }

        function getHeaders() {
            const h = {
                'Content-Type': 'application/json',
                'Accept': 'application/json'
            };
            const token = getCsrfToken();
            if (token) {
                h[getCsrfHeader()] = token;
            }
            return h;
        }

        // ==========================================
        // 1. Vote Bài Viết
        // ==========================================
        const postVoteBtns = doc.querySelectorAll('.btn-vote-post');
        postVoteBtns.forEach(btn => {
            btn.onclick = function () {
                const voteType = btn.getAttribute('data-vote-type');
                if (!voteType) return;

                btn.disabled = true;
                win.fetch(`/blog/posts/${postId}/vote`, {
                    method: 'POST',
                    headers: getHeaders(),
                    body: JSON.stringify({ voteType: voteType })
                })
                .then(res => {
                    btn.disabled = false;
                    if (!res.ok) {
                        return res.json().then(d => {
                            throw new Error(d.message || `Lỗi HTTP ${res.status}`);
                        }).catch(e => {
                            throw new Error(e.message || `Lỗi HTTP ${res.status}`);
                        });
                    }
                    return res.json();
                })
                .then(data => {
                    const voteData = data.data || data;
                    if (voteData) {
                        const upSpan = doc.querySelector('.post-upvote-count');
                        const downSpan = doc.querySelector('.post-downvote-count');
                        if (upSpan && voteData.upvoteCount !== undefined) {
                            upSpan.textContent = voteData.upvoteCount;
                        }
                        if (downSpan && voteData.downvoteCount !== undefined) {
                            downSpan.textContent = voteData.downvoteCount;
                        }
                    }
                })
                .catch(err => {
                    btn.disabled = false;
                    win.alert('Không thể thực hiện bình chọn: ' + err.message);
                });
            };
        });

        // ==========================================
        // 2. Gửi Bình Luận Cấp Gốc
        // ==========================================
        const rootCommentForm = doc.getElementById('rootCommentForm');
        const rootCommentContent = doc.getElementById('rootCommentContent');
        const btnSubmitRootComment = doc.getElementById('btnSubmitRootComment');

        if (rootCommentForm) {
            rootCommentForm.onsubmit = function (e) {
                e.preventDefault();
                const content = rootCommentContent ? rootCommentContent.value.trim() : '';
                if (!content) {
                    win.alert('Vui lòng nhập nội dung bình luận.');
                    return;
                }

                if (btnSubmitRootComment) btnSubmitRootComment.disabled = true;

                win.fetch(`/blog/posts/${postId}/comments`, {
                    method: 'POST',
                    headers: getHeaders(),
                    body: JSON.stringify({ content: content, parentId: null })
                })
                .then(res => {
                    if (btnSubmitRootComment) btnSubmitRootComment.disabled = false;
                    if (!res.ok) {
                        return res.json().then(d => {
                            throw new Error(d.message || `Lỗi HTTP ${res.status}`);
                        }).catch(e => {
                            throw new Error(e.message || `Lỗi HTTP ${res.status}`);
                        });
                    }
                    return res.json();
                })
                .then(data => {
                    const comment = data.data || data;
                    if (comment) {
                        if (rootCommentContent) rootCommentContent.value = '';
                        appendNewCommentToTree(comment);
                        updateTotalCommentsCount(1);
                    }
                })
                .catch(err => {
                    if (btnSubmitRootComment) btnSubmitRootComment.disabled = false;
                    win.alert('Lỗi gửi bình luận: ' + err.message);
                });
            };
        }

        // ==========================================
        // 3. Trả lời bình luận (Reply)
        // ==========================================
        doc.addEventListener('click', function (e) {
            const replyBtn = e.target.closest('.btn-reply-comment');
            if (replyBtn) {
                const commentCard = replyBtn.closest('.comment-card');
                if (!commentCard) return;
                const replyBox = commentCard.querySelector('.reply-box-container');
                if (replyBox) {
                    replyBox.classList.remove('d-none');
                    const input = replyBox.querySelector('.reply-input');
                    if (input) input.focus();
                }
                return;
            }

            const cancelReplyBtn = e.target.closest('.btn-cancel-reply');
            if (cancelReplyBtn) {
                const replyBox = cancelReplyBtn.closest('.reply-box-container');
                if (replyBox) {
                    replyBox.classList.add('d-none');
                    const input = replyBox.querySelector('.reply-input');
                    if (input) input.value = '';
                }
                return;
            }

            const submitReplyBtn = e.target.closest('.btn-submit-reply');
            if (submitReplyBtn) {
                const parentId = submitReplyBtn.getAttribute('data-parent-id');
                const replyBox = submitReplyBtn.closest('.reply-box-container');
                if (!replyBox) return;
                const input = replyBox.querySelector('.reply-input');
                const content = input ? input.value.trim() : '';

                if (!content) {
                    win.alert('Vui lòng nhập nội dung câu trả lời.');
                    return;
                }

                submitReplyBtn.disabled = true;

                win.fetch(`/blog/posts/${postId}/comments`, {
                    method: 'POST',
                    headers: getHeaders(),
                    body: JSON.stringify({ content: content, parentId: parentId })
                })
                .then(res => {
                    submitReplyBtn.disabled = false;
                    if (!res.ok) {
                        return res.json().then(d => {
                            throw new Error(d.message || `Lỗi HTTP ${res.status}`);
                        }).catch(e => {
                            throw new Error(e.message || `Lỗi HTTP ${res.status}`);
                        });
                    }
                    return res.json();
                })
                .then(data => {
                    const comment = data.data || data;
                    if (comment) {
                        if (input) input.value = '';
                        replyBox.classList.add('d-none');
                        appendReplyToCommentCard(submitReplyBtn.closest('.comment-card'), comment);
                        updateTotalCommentsCount(1);
                    }
                })
                .catch(err => {
                    submitReplyBtn.disabled = false;
                    win.alert('Lỗi trả lời bình luận: ' + err.message);
                });
                return;
            }

            // ==========================================
            // 4. Sửa bình luận
            // ==========================================
            const editBtn = e.target.closest('.btn-edit-comment');
            if (editBtn) {
                const commentId = editBtn.getAttribute('data-comment-id');
                const commentCard = editBtn.closest('.comment-card');
                if (!commentCard) return;
                const textEl = commentCard.querySelector('.comment-text');
                const currentText = textEl ? textEl.textContent : '';

                const newText = win.prompt('Chỉnh sửa nội dung bình luận:', currentText);
                if (newText === null || !newText.trim() || newText.trim() === currentText) return;

                win.fetch(`/blog/posts/${postId}/comments/${commentId}`, {
                    method: 'PUT',
                    headers: getHeaders(),
                    body: JSON.stringify({ content: newText.trim() })
                })
                .then(res => {
                    if (!res.ok) {
                        return res.json().then(d => {
                            throw new Error(d.message || `Lỗi HTTP ${res.status}`);
                        }).catch(e => {
                            throw new Error(e.message || `Lỗi HTTP ${res.status}`);
                        });
                    }
                    return res.json();
                })
                .then(data => {
                    const updated = data.data || data;
                    if (textEl && updated && updated.content) {
                        textEl.textContent = updated.content;
                    } else if (textEl) {
                        textEl.textContent = newText.trim();
                    }
                })
                .catch(err => {
                    win.alert('Lỗi khi sửa bình luận: ' + err.message);
                });
                return;
            }

            // ==========================================
            // 5. Xóa bình luận
            // ==========================================
            const deleteBtn = e.target.closest('.btn-delete-comment');
            if (deleteBtn) {
                const commentId = deleteBtn.getAttribute('data-comment-id');
                if (!win.confirm('Bạn có chắc chắn muốn xóa bình luận này không?')) return;

                win.fetch(`/blog/posts/${postId}/comments/${commentId}`, {
                    method: 'DELETE',
                    headers: getHeaders()
                })
                .then(res => {
                    if (!res.ok) {
                        return res.json().then(d => {
                            throw new Error(d.message || `Lỗi HTTP ${res.status}`);
                        }).catch(e => {
                            throw new Error(e.message || `Lỗi HTTP ${res.status}`);
                        });
                    }
                    return res.json();
                })
                .then(() => {
                    const commentCard = deleteBtn.closest('.comment-card');
                    if (commentCard) {
                        commentCard.remove();
                        updateTotalCommentsCount(-1);
                    }
                })
                .catch(err => {
                    win.alert('Lỗi khi xóa bình luận: ' + err.message);
                });
                return;
            }

            // ==========================================
            // 6. Vote Bình Luận
            // ==========================================
            const commentVoteBtn = e.target.closest('.btn-vote-comment');
            if (commentVoteBtn) {
                const commentId = commentVoteBtn.getAttribute('data-comment-id');
                const voteType = commentVoteBtn.getAttribute('data-vote-type');
                if (!commentId || !voteType) return;

                commentVoteBtn.disabled = true;

                win.fetch(`/blog/posts/${postId}/comments/${commentId}/vote`, {
                    method: 'POST',
                    headers: getHeaders(),
                    body: JSON.stringify({ voteType: voteType })
                })
                .then(res => {
                    commentVoteBtn.disabled = false;
                    if (!res.ok) {
                        return res.json().then(d => {
                            throw new Error(d.message || `Lỗi HTTP ${res.status}`);
                        }).catch(e => {
                            throw new Error(e.message || `Lỗi HTTP ${res.status}`);
                        });
                    }
                    return res.json();
                })
                .then(data => {
                    const voteData = data.data || data;
                    if (voteData) {
                        const commentCard = commentVoteBtn.closest('.comment-card');
                        if (commentCard) {
                            const up = commentCard.querySelector('.comment-upvote-count');
                            const down = commentCard.querySelector('.comment-downvote-count');
                            if (up && voteData.upvoteCount !== undefined) up.textContent = voteData.upvoteCount;
                            if (down && voteData.downvoteCount !== undefined) down.textContent = voteData.downvoteCount;
                        }
                    }
                })
                .catch(err => {
                    commentVoteBtn.disabled = false;
                    win.alert('Lỗi bình chọn: ' + err.message);
                });
            }
        });

        // ==========================================
        // Helper: DOM Builders (Chống XSS hoàn toàn)
        // ==========================================
        function appendNewCommentToTree(comment) {
            const container = doc.getElementById('commentsTreeContainer');
            if (!container) return;

            const noMsg = doc.getElementById('noCommentsMessage');
            if (noMsg) noMsg.remove();

            const card = buildCommentElement(comment);
            container.prepend(card);
        }

        function appendReplyToCommentCard(parentCard, replyComment) {
            if (!parentCard) return;

            let replyList = parentCard.querySelector('.nested-replies-list');
            if (!replyList) {
                replyList = doc.createElement('div');
                replyList.className = 'nested-replies-list ms-4 mt-2 pt-2 border-start ps-3 d-flex flex-column gap-2';
                parentCard.appendChild(replyList);
            }

            const card = buildCommentElement(replyComment);
            replyList.appendChild(card);
        }

        function buildCommentElement(comment) {
            const card = doc.createElement('div');
            card.className = 'comment-card border rounded-3 p-3 bg-white mb-2 shadow-xs';
            card.setAttribute('data-comment-id', comment.id);

            const header = doc.createElement('div');
            header.className = 'd-flex justify-content-between align-items-center mb-2';

            const userGroup = doc.createElement('div');
            userGroup.className = 'd-flex align-items-center gap-2';

            const icon = doc.createElement('i');
            icon.className = 'bi bi-person-circle fs-5 text-secondary';
            userGroup.appendChild(icon);

            const author = doc.createElement('span');
            author.className = 'fw-bold small';
            author.textContent = comment.authorName || 'Người dùng';
            userGroup.appendChild(author);

            const time = doc.createElement('span');
            time.className = 'text-muted small';
            time.textContent = 'Vừa xong';
            userGroup.appendChild(time);

            header.appendChild(userGroup);

            // Action buttons
            const actions = doc.createElement('div');
            actions.className = 'd-flex gap-1 align-items-center';

            const btnReply = doc.createElement('button');
            btnReply.type = 'button';
            btnReply.className = 'btn btn-sm btn-link text-decoration-none btn-reply-comment text-secondary p-0 me-2';
            btnReply.setAttribute('data-comment-id', comment.id);
            btnReply.innerHTML = '<i class="bi bi-reply me-1"></i>Trả lời';
            actions.appendChild(btnReply);

            const btnEdit = doc.createElement('button');
            btnEdit.type = 'button';
            btnEdit.className = 'btn btn-sm btn-link text-decoration-none btn-edit-comment text-primary p-0 me-2';
            btnEdit.setAttribute('data-comment-id', comment.id);
            btnEdit.innerHTML = '<i class="bi bi-pencil me-1"></i>Sửa';
            actions.appendChild(btnEdit);

            const btnDel = doc.createElement('button');
            btnDel.type = 'button';
            btnDel.className = 'btn btn-sm btn-link text-decoration-none btn-delete-comment text-danger p-0';
            btnDel.setAttribute('data-comment-id', comment.id);
            btnDel.innerHTML = '<i class="bi bi-trash me-1"></i>Xóa';
            actions.appendChild(btnDel);

            header.appendChild(actions);
            card.appendChild(header);

            // Content
            const p = doc.createElement('p');
            p.className = 'comment-text mb-2 text-dark small';
            p.textContent = comment.content || '';
            card.appendChild(p);

            // Votes
            const voteBar = doc.createElement('div');
            voteBar.className = 'd-flex align-items-center gap-2';

            const btnUp = doc.createElement('button');
            btnUp.type = 'button';
            btnUp.className = 'btn btn-sm btn-outline-secondary py-0 px-2 btn-vote-comment';
            btnUp.setAttribute('data-vote-type', 'UPVOTE');
            btnUp.setAttribute('data-comment-id', comment.id);
            btnUp.innerHTML = '<i class="bi bi-hand-thumbs-up"></i> <span class="comment-upvote-count small">0</span>';
            voteBar.appendChild(btnUp);

            const btnDown = doc.createElement('button');
            btnDown.type = 'button';
            btnDown.className = 'btn btn-sm btn-outline-secondary py-0 px-2 btn-vote-comment';
            btnDown.setAttribute('data-vote-type', 'DOWNVOTE');
            btnDown.setAttribute('data-comment-id', comment.id);
            btnDown.innerHTML = '<i class="bi bi-hand-thumbs-down"></i> <span class="comment-downvote-count small">0</span>';
            voteBar.appendChild(btnDown);

            card.appendChild(voteBar);

            // Reply Box
            const replyBox = doc.createElement('div');
            replyBox.className = 'reply-box-container mt-3 d-none';
            replyBox.innerHTML = `
                <div class="card border rounded-2 p-2 bg-light">
                    <textarea class="form-control form-control-sm mb-2 reply-input" rows="2" placeholder="Nhập câu trả lời..."></textarea>
                    <div class="d-flex justify-content-end gap-2">
                        <button type="button" class="btn btn-sm btn-light btn-cancel-reply">Hủy</button>
                        <button type="button" class="btn btn-sm btn-primary btn-submit-reply" data-parent-id="${comment.id}">Gửi trả lời</button>
                    </div>
                </div>
            `;
            card.appendChild(replyBox);

            return card;
        }

        function updateTotalCommentsCount(diff) {
            const countEl = doc.getElementById('totalCommentsCount');
            if (countEl) {
                const current = parseInt(countEl.textContent, 10) || 0;
                countEl.textContent = Math.max(0, current + diff);
            }
        }
    }

    if (typeof document !== 'undefined') {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => initBlogInteractions(document, window));
        } else {
            initBlogInteractions(document, window);
        }
    }

    if (typeof module !== 'undefined' && module.exports) {
        module.exports = { initBlogInteractions };
    } else {
        global.initBlogInteractions = initBlogInteractions;
    }
})(typeof window !== 'undefined' ? window : this);
