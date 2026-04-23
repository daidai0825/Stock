# Rule: 關聯式資料庫規範（PostgreSQL 16）

> **適用範圍**：`*.sql`、`*Mapper.xml`、`**/po/**`、`**/repository/**`、`**/db/migration/**`
> **適用 Agents**：Bruno、Brian、Preston
> **資料庫**：PostgreSQL 16（Wave 2 起替換原 Oracle 19c）
> **歷史**：本檔原為 `oracle-database.md`，2026-04-23 拍板 D-2026-04-23-04 後 rename
> **延伸**：未來若引入 NoSQL（DynamoDB / Redis 結構化），另行拆檔

---

## 核心架構原則

### Application-Centric Architecture

所有商業邏輯集中在應用層（Java Service），不依賴資料庫特性。

### Persistence Ignorance

領域模型不依賴特定資料庫技術。雖然當前用 PostgreSQL，PO / Mapper 不寫死方言。

### Smart Service, Dumb Database

- **DB 只負責**：資料持久化、索引、約束
- **DB 不負責**：業務邏輯、計算、流程控制
- **禁止**：觸發器、預存程序、Function 處理業務（PostgreSQL `PL/pgSQL` 一律不採用做業務）

---

## 命名規範

### 表命名

- **格式**：`{module}_{entity}` 全小寫，底線分隔（PostgreSQL 預設 case-folding 為小寫，全大寫反而需加雙引號）
- **單數**：`user_info` 而非 `users`
- **避免**：SQL 保留字（`user`、`order`、`group`、`session` 等）

```sql
-- ✅ 正確
CREATE TABLE user_info (...);
CREATE TABLE order_header (...);
CREATE TABLE product_info (...);

-- ❌ 錯誤
CREATE TABLE "user" (...);   -- user 保留字，被迫加雙引號
CREATE TABLE Users (...);    -- 大小寫混用，PostgreSQL 自動降為 users
```

### 欄位命名

- **格式**：snake_case 全小寫
- **ID 欄位**：`{entity}_id`（例如：`user_id`、`order_id`）
- **時間欄位**：`*_at`（例如：`created_at`、`updated_at`）

```sql
CREATE TABLE user_info (
    user_id      VARCHAR(36)  NOT NULL,
    email        VARCHAR(255) NOT NULL,
    display_name VARCHAR(100),
    created_at   TIMESTAMP    NOT NULL DEFAULT (NOW() AT TIME ZONE 'UTC'),
    updated_at   TIMESTAMP,
    CONSTRAINT pk_user_info PRIMARY KEY (user_id),
    CONSTRAINT uk_user_info_email UNIQUE (email)
);
```

> **時區提醒**：DB 統一存 UTC，應用層轉 GMT+8。詳見「時間設計」段。

### 索引命名

- **主鍵**：`pk_{table}`
- **唯一索引**：`uk_{table}_{columns}`
- **一般索引**：`idx_{table}_{columns}`
- **外鍵**：`fk_{table}_{ref-table}`
- **GIN / GiST 索引**：`gin_{table}_{column}` / `gist_{table}_{column}`

---

## 欄位型別對照

| 用途 | PostgreSQL 型別 | Java 型別 | 備註 |
|------|------------------|-----------|------|
| ID（UUID 字串） | `VARCHAR(36)` | `String` | 不用原生 `UUID` 型別，跨庫遷移成本高 |
| 短字串 | `VARCHAR(255)` | `String` | |
| 中等文字 | `VARCHAR(4000)` | `String` | |
| 長文字 | `TEXT` | `String` | PostgreSQL 無長度限制，效能與 VARCHAR 相同 |
| 布林 | `BOOLEAN` | `Boolean` | PostgreSQL 原生 BOOLEAN，不用 0/1 |
| 整數（小） | `INTEGER` | `Integer` | -2^31 ~ 2^31-1 |
| 整數（大） | `BIGINT` | `Long` | -2^63 ~ 2^63-1 |
| 金額 | `NUMERIC(18,2)` | `BigDecimal` | 等同 `DECIMAL(18,2)` |
| 高精度小數 | `NUMERIC(20,8)` | `BigDecimal` | 例如：股價 |
| 日期時間（不帶時區） | `TIMESTAMP` | `LocalDateTime` | DB 統一 UTC，應用層轉時區 |
| 日期 | `DATE` | `LocalDate` | |
| JSON | `JSONB` | `String` 或自訂物件 | 優於 `JSON`，支援 GIN 索引 |
| 二進位 | `BYTEA` | `byte[]` | 大檔建議改存 S3 |
| Enum | `VARCHAR(32)` | Java enum | 不用 PG `CREATE TYPE ... AS ENUM`，遷移痛 |

