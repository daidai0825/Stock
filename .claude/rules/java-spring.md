# Rule: Java + Spring 開發規範

> **適用範圍**：`*.java`、`*.xml`（MyBatis Mapper）
> **適用 Agents**：Bruno、Brian、Preston、Linus
> **技術版本**：Java 21、Spring Boot 3.x、MyBatis 3.5+

---

## 版本要求

| 技術 | 版本 |
|------|------|
| JDK | 21（LTS） |
| Spring Boot | 3.x（最新穩定版） |
| Spring Framework | 6.x |
| MyBatis | 3.5+（支援 Optional） |
| Maven | 3.9+ |
| Gradle | 8.x |

## 套件結構（標準）

```
com.{company}.{project}/
├── Application.java         # @SpringBootApplication
├── config/                  # 配置類
├── controller/              # REST endpoints
├── service/                 # 業務邏輯
│   └── impl/
├── repository/              # 資料存取（MyBatis Mapper）
├── po/                      # Persistent Object
├── dto/                     # Data Transfer Object
│   ├── request/
│   └── response/
├── convertor/               # PO ↔ DTO 轉換
├── enums/                   # 列舉
├── constant/                # 常數
├── exception/               # 例外
├── util/                    # 工具類別（Utils 結尾）
├── aspect/                  # AOP 切面
└── security/                # 認證授權
```

## 層級職責

### Controller

- 處理 HTTP 請求/回應
- 定義 REST endpoints
- 參數驗證（@Valid）
- 委派給 Service

```java
@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/create")
    public ApiResponse<UserDTO> createUser(@Valid @RequestBody CreateUserRequest request) {
        return ApiResponse.success(userService.createUser(request));
    }
}
```

### Service

- 業務邏輯實作
- 協調 Repository
- 交易管理（@Transactional）
- interface + impl 分離

```java
public interface UserService {
    UserDTO createUser(CreateUserRequest request);
}

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;

    @Override
    public UserDTO createUser(CreateUserRequest request) {
        // 業務邏輯
    }
}
```

### Repository（MyBatis Mapper）

```java
@Mapper
public interface UserMapper {

    // 使用 #{} 而非 ${} 避免 SQL Injection
    @Select("SELECT * FROM USER_INFO WHERE user_id = #{userId}")
    Optional<UserPO> findById(String userId);
}
```

### PO（Persistent Object）

- ORM 實體
- 對應資料庫表
- 命名避免 RDMS 保留字

```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPO {
    private String userId;        // VARCHAR2(36) UUID
    private String email;
    private LocalDateTime createdAt; // TIMESTAMP（不帶時區）
}
```

### DTO（Data Transfer Object）

- API 請求/回應
- 避免直接暴露 PO
- **不使用 Optional**

```java
// 使用 Java 21 records
public record CreateUserRequest(
    @NotBlank @Email String email,
    @NotBlank @Size(min = 8) String password
) {}

public record UserDTO(
    String userId,
    String email,
    LocalDateTime createdAt
) {}
```

## Java 21 現代語法（鼓勵使用）

### Records

```java
public record Money(BigDecimal amount, String currency) {}
```

### Sealed Classes

```java
public sealed interface Result<T>
    permits Success, Failure {}

public record Success<T>(T value) implements Result<T> {}
public record Failure<T>(String error) implements Result<T> {}
```

### Pattern Matching

```java
return switch (result) {
    case Success<String>(var value) -> "成功：" + value;
    case Failure<String>(var error) -> "失敗：" + error;
};
```

### Virtual Threads（高並發 I/O）

```java
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    tasks.forEach(executor::submit);
}
```

## 數值與時間

### 金額：BigDecimal

```java
// ✅ 正確
BigDecimal total = price.multiply(quantity);

// ❌ 禁止
double total = price * quantity;  // 精度問題
```

### 時間：LocalDateTime

```java
// ✅ 正確
LocalDateTime now = LocalDateTime.now(ZoneId.of("GMT+8"));

// ❌ 禁止
Date now = new Date();  // 已過時
Calendar cal = Calendar.getInstance();  // 已過時
```

## Optional 使用準則

| 場景 | 是否使用 |
|------|----------|
| Repository → Service | ✅ 可使用 |
| Service → Controller | ✅ 處理後轉換 |
| Enum 的 fromCode、find | ✅ 可使用 |
| DTO 宣告 | ❌ 禁止 |
| PO 宣告 | ❌ 禁止 |
| REST API 回傳 | ❌ 禁止 |
| 容器類型 | ❌ 用空集合（Collections.emptyList()） |

## Stream API 使用準則

### 鼓勵使用

- 多步驟轉換（filter → map → collect）
- 分組、聚合（groupingBy、summarizingDouble）
- 產生新的不可變集合

### 禁止使用

- 簡單迴圈（用 for-each）
- 需要 break/continue
- 集合元素 <10 且操作簡單
- 修改外部變數
- 已有現成方法（用 List.contains() 而非 stream().anyMatch()）

### 限制

- 操作鏈不超過 5 步
- 優先使用 Method Reference
- 複雜 Lambda 抽出為具名方法
- **禁止 parallelStream**（除非有效能需求 + 註解說明）
- 禁止巢狀 Stream

```java
// ✅ 好
List<String> activeUserNames = users.stream()
    .filter(User::isActive)
    .map(User::getName)
    .toList();

// ❌ 不好（應用 forEach）
users.stream().forEach(System.out::println);
```

## 交易管理

```java
// 預設 readOnly = false
@Transactional
public void createUser(...) { }

// 唯讀
@Transactional(readOnly = true)
public UserDTO findById(String id) { }

// 注意：checked exception 預設不 rollback
@Transactional(rollbackFor = Exception.class)
public void riskyOperation() throws BusinessException { }
```

## Lombok 使用

| 註解 | 推薦使用 |
|------|----------|
| @Data | PO、DTO（非 record） |
| @Builder | 複雜物件建構 |
| @RequiredArgsConstructor | Spring Bean 注入 |
| @Slf4j | 日誌 |
| @NoArgsConstructor | ORM 需要 |

避免過度使用 @Builder，優先使用 record。

## 禁止事項

- **禁止**使用已棄用方法（deprecated）
- **禁止**在 DTO/PO 使用 Optional
- **禁止**REST API 回傳 Optional
- **禁止**parallelStream（除非有理由）
- **禁止**double/float 處理金額
- **禁止**Date、Calendar
- **禁止**SQL 中使用 ${}（用 #{}）
- **禁止**降級處理、本地快取
- **禁止**業務邏輯放在資料庫（觸發器、預存程序）
- **禁止**System.out.println（用 SLF4J）
- **禁止**catch Exception 後吞掉
