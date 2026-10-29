import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor, fireEvent } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import React from 'react';
import { MemoryRouter } from 'react-router-dom';
import AuthPage from '../../../features/auth/pages/AuthPage';
import AuthContext from '../../../context/AuthContext';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('AuthPage Feature Component', () => {
  let mockAuth;

  beforeEach(() => {
    vi.clearAllMocks();
    mockAuth = {
      login: vi.fn(),
      register: vi.fn(),
      user: null,
      isAuthenticated: false,
    };
  });

  const renderAuthPage = (initialRoute = '/login') => {
    return render(
      <MemoryRouter initialEntries={[initialRoute]}>
        <AuthContext.Provider value={mockAuth}>
          <AuthPage />
        </AuthContext.Provider>
      </MemoryRouter>
    );
  };

  it('renders login tab by default and switches to register tab', async () => {
    const user = userEvent.setup();
    const { container } = renderAuthPage('/login');

    const submitBtn = container.querySelector('button[type="submit"]');
    expect(submitBtn).toHaveTextContent(/đăng nhập/i);

    const tabs = container.querySelectorAll('.auth-tab');
    const registerTab = Array.from(tabs).find((t) => t.textContent.includes('Đăng Ký'));
    expect(registerTab).toBeDefined();
    await user.click(registerTab);

    expect(container.querySelector('#fullname')).toBeInTheDocument();
  });

  it('validates email format in signup form and displays inline error', async () => {
    const user = userEvent.setup();
    const { container } = renderAuthPage('/register');

    // Chọn role Học viên
    const roleBtn = screen.getByRole('button', { name: /học viên/i });
    await user.click(roleBtn);

    // Điền các trường hợp lệ
    await user.type(container.querySelector('#fullname'), 'Nguyen Van A');
    await user.type(container.querySelector('#username'), 'nguyenvana');
    // Nhập email sai định dạng
    await user.type(container.querySelector('#email'), 'invalid-email');
    await user.type(container.querySelector('#password'), 'Password123');
    await user.type(container.querySelector('#confirmPassword'), 'Password123');

    // Check terms
    await user.click(container.querySelector('#terms'));

    const form = container.querySelector('form.auth-form');
    fireEvent.submit(form);

    await waitFor(() => {
      expect(screen.getByText(/vui lòng nhập email hợp lệ/i)).toBeInTheDocument();
    });
  });

  it('validates password minimum length in signup form', async () => {
    const user = userEvent.setup();
    const { container } = renderAuthPage('/register');

    // Chọn role Học viên
    const roleBtn = screen.getByRole('button', { name: /học viên/i });
    await user.click(roleBtn);

    // Điền các trường hợp lệ
    await user.type(container.querySelector('#fullname'), 'Nguyen Van A');
    await user.type(container.querySelector('#username'), 'nguyenvana');
    await user.type(container.querySelector('#email'), 'valid@example.com');
    // Nhập mật khẩu ngắn hơn 8 ký tự
    await user.type(container.querySelector('#password'), '12345');
    await user.type(container.querySelector('#confirmPassword'), '12345');

    // Check terms
    await user.click(container.querySelector('#terms'));

    const submitBtn = container.querySelector('button[type="submit"]');
    await user.click(submitBtn);

    await waitFor(() => {
      expect(screen.getByText(/mật khẩu phải có ít nhất 8 ký tự/i)).toBeInTheDocument();
    });
  });

  it('submits login successfully and navigates to correct role dashboard', async () => {
    const user = userEvent.setup();
    mockAuth.login.mockResolvedValueOnce({
      id: 'usr-1',
      username: 'student1',
      role: 'STUDENT',
    });

    const { container } = renderAuthPage('/login');

    const identifierInput = container.querySelector('#identifier');
    const passwordInput = container.querySelector('#password');

    await user.type(identifierInput, 'student1');
    await user.type(passwordInput, 'secret123');

    const submitBtn = container.querySelector('button[type="submit"]');
    await user.click(submitBtn);

    await waitFor(() => {
      expect(mockAuth.login).toHaveBeenCalledWith('student1', 'secret123', false);
      expect(mockNavigate).toHaveBeenCalledWith('/dashboard', { replace: true });
    });
  });
});
