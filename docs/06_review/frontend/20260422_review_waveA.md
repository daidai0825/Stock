# Frontend Code Review Report — Wave A 骨架

| 項目 | 內容 |
|------|------|
| 審查者 | Fiona（資深前端 Reviewer） |
| 審查日期 | 2026-04-22 |
| 召喚人 | Jamie |
| 受審範圍 | `/frontend/`（41 個檔案，Wave A 骨架） |
| 對應開發筆記 | [20260422_waveA_skeleton.md](../../05_development/frontend/20260422_waveA_skeleton.md) |
| 對應 SRS | [20260421_SRS_stock-analysis-mvp.md](../../03_spec/20260421_SRS_stock-analysis-mvp.md) |
| 對應拍板 | [20260422_decision-platform-form.md](../../01_leader/decisions/20260422_decision-platform-form.md) |
| 總問題數 | 🔴 1 / 🟡 6 / 🟢 7 |
| 結論 | **GO-WITH-FIXES**（修完 1 個 Blocker 即可進入 Wave B；其他 Major 可平行 Wave B 處理） |

---

## 0. 受審檔案清單（41 檔）

| 類別 | 檔案數 | 主要檔案 |
|------|--------|----------|
| 配置 | 7 | `tsconfig.json`、`package.json`、`vite.config.ts`、`.eslintrc.cjs`、`index.html`、`.env.local`、`.env.development` |
| 入口 | 3 | `main.tsx`、`App.tsx`、`vite-env.d.ts` |
| 路由 / Layout | 4 | `routes/AppRouter.tsx`、`components/common/Layout/{AppLayout,PublicLayout,ProtectedRoute}.tsx` |
| 頁面 | 6 | `pages/{Login,Login.test,Register,Home,Watchlist,Profile}/index.tsx`（含 1 個 .test） |
| Service | 3 | `services/{http,memberService,watchlistService}.ts` |
| Hook | 2 | `hooks/{useAuth,useWatchlist}.ts` |
| Store | 2 | `stores/{authStore,authStore.test}.ts` |
| Types | 3 | `types/{api,member,watchlist}.ts` |
| 常數 / i18n / Util | 7 | `constants/{errorCodes,routes}.ts`、`i18n/{index,zh-TW,en}`、`utils/datetime.ts` |
| Test 基礎 | 2 | `test/{setup.ts,testUtils.tsx}` |
| 其他 | 2 | `styles/global.css`、`README.md` |

---

## 1. 嚴重問題（Blocker）— 必須在 Wave B 啟動前修正

| # | 項目 | 檔案位置 | 問題描述 | 嚴重程度 | 單元測試 | 調整完成 | 負責人 | 備註 |
|---|------|----------|----------|----------|----------|----------|--------|------|
| B-01 | `redirectToLogin` 在攔截器內副作用過強 + 401 失去 envelope | `src/services/http.ts:42-48, 71-74` | 觸發 401 / 3001 / 3003 時呼叫 `window.location.assign`（hard reload）。這會：（1）清空 React Router 透過 state 攜帶的 `from` 路徑（見 `ProtectedRoute.tsx:15`），導致 SRS 預期「登入後回到原頁」流程失效；（2）401 路徑直接拋 `BusinessError(UNAUTHORIZED, '尚未登入或登入已過期')`，**完全丟棄 backend 在 4xx body 內可能附帶的 traceId 與真實 message**，使日誌追蹤斷鏈，違反 `api-design.md` §「可追蹤性」與 SRS §1.2 F-MEMBER 的 traceId 規範 | 🔴 嚴重 | ☐ | ☐ | Felix | 修正方向：(a) 改用 React Router 的 `navigate()`（攔截器層注入 router 實例）或 broadcast event 由 ProtectedRoute 監聽，避免 hard reload；(b) 401 也要先嘗試從 `error.response?.data` 解 envelope，保留 traceId 再 fallback 到 UNAUTHORIZED |

### B-01 修正建議 snippet

