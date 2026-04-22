# Rule: 環境配置規範

> **適用範圍**：所有專案配置
> **適用 Agents**：Sophia、Preston、Felix、Bruno、Linus

---

## 必備環境

每個專案**至少**包含以下三個環境：

| 環境 | 用途 |
|------|------|
| **local** | 本地開發 |
| **dev** | 開發團隊整合 |
| **prod** | 生產環境 |

可視需求添加：**uat**、**stg**、**preProd**

## 環境分類詳解

### local（本地環境）

- **定位**：預設環境
- **用途**：工程師本機開發
- **託管服務**：Docker 容器（真實服務，例如 Oracle XE on Docker）
- **單元測試**：Testcontainers
- **配置參數**：可明碼直接設定

### dev（開發環境）

- **用途**：前後端團隊整合測試
- **託管服務**：真實 Docker 容器或雲端服務
- **配置參數**：透過外部環境變數注入
- **部署**：CI/CD（Jenkins）

### uat（內部整合環境）

- **用途**：驗證符合需求
- **環境標準**：比照 prod
- **配置參數**：外部環境變數
- **部署**：CI/CD

### stg（外部整合環境）

- **用途**：與其他系統串接驗證
- **環境標準**：比照 prod
- **配置參數**：外部環境變數
- **部署**：CI/CD

### preProd（預生產環境）

- **用途**：純淨資料、生產驗證
- **環境標準**：比照 prod
- **配置參數**：外部環境變數
- **部署**：CI/CD

### prod（生產環境）

- **用途**：線上正式營運
- **託管服務**：DevOps + 雲端商維護
- **配置參數**：外部環境變數
- **部署**：CI/CD

## Spring Profile 範例

```yaml
# application.yml（共通配置）
spring:
  application:
    name: my-service
  profiles:
    active: ${SPRING_PROFILES_ACTIVE:local}

# application-local.yml
spring:
  datasource:
    url: jdbc:oracle:thin:@localhost:1521:XE
    username: dev
    password: dev123  # local 可明碼
logging:
  level:
    com.example: DEBUG

# application-dev.yml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
logging:
  level:
    com.example: INFO

# application-prod.yml
spring:
  datasource:
    url: ${DB_URL}
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: ${DB_POOL_MAX:50}
logging:
  level:
    com.example: WARN
```

## React 環境變數範例

```bash
# .env.local（本地，不 commit）
VITE_API_BASE_URL=http://localhost:8080
VITE_LOG_LEVEL=debug

# .env.development
VITE_API_BASE_URL=https://dev-api.example.com

# .env.production（CI 注入）
VITE_API_BASE_URL=https://api.example.com
```

## 配置原則

1. **明確設定**：所有環境參數必須有明確值
2. **無預設值依賴**：不依賴框架預設值
3. **無嵌套變數**：避免 `${A:${B:default}}` 這類
4. **敏感資訊**：必須使用環境變數或 Vault（**禁止**寫死在 yml）
5. **時區**：固定 GMT+8（除非業務需求）

## 託管服務管理

| 環境 | 託管方式 |
|------|----------|
| local | 工程師自行管理（Docker） |
| dev | DevOps 提供 Docker 容器 |
| uat / stg / preProd / prod | DevOps + 雲端商維護 |

**開發人員只使用，不介入管理**。

## 禁止事項

- **禁止**將敏感資訊（密碼、API key）寫入 git
- **禁止**在 prod 使用降級處理或本地快取
- **禁止**跨環境共用資料庫
- **禁止**使用嵌套變數
- **禁止**省略 local 環境配置
