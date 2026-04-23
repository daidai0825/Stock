# Wave 3 啟動報告

> **報告日期**：2026-04-23
> **報告人**：Jamie（Leader）
> **報告階段**：Wave 2 → Wave 3 銜接
> **目標版本**：v0.3.0（預計 6 週）

---

## 1. 一句話結論

> **Wave 3 已啟動：Patricia PRD 草稿就緒、Linus 掃描完成、2 項 TD 已修復。等使用者確認 5 個 PRD 決策後，即可進入 Peter SRS 階段。**

---

## 2. 本次階段成果（What was done）

### A. Wave 3 規劃（Patricia + Linus 並行）

| 項目 | 產出 | 狀態 |
|------|------|------|
| Wave 3 PRD 草稿 | `docs/02_product/20260423_wave3_PRD.md` | 🟡 草稿（等決策） |
| Wave 3 依賴掃描 | `docs/05_development/library/20260423_wave3_dependency_scan.md` | ✅ 完成 |
| .gitignore 新增 dependency-tree.txt 排除 | `.gitignore` | ✅ 完成 |

### B. 立即可執行的 TD 清償（並行）

| TD | 負責人 | 結果 | Commit |
|------|--------|------|--------|
| BUG-QUINCY-001（ChipDTO 移除 stockName） | Bruno | ✅ 修復 + 73/73 tests PASS | `251e322` |
| TD-Q-Firefox（playwright.config.ts 加 Firefox） | Quinn | ✅ 修復 + `npx playwright test --list` 確認 | `25b813a` |
| Wave 3 啟動文件 commit | Jamie | ✅ Patricia PRD + Linus 掃描入庫 | `ae80ae9` |

---

## 3. Wave 3 PRD 摘要（Patricia）

### 願景

讓 Wave 2 的 StockDetail 從「需要記得 stockId 才能用」進化成「打開 App 就有事可做」的日常陪伴型產品。

### 5 個核心功能

| 編號 | 功能 | 優先級 | 工時 | 為什麼 |
|------|------|--------|------|--------|
| F-W3-01 | 個股搜尋（中/英/代號 + 自動完成 + 熱門） | **P0** | M（5-7d） | 沒有搜尋等於 Wave 2 沒上線 |
| F-W3-02 | 自選股 Watchlist（單一群組） | **P0** | M（5-7d） | 核心回訪鉤子 |
| F-W3-03 | 價格警示 Price Alert | P1 | L（8-10d） | 主動觸達差異化 |
| F-W3-04 | Spring Security + 滲透測試 | **P0** | M（5-7d） | 合規門檻、技術債清償 |
| F-W3-05 | 搜尋歷史紀錄 | P1 | S（1-2d） | 搜尋體驗附加 |

**總工時**：24-33 day，**6 週版本**（含 1 週滲透測試 buffer）

### Out-of-Scope（明確不做）

10 項：多 Watchlist 群組、技術指標疊加、新聞聚合、產業分類、籌碼歷史圖、buy/sell 明細擴充、LINE Notify、Native App、收費機制等。

---

## 4. Linus 依賴掃描摘要

### 後端（Java + Maven）

| 風險等級 | 項目數 | 代表 |
|----------|--------|------|
| 🔴 必升（CVE / Critical） | 3 | Spring Boot 3.3.13→3.3.14、logback 1.5.18→1.5.32（CVE-2024-50379）、Flyway |
| 🟡 建議升 | 8 | Springdoc、Log4j 系列 |
| 🟢 可選 | 5 | Jackson、PostgreSQL JDBC major |

### 前端（npm）

| 風險等級 | 項目數 | 代表 |
|----------|--------|------|
| 🔴 必升 | 4 | axios 1.15.0→1.15.2、vitest 2.1.9→2.2.0+、vite patch、npm audit fix |
| 🟡 建議升 | 10 | React Query、react-hook-form、prettier |
| 🟢 可選 | 6 | antd v6、React Router v7、zod v4（major） |

### Sprint 0 預估

**2-3 小時** 可清空所有 🔴 必升項目（Spring Boot patch + logback + axios + vitest + plugin 補齊）。

---

## 5. 等使用者拍板的決策（合併 Wave 2 + Wave 3）

### Wave 2 殘留（Sophia 4 個）

1. ☐ Wave 2 Cloud Only？hybrid 延 v1.5？
2. ☐ 滲透測試廠商選定（建議金融產業經驗，預算 30-80 萬）
3. ☐ prod 部署策略：Canary vs Blue-Green
4. ☐ `oracle-database.md` → `relational-database.md` rename（PostgreSQL 16）

### Wave 3 新增（Patricia 5 個）

