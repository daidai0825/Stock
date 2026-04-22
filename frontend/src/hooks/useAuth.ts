import { useMutation } from '@tanstack/react-query';
import { useAuthStore } from '@/stores/authStore';
import { memberService } from '@/services/memberService';
import type { LoginRequest, RegisterRequest } from '@/types/member';

/**
 * 認證相關 hook（會員）
 * 由 TanStack Query 負責 mutation 狀態（loading / error / data）
 */

export const useLogin = () => {
  const login = useAuthStore((state) => state.login);

  return useMutation({
    mutationFn: (req: LoginRequest) => memberService.login(req),
    onSuccess: (data) => {
      login(data.accessToken, data.refreshToken, data.user);
    },
  });
};

export const useRegister = () => {
  return useMutation({
    mutationFn: (req: RegisterRequest) => memberService.register(req),
  });
};

export const useLogout = () => {
  const logout = useAuthStore((state) => state.logout);

  return useMutation({
    mutationFn: () => memberService.logout(),
    onSettled: () => {
      // 不論成功失敗都清掉本地 state，避免殘留
      logout();
    },
  });
};
