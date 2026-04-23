# 決策紀錄：Wave 3 W1 啟動 14 項一次拍板

- **編號**：D-2026-04-23-W1-KICKOFF
- **日期**：2026-04-23
- **拍板人**：Jamie（Auto Mode 路徑 A，使用者授權「全採 Sophia/Preston 建議方向」）
- **影響範圍**：W1 開發啟動、廠商簽約、雲端帳號設定
- **上層脈絡**：[D-2026-04-23-09items](./20260423_decision-wave2-wave3-9items.md) 拍板後 Sophia/Preston 列出 14 項細節需確認

---

## 0. 拍板原則

使用者於 Sprint 0 收尾報告後選擇 **A 路徑**：
> 「全採 Sophia / Preston 建議方向 → Jamie 自動推進 W1 啟動」

依此原則：
- **技術細節**（架構、套件、配置）→ Jamie 直接拍板採 Sophia/Preston 建議
- **合約 / 憑證 / 雲端帳號** → 仍需使用者親自處理（Jamie 無權限）

---

## 1. Sophia 提出的 5 項（Q1-Q5）

### Q1 — Web Push library 選型

**建議**：`nl.martijndwars:web-push:5.1.1`

**理由**：
- 維護活躍（last release 2024-08）
- 支援 VAPID 標準 RFC 8292
- 無 critical CVE（Linus 已掃，2026-04-23 報告）
- Spring Boot 整合範例完整

**拍板**：✅ 採用

**後續任務**：Linus 加入 `backend/stock-notify/pom.xml`，Bruno 整合至 `PushNotificationService`

---

### Q2 — VAPID 金鑰存放位置

**建議**：AWS Secrets Manager（生產）+ application-local.yml 明碼（local）

**理由**：
- 符合 [security-owasp.md A02 加密失效](../../.claude/rules/security-owasp.md) 規範
- 與 D-2026-04-22-D5 機密管理決策一致（Secrets Manager 統一收口）
- local 明碼僅限工程師本機（依 [environment.md local 環境配置](../../.claude/rules/environment.md)）

**金鑰結構**：
```
secret name: prod/stock-platform/vapid
{
  "publicKey": "BNc...",
  "privateKey": "abc...",
  "subject": "mailto:ops@stock-platform.example.com"
}
```

**拍板**：✅ 採用

**後續任務**：Sophia 撰寫 `terraform/modules/secrets/vapid.tf`（Wave 3 W4 進場）

---

### Q3 — AWS SES Production Access 申請

**建議**：W1 內由 AWS 帳號擁有者提交申請（Sandbox → Production，AWS Support Case，需 24-48h 審核）

**理由**：
- W4 通知模組需發信，預留 3 週 buffer 避免 W4 卡關
- 申請文件 Sophia 已草擬（含預估每月發信量、退信處理流程、Bounce/Complaint Webhook）

**拍板**：⏸ **等使用者**（Jamie 無法代申請，需 AWS root 帳號）

**Jamie 動作**：
1. 已將申請文件草稿放至 `docs/04_architecture/system/20260423_aws_ses_production_request_draft.md`（待 Sophia 補完）
2. 提醒使用者於 W1 W2 內提交，避免 W4 阻擋

---

### Q4 — Spring Security 主配置位置

**建議**：放在 `backend/stock-boot/src/main/java/.../config/SecurityConfig.java`

**理由**：
- `stock-boot` 是 Application 啟動模組，符合 [project-architecture.md](../../.claude/skills/project-architecture.md) §1.3「config 模組可依賴各模組」原則
- 與既有 `WebMvcConfig` 同層，符合慣例
- 避免在 `stock-member` 內配置（防 Wave 2 模組污染）

**拍板**：✅ 採用

**後續任務**：Bruno W1 開工點

---

### Q5 — Pentest RFP 法務複核

**建議**：使用者交付公司法務 / 採購團隊複核 RFP（`docs/04_architecture/system/20260423_pentest_rfp_draft.md`）

**理由**：
- NT$500K 預算超過一般採購授權門檻
- RFP 內含 NDA、責任歸屬、資料外洩賠償條款，需法務審核
- W6 滲透測試前 4 週需發 RFP 給 3 家廠商比價

**拍板**：⏸ **等使用者**（法務複核外部依賴）

**Jamie 動作**：W2 W1 報表會主動提醒進度

---

## 2. Preston 提出的 9 項（含 4 項投票）

