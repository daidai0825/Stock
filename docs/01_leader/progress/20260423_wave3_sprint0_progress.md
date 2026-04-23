# Wave 3 Sprint 0 進度報告（9 決策拍板 + 全團隊啟動）

> **報告日期**：2026-04-23
> **報告人**：Jamie（Leader）
> **報告階段**：Wave 3 Sprint 0 完成（PRD 正式 + Spec + 架構 + Sprint 0 升級）
> **目標版本**：v0.3.0（預計 6 週）

---

## 1. 一句話結論

> **使用者一次拍板 9 個決策後，Patricia / Linus / Peter / Sophia / Preston 五位並行啟動，Sprint 0 在 1.5h 內完成所有產出，本波 7 個 commit、19 份新文件、2 個 CVE 修補入庫。Wave 3 進入正式開發前的最後門檻：等使用者確認 4 項架構細節 + Jenkins credentials。**

---

## 2. 本波執行流（Auto Mode）

```
使用者：「2」(回覆 9 決策)
        ↓
Jamie 拍板（採用 Patricia / Sophia 建議方向，廠商簽約類保留）
        ↓ 並行召喚
┌─ Patricia：PRD 草稿 → 正式 v1.0  (3 min)
├─ Linus：Sprint 0 必升 (CVE 修補)  (1.5 h)
├─ Peter：Wave 3 SRS + API + UI + Schema-lock (12 min)
├─ Sophia：系統架構 + Spring Security + Blue-Green + RFP (10 min)
└─ Preston：專案架構 + ER + Module + Migration (10 min)
        ↓
Jamie 整合 → 7 commit → push develop
```

---

## 3. 9 項決策拍板結果

| # | 決策 | 拍板 | 紀錄 |
|---|------|------|------|
| D-01 | Wave 2 部署形態 | ✅ **Cloud Only**（Hybrid 延 v1.5） | 採 Sophia 建議 |
| D-02 | 滲透測試廠商 | ⏸ **保留**（合約類，待親簽） | 建議方向：金融 FinTech 經驗、預算 50 萬 |
| D-03 | prod 部署策略 | ✅ **Blue-Green**（不採 Canary） | 金融類偏確定性、回滾簡單 |
| D-04 | rule 檔 rename | ✅ **執行**：oracle-database.md → relational-database.md | 已 commit |
| D-05 | 廣度 vs 深度 | ✅ **廣度**（5 功能） | 採 Patricia 建議 |
| D-06 | Watchlist 登入策略 | ✅ **強制登入** | 強化 MAU、跨裝置同步 |
| D-07 | 推播管道 | ✅ **Web Push + Email** | LINE Notify EOL 不採用 |
| D-08 | 未授權回應 | ✅ **HTTP 200 + envelope code 3001** | 符合 Envelope Pattern |
| D-09 | 廠商方向 | ⏸ **合併 D-02** | — |

完整紀錄：[`docs/01_leader/decisions/20260423_decision-wave2-wave3-9items.md`](../decisions/20260423_decision-wave2-wave3-9items.md)

---

## 4. 本波產出（19 份新文件 + 7 commit）

### A. Sprint 0 升級（Linus）

| 項目 | 變化 | 結果 |
|------|------|------|
| 後端 logback | 1.5.18 → 1.5.32 | ✅ CVE-2024-50379 修補（High），21 tests PASS |
| 前端 axios | 1.7.9 → 1.15.2 | ✅ follow-redirects CVE 修補（Moderate），106 tests PASS |
| 升級報告 | `docs/05_development/library/20260423_sprint0_upgrade_report.md` | — |

### B. Patricia PRD v1.0（從草稿轉正式）

13 處改動，重點：
- §0.1 新增「決策對齊紀錄」
- §3.2 Out-of-Scope：明確排除 LINE Notify + SMS
- §4.2 F-W3-02：Watchlist 強制登入（移除訪客模式）
- §4.2 F-W3-03：完整改寫推播驗收條件（VAPID + AWS SES + 降級）
- §4.2 F-W3-04：Spring Security envelope code 3001
- §13 新增「Peter 接手指引」

### C. Peter Wave 3 規格（4 檔，17 endpoints）

