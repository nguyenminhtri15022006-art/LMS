/**
 * admin-actions.js - Client interaction helper for EduLearn LMS Admin Portal
 */

function generateSlug(text) {
    if (!text) return '';
    return text.toString()
        .toLowerCase()
        .normalize('NFD')
        .replace(/[\u0300-\u036f]/g, '')
        .replace(/[đĐ]/g, 'd')
        .replace(/[^a-z0-9\s-]/g, '')
        .trim()
        .replace(/\s+/g, '-')
        .replace(/-+/g, '-')
        .replace(/^-|-$/g, '');
}

function initAdminActions() {
    // 1. Tự động sinh slug cho Category Form
    const catInputName = document.getElementById('catInputName');
    const catInputSlug = document.getElementById('catInputSlug');
    if (catInputName && catInputSlug) {
        catInputName.addEventListener('input', () => {
            if (!catInputSlug.dataset.manualEdited) {
                catInputSlug.value = generateSlug(catInputName.value);
            }
        });
        catInputSlug.addEventListener('input', () => {
            catInputSlug.dataset.manualEdited = 'true';
        });
    }

    // 2. Tự động sinh slug cho Tag Form
    const tagInputName = document.getElementById('tagInputName');
    const tagInputSlug = document.getElementById('tagInputSlug');
    if (tagInputName && tagInputSlug) {
        tagInputName.addEventListener('input', () => {
            if (!tagInputSlug.dataset.manualEdited) {
                tagInputSlug.value = generateSlug(tagInputName.value);
            }
        });
        tagInputSlug.addEventListener('input', () => {
            tagInputSlug.dataset.manualEdited = 'true';
        });
    }

    // 3. Modal Reset Password cho User
    document.querySelectorAll('.btn-open-reset-pwd').forEach(btn => {
        btn.addEventListener('click', () => {
            const userId = btn.getAttribute('data-user-id');
            const username = btn.getAttribute('data-username') || 'Người dùng';
            const form = document.getElementById('formResetPassword');
            const targetUsernameEl = document.getElementById('resetTargetUsername');

            if (form && userId) {
                form.setAttribute('action', `/admin/users/${userId}/reset-password`);
            }
            if (targetUsernameEl) {
                targetUsernameEl.textContent = `@${username}`;
            }

            const modalEl = document.getElementById('resetPasswordModal');
            if (modalEl && window.bootstrap && window.bootstrap.Modal) {
                const modal = window.bootstrap.Modal.getOrCreateInstance(modalEl);
                modal.show();
            }
        });
    });

    // 4. Modal Khóa Khóa Học
    document.querySelectorAll('.btn-open-lock-course').forEach(btn => {
        btn.addEventListener('click', () => {
            const courseId = btn.getAttribute('data-course-id');
            const courseTitle = btn.getAttribute('data-course-title') || 'Khóa học';
            const form = document.getElementById('formLockCourse');
            const targetTitleEl = document.getElementById('lockCourseTitle');

            if (form && courseId) {
                form.setAttribute('action', `/admin/courses/${courseId}/lock`);
            }
            if (targetTitleEl) {
                targetTitleEl.textContent = courseTitle;
            }

            const modalEl = document.getElementById('lockCourseModal');
            if (modalEl && window.bootstrap && window.bootstrap.Modal) {
                const modal = window.bootstrap.Modal.getOrCreateInstance(modalEl);
                modal.show();
            }
        });
    });

    // 5. Nút Sửa Category
    document.querySelectorAll('.btn-edit-category').forEach(btn => {
        btn.addEventListener('click', () => {
            const catId = btn.getAttribute('data-cat-id');
            const catName = btn.getAttribute('data-cat-name') || '';
            const catSlug = btn.getAttribute('data-cat-slug') || '';

            prepareCategoryModal('edit', catId, catName, catSlug);

            const modalEl = document.getElementById('categoryModal');
            if (modalEl && window.bootstrap && window.bootstrap.Modal) {
                const modal = window.bootstrap.Modal.getOrCreateInstance(modalEl);
                modal.show();
            }
        });
    });

    // 6. Nút Sửa Tag
    document.querySelectorAll('.btn-edit-tag').forEach(btn => {
        btn.addEventListener('click', () => {
            const tagId = btn.getAttribute('data-tag-id');
            const tagName = btn.getAttribute('data-tag-name') || '';
            const tagSlug = btn.getAttribute('data-tag-slug') || '';

            prepareTagModal('edit', tagId, tagName, tagSlug);

            const modalEl = document.getElementById('tagModal');
            if (modalEl && window.bootstrap && window.bootstrap.Modal) {
                const modal = window.bootstrap.Modal.getOrCreateInstance(modalEl);
                modal.show();
            }
        });
    });
}

