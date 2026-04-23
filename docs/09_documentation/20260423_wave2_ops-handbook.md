# Wave 2 部署 / 維運手冊

| 項目 | 內容 |
|------|------|
| **版本** | v1.0 |
| **日期** | 2026-04-23 |
| **對象** | DevOps、SRE、系統管理員 |
| **基準文件** | [wave2_deployment_architecture.md](../04_architecture/system/20260423_wave2_deployment_architecture.md) |

---

## 快速參考

### 環境矩陣

| 環境 | 後端 | 前端 | DB | 快取 | 用途 |
|------|------|------|-----|------|------|
| **local** | http://localhost:8080 | http://localhost:5173 | Docker postgres | Docker redis | 本機開發 |
| **dev** | dev-api.stockplatform.tw | dev.stockplatform.tw | RDS t4g.medium | ElastiCache micro | 團隊整合 |
| **uat** | uat-api.stockplatform.tw | uat.stockplatform.tw | RDS r6g.large Multi-AZ | ElastiCache small | QA 驗証 |
| **preProd** | pre-api.stockplatform.tw | pre.stockplatform.tw | RDS r6g.large Multi-AZ | ElastiCache large | 最終驗証 |
| **prod** | api.stockplatform.tw | app.stockplatform.tw | RDS r6g.large Multi-AZ | ElastiCache large | 正式營運 |

---

## 本機開發環境（local）

### 前置需求

```bash
# 系統工具
git --version        # 2.40+
docker --version     # 24.0+
docker-compose --version  # 2.20+

# 後端
java -version        # 21+ LTS
./mvnw --version     # Maven 3.9+

# 前端
node --version       # 20+ LTS
npm --version        # 10+
```

### 啟動步驟

```bash
# 1. clone 專案
git clone https://github.com/your-org/stock-platform.git
cd Stock

# 2. 後端資料庫與快取（Docker Compose）
docker-compose -f docker-compose.yml up -d
# → PostgreSQL: localhost:5432
# → Redis: localhost:6379

# 3. 後端應用啟動
cd backend
./mvnw clean install
./mvnw -pl stock-boot spring-boot:run \
  -Dspring-boot.run.arguments="--spring.profiles.active=local"
# → API: http://localhost:8080/
# → Swagger: http://localhost:8080/swagger-ui.html

# 4. 前端應用啟動
cd ../frontend
npm ci
npm run dev
# → SPA: http://localhost:5173/

# 5. 驗証
curl http://localhost:8080/actuator/health
# { "status": "UP" }
```

### 本機環境變數

建立 `backend/src/main/resources/application-local.yml`：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/stock_db
    username: dev
    password: dev123
    hikari:
      maximum-pool-size: 10
      minimum-idle: 2
  redis:
    host: localhost
    port: 6379
    timeout: 2000ms
  jpa:
    hibernate:
      ddl-auto: validate  # 不自動建表，用 Flyway
  flyway:
    enabled: true
    locations: classpath:db/migration

logging:
  level:
    root: INFO
    tw.com.stockplatform: DEBUG
  pattern:
    console: "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"

jwt:
  secret: "local-secret-key-at-least-32-characters-long"
  expiration-ms: 900000  # 15 分鐘

twse:
  api:
    base-url: https://www.twse.com.tw
    timeout: 10000
```

建立 `frontend/.env.local`：

```bash
VITE_API_BASE_URL=http://localhost:8080
VITE_LOG_LEVEL=debug
```

---

## Docker 容器部署

### 後端 Dockerfile

```dockerfile
# Multi-stage build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline

COPY . .
RUN mvn -B -pl stock-boot -am package -DskipTests

# Runtime
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app

WORKDIR /app
COPY --from=builder /build/stock-boot/target/stock-boot-*.jar app.jar

USER app
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC --enable-preview"
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 前端 Dockerfile（dev/uat 用）

```dockerfile
FROM node:20-alpine AS builder
WORKDIR /build
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=builder /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
HEALTHCHECK CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1
```

### Docker Compose（本機）

```yaml
# docker-compose.yml
version: '3.8'

services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_USER: dev
      POSTGRES_PASSWORD: dev123
      POSTGRES_DB: stock_db
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U dev"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

volumes:
  postgres_data:
```

---

## 雲端環境部署（AWS）

### ECS Fargate 部署

#### 1. 構建 Docker Image

