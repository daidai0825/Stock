# Rule: AWS + 地端部署規範

> **適用範圍**：部署相關設定、IaC、Kubernetes manifest
> **適用 Agents**：Sophia、Bruno
> **環境**：AWS 雲端 + 地端混合

---

## 部署模式

支援以下三種部署模式：

| 模式 | 用途 |
|------|------|
| **Cloud Only**（純 AWS） | 預設模式 |
| **On-Premise Only**（純地端） | 客戶要求或合規限制 |
| **Hybrid**（混合） | AWS + 地端整合 |

## AWS 標準架構

### 區域與可用區

- **主要 Region**：`ap-northeast-1`（東京）
- **DR Region**：`ap-southeast-1`（新加坡）
- **多 AZ**：至少跨 2 個 AZ

### 網路架構

```
┌─────────────────────────────────────────────┐
│ VPC: 10.0.0.0/16                            │
│                                             │
│ ┌──────────────┐    ┌──────────────┐       │
│ │ Public AZ-A  │    │ Public AZ-C  │       │
│ │ 10.0.1.0/24  │    │ 10.0.2.0/24  │       │
│ │ ALB / NAT GW │    │ ALB / NAT GW │       │
│ └──────────────┘    └──────────────┘       │
│         │                  │                │
│ ┌──────────────┐    ┌──────────────┐       │
│ │ Private AZ-A │    │ Private AZ-C │       │
│ │ 10.0.10.0/24 │    │ 10.0.11.0/24 │       │
│ │ ECS / EKS    │    │ ECS / EKS    │       │
│ └──────────────┘    └──────────────┘       │
│         │                  │                │
│ ┌──────────────┐    ┌──────────────┐       │
│ │ DB AZ-A      │    │ DB AZ-C      │       │
│ │ 10.0.20.0/24 │    │ 10.0.21.0/24 │       │
│ │ RDS Primary  │    │ RDS Standby  │       │
│ └──────────────┘    └──────────────┘       │
└─────────────────────────────────────────────┘
```

### 標準 AWS 服務

| 用途 | 服務 |
|------|------|
| 計算 | ECS Fargate / EKS |
| 負載平衡 | ALB（HTTP/HTTPS）、NLB（TCP） |
| 資料庫 | RDS for Oracle（如客戶要求 Oracle） |
| 快取 | ElastiCache for Redis |
| 訊息佇列 | SQS / SNS |
| 物件儲存 | S3 |
| CDN | CloudFront |
| DNS | Route 53 |
| 認證 | Cognito（外部使用者）/ IAM（內部） |
| 機密管理 | Secrets Manager / Parameter Store |
| 監控 | CloudWatch + X-Ray |
| Log | CloudWatch Logs + S3 歸檔 |
| API Gateway | API Gateway |
| WAF | AWS WAF |

### IAM 原則

- **最小權限原則**
- 使用 Role 而非 User（給服務）
- MFA 必啟用（給人員）
- 定期輪替 access key
- IAM policy 使用 condition（IP、time、MFA）

## 地端部署架構

### 標準堆疊

| 層級 | 技術 |
|------|------|
| 容器編排 | Kubernetes / Docker Compose |
| 反向代理 | Nginx / HAProxy |
| 資料庫 | Oracle 19c+ |
| 快取 | Redis（Standalone 或 Sentinel） |
| 訊息 | RabbitMQ / Kafka |
| 監控 | Prometheus + Grafana |
| Log | ELK / Loki |

### 機器規格建議（中型專案）

| 角色 | CPU | RAM | Disk | 數量 |
|------|-----|-----|------|------|
| App Server | 8 vCPU | 16 GB | 100 GB SSD | 2-4 |
| DB Server（Oracle） | 16 vCPU | 64 GB | 1 TB SSD | 1（Active）+ 1（Standby） |
| Redis | 4 vCPU | 8 GB | 50 GB SSD | 3（Sentinel） |
| Nginx | 4 vCPU | 8 GB | 50 GB SSD | 2（HA） |

