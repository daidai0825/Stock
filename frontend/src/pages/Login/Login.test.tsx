import { describe, it, expect, vi, beforeEach } from 'vitest';
import { screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import LoginPage from './index';
import { renderWithProviders } from '@/test/testUtils';
import { memberService } from '@/services/memberService';

vi.mock('@/services/memberService', () => ({
  memberService: {
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    getProfile: vi.fn(),
    updateProfile: vi.fn(),
  },
}));

describe('LoginPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('未填寫即提交時應顯示 email 與 password 必填提示', async () => {
    const user = userEvent.setup();
    renderWithProviders(<LoginPage />);

    // antd 5 對 2 字 CJK 按鈕會自動加空格（「登 入」），用 getByRole + type=submit 較穩
    await user.click(screen.getByRole('button', { name: /登\s*入/ }));

    await waitFor(() => {
      expect(screen.getByText('請輸入電子郵件')).toBeInTheDocument();
      expect(screen.getByText('請輸入密碼')).toBeInTheDocument();
    });

    expect(memberService.login).not.toHaveBeenCalled();
  });

  it('填入合法資料時應呼叫 memberService.login', async () => {
    const user = userEvent.setup();
    vi.mocked(memberService.login).mockResolvedValueOnce({
      accessToken: 'access',
      refreshToken: 'refresh',
      expiresIn: 3600,
      user: {
        userId: 'u-1',
        email: 'user@example.com',
        displayName: 'Tester',
        status: 'ACTIVE',
        emailVerifiedAt: '2026-04-22T08:00:00.000+08:00',
        notifyEmailEnabled: true,
        notifyWebEnabled: true,
        createdAt: '2026-04-22T08:00:00.000+08:00',
      },
    });

    renderWithProviders(<LoginPage />);

    await user.type(screen.getByLabelText('電子郵件'), 'user@example.com');
    await user.type(screen.getByLabelText('密碼'), 'password123');
    // antd 5 對 2 字 CJK 按鈕會自動加空格（「登 入」），用 getByRole + type=submit 較穩
    await user.click(screen.getByRole('button', { name: /登\s*入/ }));

    await waitFor(() => {
      expect(memberService.login).toHaveBeenCalledWith({
        email: 'user@example.com',
        password: 'password123',
      });
    });
  });
});