function prepareCategoryModal(mode, id, name, slug) {
    const form = document.getElementById('categoryForm');
    const titleEl = document.getElementById('categoryModalLabel');
    const inputName = document.getElementById('catInputName');
    const inputSlug = document.getElementById('catInputSlug');
    const btnSubmit = document.getElementById('btnSubmitCategory');

    if (!form) return;

    if (mode === 'edit') {
        form.setAttribute('action', `/admin/categories/${id}`);
        if (titleEl) titleEl.innerHTML = '<i class="bi bi-pencil-square text-primary me-2"></i>Chỉnh Sửa Thể Loại';
        if (inputName) inputName.value = name || '';
        if (inputSlug) {
            inputSlug.value = slug || '';
            inputSlug.dataset.manualEdited = 'true';
        }
        if (btnSubmit) btnSubmit.innerHTML = '<i class="bi bi-check-lg me-1"></i>Cập Nhật Thể Loại';
    } else {
        form.setAttribute('action', '/admin/categories');
        if (titleEl) titleEl.innerHTML = '<i class="bi bi-folder-plus text-primary me-2"></i>Thêm Thể Loại Mới';
        if (inputName) inputName.value = '';
        if (inputSlug) {
            inputSlug.value = '';
            delete inputSlug.dataset.manualEdited;
        }
        if (btnSubmit) btnSubmit.innerHTML = '<i class="bi bi-check-lg me-1"></i>Lưu Thể Loại';
    }
}

function prepareTagModal(mode, id, name, slug) {
    const form = document.getElementById('tagForm');
    const titleEl = document.getElementById('tagModalLabel');
    const inputName = document.getElementById('tagInputName');
    const inputSlug = document.getElementById('tagInputSlug');
    const btnSubmit = document.getElementById('btnSubmitTag');

    if (!form) return;

    if (mode === 'edit') {
        form.setAttribute('action', `/admin/tags/${id}`);
        if (titleEl) titleEl.innerHTML = '<i class="bi bi-pencil-square text-danger me-2"></i>Chỉnh Sửa Thẻ Tag';
        if (inputName) inputName.value = name || '';
        if (inputSlug) {
            inputSlug.value = slug || '';
            inputSlug.dataset.manualEdited = 'true';
        }
        if (btnSubmit) btnSubmit.innerHTML = '<i class="bi bi-check-lg me-1"></i>Cập Nhật Thẻ Tag';
    } else {
        form.setAttribute('action', '/admin/tags');
        if (titleEl) titleEl.innerHTML = '<i class="bi bi-tag-fill text-danger me-2"></i>Thêm Thẻ Tag Mới';
        if (inputName) inputName.value = '';
        if (inputSlug) {
            inputSlug.value = '';
            delete inputSlug.dataset.manualEdited;
        }
        if (btnSubmit) btnSubmit.innerHTML = '<i class="bi bi-check-lg me-1"></i>Lưu Thẻ Tag';
    }
}

// Auto init on DOM ready
if (typeof document !== 'undefined') {
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAdminActions);
    } else {
        initAdminActions();
    }
}

// Export for Jest / Node environment
if (typeof module !== 'undefined' && module.exports) {
    module.exports = {
        generateSlug,
        initAdminActions,
        prepareCategoryModal,
        prepareTagModal,
    };
}
