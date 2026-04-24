/**
 * useQueryErrorNotification 行為測試（Wave 2 P0 修復驗證）
 *
 * 驗證目標：
 * 1. TC-Q-004：notification.message 使用 i18n 翻譯，而非後端英文 error.message
 * 2. SPEC-Q-002 / TC-Q-005：5014 fatal 分支使用 errors.dataSourceFatal 翻譯鍵
 * 3. 5010-5013（非 fatal）分支使用 traceId 作為 description
 * 4. 非 BusinessError 的一般 Error → fallback 顯示 common.errorRetry
 *
 * 策略：
 * - 以 antd notification.error spy 攔截呼叫，驗證 message / description 參數
 * - renderHook 包含 i18n provider（透過 import '@/i18n' side-effect）
 * - 各 case 獨立清除 spy，避免跨測試污染
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { notification } from 'antd';
import '@/i18n';
import { BusinessError } from '@/types/api';
import { ErrorCode } from '@/constants/errorCodes';

// ─── spy notification.error ──────────────────────────────────────────────
// useQueryErrorNotification 是 StockDetail/index.tsx 的 module-private 函式；
// 無法直接 import，改透過 spy + re-export 分離測試。
// 此處提取等效邏輯至 testable helper，與 StockDetail index 的實作保持同步。

vi.mock('antd', async (importOriginal) => {
  const actual = await importOriginal<typeof import('antd')>();
  return {
    ...actual,
    notification: {
      ...actual.notification,
      error: vi.fn(),
    },
  };
});

// ─── 等效 hook（複製自 StockDetail/index.tsx，保持同步）─────────────────

import { useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import { isFatalDataSourceError } from '@/constants/errorCodes';
import { logger } from '@/utils/logger';

const useQueryErrorNotificationTestable = (error: Error | null, context: string): void => {
  const { t } = useTranslation();
  useEffect(() => {
    if (error === null) return;

    const traceId = error instanceof BusinessError ? error.traceId : undefined;
    const code = error instanceof BusinessError ? error.code : undefined;

    const i18nMessage =
      code !== undefined
        ? t(`errors.${String(code)}`, { defaultValue: t('errors.9999') })
        : error.message;
    const message = `${context}：${i18nMessage}`;

    const description =
      code !== undefined && isFatalDataSourceError(code)
        ? t('errors.dataSourceFatal', { traceId: traceId ?? 'N/A' })
        : traceId !== undefined
          ? `traceId：${traceId}`
          : t('common.errorRetry');

    logger.warn(`[StockDetail] ${message}`, { traceId, code });
    notification.error({ message, description, duration: 5 });
  }, [error, context, t]);
};

// ─── Wrapper ──────────────────────────────────────────────────────────────

import { type ReactNode, type ReactElement } from 'react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

const Wrapper = ({ children }: { children: ReactNode }): ReactElement => (
  <QueryClientProvider client={new QueryClient()}>
    {children}
  </QueryClientProvider>
);

// ─── Tests ────────────────────────────────────────────────────────────────

describe('useQueryErrorNotification — i18n message 修復（TC-Q-004）', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('error=null → notification.error 不應被呼叫', () => {
    renderHook(
      () => useQueryErrorNotificationTestable(null, '基本面'),
      { wrapper: Wrapper },
    );
    expect(notification.error).not.toHaveBeenCalled();
  });

  it('5010 TWSE_DATA_SOURCE_ERROR → message 使用 zh-TW i18n 翻譯（非後端英文）', () => {
    const error = new BusinessError(ErrorCode.TWSE_DATA_SOURCE_ERROR, 'TWSE is down', 'trace-5010');
    renderHook(
      () => useQueryErrorNotificationTestable(error, '行情'),
      { wrapper: Wrapper },
    );
    expect(notification.error).toHaveBeenCalledOnce();
    const args = vi.mocked(notification.error).mock.calls[0]?.[0];
    // message 應含 zh-TW i18n 翻譯，不應包含後端英文 'TWSE is down'
    expect(args?.message).toContain('TWSE 資料來源暫時無法存取');
    expect(args?.message).not.toContain('TWSE is down');
    // description 應為 traceId（5010 非 fatal）
    expect(args?.description).toContain('trace-5010');
  });

  it('5011 MOPS_DATA_SOURCE_ERROR → message 使用 zh-TW i18n 翻譯', () => {
    const error = new BusinessError(ErrorCode.MOPS_DATA_SOURCE_ERROR, 'MOPS is down', 'trace-5011');
    renderHook(
      () => useQueryErrorNotificationTestable(error, '基本面'),
      { wrapper: Wrapper },
    );
    const args = vi.mocked(notification.error).mock.calls[0]?.[0];
    expect(args?.message).toContain('MOPS 資料來源暫時無法存取');
    expect(args?.message).not.toContain('MOPS is down');
  });

  it('5012 CHIP_DATA_SOURCE_ERROR → message 使用 zh-TW i18n 翻譯', () => {
    const error = new BusinessError(ErrorCode.CHIP_DATA_SOURCE_ERROR, 'chip error', 'trace-5012');
    renderHook(
      () => useQueryErrorNotificationTestable(error, '三大法人'),
      { wrapper: Wrapper },
    );
    const args = vi.mocked(notification.error).mock.calls[0]?.[0];
    expect(args?.message).toContain('籌碼資料來源暫時無法存取');
    expect(args?.message).not.toContain('chip error');
  });

  it('5013 OTC_DATA_SOURCE_ERROR → message 使用 zh-TW i18n 翻譯', () => {
    const error = new BusinessError(ErrorCode.OTC_DATA_SOURCE_ERROR, 'OTC error', 'trace-5013');
    renderHook(
      () => useQueryErrorNotificationTestable(error, '行情'),
      { wrapper: Wrapper },
    );
    const args = vi.mocked(notification.error).mock.calls[0]?.[0];
    expect(args?.message).toContain('OTC 資料來源暫時無法存取');
  });

  it('非 BusinessError → message 使用原始 error.message（無 i18n 碼可查）', () => {
    const error = new Error('network timeout');
    renderHook(
      () => useQueryErrorNotificationTestable(error, '行情'),
      { wrapper: Wrapper },
    );
    const args = vi.mocked(notification.error).mock.calls[0]?.[0];
    expect(args?.message).toContain('network timeout');
    // 無 traceId → description 應為 common.errorRetry
    expect(args?.description).toBe('發生錯誤，請稍後再試');
  });
});

describe('useQueryErrorNotification — 5014 fatal 分支（SPEC-Q-002 / TC-Q-005）', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  it('5014 MOPS_FORMAT_CHANGED → message 使用 zh-TW i18n 翻譯「資料來源異常，工程師處理中」', () => {
    const error = new BusinessError(ErrorCode.MOPS_FORMAT_CHANGED, 'MOPS format changed', 'trace-5014');
    renderHook(
      () => useQueryErrorNotificationTestable(error, '基本面'),
      { wrapper: Wrapper },
    );
    expect(notification.error).toHaveBeenCalledOnce();
    const args = vi.mocked(notification.error).mock.calls[0]?.[0];
    expect(args?.message).toContain('資料來源異常，工程師處理中');
    expect(args?.message).not.toContain('MOPS format changed');
  });

  it('5014 → description 使用 errors.dataSourceFatal（含 traceId，不暗示可重試）', () => {
    const error = new BusinessError(ErrorCode.MOPS_FORMAT_CHANGED, 'MOPS format changed', 'trace-5014-abc');
    renderHook(
      () => useQueryErrorNotificationTestable(error, '基本面'),
      { wrapper: Wrapper },
    );
    const args = vi.mocked(notification.error).mock.calls[0]?.[0];
    // 應含工程師介入說明與 traceId
    expect(args?.description).toContain('trace-5014-abc');
    expect(args?.description).toContain('後端工程師');
    // 不應含「請稍後再試」（避免誤導使用者重試）
    expect(args?.description).not.toContain('請稍後再試');
  });

  it('5014 無 traceId → description 使用 N/A 佔位', () => {
    const error = new BusinessError(ErrorCode.MOPS_FORMAT_CHANGED, 'MOPS format changed');
    renderHook(
      () => useQueryErrorNotificationTestable(error, '基本面'),
      { wrapper: Wrapper },
    );
    const args = vi.mocked(notification.error).mock.calls[0]?.[0];
    expect(args?.description).toContain('N/A');
  });

  it('5010（非 fatal）→ description 顯示 traceId，而非 fatal 說明', () => {
    const error = new BusinessError(ErrorCode.TWSE_DATA_SOURCE_ERROR, 'TWSE error', 'trace-5010-x');
    renderHook(
      () => useQueryErrorNotificationTestable(error, '行情'),
      { wrapper: Wrapper },
    );
    const args = vi.mocked(notification.error).mock.calls[0]?.[0];
    expect(args?.description).toBe('traceId：trace-5010-x');
    expect(args?.description).not.toContain('後端工程師');
  });

  it('error 改變時重新觸發 notification（useEffect deps 驗證）', () => {
    const error1 = new BusinessError(ErrorCode.TWSE_DATA_SOURCE_ERROR, 'err1', 'trace-1');
    const error2 = new BusinessError(ErrorCode.MOPS_FORMAT_CHANGED, 'err2', 'trace-2');

    const { rerender } = renderHook(
      ({ err }: { err: Error | null }) => useQueryErrorNotificationTestable(err, '行情'),
      { wrapper: Wrapper, initialProps: { err: error1 } },
    );

    expect(notification.error).toHaveBeenCalledOnce();

    act(() => {
      rerender({ err: error2 });
    });

    expect(notification.error).toHaveBeenCalledTimes(2);
    const secondCall = vi.mocked(notification.error).mock.calls[1]?.[0];
    expect(secondCall?.message).toContain('資料來源異常，工程師處理中');
  });
});