> **禁止 PostgreSQL 專屬型別**：`SERIAL`、`BIGSERIAL`、`TIMESTAMPTZ`、自訂 `ENUM`、`HSTORE`、`UUID`（原生）— 為的是保留切換 RDB 的彈性。

---

## ID 設計

### 預設 UUID 字串

```sql
user_id VARCHAR(36) NOT NULL  -- 由 Java UUID.randomUUID() 產生
```

```java
String userId = UUID.randomUUID().toString();
```

**禁止**：
- 使用 `SERIAL` / `BIGSERIAL` 自增 ID（無法在多 region / 分散式保證唯一）
- 使用 `nextval('seq_xxx')` Sequence 作為業務 ID
- 主鍵使用業務欄位（如 email、stock_id）

---

## 時間設計

### DB 統一 UTC，不帶時區

```sql
created_at TIMESTAMP NOT NULL  -- 不使用 TIMESTAMPTZ
                               -- 寫入時應用層已轉 UTC
```

### 應用層處理時區

```java
// 寫入：固定 UTC
LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
userPO.setCreatedAt(nowUtc);

// 讀取：渲染前轉 GMT+8
LocalDateTime localTime = userPO.getCreatedAt()
    .atOffset(ZoneOffset.UTC)
    .atZoneSameInstant(ZoneId.of("Asia/Taipei"))
    .toLocalDateTime();
```

### 對外 API：ISO 8601 含時區

```json
"createdAt": "2026-04-23T10:30:45.123+08:00"
```

> **為什麼不用 `TIMESTAMPTZ`**：PostgreSQL `TIMESTAMPTZ` 雖會自動轉 UTC 存，但讀取時依連線 session 時區轉換，跨環境（local vs prod）容易出錯。明示存 UTC 較安全。

---

## 索引策略

### 必建索引

- 主鍵（`PRIMARY KEY` 自動建）
- 外鍵欄位（PostgreSQL 不會自動為外鍵建索引，必須手動）
- 唯一性欄位（email、phone）
- 高頻查詢欄位（status、type）
- 排序欄位（created_at DESC）

### 複合索引

注意欄位順序（左前綴原則）：

```sql
-- 查詢：WHERE user_id = ? AND status = ?
CREATE INDEX idx_order_user_status ON order_header (user_id, status);
```

### PostgreSQL 特殊索引

| 索引類型 | 用途 |
|----------|------|
| `BTREE`（預設） | 等值、範圍查詢 |
| `GIN` | JSONB、全文檢索、陣列 |
| `GiST` | 地理資料、模糊比對 |
| `pg_trgm` + GIN | 中文 / 子字串 LIKE 加速（搜尋功能必備） |

```sql
-- 全文搜尋：股名 LIKE '台積%'
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE INDEX gin_stock_info_name_trgm
    ON stock_info USING GIN (stock_name gin_trgm_ops);

-- JSONB 欄位查詢
CREATE INDEX gin_alert_condition ON price_alert USING GIN (condition);
```

> **pg_trgm extension** 在 dev / prod 必須由 DBA 預先 `CREATE EXTENSION`，應用啟動時不要動。

### 避免

