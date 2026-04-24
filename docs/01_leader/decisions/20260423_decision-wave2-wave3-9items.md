# 決策紀錄：Wave 2 殘留 + Wave 3 啟動（9 項合併）

> **決策日期**：2026-04-23
> **決策人**：Jamie（Leader，Auto Mode 拍板）
> **決策範圍**：Wave 2 v0.2.0 收尾 + Wave 3 v0.3.0 啟動
> **狀態**：✅ 7 項已拍板 / ⏸ 2 項保留（合約類）

---

## 1. 背景

Wave 2 v0.2.0 程式已完成、本機可啟動，但 dev 部署與 Wave 3 啟動共有 9 項待決策事項。為避免來回，使用者授權一次拍板。Auto Mode 下採用 Patricia / Sophia 已建議方向當預設，**僅滲透測試廠商選定（合約類）保留給使用者親簽**。

---

## 2. 決策清單

### Wave 2 殘留決策（Sophia 提出）

#### D-2026-04-23-01：Wave 2 部署形態 — Cloud Only

| 項目 | 內容 |
|------|------|
| **問題** | Wave 2 是 Cloud Only 還是 Hybrid（雲 + 地端）？ |
| **方案 A** | Cloud Only（純 AWS） |
| **方案 B** | Hybrid（AWS + 地端） |
| **拍板** | ✅ **方案 A：Cloud Only** |
| **理由** | 1. Wave 2 沒有合規限制需要地端<br>2. Hybrid 增加 Direct Connect / VPN 成本與運維複雜度<br>3. 縮短 v0.2.0 上線時程<br>4. Hybrid 評估延至 v1.5（屆時若客戶要求再啟動） |
| **影響** | Sophia 系統架構文件已寫 Cloud Only，無需改動。<br>地端規格從 v0.2.0 部署清單移除。 |

#### D-2026-04-23-02：滲透測試廠商選定 — ⏸ 保留

| 項目 | 內容 |
|------|------|
| **問題** | 選哪一家滲透測試廠商？預算 30-80 萬。 |
| **拍板** | ⏸ **保留給使用者親簽** |
| **理由** | 合約決策不適合 Auto Mode 自動代決，使用者須評估廠商資歷、報價、檔期。 |
| **建議方向** | 1. 偏好金融產業經驗（FinTech / 證券）<br>2. 提供 OWASP ASVS Level 2 報告<br>3. 涵蓋 OWASP Top 10 + API Security Top 10<br>4. 提供修補建議與 retest 包套<br>5. 預算建議中段：50 萬左右 |
| **下一步** | W0 內由使用者拍板 → Sophia 草擬 RFP → 簽約 |
| **時程影響** | 廠商簽約延誤會壓縮 W6 滲透週，須及早處理。 |

#### D-2026-04-23-03：prod 部署策略 — Blue-Green

| 項目 | 內容 |
|------|------|
| **問題** | prod 環境用 Canary 還是 Blue-Green？ |
| **方案 A** | Canary（5% → 25% → 100% 漸進） |
| **方案 B** | Blue-Green（新版獨立環境，DNS 切換） |
| **拍板** | ✅ **方案 B：Blue-Green** |
| **理由** | 1. 金融類應用偏好確定性，Blue-Green 切換瞬間生效<br>2. 回滾簡單（DNS 切回舊環境）<br>3. Wave 3 流量規模未到需要 Canary 細分的程度<br>4. Canary 需要 service mesh / API gateway 流量切分能力，增加架構成本<br>5. v1.x 後若流量上升再升級為 Canary |
| **影響** | Sophia 部署架構文件加入 Blue-Green 切換腳本（ALB target group swap）。 |

#### D-2026-04-23-04：oracle-database.md → relational-database.md rename

| 項目 | 內容 |
|------|------|
| **問題** | 既然 Wave 2 已換 PostgreSQL 16，rule 檔名是否要 rename？ |
| **拍板** | ✅ **執行 rename** |
| **檔案異動** | `.claude/rules/oracle-database.md` → `.claude/rules/relational-database.md` |
| **內容調整** | 1. 標題改為「關聯式資料庫規範（PostgreSQL 16）」<br>2. Oracle 專屬語法（如 `DUAL`、`SYSTIMESTAMP`、`ROWNUM`）改為 PostgreSQL 對應<br>3. 連線池配置範例對應 PostgreSQL（HikariCP + pg JDBC）<br>4. CLAUDE.md 引用同步更新 |
| **理由** | 避免新進團隊成員誤以為仍用 Oracle，文件與實作要一致。 |

---

### Wave 3 啟動決策（Patricia 提出）

#### D-2026-04-23-05：D1 廣度 vs 深度 — 廣度（5 功能）

| 項目 | 內容 |
|------|------|
| **問題** | Wave 3 是廣度（5 功能）還是深度（強化 1 功能）？ |
| **拍板** | ✅ **廣度（5 功能）** |
| **理由** | 1. Wave 2 已交付 1 個深度功能（StockDetail），Wave 3 需擴大產品面<br>2. 5 功能組合（搜尋 + 自選股 + 警示 + 歷史 + 安全）形成完整使用流程<br>3. 強化單一功能無法產生回訪鉤子<br>4. Patricia 已評估工時 24-33 day 在 6 週內可達 |
| **取捨** | 各功能深度受限，後續 v0.4.0 再強化（如自選股多群組、技術指標疊加）。 |

#### D-2026-04-23-06：D2 Watchlist 登入策略 — 強制登入

