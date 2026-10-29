import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import VerifyOtpModal from '../../../features/auth/components/VerifyOtpModal';
import apiClient from '../../../services/api.client';

vi.mock('../../../services/api.client', () => ({
  default: {
    post: vi.fn(),
  },
}));

describe('VerifyOtpModal Component', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders countdown and 6 digit inputs when open', () => {
    render(
      <VerifyOtpModal
        isOpen={true}
        onClose={vi.fn()}
        email="student@test.com"
        initialCountdown={60}
      />
    );

    expect(screen.getByTestId('verify-otp-modal')).toBeInTheDocument();
    expect(screen.getByText(/student@test.com/i)).toBeInTheDocument();
    expect(screen.getByTestId('otp-countdown')).toHaveTextContent('60s');

    for (let i = 0; i < 6; i++) {
      expect(screen.getByTestId(`otp-digit-${i}`)).toBeInTheDocument();
    }
  });

  it('auto-submits when all 6 digits are typed', async () => {
    const user = userEvent.setup();
    const onVerified = vi.fn();
    apiClient.post.mockResolvedValueOnce({ data: { message: 'Verified' } });

    render(
      <VerifyOtpModal
        isOpen={true}
        onClose={vi.fn()}
        email="student@test.com"
        onVerified={onVerified}
        initialCountdown={60}
      />
    );

    const inputs = [
      screen.getByTestId('otp-digit-0'),
      screen.getByTestId('otp-digit-1'),
      screen.getByTestId('otp-digit-2'),
      screen.getByTestId('otp-digit-3'),
      screen.getByTestId('otp-digit-4'),
      screen.getByTestId('otp-digit-5'),
    ];

    const digits = ['1', '2', '3', '4', '5', '6'];
    for (let i = 0; i < 6; i++) {
      await user.type(inputs[i], digits[i]);
    }

    await waitFor(() => {
      expect(apiClient.post).toHaveBeenCalledWith(
        expect.stringContaining('/verify-email'),
        {
          email: 'student@test.com',
          otp: '123456',
        }
      );
    });

    expect(onVerified).toHaveBeenCalledWith('123456');
  });

  it('calls onClose when cancel button is clicked', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();

    render(
      <VerifyOtpModal
        isOpen={true}
        onClose={onClose}
        email="student@test.com"
      />
    );

    await user.click(screen.getByTestId('cancel-otp-btn'));
    expect(onClose).toHaveBeenCalled();
  });

  it('displays error message when verification fails', async () => {
    const user = userEvent.setup();
    apiClient.post.mockRejectedValueOnce({
      response: { data: { message: 'Mã OTP không hợp lệ' } },
    });

    render(
      <VerifyOtpModal
        isOpen={true}
        onClose={vi.fn()}
        email="student@test.com"
      />
    );

    for (let i = 0; i < 6; i++) {
      await user.type(screen.getByTestId(`otp-digit-${i}`), '9');
    }

    await waitFor(() => {
      expect(screen.getByTestId('otp-error')).toHaveTextContent('Mã OTP không hợp lệ');
    });
  });
});
