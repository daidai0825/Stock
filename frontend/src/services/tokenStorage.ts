/**
 * Token Storage 抽象層（FE TD-3 Phase 1）
 *
 * 動機：
 * - react-typescript.md 禁止將 token 存於 localStorage（建議 httpOnly cookie）
 * - Wave A 因後端尚未提供 httpOnly cookie endpoint，過渡期採 localStorage
 * - 此抽象層確保未來切換為 cookie 時只需修改本檔案，呼叫端零改動
 *
 * 介面語意：
 *   get()   → 取得目前 token；若不存在回 null
 *   set()   → 存入 token
 *   clear() → 清除 token
 *
 * Wave B 實作：仍走 localStorage（透過 Zustand persist 管理）
 * Wave C/D 計畫：改為讀寫 httpOnly cookie（需後端配合 Set-Cookie header）
 *
 * 注意：authStore（Zustand）是 token 的 source of truth；
 * tokenStorage 是給非 React 樹（如 axios 攔截器）同步讀取用的薄封裝，
 * 不另開一份儲存，直接讀 authStore persist key。
 */

const AUTH_STORE_KEY = 'stock-auth-store';

export interface TokenStorage {
  get(): string | null;
  set(token: string): void;
  clear(): void;
}

/**
 * localStorage 實作（Wave B 過渡）
 * 直接操作 Zustand persist 的 JSON 結構，避免重複存兩份 token。
 */
const localStorageTokenStorage: TokenStorage = {
  get(): string | null {
    try {
      const raw = localStorage.getItem(AUTH_STORE_KEY);
      if (raw === null) return null;
      const parsed: unknown = JSON.parse(raw);
      if (
        parsed !== null &&
        typeof parsed === 'object' &&
        'state' in parsed &&
        parsed.state !== null &&
        typeof parsed.state === 'object' &&
        'token' in parsed.state &&
        typeof (parsed.state as Record<string, unknown>)['token'] === 'string'
      ) {
        return (parsed.state as Record<string, unknown>)['token'] as string;
      }
      return null;
    } catch {
      return null;
    }
  },

  set(token: string): void {
    try {
      const raw = localStorage.getItem(AUTH_STORE_KEY);
      const parsed: unknown = raw !== null ? JSON.parse(raw) : { state: {} };
      if (
        parsed !== null &&
        typeof parsed === 'object' &&
        'state' in parsed &&
        parsed.state !== null &&
        typeof parsed.state === 'object'
      ) {
        const updated = {
          ...parsed,
          state: { ...(parsed.state as Record<string, unknown>), token },
        };
        localStorage.setItem(AUTH_STORE_KEY, JSON.stringify(updated));
      }
    } catch {
      // 靜默失敗：Zustand 本身會在下次 store set 時覆蓋正確值
    }
  },

  clear(): void {
    try {
      const raw = localStorage.getItem(AUTH_STORE_KEY);
      if (raw === null) return;
      const parsed: unknown = JSON.parse(raw);
      if (
        parsed !== null &&
        typeof parsed === 'object' &&
        'state' in parsed &&
        parsed.state !== null &&
        typeof parsed.state === 'object'
      ) {
        const updated = {
          ...parsed,
          state: { ...(parsed.state as Record<string, unknown>), token: null },
        };
        localStorage.setItem(AUTH_STORE_KEY, JSON.stringify(updated));
      }
    } catch {
      // 靜默失敗
    }
  },
};

/** 應用程式唯一 token storage 實例（DI-friendly，未來可替換） */
export const tokenStorage: TokenStorage = localStorageTokenStorage;