5. ☐ **D1**：廣度（5 功能） vs 深度（強化 1 功能） — Patricia 建議廣度
6. ☐ **D2**：Watchlist 強制登入 vs 訪客 localStorage — Patricia 建議強制登入（強化 MAU）
7. ☐ **D3**：推播管道 — Patricia 建議 Web Push + Email；LINE Notify 已 EOL
8. ☐ **D4**：未授權回 HTTP 401 vs HTTP 200 + code 3001 — Patricia 建議方案 B（符合既有 Envelope）
9. ☐ **D5**：滲透測試廠商方向（與 #2 合併）

> **建議**：使用者一次性回覆 9 個決策，避免來回。

---

## 6. Commit History（本階段）

```
ae80ae9 docs(wave3): kickoff - Patricia PRD + Linus dependency scan
25b813a test(e2e): add Firefox project to playwright.config (TD-Q-Firefox)
251e322 fix(backend): BUG-QUINCY-001 remove stockName from ChipDTO per schema-lock §5.3
```

3 個 commit，全部已 push 到 `origin/develop`。

---

## 7. Wave 3 建議時程

| 週次 | 範圍 | 輸出 |
|------|------|------|
| W0（本週剩餘） | Sprint 0 必升、Patricia PRD 拍板、Peter SRS 啟動 | 升級 commit、Wave 3 SRS、API Spec |
| W1 | Sophia/Preston 架構（含 Spring Security 整合）、F-W3-04 規劃 | 架構決策、ER 圖更新 |
| W2-W3 | F-W3-01（搜尋）+ F-W3-04（Spring Security）並行開發 | 後端 API + 前端 UI |
| W3-W4 | F-W3-02（Watchlist）開發 | 後端 API + 前端 UI |
| W4-W5 | F-W3-03（Price Alert）開發 + F-W3-05（搜尋歷史） | 後端排程 + 推播整合 |
| W5 | Code Review + QA | 投票 + 測試報告 |
| W6 | 滲透測試 + 修補 + Release | v0.3.0 |

---

## 8. 風險評估（Patricia 識別）

| 風險 | 影響 | 緩解 |
|------|------|------|
| 滲透測試發現 Critical 漏洞 | High | W2 提早跑 OWASP ZAP 預檢、保留 W6 buffer |
| Spring Security 接入打壞既有 API | High | 跑全 contract test、保留 W3 整合測試 |
| TWSE / OTC 股票主檔不全（影響搜尋） | Medium | W1 由 Bruno 比對 TWSE+OTC 主檔補齊 |
| Web Push 在 Safari 體驗差 | Low | UX 文案說明 + Email 後援 |
| 滲透測試廠商檔期排不到 | Medium | 提早 W0 簽約 |

---

## 9. Wave 2 → Wave 3 技術債移交清單

| TD | 嚴重度 | 預估工時 | 已清償？ |
|------|--------|----------|----------|
| BUG-QUINCY-001（ChipDTO stockName） | Major | 0.5h | ✅ Wave 3 啟動時清償 |
| TD-Q-Firefox（Playwright Firefox） | Medium | 0.5h | ✅ Wave 3 啟動時清償 |
| BUG-QUINCY-002（institutions buy/sell=0） | Major | 視 TWSE 替代方案 | ⏳ Wave 3 評估資料來源 |
| TD-OWASP-A01（Spring Security） | High | 2-3 day | ⏳ F-W3-04 主軸 |
| TD-Q-Network（網路斷線情境） | High | 1-2 day | ⏳ Wave 3 QA 補測 |
| TD-PARENT-POM（Spotless/Checkstyle） | Low | 1h | ⏳ Sprint 0 含在 Linus plugin 補齊 |
| Wave-B Round 2 IDS（9 項 schema-lock TS 修復） | Medium | 詳清單 | ⏳ Felix Wave 3 早期處理 |
| 跨 Wave-7（listQuotes N+1） | Medium | — | ⏳ F-W3-02 Watchlist 彙整時順便優化 |

---

## 10. 下一步

### 立即（等使用者）

1. **使用者一次回覆 9 個決策**（Wave 2 殘留 4 + Wave 3 新增 5）
2. 決策確認後：
   - **Patricia**：將 PRD 從 🟡 草稿轉 ✅ 正式
   - **Peter**：啟動 Wave 3 SRS + API Spec
   - **Sophia + Preston**：架構評估（含 Spring Security 整合）
   - **Linus**：執行 Sprint 0 必升項目（2-3h）

### 短期（W0-W1）

3. 滲透測試廠商接洽 + 簽約
4. dev 環境 Wave 2 部署（待 Jenkins credentials 設定）
5. Sprint 0 完成 → Sprint 1 啟動

---

**Wave 3 啟動完成。等候使用者下一步指示（建議：一次回覆 9 個決策）。**
