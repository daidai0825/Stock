# Frontend Wave A Blocker 修補筆記（B-01 / FE-01）

| 項目 | 內容 |
|------|------|
| 修補日期 | 2026-04-22 |
| 執行者 | Felix（資深前端工程師） |
| 對應 Review | [20260422_review_waveA.md](../../06_review/frontend/20260422_review_waveA.md) §1 B-01 |
| 對應規範 | `react-typescript.md`、`api-design.md`、`code-style.md`、`conversation.md` |
| 結果 | ✅ lint / type-check / test 全綠（15/15 passed） |

---

## 1. 修補範圍

### 1.1 主要 Blocker（FE-01 / B-01）

`src/services/http.ts` 攔截器原以 `window.location.assign('/login')` 跳轉：

- 觸發 hard reload，清掉 React Router `location.state.from`
- 401 / 3001 / 3003 路徑直接拋固定訊息，遺失後端 envelope 內的 `traceId`

### 1.2 同步處理 Major（FE-M02）

`src/services/http.ts` baseURL fallback 到 `localhost:8080`，prod 漏注入 env 會靜默打到本機。

### 1.3 Bruno 對接：錯誤碼語意調整

| 錯誤碼 | Wave A 舊定義 | 修正後（Wave B 對齊） |
|--------|---------------|------------------------|
| 3002 | FORBIDDEN（無權限） | TOKEN_EXPIRED（憑證逾時，可 refresh） |
| 3003 | TOKEN_EXPIRED（憑證過期） | TOKEN_INVALID（憑證無效，必須重登） |
| 3004 | -（不存在） | FORBIDDEN（已登入但無權限）|
| 9003 | -（不存在） | FEATURE_NOT_AVAILABLE（功能尚未開放） |

---

## 2. 變更檔案清單

| 檔案 | 變更類型 | 說明 |
|------|----------|------|
| `frontend/src/services/navigator.ts` | 新增 | Router navigator 注入 module（提供 `setAppNavigate` / `appNavigate`） |
| `frontend/src/components/common/Layout/NavigatorBridge.tsx` | 新增 | 在 `<BrowserRouter>` 內把 `useNavigate` 注入到 navigator module |
| `frontend/src/services/http.ts` | 重寫 | 攔截器改用 `appNavigate`、傳遞 `from`；envelope 統一包 BusinessError 保 traceId；prod 強制 env |
| `frontend/src/constants/errorCodes.ts` | 重構 | 重新定義 3002 / 3003 / 3004 / 9003；新增 `isHardLogoutError` / `isRefreshableAuthError` / `requiresRedirectToLogin` |
| `frontend/src/i18n/zh-TW.json` | 更新 | 對齊新錯誤碼語意；新增 9003 / 3004 文案 |
| `frontend/src/i18n/en.json` | 更新 | 同上（en） |
| `frontend/src/routes/AppRouter.tsx` | 更新 | 掛載 `<NavigatorBridge />` |
| `frontend/src/services/http.test.ts` | 擴充 | 新增 6 個攔截器測試案例（3001 / 3002 / 3003 / 401 / loop guard / 一般業務錯誤） |

### 額外處理（pre-existing 問題）

| 檔案 | 問題 | 處理 |
|------|------|------|
| `frontend/src/test/testUtils.tsx` | `react-refresh/only-export-components` warning + `exactOptionalPropertyTypes` 型別衝突 | 加 `eslint-disable-next-line` + `initialEntries?: string[] \| undefined` |
| `frontend/vite.config.ts` | `test` 屬性 TS 報錯（Wave A 既有） | 加 `/// <reference types="vitest" />` + 用 `UserConfig` 型別 |
| `frontend/src/pages/Login/Login.test.tsx` | antd 5 對 2 字 CJK 按鈕加空格（「登 入」），原 selector `/登入/` 失敗 | 改為 `/登\s*入/` |

---

## 3. 設計決策

### 3.1 為何選 Navigator 注入而非事件廣播

| 選項 | 優點 | 缺點 | 結論 |
|------|------|------|------|
| **A: Navigator 注入** | 攔截器同步呼叫；攜帶 state 直觀；無新概念 | 需 NavigatorBridge 元件 | ✅ 採用 |
| B: window.dispatchEvent | 解耦徹底 | 攔截器需 dispatch，由 ProtectedRoute 監聽；多一跳；測試難 | ❌ |
| C: 全域 store + ProtectedRoute 監聽 | 用既有 zustand | 需新增 redirectIntent state，複雜度高 | ❌ |

選 A 因攔截器層需要「同步、可攜 state、可被 mock」；NavigatorBridge 是必要但極小的橋接元件（10 行）。

### 3.2 3002（TOKEN_EXPIRED）的 Wave A 行為

目前 `handleTokenExpired()` 預留 hook 點，內部 fallback 為 `redirectToLogin()`：

