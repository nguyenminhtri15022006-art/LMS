/**
 * Curriculum Builder JS — Quản lý cây bài giảng trực quan trong Studio Giảng Viên
 * Hỗ trợ cấu trúc đa cấp MongoDB, sắp xếp reorder, bảo vệ an toàn XSS và xử lý trạng thái.
 */
(function (global) {
    function initCurriculumBuilder(doc, win) {
        doc = doc || document;
        win = win || window;

        const rootContainer = doc.getElementById('managementCenterContent');
        if (!rootContainer) return;

        const courseId = rootContainer.getAttribute('data-course-id');
        const csrfToken = rootContainer.getAttribute('data-csrf-token') || '';
        const csrfHeader = rootContainer.getAttribute('data-csrf-header') || 'X-CSRF-TOKEN';

        const curriculumTreeList = doc.getElementById('curriculumTreeList');
        const curriculumLoading = doc.getElementById('curriculumLoading');
        const curriculumEmptyState = doc.getElementById('curriculumEmptyState');
        const curriculumErrorState = doc.getElementById('curriculumErrorState');
        const curriculumErrorMessage = doc.getElementById('curriculumErrorMessage');
        const curriculumRetryBtn = doc.getElementById('curriculumRetryBtn');

        const btnAddChapter = doc.getElementById('btnAddChapter');
        const btnAddLesson = doc.getElementById('btnAddLesson');
        const nodeModalEl = doc.getElementById('nodeModal');
        const nodeModal = (win.bootstrap && nodeModalEl) ? new win.bootstrap.Modal(nodeModalEl) : null;
        const nodeModalTitle = doc.getElementById('nodeModalTitle');
        const nodeTitleInput = doc.getElementById('nodeTitleInput');
        const nodeTypeInput = doc.getElementById('nodeTypeInput');
        const nodeParentSelect = doc.getElementById('nodeParentSelect');
        const btnSaveNode = doc.getElementById('btnSaveNode');

        const lessonModalEl = doc.getElementById('lessonContentModal');
        const lessonModal = (win.bootstrap && lessonModalEl) ? new win.bootstrap.Modal(lessonModalEl) : null;
        const lessonIdInput = doc.getElementById('lessonIdInput');
        const lessonVideoUrl = doc.getElementById('lessonVideoUrl');
        const lessonDocumentContent = doc.getElementById('lessonDocumentContent');
        const typeVideoRadio = doc.getElementById('typeVideo');
        const typeDocumentRadio = doc.getElementById('typeDocument');
        const videoUrlGroup = doc.getElementById('videoUrlGroup');
        const documentContentGroup = doc.getElementById('documentContentGroup');
        const btnSaveLessonContent = doc.getElementById('btnSaveLessonContent');

        let currentNodes = [];

        function getHeaders() {
            const h = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
            if (csrfToken) h[csrfHeader] = csrfToken;
            return h;
        }

        function showAlert(type, message) {
            const alertBox = doc.getElementById('alertMessageContainer');
            if (!alertBox) return;
            alertBox.textContent = '';

            const alertDiv = doc.createElement('div');
            alertDiv.className = `alert alert-${type} alert-dismissible fade show rounded-3 mb-4 shadow-sm`;
            alertDiv.setAttribute('role', 'alert');

            const icon = doc.createElement('i');
            icon.className = `bi ${type === 'success' ? 'bi-check-circle-fill' : 'bi-exclamation-triangle-fill'} me-2`;
            alertDiv.appendChild(icon);

            const textSpan = doc.createElement('span');
            textSpan.textContent = message;
            alertDiv.appendChild(textSpan);

            const closeBtn = doc.createElement('button');
            closeBtn.type = 'button';
            closeBtn.className = 'btn-close';
            closeBtn.setAttribute('data-bs-dismiss', 'alert');
            closeBtn.setAttribute('aria-label', 'Close');
            closeBtn.onclick = () => alertDiv.remove();
            alertDiv.appendChild(closeBtn);

            alertBox.appendChild(alertDiv);
            if (typeof win.scrollTo === 'function') {
                win.scrollTo({ top: 0, behavior: 'smooth' });
            }
        }

        // Toggle radio Video / Document
        if (typeVideoRadio && typeDocumentRadio) {
            typeVideoRadio.addEventListener('change', () => {
                if (typeVideoRadio.checked) {
                    if (videoUrlGroup) videoUrlGroup.style.display = 'block';
                    if (documentContentGroup) documentContentGroup.style.display = 'none';
                }
            });
            typeDocumentRadio.addEventListener('change', () => {
                if (typeDocumentRadio.checked) {
                    if (videoUrlGroup) videoUrlGroup.style.display = 'none';
                    if (documentContentGroup) documentContentGroup.style.display = 'block';
                }
            });
        }

        if (curriculumRetryBtn) {
            curriculumRetryBtn.onclick = function () {
                loadCurriculum();
            };
        }

        // Tải cấu trúc cây giáo trình qua endpoint JSON BFF chuẩn
        function loadCurriculum() {
            if (curriculumLoading) curriculumLoading.classList.remove('d-none');
            if (curriculumEmptyState) curriculumEmptyState.classList.add('d-none');
            if (curriculumErrorState) curriculumErrorState.classList.add('d-none');
            if (curriculumTreeList) curriculumTreeList.textContent = '';

            win.fetch(`/instructor/courses/${courseId}/structure`, {
                headers: { 'Accept': 'application/json' }
            })
            .then(res => {
                if (!res.ok) {
                    throw new Error(`Mã phản hồi HTTP: ${res.status}`);
                }
                return res.json();
            })
            .then(res => {
                if (curriculumLoading) curriculumLoading.classList.add('d-none');
                const nodes = (res && res.data && Array.isArray(res.data.nodes)) ? res.data.nodes : [];
                currentNodes = nodes;
                renderTreeUI(nodes);
            })
            .catch(err => {
                if (curriculumLoading) curriculumLoading.classList.add('d-none');
                if (curriculumEmptyState) curriculumEmptyState.classList.add('d-none');
                if (curriculumErrorState) {
                    curriculumErrorState.classList.remove('d-none');
                    if (curriculumErrorMessage) {
                        curriculumErrorMessage.textContent = 'Không thể tải cấu trúc bài giảng: ' + err.message;
                    }
                }
            });
        }

        function renderTreeUI(nodes) {
            if (!curriculumTreeList) return;
            curriculumTreeList.textContent = '';

            if (!nodes || nodes.length === 0) {
                if (curriculumEmptyState) curriculumEmptyState.classList.remove('d-none');
                return;
            }
            if (curriculumEmptyState) curriculumEmptyState.classList.add('d-none');

            // Cập nhật select parent trong modal với các folder
            const folders = nodes.filter(n => (n.type || '').toLowerCase() === 'folder');
            if (nodeParentSelect) {
                nodeParentSelect.textContent = '';
                const defaultOpt = doc.createElement('option');
                defaultOpt.value = '';
                defaultOpt.textContent = '-- Cấp ngoài cùng (Chương độc lập) --';
                nodeParentSelect.appendChild(defaultOpt);

                folders.forEach(f => {
                    const opt = doc.createElement('option');
                    opt.value = f.id;
                    opt.textContent = '📁 ' + (f.title || 'Chương không tên');
                    nodeParentSelect.appendChild(opt);
                });
            }

            // Dựng cây đệ quy theo parentId
            const rootNodes = nodes.filter(n => !n.parentId || n.parentId === '')
                                   .sort((a, b) => (a.order || 0) - (b.order || 0));

            rootNodes.forEach((node, index) => {
                const nodeEl = buildNodeElement(node, nodes, index, rootNodes);
                curriculumTreeList.appendChild(nodeEl);
            });
        }

        function buildNodeElement(node, allNodes, index, siblingList) {
            const isFolder = (node.type || '').toLowerCase() === 'folder';

            if (isFolder) {
                const card = doc.createElement('div');
                card.className = 'card border rounded-3 p-3 bg-light shadow-sm mb-2';
                card.setAttribute('data-node-id', node.id);

                const header = doc.createElement('div');
                header.className = 'd-flex justify-content-between align-items-center';

                const titleGroup = doc.createElement('div');
                titleGroup.className = 'd-flex align-items-center gap-2';

                const icon = doc.createElement('i');
                icon.className = 'bi bi-folder2-open text-primary fs-5';
                titleGroup.appendChild(icon);

                const titleSpan = doc.createElement('span');
                titleSpan.className = 'fw-bold fs-6';
                titleSpan.textContent = node.title || 'Chương không tên';
                titleGroup.appendChild(titleSpan);

                const childNodes = allNodes.filter(n => n.parentId === node.id)
                                           .sort((a, b) => (a.order || 0) - (b.order || 0));

                const badge = doc.createElement('span');
                badge.className = 'badge bg-secondary-subtle text-secondary small';
                badge.textContent = `${childNodes.length} mục con`;
                titleGroup.appendChild(badge);

                header.appendChild(titleGroup);

                // Action buttons
                const btnGroup = doc.createElement('div');
                btnGroup.className = 'd-flex gap-1 align-items-center';

                // Move up / down
                if (siblingList && siblingList.length > 1) {
                    if (index > 0) {
                        const btnUp = doc.createElement('button');
                        btnUp.type = 'button';
                        btnUp.className = 'btn btn-sm btn-outline-secondary btn-move-up';
                        btnUp.title = 'Di chuyển lên';
                        btnUp.innerHTML = '<i class="bi bi-arrow-up"></i>';
                        btnUp.onclick = () => reorderSiblings(siblingList, index, index - 1);
                        btnGroup.appendChild(btnUp);
                    }
                    if (index < siblingList.length - 1) {
                        const btnDown = doc.createElement('button');
                        btnDown.type = 'button';
                        btnDown.className = 'btn btn-sm btn-outline-secondary btn-move-down';
                        btnDown.title = 'Di chuyển xuống';
                        btnDown.innerHTML = '<i class="bi bi-arrow-down"></i>';
                        btnDown.onclick = () => reorderSiblings(siblingList, index, index + 1);
                        btnGroup.appendChild(btnDown);
                    }
                }

                // Add child lesson
                const btnAddChild = doc.createElement('button');
                btnAddChild.type = 'button';
                btnAddChild.className = 'btn btn-sm btn-outline-primary btn-add-child-lesson';
                btnAddChild.title = 'Thêm bài học vào chương này';
                btnAddChild.innerHTML = '<i class="bi bi-plus-lg me-1"></i>Thêm bài';
                btnAddChild.onclick = () => {
                    if (nodeModalTitle) nodeModalTitle.textContent = 'Thêm Bài Học Vào Chương';
                    if (nodeTypeInput) nodeTypeInput.value = 'lesson';
                    if (nodeTitleInput) nodeTitleInput.value = '';
                    if (nodeParentSelect) nodeParentSelect.value = node.id;
                    if (nodeModal) nodeModal.show();
                };
                btnGroup.appendChild(btnAddChild);

                // Edit title
                const btnEditTitle = doc.createElement('button');
                btnEditTitle.type = 'button';
                btnEditTitle.className = 'btn btn-sm btn-outline-secondary btn-edit-title';
                btnEditTitle.title = 'Sửa tên chương';
                btnEditTitle.innerHTML = '<i class="bi bi-pencil"></i>';
                btnEditTitle.onclick = () => editNodeTitle(node.id, node.title);
                btnGroup.appendChild(btnEditTitle);

                // Delete node
                const btnDelete = doc.createElement('button');
                btnDelete.type = 'button';
                btnDelete.className = 'btn btn-sm btn-outline-danger btn-delete-node';
                btnDelete.title = 'Xóa chương';
                btnDelete.innerHTML = '<i class="bi bi-trash"></i>';
                btnDelete.onclick = () => deleteNode(node.id);
                btnGroup.appendChild(btnDelete);

                header.appendChild(btnGroup);
                card.appendChild(header);

                // Container for children
                const childContainer = doc.createElement('div');
                childContainer.className = `child-lessons-container mt-2 pt-2 border-top d-flex flex-column gap-1 ${childNodes.length === 0 ? 'd-none' : ''}`;

                childNodes.forEach((child, cIdx) => {
                    const childEl = buildNodeElement(child, allNodes, cIdx, childNodes);
                    childContainer.appendChild(childEl);
                });

                card.appendChild(childContainer);
                return card;
            } else {
                // Standalone or child lesson
                const item = doc.createElement('div');
                item.className = 'd-flex justify-content-between align-items-center p-2 rounded-2 bg-white border border-light-subtle shadow-xs mb-1';
                item.setAttribute('data-node-id', node.id);
                if (node.lessonId) item.setAttribute('data-lesson-id', node.lessonId);

                const leftGroup = doc.createElement('div');
                leftGroup.className = 'd-flex align-items-center gap-2';

                const isVideo = (node.lessonType || '').toUpperCase() === 'VIDEO';
                const lesIcon = doc.createElement('i');
                lesIcon.className = `bi ${isVideo ? 'bi-play-circle-fill text-danger' : 'bi-file-text-fill text-primary'}`;
                leftGroup.appendChild(lesIcon);

                const lesTitle = doc.createElement('span');
                lesTitle.className = 'small fw-medium';
                lesTitle.textContent = node.title || 'Bài học không tên';
                leftGroup.appendChild(lesTitle);

                const typeBadge = doc.createElement('span');
                typeBadge.className = `badge ${isVideo ? 'bg-danger-subtle text-danger' : 'bg-primary-subtle text-primary'} small py-0 px-2`;
                typeBadge.textContent = isVideo ? 'Video' : 'Tài liệu';
                leftGroup.appendChild(typeBadge);

                item.appendChild(leftGroup);

                // Right buttons
                const rightGroup = doc.createElement('div');
                rightGroup.className = 'd-flex gap-1 align-items-center';

                // Move up / down for lessons
                if (siblingList && siblingList.length > 1) {
                    if (index > 0) {
                        const btnUp = doc.createElement('button');
                        btnUp.type = 'button';
                        btnUp.className = 'btn btn-sm btn-outline-secondary btn-move-up';
                        btnUp.title = 'Di chuyển lên';
                        btnUp.innerHTML = '<i class="bi bi-arrow-up"></i>';
                        btnUp.onclick = () => reorderSiblings(siblingList, index, index - 1);
                        rightGroup.appendChild(btnUp);
                    }
                    if (index < siblingList.length - 1) {
                        const btnDown = doc.createElement('button');
                        btnDown.type = 'button';
                        btnDown.className = 'btn btn-sm btn-outline-secondary btn-move-down';
                        btnDown.title = 'Di chuyển xuống';
                        btnDown.innerHTML = '<i class="bi bi-arrow-down"></i>';
                        btnDown.onclick = () => reorderSiblings(siblingList, index, index + 1);
                        rightGroup.appendChild(btnDown);
                    }
                }

                // Edit lesson content
                const btnEditContent = doc.createElement('button');
                btnEditContent.type = 'button';
                btnEditContent.className = 'btn btn-sm btn-light border btn-edit-lesson';
                btnEditContent.title = 'Soạn nội dung bài học';
                btnEditContent.innerHTML = '<i class="bi bi-pencil-square text-primary"></i> Soạn nội dung';
                btnEditContent.onclick = () => openLessonContentModal(node.lessonId);
                rightGroup.appendChild(btnEditContent);

                // Edit lesson title
                const btnEditLesTitle = doc.createElement('button');
                btnEditLesTitle.type = 'button';
                btnEditLesTitle.className = 'btn btn-sm btn-light border btn-edit-title';
                btnEditLesTitle.title = 'Sửa tiêu đề bài học';
                btnEditLesTitle.innerHTML = '<i class="bi bi-pencil text-secondary"></i>';
                btnEditLesTitle.onclick = () => editNodeTitle(node.id, node.title);
                rightGroup.appendChild(btnEditLesTitle);

                // Delete lesson
                const btnDeleteLes = doc.createElement('button');
                btnDeleteLes.type = 'button';
                btnDeleteLes.className = 'btn btn-sm btn-light border btn-delete-node';
                btnDeleteLes.title = 'Xóa bài học';
                btnDeleteLes.innerHTML = '<i class="bi bi-trash text-danger"></i>';
                btnDeleteLes.onclick = () => deleteNode(node.id);
                rightGroup.appendChild(btnDeleteLes);

                item.appendChild(rightGroup);
                return item;
            }
        }

        // Hoán đổi thứ tự và gửi ReorderNodesRequest
        function reorderSiblings(siblings, fromIdx, toIdx) {
            const reordered = [...siblings];
            const temp = reordered[fromIdx];
            reordered[fromIdx] = reordered[toIdx];
            reordered[toIdx] = temp;

            const nodesPayload = reordered.map((item, idx) => ({
                nodeId: item.id,
                parentId: item.parentId || null,
                order: idx + 1
            }));

            win.fetch(`/instructor/courses/${courseId}/structure/reorder`, {
                method: 'PUT',
                headers: getHeaders(),
                body: JSON.stringify({ nodes: nodesPayload })
            })
            .then(res => {
                if (!res.ok) {
                    return res.json().then(data => {
                        throw new Error(data.message || `Lỗi HTTP ${res.status}`);
                    }).catch(e => {
                        throw new Error(e.message || `Lỗi HTTP ${res.status}`);
                    });
                }
                return res.json();
            })
            .then(() => {
                showAlert('success', 'Đã cập nhật thứ tự giáo trình.');
                loadCurriculum();
            })
            .catch(err => {
                showAlert('danger', 'Không thể sắp xếp lại: ' + err.message);
                loadCurriculum(); // Rollback UI
            });
        }

        function editNodeTitle(nodeId, currentTitle) {
            const newTitle = win.prompt('Nhập tiêu đề mới:', currentTitle || '');
            if (!newTitle || !newTitle.trim() || newTitle.trim() === currentTitle) return;

            win.fetch(`/instructor/courses/${courseId}/structure/nodes/${nodeId}`, {
                method: 'PATCH',
                headers: getHeaders(),
                body: JSON.stringify({ title: newTitle.trim() })
            })
            .then(res => {
                if (!res.ok) {
                    return res.json().then(data => {
                        throw new Error(data.message || `Lỗi HTTP ${res.status}`);
                    }).catch(e => {
                        throw new Error(e.message || `Lỗi HTTP ${res.status}`);
                    });
                }
                return res.json();
            })
            .then(() => {
                showAlert('success', 'Đã cập nhật tiêu đề thành công.');
                loadCurriculum();
            })
            .catch(err => {
                showAlert('danger', 'Lỗi khi cập nhật tiêu đề: ' + err.message);
            });
        }

        function deleteNode(nodeId) {
            if (!win.confirm('Bạn có chắc chắn muốn xóa mục này khỏi giáo trình?')) return;

            win.fetch(`/instructor/courses/${courseId}/structure/nodes/${nodeId}`, {
                method: 'DELETE',
                headers: getHeaders()
            })
            .then(res => {
                if (!res.ok) {
                    return res.json().then(data => {
                        throw new Error(data.message || `Lỗi HTTP ${res.status}`);
                    }).catch(e => {
                        throw new Error(e.message || `Lỗi HTTP ${res.status}`);
                    });
                }
                return res.json();
            })
            .then(() => {
                showAlert('success', 'Đã xóa mục khỏi cây bài giảng.');
                loadCurriculum();
            })
            .catch(err => {
                showAlert('danger', 'Lỗi khi xóa: ' + err.message);
            });
        }

        function openLessonContentModal(lessonId) {
            if (!lessonId) {
                win.alert('Bài học này chưa có mã lessonId.');
                return;
            }
            if (lessonIdInput) lessonIdInput.value = lessonId;
            if (lessonVideoUrl) lessonVideoUrl.value = '';
            if (lessonDocumentContent) lessonDocumentContent.value = '';

            win.fetch(`/instructor/courses/${courseId}/lessons/${lessonId}`, {
                headers: { 'Accept': 'application/json' }
            })
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(res => {
                const lData = res.data || res;
                const lType = (lData.lessonType || 'VIDEO').toUpperCase();
                if (lType === 'VIDEO') {
                    if (typeVideoRadio) typeVideoRadio.checked = true;
                    if (videoUrlGroup) videoUrlGroup.style.display = 'block';
                    if (documentContentGroup) documentContentGroup.style.display = 'none';
                    if (lessonVideoUrl && lData.content && lData.content.videoUrl) {
                        lessonVideoUrl.value = lData.content.videoUrl;
                    }
                } else {
                    if (typeDocumentRadio) typeDocumentRadio.checked = true;
                    if (videoUrlGroup) videoUrlGroup.style.display = 'none';
                    if (documentContentGroup) documentContentGroup.style.display = 'block';
                    if (lessonDocumentContent && lData.content && lData.content.text) {
                        lessonDocumentContent.value = lData.content.text;
                    }
                }
                if (lessonModal) lessonModal.show();
            })
            .catch(() => {
                if (lessonModal) lessonModal.show();
            });
        }

        // Trigger buttons
        if (btnAddChapter) {
            btnAddChapter.onclick = function () {
                if (nodeModalTitle) nodeModalTitle.textContent = 'Thêm Chương Mục Mới';
                if (nodeTypeInput) nodeTypeInput.value = 'folder';
                if (nodeTitleInput) nodeTitleInput.value = '';
                if (nodeParentSelect) nodeParentSelect.value = '';
                if (nodeModal) nodeModal.show();
            };
        }

        if (btnAddLesson) {
            btnAddLesson.onclick = function () {
                if (nodeModalTitle) nodeModalTitle.textContent = 'Thêm Bài Học Mới';
                if (nodeTypeInput) nodeTypeInput.value = 'lesson';
                if (nodeTitleInput) nodeTitleInput.value = '';
                if (nodeModal) nodeModal.show();
            };
        }

        if (btnSaveNode) {
            btnSaveNode.onclick = function () {
                const title = nodeTitleInput ? nodeTitleInput.value.trim() : '';
                if (!title) {
                    win.alert('Vui lòng nhập tiêu đề');
                    return;
                }
                const type = nodeTypeInput ? nodeTypeInput.value : 'folder';
                const parentId = nodeParentSelect ? (nodeParentSelect.value || null) : null;

                win.fetch(`/instructor/courses/${courseId}/structure/nodes`, {
                    method: 'POST',
                    headers: getHeaders(),
                    body: JSON.stringify({
                        type: type,
                        title: title,
                        parentId: parentId,
                        order: currentNodes.length + 1
                    })
                })
                .then(res => {
                    if (!res.ok) {
                        return res.json().then(data => {
                            throw new Error(data.message || `Lỗi HTTP ${res.status}`);
                        }).catch(e => {
                            throw new Error(e.message || `Lỗi HTTP ${res.status}`);
                        });
                    }
                    return res.json();
                })
                .then(() => {
                    if (nodeModal) nodeModal.hide();
                    showAlert('success', 'Thêm mục mới thành công!');
                    loadCurriculum();
                })
                .catch(err => {
                    // GIỮ FORM, KHÔNG ĐÓNG MODAL KHI THẤT BẠI
                    showAlert('danger', 'Lỗi khi thêm mục: ' + err.message);
                });
            };
        }

        if (btnSaveLessonContent) {
            btnSaveLessonContent.onclick = function () {
                const lId = lessonIdInput ? lessonIdInput.value : '';
                const isVideo = typeVideoRadio ? typeVideoRadio.checked : true;
                const lessonType = isVideo ? 'VIDEO' : 'DOCUMENT';
                const content = {};

                if (isVideo) {
                    content.videoUrl = lessonVideoUrl ? lessonVideoUrl.value.trim() : '';
                } else {
                    content.text = lessonDocumentContent ? lessonDocumentContent.value.trim() : '';
                }

                win.fetch(`/instructor/courses/${courseId}/lessons/${lId}`, {
                    method: 'PUT',
                    headers: getHeaders(),
                    body: JSON.stringify({
                        lessonType: lessonType,
                        content: content
                    })
                })
                .then(res => {
                    if (!res.ok) {
                        return res.json().then(data => {
                            throw new Error(data.message || `Lỗi HTTP ${res.status}`);
                        }).catch(e => {
                            throw new Error(e.message || `Lỗi HTTP ${res.status}`);
                        });
                    }
                    return res.json();
                })
                .then(() => {
                    if (lessonModal) lessonModal.hide();
                    showAlert('success', 'Đã lưu nội dung bài học thành công!');
                })
                .catch(err => {
                    // GIỮ FORM, KHÔNG ĐÓNG MODAL KHI THẤT BẠI
                    showAlert('danger', 'Lỗi lưu bài học: ' + err.message);
                });
            };
        }

        // Tải cây lần đầu
        loadCurriculum();
    }

    if (typeof document !== 'undefined') {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => initCurriculumBuilder(document, window));
        } else {
            initCurriculumBuilder(document, window);
        }
    }

    if (typeof module !== 'undefined' && module.exports) {
        module.exports = { initCurriculumBuilder };
    } else {
        global.initCurriculumBuilder = initCurriculumBuilder;
    }
})(typeof window !== 'undefined' ? window : this);
