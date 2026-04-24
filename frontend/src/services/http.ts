import axios, { type AxiosInstance, type AxiosResponse, type AxiosError } from 'axios';
import { useAuthStore } from '@/stores/authStore';
import { tokenStorage } from '@/services/tokenStorage';
import { BusinessError, type ApiResponse } from '@/types/api';
import {
  ErrorCode,
  isHardLogoutError,
  isRefreshableAuthError,
  requiresRedirectToLogin,
} from '@/constants/errorCodes';
import { RoutePath } from '@/constants/routes';
import { appNavigate } from '@/services/navigator';

/**
 * HTTP 客戶端（對應 .claude/rules/api-design.md）
 *
 * 規範：
 * - 統一 POST 方法
 * - HTTP 200 + Envelope（code/message/data/traceId）
 * - code === 0 → 解 data
 * - code !== 0 → 拋 BusinessError（必含 traceId 透傳）
 * - 401 / 業務碼 3001（未登入）/ 3003（Token 無效）→ 清空 auth + 跳登入並帶 from
 * - 業務碼 3002（Token 過期）→ Wave A 暫時 fallback 為跳登入；Wave B 接 refresh token hook
 *
 * 不做：
 * - 降級處理（system-design.md 禁止）
 * - 本地快取（system-design.md 禁止）
 * - hard reload（會清掉 ProtectedRoute 透過 state 攜帶的 from）
 *
 * Wave A → Wave B 修補（2026-04-22, B-01）：
 * - 改用 React Router navigator 注入機制（services/navigator.ts），停用 window.location.assign
 * - 401 / 業務錯誤一律從 envelope 取 traceId，包進 BusinessError 透傳給上層
 * - 3002 / 3003 區分語意，並預留 refresh token hook 點
 * - prod 強制要求 VITE_API_BASE_URL 注入，避免靜默打 localhost（M-02）
 */

const resolveBaseUrl = (): string => {
  const configured = import.meta.env.VITE_API_BASE_URL;
  const env = import.meta.env.VITE_APP_ENV;

  if (typeof configured === 'string' && configured.length > 0) {
    return configured;
  }

  // prod build 必須注入，否則直接 throw 阻斷啟動（避免靜默打 localhost）
  if (import.meta.env.PROD === true || env === 'prod') {
    throw new Error(
      '[http] VITE_API_BASE_URL is required for prod build but was not injected. ' +
        'Please configure CI/CD environment variables.',
    );
  }

  // local / dev 等非 prod 環境允許 fallback，但加 console.warn 提醒
  console.warn(
    '[http] VITE_API_BASE_URL not set; falling back to http://localhost:8080. ' +
      'This fallback is only allowed in non-prod environments.',
  );
  return 'http://localhost:8080';
};

const baseURL = resolveBaseUrl();

export const http: AxiosInstance = axios.create({
  baseURL,
  timeout: 10_000,
  headers: {
    'Content-Type': 'application/json',
    Accept: 'application/json',
  },
});