```ts
// services/http.ts（修正方向，僅示意）
const buildBusinessErrorFromAxios = (error: AxiosError<ApiResponse<unknown>>): BusinessError => {
  const body = error.response?.data;
  if (body && typeof body === 'object' && 'code' in body) {
    return new BusinessError(body.code as number, body.message ?? error.message, body.traceId, body.errors);
  }
  return new BusinessError(ErrorCode.UNKNOWN, error.message || 'network error');
};

// onRejected
const businessError = buildBusinessErrorFromAxios(error);
if (error.response?.status === 401 || isAuthError(businessError.code)) {
  redirectToLogin();
}
return Promise.reject(businessError);
```

並提供 `setRouterNavigate(navigate: NavigateFunction)` 由 `App.tsx` 在 mount 時注入，攔截器使用該 navigator 而非 `window.location.assign`。

---

## 2. 主要問題（Major）— Wave B 內必須補齊

| # | 項目 | 檔案位置 | 問題描述 | 嚴重程度 | 單元測試 | 調整完成 | 負責人 | 備註 |
|---|------|----------|----------|----------|----------|----------|--------|------|
| M-01 | Token 持久化於 `localStorage`（XSS 高風險） | `src/stores/authStore.ts:48-57` | 已於程式碼註解與筆記中標示為過渡方案，TODO 對齊 Wave B。但目前 `partialize` 仍持久化 `token` / `refreshToken` 至 `localStorage`，**任何 XSS 漏洞即可洩漏 JWT**。違反 `react-typescript.md`「禁止」條款與 `security-owasp.md` A02 / A07 | 🟡 中等 | ☐ | ☐ | Felix + Bruno | **需 Wave B 完成切換 httpOnly cookie + CSRF**。建議過渡期 partialize 僅保留 `user` / `isAuthenticated`，token 改放 in-memory（重新整理後重打 `/profile/get` 確認 session）。Wave B 內必須關閉 |
| M-02 | `baseURL` fallback 到 `localhost:8080` 在 prod 高風險 | `src/services/http.ts:22` | `import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'` — 若 CI 漏注入 `VITE_API_BASE_URL`，prod build 會打到 localhost。違反 `environment.md`「無預設值依賴」與「無嵌套變數」 | 🟡 中等 | ☐ | ☐ | Felix | 修正：開發期允許 fallback（依 `VITE_APP_ENV !== 'local'` 強制要求 env 存在），否則 build 階段直接 `throw new Error('VITE_API_BASE_URL is required')` |
| M-03 | 401 處理沒覆蓋 refresh token 流程 | `src/services/http.ts:71-74`、`hooks/useAuth.ts` | SRS F-MEMBER-06 已明列 Refresh Token 換新流程，攔截器目前 401 直接登出。雖開發筆記 TD #8 已列入 Wave B，但 401 直接 hard logout 會造成 Wave A → Wave B 銜接時 UX 倒退；需先在攔截器留 hook 點（`refreshTokenInterceptor` 注入位） | 🟡 中等 | ☐ | ☐ | Felix | 結構先預留：抽出 `handleUnauthorized()` 函式，內部 TODO 標記 Wave B 接 refresh logic |
| M-04 | `errorMessage` 直接顯示 `[code] message`，未本地化 | `src/pages/Login/index.tsx:32-37`、`Register/index.tsx:40-45`、`Profile/index.tsx:37-42` | 已在 i18n 提供 `errors.{code}` 對照，但三頁都採 raw `[code] message`，未走 i18n。對使用者顯示「[2010] Email already registered」混雜中英／碼，違反 SRS §11 合規文案統一性 | 🟡 中等 | ☐ | ☐ | Felix | 統一改為 `t(\`errors.${code}\`, { defaultValue: message })` 並抽到 `src/utils/errorMessage.ts` 共用 |
| M-05 | `Profile.useEffect` 對 `user` 變動時 reset form，會覆寫使用者編輯 | `src/pages/Profile/index.tsx:27-31` | useEffect 依賴 `[user, form]`，當 mutation `onSuccess` 觸發 `setUser(data)` 後 user 物件 reference 變動 → 立刻 `form.setFieldsValue`，會覆寫使用者下一筆編輯。同時 `initialValues` 已設過一次，effect 屬於重複；違反 `react-typescript.md` Hook 依賴準則 | 🟡 中等 | ☐ | ☐ | Felix | 改用 `useEffect(() => { ... }, [user?.userId])` 僅在帳號切換時 reset；或乾脆移除 effect，依 `initialValues` + 手動在 onCancel 時 `form.resetFields()` |
| M-06 | `lightweight-charts` 已裝 0 處引用，浪費 bundle | `package.json:27`、`vite.config.ts:28` | Felix 自陳已加入 manualChunks，但 Wave A 完全未用。Wave B 才會使用。違反 `environment.md` 配置精簡原則；亦讓 Linus 的 lock file 多了 ~200KB 相依 | 🟡 中等 | ☐ | ☐ | Felix + Linus | 兩個方向擇一：(a) Wave B 才裝，移出 dependencies；(b) 保留但於 README 註明「Wave A 預裝，Wave B 啟用」並確認 Vite tree-shaking 不會打進 prod bundle（manualChunks 設定下 chart chunk 僅 lazy import 才不會載入，目前無 import 屬於 dead chunk） |

