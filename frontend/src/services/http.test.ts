import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import type { AxiosAdapter, AxiosResponse } from 'axios';
import { http, postJson } from './http';
import { BusinessError } from '@/types/api';
import { useAuthStore } from '@/stores/authStore';
import { setAppNavigate } from '@/services/navigator';
import { ErrorCode } from '@/constants/errorCodes';
import { RoutePath } from '@/constants/routes';

/**
 * 直接覆寫 axios adapter，無需引入 axios-mock-adapter 第三方相依
 * 對應「禁止過度引入相依」與保持 lock file 精簡原則
 */

interface MockHandler {
  status: number;
  body: unknown;
}

let handlers: Map<string, MockHandler>;
let originalAdapter: AxiosAdapter | undefined;

const installMockAdapter = (): void => {
  handlers = new Map();
  originalAdapter = http.defaults.adapter as AxiosAdapter | undefined;
  http.defaults.adapter = (config) => {
    const key = `${(config.method ?? 'POST').toUpperCase()} ${config.url ?? ''}`;
    const handler = handlers.get(key);
    if (handler === undefined) {
      return Promise.reject(new Error(`No mock for ${key}`));
    }
    const response: AxiosResponse = {
      data: handler.body,
      status: handler.status,
      statusText: 'OK',
      headers: {},
      config,
    };
    // axios 會依 status 判斷成功/失敗（預設 2xx 為成功）
    if (handler.status >= 200 && handler.status < 300) {
      return Promise.resolve(response);
    }
    // 模擬 axios 對非 2xx 的 reject 行為
    const error = Object.assign(new Error(`Request failed with status code ${handler.status}`), {
      isAxiosError: true,
      response,
      config,
      toJSON: () => ({}),
    });
    return Promise.reject(error);
  };
};

const restoreAdapter = (): void => {
  if (originalAdapter !== undefined) {
    http.defaults.adapter = originalAdapter;
  }
  handlers.clear();
};

const mockPost = (url: string, status: number, body: unknown): void => {
  handlers.set(`POST ${url}`, { status, body });
};

describe('http envelope interceptor', () => {
  beforeEach(() => {
    installMockAdapter();
    useAuthStore.getState().logout();
  });

  afterEach(() => {
    restoreAdapter();
    setAppNavigate(null);
  });

  it('code === 0 時應解出 envelope.data', async () => {
    mockPost('/api/v1/test/get', 200, {
      code: 0,
      message: 'success',
      data: { id: 'abc', name: 'Hello' },
      timestamp: '2026-04-22T08:00:00.000+08:00',
      traceId: 'trace-1',
    });

    const data = await postJson<{ id: string }, { id: string; name: string }>('/api/v1/test/get', { id: 'abc' });
    expect(data).toEqual({ id: 'abc', name: 'Hello' });
  });

  it('code !== 0 時應拋出 BusinessError 並保留 traceId', async () => {
    mockPost('/api/v1/test/get', 200, {
      code: 2001,
      message: '使用者不存在',
      data: null,
      timestamp: '2026-04-22T08:00:00.000+08:00',
      traceId: 'trace-2',
    });

    await expect(postJson('/api/v1/test/get', {})).rejects.toMatchObject({
      name: 'BusinessError',
      code: 2001,
      message: '使用者不存在',
      traceId: 'trace-2',
    });
  });

  it('BusinessError 應為 Error 子類別', () => {
    const err = new BusinessError(9999, 'unknown');
    expect(err).toBeInstanceOf(Error);
    expect(err.code).toBe(9999);
    expect(err.name).toBe('BusinessError');
  });
});

