import { describe, it, expect, beforeEach } from 'vitest';
import { useAuthStore } from './authStore';
import type { MemberProfile } from '@/types/member';

const mockUser: MemberProfile = {
  userId: 'u-1',
  email: 'tester@example.com',
  displayName: 'Tester',
  status: 'ACTIVE',
  emailVerifiedAt: '2026-04-22T08:00:00.000+08:00',
  notifyEmailEnabled: true,
  notifyWebEnabled: true,
  createdAt: '2026-04-22T08:00:00.000+08:00',
};

describe('authStore', () => {
  beforeEach(() => {
    useAuthStore.getState().logout();
    localStorage.clear();
  });

  it('初始狀態應為未登入', () => {
    const state = useAuthStore.getState();
    expect(state.token).toBeNull();
    expect(state.user).toBeNull();
    expect(state.isAuthenticated).toBe(false);
  });

  it('login 後應寫入 token、user 並標記為已登入', () => {
    useAuthStore.getState().login('access-token', 'refresh-token', mockUser);

    const state = useAuthStore.getState();
    expect(state.token).toBe('access-token');
    expect(state.refreshToken).toBe('refresh-token');
    expect(state.user).toEqual(mockUser);
    expect(state.isAuthenticated).toBe(true);
  });

  it('logout 後應清空所有 auth 狀態', () => {
    useAuthStore.getState().login('access-token', 'refresh-token', mockUser);
    useAuthStore.getState().logout();

    const state = useAuthStore.getState();
    expect(state.token).toBeNull();
    expect(state.refreshToken).toBeNull();
    expect(state.user).toBeNull();
    expect(state.isAuthenticated).toBe(false);
  });

  it('setUser 可單獨更新 user 不影響 token', () => {
    useAuthStore.getState().login('access-token', 'refresh-token', mockUser);
    const updated: MemberProfile = { ...mockUser, displayName: '新名稱' };
    useAuthStore.getState().setUser(updated);

    const state = useAuthStore.getState();
    expect(state.user?.displayName).toBe('新名稱');
    expect(state.token).toBe('access-token');
  });
});
