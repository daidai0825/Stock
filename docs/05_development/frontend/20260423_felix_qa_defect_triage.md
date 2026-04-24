# Felix QA 缺陷 Triage 報告

| 項目 | 內容 |
|------|------|
| 撰寫者 | Felix（資深前端工程師） |
| 日期 | 2026-04-23 |
| 對應 QA 報告 | docs/07_qa/test-cases/20260423_quinn_wave2_e2e_cases.md |
| 缺陷來源 | Quinn Wave 2 E2E 測試 |
| 涵蓋缺陷 | BUG-Q-001、SPEC-Q-002 |

---

## 一、BUG-Q-001：i18n 語言切換「閃爍」問題

### 確認狀態

**confirmed（已確認，為潛在問題）**

### 根因分析

檢查以下三個關鍵點：

**1. `i18n/index.ts` 初始化方式**

```typescript
// 目前實作
void i18n.use(initReactI18next).init({
  resources: { 'zh-TW': { translation: zhTW }, en: { translation: en } },
  lng: 'zh-TW',        // 硬編碼預設語言，不讀 localStorage
  fallbackLng: 'zh-TW',
  interpolation: { escapeValue: false },
  returnNull: false,
});
```

現行實作使用靜態 `resources`（JSON 直接 import），`init()` 是同步完成的（無 backend plugin、無非同步載入）。因此 i18n 實際上在模組 import 時就已初始化完畢，不存在非同步 loading 期。

**2. `main.tsx` 的啟動順序**

```
1. import '@/i18n'（模組 side-effect → 同步 init）
2. enableMocks()（非同步等待 MSW worker）
3. createRoot(...).render(...)
```

App.tsx 透過 `import '@/i18n'` 觸發初始化，而 React tree 在 `enableMocks()` await 結束後才 mount。因此 i18n 在 React 掛載前必然已就緒，**MSW 等待期間不會有 React 樹在運行**，理論上不構成 race condition。

**3. Quinn 指出的真實場景**

Quinn 關注的是「語言切換」而非「首次載入」。Playwright 用 `page.addInitScript` 在頁面 JS 執行前注入 `localStorage.i18nextLng`，但目前 `i18n/index.ts` 的 `lng` 設定為 **硬編碼 `'zh-TW'`**，完全不讀取 `localStorage`。

這意味著：
- Playwright 注入 `localStorage.i18nextLng = 'en'` 完全無效
- TC-Q-002-05（en 文字驗證）測試將永遠失敗，因為 i18n 永遠以 zh-TW 初始化
- 不存在「閃爍」，但存在「語言無法切換」的功能性缺失

**4. React 18 Concurrent Mode 影響**

`StrictMode` 在 dev 模式下會 double-invoke render，若 `useTranslation()` 在第一次 render 時取值為 fallback，第二次才拿到正確翻譯，視覺上可能出現一瞬間的 key string 或 fallback 語言字串。但因目前資源為同步 bundle，此風險較低。

**根本問題**：`i18n/index.ts` 不讀取 `localStorage.i18nextLng`，導致 Playwright 語言注入機制失效，且缺乏明確性初始化保護（如 `initReady` 旗標）。

### 影響範圍

| 受影響項目 | 說明 |
|-----------|------|
| TC-Q-002-04（zh-TW 文字驗證） | 可能正常（預設就是 zh-TW） |
| TC-Q-002-05（en 文字驗證） | 必然失敗，i18n 不讀 localStorage |
| TC-Q-005（5010-5014 en notification） | 同上，所有 en 語言的 Playwright 測試均受影響 |
| 使用者語言偏好持久化 | 目前無法保存（刷新後回 zh-TW） |
| 所有頁面 | 範圍廣泛，因為 i18n 為全域單例 |

### 修復難度

**S（Small）** — 修改 `i18n/index.ts` 讀取 `localStorage` 即可，實作面簡單。加上型別安全的語言 resolver 約 2-3 小時。

