# QA 缺陷 Triage 報告：Wave 2 Round 2

| 項目 | 內容 |
|------|------|
| 撰寫者 | Bruno（資深後端工程師） |
| 撰寫日期 | 2026-04-23 |
| 對應 QA 報告 | `docs/07_qa/test-cases/20260423_quincy_wave2_test-cases.md` |
| 參考規格 | `docs/03_spec/20260422_schema-lock_stock-detail-apis.md` v1.0 |
| 對象 | Jamie、Peter（spec 釐清）、Brian（審查參考） |

---

## 缺陷總覽

| 編號 | 來源 | 標題 | 確認狀態 | 嚴重度 | 修復難度 | 預估工時 | Block Wave 2？ |
|------|------|------|----------|--------|----------|----------|---------------|
| BUG-QUINCY-001 | ChipDTO | 多送 `stockName` 欄位（超出 schema contract） | **Confirmed** | Major | S | 0.5h | 否（M：可接受） |
| BUG-QUINCY-002 | ChipConvertor | `buy` / `sell` 恆為 0，違反 schema-lock §5.3 | **Confirmed** | Major | L | Wave 3 | 否（M：已知 TD） |
| SPEC-QUINCY-001 | QuoteServiceImpl | `isStale=true` 時 `quoteDate` 語義不明確 | **Spec-Clarification-Needed** | - | - | - | 否（spec 先行） |

---

## 缺陷詳情

---

### BUG-QUINCY-001：ChipDTO 多送 `stockName` 欄位

#### 確認狀態

**Confirmed**（與 Quincy 描述方向相反，但問題確實存在）

> 注意：Quincy 報告描述為「缺少 stockName」，但閱讀 schema-lock §5.3 與前端 TypeScript 型別定義後，
> 實際問題是**多送了** stockName，而非缺少。原始 QA 報告 BUG-QUINCY-001 的標題也明確說
> "ChipDTO 多送 stockName 欄位"。確認問題方向與 Quincy 的 TC-CHIP-005 一致。

#### 根因分析

**ChipDTO.java**（第 18-24 行）含 `stockName` 欄位：

```java
public record ChipDTO(
    String stockId,
    String stockName,   // <-- schema-lock §5.3 未定義此欄位
    LocalDate date,
    List<InstitutionItem> institutions,
    long totalNetBuySell,
    String source
) {}
```

**schema-lock §5.3** 定義的 `StockChip` TypeScript 介面（第 516-522 行）：

```typescript
export interface StockChip {
  stockId: string;
  date: string;
  institutions: InstitutionItem[];
  totalNetBuySell: number;
  source: 'TWSE' | 'OTC';
}
```

`StockChip` 共 5 個欄位，無 `stockName`。ChipConvertor 也確實將 `po.getStockName()` 傳入 ChipDTO 建構式（第 44-51 行），導致 JSON response 中多出 `stockName` 欄位。

#### 影響範圍

- **受影響 API**：`POST /api/v1/chip/get`
- **受影響 client**：前端 Felix 的 `StockChip` 型別（前端目前可能已忽略此欄位，但 Pact contract test 在 Wave 3 引入後將直接 FAIL）
- **受影響測試**：TC-CHIP-005（Schema Contract）、TC-CONTRACT-003

#### 修復難度

**S（Small）**—— 純移除欄位，不涉及業務邏輯。

#### 預估工時

0.5h（含 DTO 修改、Convertor 調整、測試更新）

#### 修復方案

**Step 1**：移除 `ChipDTO.java` 的 `stockName` 欄位：

```java
// Before
public record ChipDTO(
    String stockId,
    String stockName,   // 移除此行
    LocalDate date,
    List<InstitutionItem> institutions,
    long totalNetBuySell,
    String source
) {}

// After
public record ChipDTO(
    String stockId,
    LocalDate date,
    List<InstitutionItem> institutions,
    long totalNetBuySell,
    String source
) {}
```

**Step 2**：修改 `ChipConvertor.java`，移除 `stockName` 傳入：

```java
// Before
return new ChipDTO(
    po.getStockId(),
    po.getStockName(),   // 移除此行
    po.getTradeDate(),
    institutions,
    total,
    po.getMarket()
);

// After
return new ChipDTO(
    po.getStockId(),
    po.getTradeDate(),
    institutions,
    total,
    po.getMarket()
);
```

**Step 3**：更新 `ChipServiceImplTest.java` 中 `buildChipDTO()` 輔助方法（移除 `stockName` 引數）。

#### Block Wave 2 Release？

**否（M：可接受）**。前端目前忽略多餘欄位，不影響功能。Wave 3 Pact contract test 引入前修正即可。
建議在 Wave 3 啟動前完成，避免 contract test 一上線就 FAIL。

---

### BUG-QUINCY-002：`institutions[*].buy` / `sell` 恆為 0

#### 確認狀態

**Confirmed**

#### 根因分析

