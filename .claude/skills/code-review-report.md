---
name: code-review-report
description: Code Review 報告模板。由 Fiona（前端 Reviewer）與 Brian（後端 Reviewer）使用，產出標準化的 Code Review 報告。
---

# Skill: Code Review Report

## 使用時機

- **使用者**：Fiona（前端）、Brian（後端）
- **輸出路徑**：
  - `docs/06_review/frontend/YYYYMMDD_PR{number}_review.md`
  - `docs/06_review/backend/YYYYMMDD_PR{number}_review.md`
- **觸發時機**：開發者完成 PR 後

## 模板

```markdown
# Code Review 報告 - PR #{number}

## 基本資訊

| 項目 | 內容 |
|------|------|
| 審查者 | Fiona / Brian |
| 審查日期 | YYYY-MM-DD HH:MM (GMT+8) |
| PR 編號 | #123 |
| PR 標題 | [feat][user] 新增使用者註冊功能 |
| PR 連結 | [URL] |
| 開發者 | Felix / Bruno |
| 變更分支 | feature/USER-123-add-registration → develop |
| 變更檔案數 | 12 |
| 變更行數 | +420 / -50 |
| Review 輪次 | #1 |

## 摘要

| 嚴重度 | 數量 |
|--------|------|
| 🔴 Blocker | 2 |
| 🟡 Major | 5 |
| 🟢 Minor | 8 |
| ✅ 良好實踐 | 3 |

**最終判定**：🔴 退回修正（2 個 Blocker）

---

## 🔴 Blocker（必須修復）

### #1 SQL Injection 風險

| 項目 | 內容 |
|------|------|
| 檔案位置 | `repository/UserMapper.java:45` |
| 嚴重度 | 🔴 嚴重 |
| 類別 | 安全性（OWASP A03:2021） |

**問題描述**：

```java
@Select("SELECT * FROM USER_INFO WHERE email = '${email}'")
Optional<UserPO> findByEmail(String email);
```

使用 `${email}` 直接拼接 SQL，存在 SQL Injection 風險。攻擊者可注入惡意 SQL。

**重現方式**：
傳入 `email = "x' OR '1'='1"` 將導致全表查詢。

**建議修正**：

```java
@Select("SELECT * FROM USER_INFO WHERE email = #{email}")
Optional<UserPO> findByEmail(String email);
```

使用 `#{email}` 採用 PreparedStatement 防護。