| 項目 | 估計 |
|------|------|
| 修改 i18n/index.ts | 1 小時 |
| 加入 vite-env.d.ts query param 支援 | 1 小時 |
| 單元測試更新 | 0.5 小時 |
| **合計** | **2.5 小時** |

### 修復方案示意

```typescript
// i18n/index.ts 修改方向

/** 安全讀取初始語言（優先級：query param > localStorage > 預設 zh-TW）
 *
 * 理由：
 * - query param 提供給 QA / Playwright 確定性覆蓋，不依賴 localStorage 時序
 * - localStorage 保存使用者語言偏好
 * - 預設 zh-TW 符合台灣股市為主要市場
 */
const SUPPORTED_LANGS = ['zh-TW', 'en'] as const;
type SupportedLang = (typeof SUPPORTED_LANGS)[number];

const isSupportedLang = (val: unknown): val is SupportedLang =>
  SUPPORTED_LANGS.includes(val as SupportedLang);

const resolveInitialLang = (): SupportedLang => {
  // 1. query param（?lang=en）：QA / Playwright 專用，優先級最高
  const params = new URLSearchParams(window.location.search);
  const queryLang = params.get('lang');
  if (isSupportedLang(queryLang)) return queryLang;

  // 2. localStorage（使用者偏好持久化）
  const stored = localStorage.getItem('i18nextLng');
  if (isSupportedLang(stored)) return stored;

  // 3. 預設 zh-TW
  return 'zh-TW';
};

void i18n.use(initReactI18next).init({
  resources: {
    'zh-TW': { translation: zhTW },
    en: { translation: en },
  },
  lng: resolveInitialLang(),
  fallbackLng: 'zh-TW',
  interpolation: { escapeValue: false },
  returnNull: false,
});
```

Playwright 注入改為 query param 方式（更可靠）：

```typescript
// Playwright test helper（示意）
await page.goto('/stocks/2330?lang=en');
// 不再依賴 localStorage addInitScript
```

### 是否 Block Wave 2 Release

**視測試策略決定**：

- 若 Quinn E2E 測試中 TC-Q-002-05 與 TC-Q-005 系列納入 Wave 2 必過清單 → **Block**
- 若上述 en 語言 E2E 測試改為 Wave 3 再驗 → **Not Block**（功能性 bug，UX 影響低）

建議：在 Wave 2 內修復（工時小，且 TC-Q-005 為 P1 測試案例），避免技術債累積。

---

## 二、SPEC-Q-002：5014 MOPS_FORMAT_CHANGED 不應顯示重試按鈕

### 確認狀態

**confirmed（已確認，為 Spec 未明確定義導致的行為缺失）**

### 根因分析

**1. 現行 `errorCodes.ts` 的 `isDataSourceError`**

```typescript
// 目前實作
export const isDataSourceError = (code: number): boolean =>
  code >= 5010 && code <= 5014;
```

此函式將 5010-5014 一律視為同質錯誤。但程式碼註解中已明確標注：

```typescript
// 注意：5014 (MOPS_FORMAT_CHANGED) 解析失敗，不應提供重試按鈕。
```

**設計意圖已存在，但缺少對應的區分函式。**

**2. `useQueryErrorNotification` 的 description 邏輯**

```typescript
// StockDetail/index.tsx
const useQueryErrorNotification = (error: Error | null, context: string): void => {
  const { t } = useTranslation();
  useEffect(() => {
    if (error === null) return;
    const traceId = error instanceof BusinessError ? error.traceId : undefined;
    const message = `${context}：${error.message}`;
    const description = traceId !== undefined
      ? `traceId：${traceId}`           // 有 traceId → 顯示 traceId（無重試字樣）
      : t('common.errorRetry');          // 無 traceId → 顯示「發生錯誤，請稍後再試」
    notification.error({ message, description, duration: 5 });
  }, [error, context, t]);
};
```

實際情況：BusinessError 必含 `traceId`（由 `http.ts` 攔截器確保），因此 **所有業務錯誤都會走「顯示 traceId」的路徑**，description 不含重試字樣。