- 過多索引（影響寫入效能）
- 在低基數欄位建索引（如 boolean、status 只有 2~3 種值）
- 部分索引（partial index）除非確認查詢場景

---

## SQL 撰寫規範

### MyBatis Mapper

```java
// ✅ 使用 #{} 避免 SQL Injection
@Select("SELECT user_id, email, created_at FROM user_info WHERE user_id = #{userId}")
Optional<UserPO> findById(String userId);

// ❌ 禁止使用 ${}（除非是動態欄位/表名）
@Select("SELECT * FROM user_info WHERE user_id = ${userId}")  // SQL Injection 風險
```

### 動態 SQL（XML）

```xml
<!-- UserMapper.xml -->
<select id="search" resultType="UserPO">
    SELECT user_id, email, status, created_at
    FROM user_info
    <where>
        <if test="email != null">
            AND email = #{email}
        </if>
        <if test="status != null">
            AND status = #{status}
        </if>
    </where>
    ORDER BY created_at DESC
    LIMIT #{limit} OFFSET #{offset}
</select>
```

### 分頁（PostgreSQL）

```sql
-- 使用 LIMIT ... OFFSET（PostgreSQL 慣用語法）
SELECT user_id, email, created_at
FROM user_info
ORDER BY created_at DESC
LIMIT 10 OFFSET 20;
```

> **大 OFFSET 警告**：`OFFSET 100000` 等於掃描 100000 筆後丟棄，效能極差。深分頁改用 keyset pagination：
> ```sql
> WHERE created_at < ?  -- 上一頁最後一筆的 created_at
> ORDER BY created_at DESC LIMIT 10
> ```

### 避免 SELECT *

```sql
-- ❌ 不好
SELECT * FROM user_info;

-- ✅ 明確指定欄位
SELECT user_id, email, created_at FROM user_info;
```

### Upsert（避免 race condition）

```sql
-- PostgreSQL ON CONFLICT
INSERT INTO watchlist (watchlist_id, user_id, stock_id, created_at)
VALUES (#{watchlistId}, #{userId}, #{stockId}, NOW())
ON CONFLICT (user_id, stock_id) DO NOTHING;
```

---

## 約束設計

```sql
-- 主鍵
CONSTRAINT pk_user_info PRIMARY KEY (user_id)

-- 唯一
CONSTRAINT uk_user_info_email UNIQUE (email)

-- 外鍵（建議搭配索引）
CONSTRAINT fk_order_user FOREIGN KEY (user_id)
    REFERENCES user_info(user_id) ON DELETE RESTRICT

-- Check
CONSTRAINT ck_user_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BANNED'))

-- Not Null
email VARCHAR(255) NOT NULL
```

> **ON DELETE 策略**：金融類資料一律 `RESTRICT`（不可級聯刪），避免誤刪導致歷史資料消失。需要刪除請改為 soft delete（加 `deleted_at` 欄位）。

---

## 交易管理

由 Spring 控制（`@Transactional`），**禁止**在 SQL 中使用 `BEGIN` / `COMMIT` / `ROLLBACK`。

PostgreSQL 預設 isolation level 為 `READ COMMITTED`，金融類更新熱點建議：

```java
@Transactional(isolation = Isolation.REPEATABLE_READ)
public void transfer(...) { ... }
```

---

## 連線池配置

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/stock?currentSchema=stock&ApplicationName=stock-platform
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 50    # 視併發調整，PostgreSQL 預設 max_connections=100
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
      data-source-properties:
        prepareThreshold: 5    # 提升 prepared statement cache
        ApplicationName: stock-platform
```

> **`maximum-pool-size` × Pod 數** 不可超過 PostgreSQL `max_connections`，否則 fail-fast。生產建議搭配 PgBouncer 做 connection pooling。

---

## 效能準則

### 避免 N+1 Query

```java
// ❌ N+1（每個 user 一次查詢）
List<User> users = userMapper.findAll();
users.forEach(u -> u.setOrders(orderMapper.findByUserId(u.getId())));