---

## 3. 輕微問題（Minor）— 建議調整

| # | 項目 | 檔案位置 | 問題描述 | 嚴重程度 | 單元測試 | 調整完成 | 負責人 | 備註 |
|---|------|----------|----------|----------|----------|----------|--------|------|
| L-01 | `Watchlist.handleAdd` 用 `console.info` 為 placeholder | `src/pages/Watchlist/index.tsx:15-19` | 雖 ESLint 規則允許 `console.info`，但生產 bundle 會留下 dead log。應改為 antd `message.info(t('watchlist.comingSoon'))` 給使用者明確回饋 | 🟢 輕微 | ☐ | ☐ | Felix | - |
| L-02 | `AppRouter` `/` 預設導 `/home`，未登入流程多一次跳轉 | `src/routes/AppRouter.tsx:37-38` | 未登入訪問 `/` → 先 Navigate 到 `/home` → ProtectedRoute 再導 `/login`，發生兩次 navigate。改為依 `isAuthenticated` 條件分流可省一次 | 🟢 輕微 | ☐ | ☐ | Felix | 非阻擋；現行可運作 |
| L-03 | `disclaimer-text` 全域 CSS 命名易與其他模組衝突 | `src/styles/global.css:30-36` | 未走 CSS Module 或 antd Token，全域 class 命名應加 prefix 如 `.app-disclaimer-text` | 🟢 輕微 | ☐ | ☐ | Felix | 全域 css 已最小化，未來若擴增建議改 CSS Module |
| L-04 | `useAuth.useLogout.onSettled` 在 mutation 失敗也清空 state | `src/hooks/useAuth.ts:30-37` | 雖註解說明意圖，但若 server 回 409（同 token 已登出）即清空 local 是 OK；若回 5xx 系統錯誤導致 token 仍有效卻清掉 → 使用者下一次操作必失敗。可在註解明列「即便如此仍清空」的 trade-off | 🟢 輕微 | ☐ | ☐ | Felix | 註解補強即可 |
| L-05 | `i18n.subtitle` 與其他多個 key 未在 UI 引用 | `src/i18n/{zh-TW,en}.json` | `app.subtitle`、`auth.loginSuccess`、`auth.logoutSuccess`、`watchlist.removeConfirm`、`watchlist.limitNotice`、`watchlist.columns.actions`、`errors.network` 均未在 Wave A 引用 | 🟢 輕微 | ☐ | ☐ | Felix | Wave B 會用到，可保留，但建議在 README 或 i18n header 注明「Wave B 預留」 |
| L-06 | `Login.test.tsx` 缺業務錯誤碼路徑覆蓋 | `src/pages/Login/Login.test.tsx` | 目前 2 個 case 僅覆蓋必填驗證 + 成功呼叫；未覆蓋 `BusinessError`（如 2011 密碼錯誤）顯示路徑。以 Wave A 骨架可接受，但屬「核心鏈路」 | 🟢 輕微 | ☐ | ☐ | Felix / Quincy | Wave B 補測試案例（mock memberService.login 拒絕，斷言 Alert 顯示 errorMessage） |
| L-07 | a11y：登入失敗 Alert 未綁 `role="alert"` 自動讀屏 | `src/pages/Login/index.tsx:46`、`Register/index.tsx:56`、`Profile/index.tsx:52` | antd `<Alert type="error">` 預設 role 為 `alert`，但若使用者已登入頁且 Alert 改變內容可能不會被讀屏。建議顯式 `role="alert" aria-live="assertive"` | 🟢 輕微 | ☐ | ☐ | Felix | Wave C a11y 全站審視時統一處理 |