// Request：帶 JWT
// M-FE-WaveB-01：改由 tokenStorage.get() 取得 token，
// 讓抽象層真正被使用；未來換 httpOnly cookie 只需修改 tokenStorage.ts。
http.interceptors.request.use((config) => {
  const token = tokenStorage.get();
  if (token !== null && token.length > 0) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

/**
 * 取得目前 location 路徑（測試環境 jsdom 可用 window.location；SSR 則回 null）。
 * 提供給 ProtectedRoute 在登入後返回原頁。
 */
const currentPathname = (): string | null => {
  if (typeof window === 'undefined') {
    return null;
  }
  const path = window.location.pathname;
  return path === RoutePath.LOGIN ? null : path;
};

/**
 * 將攔截器所需的 redirect 行為集中在此。
 *
 * 設計重點：
 * 1. 使用 React Router navigator（services/navigator.ts）而非 window.location.assign，
 *    避免 hard reload 清掉 ProtectedRoute 攜帶的 from state。
 * 2. 帶上 location.state.from = 當前路徑，讓使用者重新登入後可回原頁
 *    （Login 頁讀取 location.state.from 作為 navigate target）。
 * 3. logout 在 navigate 之前呼叫，確保下一次 ProtectedRoute 渲染時 isAuthenticated = false。
 */
const redirectToLogin = (): void => {
  useAuthStore.getState().logout();

  const from = currentPathname();
  appNavigate(RoutePath.LOGIN, {
    replace: true,
    state: from !== null ? { from } : undefined,
  });
};

/**
 * Wave B 預留 hook 點。
 * Wave A：直接 fallback 為 redirectToLogin，使用者需重新輸入帳密。
 * Wave B：呼叫 memberService.refreshToken() 並重試原請求；失敗才 redirectToLogin。
 */
const handleTokenExpired = (): void => {
  // TODO(Wave B): 接入 refresh token 流程
  // 1. 取 useAuthStore.getState().refreshToken
  // 2. 呼叫 memberService.refreshToken({ refreshToken })
  // 3. 成功 → 寫回 store + 重試原請求
  // 4. 失敗 → redirectToLogin()
  redirectToLogin();
};

/**
 * 從 axios 錯誤物件嘗試解出後端 envelope，保留 traceId / message / details。
 * 用於 4xx / 5xx 等 axios reject 路徑，避免遺失追蹤資訊。
 */
const buildBusinessErrorFromAxios = (error: AxiosError<ApiResponse<unknown>>): BusinessError => {
  const body = error.response?.data;
  if (body !== null && typeof body === 'object' && 'code' in body) {
    const envelope = body as ApiResponse<unknown>;
    return new BusinessError(
      envelope.code,
      envelope.message ?? error.message,
      envelope.traceId,
      envelope.errors,
    );
  }

  // 沒有 envelope（純網路錯誤、CORS、timeout 等）
  return new BusinessError(ErrorCode.UNKNOWN_ERROR, error.message !== '' ? error.message : 'network error');
};

/**
 * 依錯誤碼決定 redirect 行為（集中管理，方便測試覆蓋）。
 */
const dispatchAuthRedirect = (code: number): void => {
  if (isRefreshableAuthError(code)) {
    handleTokenExpired();
    return;
  }
  if (isHardLogoutError(code)) {
    redirectToLogin();
  }
};

// Response：解 Envelope；非 0 拋 BusinessError
http.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<unknown>>) => {
    const envelope = response.data;
    if (envelope === null || typeof envelope !== 'object' || !('code' in envelope)) {
      throw new BusinessError(ErrorCode.UNKNOWN_ERROR, 'Invalid envelope structure');
    }

    if (envelope.code === ErrorCode.SUCCESS) {
      // 攔截器層僅做解封，不改 status；具體 service 取 response.data.data
      return response;
    }

    if (requiresRedirectToLogin(envelope.code)) {
      dispatchAuthRedirect(envelope.code);
    }

    throw new BusinessError(envelope.code, envelope.message, envelope.traceId, envelope.errors);
  },
  (error: AxiosError<ApiResponse<unknown>>) => {
    // 先嘗試從 envelope 解 traceId（不論 4xx / 5xx），保留可追蹤性
    const businessError = buildBusinessErrorFromAxios(error);

    // HTTP 401 視同未登入；envelope 解出的 3001 / 3002 / 3003 走對應 redirect
    if (error.response?.status === 401 || requiresRedirectToLogin(businessError.code)) {
      // 若是 401 但沒有業務碼，視為 UNAUTHORIZED 觸發 hard logout
      const codeForRedirect =
        businessError.code === ErrorCode.UNKNOWN_ERROR ? ErrorCode.UNAUTHORIZED : businessError.code;
      dispatchAuthRedirect(codeForRedirect);
    }

    return Promise.reject(businessError);
  },
);

/**
 * 統一 POST helper：所有 service 用此，不直接呼叫 http.post，避免漏處理 envelope
 */
export const postJson = async <TReq, TRes>(path: string, body: TReq): Promise<TRes> => {
  const response = await http.post<ApiResponse<TRes>>(path, body);
  return response.data.data;
};
