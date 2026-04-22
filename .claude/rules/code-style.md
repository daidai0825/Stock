# Rule: 程式碼風格規範

> **適用範圍**：所有程式碼檔案
> **適用 Agents**：Felix、Bruno、Fiona、Brian

---

## 通用原則

### 行寬

- **最大字元數**：120
- 避免過早換行
- 適合寬螢幕橫向閱讀

### 縮排

- Java：4 個空格
- TypeScript / TSX：2 個空格
- 不使用 Tab（除非 Makefile）

### 結尾

- 檔案結尾必須有換行（newline）
- 移除行尾多餘空白

## 檔案編碼與換行

### 編碼

| 檔案類型 | 編碼 |
|----------|------|
| `*.java`、`*.ts`、`*.tsx`、`*.py` 等 | UTF-8 |
| `*.sh` | UTF-8 |
| `*.cmd`、`*.bat` | UTF-8 |
| `*.md`、`*.json`、`*.yml` | UTF-8 |

### 換行符

| 檔案類型 | 換行 |
|----------|------|
| `*.sh` | LF（Linux） |
| `*.cmd`、`*.bat` | CRLF（Windows） |
| 其他原始碼 | LF |

**禁止**混用換行格式。

## .editorconfig 範本

```ini
# .editorconfig
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true
indent_style = space

[*.java]
indent_size = 4

[*.{ts,tsx,js,jsx,json,yml,yaml,md}]
indent_size = 2

[*.{cmd,bat}]
end_of_line = crlf

[Makefile]
indent_style = tab
```

## 命名規範

### 通用

- **避免**單字母變數（除迴圈索引 `i`、`j`）
- **避免**縮寫（除非眾所皆知，如 `id`、`url`、`api`）
- **語意**清楚，看名稱知用途

### Java

| 對象 | 命名 |
|------|------|
| 類別 | PascalCase（`UserService`） |
| 介面 | PascalCase（`UserService`，**不加** I 前綴） |
| 方法 | camelCase（`findById`） |
| 變數 | camelCase（`userName`） |
| 常數 | UPPER_SNAKE_CASE（`MAX_RETRY_COUNT`） |
| 套件 | lowercase（`com.example.user`） |
| Enum 值 | UPPER_SNAKE_CASE（`ACTIVE`） |
| 工具類別 | 以 `Utils` 結尾（`DateUtils`） |
| 例外 | 以 `Exception` 結尾（`BusinessException`） |
| 測試類別 | 以 `Test` 結尾（`UserServiceTest`） |
| Controller | 以 `Controller` 結尾 |
| Service | 以 `Service` 結尾（介面）/ `ServiceImpl`（實作） |
| Mapper | 以 `Mapper` 結尾 |
| PO | 以 `PO` 結尾 |
| DTO | 以 `DTO` / `Request` / `Response` 結尾 |

### TypeScript / React

| 對象 | 命名 |
|------|------|
| 元件 | PascalCase（`UserProfile`） |
| 元件檔名 | PascalCase（`UserProfile.tsx`） |
| Hook | camelCase + `use` 前綴（`useUserData`） |
| 函式 | camelCase（`fetchUser`） |
| 變數 | camelCase（`userName`） |
| 常數 | UPPER_SNAKE_CASE（`MAX_PAGE_SIZE`） |
| 型別 / 介面 | PascalCase（`User`、`UserProps`） |
| 列舉 | PascalCase（`UserStatus`），值 PascalCase（`Active`） |
| 一般檔名 | camelCase（`userService.ts`） |
| 樣式檔 | `*.module.css`（CSS Module） |

### 資料庫

詳見 [oracle-database.md](oracle-database.md)。

## 註解規範

### 預設不寫註解

只在以下情況寫註解：

1. 解釋**為什麼**（不是什麼）
2. 隱藏的限制或約束
3. 微妙的不變式
4. workaround 與其原因
5. 會讓讀者驚訝的行為

### 禁止的註解

```java
// ❌ 顯而易見
// 增加 i
i++;

// ❌ 解釋程式碼做什麼（命名應自我說明）
// 取得使用者資料
User user = getUserById(id);

// ❌ 引用任務、票券、PR
// for ticket #123
// added in PR #456
```

### Javadoc / TSDoc

僅用於：
- 公開 API（library 或對外介面）
- 複雜的演算法
- 非顯而易見的副作用

範例：
```java
/**
 * 計算複利。
 * 注意：rate 必須是月利率（年利率 / 12）。
 *
 * @param principal 本金
 * @param rate 月利率（0.01 = 1%）
 * @param months 月數
 * @return 終值
 */
public BigDecimal compoundInterest(BigDecimal principal, BigDecimal rate, int months) { }
```

## 匯入整理

### Java

順序：
1. `java.*`
2. `javax.*` / `jakarta.*`
3. 第三方（`org.springframework.*`、`com.fasterxml.*`）
4. 同公司專案（`com.{company}.*`）
5. 靜態匯入（最後）

每組之間空一行。**禁止**萬用字元匯入（`import java.util.*`）。

### TypeScript

順序：
1. React / 框架
2. 第三方套件
3. 專案絕對路徑（`@/...`）
4. 相對路徑
5. 樣式檔

```typescript
import { useState, type FC } from 'react';
import { Button, Card } from 'antd';
import { useQuery } from '@tanstack/react-query';

import { userService } from '@/services/userService';
import { type User } from '@/types';

import { Avatar } from './components/Avatar';
import styles from './UserProfile.module.css';
```

## 函式長度

- **單一函式**：建議 ≤50 行
- **單一類別/檔案**：建議 ≤500 行
- **單一方法參數**：建議 ≤5 個（過多用 DTO）
- **巢狀深度**：建議 ≤3 層

## 例外處理

```java
// ❌ 禁止吞掉例外
try {
    riskyOperation();
} catch (Exception e) {
    // 什麼都不做
}

// ❌ 禁止 catch Exception 後印出
} catch (Exception e) {
    e.printStackTrace();  // 應使用 SLF4J
}

// ✅ 正確
} catch (BusinessException e) {
    log.warn("業務例外：{}", e.getMessage());
    throw e;
} catch (Exception e) {
    log.error("未預期錯誤", e);
    throw new SystemException("系統繁忙", e);
}
```

## 日誌

使用 SLF4J，不使用 `System.out.println`。

```java
// 後端
@Slf4j
public class UserService {
    public void createUser(...) {
        log.info("建立使用者：email={}", email);
        log.debug("詳細資料：{}", request);
        log.warn("使用者已存在：email={}", email);
        log.error("資料庫錯誤", exception);
    }
}
```

```typescript
// 前端：使用 logger（不直接 console.log）
import { logger } from '@/utils/logger';

logger.info('User created', { userId });
```

## 禁止事項

- **禁止**超過 120 字元（除非無法拆分如長字串）
- **禁止**混用換行格式
- **禁止**檔案編碼非 UTF-8
- **禁止**寫顯而易見的註解
- **禁止**`System.out.println` / `console.log`（用 logger）
- **禁止**萬用字元匯入
- **禁止**dead code（註解掉的程式碼）
- **禁止**TODO 不寫日期與責任人
