/**
 * learning-hub.js - Quản lý tương tác phòng học:
 * 1. Đánh dấu hoàn thành bài học, cập nhật % tiến độ, số bài xong và icon checkmark từ dữ liệu backend
 * 2. Tải và hiển thị modal Lịch học với đủ 4 trạng thái: Loading, Empty, Success, Error
 * 3. Tải và hiển thị modal Thông báo giảng viên với 4 trạng thái và phân trang (Next/Prev)
 */
function initLearningHub(doc, win) {
    const window = win || globalThis;
    const document = doc || window.document;

    // API strings are data, never HTML. Only application-owned markup uses innerHTML.
    function textElement(tag, className, text) {
        const node = document.createElement(tag);
        node.className = className;
        node.textContent = text == null ? '' : String(text);
        return node;
    }

    function meetingUrl(raw) {
        if (typeof raw !== 'string' || /[\u0000-\u0020\u007f\\<>"']/.test(raw)) return null;
        try {
            const url = new URL(raw);
            if (!['http:', 'https:'].includes(url.protocol) || !url.hostname || url.username || url.password) return null;
            return url.href;
        } catch (_) {
            return null;
        }
    }

    const completeBtn = document.getElementById('completeLessonBtn');
    const completeBtnText = document.getElementById('completeBtnText');
    const completedBadge = document.getElementById('completedBadge');
    const completeAlert = document.getElementById('completeAlert');
    const completeAlertMsg = document.getElementById('completeAlertMsg');
    const completeSuccessAlert = document.getElementById('completeSuccessAlert');

    const courseProgressBar = document.getElementById('courseProgressBar');
    const progressPercentText = document.getElementById('progressPercentText');
    const completedLessonsCount = document.getElementById('completedLessonsCount');

    const courseIdInput = document.getElementById('courseId');
    const lessonIdInput = document.getElementById('lessonId');
    const lessonTypeInput = document.getElementById('lessonType');
    const csrfInput = document.querySelector('input[name="_csrf"]');

    if (!courseIdInput) return;

    const courseId = courseIdInput.value;
    const lessonId = lessonIdInput ? lessonIdInput.value : '';
    const lessonType = lessonTypeInput && lessonTypeInput.value ? lessonTypeInput.value : 'DOCUMENT';
    const csrfToken = csrfInput ? csrfInput.value : '';

    // ==========================================
    // 1. XỬ LÝ HOÀN THÀNH BÀI HỌC (COMPLETE LESSON)
    // ==========================================
    if (completeBtn && lessonId) {
        completeBtn.addEventListener('click', async function () {
            completeBtn.disabled = true;
            const originalHtml = completeBtnText ? completeBtnText.innerHTML : 'Đánh Dấu Hoàn Thành';
            if (completeBtnText) {
                completeBtnText.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>Đang lưu...';
            }

            if (completeAlert) completeAlert.classList.add('d-none');
            if (completeSuccessAlert) completeSuccessAlert.classList.add('d-none');

            try {
                const response = await window.fetch(`/learn/${courseId}/lessons/${lessonId}/complete`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'X-CSRF-TOKEN': csrfToken
                    },
                    body: JSON.stringify({ lessonType: lessonType })
                });

                const data = await response.json().catch(function () { return {}; });

                if (response.ok) {
                    // Cập nhật trạng thái nút và badge
                    completeBtn.classList.add('d-none');
                    if (completedBadge) completedBadge.classList.remove('d-none');
                    if (completeSuccessAlert) completeSuccessAlert.classList.remove('d-none');

                    // Cập nhật checkmark cho bài học trong mục lục
                    const activeLessonLink = document.querySelector(`a[data-lesson-id="${lessonId}"]`);
                    if (activeLessonLink) {
                        const checkContainer = activeLessonLink.querySelector('.lesson-check-icon');
                        if (checkContainer && !checkContainer.querySelector('.bi-check-circle-fill')) {
                            checkContainer.innerHTML = '<i class="bi bi-check-circle-fill text-success ms-2"></i>';
                        }
                    }

                    // Cập nhật tiến độ học tập từ dữ liệu trả về của backend
                    if (data && data.data) {
                        const p = data.data;
                        if (p.percentComplete !== undefined && p.percentComplete !== null) {
                            const percentVal = Math.round(Number(p.percentComplete));
                            if (courseProgressBar) {
                                courseProgressBar.style.width = percentVal + '%';
                                courseProgressBar.setAttribute('aria-valuenow', percentVal);
                            }
                            if (progressPercentText) {
                                progressPercentText.textContent = percentVal;
                            }
                        }
                        if (p.completedLessons !== undefined && p.completedLessons !== null) {
                            if (completedLessonsCount) {
                                completedLessonsCount.textContent = p.completedLessons;
                            }
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
    }

    // ==========================================
    // 2. MODAL LỊCH HỌC (SCHEDULES)
    // ==========================================
    const schedulesModal = document.getElementById('schedulesModal');
    const schedulesLoading = document.getElementById('schedulesLoading');
    const schedulesError = document.getElementById('schedulesError');
    const schedulesErrorMsg = document.getElementById('schedulesErrorMsg');
    const schedulesEmpty = document.getElementById('schedulesEmpty');
    const schedulesContent = document.getElementById('schedulesContent');
    const schedulesRetryBtn = document.getElementById('schedulesRetryBtn');

    async function loadSchedules() {
        if (!schedulesLoading) return;
        schedulesLoading.classList.remove('d-none');
        if (schedulesError) schedulesError.classList.add('d-none');
        if (schedulesEmpty) schedulesEmpty.classList.add('d-none');
        if (schedulesContent) {
            schedulesContent.classList.add('d-none');
            schedulesContent.innerHTML = '';
        }

        try {
            const res = await window.fetch(`/courses/${courseId}/schedules`);
            if (!res.ok) throw new Error('HTTP ' + res.status);
            const body = await res.json();
            const list = Array.isArray(body) ? body : (body && body.data ? body.data : []);

            schedulesLoading.classList.add('d-none');

            if (!list || list.length === 0) {
                if (schedulesEmpty) schedulesEmpty.classList.remove('d-none');
            } else {
                if (schedulesContent) {
                    const items = list.map(function (schedule) {
                        const item = textElement('div', 'list-group-item px-0 py-3 border-bottom', '');
                        item.append(textElement('h6', 'fw-bold mb-1', schedule.title || 'Buổi học'));
                        if (schedule.note) {
                            const note = textElement('p', 'text-muted small mb-2', schedule.note);
                            note.style.whiteSpace = 'pre-wrap';
                            item.append(note);
                        }
                        const safeUrl = meetingUrl(schedule.meetingUrl);
                        if (safeUrl) {
                            const link = textElement('a', 'btn btn-sm btn-outline-primary', 'Tham gia phòng học');
                            link.href = safeUrl;
                            link.target = '_blank';
                            link.rel = 'noopener noreferrer';
                            item.append(link);
                        }
                        return item;
                    });
                    schedulesContent.replaceChildren(...items);
                    schedulesContent.classList.remove('d-none');
                }
            }
        } catch (err) {
            if (schedulesLoading) schedulesLoading.classList.add('d-none');
            if (schedulesError) {
                if (schedulesErrorMsg) schedulesErrorMsg.textContent = 'Lỗi kết nối khi tải lịch học.';
                schedulesError.classList.remove('d-none');
            }
        }
    }

    if (schedulesModal) {
        schedulesModal.addEventListener('show.bs.modal', function () {
            loadSchedules();
        });
    }
    if (schedulesRetryBtn) {
        schedulesRetryBtn.addEventListener('click', function () {
            loadSchedules();
        });
    }

    // ==========================================
    // 3. MODAL THÔNG BÁO GIẢNG VIÊN (ANNOUNCEMENTS)
    // ==========================================
    const announcementsModal = document.getElementById('announcementsModal');
    const announcementsLoading = document.getElementById('announcementsLoading');
    const announcementsError = document.getElementById('announcementsError');
    const announcementsErrorMsg = document.getElementById('announcementsErrorMsg');
    const announcementsEmpty = document.getElementById('announcementsEmpty');
    const announcementsContent = document.getElementById('announcementsContent');
    const announcementsRetryBtn = document.getElementById('announcementsRetryBtn');
    const announcementsPagination = document.getElementById('announcementsPagination');
    const announcementsPageInfo = document.getElementById('announcementsPageInfo');
    const announcementsPrevBtn = document.getElementById('announcementsPrevBtn');
    const announcementsNextBtn = document.getElementById('announcementsNextBtn');

    let announcementsCurrentPage = 0;
    let announcementsTotalPages = 1;

    async function loadAnnouncements(page) {
        announcementsCurrentPage = page || 0;
        if (!announcementsLoading) return;
        announcementsLoading.classList.remove('d-none');
        if (announcementsError) announcementsError.classList.add('d-none');
        if (announcementsEmpty) announcementsEmpty.classList.add('d-none');
        if (announcementsContent) {
            announcementsContent.classList.add('d-none');
            announcementsContent.innerHTML = '';
        }
        if (announcementsPagination) announcementsPagination.classList.add('d-none');

        try {
            const res = await window.fetch(`/courses/${courseId}/announcements?page=${announcementsCurrentPage}&size=10`);
            if (!res.ok) throw new Error('HTTP ' + res.status);
            const body = await res.json();
            const dataObj = body && body.data ? body.data : {};
            const content = Array.isArray(dataObj.content) ? dataObj.content : (Array.isArray(body) ? body : []);
            announcementsTotalPages = dataObj.totalPages !== undefined ? dataObj.totalPages : 1;

            announcementsLoading.classList.add('d-none');

            if (!content || content.length === 0) {
                if (announcementsEmpty) announcementsEmpty.classList.remove('d-none');
            } else {
                if (announcementsContent) {
                    const items = content.map(function (announcement) {
                        const item = textElement('div', 'list-group-item px-0 py-3 border-bottom', '');
                        const header = textElement('div', 'd-flex justify-content-between align-items-center mb-1', '');
                        header.append(textElement('h6', 'fw-bold mb-0', announcement.title || 'Thông báo'));
                        if (announcement.createdAt) {
                            header.append(textElement('span', 'text-muted small', announcement.createdAt));
                        }
                        const message = textElement('p', 'text-muted small mb-0', announcement.content);
                        message.style.whiteSpace = 'pre-wrap';
                        item.append(header, message);
                        return item;
                    });
                    announcementsContent.replaceChildren(...items);
                    announcementsContent.classList.remove('d-none');
                }

                if (announcementsPagination && announcementsTotalPages > 1) {
                    announcementsPagination.classList.remove('d-none');
                    if (announcementsPageInfo) {
                        announcementsPageInfo.textContent = `Trang ${announcementsCurrentPage + 1} / ${announcementsTotalPages}`;
                    }
                    if (announcementsPrevBtn) {
                        announcementsPrevBtn.disabled = (announcementsCurrentPage <= 0);
                    }
                    if (announcementsNextBtn) {
                        announcementsNextBtn.disabled = (announcementsCurrentPage >= announcementsTotalPages - 1);
                    }
                }
            }
        } catch (err) {
            if (announcementsLoading) announcementsLoading.classList.add('d-none');
            if (announcementsError) {
                if (announcementsErrorMsg) announcementsErrorMsg.textContent = 'Lỗi kết nối khi tải thông báo.';
                announcementsError.classList.remove('d-none');
            }
        }
    }

    if (announcementsModal) {
        announcementsModal.addEventListener('show.bs.modal', function () {
            loadAnnouncements(0);
        });
    }
    if (announcementsRetryBtn) {
        announcementsRetryBtn.addEventListener('click', function () {
            loadAnnouncements(announcementsCurrentPage);
        });
    }
    if (announcementsPrevBtn) {
        announcementsPrevBtn.addEventListener('click', function () {
            if (announcementsCurrentPage > 0) {
                loadAnnouncements(announcementsCurrentPage - 1);
            }
        });
    }
    if (announcementsNextBtn) {
        announcementsNextBtn.addEventListener('click', function () {
            if (announcementsCurrentPage < announcementsTotalPages - 1) {
                loadAnnouncements(announcementsCurrentPage + 1);
            }
        });
    }

    return {
        loadSchedules: loadSchedules,
        loadAnnouncements: loadAnnouncements
    };
}

if (typeof document !== 'undefined') {
    document.addEventListener('DOMContentLoaded', function () {
        initLearningHub(document, window);
    });
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = { initLearningHub };
}