**參考**：
- [OWASP SQL Injection](https://owasp.org/www-community/attacks/SQL_Injection)
- [MyBatis 官方文件 - 字串替換](https://mybatis.org/mybatis-3/sqlmap-xml.html#string-substitution)

---

### #2 金額計算使用 double

| 項目 | 內容 |
|------|------|
| 檔案位置 | `service/impl/OrderServiceImpl.java:78` |
| 嚴重度 | 🔴 嚴重 |
| 類別 | 邏輯錯誤 |

**問題描述**：

```java
double total = price * quantity;
```

`double` 無法精確表示十進位數，會造成金額計算誤差（例如 0.1 + 0.2 != 0.3）。

**建議修正**：

```java
BigDecimal total = price.multiply(BigDecimal.valueOf(quantity));
```

**參考**：
- [Effective Java Item 60: Avoid float and double if exact answers are required]
- 專案規範 [java-spring](../rules/java-spring.md#數值與時間)

---

## 🟡 Major（強烈建議修復）

### #3 N+1 Query 問題

| 項目 | 內容 |
|------|------|
| 檔案位置 | `service/impl/UserServiceImpl.java:120-130` |
| 嚴重度 | 🟡 中等 |
| 類別 | 效能 |

**問題描述**：

```java
List<UserDTO> users = userMapper.findAll();
users.forEach(u -> u.setOrders(orderMapper.findByUserId(u.getUserId())));
```

對每個 user 執行一次 query，N 個 user 會產生 N+1 次資料庫查詢。

**建議修正**：

新增一次性 join 查詢：

```java
List<UserWithOrdersPO> data = userMapper.findAllWithOrders();
```

或使用 MyBatis `<collection>` 完成關聯載入。

---

### #4 [其他 Major 問題...]

---

## 🟢 Minor（可選修復）

### #5 變數命名建議

| 項目 | 內容 |
|------|------|
| 檔案位置 | `service/UserService.java:30` |
| 嚴重度 | 🟢 輕微 |
| 類別 | 可讀性 |

**問題描述**：

```java
public UserDTO get(String id) { ... }
```

`get` 與 `id` 過於通用，建議更明確。

**建議修正**：

```java
public UserDTO findById(String userId) { ... }
```

---

### #6 [其他 Minor 問題...]

---

## ✅ 良好實踐（鼓勵）

### Java 21 Records 使用

`CreateUserRequest.java` 使用 records 取代 POJO，程式碼簡潔且不可變。良好示範。

### 完整的單元測試

`UserServiceImplTest.java` 涵蓋了正常、邊界、例外情境，覆蓋率 88%。值得讚賞。

### 結構化日誌

使用 `StructuredArguments.kv()` 輸出結構化日誌，方便後續查詢。

---

## 整體評價

| 維度 | 評分 | 說明 |
|------|------|------|
| 架構符合度 | ✅ 8/10 | 大致符合 Preston 設計，個別細節需調整 |
| 程式碼品質 | ⚠️ 6/10 | 有 Blocker 與 Major 問題待修 |
| 安全性 | 🔴 4/10 | 1 個 SQL Injection Blocker |
| 效能 | ⚠️ 6/10 | N+1 query 待處理 |
| 測試覆蓋率 | ✅ 8/10 | 88%，超過標準 |
| 文件完整度 | ✅ 8/10 | 開發筆記完整 |
| **總評** | ⚠️ 6.7/10 | **需修正 Blocker 才能進入測試** |

---

## 追蹤清單

| 項目 | 檔案位置 | 問題描述 | 嚴重度 | 單元測試 | 調整完成 | 負責人 | 備註 |
|------|----------|----------|--------|----------|----------|--------|------|
| SQL Injection | `UserMapper.java:45` | `${email}` 改 `#{email}` | 🔴 嚴重 | ☐ | ☐ | Bruno | - |
| 金額用 double | `OrderServiceImpl.java:78` | 改用 BigDecimal | 🔴 嚴重 | ☐ | ☐ | Bruno | - |
| N+1 Query | `UserServiceImpl.java:120` | 改 join 查詢 | 🟡 中等 | ☐ | ☐ | Bruno | - |
| 變數命名 | `UserService.java:30` | `get` → `findById` | 🟢 輕微 | ☐ | ☐ | Bruno | - |

---

## 投票（如需）

本次 Review 無架構議題需要投票。

> 若有架構議題，記錄如下：
>
> | 議題 | Sophia | Preston | Fiona | Brian | 結果 |
> |------|--------|---------|-------|-------|------|
> | 是否使用 Redis Cluster | A | B | A | A | A 通過（3:1） |

---

## 結論

**狀態**：🔴 退回修正（2 個 Blocker）

**下一步**：

1. Bruno 修正 #1（SQL Injection）與 #2（double）
2. Bruno 補上對應的單元測試
3. 重新提交 PR
4. 透過 Jamie 通知 Brian 進行第二輪 Review

**預計第二輪 Review 時間**：YYYY-MM-DD

**備註**：
- N+1 query（#3）建議在這次一併修正，避免後續 refactor
- 其他 Minor 可列入下次重構 backlog

---

**Review 簽核**：Brian / Fiona
**簽核日期**：YYYY-MM-DD HH:MM
```

## 嚴重度判定標準

| 嚴重度 | 定義 | 範例 | 處置 |
|--------|------|------|------|
| 🔴 Blocker | 必須修復才能 merge | 安全漏洞、邏輯錯誤、缺少測試、N+1 | 退回開發者 |
| 🟡 Major | 強烈建議修復 | 效能、可維護性、命名嚴重不當 | 建議修復，可進測試 |
| 🟢 Minor | 可選修復 | 註解、輕微命名、小改善 | 列入 backlog |

## 撰寫要點

1. **每個問題都附**：檔案位置、嚴重度、類別、描述、修正建議、參考來源
2. **量化評分**：給每個維度打分數
3. **追蹤清單**：表格便於後續追蹤
4. **投票紀錄**：架構議題需要紀錄
5. **建設性語氣**：針對程式碼非個人

## 禁止事項

- 禁止只說「不好」不說「怎麼改」
- 禁止對個人攻擊
- 禁止省略嚴重度
- 禁止省略結論與下一步
- 禁止放行有 Blocker 的 PR
