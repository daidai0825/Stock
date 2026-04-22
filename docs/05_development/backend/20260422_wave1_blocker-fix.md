# Wave 1 Blocker / Major 修補筆記

> **日期**：2026-04-22
> **負責人**：Bruno（Backend）
> **依據**：Brian（Backend Reviewer）Wave 1 Code Review 報告
> **範圍**：BE-01 / BE-02 / BE-03（Blocker）+ BE-M02（Major）
> **驗證**：`mvn test`（IntelliJ 內建 Maven 3.9.11）→ BUILD SUCCESS

---

## 修補總覽

| 編號 | 嚴重程度 | 主題 | 狀態 |
|------|----------|------|------|
| BE-01 | 🔴 Blocker | `MemberMapper.insert` 用 `@Select` 註解 INSERT，回傳 Optional 永遠空 | ☑ 已修 |
| BE-02 | 🔴 Blocker | `MemberController.currentUserId` catch-all 吞掉 `ExpiredJwtException` | ☑ 已修 |
| BE-03 | 🔴 Blocker | `AuditAspect` 讀 `MDC.get("userId")` 但無人寫入 | ☑ 已修 |
| BE-M02 | 🟡 Major | 9 個 placeholder controller 一律回 `9001 SYSTEM_BUSY`，與真實系統錯誤混淆 | ☑ 已修 |

---

## BE-01：MemberMapper 註冊路徑修復

### 問題

`MemberMapper.insert` 原本用 `@Select` 搭配 `RETURNING user_id` 子句，回傳 `Optional<String>`：

```java
@Select("""
    INSERT INTO users ... RETURNING user_id
    """)
Optional<String> insert(UserPO po);
```

MyBatis 在 `@Select` 註解下會把 INSERT 當查詢執行，多數驅動不會把 `RETURNING` 子句的值回填，註冊流程實質上「永遠拿不到 userId」，即使 DB 有寫入。

### 修補

`backend/stock-member/src/main/java/tw/com/stockplatform/member/repository/MemberMapper.java`：

- `@Select` → `@Insert`
- 移除 `RETURNING user_id` 子句
- 回傳型別改為 `int`（受影響筆數）

```java
@Insert("""
    INSERT INTO users
        (user_id, email, password_hash, display_name, status,
         email_verified_at, created_at, updated_at, deleted_at)
    VALUES
        (#{userId}, #{email}, #{passwordHash}, #{displayName}, #{status},
         #{emailVerifiedAt}, #{createdAt}, #{updatedAt}, #{deletedAt})
    """)
int insert(UserPO po);
```

`backend/stock-member/src/main/java/tw/com/stockplatform/member/service/impl/MemberServiceImpl.java`：

- 改為依 `int rows` 判斷成功；`rows != 1` 直接拋 `BusinessException(SYSTEM_BUSY)`，避免靜默失敗。
- userId 由 service 端產生（`UUID.randomUUID().toString()`）後直接帶入 PO，後續流程沿用同一個 userId。

### 測試

`MemberServiceImplTest`：

- `register_Success` — 模擬 `insert(...)` 回傳 1，驗證 `MDC.userId` 被注入、`USER_PREFERENCES.insert()` 被呼叫。
- `register_InsertNoRows` — 模擬 `insert(...)` 回傳 0，驗證拋 `SYSTEM_BUSY` 且不再寫 `USER_PREFERENCES`。

### 旁證

`UserPreferenceMapper.insert` 本來就用 `@Insert` 回傳 `int`，與本次修補後的 `MemberMapper` 行為一致——這是純抄錯，不是設計議題。

---

## BE-02：JWT 例外分流（3001 / 3002 / 3003）

### 問題

`MemberController.currentUserId` 原本：

```java
try {
    return jwtTokenProvider.parse(token).getSubject();
} catch (Exception ex) {
    throw new BusinessException(ErrorCode.UNAUTHORIZED);
}
```

