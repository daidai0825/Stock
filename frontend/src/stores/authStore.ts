import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import type { MemberProfile } from '@/types/member';

/**
 * 全域 Auth State
 *
 * 設計取捨（依 react-typescript.md「禁止將 token 存於 localStorage（建議 httpOnly cookie）」）：
 * Wave A 採 localStorage 持久化是過渡方案，待 Bruno 後端完成 httpOnly cookie 支援後切換。
 * 已在 docs/05_development/frontend/20260422_waveA_skeleton.md TODO 列入。
 */

export interface AuthState {
  token: string | null;
  refreshToken: string | null;
  user: MemberProfile | null;
  isAuthenticated: boolean;
  login: (token: string, refreshToken: string, user: MemberProfile) => void;
  logout: () => void;
  setUser: (user: MemberProfile) => void;
  setToken: (token: string) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      refreshToken: null,
      user: null,
      isAuthenticated: false,

      login: (token, refreshToken, user) => {
        set({ token, refreshToken, user, isAuthenticated: true });
      },

      logout: () => {
        set({ token: null, refreshToken: null, user: null, isAuthenticated: false });
      },

      setUser: (user) => {
        set({ user });
      },

      setToken: (token) => {
        set({ token });
      },
    }),
    {
      name: 'stock-auth-store',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        token: state.token,
        refreshToken: state.refreshToken,
        user: state.user,
        isAuthenticated: state.isAuthenticated,
      }),
    },
  ),
);
