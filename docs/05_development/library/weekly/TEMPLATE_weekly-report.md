# Library 週報 — YYYY-MM-DD

- **撰寫者**：Linus（Library 工程師）
- **掃描週次**：第 N 週（YYYY-MM-DD ~ YYYY-MM-DD）
- **下次掃描時間**：YYYY-MM-DD 09:00（GMT+8）
- **報告路徑**：`docs/05_development/library/weekly/YYYYMMDD_weekly-report.md`

---

## 0. 摘要

| 項目 | 數值 | 變動（vs 上週） |
|------|------|----------------|
| 後端直接依賴數 | XX 個 | + / - / 0 |
| 後端遞移依賴數 | ~ XXX 個 | + / - / 0 |
| 前端直接依賴數 | XX 個 | + / - / 0 |
| 前端遞移依賴數 | ~ X,XXX 個 | + / - / 0 |
| **本週新增 Critical CVE** | X 個 | - |
| **本週新增 High CVE** | X 個 | - |
| **本週新增 Medium CVE** | X 個 | - |
| **本週新增 Low CVE** | X 個 | - |
| 可升級 — major | X 個 | - |
| 可升級 — minor | X 個 | - |
| 可升級 — patch | X 個 | - |

---

## 1. 🔴 Critical CVE（立即處理）

> 處理時限：依 Linus agent 規範，立即停止 merge、上報 Jamie 召集緊急修復

| 套件 | 現版本 | CVE | CVSS | 修復版本 | 影響範圍 | 狀態 |
|------|--------|-----|------|----------|----------|------|
| - | - | - | - | - | - | ☐ |

**本週說明**：（若無，填寫「本週無 Critical CVE」）

---

## 2. 🟠 High CVE（24 小時內處理）

| 套件 | 現版本 | CVE | CVSS | 修復版本 | 影響範圍 | 負責人 | 狀態 |
|------|--------|-----|------|----------|----------|--------|------|
| - | - | - | - | - | - | - | ☐ |

**本週說明**：

---

## 3. 🟡 Medium CVE（一週內處理）

| 套件 | 現版本 | CVE | CVSS | 修復版本 | 影響範圍 | 負責人 | 狀態 |
|------|--------|-----|------|----------|----------|--------|------|
| - | - | - | - | - | - | - | ☐ |

**本週說明**：

---

## 4. 🟢 Low CVE（季度盤點處理）

| 套件 | 現版本 | CVE | CVSS | 修復版本 | 備註 |
|------|--------|-----|------|----------|------|
| - | - | - | - | - | - |

**本週說明**：

---

## 5. 升級建議

### 5.1 後端（Maven）

| 套件 | 現版本 | 建議版本 | 類型 | 風險評估 | 預估升級成本 |
|------|--------|----------|------|----------|------------|
| - | - | - | major / minor / patch | 高 / 中 / 低 | X 人天 |

### 5.2 前端（npm）

| 套件 | 現版本 | 建議版本 | 類型 | 風險評估 | 預估升級成本 |
|------|--------|----------|------|----------|------------|
| - | - | - | major / minor / patch | 高 / 中 / 低 | X 人天 |

---

## 6. Maintenance Mode 套件監控

> 依 `dependency-inventory.md` 標記為 maintenance mode 的套件，需每週確認 GitHub 活動

| 套件 | 最後更新 | GitHub 活動 | Java 21 issue | 替代方案準備度 | 風險變化 |
|------|----------|-------------|--------------|--------------|---------|
| easy-rules | 2020-12 | - | 無新 issue | RuleBook 0.13 | 持平 |
| pushy | YYYY-MM | - | - | 自寫 OkHttp + JWT | 持平 |
| lightweight-charts-indicators | YYYY-MM | - | - | 自維 fork | 持平 |

---

## 7. 抑制規則複審

> 依 `backend/dependency-check.xml` 中設定 `until` 屬性的抑制規則

| CVE | 抑制套件 | 評估人 | 抑制日期 | 到期日 | 本週是否到期 | 動作 |
|-----|----------|--------|----------|--------|------------|------|
| - | - | - | - | - | 否 | - |

---

## 8. License 合規檢查

| 套件 | License | 合規狀態 | 備註 |
|------|---------|---------|------|
| - | - | ✓ / ⚠️ / ✗ | - |

**本週風險**：（若無變動，填寫「本週 License 組合無變動」）

---

## 9. 下週行動

- [ ] 處理 Critical CVE（若有）
- [ ] 24 小時內提出 High CVE 修復方案
- [ ] 排程 X 套件升級
- [ ] 抑制規則 X 件到期複審
- [ ] 與 Bruno / Felix 討論升級時程

---

## 10. 上報 Jamie 事項

| # | 事項 | 嚴重度 | 建議行動 |
|---|------|--------|----------|
| - | - | 🔴 / 🟠 / 🟡 / 🟢 | - |

---

## 附錄 A：完整掃描指令紀錄

```bash
# 後端掃描
cd backend
mvn org.owasp:dependency-check-maven:aggregate
# 報告：backend/target/dependency-check-report.html

# 前端掃描
cd frontend
npm audit --json > audit-report-YYYYMMDD.json
npm outdated > outdated-YYYYMMDD.txt
```

## 附錄 B：本週掃描環境

| 項目 | 版本 |
|------|------|
| OS | macOS Darwin 25.x / Ubuntu 22.04 |
| JDK | 21 |
| Maven | 3.9.x |
| Node | 20.x |
| OWASP Dependency Check | 10.0.4 |
| NVD 資料庫更新時間 | YYYY-MM-DD HH:MM |

---

**首次週報排程**：每週一 09:00（GMT+8）

**週報撰寫規範**：
1. 所有日期使用 ISO 8601 格式（YYYY-MM-DD）
2. 檔案名稱：`YYYYMMDD_weekly-report.md`
3. 檔案內日期必須與檔名一致（依 documentation.md 規範）
4. 空欄位填 `-`，禁止留白
5. emoji 使用 Unicode（🔴/🟠/🟡/🟢/✓/☐/☑），禁用 `:red_circle:` 等語法
6. CVE 編號格式：`CVE-YYYY-NNNNN`
7. CVSS 分數保留一位小數（例 7.5）

**模板結束**
