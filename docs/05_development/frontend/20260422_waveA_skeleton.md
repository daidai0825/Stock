# Felix 開發筆記 — Wave A 骨架

- **撰寫者**：Felix（資深前端工程師）
- **撰寫日期**：2026-04-22
- **召喚人**：Jamie（Stage 4 / Wave A）
- **範圍**：前端基礎建設 + M-MEMBER 完整 + M-WATCHLIST 骨架
- **對應決策**：[20260422_decision-platform-form](../../01_leader/decisions/20260422_decision-platform-form.md)
- **對應評估**：[20260421_initial-evaluation](../library/20260421_initial-evaluation.md)

---

## 1. 已完成項目

### 1.1 專案基礎

| 項目 | 內容 |
|------|------|
| 建構 | Vite 6.0 + SWC |
| 語言 | TypeScript 5.7（strict、noUncheckedIndexedAccess、exactOptionalPropertyTypes 全開） |
| Lint | ESLint 8 + Prettier 3 |
| 測試 | Vitest 2.1 + Testing Library 16 + jsdom |
| 路徑別名 | `@/*` → `src/*` |
| i18n | react-i18next 15，zh-TW（預設）/ en 兩語系 |
| 日期 | dayjs + utc + timezone（固定 Asia/Taipei） |
| 圖表 | lightweight-charts 4.2 已裝（Wave A 不用） |
| 環境檔 | `.env.local`、`.env.development`（prod 由 CI 注入，未 commit） |

### 1.2 架構層

| 層 | 檔案 |
|----|------|
| HTTP | `src/services/http.ts`：axios 統一 POST、Envelope 攔截器、JWT 自動帶入、401 / 業務碼 3001/3003 自動跳登入 |
| State | `src/stores/authStore.ts`：Zustand + persist (localStorage) |
| Server State | `src/hooks/useAuth.ts`、`src/hooks/useWatchlist.ts`：TanStack Query mutation/query |
| 路由 | `src/routes/AppRouter.tsx`：公開 + ProtectedRoute |
| Layout | `AppLayout`（含 Sider / Footer 合規聲明）+ `PublicLayout`（登入/註冊用） |

### 1.3 頁面（共 5 支）

| 頁面 | 檔案 | 狀態 |
|------|------|------|
| 登入 | `pages/Login` | ✅ 完整：Form validation、loading、business error 顯示 |
| 註冊 | `pages/Register` | ✅ 完整：含密碼確認比對 |
| 首頁 | `pages/Home` | ✅ 歡迎詞 + 4 個模組導覽卡 |
| 自選股 | `pages/Watchlist` | ⚠️ Wave A 骨架：使用 mock data；新增按鈕僅 console.info |
| 個人資料 | `pages/Profile` | ✅ 顯示 + 修改顯示名稱、推播偏好預留 |

### 1.4 Service 層（共 2 支）

| Service | 函式 | 狀態 |
|---------|------|------|
| `memberService` | login / register / logout / getProfile / updateProfile | ✅ 真實 axios 呼叫 |
| `watchlistService` | list / add / remove | ⚠️ Wave A：mock，待後端 Wave 2 切真實 |

### 1.5 單元測試（共 3 支，覆蓋核心鏈路）

| 測試檔 | 涵蓋 |
|--------|------|
| `pages/Login/Login.test.tsx` | 必填驗證、表單提交呼叫 service |
| `stores/authStore.test.ts` | login / logout / setUser 狀態變化 |
| `services/http.test.ts` | Envelope code === 0 解 data；code !== 0 拋 BusinessError；含 traceId |

### 1.6 合規文案（D1 強制）

- Footer 顯示完整 disclaimer（zh-TW 與 en 雙語）
- 全站文案僅使用：「個股健康度」「技術型態觀察」「籌碼動向觀察」「條件達成提醒」「5 級訊號燈」
- 禁用詞（「建議買入」「投資建議」「應該」「強烈買進」）已通盤審視，**前端 0 出現**

---

## 2. 設計取捨紀錄

### 2.1 Token 暫存 localStorage（過渡）