**結論：前端 notification 本身並沒有顯示重試按鈕**（Ant Design `notification.error` API 無內建「重試按鈕」，description 只是文字）。Quinn 報告的問題在語意層面：description 是否傳達「可以重試」的錯誤概念。

**3. zh-TW.json 中 5014 的文字**

```json
"5014": "MOPS 資料格式異動，請聯絡系統管理員"
```

此文字已在 `errors.5014` 鍵中定義，且語意正確（「聯絡系統管理員」而非「請稍後再試」）。

**問題核心**：`useQueryErrorNotification` 的 `message` 欄位直接使用 `error.message`，而 `error.message` 來自後端 envelope 的英文訊息（`'MOPS response format has changed; parser needs update'`），不是前端 i18n 的 `errors.5014` 翻譯字串。換言之，**前端 i18n 的 5014 錯誤文字雖然正確，但目前根本沒有被使用**。

所有 5xxx 錯誤通知顯示的是後端英文 message，不是前端 i18n 翻譯。這是比 Quinn 指出的問題更根本的設計缺失。

### 影響範圍

| 受影響項目 | 說明 |
|-----------|------|
| 所有 5010-5014 錯誤的 notification message | 顯示後端英文，未使用 i18n |
| TC-Q-004-05（5014 zh-TW 通知文字驗證） | 預期「MOPS 資料格式異動，請聯絡系統管理員」但實際顯示後端英文 |
| TC-Q-005-02（5014 en 通知文字驗證） | 同上，en.json 的 5014 也未使用 |
| TC-Q-004-01 ~ 04（5010-5013 所有通知文字） | 同樣受影響 |
| StockDetail 頁面 | 唯一使用 `useQueryErrorNotification` 的頁面 |

### 修復難度

**M（Medium）** — 需在 `useQueryErrorNotification` 加入錯誤碼 → i18n key 的對應邏輯，並對 5014 做分支處理。同時需要澄清 Spec：5014 的 notification 行為要不同於 5010-5013。

| 項目 | 估計 |
|------|------|
| 修改 `useQueryErrorNotification` 使用 i18n message | 1 小時 |
| 新增 `isFatalDataSourceError` 並加入 description 分支 | 0.5 小時 |
| 更新 `errorCodes.ts` | 0.5 小時 |
| 更新單元測試 | 1 小時 |
| **合計** | **3 小時** |

### 處理方案選項（待 Peter 拍板）

#### 方案 A：僅區分 description 文字（最小修改）

在 `useQueryErrorNotification` 中，根據錯誤碼決定 description：
- 5010-5013：description 顯示 `traceId：xxx`，語意為「暫時性問題，工程師可追蹤」
- 5014：description 顯示「資料來源異常，請聯絡工程團隊。traceId：xxx」，強調非使用者可解決

同時修正 message 改用 i18n 翻譯（`t('errors.5014')`）。

優點：改動小，不影響 UI 結構；缺點：notification 視覺上仍相同（均為 error 型）。

#### 方案 B：5014 使用不同 notification type（建議方向）

```typescript
// 概念示意
const isFatalDataSourceError = (code: number): boolean => code === ErrorCode.MOPS_FORMAT_CHANGED;

// 5010-5013：notification.error（可恢復，顯示 traceId）
// 5014：notification.warning 或 notification.error + 特定 description（不含重試暗示）
// description 固定為 i18n key：errors.fatalDataSource 或複用 errors.5014
```

新增 i18n key 範例：
```json
// zh-TW.json
"errors": {
  "fatalDataSourceDesc": "此錯誤需後端工程師處理，無法透過重試解決。traceId：{{traceId}}"
}
```

優點：視覺語意清楚；缺點：需新增 i18n key，需 Peter 確認文案。

#### 方案 C：5014 顯示 inline 狀態而非 notification（破壞性較大，Wave 3 議題）