---

## 4. 整體評價

### 4.1 架構符合度

| 評估面 | 等級 | 說明 |
|--------|------|------|
| 分層清晰度 | ✓ 良好 | `services/hooks/stores/types/constants` 分層合規，符合 `react-typescript.md` 目錄結構建議 |
| TS strict 配置 | ✓ 優秀 | strict、noUncheckedIndexedAccess、exactOptionalPropertyTypes、noImplicitAny 全開；全專案 0 個 `any` / `@ts-ignore` / 非空斷言。ESLint 額外加 `no-explicit-any: error`、`max-warnings 0`，水準在合規之上 |
| Envelope 攔截器設計 | ⚠ 需修 | 整體骨架對齊 `api-design.md`（success → 解 data，code !== 0 → BusinessError），但 401 路徑遺失 traceId（B-01）；redirect 機制違反 SPA UX |
| State 管理 | ✓ 合理 | Zustand + persist + partialize 結構正確，TanStack Query mutation/query 分離；測試已涵蓋 store 4 個關鍵狀態變化 |
| 路由 / RWD | ✓ 通過 | ProtectedRoute + Public/AppLayout 分離，Sider `breakpoint="md"` 自動收合，符合拍板 D「純 Web Responsive」 |
| 合規文案 | ✓ 完美 | 全站 zh-TW + en disclaimer 雙覆蓋，禁用詞 0 出現，「個股健康度」「技術型態觀察」「籌碼動向觀察」「條件達成提醒」「5 級訊號燈」用詞精準對齊 SRS §0 |

### 4.2 測試覆蓋率（粗估）

| 範疇 | 覆蓋程度 | 備註 |
|------|----------|------|
| `services/http.ts` | 中（envelope happy path + business error） | 401 / hard redirect / network error 路徑未測 |
| `stores/authStore.ts` | 高（4 個關鍵狀態變化） | persist 未測 |
| `pages/Login` | 低（必填 + happy path） | 業務錯誤路徑未測（L-06） |
| 其他頁面 | 0 | Wave A 骨架可接受，Wave B 必須補 |
| **粗估行覆蓋率** | **~30%（Wave A 範圍）** | Wave A 骨架可接受，Wave B 須拉到 ≥60%，Wave C 上線前 ≥80% 對齊 `jenkins-cicd.md` Quality Gate |

### 4.3 合規符合度

| 規則檔 | 符合度 | 違反處 |
|--------|--------|--------|
| `api-design.md`（Envelope） | 95% | B-01 traceId 遺失 |
| `react-typescript.md` | 90% | M-01 token localStorage（已標 TD）、M-05 Hook deps |
| `code-style.md` | 100% | UTF-8 + LF、120 字元、無 console.log、命名一致 |
| `conversation.md`（禁用詞） | 100% | 0 中國用語、0 禁用投資術語 |
| `environment.md` | 80% | M-02 baseURL fallback 違反「無預設值依賴」 |
| `security-owasp.md` A02 / A07 | 70% | M-01 token 存 localStorage（已知 TD） |