| 項目 | 內容 |
|------|------|
| **問題** | 自選股要求登入還是訪客可用 localStorage？ |
| **方案 A** | 訪客可用 localStorage |
| **方案 B** | 強制登入 |
| **拍板** | ✅ **方案 B：強制登入** |
| **理由** | 1. 強化 MAU（月活躍使用者）為核心指標<br>2. 跨裝置同步是自選股核心價值（手機、平板、桌機）<br>3. 後續價格警示功能必須有 user identity<br>4. localStorage 上限與清除風險高<br>5. 配合 F-W3-04 Spring Security 同步上線 |
| **取捨** | 訪客流失率可能上升，需在「未登入點擊自選星」時提供「登入即享跨裝置同步」引導文案。 |

#### D-2026-04-23-07：D3 推播管道 — Web Push + Email

| 項目 | 內容 |
|------|------|
| **問題** | 價格警示用什麼推播管道？ |
| **方案候選** | LINE Notify、Web Push、Email、SMS |
| **拍板** | ✅ **Web Push + Email** |
| **理由** | 1. **LINE Notify 已 EOL**（2025-03-31 終止），不可採用<br>2. Web Push 免費、跨平台、即時性高<br>3. Email 後援 Safari iOS Web Push 體驗差的情境<br>4. SMS 成本高（每則 0.5-1 元），Wave 3 暫不導入<br>5. LINE Messaging API（付費）延至 v1.0 評估 |
| **影響** | Bruno 後端整合 Web Push（VAPID）+ AWS SES。<br>Felix 前端整合 Notification API + Service Worker。 |

#### D-2026-04-23-08：D4 未授權回應 — HTTP 200 + Envelope code 3001

| 項目 | 內容 |
|------|------|
| **問題** | 未授權 / 未登入用 HTTP 401 還是 HTTP 200 + code 3001？ |
| **方案 A** | HTTP 401 |
| **方案 B** | HTTP 200 + Envelope code 3001 |
| **拍板** | ✅ **方案 B：Envelope code 3001** |
| **理由** | 1. 符合既有 API 設計規範（`api-design.md` Envelope Pattern）<br>2. 統一前端錯誤處理（一律從 envelope.code 判斷）<br>3. 避免某些 proxy / firewall 對 401 特殊處理<br>4. Spring Security 自訂 AuthenticationEntryPoint 回 200 + code 3001<br>5. 與 OWASP A07 認證失效規範一致 |
| **影響** | Bruno 實作 Spring Security 時須客製 EntryPoint。<br>Felix 在 axios interceptor 偵測到 code=3001 時導向登入頁。 |

#### D-2026-04-23-09：D5 滲透測試廠商方向（合併 D-2026-04-23-02）

| 項目 | 內容 |
|------|------|
| **拍板** | ⏸ **保留**（同 D-2026-04-23-02） |

---

## 3. 投票紀錄

本決策採 **Auto Mode Leader 拍板**，未啟動 4 票投票機制（決策皆為產品 / 部署策略，非架構技術衝突）。

| 決策 | 是否需架構 4 票投票 | 理由 |
|------|------------------|------|
| D-01 Cloud Only | ❌ 不需 | Sophia 已建議 |
| D-02 滲透廠商 | ⏸ 保留 | 合約決策非架構衝突 |
| D-03 Blue-Green | ❌ 不需 | Sophia 部署策略選擇 |
| D-04 rename rule | ❌ 不需 | 文件命名一致性 |
| D-05 廣度 | ❌ 不需 | Patricia 產品策略 |
| D-06 強制登入 | ❌ 不需 | Patricia 產品策略 |
| D-07 推播管道 | ❌ 不需 | Patricia + Sophia 已對齊 |
| D-08 Envelope 3001 | ❌ 不需 | 符合既有 api-design.md 規範 |
| D-09 廠商方向 | ⏸ 合併 D-02 | — |

> 後續若 Sophia / Preston / Brian / Fiona 對任一拍板有異議，可發起架構決策投票推翻。

---

## 4. 拍板後即觸發的後續任務

| # | 任務 | 負責人 | 預計時程 |
|---|------|--------|----------|
| 1 | PRD 從 🟡 草稿轉 ✅ 正式 | Patricia | 0.5h |
| 2 | Sprint 0 必升項目（Spring Boot patch + logback + axios + vitest + plugin） | Linus | 2-3h |
| 3 | Wave 3 SRS + API Spec | Peter | 2-3 day |
| 4 | Wave 3 系統架構（含 Spring Security 整合） | Sophia | 2 day |
| 5 | Wave 3 專案架構（含 Watchlist / Search 模組劃分） | Preston | 2 day |
| 6 | rule 檔 rename（oracle-database.md → relational-database.md） | Jamie | 0.5h |
| 7 | Sophia 滲透測試 RFP 草稿 | Sophia | 1 day |
| 8 | dev 環境 Wave 2 部署（待 Jenkins credentials） | Jamie | 等使用者提供 credentials |

---

## 5. 後續可能變動點

- **D-02 / D-09 廠商選定** 必須在 W0 結束前完成，否則壓縮 W6 滲透週
- **D-06 強制登入** 若實際 MAU 數據顯示流失過大，v0.4.0 評估訪客模式回退
- **D-07 推播管道** Safari iOS Web Push 體驗會持續觀察，必要時補 Email 強制
- **D-04 rename** 若後續引入 NoSQL（DynamoDB、Redis 結構化），須再拆 rule 檔

---

**決策已生效，後續團隊產出文件 / 程式碼依本決策執行。**
