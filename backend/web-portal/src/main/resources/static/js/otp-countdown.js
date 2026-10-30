document.addEventListener('DOMContentLoaded', function () {
    const timerElement = document.getElementById('countdownTimer');
    const resendBtn = document.getElementById('resendOtpBtn');
    const resendBtnText = document.getElementById('resendBtnText') || resendBtn;
    const resendAlert = document.getElementById('resendAlert');
    const resendAlertMessage = document.getElementById('resendAlertMessage');
    const resendSuccessAlert = document.getElementById('resendSuccessAlert');
    const resendSuccessAlertMessage = document.getElementById('resendSuccessAlertMessage');
    const emailInput = document.getElementById('email');

    if (!timerElement || !resendBtn) return;

    let currentInterval = null;
    let timeLeft = 60;

    function startCountdown(seconds) {
        if (currentInterval) {
            clearInterval(currentInterval);
        }
        timeLeft = seconds;
        timerElement.textContent = timeLeft;
        resendBtn.disabled = true;

        currentInterval = setInterval(function () {
            timeLeft--;
            timerElement.textContent = timeLeft;

            if (timeLeft <= 0) {
                clearInterval(currentInterval);
                currentInterval = null;
                timerElement.textContent = '0';
                resendBtn.disabled = false;
            }
        }, 1000);
    }

    function showError(message) {
        if (resendSuccessAlert) resendSuccessAlert.classList.add('d-none');
        if (resendAlert && resendAlertMessage) {
            resendAlertMessage.textContent = message;
            resendAlert.classList.remove('d-none');
        } else {
            alert(message);
        }
    }

    function showSuccess(message) {
        if (resendAlert) resendAlert.classList.add('d-none');
        if (resendSuccessAlert && resendSuccessAlertMessage) {
            resendSuccessAlertMessage.textContent = message;
            resendSuccessAlert.classList.remove('d-none');
        }
    }

    // Khởi động đếm ngược lần đầu khi vào trang
    if (timerElement.dataset.deliveryFailed === 'true') {
        timeLeft = 0;
        timerElement.textContent = '0';
        resendBtn.disabled = false;
    } else {
        startCountdown(60);
    }

    resendBtn.addEventListener('click', async function () {
        const email = emailInput ? emailInput.value.trim() : '';
        if (!email) {
            showError('Không tìm thấy thông tin email.');
            return;
        }

        // Tạm khóa nút và hiển thị trạng thái đang gửi
        resendBtn.disabled = true;
        const originalHtml = resendBtnText.innerHTML;
        resendBtnText.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>Đang gửi...';

        // Lấy CSRF token từ form
        const csrfInput = document.querySelector('input[name="_csrf"]');
        const csrfToken = csrfInput ? csrfInput.value : '';

        try {
            const response = await fetch('/verify-email/resend', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'X-CSRF-TOKEN': csrfToken
                },
                body: JSON.stringify({ email: email })
            });

            const data = await response.json().catch(function () { return {}; });

            if (response.ok) {
                // CHỈ KHỞI ĐỘNG LẠI ĐẾM NGƯỢC KHI API GỬI THÀNH CÔNG
                showSuccess(data.message || 'Mã OTP mới đã được gửi thành công.');
                startCountdown(60);
            } else {
                // HIỂN THỊ LỖI NẾU GỬI THẤT BẠI - KHÔNG KHỞI ĐỘNG LẠI ĐẾM NGƯỢC
                showError(data.message || 'Không thể gửi lại mã OTP. Vui lòng thử lại sau.');
                if (timeLeft <= 0) {
                    resendBtn.disabled = false;
                }
            }
        } catch (error) {
            showError('Có lỗi kết nối mạng. Vui lòng thử lại sau.');
            if (timeLeft <= 0) {
                resendBtn.disabled = false;
            }
        } finally {
            resendBtnText.innerHTML = originalHtml;
        }
    });
});
