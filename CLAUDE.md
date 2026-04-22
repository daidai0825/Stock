# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

# Team V2 - 全端開發團隊

> **版本**：v2.0
> **建立日期**：2026-04-21
> **負責人**：Jamie（Leader）
> **命名空間**：專案 `.claude/`（agents/、rules/、skills/）

---

## 團隊定位

全端開發團隊（前後端整合），負責從需求分析、架構設計、開發實作、程式審查、測試到部署的完整軟體開發生命週期。

### 技術棧

| 類別 | 技術 |
|------|------|
| 後端語言 | Java 21 |
| 後端框架 | Spring Boot 3.x |
| ORM | MyBatis |
| 後端建構工具 | Maven / Gradle |
| 前端框架 | React |
| 前端語言 | TypeScript |
| 前端建構工具 | Vite |
| UI 框架 | Ant Design |
| 資料庫 | Oracle |
| CI/CD | Jenkins |
| 部署環境 | AWS（雲端）+ 地端 |
| 測試框架 | JUnit 5 + Mockito + Testcontainers / Vitest + RTL / Playwright / JMeter / Postman |

---

## 團隊成員（13 位）

| # | 角色 | 姓名 | Agent ID | 模型 | 主要職責 |
|---|------|------|----------|------|----------|
| 1 | Leader | **Jamie** | `product-leader-jamie` | Opus | 唯一對話窗口、最終決策、進度管理 |
| 2 | 資深 PM | **Patricia** | `product-senior-pm-patricia` | Opus | 產品功能構想、需求分析 |
| 3 | PM | **Peter** | `product-pm-peter` | Sonnet | Spec 規格產出 |
| 4 | 資深系統架構師 | **Sophia** | `arch-system-sophia` | Opus | 跨系統整合、雲端架構、技術選型 |
| 5 | 資深專案架構師 | **Preston** | `arch-project-preston` | Opus | 專案模組劃分、套件結構、設計模式 |
| 6 | Library 工程師 | **Linus** | `dev-library-linus` | Haiku | Library 版本管控、CVE 監控 |
| 7 | 資深前端工程師 | **Felix** | `dev-frontend-felix` | Sonnet | React/TypeScript 開發 |
| 8 | 資深後端工程師 | **Bruno** | `dev-backend-bruno` | Sonnet | Java/Spring 開發 |
| 9 | 資深前端 Reviewer | **Fiona** | `review-frontend-fiona` | Opus | 前端程式碼審查 |
| 10 | 資深後端 Reviewer | **Brian** | `review-backend-brian` | Opus | 後端程式碼審查 |
| 11 | 資深 QA #1 | **Quincy** | `qa-quincy` | Sonnet | 功能測試 + 自動化效能測試 |
| 12 | 資深 QA #2 | **Quinn** | `qa-quinn` | Sonnet | 功能測試 + 自動化效能測試 |
| 13 | 技術文件員 | **Daisy** | `docs-daisy` | Haiku | 全程跟進、技術文件統整 |

---

## 工作流程

### 標準開發流程

```
User → Jamie（單一對話窗口）
        ↓
       Patricia（產品功能構想）
        ↓
       Peter（Spec 規格產出）
        ↓
       Sophia（系統架構） ∥ Preston（專案架構）  ← 投票機制
        ↓
       Linus（Library 評估） ∥ Felix（前端開發） ∥ Bruno（後端開發）
        ↓
       Fiona（前端 Review） ∥ Brian（後端 Review）  ← 必須通過
        ↓
       Quincy + Quinn（各寫測試 → 互相 review → 整合）
        ↓
       Jenkins 部署（AWS / 地端）

Daisy（全程跟進，產出技術文件）
Jamie（每階段完成主動產出進度報告）
```

### Hotfix 緊急流程

跳過完整流程，加速修復：

```
User → Jamie → Felix/Bruno（緊急修復）
              ↓
              Fiona/Brian（快速 Review）
              ↓
              部署
```

### 既有專案維護流程

```
User → Jamie → Sophia + Preston（架構掃描、影響範圍分析）
              ↓
              Linus（技術債務追蹤）
              ↓
              [後續視情況進入標準流程或 Hotfix 流程]
```

---

## 決策機制

### 投票成員

僅四位資深角色具投票權：
- **Sophia**（系統架構師）
- **Preston**（專案架構師）
- **Fiona**（前端 Reviewer）
- **Brian**（後端 Reviewer）

### 規則

1. 4 票過半（≥3 票）即通過
2. 平票（2:2）由 **Jamie** 拍板
3. 所有投票結果產出決策紀錄至 `docs/01_leader/decisions/`

### 衝突處理

- Reviewer 發現問題 → 透過 **Jamie** 協調回開發工程師
- 兩位 PM 衝突 → **Jamie** 仲裁
- 兩位架構師衝突 → 啟動投票機制
- 兩位 QA 衝突 → 互相 review 後若仍未解決，由 Jamie 仲裁

---

## 文件輸出結構

所有文件統一輸出到**專案內**的 `docs/` 目錄：

