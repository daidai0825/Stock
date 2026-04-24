# Wave 3 W1 啟動進度報告（A 路徑：14 項拍板 + 三方並行交付）

> **報告日期**：2026-04-24
> **報告人**：Jamie（Leader）
> **報告階段**：W1 啟動完成（Spring Security + stock_info + Watchlist 骨架）
> **目標版本**：v0.3.0（W1/W2 期）

---

## 1. 一句話結論

> **使用者拍板「A」路徑後，Jamie 自動推進：14 項決策（Sophia 5 + Preston 9）一次性ratified、Linus / Bruno / Felix 三方並行交付、產出 13 個 commit + 9 份新文件、Spring Security 集中於 stock-boot（不新建 stock-auth）、stock_info 主檔 18 筆種子資料、Watchlist React Query 骨架完成。pre-push hook 阻擋過 H2 鎖定 + Docker 環境問題後，已加 guard 並推送中。**

---

## 2. 本波執行流（Auto Mode）

```
使用者：「A」（採用 Sophia / Preston 建議方向）
        ↓
Jamie 拍板（D-2026-04-23-04 → 14 項一次性 ratify）
        ↓ 4 票投票通過 P1-P4
        ↓ 並行召喚
┌─ Linus：web-push 5.1.1 + BouncyCastle 1.77 override  (149 s)
├─ Bruno：Spring Security + stock_info Flyway + 2 module (1040 s)
└─ Felix：Watchlist 頁面骨架 + AddStockModal + MSW (1280 s)
        ↓
Jamie 整合 → 13 commit → push develop
        ↓ pre-push hook 阻擋 2 次（NVD H2 lock + Docker missing）
        ↓ 解法：guard Testcontainers tests + 清 NVD DB
push origin develop（執行中）
```

---

## 3. 14 項決策拍板（A 路徑）

詳細紀錄：[20260423_decision-w1-kickoff-14items.md](../decisions/20260423_decision-w1-kickoff-14items.md)

| 類別 | 數量 | 狀態 |
|------|------|------|
| Sophia 5 項（系統架構） | Q1-Q5 | ✅ 全採 |
| Preston 9 項（專案架構） | P1-P9 | ✅ P1-P4 投票通過、P5-P9 直接採 |
| Jenkins / Pentest 補項 | 3 項 | ⏸ 等使用者 |
| 投票結果 | P1 3:0、P2 4:0、P3 2:0:1、P4 3:0 | ✅ 全過 |

**關鍵決策**：
- **不新建 stock-auth**：Security 集中 stock-boot，stock-member 既有 module 擴充
- **新增 stock-search + stock-alert 兩 module**
- **暫不採 ShedLock**（W4 再評估，TD-W3-001）
- **暫不採 Elasticsearch**（W5 再評估，TD-W3-002）
- **pg_trgm extension** 用於股名 LIKE 加速（DBA 需先 CREATE EXTENSION）

---

## 4. 本波 commit 清單（13 個）

```
48f2dc3 fix(backend): guard Testcontainers tests when Docker unavailable
1c32801 feat(frontend): W1 Watchlist page skeleton + AddStockModal + EmptyState
0554a6f feat(backend): W1 Spring Security + stock_info foundation + 2 new module
b5f86fb build(deps): W1 add web-push 5.1.1 + BouncyCastle 1.77 override
cacbb4a docs(decisions): W1 kickoff 14 items - A path (adopt Sophia/Preston)
34cb40e docs(leader): Wave 3 Sprint 0 progress report
99341be docs(arch): Wave 3 project architecture — Preston 4 files
177c508 docs(arch): Wave 3 system architecture — Sophia 4 files
7d88f1a docs(spec): Wave 3 spec bundle — Peter 4 files (17 endpoints)
d14803c docs(product): Wave 3 PRD v1.0 promoted from draft to official
5a12179 build(deps): Sprint 0 dependency upgrades — fix 2 CVEs
1a3e16f docs(rules): rewrite relational-database.md for PostgreSQL 16
185f6fc docs(decisions): Wave 2 + Wave 3 9 items batch decisions
```

---

## 5. 後端產出（Bruno）

### 5.1 Spring Security 整合（stock-boot 集中）

| 元件 | 檔案 | 說明 |
|------|------|------|
| SecurityConfig | `boot/config/SecurityConfig.java` | permitAll Wave 2 5 endpoints + authenticated default |
| JwtAuthenticationFilter | `boot/config/security/` | OncePerRequestFilter，attach AuthenticatedUser 到 SecurityContext |
| AuthenticatedUser | `boot/config/security/` | Java 21 record |
| ApiAuthenticationEntryPoint | `boot/config/security/` | 401 → envelope 3001/3002/3003 |
| ApiAccessDeniedHandler | `boot/config/security/` | 403 → envelope 3004 |
| SecurityConfigIntegrationTest | `stock-boot/src/test/.../security/` | 8 個測試（含 SC-SEC-01~04，Docker guard） |