```bash
# 後端
cd backend
docker build -t stock-platform/stock-boot:v0.2.0 .
aws ecr get-login-password --region ap-northeast-1 | \
  docker login --username AWS --password-stdin \
  <account-id>.dkr.ecr.ap-northeast-1.amazonaws.com
docker tag stock-platform/stock-boot:v0.2.0 \
  <account-id>.dkr.ecr.ap-northeast-1.amazonaws.com/stock-boot:v0.2.0
docker push <account-id>.dkr.ecr.ap-northeast-1.amazonaws.com/stock-boot:v0.2.0
```

#### 2. ECS Task Definition

```json
{
  "family": "stock-boot",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "1024",
  "memory": "2048",
  "containerDefinitions": [
    {
      "name": "stock-boot",
      "image": "<account-id>.dkr.ecr.ap-northeast-1.amazonaws.com/stock-boot:v0.2.0",
      "essential": true,
      "portMappings": [
        {
          "containerPort": 8080,
          "hostPort": 8080,
          "protocol": "tcp"
        }
      ],
      "environment": [
        {
          "name": "SPRING_PROFILES_ACTIVE",
          "value": "prod"
        }
      ],
      "secrets": [
        {
          "name": "DB_URL",
          "valueFrom": "/stock/prod/db/url"
        },
        {
          "name": "DB_USERNAME",
          "valueFrom": "/stock/prod/db/username"
        },
        {
          "name": "DB_PASSWORD",
          "valueFrom": "/stock/prod/db/password"
        },
        {
          "name": "JWT_SECRET",
          "valueFrom": "/stock/prod/jwt/secret"
        }
      ],
      "logConfiguration": {
        "logDriver": "awslogs",
        "options": {
          "awslogs-group": "/ecs/stock-boot",
          "awslogs-region": "ap-northeast-1",
          "awslogs-stream-prefix": "ecs"
        }
      },
      "healthCheck": {
        "command": ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health || exit 1"],
        "interval": 30,
        "timeout": 5,
        "retries": 3,
        "startPeriod": 60
      }
    }
  ]
}
```

#### 3. ECS Service

```bash
aws ecs create-service \
  --cluster stock-platform-prod \
  --service-name stock-boot \
  --task-definition stock-boot:1 \
  --desired-count 4 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxx,subnet-yyy],securityGroups=[sg-app],assignPublicIp=DISABLED}" \
  --load-balancers "targetGroupArn=arn:aws:elasticloadbalancing:ap-northeast-1:<account-id>:targetgroup/stock-boot/xxx,containerName=stock-boot,containerPort=8080" \
  --auto-scaling-group-name stock-boot-asg
```

#### 4. CloudFront + S3（前端）

```bash
# 上傳前端到 S3
aws s3 sync frontend/dist/ s3://stock-platform-prod-static/ --delete

# CloudFront 快取失效
aws cloudfront create-invalidation \
  --distribution-id E3XXXXXXXXXXXXX \
  --paths "/*"
```

---

## 健康檢查與監控

### 健康檢查端點

```bash
# 基礎健康檢查
curl http://api.stockplatform.tw/actuator/health
# { "status": "UP" }

# 詳細健康檢查（含所有元件）
curl http://api.stockplatform.tw/actuator/health/liveness
# { "status": "UP", "components": { "db": { "status": "UP" }, "redis": { "status": "UP" } } }

# Readiness（可處理流量）
curl http://api.stockplatform.tw/actuator/health/readiness
```

### CloudWatch 監控

| Metric | 閾值 | 動作 |
|--------|------|------|
| ALB TargetResponseTime P95 | > 500ms（5 分鐘） | 告警至 Slack |
| ECS CPU | > 80%（10 分鐘） | 觸發 auto-scaling |
| ECS Memory | > 85%（5 分鐘） | 觸發 auto-scaling |
| RDS CPU | > 80%（10 分鐘） | Email + Slack |
| RDS ReplicaLag | > 60s（5 分鐘） | 即時告警 |
| ElastiCache Evictions | > 100/min | Slack 警告 |
| SQS DLQ MessageCount | > 0 | 即時告警 |

### 日誌收集

所有應用日誌自動轉送至 CloudWatch Logs（`/ecs/stock-boot`）與 S3 歸檔（Glacier）。

```bash
# 查詢最近 1 小時的 ERROR 日誌
aws logs filter-log-events \
  --log-group-name /ecs/stock-boot \
  --filter-pattern "ERROR" \
  --start-time $(date -d '1 hour ago' +%s)000
```

---

## 資料庫管理

### 資料庫遷移（Flyway）

遷移檔案位於 `backend/src/main/resources/db/migration/`：