### P1 — 不新建 stock-auth，改擴展 stock-member ⚖️ **投票**

**建議**：不新建 `stock-auth` 模組，改在 `stock-member` 增加 `auth/` 子套件

**理由**：
- Wave 1 `stock-member` 已含 user_info、login endpoint
- 新建 stock-auth 會造成 Wave 1 既有功能跨模組依賴，違反 [project-architecture.md §1.4 依賴規則](../../.claude/skills/project-architecture.md)
- Spring Security 配置一處（stock-boot），認證業務邏輯內聚於 stock-member

**反對方案**：另設 stock-auth 純認證模組（Sophia 原始建議）

**4 票投票**（Sophia / Preston / Brian / Fiona）：
- Sophia：✅ 同意（撤回原建議，Preston 觀點正確）
- Preston：✅ 提案者
- Brian：✅ 同意（避免 Wave 1 大改）
- Fiona：⏸ 棄權（前端不影響）

**結果**：3:0 通過（Auto Mode 採 Preston 提案）

**拍板**：✅ 採用

---

### P2 — Wave 3 模組劃分 ⚖️ **投票**

**建議**：
- 新建 `stock-search`（搜尋）
- 新建 `stock-alert`（價格提醒）
- 擴展 `stock-watchlist`（自選股，Wave 1 已有 placeholder）
- 擴展 `stock-notify`（推播 + email，Wave 1 已有 placeholder）
- 擴展 `stock-member`（加 auth/）
- **不**新建 stock-auth

**4 票投票**：
- Sophia：✅
- Preston：✅
- Brian：✅
- Fiona：✅

**結果**：4:0 全票通過

**拍板**：✅ 採用

**後續任務**：Bruno 依此建立 Maven module

---

### P3 — 暫不引入 ShedLock（@Scheduled 多 Pod 去重）⚖️ **投票**

**建議**：W1-W4 採單一 Pod 跑排程 + Feature Flag；W5 觀察是否需 ShedLock

**理由**：
- Wave 3 只有 2 個排程任務（hot_search 重算、notification_log 歸檔）
- 引入 ShedLock 需新增 Redis / DB lock table，複雜度提升
- 單 Pod 失敗 = 排程跳過一次（可接受），不影響核心功能
- Wave 4+ 若排程數量 >5 再評估

**反對方案**：W1 直接引入 ShedLock（Sophia 原始建議，避免後續加裝）

**4 票投票**：
- Sophia：⚠️ 同意但保留（記錄到技術債）
- Preston：✅ 提案者
- Brian：✅ 同意（YAGNI）
- Fiona：⏸ 棄權

**結果**：2:0:1 通過（Sophia 條件同意）

**拍板**：✅ 採用 + 加入技術債清單 `TD-W3-001-ShedLock-evaluation`

---

### P4 — pg_trgm vs Elasticsearch ⚖️ **投票**

**建議**：Wave 3 用 pg_trgm；Wave 5+ 若需中文斷詞 / 同義詞 / 全文索引才考慮 Elasticsearch

**理由**：
- Wave 3 搜尋功能僅「股名 / 股號模糊比對」（max 4000 檔股票）
- pg_trgm + GIN index 在 4000 筆 LIKE 查詢 < 50ms（Sophia POC 驗證）
- 引入 Elasticsearch 需額外 EC2 / OpenSearch Service，月費 +$300（D-2026-04-22-D5 成本控制違反）

**反對方案**：W1 直接上 OpenSearch（Sophia 原始建議，未來不用搬遷）

**4 票投票**：
- Sophia：✅ 同意（撤回原建議）
- Preston：✅ 提案者
- Brian：✅
- Fiona：⏸ 棄權

**結果**：3:0 通過

**拍板**：✅ 採用 + 技術債 `TD-W3-002-Elasticsearch-evaluation-Wave5`

---

### P5 — pg_trgm extension 安裝權限

**建議**：DBA 在 dev / prod 預先 `CREATE EXTENSION pg_trgm;`，應用啟動不執行

**理由**：
- pg_trgm 需 superuser 權限，應用層 DB user 無此權限（依 [relational-database.md](../../.claude/rules/relational-database.md)）
- Flyway migration 不應依賴 superuser

**拍板**：✅ 採用

**後續任務**：Sophia 寫 DBA 申請單模板，使用者 W1 內請 DBA 執行

---

### P6 — @Scheduled 多 Pod 去重

詳見 P3（已併入投票）