| 檔案 | 內容 |
|------|------|
| `20260423_wave3_SRS.md` | 系統需求規格 |
| `20260423_wave3_api-spec.md` | 17 個 endpoint（含 OpenAPI 片段） |
| `20260423_wave3_ui-spec.md` | UI 規格 + 主要流程圖 |
| `20260423_wave3_schema-lock.md` | Wave 3 schema-lock（單一事實源） |

API 分布：搜尋 2、Watchlist 3、警示 6、Security 2、搜尋歷史 3、認證 1。

新增錯誤碼：3001 / 3002 / 3003 / 4011 / 4012 / 4013 / 5021 / 5022。

### D. Sophia Wave 3 系統架構（4 檔）

| 檔案 | 重點 |
|------|------|
| `20260423_wave3_system-architecture.md` | C4 圖、月成本 $511、NFR、風險 |
| `20260423_wave3_spring-security-integration.md` | JWT、AuthenticationEntryPoint、Wave 2 零衝擊 |
| `20260423_wave3_blue-green-deployment.md` | 5 個切換腳本、Schema 三階段策略 |
| `20260423_pentest_rfp_draft.md` | OWASP ASVS L2、預算 50 萬 |

**關鍵保證**：Wave 2 既有 5 個 API（quote/get、quote/list、quote/history、fundamental/get、chip/get）零衝擊，加入 SecurityConfig `permitAll()` 白名單即可。

### E. Preston Wave 3 專案架構（4 檔）

| 檔案 | 重點 |
|------|------|
| `20260423_wave3_project-architecture.md` | Mermaid 模組依賴圖、跨模組時序 |
| `20260423_wave3_module-breakdown.md` | 5 模組套件樹 + ArchUnit 規則 |
| `20260423_wave3_er-diagram.md` | 9 張新表 DDL + 25 條索引 |
| `20260423_wave3_flyway-migration-plan.md` | 10 個 V3.x.x migration |

**模組調整**（與 PRD 提示不同）：
- 不新建 `stock-auth` → 擴充 `stock-member`（Wave 1 已有 JWT/Login）
- 不新建 `stock-notification` → 落地 `stock-notify`（Wave 1 placeholder）
- 新增模組 2 個：`stock-search`、`stock-alert`

新增表：`stock_info`、`refresh_token`、`watchlist`、`price_alert`、`alert_trigger_log`、`search_history`、`hot_search`、`push_subscription`、`notification_log`。

### F. Rule 檔改寫（Jamie）

`oracle-database.md` → `relational-database.md`，內容從 Oracle 19c 全面改寫為 PostgreSQL 16：
- 命名 snake_case 全小寫（PG case-folding）
- 型別對照表：VARCHAR / NUMERIC / TIMESTAMP / JSONB
- 索引：補 GIN / pg_trgm（搜尋必備）
- 分頁：LIMIT/OFFSET + keyset pagination
- Migration：補 Blue-Green 相容策略

連動更新：CLAUDE.md、code-style.md、project-architecture.md、preston.md、.gitignore。

---

## 5. Commit History（本波）

```
99341be  docs(arch): Wave 3 project architecture — Preston 4 files
177c508  docs(arch): Wave 3 system architecture — Sophia 4 files
7d88f1a  docs(spec): Wave 3 spec bundle — Peter 4 files (17 endpoints)
d14803c  docs(product): Wave 3 PRD v1.0 promoted from draft to official
5a12179  build(deps): Sprint 0 dependency upgrades — fix 2 CVEs
1a3e16f  docs(rules): rewrite relational-database.md for PostgreSQL 16
185f6fc  docs(decisions): Wave 2 + Wave 3 9 items batch decisions
```

7 個 commit，邏輯獨立，已 push 到 `origin/develop`。

---

## 6. 等使用者拍板（合併新舊）

### 合約類（D-02 / D-09）— 必須親簽

- ☐ 滲透測試廠商選定（建議金融 FinTech 經驗，預算 50 萬，OWASP ASVS L2）
  - 廠商 RFP 草稿已產出：`docs/04_architecture/system/20260423_pentest_rfp_draft.md`
  - **時程壓力**：W0 結束前必須簽約，否則壓縮 W6 滲透週

### Sophia 提出的 5 項待確認

1. ☐ **Q1**：web-push library CVE 評估（建議 `nl.martijndwars:web-push 5.1.1`，待 Linus 在 W0 確認）
2. ☐ **Q2**：VAPID 金鑰儲存（建議 AWS Secrets Manager + 每年輪替）
3. ☐ **Q3**：AWS SES Production Access 是否已開通（W2 前必須完成）
4. ☐ **Q4**：Spring Security 主配置位置（建議放 `stock-boot` 避免反向依賴）
5. ☐ **Q5**：滲透 RFP 是否需法務先 Review 再發

