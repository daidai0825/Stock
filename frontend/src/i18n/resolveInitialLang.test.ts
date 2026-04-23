/**
 * resolveInitialLang() 單元測試
 *
 * 驗證優先序：
 *   1. URL query param `?lang=`（最高）
 *   2. localStorage `i18nextLng`
 *   3. 預設 `zh-TW`（最低）
 *
 * 各測試皆在 beforeEach 重設 window.location 與 localStorage，
 * 確保測試間完全隔離。
 */

import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { resolveInitialLang } from './index';

// 儲存原始 window.location 以便還原
const originalLocation = window.location;

const setSearchParams = (search: string): void => {
  Object.defineProperty(window, 'location', {
    writable: true,
    value: { ...originalLocation, search },
  });
};

describe('resolveInitialLang()', () => {
  beforeEach(() => {
    localStorage.clear();
    // 還原 search 為空
    Object.defineProperty(window, 'location', {
      writable: true,
      value: { ...originalLocation, search: '' },
    });
  });

  afterEach(() => {
    // 還原真正的 window.location
    Object.defineProperty(window, 'location', {
      writable: true,
      value: originalLocation,
    });
  });

  // ── 預設行為 ──────────────────────────────────────────────────────────

  it('無任何來源時回傳預設 zh-TW', () => {
    expect(resolveInitialLang()).toBe('zh-TW');
  });

  // ── localStorage 優先序 ──────────────────────────────────────────────

  it('localStorage 設定 en → 回傳 en', () => {
    localStorage.setItem('i18nextLng', 'en');
    expect(resolveInitialLang()).toBe('en');
  });

  it('localStorage 設定 zh-TW → 回傳 zh-TW', () => {
    localStorage.setItem('i18nextLng', 'zh-TW');
    expect(resolveInitialLang()).toBe('zh-TW');
  });

  it('localStorage 設定非支援語言（fr）→ 降級到預設 zh-TW', () => {
    localStorage.setItem('i18nextLng', 'fr');
    expect(resolveInitialLang()).toBe('zh-TW');
  });

  it('localStorage 設定空字串 → 降級到預設 zh-TW', () => {
    localStorage.setItem('i18nextLng', '');
    expect(resolveInitialLang()).toBe('zh-TW');
  });

  // ── query param 優先序 ──────────────────────────────────────────────

  it('?lang=en → 回傳 en（不讀 localStorage）', () => {
    localStorage.setItem('i18nextLng', 'zh-TW');
    setSearchParams('?lang=en');
    expect(resolveInitialLang()).toBe('en');
  });

  it('?lang=zh-TW → 回傳 zh-TW', () => {
    setSearchParams('?lang=zh-TW');
    expect(resolveInitialLang()).toBe('zh-TW');
  });

  it('?lang=fr（非支援）→ 降級到 localStorage → 再降級到預設 zh-TW', () => {
    setSearchParams('?lang=fr');
    expect(resolveInitialLang()).toBe('zh-TW');
  });

  it('?lang=fr + localStorage=en → 降級到 localStorage en', () => {
    localStorage.setItem('i18nextLng', 'en');
    setSearchParams('?lang=fr');
    expect(resolveInitialLang()).toBe('en');
  });

  it('query param 優先於 localStorage：?lang=en + localStorage=zh-TW → en', () => {
    localStorage.setItem('i18nextLng', 'zh-TW');
    setSearchParams('?lang=en');
    expect(resolveInitialLang()).toBe('en');
  });

  it('?lang= 空值 → 降級到 localStorage', () => {
    localStorage.setItem('i18nextLng', 'en');
    setSearchParams('?lang=');
    expect(resolveInitialLang()).toBe('en');
  });
});
