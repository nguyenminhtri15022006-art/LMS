import React, { useState, useEffect, useRef } from 'react';
import apiClient from '../../../services/api.client';
import { ENDPOINTS } from '../../../constants/endpoints';
import '../styles/VerifyOtpModal.css';

export default function VerifyOtpModal({ isOpen, onClose, email, onVerified, initialCountdown = 60 }) {
  const [digits, setDigits] = useState(['', '', '', '', '', '']);
  const [countdown, setCountdown] = useState(initialCountdown);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const inputRefs = useRef([]);

  useEffect(() => {
    if (!isOpen) {
      setDigits(['', '', '', '', '', '']);
      setError('');
      setSuccess('');
      setCountdown(initialCountdown);
      return;
    }

    const timer = setInterval(() => {
      setCountdown((prev) => (prev > 0 ? prev - 1 : 0));
    }, 1000);

    return () => clearInterval(timer);
  }, [isOpen, initialCountdown]);

  if (!isOpen) return null;

  const handleChange = (index, value) => {
    if (!/^\d*$/.test(value)) return;
    const newDigits = [...digits];
    newDigits[index] = value.slice(-1);
    setDigits(newDigits);
    setError('');

    if (value && index < 5) {
      inputRefs.current[index + 1]?.focus();
    }

    // Auto submit if all 6 digits are filled
    const fullOtp = newDigits.join('');
    if (fullOtp.length === 6 && !newDigits.includes('')) {
      submitOtp(fullOtp);
    }
  };

  const handleKeyDown = (index, e) => {
    if (e.key === 'Backspace' && !digits[index] && index > 0) {
      inputRefs.current[index - 1]?.focus();
    }
  };

  const submitOtp = async (code) => {
    const otpCode = code || digits.join('');
    if (otpCode.length !== 6) {
      setError('Vui lòng nhập đủ 6 chữ số OTP');
      return;
    }

    setLoading(true);
    setError('');
    try {
      await apiClient.post(ENDPOINTS.AUTH.VERIFY_EMAIL, {
        email,
        otp: otpCode,
      });
      setSuccess('Xác thực thành công!');
      if (onVerified) onVerified(otpCode);
      setTimeout(() => {
        if (onClose) onClose();
      }, 1000);
    } catch (err) {
      setError(err.response?.data?.message || 'Mã OTP không chính xác hoặc đã hết hạn');
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    if (countdown > 0) return;
    setLoading(true);
    setError('');
    try {
      await apiClient.post(ENDPOINTS.AUTH.VERIFY_EMAIL_SEND, { email });
      setCountdown(initialCountdown);
      setSuccess('Đã gửi lại mã OTP vào email của bạn.');
    } catch (err) {
      setError(err.response?.data?.message || 'Gửi lại mã OTP thất bại');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="otp-modal-backdrop" role="dialog" aria-modal="true" data-testid="verify-otp-modal">
      <div className="otp-modal-card">
        <h3 className="otp-modal-title">Xác thực mã OTP</h3>
        <p className="otp-modal-desc">
          Mã xác thực 6 chữ số đã được gửi tới <strong>{email || 'email của bạn'}</strong>.
        </p>

        {error && <div className="otp-modal-alert error" data-testid="otp-error">{error}</div>}
        {success && <div className="otp-modal-alert success" data-testid="otp-success">{success}</div>}

        <div className="otp-inputs-row">
          {digits.map((digit, index) => (
            <input
              key={index}
              ref={(el) => (inputRefs.current[index] = el)}
              type="text"
              inputMode="numeric"
              maxLength={1}
              value={digit}
              onChange={(e) => handleChange(index, e.target.value)}
              onKeyDown={(e) => handleKeyDown(index, e)}
              className="otp-digit-input"
              data-testid={`otp-digit-${index}`}
              aria-label={`Digit ${index + 1}`}
              disabled={loading}
              autoFocus={index === 0}
            />
          ))}
        </div>

        <div className="otp-countdown-row" data-testid="otp-countdown">
          {countdown > 0 ? (
            <span>Mã có hiệu lực: <strong>{countdown}s</strong></span>
          ) : (
            <button
              type="button"
              className="otp-resend-btn"
              onClick={handleResend}
              disabled={loading}
              data-testid="resend-otp-btn"
            >
              Gửi lại mã OTP
            </button>
          )}
        </div>

        <div className="otp-actions-row">
          <button
            type="button"
            className="otp-cancel-btn"
            onClick={onClose}
            disabled={loading}
            data-testid="cancel-otp-btn"
          >
            Hủy
          </button>
          <button
            type="button"
            className="otp-submit-btn"
            onClick={() => submitOtp()}
            disabled={loading || digits.join('').length !== 6}
            data-testid="submit-otp-btn"
          >
            {loading ? 'Đang xử lý...' : 'Xác thực'}
          </button>
        </div>
      </div>
    </div>
  );
}