---

### P7 — notification_log 歸檔策略

**建議**：90 天熱資料留 PostgreSQL，超過 90 天歸檔 S3 Glacier，DB 刪除

**理由**：
- 通知歷史單筆小（< 1KB），但月增量約 50 萬筆，1 年累積 600 萬筆會影響查詢
- 90 天 dashboard 顯示需求 + 1 年法遵保存（依公司資安政策）
- S3 Glacier 月費 < $1/100GB

**拍板**：✅ 採用

**後續任務**：Preston 補入 ER Diagram 註記，Bruno 寫排程 job（Wave 3 W4）

---

### P8 — stock_info 主檔 SLA

**建議**：T+1 凌晨 02:00（GMT+8）由排程從 TWSE / TPEx 開放資料更新；失敗 retry 3 次後告警

**理由**：
- 上市櫃股票每日有新增 / 下市，主檔需保新
- 凌晨 02:00 避開盤後資料整理時段（00:00-01:30）
- TWSE OpenAPI 不收費（已驗證）

**拍板**：✅ 採用

**後續任務**：Bruno W1 寫 stock_info 主檔匯入 + 排程

---

### P9 — Hot Search Redis ZSET 結構

**建議**：
```
ZSET key: hot_search:{date YYYYMMDD}
score: 搜尋次數
member: stock_id
TTL: 7 天
```

**理由**：
- ZSET 原生支援排行榜（ZREVRANGE 0 9 取 top 10）
- 按日切 key 避免單 key 過大
- 7 天 TTL 自動清理

**拍板**：✅ 採用

**後續任務**：Bruno 整合至 `SearchService`

---

## 3. Jenkins 3 個 Credentials ⏸ **等使用者**

| Credential ID | 用途 | 由誰提供 |
|---------------|------|----------|
| `docker-credentials` | Docker Hub / ECR push | DevOps |
| `kubeconfig-dev` | dev EKS cluster 認證 | DevOps / AWS root |
| `dev-api-base-url` | 前端 build-time 環境變數 | Sophia 已決定為 `https://dev-api.stock-platform.example.com` |

**Jamie 動作**：
- `dev-api-base-url` 已可填入 Jenkins（Sophia 已決定 URL）
- `docker-credentials` / `kubeconfig-dev` 等使用者提供
- 未提供前 W1 開發不阻擋（CI 仍可跑 build + test，僅 deploy 卡）

---

## 4. 滲透測試廠商選定 ⏸ **等使用者**

詳見 D-W3-W1-Q3 / Q5（合約 + 法務雙阻擋）

**Jamie 動作**：
- 已準備 RFP 草稿
- 主動於每週進度報告追蹤狀態
- W4 內若無廠商選定，自動降級為 W7 完成（不阻擋 release）

---

## 5. W1 開工授權

依本拍板，**W1 立即可開工項目**：

| 工程師 | 任務 | 阻擋 |
|--------|------|------|
| **Bruno** | Spring Security 整合（stock-boot SecurityConfig）+ stock_info 主檔匯入 | 無 |
| **Felix** | Watchlist 頁面骨架（依 ui-spec.md） | 無 |
| **Linus** | 新增 web-push 5.1.1 至 stock-notify pom | 無 |
| **Sophia** | DBA pg_trgm 申請單 + AWS SES 申請文件補完 | 無（產文件） |
| **Preston** | 補 notification_log 歸檔欄位至 ER Diagram | 無 |

**等使用者解阻塞**才能進入的項目（不影響 W1）：
- AWS SES Production（W4 才需要）
- Pentest 廠商（W6 才需要）
- Jenkins kubeconfig-dev（W1 結束時 deploy 才需要）

---

## 6. 技術債紀錄

新增 2 筆：

| 編號 | 內容 | 觸發條件 | 預計處理 |
|------|------|----------|----------|
| TD-W3-001 | ShedLock 評估 | 排程任務 >5 個 | Wave 4 評估 |
| TD-W3-002 | Elasticsearch 評估 | 中文斷詞 / 全文檢索需求 | Wave 5 評估 |

---

## 7. 後續產文

- [ ] Linus：更新 `docs/05_development/library/` web-push 採用紀錄
- [ ] Sophia：DBA pg_trgm 申請單、AWS SES 申請文件補完
- [ ] Bruno：W1 開發筆記
- [ ] Felix：W1 開發筆記
- [ ] Daisy：本拍板紀錄歸檔索引
