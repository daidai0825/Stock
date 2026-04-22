# Rule: Oracle 資料庫規範

> **適用範圍**：`*.sql`、`*Mapper.xml`、`**/po/**`、`**/repository/**`
> **適用 Agents**：Bruno、Brian、Preston
> **資料庫**：Oracle 19c+

---

## 核心架構原則

### Application-Centric Architecture

所有商業邏輯集中在應用層（Java Service），不依賴資料庫特性。

### Persistence Ignorance

領域模型不依賴特定資料庫技術。

### Smart Service, Dumb Database

- **DB 只負責**：資料持久化、索引、約束
- **DB 不負責**：業務邏輯、計算、流程控制
- **禁止**：觸發器、預存程序、Function 處理業務

## 命名規範

### 表命名

- **格式**：`{MODULE}_{ENTITY}` 全大寫，底線分隔
- **單數**：`USER_INFO` 而非 `USERS`
- **避免**：RDMS 保留字（USER、ORDER、GROUP 等）

```sql
-- ✅ 正確
CREATE TABLE USER_INFO (...);
CREATE TABLE ORDER_HEADER (...);
CREATE TABLE PRODUCT_INFO (...);

-- ❌ 錯誤
CREATE TABLE USER (...);    -- USER 是保留字
CREATE TABLE Users (...);   -- 大小寫混用
```

### 欄位命名

- **格式**：snake_case 全小寫
- **ID 欄位**：`{entity}_id`（例如：`user_id`、`order_id`）
- **時間欄位**：`*_at`（例如：`created_at`、`updated_at`）

```sql
CREATE TABLE USER_INFO (
    user_id      VARCHAR2(36)  NOT NULL,
    email        VARCHAR2(255) NOT NULL,
    display_name VARCHAR2(100),
    created_at   TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at   TIMESTAMP,
    CONSTRAINT pk_user_info PRIMARY KEY (user_id),
    CONSTRAINT uk_user_info_email UNIQUE (email)
);
```

### 索引命名

- **主鍵**：`pk_{table}`
- **唯一索引**：`uk_{table}_{columns}`
- **一般索引**：`idx_{table}_{columns}`
- **外鍵**：`fk_{table}_{ref-table}`

## 欄位型別對照

| 用途 | Oracle 型別 | Java 型別 |
|------|-------------|-----------|
| ID（UUID） | `VARCHAR2(36)` | `String` |
| 短字串 | `VARCHAR2(255)` | `String` |
| 長文字 | `CLOB` | `String` |
| 布林 | `NUMBER(1)` | `Boolean`（0/1） |
| 整數 | `NUMBER(10)` | `Integer` / `Long` |
| 金額 | `NUMBER(18,2)` | `BigDecimal` |
| 日期時間（不帶時區） | `TIMESTAMP` | `LocalDateTime` |
| 日期 | `DATE` | `LocalDate` |
| JSON | `CLOB` + `IS JSON` | `String` 或自訂物件 |
| 二進位 | `BLOB` | `byte[]` |

## ID 設計

### 預設 UUID 字串

```sql
user_id VARCHAR2(36) NOT NULL  -- 由 Java UUID.randomUUID() 產生
```

```java
String userId = UUID.randomUUID().toString();
```

**禁止**：
- 使用 Oracle SEQUENCE 作為業務 ID（除非有特殊需求）
- 使用自增 ID（無法在分散式系統中保證唯一性）

## 時間設計

### 不帶時區

```sql
created_at TIMESTAMP NOT NULL  -- 不使用 TIMESTAMP WITH TIME ZONE
```

### 應用層處理時區

```java
// 統一在應用層處理時區（GMT+8）
LocalDateTime now = LocalDateTime.now(ZoneId.of("GMT+8"));
userPO.setCreatedAt(now);
```

對外 API 才轉換為 ISO 8601 含時區：
```
"createdAt": "2026-04-21T10:30:45.123+08:00"
```

## 索引策略

### 必建索引

- 主鍵（自動）
- 外鍵（避免子查詢效能問題）
- 唯一性欄位（email、phone）
- 高頻查詢欄位（status、type）
- 排序欄位（created_at DESC）

### 複合索引

注意欄位順序（左前綴原則）：

```sql
-- 查詢：WHERE user_id = ? AND status = ?
CREATE INDEX idx_order_user_status ON ORDER_HEADER (user_id, status);
```

### 避免

- 過多索引（影響寫入效能）
- 在低基數欄位建索引（如 boolean）

## SQL 撰寫規範

### MyBatis Mapper

```java
// ✅ 使用 #{} 避免 SQL Injection
@Select("SELECT * FROM USER_INFO WHERE user_id = #{userId}")
Optional<UserPO> findById(String userId);

// ❌ 禁止使用 ${}（除非是動態欄位/表名）
@Select("SELECT * FROM USER_INFO WHERE user_id = ${userId}")  // SQL Injection 風險
```

### 動態 SQL（XML）

```xml
<!-- UserMapper.xml -->
<select id="search" resultType="UserPO">
    SELECT * FROM USER_INFO
    <where>
        <if test="email != null">
            AND email = #{email}
        </if>
        <if test="status != null">
            AND status = #{status}
        </if>
    </where>
    ORDER BY created_at DESC
    OFFSET #{offset} ROWS FETCH NEXT #{limit} ROWS ONLY
</select>
```

### 分頁（Oracle 12c+）

```sql
-- 使用 OFFSET ... FETCH（避免 ROWNUM 寫法的複雜性）
SELECT * FROM USER_INFO
ORDER BY created_at DESC
OFFSET 20 ROWS FETCH NEXT 10 ROWS ONLY;
```

### 避免 SELECT *

```sql
-- ❌ 不好
SELECT * FROM USER_INFO;

-- ✅ 明確指定欄位
SELECT user_id, email, created_at FROM USER_INFO;
```

## 約束設計

```sql
-- 主鍵
CONSTRAINT pk_user_info PRIMARY KEY (user_id)

-- 唯一
CONSTRAINT uk_user_info_email UNIQUE (email)

-- 外鍵（建議搭配索引）
CONSTRAINT fk_order_user FOREIGN KEY (user_id) REFERENCES USER_INFO(user_id)

-- Check
CONSTRAINT ck_user_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BANNED'))

-- Not Null（直接欄位定義）
email VARCHAR2(255) NOT NULL
```

## 交易管理

由 Spring 控制（@Transactional），**禁止**在 SQL 中使用 COMMIT/ROLLBACK。

## 連線池配置

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50    # 視併發調整
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

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

## 資料遷移（Migration）

使用 Flyway 或 Liquibase（推薦 Flyway）：

```
src/main/resources/db/migration/
├── V1.0.0__init_schema.sql
├── V1.0.1__add_user_status.sql
└── V1.1.0__add_order_table.sql
```

## 禁止事項

- **禁止**業務邏輯放入觸發器、預存程序
- **禁止**使用 `${}` 拼接 SQL（除非動態表名/欄位名）
- **禁止**`SELECT *` 在生產程式碼
- **禁止**在 Java 用字串拼接 SQL
- **禁止**使用 RDMS 保留字命名表/欄位
- **禁止**主鍵使用業務欄位（如 email）
- **禁止**自增 ID（用 UUID）
- **禁止**`TIMESTAMP WITH TIME ZONE`（時區在應用層處理）
- **禁止**使用 `double`/`float`（用 `NUMBER(精度,小數)` + BigDecimal）
- **禁止**忽略外鍵索引