---

## 5. 結論

### **GO-WITH-FIXES**

| 條件 | 內容 |
|------|------|
| **進入 Wave B 的必要條件** | 修復 **B-01**（redirect 機制 + 401 traceId 保留） |
| **Wave B 並行處理** | M-01（token 改 httpOnly cookie，與 Bruno 同步）、M-02（env 強制檢查）、M-03（refresh token hook 點）、M-04（i18n 統一）、M-05（Profile useEffect）、M-06（lightweight-charts 啟用） |
| **Wave C 收斂** | L-06（測試覆蓋率拉到 60%+）、L-07（a11y 全站審視） |
| **Wave A 骨架的 K 線、健康度、5 級訊號燈、推播設定均尚未實作，皆為預期，不列為 Blocker** | - |

---

## 6. 給 Bruno 的跨團隊提醒（透過 Jamie 轉達）

| # | 議題 | 細節 |
|---|------|------|
| FE→BE-01 | 業務錯誤碼必須與 `src/constants/errorCodes.ts` 對齊 | 前端已定義 17 個錯誤碼（USER_NOT_FOUND=2001、EMAIL_ALREADY_REGISTERED=2010、PASSWORD_INCORRECT=2011、EMAIL_NOT_VERIFIED=2012、WATCHLIST_LIMIT_EXCEEDED=2050、UNAUTHORIZED=3001、TOKEN_EXPIRED=3003、TWSE_UNAVAILABLE=5101、SYSTEM_BUSY=9001 等）。Bruno 開始 Wave 1 M-MEMBER 實作前請對齊；新增碼必須**雙邊同步**並更新 `errorCodes.ts` 與 i18n `errors.{code}` |
| FE→BE-02 | Envelope traceId 必須 100% 回傳（含 4xx / 5xx body） | B-01 修正後攔截器會從 `error.response?.data.traceId` 取值，請 Bruno 在 `@ControllerAdvice` 全域例外處理器內**保證 4xx/5xx 也回 envelope**（含 traceId），勿使用 Spring 預設 error response |
| FE→BE-03 | httpOnly cookie + CSRF 切換時程 | 前端 M-01（token localStorage）為過渡方案，Wave B 必須切換。請 Bruno 在 M-MEMBER 實作時即支援：(a) `Set-Cookie: HttpOnly; Secure; SameSite=Strict`；(b) `/api/v1/member/csrf-token` endpoint 提供 CSRF token；(c) refresh token 流程設計（前端 401 自動 refresh） |
| FE→BE-04 | F-MEMBER-04 LoginResponse 結構需保留 | 前端 `LoginResponse` 需 `accessToken`、`refreshToken`、`expiresIn`、`user`（內含 `MemberProfile` 8 個欄位含 `notifyEmailEnabled` / `notifyWebEnabled`）。若改 httpOnly cookie 模式，token 可從 body 移除但需提供 `expiresIn` 給前端控管 session |
| FE→BE-05 | M-WATCHLIST 介面對齊 | 前端 mock 採 `WatchlistItem`（含 `healthScore` / `dataDelayMinutes`），Bruno 實作 `/api/v1/watchlist/{list,add,remove}` 時請對齊欄位命名與型別（`number \| null` 而非 `Optional<Number>` — DTO 規範禁用 Optional） |

---

## 7. 後續 Action（給 Jamie）

1. 將本報告 link 給 Felix，要求 B-01 在 1 個工作日內修正並補測試
2. 將「給 Bruno 的跨團隊提醒」第 1-5 點加入 Wave B kickoff agenda
3. M-01（token 切換）需要 Sophia / Brian 投票確認 httpOnly cookie + CSRF 設計
4. Wave B kickoff 前，邀請 Felix + Bruno 共同 review `errorCodes.ts` 對齊
