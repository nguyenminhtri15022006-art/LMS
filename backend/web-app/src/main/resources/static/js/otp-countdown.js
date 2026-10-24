document.addEventListener('DOMContentLoaded', function () {
    const timerElement = document.getElementById('countdownTimer');
    const resendBtn = document.getElementById('resendOtpBtn');
    let timeLeft = 60;

    if (!timerElement || !resendBtn) return;

    const interval = setInterval(function () {
        timeLeft--;
        timerElement.textContent = timeLeft;

        if (timeLeft <= 0) {
            clearInterval(interval);
            timerElement.textContent = '0';
            resendBtn.disabled = false;
        }
    }, 1000);

    resendBtn.addEventListener('click', function () {
        resendBtn.disabled = true;
        timeLeft = 60;
        timerElement.textContent = timeLeft;

        // Optional notification
        const emailInput = document.getElementById('email');
        if (emailInput && emailInput.value) {
            console.log('Yêu cầu gửi lại OTP cho email: ' + emailInput.value);
        }

        const newInterval = setInterval(function () {
            timeLeft--;
            timerElement.textContent = timeLeft;
            if (timeLeft <= 0) {
                clearInterval(newInterval);
                timerElement.textContent = '0';
                resendBtn.disabled = false;
            }
        }, 1000);
    });
});
