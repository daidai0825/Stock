# Code Review 報告 — Wave B 個股詳情頁 + K 線整合

| 項目 | 內容 |
|------|------|
| 審查者 | Fiona（資深前端 Reviewer） |
| 審查日期 | 2026-04-22（GMT+8） |
| 召喚人 | Jamie |
| 受審範圍 | Wave B 新增/異動共 16 檔（StockDetail 主頁、5 元件、4 hooks、1 service、1 tokenStorage、1 type、3 mocks、1 router） |
| 對應開發筆記 | `docs/05_development/frontend/20260422_waveB_notes.md` |
| 對應 TD | `docs/05_development/frontend/20260422_waveB_TD.md` |
| 前置 Review | `docs/06_review/frontend/20260422_review_waveA.md`（Wave A 已 GO-WITH-FIXES） |
| 機械檢查 | type-check 0 errors / lint 0 errors / vitest 34/34 passed / 禁用詞 0 違規 |

---

## 1. 總評

**結論：NO-GO（必須先處理 Blocker 才能進入 Wave B QA / 整合階段）**
**分數：5.5 / 10**

機械檢查全綠（type-check / lint / vitest / 禁用詞）、TS strict 零妥協（0 any / 0 ts-ignore / 0 non-null assertion）、Wave A 標準延續良好；元件結構清晰、KLineChart 記憶體釋放與 ResizeObserver 處理到位、合規 disclaimer 與 source tooltip 完整。

但 Wave B 暴露兩個「測試綠燈但實際整合崩潰」的死角：
- **`useQueryErrorNotification` 不是合法的 hook**——沒有 `useEffect` 包裹，會在每次 re-render 時無條件 fire `notification.error`，UX 會被連續 toast 淹沒。
- **與 Bruno Wave 2 後端 DTO 完全不對齊**——欄位名、欄位結構、請求 contract 全部不一致；MSW 假資料因為自封自演也驗不出來。任何接上真實後端的整合測試會 100% 失敗。

加上 `tokenStorage` 是死碼（沒有任何 caller）、MSW dev fallback 在 dev 環境根本沒啟動（main.tsx 沒掛載），這兩項屬於「文件宣稱 vs 實際不一致」的可信度問題。

修補後預期可達 8.5+ 分。

---

## 2. Blocker（🔴 必修，未修不得進入 QA / 整合階段）

### B-FE-WaveB-01｜`useQueryErrorNotification` 違反 Hook Rules，造成 notification 無限觸發

**檔案**：`frontend/src/pages/StockDetail/index.tsx:29-38`

```ts
const useQueryErrorNotification = (error: Error | null, context: string): void => {
  const { t } = useTranslation();
  if (error !== null) {                         // ← 直接 in render body
    ...
    logger.warn(`[StockDetail] ${message}`, { traceId });
    notification.error({ message, description, duration: 5 });  // ← 副作用！
  }
};
```

雖然以 `use` 前綴宣告，但**完全沒有 `useEffect` 包裹**——副作用直接寫在 render body。後果：

1. React 18 StrictMode 下每次 mount 雙重觸發
2. React Query `refetchInterval: 30_000`（`useStockQuote`）背景 refetch 失敗時，error 會持續存在 → 每次父元件 re-render（如 period state 變更、其他 query 完成、視窗 focus）都會 fire 一次 `notification.error`
3. `logger.warn` 會被刷到爆量
4. `useQueryErrorNotification` 被呼叫 4 次（quote/history/fundamental/chip），任一個失敗 → 多重 toast 疊加

**修正方向**：

```ts
const useQueryErrorNotification = (error: Error | null, context: string): void => {
  const { t } = useTranslation();
  useEffect(() => {
    if (error === null) return;
    const traceId = error instanceof BusinessError ? error.traceId : undefined;
    const message = `${context}：${error.message}`;
    const description = traceId !== undefined ? `traceId：${traceId}` : t('common.errorRetry');
    logger.warn(`[StockDetail] ${message}`, { traceId });
    notification.error({ message, description, duration: 5 });
  }, [error, context, t]);
};
```

並補單元測試：error null → no notification；error 變化 → 觸發一次；同 error 連續 render → 不重複觸發。

---

### B-FE-WaveB-02｜StockService 與 Bruno Wave 2 後端 DTO 完全不對齊（跨團隊）

**檔案**：
- `frontend/src/types/stock.ts`
- `frontend/src/services/stockService.ts`
- 對照後端：
  - `backend/stock-quote/.../dto/response/QuoteDTO.java`
  - `backend/stock-quote/.../dto/request/QuoteHistoryRequest.java`
  - `backend/stock-fundamental/.../dto/response/FundamentalDTO.java`
  - `backend/stock-chip/.../dto/response/ChipDTO.java`