catch-all `Exception` 把 `ExpiredJwtException` 也吞掉，前端永遠收到 `3001 UNAUTHORIZED`，無法區分「沒帶 token」、「token 過期該 refresh」、「簽章不符」三種情境，也無法觸發 refresh token 流程（即使 Wave 1 還沒上 refresh，這個分流仍是必要的契約）。

### 修補

`backend/stock-member/src/main/java/tw/com/stockplatform/member/controller/MemberController.java`：

```java
private String currentUserId(HttpServletRequest request) {
    String header = request.getHeader(AUTH_HEADER);
    if (header == null || !header.startsWith(BEARER_PREFIX)) {
        throw new BusinessException(ErrorCode.UNAUTHORIZED);          // 3001
    }
    String token = header.substring(BEARER_PREFIX.length());
    try {
        String userId = jwtTokenProvider.parse(token).getSubject();
        MDC.put(TraceIdFilter.USER_ID_MDC_KEY, userId);                // BE-03 一併處理
        return userId;
    } catch (ExpiredJwtException ex) {
        throw new BusinessException(ErrorCode.TOKEN_EXPIRED);          // 3002
    } catch (JwtException | IllegalArgumentException ex) {
        throw new BusinessException(ErrorCode.TOKEN_INVALID);          // 3003
    }
}
```

| 情境 | 後端回應 | 前端動作 |
|------|----------|----------|
| 沒帶 Authorization / 不是 Bearer | `3001 UNAUTHORIZED` | 導回登入（埋點：unauthenticated） |
| token 已過期 | `3002 TOKEN_EXPIRED` | 觸發 refresh token；失敗才導回登入 |
| 簽章不符 / 格式錯誤 | `3003 TOKEN_INVALID` | 直接導回登入（埋點：tampered token） |

### 測試

`JwtTokenProviderTest`（新增 4 條，確認底層擲出的例外型別）：

- `parse_Valid` — 合法 token 取得 subject。
- `parse_Expired` — 過期 token 擲 `ExpiredJwtException`。
- `parse_InvalidSignature` — 用不同 secret 簽章 → `SignatureException`（屬於 `JwtException`）。
- `parse_Malformed` — 隨意字串 → `MalformedJwtException`（屬於 `JwtException`）。

`MemberControllerTest`（新增 5 條，驗證 controller 分流）：

- 無 header → 3001
- `Basic abc` 開頭 → 3001
- ExpiredJwtException → 3002
- MalformedJwtException → 3003
- 合法 Claims → 成功並寫入 MDC.userId

### 備案紀錄

Brian 在報告中提的「更佳作法」是讓 `JwtTokenProvider` 自行包裝成自訂例外，避免 controller 直接依賴 jjwt 的 exception class。Wave 1 範圍以 Brian 的最低需求（兩個 catch 分支）為界，未一併重構，保留作為 Wave 2 技術債候選項。

---

## BE-03：MDC.userId 注入

### 問題

`AuditAspect.@AfterReturning` / `@AfterThrowing` 都讀 `MDC.get("userId")`，但 `TraceIdFilter` 只放 `traceId`，沒人 put `userId`，導致所有 audit log 的 `userId` 永遠為 null。

### 修補

#### 1. 統一定義 MDC key

`backend/stock-common/src/main/java/tw/com/stockplatform/common/trace/TraceIdFilter.java`：

```java
public static final String TRACE_ID_MDC_KEY = "traceId";
public static final String USER_ID_MDC_KEY = "userId";
```

`finally` block 同時清掉兩個 key，避免 thread pool 重用時殘留。

#### 2. 在三個入口注入 userId

| 入口 | 檔案 | 注入時機 |
|------|------|----------|
| 註冊成功 | `MemberServiceImpl.register` | `memberMapper.insert(...) == 1` 之後 |
| 登入成功 | `MemberServiceImpl.login` | 密碼驗證通過、構造 `LoginResponse` 之前 |
| 任何帶 token 的呼叫 | `MemberController.currentUserId` | `jwtTokenProvider.parse(token)` 成功之後 |

#### 3. 清理時機