- Wave A：使用者體驗等同 3003，需重登（可接受）
- Wave B：接 `memberService.refreshToken({ refreshToken })`，成功則重試原請求；失敗才 redirect

決策依據：避免 Wave A 引入半成品 refresh 邏輯（無 backoff、無重試上限會炸）；先把 hook 點抽出，Wave B 純粹補實作。

### 3.3 prod 強制 env 而 dev fallback

`resolveBaseUrl()` 邏輯：

1. `VITE_API_BASE_URL` 有值 → 用之
2. 否則 `import.meta.env.PROD === true` 或 `VITE_APP_ENV === 'prod'` → `throw Error`（阻斷啟動）
3. 否則 `console.warn` 後 fallback 到 `http://localhost:8080`

避免破壞工程師本機 `npm run dev` 體驗，同時防止 prod 靜默打 localhost。

### 3.4 從 envelope 解 traceId 的實作位置

抽出 `buildBusinessErrorFromAxios()`：

- 4xx / 5xx 路徑：先嘗試解 envelope（含 traceId），fallback 才用 axios `error.message`
- 解出後若是 401 或 redirect 類錯誤碼 → `dispatchAuthRedirect`
- 一律 reject `BusinessError`，上層元件可從 `err.traceId` 寫 logger

---

## 4. 測試覆蓋

### 4.1 新增測試（6 個）

| Case | 預期行為 |
|------|----------|
| 業務碼 3003（TOKEN_INVALID）→ 清 auth + navigate(/login, {from}) | ✅ |
| 業務碼 3002（TOKEN_EXPIRED）→ navigate(/login, {from})（Wave A fallback） | ✅ |
| 業務碼 3001（UNAUTHORIZED）→ navigate(/login, {from}) | ✅ |
| HTTP 401 含 envelope → 解 traceId + navigate | ✅ |
| 當前路徑為 /login → state.from = undefined（避免迴圈） | ✅ |
| 一般業務錯誤（2011）→ 不觸發 navigate | ✅ |

### 4.2 整體結果

```
Test Files  3 passed (3)
Tests       15 passed (15)
- src/stores/authStore.test.ts        4 tests
- src/services/http.test.ts           9 tests（新增 6 個）
- src/pages/Login/Login.test.tsx      2 tests
```

---

## 5. 跨團隊同步事項

### 5.1 給 Bruno

Wave B kickoff 前必須對齊：

- 新版 errorCodes 已寫入 `frontend/src/constants/errorCodes.ts`
- Bruno 應同步建立 `docs/05_development/backend/errorCodes.md` 對照表並對齊
  3002 / 3003 / 3004 / 9003 的語意
- envelope 必須在 4xx / 5xx body 也回 traceId（@ControllerAdvice 全域處理器層保證）
- 9001 placeholder 改為 9003，Wave B 使用

### 5.2 給 Fiona（Reviewer）

B-01 修補完成，請 review 以下要點：

1. NavigatorBridge 掛載位置（AppRouter 內、Routes 外） — 確認 SSR / 測試環境兼容
2. dispatchAuthRedirect 對於「401 但無 envelope」的 fallback 行為（強制視為 UNAUTHORIZED）
3. 3002 fallback 行為是否能接受作為 Wave A 過渡

### 5.3 待 Wave B 處理

- M-01：token 改 httpOnly cookie（與 Bruno 同步）
- M-03：`handleTokenExpired()` 接 refresh token 實作
- M-04：i18n 統一 errorMessage 抽 `src/utils/errorMessage.ts`
- M-05：Profile useEffect 依賴改 `[user?.userId]`
- M-06：lightweight-charts 啟用或移除

---

## 6. 發現的新問題

| # | 嚴重度 | 描述 | 後續處理 |
|---|--------|------|----------|
| N-01 | 🟢 輕微 | `vite.config.ts` 原本有 TS 型別錯誤（`test` 屬性），表示 Wave A 提交時 `npm run type-check` 是 fail 的。建議 Linus 將 `npm run type-check` 加入 git pre-push hook | Linus 評估 |
| N-02 | 🟢 輕微 | `Login.test.tsx` 原 selector `getByRole('button', { name: /登入/ })` 是 fail（antd 5 自動加 CJK 空格），表示 `npm test` 在 Wave A 提交時就 fail | 已修；建議 Quincy / Quinn 跑前置 baseline |
| N-03 | 🟢 輕微 | `react-refresh/only-export-components` 對 testUtils 報 warning（max-warnings=0 會擋），原 Wave A 提交時 `npm run lint` 應已 fail | 已 disable 該檔；建議檢視 ESLint 設定是否該排除 `*/test/**` |

> 上述 N-01 / N-02 / N-03 暗示 Wave A skeleton 提交時可能未跑完整 lint + type-check + test 三連，建議 CI 加 quality gate。

---

## 7. 驗證指令

```bash
cd frontend
npm install
npm run lint        # 0 errors / 0 warnings
npm run type-check  # 0 errors
npm test            # 3 files / 15 tests passed
```