`react-typescript.md` 規範「禁止將 token 存於 localStorage（建議 httpOnly cookie）」。

- Wave A：採 `zustand persist` + `localStorage`，因後端 Wave 1 尚未提供 httpOnly cookie 支援
- TODO（Wave B）：與 Bruno 確認後切換到 httpOnly cookie + CSRF token；移除 authStore 的 token 持久化欄位

### 2.2 不裝 axios-mock-adapter

http.test.ts 直接覆寫 `axios.defaults.adapter`，避免新增第三方相依（依 Linus 報告精神保持 lock file 精簡）。

### 2.3 React Query 禁用 retry

依 `system-design.md`「禁止設計降級處理」，`QueryClient` 設 `retry: false`，錯誤直接由 UI 呈現。

### 2.4 不裝 React Native

依 [20260422 platform-form 拍板](../../01_leader/decisions/20260422_decision-platform-form.md)，純 Web Responsive；Sider 使用 antd `breakpoint="md"` 自動收合，符合 RWD。

---

## 3. 已知 TODO（Wave B / C）

| # | 項目 | 等待對象 | Wave |
|---|------|----------|------|
| 1 | 自選股 add / remove 切真實 API | Bruno（Wave 2 完成 watchlist API） | B |
| 2 | 自選股新增彈窗（含股票搜尋 F-WATCH-06） | - | B |
| 3 | K 線圖元件（lightweight-charts 已裝） | Bruno（M-QUOTE F-QUOTE-03~06） | B |
| 4 | 技術型態觀察 / 籌碼動向觀察訊號燈元件（5 級） | Bruno（M-TECH / M-CHIP / M-SCORE） | B |
| 5 | 個股健康度顯示元件（0-100 + 訊號燈） | Bruno（M-SCORE） | B |
| 6 | 推播偏好可編輯：Web FCM 註冊、Telegram chat_id 綁定 | Bruno（M-NOTIFY） | C |
| 7 | Token 改 httpOnly cookie + CSRF | Bruno（會員模組安全強化） | B |
| 8 | Refresh token 自動換新（401 → 嘗試 refresh → 重發） | Bruno | B |
| 9 | 條件達成提醒設定頁（CRUD） | Bruno（F-NOTIFY-01） | C |
| 10 | E2E 測試（Playwright） | - | C |
| 11 | a11y 全站審視（aria、tab 順序、對比） | - | C |
| 12 | 風控標籤紅色橫幅（M-RISK F-RISK-02） | Bruno | C |

---

## 4. 待 Peter 釐清（SRS 模糊點）

> 目前**沒有**需要 Peter 釐清的 SRS 點。Wave A 範圍（M-MEMBER / M-WATCHLIST 骨架 / Layout / 合規文案）SRS §1.2、§2.1 與 §11 描述清楚，足以實作。
>
> Wave B 啟動前可能需 Peter 補充：
> 1. 5 級訊號燈的「圖示與顏色規範」是否由 PM 統一給設計圖
> 2. 個股健康度顯示是否要有歷史趨勢小圖（SRS §1.2 F-SCORE-06 提「每日快照」但未提 UI）
> 3. 自選股「標籤分組（F-WATCH-05）」UI 是 Tab、Sider 還是下拉

---

## 5. 自我檢查（提交給 Fiona Review 前）

- [x] 全部檔案 UTF-8 LF
- [x] 嚴格 TypeScript（無 `any`）
- [x] 單元測試骨架已寫
- [x] i18n 不出現禁用詞
- [x] Footer 全頁顯示 disclaimer
- [x] 環境變數未硬編碼於程式碼
- [x] Token 注入走 axios interceptor 而非元件
- [x] 不使用 parallelStream / 無相關場景
- [ ] `npm install` 與 `npm run lint` / `npm test` 成功（待 Linus 在實機環境驗證）

---

## 6. 交接 Jamie

- **回報內容**：見對話訊息
- **下一步建議**：Jamie → Fiona 進行 Wave A code review；同步通知 Bruno 後端可開始 Wave 1 M-MEMBER 對接