**ChipConvertor.java**（第 35-40 行）明確標注技術債：

```java
// buy/sell 暫填 0（PO 僅存 netShares，Wave 3 補充明細欄位）
List<InstitutionItem> institutions = List.of(
    new InstitutionItem("外資",   0L, 0L, foreignNet),
    new InstitutionItem("投信",   0L, 0L, investmentNet),
    new InstitutionItem("自營商", 0L, 0L, dealerNet)
);
```

根本原因是 **TWSE 官方 API 的三大法人資料**（`三大法人買賣超`）**僅提供 netBuySell（買賣超淨值），不直接提供 buy/sell 明細**。
`TWSEInstitutionalDTO` record 及 `StockChipPO` 均無 `foreignBuyShares`、`foreignSellShares`
等明細欄位。這是資料來源層面的限制，非程式實作疏失。

schema-lock §5.3 `InstitutionItem` 定義 `buy: number` / `sell: number` 為必填欄位（Y），
但資料來源目前無法提供真實值，導致 schema 要求與實際資料能力之間存在落差。

#### 影響範圍

- **受影響 API**：`POST /api/v1/chip/get`
- **受影響欄位**：`data.institutions[0~2].buy`、`data.institutions[0~2].sell`（恆回 0）
- **受影響 client**：前端 ChipCard 若有顯示 buy/sell 明細，數值將永遠為 0
- **受影響測試**：TC-CHIP-002（已標記為 Known Defect）、TC-CONTRACT-003

#### 修復難度

**L（Large）**—— 需跨層修改：

1. 確認 TWSE 是否有提供 buy/sell 明細的其他 API（TWSE 三大法人詳細資料表）
2. 若有：新增 API client 呼叫、解析、`TWSEInstitutionalDTO` 新增欄位、`StockChipPO` 新增欄位（DB migration）、Convertor 更新
3. 若無：需評估是否放寬 schema contract，將 `buy`/`sell` 改為 optional，或保留 0 作為合理 fallback

#### 預估工時

**Wave 3 再排入評估**，依資料來源確認結果決定：

- 若 TWSE 有明細 API：8~12h（含 API 串接、DB migration、測試）
- 若 TWSE 無明細 API：需 Peter 重新定義 schema，可能 2~4h（spec 調整 + 欄位改 optional）

#### 修復方案（方向）

**方案 A（資料可取得）**：

1. 確認 TWSE `三大法人買賣統計` 明細端點（例如 `t86` 表）是否提供 buy/sell 分列數值
2. 擴充 `TWSEInstitutionalDTO` 加入 `foreignBuyShares`、`foreignSellShares` 等欄位
3. `StockChipPO` 新增對應欄位，撰寫 DB migration（Flyway）
4. `ChipConvertor` 移除暫填邏輯，使用真實值

**方案 B（資料不可取得）**：

1. 向 Peter 申請修訂 schema-lock，將 `buy`/`sell` 標記為 `N`（optional）或說明為「TWSE 限制，恆為 0」
2. 保留現有暫填邏輯，補充文件說明

#### Block Wave 2 Release？

**否（M：已知 TD，Wave 2 接受此暫行決策）**。
TC-CHIP-002 已標記為 "Known Defect BUG-QUINCY-002"，前端亦知悉。
Wave 3 啟動前必須完成資料來源評估並確定處理方案。

---

### SPEC-QUINCY-001：`isStale=true` 時 `quoteDate` 應為哪一天？

#### 確認狀態

**Spec-Clarification-Needed**

#### 現行實作分析

閱讀 `QuoteServiceImpl.java` 後確認現行邏輯如下：

**情境 1：DB 有資料，且屬有效交易日範圍（≤3 個工作日）**（第 136-147 行）：

```java
if (dbResult.isPresent()) {
    StockQuotePO po = dbResult.get();
    if (tradingCalendarService.isValidRecentTradingDay(po.getQuoteDate(), today)) {
        boolean stale = !today.equals(po.getQuoteDate());
        // stale = true 時，quoteDate = po.getQuoteDate()（DB 那筆的日期）
        QuoteDTO dto = stale
            ? quoteConvertor.toDTOStale(po, previousClose)
            : quoteConvertor.toDTO(po, previousClose);
        ...
    }
}
```

**情境 2：外部回空（週末/假日）→ fallback DB**（第 157-165 行）：

```java
if (dbResult.isPresent()) {
    // quoteDate = dbResult.get().getQuoteDate()（DB 最後一筆的日期）
    QuoteDTO staleDto = quoteConvertor.toDTOStale(dbResult.get(), previousClose);
    ...
}
```

**結論**：現行實作在兩種 isStale=true 情境下，`quoteDate` 均取自 **DB 那筆記錄的 `quoteDate`**（即 fallback 到的那筆資料實際發生的交易日日期），而非今日日期。

**MSW mock 的行為**（Quincy 報告 SPEC-QUINCY-001 提到）：`staleQuote` mock 使用前一個交易日（2026-04-19）作為 quoteDate，與後端現行實作一致。