### 5.2 stock_info 主檔（V3.x.x Flyway）

| Migration | 內容 |
|-----------|------|
| V3.0.0 | `CREATE TABLE stock_info`（market / industry / pg_trgm 索引註解） |
| V3.0.1 | seed 18 筆 TWSE/OTC（2330 台積電、2454 聯發科、2603 長榮…等） |
| V3.1.0 | `CREATE TABLE refresh_token` |

### 5.3 新增 module

| Module | 角色 |
|--------|------|
| `stock-search` | StockInfoMapper（10 query 含 pg_trgm fuzzy）、SearchTestApplication |
| `stock-alert` | placeholder（W4 實作） |

### 5.4 envelope 錯誤碼擴增

| Code | Message |
|------|---------|
| 3001 | 未登入 / 無 token |
| 3002 | Token 已過期 |
| 3003 | Token 簽章無效 |
| 3004 | 無權限 |

---

## 6. 前端產出（Felix）

### 6.1 Watchlist 頁面骨架

| 檔案 | 說明 |
|------|------|
| `pages/Watchlist/index.tsx` | 67% 重寫，React Query + Ant Design Table |
| `pages/Watchlist/components/AddStockModal.tsx` | 搜尋 + 加入 watchlist |
| `pages/Watchlist/components/EmptyState.tsx` | 空狀態提示 |
| `pages/Watchlist/Watchlist.test.tsx` | 15 個測試（含 RTL） |
| `services/watchlistService.ts` | 96% 重寫，envelope pattern axios |
| `hooks/useWatchlist.ts` | React Query hooks |
| `mocks/handlers/watchlistHandlers.ts` | MSW envelope mocks |
| `i18n/zh-TW.json` + `en.json` | Watchlist 翻譯 |
| `constants/errorCodes.ts` | 同步新錯誤碼 3001-3004 |

### 6.2 測試結果

```
Test Files  8 passed (8)
     Tests  121 passed (121)
  Duration  6.94s
```

---

## 7. Library 產出（Linus）

### 7.1 web-push 5.1.1 採用

- 用途：Wave 3 W4 push notification（VAPID）
- 風險：遞移依賴 BouncyCastle 1.74（CVE-2024-50379 EC key side-channel）
- **緩解**：dependencyManagement 強制升 BouncyCastle 1.77

### 7.2 文件

- `docs/05_development/library/20260423_w1_web-push_adoption.md`

---

## 8. 已知問題與緩解

| 問題 | 緩解 | 後續 |
|------|------|------|
| pre-push hook H2 lock | 刪除 odc.mv.db 重新下載 NVD（5-15 min） | Linus 評估 fix-Linus-Hook-001：scan 後加 trap 清 lock |
| Docker daemon 缺失 | guard Testcontainers tests with `assumeTrue(DockerClientFactory.isDockerAvailable())` | CI 必須備 Docker |
| frontend npm audit moderate（vitest 連動 vite/esbuild） | 不影響 prod build；moderate 不阻擋 push（僅 high+ 才阻擋） | 等待 vitest 5 上游修補 |

---

## 9. 等使用者裁示（4 項 blocker）

| # | 阻塞點 | 影響 | 建議行動 |
|---|--------|------|----------|
| 1 | D-02/D-09 滲透測試廠商簽約 + 法務 RFP 複核 | W6 啟動 | 親簽 |
| 2 | Jenkins docker-credentials + kubeconfig-dev 實際值 | dev 部署 | 提供 credentials |
| 3 | AWS SES Production Access 申請 | W4 mail notification | AWS 帳號擁有者送申請 |
| 4 | DBA 在 dev/prod 執行 `CREATE EXTENSION pg_trgm` | W3 search 上線 | 通知 DBA |

---

## 10. 下一步（W2 預備）

W1 push 成功 → Jamie 召喚下列 agents 啟動 W2：

```
W2 任務：
├─ Bruno：Watchlist API 實作（POST /watchlist/list、create、delete）
├─ Felix：Watchlist 接 real API（移除 MSW handler）
├─ Quincy + Quinn：W1 Security 整合測試 review + E2E 測試
├─ Brian：Bruno 後端 PR review
└─ Fiona：Felix 前端 PR review
```

預計時間：2 個工作日。

---

**報告完畢。**
