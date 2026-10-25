/**
 * Instructor Management JS — Quản lý Học Viên, Lịch Học Trực Tuyến và Bảng Tin
 * Hỗ trợ phân trang, sửa lịch học, phân tích kết quả mời học viên, và bảo vệ an toàn XSS.
 */
(function (global) {
    function initInstructorManagement(doc, win) {
        doc = doc || document;
        win = win || window;

        const rootContainer = doc.getElementById('managementCenterContent');
        if (!rootContainer) return;

        const courseId = rootContainer.getAttribute('data-course-id');
        const csrfToken = rootContainer.getAttribute('data-csrf-token') || '';
        const csrfHeader = rootContainer.getAttribute('data-csrf-header') || 'X-CSRF-TOKEN';

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

        function formatDate(dateStr) {
            if (!dateStr) return '—';
            try {
                const d = new Date(dateStr);
                if (isNaN(d.getTime())) return dateStr;
                return d.toLocaleString('vi-VN');
            } catch {
                return dateStr;
            }
        }

        function toLocalDatetimeString(dateStr) {
            if (!dateStr) return '';
            const d = new Date(dateStr);
            if (isNaN(d.getTime())) return '';
            const pad = n => String(n).padStart(2, '0');
            return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
        }

        function isSafeUrl(url) {
            if (!url || typeof url !== 'string') return false;
            const trimmed = url.trim();
            // Chỉ cho phép URL bắt đầu bằng http:// hoặc https://
            return /^https?:\/\//i.test(trimmed);
        }

        // ==========================================
        // 1. TAB HỌC VIÊN & LỜI MỜI
        // ==========================================
        const studentsTab = doc.getElementById('students-tab');
        const studentsTableBody = doc.getElementById('studentsTableBody');
        const btnSendInvite = doc.getElementById('btnSendInvite');
        const inviteEmailInput = doc.getElementById('inviteEmailInput');
        const inviteStudentModalEl = doc.getElementById('inviteStudentModal');
        const inviteStudentModal = (win.bootstrap && inviteStudentModalEl) ? new win.bootstrap.Modal(inviteStudentModalEl) : null;

        const studentsPagination = doc.getElementById('studentsPagination');
        const studentsPageInfo = doc.getElementById('studentsPageInfo');
        const studentsPrevBtn = doc.getElementById('studentsPrevBtn');
        const studentsNextBtn = doc.getElementById('studentsNextBtn');

        let currentStudentPage = 0;
        const studentPageSize = 10;

        if (studentsTab) {
            studentsTab.addEventListener('shown.bs.tab', () => {
                currentStudentPage = 0;
                loadStudents(currentStudentPage);
            });
        }

        if (studentsPrevBtn) {
            studentsPrevBtn.onclick = function () {
                if (currentStudentPage > 0) {
                    currentStudentPage--;
                    loadStudents(currentStudentPage);
                }
            };
        }

        if (studentsNextBtn) {
            studentsNextBtn.onclick = function () {
                currentStudentPage++;
                loadStudents(currentStudentPage);
            };
        }

        function loadStudents(page = 0) {
            if (!studentsTableBody) return;
            studentsTableBody.textContent = '';
            const loadingRow = doc.createElement('tr');
            const loadingCell = doc.createElement('td');
            loadingCell.colSpan = 5;
            loadingCell.className = 'text-center py-4';
            loadingCell.innerHTML = '<div class="spinner-border spinner-border-sm text-primary me-2"></div> Đang tải danh sách học viên...';
            loadingRow.appendChild(loadingCell);
            studentsTableBody.appendChild(loadingRow);

            win.fetch(`/instructor/courses/${courseId}/students?page=${page}&size=${studentPageSize}`, {
                headers: { 'Accept': 'application/json' }
            })
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(res => {
                studentsTableBody.textContent = '';
                const pageData = res.data || res || {};
                const students = Array.isArray(pageData.content) ? pageData.content : (Array.isArray(pageData) ? pageData : []);
                const totalPages = pageData.totalPages || (students.length > 0 ? 1 : 0);
                const totalElements = pageData.totalElements != null ? pageData.totalElements : students.length;
                currentStudentPage = pageData.number != null ? pageData.number : page;

                // Cập nhật phân trang
                if (studentsPagination) {
                    if (totalPages > 1 || totalElements > 0) {
                        studentsPagination.classList.remove('d-none');
                        if (studentsPageInfo) {
                            studentsPageInfo.textContent = `Trang ${currentStudentPage + 1} / ${Math.max(totalPages, 1)} (Tổng số: ${totalElements})`;
                        }
                        if (studentsPrevBtn) studentsPrevBtn.disabled = currentStudentPage <= 0;
                        if (studentsNextBtn) studentsNextBtn.disabled = currentStudentPage >= totalPages - 1;
                    } else {
                        studentsPagination.classList.add('d-none');
                    }
                }

                if (students.length === 0) {
                    const emptyRow = doc.createElement('tr');
                    const emptyCell = doc.createElement('td');
                    emptyCell.colSpan = 5;
                    emptyCell.className = 'text-center py-5 text-muted';
                    emptyCell.innerHTML = '<i class="bi bi-people display-4 opacity-50 mb-2"></i><p class="mb-0">Chưa có học viên nào ghi danh vào khóa học này.</p>';
                    emptyRow.appendChild(emptyCell);
                    studentsTableBody.appendChild(emptyRow);
                    return;
                }

                students.forEach(st => {
                    const tr = doc.createElement('tr');

                    // Cột 1: Thông tin học viên
                    const tdUser = doc.createElement('td');
                    tdUser.className = 'ps-3';
                    const userDiv = doc.createElement('div');
                    userDiv.className = 'd-flex align-items-center gap-2';

                    const avatar = doc.createElement('div');
                    avatar.className = 'avatar-circle rounded-circle bg-primary text-white d-flex align-items-center justify-content-center fw-bold';
                    avatar.style.width = '36px';
                    avatar.style.height = '36px';
                    const nameChar = (st.fullname || st.username || 'U').charAt(0).toUpperCase();
                    avatar.textContent = nameChar;
                    userDiv.appendChild(avatar);

                    const nameSpan = doc.createElement('span');
                    nameSpan.className = 'fw-bold';
                    nameSpan.textContent = st.fullname || st.username || 'Học viên';
                    userDiv.appendChild(nameSpan);

                    tdUser.appendChild(userDiv);
                    tr.appendChild(tdUser);

                    // Cột 2: Email
                    const tdEmail = doc.createElement('td');
                    tdEmail.textContent = st.email || '—';
                    tr.appendChild(tdEmail);

                    // Cột 3: Ngày ghi danh
                    const tdDate = doc.createElement('td');
                    tdDate.className = 'text-muted small';
                    tdDate.textContent = formatDate(st.enrolledAt);
                    tr.appendChild(tdDate);

                    // Cột 4: Trạng thái
                    const tdStatus = doc.createElement('td');
                    const badge = doc.createElement('span');
                    badge.className = 'badge bg-success-subtle text-success';
                    badge.textContent = 'Đã ghi danh';
                    tdStatus.appendChild(badge);
                    tr.appendChild(tdStatus);

                    // Cột 5: Thao tác xóa
                    const tdAction = doc.createElement('td');
                    tdAction.className = 'text-end pe-3';
                    const btnRemove = doc.createElement('button');
                    btnRemove.type = 'button';
                    btnRemove.className = 'btn btn-sm btn-outline-danger btn-remove-student';
                    btnRemove.innerHTML = '<i class="bi bi-person-x me-1"></i>Xóa';
                    btnRemove.onclick = () => removeStudent(st.userId || st.id);
                    tdAction.appendChild(btnRemove);
                    tr.appendChild(tdAction);

                    studentsTableBody.appendChild(tr);
                });
            })
            .catch(err => {
                studentsTableBody.textContent = '';
                const errRow = doc.createElement('tr');
                const errCell = doc.createElement('td');
                errCell.colSpan = 5;
                errCell.className = 'text-center py-4 text-danger';
                errCell.textContent = 'Không thể tải danh sách học viên: ' + err.message;
                errRow.appendChild(errCell);
                studentsTableBody.appendChild(errRow);
            });
        }

        function removeStudent(userId) {
            if (!userId) return;
            if (!win.confirm('Bạn có chắc chắn muốn xóa học viên này khỏi khóa học?')) return;

            win.fetch(`/instructor/courses/${courseId}/students/${userId}`, {
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
                showAlert('success', 'Đã xóa học viên khỏi khóa học.');
                loadStudents(currentStudentPage);
            })
            .catch(err => {
                showAlert('danger', 'Lỗi khi xóa học viên: ' + err.message);
            });
        }

        if (btnSendInvite) {
            btnSendInvite.onclick = function () {
                const email = inviteEmailInput ? inviteEmailInput.value.trim() : '';
                if (!email) {
                    win.alert('Vui lòng nhập địa chỉ email hợp lệ.');
                    return;
                }

                win.fetch(`/instructor/courses/${courseId}/invite`, {
                    method: 'POST',
                    headers: getHeaders(),
                    body: JSON.stringify({ email: email })
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
                .then(res => {
                    const data = res.data || res || {};
                    const successList = Array.isArray(data.successEmails) ? data.successEmails : [];
                    const alreadyList = Array.isArray(data.alreadyEnrolledEmails) ? data.alreadyEnrolledEmails : [];
                    const notFoundList = Array.isArray(data.notFoundEmails) ? data.notFoundEmails : [];

                    if (successList.length > 0) {
                        if (inviteStudentModal) inviteStudentModal.hide();
                        if (inviteEmailInput) inviteEmailInput.value = '';
                        showAlert('success', `Đã ghi danh tài khoản thành công: ${successList.join(', ')}.`);
                        loadStudents(0);
                    } else if (alreadyList.length > 0) {
                        // KHÔNG ĐÓNG MODAL, BÁO CẢNH BÁO
                        showAlert('warning', `Tài khoản ${alreadyList.join(', ')} đã tham gia khóa học từ trước.`);
                    } else if (notFoundList.length > 0) {
                        // KHÔNG ĐÓNG MODAL, BÁO LỖI
                        showAlert('danger', `Không tìm thấy tài khoản với email: ${notFoundList.join(', ')}.`);
                    } else {
                        // Trường hợp response thông thường
                        if (inviteStudentModal) inviteStudentModal.hide();
                        if (inviteEmailInput) inviteEmailInput.value = '';
                        showAlert('success', 'Đã ghi danh học viên vào khóa học.');
                        loadStudents(0);
                    }
                })
                .catch(err => {
                    // GIỮ NGUYÊN FORM, KHÔNG ĐÓNG MODAL KHI THẤT BẠI
                    showAlert('danger', 'Lỗi khi ghi danh học viên: ' + err.message);
                });
            };
        }

        // ==========================================
        // 2. TAB LỊCH HỌC TRỰC TUYẾN (SCHEDULES)
        // ==========================================
        const schedulesTab = doc.getElementById('schedules-tab');
        const schedulesListContainer = doc.getElementById('schedulesListContainer');
        const btnCreateSchedule = doc.getElementById('btnCreateSchedule');
        const scheduleModalEl = doc.getElementById('scheduleModal');
        const scheduleModal = (win.bootstrap && scheduleModalEl) ? new win.bootstrap.Modal(scheduleModalEl) : null;
        const scheduleModalTitle = doc.getElementById('scheduleModalTitle');
        const scheduleIdInput = doc.getElementById('scheduleIdInput');
        const scheduleTitleInput = doc.getElementById('scheduleTitleInput');
        const scheduleStartInput = doc.getElementById('scheduleStartInput');
        const scheduleEndInput = doc.getElementById('scheduleEndInput');
        const scheduleNoteInput = doc.getElementById('scheduleNoteInput');
        const btnSaveSchedule = doc.getElementById('btnSaveSchedule');

        let currentSchedules = [];

        if (schedulesTab) {
            schedulesTab.addEventListener('shown.bs.tab', loadSchedules);
        }

        function loadSchedules() {
            if (!schedulesListContainer) return;
            schedulesListContainer.textContent = '';
            const loadingDiv = doc.createElement('div');
            loadingDiv.className = 'col-12 text-center py-4';
            loadingDiv.innerHTML = '<div class="spinner-border spinner-border-sm text-primary me-2"></div> Đang tải lịch học...';
            schedulesListContainer.appendChild(loadingDiv);

            win.fetch(`/instructor/courses/${courseId}/schedules`, {
                headers: { 'Accept': 'application/json' }
            })
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(res => {
                schedulesListContainer.textContent = '';
                const schedules = (res && res.data && Array.isArray(res.data)) ? res.data : (Array.isArray(res) ? res : []);
                currentSchedules = schedules;

                if (schedules.length === 0) {
                    const emptyDiv = doc.createElement('div');
                    emptyDiv.className = 'col-12 text-center py-5 text-muted';
                    emptyDiv.innerHTML = '<i class="bi bi-calendar-x display-4 opacity-50 mb-2"></i><p class="mb-0">Chưa có buổi học trực tuyến nào được lên lịch.</p>';
                    schedulesListContainer.appendChild(emptyDiv);
                    return;
                }

                schedules.forEach(sc => {
                    const col = doc.createElement('div');
                    col.className = 'col-md-6';

                    const card = doc.createElement('div');
                    card.className = 'card border rounded-3 p-3 bg-white shadow-sm h-100';

                    // Header: Title & Action buttons
                    const topDiv = doc.createElement('div');
                    topDiv.className = 'd-flex justify-content-between align-items-start mb-2';

                    const titleEl = doc.createElement('h6');
                    titleEl.className = 'fw-bold mb-0 text-primary';
                    titleEl.textContent = sc.title || 'Buổi học không tên';
                    topDiv.appendChild(titleEl);

                    const btnDiv = doc.createElement('div');
                    btnDiv.className = 'd-flex gap-1';

                    // Edit schedule button
                    const btnEdit = doc.createElement('button');
                    btnEdit.type = 'button';
                    btnEdit.className = 'btn btn-sm btn-outline-primary btn-edit-schedule';
                    btnEdit.title = 'Sửa lịch học';
                    btnEdit.innerHTML = '<i class="bi bi-pencil"></i>';
                    btnEdit.onclick = () => openEditScheduleModal(sc);
                    btnDiv.appendChild(btnEdit);

                    // Delete schedule button
                    const btnDel = doc.createElement('button');
                    btnDel.type = 'button';
                    btnDel.className = 'btn btn-sm btn-outline-danger btn-delete-schedule';
                    btnDel.title = 'Xóa lịch học';
                    btnDel.innerHTML = '<i class="bi bi-trash"></i>';
                    btnDel.onclick = () => deleteSchedule(sc.id);
                    btnDiv.appendChild(btnDel);

                    topDiv.appendChild(btnDiv);
                    card.appendChild(topDiv);

                    // Time details
                    const timeDiv = doc.createElement('div');
                    timeDiv.className = 'text-muted small mb-2';
                    timeDiv.textContent = `Bắt đầu: ${formatDate(sc.startTime)} | Kết thúc: ${formatDate(sc.endTime)}`;
                    card.appendChild(timeDiv);

                    // Meeting link / note with XSS protection
                    const noteDiv = doc.createElement('div');
                    noteDiv.className = 'small bg-light p-2 rounded mb-2';

                    const linkIcon = doc.createElement('i');
                    linkIcon.className = 'bi bi-link-45deg me-1';
                    noteDiv.appendChild(linkIcon);

                    const noteLabel = doc.createElement('span');
                    noteLabel.className = 'fw-bold me-1';
                    noteLabel.textContent = 'Link / Ghi chú: ';
                    noteDiv.appendChild(noteLabel);

                    const linkOrNote = sc.meetingUrl || sc.note || '';
                    if (isSafeUrl(linkOrNote)) {
                        const link = doc.createElement('a');
                        link.href = linkOrNote.trim();
                        link.target = '_blank';
                        link.rel = 'noopener noreferrer';
                        link.textContent = linkOrNote.trim();
                        noteDiv.appendChild(link);
                    } else {
                        // An toàn XSS: không tạo thẻ <a> với javascript: hoặc dữ liệu không an toàn
                        const safeText = doc.createElement('span');
                        safeText.textContent = linkOrNote || 'Không có ghi chú';
                        noteDiv.appendChild(safeText);
                    }

                    card.appendChild(noteDiv);
                    col.appendChild(card);
                    schedulesListContainer.appendChild(col);
                });
            })
            .catch(err => {
                schedulesListContainer.textContent = '';
                const errDiv = doc.createElement('div');
                errDiv.className = 'col-12 text-center py-4 text-danger';
                errDiv.textContent = 'Không thể tải lịch học: ' + err.message;
                schedulesListContainer.appendChild(errDiv);
            });
        }

        function openEditScheduleModal(sc) {
            if (scheduleModalTitle) scheduleModalTitle.textContent = 'Chỉnh Sửa Lịch Học Trực Tuyến';
            if (scheduleIdInput) scheduleIdInput.value = sc.id;
            if (scheduleTitleInput) scheduleTitleInput.value = sc.title || '';
            if (scheduleStartInput) scheduleStartInput.value = toLocalDatetimeString(sc.startTime);
            if (scheduleEndInput) scheduleEndInput.value = toLocalDatetimeString(sc.endTime);
            if (scheduleNoteInput) scheduleNoteInput.value = sc.meetingUrl || sc.note || '';
            if (scheduleModal) scheduleModal.show();
        }

        function deleteSchedule(scheduleId) {
            if (!win.confirm('Bạn có chắc chắn muốn xóa lịch học này?')) return;

            win.fetch(`/instructor/courses/${courseId}/schedules/${scheduleId}`, {
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
                showAlert('success', 'Đã xóa lịch học thành công.');
                loadSchedules();
            })
            .catch(err => {
                showAlert('danger', 'Lỗi: ' + err.message);
            });
        }

        if (btnCreateSchedule) {
            btnCreateSchedule.onclick = function () {
                if (scheduleModalTitle) scheduleModalTitle.textContent = 'Thêm Lịch Học Trực Tuyến';
                if (scheduleIdInput) scheduleIdInput.value = '';
                if (scheduleTitleInput) scheduleTitleInput.value = '';
                if (scheduleStartInput) scheduleStartInput.value = '';
                if (scheduleEndInput) scheduleEndInput.value = '';
                if (scheduleNoteInput) scheduleNoteInput.value = '';
                if (scheduleModal) scheduleModal.show();
            };
        }

        if (btnSaveSchedule) {
            btnSaveSchedule.onclick = function () {
                const title = scheduleTitleInput ? scheduleTitleInput.value.trim() : '';
                const start = scheduleStartInput ? scheduleStartInput.value : '';
                const end = scheduleEndInput ? scheduleEndInput.value : '';
                const note = scheduleNoteInput ? scheduleNoteInput.value.trim() : '';

                if (!title || !start || !end) {
                    win.alert('Vui lòng nhập đầy đủ tiêu đề, thời gian bắt đầu và kết thúc.');
                    return;
                }

                const startTimeIso = new Date(start).toISOString();
                const endTimeIso = new Date(end).toISOString();
                const sId = scheduleIdInput ? scheduleIdInput.value : '';
                const isEdit = Boolean(sId);

                const url = isEdit
                    ? `/instructor/courses/${courseId}/schedules/${sId}`
                    : `/instructor/courses/${courseId}/schedules`;
                const method = isEdit ? 'PUT' : 'POST';

                win.fetch(url, {
                    method: method,
                    headers: getHeaders(),
                    body: JSON.stringify({
                        title: title,
                        startTime: startTimeIso,
                        endTime: endTimeIso,
                        note: note
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
                    if (scheduleModal) scheduleModal.hide();
                    showAlert('success', isEdit ? 'Đã cập nhật lịch học thành công!' : 'Đã tạo lịch học thành công!');
                    loadSchedules();
                })
                .catch(err => {
                    // GIỮ FORM, KHÔNG ĐÓNG MODAL KHI THẤT BẠI
                    showAlert('danger', 'Lỗi khi lưu lịch học: ' + err.message);
                });
            };
        }

        // ==========================================
        // 3. TAB BẢNG TIN THÔNG BÁO (ANNOUNCEMENTS)
        // ==========================================
        const announcementsTab = doc.getElementById('announcements-tab');
        const announcementsListContainer = doc.getElementById('announcementsListContainer');
        const announcementForm = doc.getElementById('announcementForm');
        const announcementTitleInput = doc.getElementById('announcementTitleInput');
        const announcementContentInput = doc.getElementById('announcementContentInput');

        const announcementsPagination = doc.getElementById('announcementsPagination');
        const announcementsPageInfo = doc.getElementById('announcementsPageInfo');
        const announcementsPrevBtn = doc.getElementById('announcementsPrevBtn');
        const announcementsNextBtn = doc.getElementById('announcementsNextBtn');

        let currentAnnouncementPage = 0;
        const announcementPageSize = 10;

        if (announcementsTab) {
            announcementsTab.addEventListener('shown.bs.tab', () => {
                currentAnnouncementPage = 0;
                loadAnnouncements(currentAnnouncementPage);
            });
        }

        if (announcementsPrevBtn) {
            announcementsPrevBtn.onclick = function () {
                if (currentAnnouncementPage > 0) {
                    currentAnnouncementPage--;
                    loadAnnouncements(currentAnnouncementPage);
                }
            };
        }

        if (announcementsNextBtn) {
            announcementsNextBtn.onclick = function () {
                currentAnnouncementPage++;
                loadAnnouncements(currentAnnouncementPage);
            };
        }

        function loadAnnouncements(page = 0) {
            if (!announcementsListContainer) return;
            announcementsListContainer.textContent = '';
            const loadingDiv = doc.createElement('div');
            loadingDiv.className = 'text-center py-4';
            loadingDiv.innerHTML = '<div class="spinner-border spinner-border-sm text-primary me-2"></div> Đang tải thông báo...';
            announcementsListContainer.appendChild(loadingDiv);

            win.fetch(`/instructor/courses/${courseId}/announcements?page=${page}&size=${announcementPageSize}`, {
                headers: { 'Accept': 'application/json' }
            })
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(res => {
                announcementsListContainer.textContent = '';
                const pageData = res.data || res || {};
                const announcements = Array.isArray(pageData.content) ? pageData.content : (Array.isArray(pageData) ? pageData : []);
                const totalPages = pageData.totalPages || (announcements.length > 0 ? 1 : 0);
                const totalElements = pageData.totalElements != null ? pageData.totalElements : announcements.length;
                currentAnnouncementPage = pageData.number != null ? pageData.number : page;

                // Cập nhật phân trang
                if (announcementsPagination) {
                    if (totalPages > 1 || totalElements > 0) {
                        announcementsPagination.classList.remove('d-none');
                        if (announcementsPageInfo) {
                            announcementsPageInfo.textContent = `Trang ${currentAnnouncementPage + 1} / ${Math.max(totalPages, 1)} (Tổng số: ${totalElements})`;
                        }
                        if (announcementsPrevBtn) announcementsPrevBtn.disabled = currentAnnouncementPage <= 0;
                        if (announcementsNextBtn) announcementsNextBtn.disabled = currentAnnouncementPage >= totalPages - 1;
                    } else {
                        announcementsPagination.classList.add('d-none');
                    }
                }

                if (announcements.length === 0) {
                    const emptyDiv = doc.createElement('div');
                    emptyDiv.className = 'text-center py-4 text-muted';
                    emptyDiv.innerHTML = '<i class="bi bi-megaphone display-4 opacity-50 mb-2"></i><p class="mb-0">Chưa có thông báo nào trong khóa học này.</p>';
                    announcementsListContainer.appendChild(emptyDiv);
                    return;
                }

                announcements.forEach(an => {
                    const item = doc.createElement('div');
                    item.className = 'card border rounded-3 p-3 bg-white shadow-sm';

                    const topRow = doc.createElement('div');
                    topRow.className = 'd-flex justify-content-between align-items-start mb-2';

                    const titleBox = doc.createElement('div');
                    const titleEl = doc.createElement('h6');
                    titleEl.className = 'fw-bold mb-1 text-primary';
                    titleEl.textContent = an.title || 'Thông báo không tên';
                    titleBox.appendChild(titleEl);

                    const timeSpan = doc.createElement('span');
                    timeSpan.className = 'text-muted small';
                    timeSpan.innerHTML = `<i class="bi bi-clock me-1"></i>${formatDate(an.createdAt)}`;
                    titleBox.appendChild(timeSpan);
                    topRow.appendChild(titleBox);

                    const btnDel = doc.createElement('button');
                    btnDel.type = 'button';
                    btnDel.className = 'btn btn-sm btn-outline-danger btn-delete-announcement';
                    btnDel.title = 'Xóa thông báo';
                    btnDel.innerHTML = '<i class="bi bi-trash"></i>';
                    btnDel.onclick = () => deleteAnnouncement(an.id);
                    topRow.appendChild(btnDel);

                    item.appendChild(topRow);

                    const contentP = doc.createElement('p');
                    contentP.className = 'mb-0 text-secondary';
                    contentP.textContent = an.content || '';
                    item.appendChild(contentP);

                    announcementsListContainer.appendChild(item);
                });
            })
            .catch(err => {
                announcementsListContainer.textContent = '';
                const errDiv = doc.createElement('div');
                errDiv.className = 'text-center py-4 text-danger';
                errDiv.textContent = 'Không thể tải thông báo: ' + err.message;
                announcementsListContainer.appendChild(errDiv);
            });
        }

        function deleteAnnouncement(announcementId) {
            if (!win.confirm('Bạn có chắc chắn muốn xóa thông báo này?')) return;

            win.fetch(`/instructor/courses/${courseId}/announcements/${announcementId}`, {
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
                showAlert('success', 'Đã xóa thông báo thành công.');
                loadAnnouncements(currentAnnouncementPage);
            })
            .catch(err => {
                showAlert('danger', 'Lỗi: ' + err.message);
            });
        }

        if (announcementForm) {
            announcementForm.onsubmit = function (e) {
                e.preventDefault();
                const title = announcementTitleInput ? announcementTitleInput.value.trim() : '';
                const content = announcementContentInput ? announcementContentInput.value.trim() : '';
                if (!title || !content) return;

                win.fetch(`/instructor/courses/${courseId}/announcements`, {
                    method: 'POST',
                    headers: getHeaders(),
                    body: JSON.stringify({
                        title: title,
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
                    if (announcementTitleInput) announcementTitleInput.value = '';
                    if (announcementContentInput) announcementContentInput.value = '';
                    showAlert('success', 'Đã đăng thông báo mới thành công!');
                    loadAnnouncements(0);
                })
                .catch(err => {
                    // GIỮ NGUYÊN FORM KHI THẤT BẠI
                    showAlert('danger', 'Lỗi khi đăng thông báo: ' + err.message);
                });
            };
        }
    }

    if (typeof document !== 'undefined') {
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', () => initInstructorManagement(document, window));
        } else {
            initInstructorManagement(document, window);
        }
    }

    if (typeof module !== 'undefined' && module.exports) {
        module.exports = { initInstructorManagement };
    } else {
        global.initInstructorManagement = initInstructorManagement;
    }
})(typeof window !== 'undefined' ? window : this);