**問題描述**：

| 端點 | Felix 期待 | Bruno 實際 | 影響 |
|---|---|---|---|
| `POST /quote/get` 回傳 | `{stockId, stockName, price, change, changePercent, volume, updatedAt}` | `{stockId, stockName, market, openPrice, highPrice, lowPrice, closePrice, volume, quoteDate}` | **PriceHeader 全部欄位拿不到資料**（無 price / change / changePercent / updatedAt） |
| `POST /quote/history` 請求 | `{stockId, period: daily/weekly/monthly}` | `{stockId, month: LocalDate}` | **完全不同語意**（前端要週期聚合、後端要月份切片） |
| `POST /quote/history` 回傳 | `[{date, open, high, low, close, volume}]` | `List<QuoteDTO>` | 欄位名不匹配（openPrice vs open） |
| `POST /fundamental/get` 回傳 | `{eps, per, pbr, roe, updatedAt, source}` | `{eps, perRatio, pbrRatio, roe, reportYear, reportQuarter, stockName}` | per vs perRatio、無 source、updatedAt 改為 year+quarter |
| `POST /chip/get` 回傳 | `{date, institutions: [{name, netBuySell, buy, sell}], source}` | `{tradeDate, foreignNetShares, investmentTrustNetShares, dealerNetShares, totalInstitutionalNet}` | **結構完全不同**（前端要 array、後端是 4 欄位平鋪）；前端 ChipCard 要顯示 buy/sell 但後端只給 netShares |

**SRS 文件再添亂**：`docs/03_spec/20260421_SRS_stock-analysis-mvp.md:557` 寫 `/api/v1/quote/history/get`，但 Felix 與 Bruno 都實作 `/api/v1/quote/history`（少了 `/get`）——三方不一致。

**為什麼測試沒抓到**：StockDetail.test.tsx 全程用前端自定的 mock 物件，MSW handlers 也用 Felix 的 schema 寫的——自封自演，**完全沒有 contract test 與後端對齊**。

**修正方向**：先請 Jamie 仲裁標準（推薦三方契約以 SRS 與 PM/Peter 為準，由 Sophia + Preston 召集 Felix + Bruno 對齊欄位 contract），然後同步調整 types / service / handlers / 元件 binding。

詳見「6. 跨團隊對齊問題」X-WaveB-01 ~ X-WaveB-05。

---

## 3. Major（🟡 強烈建議修復）

### M-FE-WaveB-01｜`tokenStorage.ts` 是 dead code，TD-3 phase 1「抽象但暫不切換」聲稱不成立

**檔案**：`frontend/src/services/tokenStorage.ts`

`grep "tokenStorage"` 在 src 內**沒有任何 caller**（除了該檔自身與註解）。`http.ts:73` 仍直接呼叫 `useAuthStore.getState().token`，**完全沒走過 tokenStorage 抽象**。TD-3 phase 1 聲稱「未來換 cookie 只動 1 個檔」是空話，真正換 cookie 時要動的是 `http.ts`、`authStore.ts`、`memberService.ts`，而不是這個沒人用的抽象層。

**修正方向**（二擇一）：
- (A) 把 `http.ts:73` 改成 `tokenStorage.get()`，讓抽象真的被使用，並加單元測試驗證 round-trip。
- (B) 直接刪除 `tokenStorage.ts`，等 Wave C 真要換 httpOnly cookie 時再做（YAGNI）。

---

### M-FE-WaveB-02｜MSW dev fallback 在 dev 環境根本沒啟動

**檔案**：`frontend/src/main.tsx` + `frontend/src/mocks/browser.ts`

- `mocks/browser.ts:6` 註解寫「main.tsx 在 `VITE_MSW_ENABLED=true` 時動態 import」，但 main.tsx **完全沒有任何 MSW 邏輯**。
- `public/` 目錄不存在，`mockServiceWorker.js` 沒生成。
- `.env.local` 也沒設 `VITE_MSW_ENABLED`。

整套 MSW dev fallback 只在 vitest 環境有效，瀏覽器 dev 環境完全沒掛載。**TD-5 列「低」優先級不正確**，應為「中—發布前必修」。

**修正方向**：
1. main.tsx 加：
   ```tsx
   if (import.meta.env.DEV && import.meta.env.VITE_MSW_ENABLED === 'true') {
     const { worker } = await import('./mocks/browser');
     await worker.start({ onUnhandledRequest: 'bypass' });
   }
   ```
2. `package.json` 加 `"prepare": "msw init public/ --save"`。
3. `.env.local` 補預設 `VITE_MSW_ENABLED=false`。