```
V1__init_schema.sql
V1.0.1__add_quote_table.sql
V1.0.2__add_fundamental_table.sql
V1.0.3__add_chip_table.sql
V2.0.0__add_kline_history_table.sql
```

**部署時自動執行**（Spring Boot 啟動時）。

### 手動執行遷移

```bash
# 本機驗証遷移（dry-run）
cd backend
./mvnw flyway:validate -Dspring.profiles.active=prod

# 實際執行
./mvnw flyway:migrate
```

### RDS 備份與還原

```bash
# 建立手動快照
aws rds create-db-snapshot \
  --db-instance-identifier stock-db-prod \
  --db-snapshot-identifier stock-db-prod-manual-2026-04-23

# 從快照還原
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier stock-db-restored \
  --db-snapshot-identifier stock-db-prod-manual-2026-04-23
```

### 跨區複製（Disaster Recovery）

```bash
# 複製 RDS 快照到 ap-southeast-1
aws rds copy-db-snapshot \
  --source-db-snapshot-identifier arn:aws:rds:ap-northeast-1:...:snapshot:stock-db-prod-2026-04-23 \
  --target-db-snapshot-identifier stock-db-prod-2026-04-23-dr \
  --region ap-southeast-1
```

---

## 故障排除

### 常見問題

#### 1. API 回應超時（> 200ms P95）

**徵兆**：CloudWatch ALB TargetResponseTime > 500ms

**檢查清單**：
1. 檢查 RDS 連接池是否滿
   ```bash
   # 查看 Hikari 連接數
   curl http://api/actuator/prometheus | grep hikari_connections
   ```
2. 檢查外部 API（TWSE/MOPS）是否響應緩慢
   ```bash
   # 查看平均回應時間
   aws logs insights query --log-group-name /ecs/stock-boot \
     --query-string 'fields @duration | stats avg(@duration) by uri'
   ```
3. 檢查 Redis 快取是否失效
   ```bash
   # 檢查 ElastiCache 連接數、命令延遲
   aws elasticache describe-cache-nodes --cache-cluster-id stock-redis-prod
   ```

**修復**：
- 增加 ECS task 數量（auto-scaling）
- 增加 RDS 連接池大小
- 清理 Redis 過期快取

#### 2. 資料來源錯誤（code 5010/5011/5012）

**徵兆**：API 回傳「TWSE/MOPS 資料不可用」

**檢查**：
1. TWSE / OTC / MOPS API 是否正常
   ```bash
   # 手動測試 TWSE API
   curl https://www.twse.com.tw/api/corporateAction/getStock?response=json&stockInfo=2330
   ```
2. NAT Gateway 是否正常
   ```bash
   aws ec2 describe-nat-gateways --filter Name=state,Values=available
   ```
3. Security Group 是否阻擋外出流量
   ```bash
   # 檢查 sg-app 的 outbound rule（應允許 TCP 443 to 0.0.0.0/0）
   aws ec2 describe-security-groups --group-ids sg-app
   ```

**修復**：
- 重新執行外部 API 呼叫（API 提供自動重試）
- 檢查 fallback 機制（DB 快取資料）

#### 3. 資料庫連接失敗

**徵兆**：應用無法啟動，日誌顯示「Hikari connection timeout」

**檢查**：
1. RDS 安全組是否允許應用存取
   ```bash
   # sg-app 應允許 TCP 5432 to sg-db
   aws ec2 describe-security-groups --group-ids sg-db
   ```
2. RDS 主從同步是否正常
   ```bash
   # 查看 ReplicaLag
   aws rds describe-db-instances --db-instance-identifier stock-db-prod \
     | grep -E "LatestRestorableTime|DBInstanceStatus"
   ```
3. Secrets Manager 是否正確注入
   ```bash
   # 驗証 secret 是否存在
   aws secretsmanager get-secret-value --secret-id /stock/prod/db/master
   ```

**修復**：
- 檢查 AWS Secrets Manager 輪替狀態
- 驗証 IAM 角色是否有讀取 Secret 權限
- 強制故障轉移（Multi-AZ）：
  ```bash
  aws rds reboot-db-instance \
    --db-instance-identifier stock-db-prod \
    --force-failover
  ```

---

## 部署流程

### Dev 環境（自動）

```
develop 分支 push
  ↓
Jenkins Multibranch Pipeline 觸發
  ↓
[Build] → [Test] → [Coverage] → [SonarQube] → [Dependency Check]
  ↓
若全綠：[Package] → [Push ECR] → [Deploy ECS]
  ↓
Rolling Update（1 task at a time）
  ↓
Smoke Test（curl /health）
  ↓
Slack 通知 #deployments
```