#### 問題核心

schema-lock §1.3 對 `isStale=true` 的說明為：

> `isStale: true` 時，前端應顯示「資料延遲，最後更新：{quoteDate}」之類的提示文案。

但未明確定義 quoteDate 在 isStale=true 時的確切語義，導致：

1. **前端理解**：可能認為 quoteDate 應是「今日日期」（表示今天嘗試取得資料失敗）
2. **後端現行實作**：quoteDate = fallback DB 那筆資料的交易日（如 2026-04-19）
3. **MSW mock**：使用前一個交易日（與後端一致）

這三者目前恰好一致，但 schema 未明確規範，未來可能出現歧義。

#### 方案分析

| 方案 | quoteDate 語義 | 優點 | 缺點 |
|------|---------------|------|------|
| **A（現行實作）** | fallback DB 那筆資料的實際交易日（例 2026-04-19） | 語義精確，告知使用者「最後一筆有效資料是哪天的」；前端顯示文案最有意義 | 需要明確於 schema 中說明 |
| **B** | 今日日期（API 被呼叫當天） | 簡單，反映「今日嘗試取得失敗」 | 對使用者無意義（使用者不在乎「今天」，而在乎「資料是幾號的」）；前端顯示「最後更新：今日」語義矛盾 |
| **C** | 兩個欄位並陳（quoteDate = 資料實際交易日 + requestDate = 請求日） | 資訊最完整 | 需修改 schema，增加 payload 複雜度，Wave 2 影響範圍大 |

#### 建議方向

**建議採用方案 A**，並在 schema-lock 補充說明：

> `isStale: true` 時，`quoteDate` 為 fallback DB 最後一筆資料的實際交易日（非請求當日）。
> 前端應顯示「資料延遲，最後更新：{quoteDate}」，其中 `quoteDate` 反映最後一次成功取得資料的交易日。

理由：
1. 現行後端實作已使用此語義，無需改 code
2. MSW mock 已對齊（2026-04-19 = 前一個交易日）
3. 對使用者而言，「最後更新：2026-04-19」比「最後更新：2026-04-23（今日）」更有意義
4. 修改成本最低（僅需 spec 補充文字，code 不動）

#### 待 Peter 拍板事項

1. 確認 `quoteDate` 在 `isStale=true` 時的官方語義（建議選方案 A）
2. 若選方案 A，請在 schema-lock §1.3 補充以下說明：
   > `isStale=true` 時，`quoteDate` 為 fallback DB 最後一筆資料的實際交易日，非請求當日。
3. 確認前端 TC-QUOTE-007 的驗收基準（建議以「fallback DB 那筆的 quoteDate」為預期值，如 Quincy 目前標記的一樣）

#### Block Wave 2 Release？

**否**。現行實作與 MSW mock 一致，前端行為正確。僅需 Peter 補充 spec 文字，避免 Wave 3 引入 contract test 時產生歧義。

---

## 整體 Wave 2 Release 建議

| 評估面向 | 結論 |
|----------|------|
| BUG-QUINCY-001 | **不 block**，建議 Wave 3 前修正，修復簡單（0.5h）|
| BUG-QUINCY-002 | **不 block**，已知 TD，Wave 3 排入評估 |
| SPEC-QUINCY-001 | **不 block**，現行實作與 mock 一致，請 Peter 補充 spec 文字即可 |
| **Wave 2 整體** | **可放行**，3 個問題均不構成 release blocker |

### 建議後續行動

1. **即刻（Wave 2 release 前）**：無需修改 code，僅請 Peter 在 schema-lock §1.3 補充 `quoteDate` 在 isStale=true 時的語義說明
2. **Wave 3 Sprint 開始前**：
   - BUG-QUINCY-001 修復（0.5h，移除 ChipDTO.stockName）
   - BUG-QUINCY-002 資料來源評估（確認 TWSE 是否提供 buy/sell 明細），產出評估報告給 Linus + Sophia

---

## 附錄：原始碼對照

| 問題 | 關鍵檔案 | 關鍵行號 |
|------|----------|----------|
| BUG-QUINCY-001 | `backend/stock-chip/src/main/java/tw/com/stockplatform/chip/dto/response/ChipDTO.java` | L19（`stockName` 欄位） |
| BUG-QUINCY-001 | `backend/stock-chip/src/main/java/tw/com/stockplatform/chip/convertor/ChipConvertor.java` | L44-51（`toDTO` 建構式） |
| BUG-QUINCY-002 | `backend/stock-chip/src/main/java/tw/com/stockplatform/chip/convertor/ChipConvertor.java` | L35-40（`buy/sell 暫填 0`） |
| SPEC-QUINCY-001 | `backend/stock-quote/src/main/java/tw/com/stockplatform/quote/service/impl/QuoteServiceImpl.java` | L136-165（isStale 判斷邏輯） |
