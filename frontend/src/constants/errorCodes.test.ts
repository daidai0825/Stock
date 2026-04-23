/**
 * errorCodes.ts 單元測試
 *
 * 涵蓋：
 * - isFatalDataSourceError()：5014 判斷
 * - isDataSourceError()：5010-5014 範圍
 * - isHardLogoutError()、isRefreshableAuthError()、requiresRedirectToLogin()：認證碼
 */

import { describe, it, expect } from 'vitest';
import {
  ErrorCode,
  isFatalDataSourceError,
  isDataSourceError,
  isHardLogoutError,
  isRefreshableAuthError,
  requiresRedirectToLogin,
} from './errorCodes';

// ─── isFatalDataSourceError ───────────────────────────────────────────────

describe('isFatalDataSourceError()', () => {
  it('5014 MOPS_FORMAT_CHANGED → true', () => {
    expect(isFatalDataSourceError(ErrorCode.MOPS_FORMAT_CHANGED)).toBe(true);
  });

  it('5014 直接數值 → true', () => {
    expect(isFatalDataSourceError(5014)).toBe(true);
  });

  it('5010 TWSE_DATA_SOURCE_ERROR → false（可重試）', () => {
    expect(isFatalDataSourceError(ErrorCode.TWSE_DATA_SOURCE_ERROR)).toBe(false);
  });

  it('5011 MOPS_DATA_SOURCE_ERROR → false（可重試）', () => {
    expect(isFatalDataSourceError(ErrorCode.MOPS_DATA_SOURCE_ERROR)).toBe(false);
  });

  it('5012 CHIP_DATA_SOURCE_ERROR → false', () => {
    expect(isFatalDataSourceError(ErrorCode.CHIP_DATA_SOURCE_ERROR)).toBe(false);
  });

  it('5013 OTC_DATA_SOURCE_ERROR → false', () => {
    expect(isFatalDataSourceError(ErrorCode.OTC_DATA_SOURCE_ERROR)).toBe(false);
  });

  it('0 SUCCESS → false', () => {
    expect(isFatalDataSourceError(ErrorCode.SUCCESS)).toBe(false);
  });

  it('9999 UNKNOWN_ERROR → false', () => {
    expect(isFatalDataSourceError(ErrorCode.UNKNOWN_ERROR)).toBe(false);
  });

  it('5015（未定義碼）→ false', () => {
    expect(isFatalDataSourceError(5015)).toBe(false);
  });
});

// ─── isDataSourceError ────────────────────────────────────────────────────

describe('isDataSourceError()', () => {
  it('5010 → true', () => {
    expect(isDataSourceError(5010)).toBe(true);
  });

  it('5011 → true', () => {
    expect(isDataSourceError(5011)).toBe(true);
  });

  it('5012 → true', () => {
    expect(isDataSourceError(5012)).toBe(true);
  });

  it('5013 → true', () => {
    expect(isDataSourceError(5013)).toBe(true);
  });

  it('5014 → true（fatal 也屬於 data source error 範疇）', () => {
    expect(isDataSourceError(5014)).toBe(true);
  });

  it('5009 → false（範圍外）', () => {
    expect(isDataSourceError(5009)).toBe(false);
  });

  it('5015 → false（範圍外）', () => {
    expect(isDataSourceError(5015)).toBe(false);
  });
});

// ─── 認證相關輔助函式（確保未被 fatal 修改破壞）────────────────────────────

describe('isHardLogoutError()', () => {
  it('3001 UNAUTHORIZED → true', () => {
    expect(isHardLogoutError(ErrorCode.UNAUTHORIZED)).toBe(true);
  });

  it('3003 TOKEN_INVALID → true', () => {
    expect(isHardLogoutError(ErrorCode.TOKEN_INVALID)).toBe(true);
  });

  it('3002 TOKEN_EXPIRED → false（走 refresh 流程）', () => {
    expect(isHardLogoutError(ErrorCode.TOKEN_EXPIRED)).toBe(false);
  });
});

describe('isRefreshableAuthError()', () => {
  it('3002 TOKEN_EXPIRED → true', () => {
    expect(isRefreshableAuthError(ErrorCode.TOKEN_EXPIRED)).toBe(true);
  });

  it('3001 UNAUTHORIZED → false', () => {
    expect(isRefreshableAuthError(ErrorCode.UNAUTHORIZED)).toBe(false);
  });
});

describe('requiresRedirectToLogin()', () => {
  it('3001 → true', () => {
    expect(requiresRedirectToLogin(ErrorCode.UNAUTHORIZED)).toBe(true);
  });

  it('3002 → true（refresh fallback）', () => {
    expect(requiresRedirectToLogin(ErrorCode.TOKEN_EXPIRED)).toBe(true);
  });

  it('3003 → true', () => {
    expect(requiresRedirectToLogin(ErrorCode.TOKEN_INVALID)).toBe(true);
  });

  it('5014 → false（不觸發登入跳轉）', () => {
    expect(requiresRedirectToLogin(ErrorCode.MOPS_FORMAT_CHANGED)).toBe(false);
  });
});
