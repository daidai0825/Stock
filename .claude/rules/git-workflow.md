# Rule: Git 工作流程

> **適用範圍**：所有 git 操作
> **適用 Agents**：全體（特別是 Felix、Bruno、Linus）

---

## 分支策略：GitFlow 簡化版

```
main               ← 生產版本（受保護，只接受 PR）
  └─ release/*    ← 發版分支（QA 驗證）
       └─ develop ← 開發主幹
            ├─ feature/*  ← 功能開發
            ├─ bugfix/*   ← 一般 bug 修復
            └─ hotfix/*   ← 緊急修復（從 main 分支）
```

### 各分支用途

| 分支 | 來源 | 合併目標 | 說明 |
|------|------|----------|------|
| `main` | - | - | 生產正式版本，每次合併產出 tag |
| `release/x.y.z` | `develop` | `main` + `develop` | 發版前 QA 驗證 |
| `develop` | `main` | `release/*` | 開發主幹 |
| `feature/{ticket}-{desc}` | `develop` | `develop` | 新功能 |
| `bugfix/{ticket}-{desc}` | `develop` | `develop` | 一般 bug |
| `hotfix/{ticket}-{desc}` | `main` | `main` + `develop` | 緊急修復 |

## 分支命名

```
feature/USER-123-add-login
bugfix/USER-456-fix-pagination
hotfix/USER-789-critical-payment-bug
release/1.2.0
```

## Commit Message 規範

### 格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type

| Type | 用途 |
|------|------|
| `feat` | 新功能 |
| `fix` | bug 修復 |
| `docs` | 文件 |
| `style` | 格式（不影響邏輯） |
| `refactor` | 重構 |
| `perf` | 效能優化 |
| `test` | 測試 |
| `build` | 建構系統、依賴 |
| `ci` | CI/CD 配置 |
| `chore` | 雜項 |
| `revert` | 還原 |

### 範例

```
feat(user): 新增使用者註冊 API

- 實作 POST /api/v1/user/create
- 加入 email 驗證
- 撰寫單元測試（覆蓋率 85%）

Closes USER-123
```

```
fix(payment): 修正並發扣款重複問題

使用 Redis 分散式鎖避免競態條件。

Refs USER-456
```

### 規範

- **subject**：50 字元內，不加句點
- **body**：72 字元換行，說明「為什麼」與「怎麼做」
- **footer**：關聯票券（Closes、Refs、Breaks）
- 使用**繁體中文**或**英文**（同專案統一）

## PR 規範

### PR 標題

格式：`[<type>][<module>] <description>`

範例：
- `[feat][user] 新增使用者註冊功能`
- `[fix][payment] 修正並發扣款問題`

### PR 描述模板

```markdown
## 變更摘要

{1-3 句話說明這個 PR 做了什麼}

## 變更類型

- [ ] 新功能
- [ ] Bug 修復
- [ ] 重構
- [ ] 效能優化
- [ ] 文件更新

## 關聯票券

Closes USER-123

## 測試方式

- [x] 單元測試
- [x] 整合測試（Testcontainers）
- [ ] E2E 測試（Playwright）
- [ ] 手動測試

## 截圖（前端 PR 必填）

[截圖]

## Checklist

- [ ] 程式碼遵循風格規範
- [ ] 單元測試覆蓋率 ≥80%
- [ ] 通過 Linus 的依賴掃描
- [ ] 已更新相關文件
- [ ] 無 Blocker CVE

## 影響範圍

- 影響模組：user、auth
- 資料庫變更：是 / 否
- API 變更：是 / 否（破壞性變更：是 / 否）
- 配置變更：是 / 否
```

## PR 流程

1. **開發者**建立 PR
2. **Linus** 自動觸發依賴掃描
3. **CI** 自動跑單元測試 + lint + type check
4. **Reviewer**（Fiona / Brian）人工 Code Review
5. 修正後重新 review
6. **Approve** 後 merge（squash 或 rebase）
7. **觸發** Jenkins 部署到 dev

## Merge 策略

| 分支合併 | 策略 |
|----------|------|
| `feature/*` → `develop` | Squash and Merge |
| `develop` → `release/*` | Merge Commit |
| `release/*` → `main` | Merge Commit + Tag |
| `hotfix/*` → `main` | Merge Commit + Tag |

## Tag 規範

使用 [Semantic Versioning](https://semver.org/lang/zh-TW/)：

```
v1.2.3
│ │ └─ patch（bug 修復、不影響 API）
│ └─── minor（新功能、向後相容）
└───── major（破壞性變更）
```

範例：
```bash
git tag -a v1.2.0 -m "Release 1.2.0"
git push origin v1.2.0
```

## .gitignore 必含

### Java

```
target/
build/
*.class
*.jar
*.war
.idea/
.vscode/
*.iml
.DS_Store
```

### Node / React

```
node_modules/
dist/
build/
.env.local
.env.*.local
*.log
.DS_Store
.vscode/
.idea/
```

### 通用敏感檔案

```
*.pem
*.key
*.crt
.env
secrets/
credentials.json
```

## 受保護分支

`main` 與 `develop` 受保護：

- 禁止直接 push
- 必須透過 PR
- 必須通過 CI
- 必須至少 1 位 Reviewer Approve（Fiona / Brian）
- `main` 必須通過 QA（Quincy + Quinn）

## 提交頻率

- 鼓勵小而頻繁的 commit
- 每個 commit 應為**邏輯獨立**的變更
- 大型功能拆解為多個 PR

## Hotfix 流程

詳見 [hotfix-sop](../skills/hotfix-sop.md) skill。

簡述：
1. 從 `main` 分支建立 `hotfix/*`
2. 修復 + 快速 Review（Fiona / Brian）
3. 合併到 `main` + tag
4. 同步合併到 `develop`
5. 立即部署

## 禁止事項

- **禁止**直接 push 到 `main` / `develop`
- **禁止**force push 到共用分支
- **禁止**commit 敏感資訊（用 .gitignore + git-secrets）
- **禁止**commit 大檔案（>10MB，用 Git LFS）
- **禁止**commit 註解掉的程式碼
- **禁止**modify 已 push 的歷史（除非團隊同意）
- **禁止**不寫 type 的 commit message
- **禁止**用 `--no-verify` 跳過 hooks（除非有正當理由）