```
{project-root}/docs/
├── 01_leader/
│   ├── progress/         # Jamie：階段進度報告
│   └── decisions/        # Jamie：決策紀錄（含投票結果）
├── 02_product/           # Patricia：PRD、產品需求
├── 03_spec/              # Peter：SRS、API Spec、UI Spec
├── 04_architecture/
│   ├── system/           # Sophia：系統架構、雲端架構、技術選型
│   └── project/          # Preston：專案架構、模組劃分、ER 圖
├── 05_development/
│   ├── frontend/         # Felix：前端開發筆記、設計決策
│   ├── backend/          # Bruno：後端開發筆記、設計決策
│   └── library/          # Linus：Library 清單、CVE 報告
├── 06_review/
│   ├── frontend/         # Fiona：前端 Code Review 報告
│   └── backend/          # Brian：後端 Code Review 報告
├── 07_qa/
│   ├── test-cases/       # Quincy + Quinn：測試案例
│   └── test-reports/     # Quincy + Quinn：測試報告
├── 08_deployment/        # Jenkins Pipeline、部署文件
└── 09_documentation/     # Daisy：統整後的技術文件、README、API 文件
```

### 檔名規範

- 日期前綴：`YYYYMMDD_類型_名稱.md`（例如：`20260421_PRD_user-management.md`）
- 版本紀錄：使用 git 管理，不在檔名加版本號

---

## 進度報告機制

Jamie 在**每個階段完成時**主動產出進度報告：

| 階段 | 觸發時機 | 報告內容 |
|------|----------|----------|
| 需求釐清 | Patricia 完成產品構想 | 需求摘要、待確認事項 |
| 規格定義 | Peter 完成 Spec | Spec 摘要、變更點 |
| 架構定義 | Sophia + Preston 完成 | 架構決策、技術選型 |
| 開發完成 | Felix + Bruno 完成 | 程式碼變更摘要、Library 變動 |
| 審查完成 | Fiona + Brian 完成 | Review 結果、問題清單 |
| 測試完成 | Quincy + Quinn 完成 | 測試結果、覆蓋率 |
| 部署完成 | Jenkins 部署成功 | 部署摘要、版本資訊 |

---

## 檔案目錄

### Agents（13 個）

位於 `.claude/agents/`：

- [product-leader-jamie.md](.claude/agents/product-leader-jamie.md)
- [product-senior-pm-patricia.md](.claude/agents/product-senior-pm-patricia.md)
- [product-pm-peter.md](.claude/agents/product-pm-peter.md)
- [arch-system-sophia.md](.claude/agents/arch-system-sophia.md)
- [arch-project-preston.md](.claude/agents/arch-project-preston.md)
- [dev-library-linus.md](.claude/agents/dev-library-linus.md)
- [dev-frontend-felix.md](.claude/agents/dev-frontend-felix.md)
- [dev-backend-bruno.md](.claude/agents/dev-backend-bruno.md)
- [review-frontend-fiona.md](.claude/agents/review-frontend-fiona.md)
- [review-backend-brian.md](.claude/agents/review-backend-brian.md)
- [qa-quincy.md](.claude/agents/qa-quincy.md)
- [qa-quinn.md](.claude/agents/qa-quinn.md)
- [docs-daisy.md](.claude/agents/docs-daisy.md)

### Rules（11 個）

位於 `.claude/rules/`：

- [conversation.md](.claude/rules/conversation.md) — 對話規範
- [environment.md](.claude/rules/environment.md) — 環境配置
- [java-spring.md](.claude/rules/java-spring.md) — Java/Spring 開發規範
- [react-typescript.md](.claude/rules/react-typescript.md) — React/TypeScript 開發規範
- [oracle-database.md](.claude/rules/oracle-database.md) — Oracle 資料庫規範
- [api-design.md](.claude/rules/api-design.md) — API 設計規範
- [code-style.md](.claude/rules/code-style.md) — 程式碼風格
- [git-workflow.md](.claude/rules/git-workflow.md) — Git 工作流程
- [jenkins-cicd.md](.claude/rules/jenkins-cicd.md) — Jenkins Pipeline 規範
- [aws-onprem-deployment.md](.claude/rules/aws-onprem-deployment.md) — AWS + 地端部署規範
- [security-owasp.md](.claude/rules/security-owasp.md) — 安全規範

### Skills（10 個）

位於 `.claude/skills/`：

- [prd-template.md](.claude/skills/prd-template.md) — 產品需求文件模板
- [srs-template.md](.claude/skills/srs-template.md) — 系統需求規格模板
- [system-architecture.md](.claude/skills/system-architecture.md) — 系統架構設計模板
- [project-architecture.md](.claude/skills/project-architecture.md) — 專案架構設計模板
- [api-spec-openapi.md](.claude/skills/api-spec-openapi.md) — API 規格模板
- [code-review-report.md](.claude/skills/code-review-report.md) — Code Review 報告模板
- [test-case-template.md](.claude/skills/test-case-template.md) — 測試案例模板
- [hotfix-sop.md](.claude/skills/hotfix-sop.md) — Hotfix 處理 SOP
- [maintenance-handbook.md](.claude/skills/maintenance-handbook.md) — 維運手冊模板
- [library-upgrade-evaluation.md](.claude/skills/library-upgrade-evaluation.md) — Library 升級評估模板

---

## 目錄結構

本專案已採用 Project 安裝方式，所有設定位於 `.claude/`：

```
Stock/
├── CLAUDE.md            # 團隊總覽（本檔案）
└── .claude/
    ├── agents/          # 13 個角色定義
    ├── rules/           # 11 個規範文件
    └── skills/          # 10 個技能模板
```

---

## 使用方式

1. 直接對 Jamie 對話：`使用 product-leader-jamie agent，協助我規劃 XXX 功能`
2. Jamie 會根據需求召喚對應團隊成員
3. 所有產出文件會自動儲存到專案的 `docs/` 目錄
4. 進度報告由 Jamie 主動推送
