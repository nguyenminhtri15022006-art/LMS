document.addEventListener('DOMContentLoaded', function () {
    const fileInput = document.getElementById('avatarFileInput');
    const avatarImg = document.getElementById('avatarImage');
    const progressEl = document.getElementById('avatarUploadProgress');
    const msgEl = document.getElementById('avatarUploadMessage');
    const hiddenAvatarUrl = document.getElementById('profileAvatarUrl');

    if (!fileInput) return;

    fileInput.addEventListener('change', async function (e) {
        const file = e.target.files[0];
        if (!file) return;

        // Reset status
        if (progressEl) progressEl.classList.remove('d-none');
        if (msgEl) {
            msgEl.textContent = 'Đang chuẩn bị tải lên...';
            msgEl.className = 'small text-muted mb-2';
        }

        try {
            // 1. Get CSRF token
            const csrfTokenInput = document.querySelector('input[name="_csrf"]');
            const csrfToken = csrfTokenInput ? csrfTokenInput.value : '';

            // 2. Request Presigned URL from web-app BFF
            const presignedRes = await fetch('/profile/avatar/presigned-url', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': csrfToken
                },
                body: JSON.stringify({
                    fileName: file.name,
                    fileType: file.type || 'image/png',
                    folder: 'avatars'
                })
            });

            if (!presignedRes.ok) {
                throw new Error('Không thể khởi tạo chính sách tải lên MinIO');
            }

            const policy = await presignedRes.json();
            const uploadUrl = policy.uploadUrl;
            const formFields = policy.formFields || {};
            const fileUrl = policy.publicUrl || policy.fileUrl;

            // 3. Prepare Multipart Form Data for MinIO Direct Upload
            const formData = new FormData();
            Object.keys(formFields).forEach(key => {
                formData.append(key, formFields[key]);
            });
            formData.append('Content-Type', file.type || 'image/png');
            formData.append('file', file);

            if (msgEl) msgEl.textContent = 'Đang tải ảnh trực tiếp lên MinIO S3...';

            // 4. Direct Upload to MinIO
            const uploadRes = await fetch(uploadUrl, {
                method: 'POST',
                body: formData
            });

            if (uploadRes.status === 204 || uploadRes.status === 200) {
                if (msgEl) {
                    msgEl.textContent = 'Tải ảnh đại diện thành công!';
                    msgEl.className = 'small text-success fw-semibold mb-2';
                }
                if (avatarImg) avatarImg.src = fileUrl;
                if (hiddenAvatarUrl) hiddenAvatarUrl.value = fileUrl;

                // Tự động lưu profile sau khi upload thành công
                const profileForm = document.getElementById('profileForm');
                if (profileForm) {
                    profileForm.submit();
                }
            } else {
                throw new Error('MinIO phản hồi trạng thái: ' + uploadRes.status);
            }
        } catch (err) {
            console.error('Avatar upload error:', err);
            if (msgEl) {
                msgEl.textContent = 'Lỗi tải ảnh: ' + err.message;
                msgEl.className = 'small text-danger fw-semibold mb-2';
            }
        } finally {
            if (progressEl) progressEl.classList.add('d-none');
            fileInput.value = '';
        }
    });
});
