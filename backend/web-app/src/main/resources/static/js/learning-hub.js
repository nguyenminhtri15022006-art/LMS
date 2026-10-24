document.addEventListener('DOMContentLoaded', function () {
    const completeBtn = document.getElementById('completeLessonBtn');
    const completeBtnText = document.getElementById('completeBtnText');
    const completedBadge = document.getElementById('completedBadge');
    const completeAlert = document.getElementById('completeAlert');
    const completeAlertMsg = document.getElementById('completeAlertMsg');
    const completeSuccessAlert = document.getElementById('completeSuccessAlert');

    const courseIdInput = document.getElementById('courseId');
    const lessonIdInput = document.getElementById('lessonId');
    const csrfInput = document.querySelector('input[name="_csrf"]');

    if (!completeBtn || !courseIdInput || !lessonIdInput) return;

    const courseId = courseIdInput.value;
    const lessonId = lessonIdInput.value;
    const csrfToken = csrfInput ? csrfInput.value : '';

    completeBtn.addEventListener('click', async function () {
        completeBtn.disabled = true;
        const originalHtml = completeBtnText ? completeBtnText.innerHTML : 'Đánh Dấu Hoàn Thành';
        if (completeBtnText) {
            completeBtnText.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>Đang lưu...';
        }

        if (completeAlert) completeAlert.classList.add('d-none');
        if (completeSuccessAlert) completeSuccessAlert.classList.add('d-none');

        try {
            const response = await fetch(`/learn/${courseId}/lessons/${lessonId}/complete`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': csrfToken
                },
                body: JSON.stringify({ lessonType: 'DOCUMENT' })
            });

            const data = await response.json().catch(function () { return {}; });

            if (response.ok) {
                // Hiển thị badge hoàn thành và ẩn nút hoàn thành
                completeBtn.classList.add('d-none');
                if (completedBadge) completedBadge.classList.remove('d-none');
                if (completeSuccessAlert) completeSuccessAlert.classList.remove('d-none');

                // Thêm icon checkmark xanh cho bài học đang chọn trong mục lục
                const activeLessonLink = document.querySelector(`a[data-lesson-id="${lessonId}"]`);
                if (activeLessonLink) {
                    const checkContainer = activeLessonLink.querySelector('.lesson-check-icon');
                    if (checkContainer && !checkContainer.querySelector('.bi-check-circle-fill')) {
                        checkContainer.innerHTML = '<i class="bi bi-check-circle-fill text-success ms-2"></i>';
                    }
                }
            } else {
                completeBtn.disabled = false;
                if (completeBtnText) completeBtnText.innerHTML = originalHtml;
                if (completeAlert && completeAlertMsg) {
                    completeAlertMsg.textContent = data.message || 'Không thể đánh dấu hoàn thành bài học.';
                    completeAlert.classList.remove('d-none');
                }
            }
        } catch (error) {
            completeBtn.disabled = false;
            if (completeBtnText) completeBtnText.innerHTML = originalHtml;
            if (completeAlert && completeAlertMsg) {
                completeAlertMsg.textContent = 'Lỗi kết nối mạng khi hoàn thành bài học.';
                completeAlert.classList.remove('d-none');
            }
        }
    });
});