### Preston 提出的 9 項待 Sophia 配合

1. ☐ RDS pg_trgm extension 權限（中文搜尋必需）
2. ☐ `@Scheduled` 多 Pod 防重（暫單 Pod / 待 ShedLock）
3. ☐ VAPID key 儲存（同 Sophia Q2）
4. ☐ Web Push lib 選型（同 Sophia Q1）
5. ☐ AWS SES production access（同 Sophia Q3）
6. ☐ Spring Security 主配置位置（同 Sophia Q4）
7. ☐ `notification_log` 歸檔策略（90 天搬 S3？延 Wave 4？）
8. ☐ `stock_info` 主檔資料來源 SLA 與排程頻率
9. ☐ 熱門搜尋 Redis ZSET 加速（Wave 3 / Wave 4）

### Preston 提出的 4 項架構投票議題（4 票機制）

- ☐ Q1：不新建 stock-auth（採擴充 stock-member）
- ☐ Q2：模組劃分（stock-search、stock-alert、stock-watchlist、stock-notify、stock-member 擴充）
- ☐ Q3：暫不導入 ShedLock（單 Pod 跑排程）
- ☐ Q4：pg_trgm 中文搜尋（vs Elasticsearch）

> **建議**：Sophia + Preston 衝突點 0，可一次性合併確認 9 項；4 票投票議題另開（Brian + Fiona 須加入）。

### Jenkins credentials（DevOps）

- ☐ `docker-credentials`（推 image 用）
- ☐ `kubeconfig-dev`（dev cluster）
- ☐ `dev-api-base-url`（smoke test）

---

## 7. 風險矩陣（Wave 3）

| 風險 | 影響 | 機率 | 緩解 |
|------|------|------|------|
| 廠商簽約延誤 | High | Medium | W0 提早簽 + Sophia 草擬 RFP（已備） |
| Spring Security 衝擊 Wave 2 API | High | Low | Sophia 已驗證 5 個 endpoint 零衝擊 |
| pg_trgm 中文搜尋效能不足 | Medium | Low | 預備 fallback 為 LIKE，效能不足升 Elasticsearch |
| Web Push iOS Safari 體驗差 | Low | High | Email 後援 + UX 文案 |
| AWS SES 沙盒限制 | Medium | Medium | W2 前 production access 申請 |
| logback 升級回歸 | Low | Low | ✅ 已驗證 21 tests PASS |

---

## 8. 6 週時程更新

| 週次 | 範圍 | 狀態 |
|------|------|------|
| W0（本週剩餘） | ✅ Sprint 0 升級、PRD 正式、SRS、架構 | **超前完成** |
| W0 剩餘 | 廠商簽約、Jenkins credentials、Sophia/Preston 9 項確認 | ⏳ 等使用者 |
| W1 | Felix / Bruno 開工：Spring Security 整合、stock_info 主檔匯入 | 待啟動 |
| W2-W3 | F-W3-01（搜尋）+ F-W3-04（Spring Security）並行 | — |
| W3-W4 | F-W3-02（Watchlist）+ F-W3-05（搜尋歷史） | — |
| W4-W5 | F-W3-03（Price Alert）+ Web Push + SES 整合 | — |
| W5 | Code Review + QA | — |
| W6 | 滲透測試 + 修補 + Release | — |

---

## 9. 下一步

### 立即（等使用者）

1. **使用者一次回覆 14 項**（廠商 1 + Sophia 5 + Preston 9 + Jenkins 3 = 18 項，但 Sophia / Preston 重疊 4 項，實際 14 項）
2. 使用者可以選擇：
   - **A**：全採用 Sophia / Preston 建議方向 → Jamie 自動推進（Auto Mode）
   - **B**：逐項確認（避免後續架構變動）
   - **C**：先回廠商 + Jenkins，技術細節由 Jamie 拍板

### 短期（W0-W1）

3. 廠商簽約 + RFP 法務 Review
4. Felix / Bruno W1 開工 kick-off
5. dev 環境 Wave 2 部署測試

---

**Wave 3 Sprint 0 完成，待使用者拍板 W0 剩餘項目即可進入 W1 開發。**