// ✅ 一次 JOIN
List<User> users = userMapper.findAllWithOrders();
```

### 大量資料用批次

```java
// ✅ Batch insert
userMapper.batchInsert(users);  // MyBatis ExecutorType.BATCH
```

### 大量讀取用 ResultHandler

```java
// 避免一次載入到記憶體
sqlSession.select("findAll", new ResultHandler<UserPO>() {
    @Override
    public void handleResult(ResultContext<? extends UserPO> ctx) {
        // 處理單筆
    }
});
```

### EXPLAIN ANALYZE

開發環境慢查詢必跑：

```sql
EXPLAIN (ANALYZE, BUFFERS) SELECT ... FROM user_info WHERE ...;
```

關注：
- `Seq Scan` 是否該改 Index Scan
- `Rows Removed by Filter` 過高 → index selectivity 差
- `Buffers: shared read` 過高 → cache miss、考慮加記憶體或調 `shared_buffers`

---

## 資料遷移（Flyway）

```
src/main/resources/db/migration/
├── V1.0.0__init_schema.sql
├── V1.0.1__add_user_status.sql
├── V2.0.0__wave3_baseline.sql
└── V2.1.0__add_watchlist.sql
```

### Wave 版本對應

| Wave | Flyway prefix | 說明 |
|------|---------------|------|
| Wave 1 / 2 | V1.x.x | StockDetail 既有 schema |
| Wave 3 | V2.x.x | watchlist / alert / search / auth |
| Wave 4+ | V3.x.x | 後續 |

### 撰寫準則

- **向下相容優先**：新增欄位用 `ALTER TABLE ADD COLUMN ... NULL`，避免 lock 全表
- **大型 migration 拆步**：Blue-Green 部署需要新舊版本並存，schema 一次只能擴張不收縮
  - Step 1：加新欄位
  - Step 2：雙寫
  - Step 3：回填舊資料
  - Step 4：切讀新欄位
  - Step 5（下個 release）：刪舊欄位
- **不可重做**：production migration 一旦執行，禁止修改 SQL 內容（會 checksum mismatch），改用新版本號修補
- **migration 內禁止業務邏輯**：只動 schema、reference data，業務資料由應用層補

---

## 備份策略

| 類型 | 頻率 | 保留 |
|------|------|------|
| `pg_basebackup` 全量 | 每日 | 30 天 |
| WAL 增量（PITR） | 持續 | 7 天 |
| 月度 snapshot 歸檔 S3 | 每月 1 號 | 12 個月 |
| DR 演練 | 每季 | — |

RDS PostgreSQL 啟用 Multi-AZ + Automated Backups + Snapshot to S3。

---

## 禁止事項

- **禁止**業務邏輯放入觸發器、預存程序、PL/pgSQL function
- **禁止**使用 `${}` 拼接 SQL（除非動態表名 / 欄位名）
- **禁止**`SELECT *` 在生產程式碼
- **禁止**在 Java 用字串拼接 SQL
- **禁止**使用 SQL 保留字命名表 / 欄位
- **禁止**主鍵使用業務欄位（如 email、stock_id）
- **禁止**自增 ID（`SERIAL` / `BIGSERIAL` / Sequence 作業務 ID）
- **禁止**`TIMESTAMPTZ`（時區在應用層處理，DB 存 UTC）
- **禁止**使用 `double` / `float` / `REAL` / `DOUBLE PRECISION`（金額用 `NUMERIC(18,2)` + BigDecimal）
- **禁止**忽略外鍵索引（PostgreSQL 不會自動建）
- **禁止**`ON DELETE CASCADE`（金融類資料一律 RESTRICT + soft delete）
- **禁止**PostgreSQL 專屬型別（`HSTORE`、自訂 `ENUM`、原生 `UUID`、`SERIAL`）
- **禁止**migration 已執行後修改內容（checksum mismatch）
- **禁止**Blue-Green 部署期間做 schema 收縮（drop column / rename）