describe('http auth redirect (B-01 修正)', () => {
  let navigateSpy: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    installMockAdapter();
    useAuthStore.getState().logout();
    navigateSpy = vi.fn();
    setAppNavigate(navigateSpy);
  });

  afterEach(() => {
    restoreAdapter();
    setAppNavigate(null);
  });

  it('業務碼 3003（TOKEN_INVALID）應清空 auth、navigate 到 /login 並帶 from', async () => {
    // 先建立登入狀態與一個非 /login 的當前路徑
    useAuthStore.getState().login('jwt-token', 'refresh-token', {
      userId: 'u-1',
      email: 'user@example.com',
      displayName: 'Tester',
      status: 'ACTIVE',
      emailVerifiedAt: '2026-04-22T08:00:00.000+08:00',
      notifyEmailEnabled: true,
      notifyWebEnabled: true,
      createdAt: '2026-04-22T08:00:00.000+08:00',
    });
    window.history.replaceState({}, '', '/watchlist');

    mockPost('/api/v1/test/get', 200, {
      code: ErrorCode.TOKEN_INVALID,
      message: 'token revoked',
      data: null,
      timestamp: '2026-04-22T08:00:00.000+08:00',
      traceId: 'trace-3003',
    });

    await expect(postJson('/api/v1/test/get', {})).rejects.toMatchObject({
      code: ErrorCode.TOKEN_INVALID,
      traceId: 'trace-3003',
    });

    // 必須有 navigate 動作（非 hard reload）
    expect(navigateSpy).toHaveBeenCalledTimes(1);
    expect(navigateSpy).toHaveBeenCalledWith(RoutePath.LOGIN, {
      replace: true,
      state: { from: '/watchlist' },
    });

    // auth 應已清空
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
    expect(useAuthStore.getState().token).toBeNull();
  });

  it('業務碼 3002（TOKEN_EXPIRED）目前 fallback 為跳登入，traceId 仍透傳', async () => {
    window.history.replaceState({}, '', '/profile');

    mockPost('/api/v1/test/get', 200, {
      code: ErrorCode.TOKEN_EXPIRED,
      message: 'token expired',
      data: null,
      timestamp: '2026-04-22T08:00:00.000+08:00',
      traceId: 'trace-3002',
    });

    await expect(postJson('/api/v1/test/get', {})).rejects.toMatchObject({
      code: ErrorCode.TOKEN_EXPIRED,
      traceId: 'trace-3002',
    });

    expect(navigateSpy).toHaveBeenCalledWith(RoutePath.LOGIN, {
      replace: true,
      state: { from: '/profile' },
    });
  });

  it('業務碼 3001（UNAUTHORIZED）應跳登入並帶 from', async () => {
    window.history.replaceState({}, '', '/home');

    mockPost('/api/v1/test/get', 200, {
      code: ErrorCode.UNAUTHORIZED,
      message: 'not logged in',
      data: null,
      timestamp: '2026-04-22T08:00:00.000+08:00',
      traceId: 'trace-3001',
    });

    await expect(postJson('/api/v1/test/get', {})).rejects.toMatchObject({
      code: ErrorCode.UNAUTHORIZED,
      traceId: 'trace-3001',
    });

    expect(navigateSpy).toHaveBeenCalledWith(RoutePath.LOGIN, {
      replace: true,
      state: { from: '/home' },
    });
  });

  it('HTTP 401（4xx）應從 envelope 解出 traceId 並跳登入', async () => {
    window.history.replaceState({}, '', '/watchlist');

    mockPost('/api/v1/test/get', 401, {
      code: ErrorCode.UNAUTHORIZED,
      message: 'unauthorized',
      data: null,
      timestamp: '2026-04-22T08:00:00.000+08:00',
      traceId: 'trace-401',
    });

    await expect(postJson('/api/v1/test/get', {})).rejects.toMatchObject({
      code: ErrorCode.UNAUTHORIZED,
      traceId: 'trace-401',
    });

    expect(navigateSpy).toHaveBeenCalledWith(RoutePath.LOGIN, {
      replace: true,
      state: { from: '/watchlist' },
    });
  });

  it('當前路徑已在 /login 時不應帶 from 避免迴圈', async () => {
    window.history.replaceState({}, '', RoutePath.LOGIN);

    mockPost('/api/v1/test/get', 200, {
      code: ErrorCode.TOKEN_INVALID,
      message: 'invalid',
      data: null,
      timestamp: '2026-04-22T08:00:00.000+08:00',
      traceId: 'trace-loop',
    });

    await expect(postJson('/api/v1/test/get', {})).rejects.toMatchObject({
      code: ErrorCode.TOKEN_INVALID,
    });

    expect(navigateSpy).toHaveBeenCalledWith(RoutePath.LOGIN, {
      replace: true,
      state: undefined,
    });
  });

  it('一般業務錯誤（如 2011 密碼錯誤）不應觸發 navigate', async () => {
    mockPost('/api/v1/test/get', 200, {
      code: ErrorCode.EMAIL_OR_PASSWORD_INCORRECT,
      message: 'wrong password',
      data: null,
      timestamp: '2026-04-22T08:00:00.000+08:00',
      traceId: 'trace-2011',
    });

    await expect(postJson('/api/v1/test/get', {})).rejects.toMatchObject({
      code: ErrorCode.EMAIL_OR_PASSWORD_INCORRECT,
      traceId: 'trace-2011',
    });

    expect(navigateSpy).not.toHaveBeenCalled();
  });
});