---

### M-FE-WaveB-03｜「整合測試」名不副實

**檔案**：`frontend/src/pages/StockDetail/StockDetail.test.tsx`

檔名與第 1 行註解寫「StockDetail 整合測試」，但**整支測試完全沒有 render `<StockDetailPage />`**。第 244 行 `describe('StockDetailPage 錯誤處理')` 內部其實只 render 了 `<PriceHeader>`。4 支 hook 完全沒有單元測試但開發筆記宣稱涵蓋——文件不實。

**修正方向**：
1. 加 1 個 happy path 整合測試 + 1 個錯誤路徑。
2. 4 支 hook 各加 3 個 `renderHook` 測試（loading / success / error）。
3. 修正 line 244 describe 名稱。

---

### M-FE-WaveB-04｜StockDetail 缺空 stockId / 找不到股票的 UX

**檔案**：`frontend/src/pages/StockDetail/index.tsx:41`

`stockId` 預設空字串時，4 個 hook `enabled: false`，畫面卡在 `<PriceHeader>` empty「行情載入中…」誤導使用者。後端 4001 回來時，雖然 notification 跳，但頁面**沒有 ErrorBoundary 或 empty state**。

**修正方向**：
- stockId 為空時 `<Navigate to={RoutePath.HOME} />` 或顯示「請選擇股票」。
- 4001 時頁面頂端顯示 Result「查無此股票代號」+ 返回按鈕（i18n `stock.notFound` 已定義但沒被使用）。

---

### M-FE-WaveB-05｜`buildStockDetailPath` 沒有任何 caller，user journey 斷掉

**檔案**：`frontend/src/constants/routes.ts:18`

整個前端**沒有任何 UI 元件可以點擊進入 `/stocks/:stockId`**——使用者只能手動打網址。

**修正方向**：至少在 WatchlistPage stock code 欄位加 `<Link to={buildStockDetailPath(row.stockCode)}>`。

---

## 4. Minor（🟢 可選改善）

### m-FE-WaveB-01｜KLineChart `chartHeight` 用 useState 卻沒 setter
直接 `const CHART_HEIGHT = 400;` 拉到模組頂層即可。

### m-FE-WaveB-02｜StockDetail.test.tsx 多行 box-drawing 註解超過 120 字元
6 行 `// ─── XXX ─────...` 寬字元計算下 length 200+，違反 code-style.md 行寬 120。

### m-FE-WaveB-03｜KLineChart 主題硬寫 `#ffffff`，不支援 dark mode
`KLineChart.tsx:78-84` hard code 白底色與灰格線。Wave C/D 應從 antd token 取色。

### m-FE-WaveB-04｜useStockQuote `refetchInterval: 30_000` 不分盤中盤後
收盤後持續 30 秒打 API 浪費後端資源。

### m-FE-WaveB-05｜`PERIODS` 與 i18n key 強耦合
新增週期時要同步改兩處且不易發現。

### m-FE-WaveB-06｜`PriceHeader.tsx:17 parseFloat` 對 BigDecimal string 精度
雖然只決定方向不參與運算，但建議改 `change.startsWith('-') ? 'down' : ...` 純字串比較。

### m-FE-WaveB-07｜`scripts/check-banned-words.ts` 不存在
Wave A / Wave B 都靠手動 grep。建議補腳本進 CI（Linus 整合到 husky pre-commit）。

---

## 5. 亮點（給 Felix 鼓勵）

1. **TS strict 零妥協延續完美**：0 any / 0 ts-ignore / 0 non-null assertion / 0 console / 0 dangerouslySetInnerHTML，連 `tokenStorage.ts` 內為了讀 Zustand persist JSON 也老實寫了 type guard。
2. **KLineChart 記憶體釋放與 ResizeObserver 處理到位**。
3. **Wave A 投票標準延續**：合規 disclaimer、source tooltip、aria-label 都到位。
4. **路由 lazy + manualChunks 正確**：`lightweight-charts` 拉到獨立 chunk。
5. **錯誤碼 traceId 透傳邏輯正確**：BusinessError.traceId → notification.description 的設計意圖正確（雖然 hook bug 待修）。
6. **34/34 測試全綠 + lint 0 + type-check 0**：機械檢查紀律保持。
7. **`fmtNumber` / `netColor` / `getPriceDirection` 抽出純函式**保持元件精簡。

---

## 6. 跨團隊對齊問題（X-XX 編號 → 給 Jamie 仲裁）

### X-WaveB-01｜`/quote/get` 回傳 DTO schema 對不上