針對 FundamentalCard 直接渲染一個「資料異常」的空狀態元件，取代 notification。使用者可看到哪個資料塊出問題，而非彈窗。

優點：UX 最清楚；缺點：需修改 StockDetail 整體錯誤處理架構，工時較大（M-L），建議 Wave 3 評估。

**Felix 建議方向：方案 B**

理由：
1. 在現有 notification 架構下改動最小
2. 視覺上區分「可等待恢復」vs「需工程師介入」
3. i18n 鍵已有正確文字（`errors.5014`），需新增的只是 description 鍵
4. 需要 Peter 確認 description 文案措辭，以及是否新增 `isFatalDataSourceError` 到 `errorCodes.ts`

### 同步發現的根本問題（需一併修復）

**`useQueryErrorNotification` 的 message 未使用 i18n**：目前所有 5xxx 通知的 message 顯示後端英文字串，應改為：

```typescript
// 修改前
const message = `${context}：${error.message}`;

// 修改後（根據錯誤碼查 i18n，fallback 才用 error.message）
const errorMessage = error instanceof BusinessError
  ? (t(`errors.${error.code}`, { defaultValue: error.message }) as string)
  : error.message;
const message = `${context}：${errorMessage}`;
```

此修正涵蓋 TC-Q-004 與 TC-Q-005 全部 Playwright 測試案例，是 i18n 5014 問題的前提修復。

### 是否 Block Wave 2 Release

**Block（TC-Q-004 為 P0 測試案例）**

TC-Q-004-01 ~ TC-Q-004-05（5010-5014 zh-TW 通知文字驗證）均為 P0，且驗證 notification message 是否使用 i18n 翻譯。目前實作必然失敗（顯示後端英文）。需在 Wave 2 release 前修復。

---

## 三、整體缺陷摘要表

| 缺陷 ID | 確認狀態 | 根因 | 影響範圍 | 難度 | 預估工時 | Block Wave 2 |
|---------|----------|------|---------|------|---------|--------------|
| BUG-Q-001 | confirmed | i18n 不讀 localStorage，Playwright 語言注入失效 | 所有 en 語言 E2E 測試（TC-Q-002-05、TC-Q-005） | S | 2.5 hr | 視測試策略（若 TC-Q-005 為 P1 必過） |
| SPEC-Q-002 | confirmed | notification message 未使用 i18n；5014 與 5010-5013 無語意區分 | TC-Q-004 全部（P0）、TC-Q-005 | M | 3 hr | **Yes（P0）** |

### 修復優先順序建議

1. **SPEC-Q-002 先行**：TC-Q-004 為 P0，Block Wave 2，工時估計 3 小時，應立即排入 sprint。修復含兩個子工作：
   - (a) `useQueryErrorNotification` 改用 i18n message
   - (b) 5014 加入 `isFatalDataSourceError` 分支（待 Peter spec 確認）

2. **BUG-Q-001 同步進行**：工時小（2.5 小時），且修復後 Playwright en 語言測試才能正確執行。建議與 SPEC-Q-002 同批 PR。

### 需要 Peter 確認的 Spec 決策

針對 SPEC-Q-002，請 Peter 就以下問題拍板：

1. 5014 的 notification description 文案是否採用「此錯誤需後端工程師處理，無法透過重試解決」？或有其他措辭？
2. 5014 是否沿用 `notification.error`（紅色），或改用 `notification.warning`（橘色）以區隔嚴重度？
3. `isFatalDataSourceError` 目前只含 5014，未來是否有其他「結構性錯誤」碼需納入？

### 說明事項

- Quinn 提到「顯示重試按鈕」的描述在 Ant Design `notification.error` API 層面不準確（notification 本身無按鈕 API），但語意問題（description 暗示可重試 vs. 不可重試）已確認存在，且根因比原描述更深（i18n 翻譯根本未被使用）。
- BUG-Q-002（MSW 5013/5014 handlers）、BUG-Q-003（period 白名單）均已確認為已解決，不在本次 triage 範圍。

---

*Felix — 2026-04-23*