### UAT 環境（人工）

```
release/* 分支建立 + push
  ↓
Jenkins 自動執行至 Dependency Check
  ↓
QA Lead 在 Jenkins UI 點 "Deploy to UAT"
  ↓
Rolling Update（2 task → 4 task）
  ↓
Quincy / Quinn 執行 P0 測試
```

### Prod 環境（謹慎）

```
Prod release 票券建立
  ↓
Jamie + DevOps Lead 雙人審批
  ↓
Jenkins Canary 部署：
  - Stage 1: 10% 流量（1 task）→ 觀察 30 min
  - Stage 2: 50% 流量（2 task）→ 觀察 30 min
  - Stage 3: 100% 流量（4+ task）→ 觀察 30 min
  ↓
若任何階段出現異常，自動 rollback
  ↓
部署完成，更新 CHANGELOG
```

### Rollback 程序

```bash
# 自動 rollback（若 CloudWatch Alarm 觸發）
# 或手動 rollback：

# 查看前一個版本
aws ecs describe-task-definition --task-definition stock-boot

# 更新服務回滾用前一版本
aws ecs update-service \
  --cluster stock-platform-prod \
  --service stock-boot \
  --task-definition stock-boot:123  # 前一版本 revision

# 監控 rollback 進度
aws ecs describe-services \
  --cluster stock-platform-prod \
  --services stock-boot
```

---

## 災難恢復演練

每季執行一次 DR 演練（建議週六凌晨 02:00）。

### 演練清單

- [ ] **演練 1**：單一 ECS task 強制 stop → 驗証自動重啟（< 1 min）
- [ ] **演練 2**：手動 RDS Multi-AZ failover → 驗証 DNS 切換（< 60s）
- [ ] **演練 3**：從週日快照還原到新 RDS instance（staging account） → 驗証資料完整
- [ ] **演練 4**：模擬 region 故障，跑 cross-region snapshot 還原流程（20 min）
- [ ] **演練 5**：PITR 還原 2 小時前狀態

### 演練報告

```
DR 演練報告 - 2026-Q2
日期：2026-04-22（週六 02:00-04:00）
演練 1：ECS task stop
  預期 RTO：1 分鐘
  實際 RTO：0.8 分鐘 ✓

演練 2：RDS failover
  預期 RTO：60 秒
  實際 RTO：45 秒 ✓

演練 3：Snapshot restore
  預期時間：20 分鐘
  實際時間：18 分鐘 ✓
  資料驗証：全部列表與 prod 一致 ✓

演練 4：Cross-region snapshot
  預期時間：30 分鐘
  實際時間：28 分鐘 ✓

演練 5：PITR
  還原時間點：2 小時前
  還原完成時間：5 分鐘 ✓

整體評分：A（目標達成）
改善項目：None
下次演練：2026-07-15
```

---

## 效能調優

### JVM 優化（prod 環境）

在 ECS task definition 加入：

```bash
JAVA_OPTS="\
  -XX:MaxRAMPercentage=75 \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/tmp/heapdump.hprof \
  -XX:+ParallelRefProcEnabled \
  --enable-preview"
```

### 資料庫連接池調優

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 50    # prod：適應高併發
      minimum-idle: 10
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000    # 30 分鐘，低於 DB 設定的 60 分鐘
```

### Redis 快取策略

| 資料 | TTL | eviction |
|------|-----|----------|
| 行情（quote） | 30 秒 | LRU |
| 基本面（fundamental） | 24 小時 | LRU |
| 籌碼（chip） | 1 小時 | LRU |

---

## 合規與安全

- ✅ TLS 1.3（所有 HTTPS 連線）
- ✅ 密碼 BCrypt cost=12
- ✅ API JWT 認證
- ✅ SQL 參數化（防 SQL injection）
- ✅ 敏感資料加密（KMS）
- ✅ 日誌遮罩（email / token）
- ✅ WAF 規則（OWASP Top 10）
- ✅ DDoS 防護（AWS Shield）
- ✅ 每月 CVE 掃描
- ✅ 季度滲透測試（外部廠商）

---

## 維運運作時間

- **SLA**：99.5%（一般時段）/ 99.9%（盤中 09:00-13:30）
- **支援時段**：週一～週五 09:00-18:00（台北時間）
- **On-call**：24/7（P0 事件）

**聯絡方式**：
- Slack：`#oncall-infra`
- Email：infra-team@stockplatform.tw
- PagerDuty：[link]

---

**文件版本**：v1.0  
**最後更新**：2026-04-23  
**維護人員**：DevOps Team
