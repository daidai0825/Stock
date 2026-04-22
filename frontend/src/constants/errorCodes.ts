/**
 * 業務錯誤碼對照（前端側）
 * 對應 .claude/rules/api-design.md §業務錯誤碼分段：
 *   0           : 成功
 *   1000-1999   : 參數校驗
 *   2000-2999   : 業務邏輯
 *   3000-3999   : 權限相關
 *   4000-4999   : 資源相關
 *   5000-5999   : 第三方服務
 *   9000-9999   : 系統錯誤
 *
 * 與後端 Bruno 對齊；新增碼必須先協調後雙邊同步。
 * 對照來源：docs/05_development/backend/errorCodes.md（Bruno 維護）
 *
 * Wave A → Wave B 修正（2026-04-22）：
 * - 3002 由 FORBIDDEN 改為 TOKEN_EXPIRED（憑證逾時，前端可觸發 refresh）
 * - 3003 由 TOKEN_EXPIRED 改為 TOKEN_INVALID（憑證無效/被撤銷，必須重新登入）
 * - FORBIDDEN 移至 3004
 * - 新增 9003 FEATURE_NOT_AVAILABLE（功能尚未開放，取代原本誤用的 9001）
 */

export const ErrorCode = {
  SUCCESS: 0,

  // 參數校驗
  PARAM_MISSING: 1001,
  PARAM_FORMAT: 1002,
  PARAM_OUT_OF_RANGE: 1003,

  // 業務邏輯
  USER_NOT_FOUND: 2001,
  EMAIL_ALREADY_REGISTERED: 2010,
  PASSWORD_INCORRECT: 2011,
  EMAIL_NOT_VERIFIED: 2012,
  WATCHLIST_LIMIT_EXCEEDED: 2050,

  // 權限
  UNAUTHORIZED: 3001, // 未登入（無 token 或從未登入即進保護路由）
  TOKEN_EXPIRED: 3002, // Access token 過期（可觸發 refresh）
  TOKEN_INVALID: 3003, // Token 無效/被撤銷（必須重新登入）
  FORBIDDEN: 3004, // 已登入但無權限執行該操作

  // 資源
  RESOURCE_NOT_FOUND: 4001,
  RESOURCE_DELETED: 4002,

  // 第三方
  TWSE_UNAVAILABLE: 5101,

  // 系統
  SYSTEM_BUSY: 9001,
  FEATURE_NOT_AVAILABLE: 9003, // 功能尚未開放（取代過去誤用 9001 的位置）
  UNKNOWN: 9999,
} as const;

export type ErrorCodeValue = (typeof ErrorCode)[keyof typeof ErrorCode];

/**
 * 屬於「必須立即重新登入」的錯誤碼。
 * 包含：未登入（3001）、Token 無效（3003）。
 *
 * 注意：TOKEN_EXPIRED（3002）不在此列 — 該情境應觸發 refresh token 流程
 * （Wave B 接入），目前若 refresh hook 尚未實作則 fallback 走重新登入。
 */
export const isHardLogoutError = (code: number): boolean => {
  return code === ErrorCode.UNAUTHORIZED || code === ErrorCode.TOKEN_INVALID;
};

/**
 * 屬於「Token 過期可嘗試 refresh」的錯誤碼。
 * Wave A：尚無 refresh 實作，呼叫端可暫時 fallback 為 hard logout。
 * Wave B：useAuth 將在攔截器觸發此狀態時呼叫 refresh endpoint。
 */
export const isRefreshableAuthError = (code: number): boolean => {
  return code === ErrorCode.TOKEN_EXPIRED;
};

/**
 * 任一種需要將使用者導回登入頁的錯誤碼（hard logout 或 refresh fallback）。
 * 用於 http 攔截器決定是否跳轉。
 */
export const requiresRedirectToLogin = (code: number): boolean => {
  return isHardLogoutError(code) || isRefreshableAuthError(code);
};