## 混合架構連線

### VPN 方案

- AWS Site-to-Site VPN
- 加密：IPsec
- 適用：低流量、預算有限

### Direct Connect 方案

- AWS Direct Connect（專線）
- 頻寬：1 Gbps / 10 Gbps
- 適用：大流量、低延遲需求
- DR：搭配 VPN 作 backup

## 容器化規範

### Dockerfile 範例（Java）

```dockerfile
# Multi-stage build
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn package -DskipTests

# Runtime
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s \
    CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Dockerfile 範例（Vite + Nginx）

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

## Kubernetes 規範

### Deployment 範例

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: my-service
  namespace: production
  labels:
    app: my-service
    version: v1.2.0
spec:
  replicas: 3
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1
      maxUnavailable: 0
  selector:
    matchLabels:
      app: my-service
  template:
    metadata:
      labels:
        app: my-service
        version: v1.2.0
    spec:
      containers:
      - name: my-service
        image: registry.example.com/my-service:v1.2.0
        ports:
        - containerPort: 8080
        resources:
          requests:
            cpu: 500m
            memory: 1Gi
          limits:
            cpu: 2000m
            memory: 2Gi
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 60
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 5
        envFrom:
        - configMapRef:
            name: my-service-config
        - secretRef:
            name: my-service-secret
```

## IaC（基礎設施即程式碼）

### AWS：使用 Terraform

```
terraform/
├── modules/
│   ├── network/
│   ├── compute/
│   └── database/
├── environments/
│   ├── dev/
│   ├── uat/
│   └── prod/
└── main.tf
```

State 存於 S3 + DynamoDB lock：

```hcl
terraform {
  backend "s3" {
    bucket         = "my-tf-state"
    key            = "prod/terraform.tfstate"
    region         = "ap-northeast-1"
    dynamodb_table = "tf-state-lock"
    encrypt        = true
  }
}
```

### 地端：使用 Ansible

```
ansible/
├── inventory/
│   ├── dev.yml
│   └── prod.yml
├── playbooks/
│   ├── setup.yml
│   └── deploy.yml
└── roles/
```

## 監控標準

### Metrics（Prometheus / CloudWatch）

必收集：
- CPU、Memory、Disk、Network
- Request rate、Error rate、Duration（RED）
- 資料庫：connection pool、query time、locks
- JVM：heap、GC、thread

### Logging

- 結構化（JSON）
- 包含 traceId、userId、timestamp
- 集中收集（CloudWatch Logs / ELK）
- Retention：dev 7 天、prod 90 天 + S3 歸檔 1 年

### Tracing

- AWS X-Ray 或 Jaeger
- 整合 Spring Cloud Sleuth
- 涵蓋跨服務呼叫

### Alerting

| 等級 | 通道 | 範例 |
|------|------|------|
| P0 | PagerDuty + Phone | prod 服務全掛 |
| P1 | Slack + Email | error rate > 5% |
| P2 | Slack | 部署失敗 |
| P3 | Email | 週報 |

## 災難復原

| 指標 | 目標 |
|------|------|
| RTO（恢復時間） | < 1 小時 |
| RPO（資料遺失） | < 15 分鐘 |
| 備份頻率 | DB 每日全備 + 每小時增量 |
| 備份保留 | 30 天 + 月度長期備份 12 個月 |
| DR 演練 | 每季 1 次 |

## 安全強化

- TLS 1.3（最低 TLS 1.2）
- HSTS 啟用
- CSP（Content Security Policy）
- WAF 規則（OWASP Top 10）
- DDoS 防護（CloudFront / AWS Shield）
- 定期滲透測試（每半年）

## 禁止事項

- **禁止**生產環境使用單一 AZ
- **禁止**RDS 不啟用 Multi-AZ
- **禁止**資料庫直接對外暴露
- **禁止**將 secret 寫入 ConfigMap（用 Secret + KMS）
- **禁止**容器以 root 執行
- **禁止**跳過 health check
- **禁止**生產環境無備份
- **禁止**無監控與告警