由 `TraceIdFilter` 在請求結束的 `finally` 一次清乾淨；不在 service / controller 的 finally 個別清，因為同一請求可能多次 put（`currentUserId` 每次呼叫都會 put）。

### 測試

`MemberServiceImplTest`：

- `register_Success` 斷言 `MDC.get("userId")` 不為 null。
- `login_Success` 斷言 `MDC.get("userId")` 等於預期 `uid-1`。
- `@AfterEach` 清除 MDC，避免測試之間污染。

`MemberControllerTest.currentUserId_ValidTokenInjectsMdc` 斷言合法 token 解析後 MDC 有 `uid-42`。

### 後續（Wave 2 候選）

- `AuditAspect` 取 userId 時若仍為 null，建議補一筆 `@SystemAudit` 標記「未認證流程」，避免靜默失敗。
- 可考慮把 MDC put / clear 抽到 `JwtAuthenticationFilter`（Wave 1 尚未實作），讓 controller 不必自己管。

---

## BE-M02：Placeholder Controller 改回 9003

### 問題

Wave 1 所有未實作模組的 `*PlaceholderController._status` 一律回 `9001 SYSTEM_BUSY`，前端會誤判為「真實的系統繁忙」而觸發重試 / 告警，與 Wave 2+ 才會上線的「該模組尚未開放」混為一談。

### 修補

#### 1. 新增錯誤碼

`backend/stock-common/src/main/java/tw/com/stockplatform/common/constant/ErrorCode.java`：

```java
FEATURE_NOT_AVAILABLE(9003, "功能尚未開放，敬請期待"),
```

維持 9xxx 系統錯誤段；9001 留給「真實系統繁忙」、9003 專供 placeholder。

#### 2. 9 個 Placeholder Controller 全部更新

| 模組 | 檔案 |
|------|------|
| watchlist | `WatchlistPlaceholderController.java` |
| quote | `QuotePlaceholderController.java` |
| technical | `TechnicalPlaceholderController.java` |
| chip | `ChipPlaceholderController.java` |
| fundamental | `FundamentalPlaceholderController.java` |
| news | `NewsPlaceholderController.java` |
| risk | `RiskPlaceholderController.java` |
| score | `ScorePlaceholderController.java` |
| notify | `NotifyPlaceholderController.java` |

```java
return ApiResponse.fail(ErrorCode.FEATURE_NOT_AVAILABLE.getCode(),
    "功能尚未開放，敬請期待");
```

註解亦同步從「9001 系統繁忙」改為「9003 FEATURE_NOT_AVAILABLE」。

---

## 跨團隊產出

### `errorCodes.md`（給 Felix）

`docs/05_development/backend/errorCodes.md` 新增完整錯誤碼對照表，含：

- 設計原則（Envelope Pattern、HTTP 200、依 code 分流）
- 分段總覽（0 / 1xxx / 2xxx / 3xxx / 4xxx / 5xxx / 9xxx）
- 完整錯誤碼清單與前端建議行為
- JWT 例外分流契約（3001 / 3002 / 3003）
- Placeholder API 行為說明（9001 vs 9003）
- 參數校驗 `errors[]` 結構
- traceId 對齊提醒

---

## 修改檔案清單

### 主程式

- `backend/stock-common/src/main/java/tw/com/stockplatform/common/constant/ErrorCode.java`
- `backend/stock-common/src/main/java/tw/com/stockplatform/common/trace/TraceIdFilter.java`
- `backend/stock-member/src/main/java/tw/com/stockplatform/member/repository/MemberMapper.java`
- `backend/stock-member/src/main/java/tw/com/stockplatform/member/service/impl/MemberServiceImpl.java`
- `backend/stock-member/src/main/java/tw/com/stockplatform/member/controller/MemberController.java`
- `backend/stock-watchlist/src/main/java/tw/com/stockplatform/watchlist/controller/WatchlistPlaceholderController.java`
- `backend/stock-quote/src/main/java/tw/com/stockplatform/quote/controller/QuotePlaceholderController.java`
- `backend/stock-technical/src/main/java/tw/com/stockplatform/technical/controller/TechnicalPlaceholderController.java`
- `backend/stock-chip/src/main/java/tw/com/stockplatform/chip/controller/ChipPlaceholderController.java`
- `backend/stock-fundamental/src/main/java/tw/com/stockplatform/fundamental/controller/FundamentalPlaceholderController.java`
- `backend/stock-news/src/main/java/tw/com/stockplatform/news/controller/NewsPlaceholderController.java`
- `backend/stock-risk/src/main/java/tw/com/stockplatform/risk/controller/RiskPlaceholderController.java`
- `backend/stock-score/src/main/java/tw/com/stockplatform/score/controller/ScorePlaceholderController.java`
- `backend/stock-notify/src/main/java/tw/com/stockplatform/notify/controller/NotifyPlaceholderController.java`

