# 決策紀錄：架構階段 Top 5 議題拍板

- **日期**：2026-04-21
- **決策者**：Dale + Jamie 確認
- **議題來源**：Sophia + Preston 於 Stage 3 提出
- **決策性質**：架構策略 + 部署策略

---

## A1：行情資料來源

- **方案 A**：TWSE / OTC 公開資料 ✅ **採納**
- 方案 B：付費商用授權
- 方案 C：混合
- 方案 D：延後決定

### 落實要求

| 項目 | 內容 |
|------|------|
| 資料源 | TWSE 公開 API、OTC 公開 API、MOPS 公開資訊觀測站 |
| 法律風險 | 使用者承擔，**v1 必須在使用者條款明列「資料來源為公開資料，僅供參考」** |
| Rate Limit | 嚴格控制爬取頻率，避免被封鎖 |
| Cache 策略 | Redis 快取 + 排程批次更新，降低對來源請求量 |
| Fallback | 來源異常時顯示「資料暫無法取得」（依 D1 合規文案） |

---

## A2：部署環境（**部分撤回 D2**）

- 原 D2 拍板：AWS + 地端混合
- **新拍板**：v1 開發階段**純 Docker 本機運行**，prod 部署架構**後續再定** ✅

### 落實要求

| 項目 | v1 階段 | v2 / 上線階段 |
|------|---------|---------------|
| 執行環境 | Docker Compose 本機 | 待定（AWS / 地端 / 混合） |
| 資料庫 | PostgreSQL on Docker | 待定 |
| 快取 | Redis on Docker | 待定 |
| 推播 | mock service / sandbox（Telegram Bot 可用真實） | 真實服務 |
| 行情資料 | 直接打 TWSE/OTC API | 同 |
| 監控 | local log | 待定 |
| **CI/CD** | **不需 Jenkins，先以 docker-compose up 為主** | 待 v2 規劃 |

### 影響

- ✅ Sophia 的 [系統架構文件](../../04_architecture/system/20260421_SystemArch_stock-analysis.md) 改列為**「v2 部署參考」**，v1 不執行
- ✅ 不需 Direct Connect / DMS / VPN 規劃
- ✅ 月費歸零（除 Telegram Bot 與 APNs 開發者帳號）
- ⚠️ Preston 的 Maven 結構維持有效，但不需 Spring Cloud Gateway（單體部署用 Spring MVC routing）
- ⚠️ 推播 v1 可只開 FCM + Telegram，APNs 等真機測試再啟

---

## A3：架構粒度

- **方案 A**：單體優先（Modular Monolith） ✅ **採納**
- 方案 B：微服務優先
- 方案 C：架構師投票

### 落實要求

| 項目 | 內容 |
|------|------|
| 部署形態 | 單一 Spring Boot 應用（stock-boot 啟動所有模組） |
| 模組結構 | 維持 Preston 的 14 Maven module |
| 模組通訊 | 直接方法呼叫（同進程）；**禁止**跨模組直接存取對方 Repository |
| 未來拆分 | 模組邊界即微服務邊界（為 v2 拆分鋪路） |
| ArchUnit | 必須加入 PR 檢查，防止跨模組依賴反向 |
| API Gateway | v1 不需獨立服務，用 Spring Boot Controller 直接對外 |

---

## A4：審計記錄（AUDIT_LOG）

- **方案 A**：納入 v1 ✅ **採納**
- 方案 B：延後 v1.1

### 落實要求

| 項目 | 內容 |
|------|------|
| 資料表 | `audit_logs`（id, user_id, action, target_type, target_id, ip, user_agent, request_payload, response_status, created_at） |
| 切面 | `AuditAspect`（依註解 `@Audited` 觸發） |
| 適用操作 | 登入/登出、會員資料變更、自選股增刪、警示條件變更、敏感查詢 |
| 保留期 | 至少 1 年（依個資法） |
| 開發成本 | +3 天（已可吸收於 Q4 2026 時程） |

---

## A5：預算

- **方案 A**：先本機運行（v1 不產生雲端費） ✅ **採納**

### 落實要求

| 項目 | 內容 |
|------|------|
| v1 預算 | **接近 $0**（除 Telegram Bot 與 APNs 開發者帳號 USD 99/年） |
| v2 部署預算 | **後續再評估**（Sophia 預估 NT$ 100K-175K/月，需於 v2 啟動前確認） |

---

## 全域影響清單

### 撤銷項目（v1 不做）
- ❌ AWS VPC / ALB / ECS Fargate（v1）
- ❌ ElastiCache / SQS / EventBridge（v1）
- ❌ Direct Connect / VPN / 地端機房
- ❌ Jenkins CI/CD（v1 暫不需）
- ❌ Spring Cloud Gateway / BFF 拆分（A3-A 採單體）

### 維持項目
- ✅ Java 21 + Spring Boot 3.x + MyBatis 3.5+
- ✅ PostgreSQL 16 + Redis 7（Docker）
- ✅ Preston 14 Maven module 結構
- ✅ 11 資料表 + 季度分區（local PG 可實作）
- ✅ Telegram Bot + FCM + APNs 推播
- ✅ 合規文案規範
- ✅ AUDIT_LOG（**新增 v1 範圍**）

### 新增項目
- ➕ Docker Compose 開發環境配置（PostgreSQL + Redis + 應用）
- ➕ AUDIT_LOG 資料表 + AuditAspect

---

## 簽核

- **使用者拍板**：Dale（2026-04-21）
- **Leader 確認**：Jamie（2026-04-21）
- **覆蓋舊決策**：D2 部分撤回（AWS + 地端 → v1 純 Docker）
