/**
 * 業務錯誤碼對照（前端側）
 *
 * 唯一事實源：docs/03_spec/20260422_errorCodes_central.md（Peter 維護）
 * 對應 Java：backend/stock-common/.../constant/ErrorCode.java（Bruno 維護）
 *
 * Wave B Round 2 對齊（2026-04-22）：
 * - 廢除 TWSE_UNAVAILABLE: 5101（與後端 5010 語意重疊，Jamie 仲裁 D4）
 * - 改為 TWSE_DATA_SOURCE_ERROR: 5010（後端 5010 體系）
 * - 新增 MOPS_DATA_SOURCE_ERROR: 5011
 * - 新增 CHIP_DATA_SOURCE_ERROR: 5012
 * - 新增 OTC_DATA_SOURCE_ERROR: 5013（Wave B 新增）
 * - 新增 MOPS_FORMAT_CHANGED: 5014（Wave B 新增）
 * - 修正 PARAM_MISSING → PARAM_REQUIRED（對齊 central 1001 Constant 名稱）
 * - 修正 PARAM_FORMAT → PARAM_FORMAT_INVALID（對齊 central 1002 Constant 名稱）
 * - 新增 PARAM_TOO_LONG: 1004
 * - 修正 USER_NOT_FOUND → 4002（原 2001 是舊版定義，central 已更正）
 * - 新增完整 2xxx、4xxx、9xxx 段落對齊 central 清單
 *
 * 禁止事項：
 * - 禁止直接修改此檔，必須先更新 docs/03_spec/20260422_errorCodes_central.md
 * - 新增 / 修改 / 廢除必須 Bruno + Felix 在同一 PR 同步
 */

export const ErrorCode = {
  SUCCESS: 0,

  // 參數校驗
  PARAM_REQUIRED: 1001,
  PARAM_FORMAT_INVALID: 1002,
  PARAM_OUT_OF_RANGE: 1003,
  PARAM_TOO_LONG: 1004,

  // 業務邏輯
  EMAIL_ALREADY_REGISTERED: 2010,
  EMAIL_OR_PASSWORD_INCORRECT: 2011,
  EMAIL_NOT_VERIFIED: 2012,
  VERIFY_TOKEN_EXPIRED: 2013,
  VERIFY_TOKEN_INVALID: 2014,
  VERIFY_RESEND_LIMIT: 2015,
  OLD_PASSWORD_INCORRECT: 2016,
  WATCHLIST_ALREADY_EXISTS: 2020,
  WATCHLIST_LIMIT_REACHED: 2021,
  WATCHLIST_GROUP_LIMIT: 2022,
  WATCHLIST_NOT_FOUND: 2023,   // Wave 3 新增
  ALERT_DUPLICATE: 2030,
  ALERT_DELETED: 2031,
  ALERT_LIMIT_REACHED: 2032,   // Wave 3 新增
  ACCOUNT_CLOSED: 2040,
  ACCOUNT_CLOSE_EXPIRED: 2041,

  // 權限
  UNAUTHORIZED: 3001,    // 未登入（無 token 或從未登入即進保護路由）
  TOKEN_EXPIRED: 3002,   // Access token 過期（可觸發 refresh）
  TOKEN_INVALID: 3003,   // Token 無效 / 被撤銷（必須重新登入）
  FORBIDDEN: 3004,       // 已登入但無此功能權限
  ACCOUNT_SUSPENDED: 3010,
  ACCOUNT_DELETED: 3011,

  // 資源
  STOCK_NOT_FOUND: 4001,
  USER_NOT_FOUND: 4002,
  ALERT_NOT_FOUND: 4003,
  NOTIFY_RECORD_NOT_FOUND: 4004,
  STOCK_DELISTED: 4010,

  // 第三方
  EMAIL_SERVICE_ERROR: 5001,
  WEB_PUSH_SERVICE_ERROR: 5002,
  TWSE_DATA_SOURCE_ERROR: 5010,   // 取代廢除的 TWSE_UNAVAILABLE: 5101
  MOPS_DATA_SOURCE_ERROR: 5011,
  CHIP_DATA_SOURCE_ERROR: 5012,
  OTC_DATA_SOURCE_ERROR: 5013,    // Wave B 新增
  MOPS_FORMAT_CHANGED: 5014,      // Wave B 新增；MOPS 解析失敗，需後端介入修復

  // 系統
  SYSTEM_BUSY: 9001,
  SYSTEM_MAINTENANCE: 9002,
  FEATURE_NOT_AVAILABLE: 9003,    // 功能尚未開放（Placeholder API）
  UNKNOWN_ERROR: 9999,
} as const;

export type ErrorCodeValue = (typeof ErrorCode)[keyof typeof ErrorCode];

/**
 * 屬於「必須立即重新登入」的錯誤碼。
 * 包含：未登入（3001）、Token 無效（3003）。
 *
 * 注意：TOKEN_EXPIRED（3002）不在此列 — 該情境應觸發 refresh token 流程。
 * Wave B 接入 refresh hook；若 refresh 失敗則 fallback 至 hard logout。
 */
export const isHardLogoutError = (code: number): boolean =>
  code === ErrorCode.UNAUTHORIZED || code === ErrorCode.TOKEN_INVALID;

/**
 * 屬於「Token 過期可嘗試 refresh」的錯誤碼。
 */
export const isRefreshableAuthError = (code: number): boolean =>
  code === ErrorCode.TOKEN_EXPIRED;

/**
 * 任一種需要將使用者導回登入頁的錯誤碼（hard logout 或 refresh fallback）。
 * 用於 http 攔截器決定是否跳轉。
 */
export const requiresRedirectToLogin = (code: number): boolean =>
  isHardLogoutError(code) || isRefreshableAuthError(code);

/**
 * 屬於「第三方資料來源不可用」的錯誤碼（5010-5014）。
 * 前端建議行為：顯示「資料來源暫時無法使用」提示，可顯示 isStale fallback 資料。
 */
export const isDataSourceError = (code: number): boolean =>
  code >= 5010 && code <= 5014;

/**
 * 屬於「結構性資料來源錯誤」的錯誤碼——需後端工程師介入，無法透過重試解決。
 *
 * 目前涵蓋：
 * - 5014 (MOPS_FORMAT_CHANGED)：MOPS HTML 格式異動導致解析器失效。
 *   此類錯誤需更新 parser 才能修復，對使用者顯示「工程師處理中」而非「請稍後再試」。
 *
 * UI 行為：notification.error + description 使用 `errors.dataSourceFatal` 翻譯鍵，
 * 不暗示可重試。
 */
export const isFatalDataSourceError = (code: number): boolean =>
  code === ErrorCode.MOPS_FORMAT_CHANGED;