### 測試

- `backend/stock-member/src/test/java/tw/com/stockplatform/member/service/MemberServiceImplTest.java`（新增 1 條 + 強化 2 條）
- `backend/stock-member/src/test/java/tw/com/stockplatform/member/security/JwtTokenProviderTest.java`（**新檔**，4 條）
- `backend/stock-member/src/test/java/tw/com/stockplatform/member/controller/MemberControllerTest.java`（**新檔**，5 條）

### 文件

- `docs/05_development/backend/errorCodes.md`（**新檔**，給 Felix）
- `docs/05_development/backend/20260422_wave1_blocker-fix.md`（本檔）

---

## 測試結果

```
$ mvn test
[INFO] ------------------------------------------------------------------------
[INFO] Reactor Summary for stock-platform 0.0.1-SNAPSHOT:
[INFO] stock-platform .................................... SUCCESS
[INFO] stock-common ...................................... SUCCESS
[INFO] stock-domain ...................................... SUCCESS
[INFO] stock-infrastructure .............................. SUCCESS
[INFO] stock-member ...................................... SUCCESS
[INFO]   Tests run: 14, Failures: 0, Errors: 0, Skipped: 0
[INFO]     - MemberServiceImplTest         5 / 5
[INFO]     - JwtTokenProviderTest          4 / 4
[INFO]     - MemberControllerTest          5 / 5
[INFO] stock-watchlist ................................... SUCCESS
[INFO] stock-quote ....................................... SUCCESS
[INFO] stock-technical ................................... SUCCESS
[INFO] stock-chip ........................................ SUCCESS
[INFO] stock-fundamental ................................. SUCCESS
[INFO] stock-news ........................................ SUCCESS
[INFO] stock-risk ........................................ SUCCESS
[INFO] stock-score ....................................... SUCCESS
[INFO] stock-notify ...................................... SUCCESS
[INFO] stock-boot ........................................ SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
```

`mvn` 走 IntelliJ 內建 Maven 3.9.11（系統 PATH 上沒有獨立 Maven）。

---

## 新發現的問題（給 Jamie 評估）

1. **`JwtTokenProvider` 直接洩漏 jjwt 例外類型到 controller**
   Brian 報告中已點名為「更佳作法」候選；本次未一併重構，建議列入 Wave 2 技術債清單。

2. **MDC.userId 注入點分散在 3 處（register / login / currentUserId）**
   未來若新增「驗證 email」、「忘記密碼」等流程，每個入口都需要記得寫一行 `MDC.put`，容易漏。建議 Wave 2 統一在 `JwtAuthenticationFilter` 處理。

3. **`LoginResponse.member` 是否要保留**（Brian Major #7）
   屬於 API 契約，需與 Felix 確認；本次未動，待 Jamie 排入後續討論。

4. **placeholder controller 沒有 `@PostMapping` 以外的方法**
   Wave 2 各模組接手時，若採 RESTful 設計需重新調整路徑。本次只改回應碼，不動路徑。

5. **`AuditAspect` 在 userId 為 null 時的行為未驗證**
   建議 Wave 1 收尾前補一條整合測試，確保「未登入呼叫 placeholder」不會因 null userId 在切面爆掉。