| Felix（前端 StockQuote） | Bruno（後端 QuoteDTO） |
|---|---|
| `price`, `change`, `changePercent` | `openPrice / highPrice / lowPrice / closePrice` |
| `updatedAt: string`（ISO 8601 含時區）| `quoteDate: LocalDate`（無時間） |
| 無 market | `market` |

**仲裁建議**：個股詳情頁需要「即時行情 + 漲跌幅」，後端目前只有 OHLC 沒有「相對前日漲跌」。建議 Bruno 補 `previousClose / change / changePercent`，或新增專屬 endpoint `/api/v1/quote/realtime`。

### X-WaveB-02｜`/quote/history` 請求 contract 完全不同語意

| Felix | Bruno |
|---|---|
| `{stockId, period: 'daily'\|'weekly'\|'monthly'}` | `{stockId, month: LocalDate}` |

「週期聚合」vs「月份切片」是兩個 API。Felix 的需求才是 K 線圖該有的，建議 Sophia + Preston 召集對 contract。

### X-WaveB-03｜`/fundamental/get` 欄位命名與粒度不一致

| Felix | Bruno |
|---|---|
| `per`, `pbr` | `perRatio`, `pbrRatio` |
| `updatedAt: string` | `reportYear: int`, `reportQuarter: int` |
| `source: string` | （無） |

**仲裁建議**：以 Felix 為準（顯示用 DTO），Bruno 在 controller 層做 convertor。

### X-WaveB-04｜`/chip/get` 回傳結構完全不同

| Felix | Bruno |
|---|---|
| `institutions: [{name, netBuySell, buy, sell}]` | `foreignNetShares / investmentTrustNetShares / dealerNetShares / totalInstitutionalNet` |

**仲裁建議**：以 Felix 為準（顯示用 table），Bruno 改為 array 結構並補 `buy/sell`。或退一步，Felix 改前端只顯示 net。

### X-WaveB-05｜SRS 與實作端點不一致

SRS line 557 寫 `/api/v1/quote/history/get`，Felix + Bruno 都實作 `/api/v1/quote/history`。需 Peter 更新 SRS 對齊實作或反向修實作。Jamie 拍板。

---

## 7. 技術債務（TD）建議

| TD 編號 | 項目 | 優先級 | 預計波段 |
|---|---|---|---|
| TD-3（修正） | tokenStorage 抽象層真正接入 http.ts | 高 | Wave B 收尾 |
| TD-5（升級） | MSW Service Worker 實際在 dev 啟動 | 中（不是低）| Wave B 收尾 |
| **TD-7**（新增） | 個股導航入口（Watchlist row click → /stocks/:stockId） | 中 | Wave B 收尾 / Wave C |
| **TD-8**（新增） | KLineChart 支援 antd token 主題（dark mode 預備） | 低 | Wave C/D |
| **TD-9**（新增） | useStockQuote refetchInterval 盤中盤後動態切換 | 中 | Wave C |
| **TD-10**（新增） | 與後端建立 contract test（Pact 或 OpenAPI schema validate） | 高 | Wave B 收尾後立即啟動 |
| **TD-11**（新增） | check-banned-words.ts 腳本實作並接入 CI | 中 | Wave C |

---

## 8. 投票需求

本次 Wave B Review 涉及兩個跨團隊架構議題（X-WaveB-01～04、X-WaveB-05），建議由 Jamie 啟動四人投票：
- 前端 Reviewer Fiona
- 後端 Reviewer Brian
- 系統架構師 Sophia
- 專案架構師 Preston

議程：
1. **議題 A**：Wave B 個股詳情頁 4 支 API contract 由誰為準（Felix or Bruno）？是否引入 BFF 層？
2. **議題 B**：是否強制本專案在 Wave B 收尾建立 contract test 機制（TD-10）？

---

## 9. 退回流程

1. 將本報告交給 Felix，要求修補 B-FE-WaveB-01（純前端，1 小時內可完成）。
2. 同時召喚 Brian 確認 X-WaveB-01～04 的後端對齊計畫，由 Sophia + Preston 主持四人會議拍板 contract。
3. Felix 修完 Blocker + 跨團隊 contract 對齊版 service/types 後，重新提交 Review。
4. M-FE-WaveB-01～05 可平行於 Wave B QA 階段處理，不阻塞 QA 啟動（除非 Jamie 認為 M-FE-WaveB-04 屬於 P0）。

**第二輪 Review 預期通過條件**：
- B-FE-WaveB-01 修正 + 對應 hook 單元測試
- X-WaveB-01～05 拍板 + Felix types/service/handlers 全面對齊
- M-FE-WaveB-01、02、03 至少完成 2 項
- TD list 重新整理（TD-3 / TD-5 改述）

預期分數可達 8.5–9.0 / 10。
