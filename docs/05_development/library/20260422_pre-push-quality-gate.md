# Pre-Push Quality Gate — 品質檢查規範

> **維護者**：Linus（Library 工程師）  
> **建立日期**：2026-04-22  
> **版本**：v2（品質檢查 + CVE 掃描）  
> **適用範圍**：所有開發者的 git push

---

## 背景

Wave A（Felix）skeleton 提交時繞過品質檢查，導致 3 項本應被 CI 擋下的問題：

1. TypeScript 型別錯誤（type-check 未抓到）
2. Vitest 測試失敗（antd 5 中文選擇器）
3. ESLint 警告超限（lint --max-warnings=0）

**本規範鎖死 pre-push quality gate**，確保品質檢查先於遠端 CI 執行。

---

## 啟用方式

每位開發者 **clone 或 pull** 後執行一次：

```bash
git config core.hooksPath .githooks
```

驗證設定成功：

```bash
cat .git/config | grep hooksPath
# 應輸出：hooksPath = .githooks
```

---

## 檢查項目與耗時

| 側 | 檢查項目 | 耗時 | 觸發條件 |
|-----|---------|------|---------|
| **後端** | Unit Tests（mvn test） | 2-5 min | backend/ 有變動 |
| **後端** | CVE 掃描（OWASP） | 5-15 min | backend/pom.xml 有變動 |
| **前端** | ESLint | 10-30 sec | frontend/ 有變動 |
| **前端** | TypeScript Type Check | 20-40 sec | frontend/ 有變動 |
| **前端** | Unit Tests（Vitest） | 30-60 sec | frontend/ 有變動 |
| **前端** | CVE 掃描（npm audit） | 10-30 sec | frontend/package.json 有變動 |

### 預估 push 耗時

| 場景 | 耗時 |
|------|------|
| 純前端變動（無 package.json） | **1-2 分鐘** |
| 純後端變動（無 pom.xml） | **2-5 分鐘** |
| 前後端都變動（無依賴檔） | **3-7 分鐘** |
| 前後端 + 依賴檔變動 | **8-20 分鐘** |
| 純文件變動（docs/） | **<1 秒**（直接跳過） |

---

## 執行流程

### 1. 變動偵測

Hook 首先分析 git diff，判斷哪些部分有變動：

```bash
git diff --name-only "@{u}...HEAD"
# 或首次 push 時比對 origin/main
```

**智慧決策**：

- `backend/pom.xml` 有變動 → 執行 CVE 掃描（5-15 分鐘）
- `backend/` 其他檔有變動 → 執行單元測試（2-5 分鐘）
- `frontend/package.json` 有變動 → 執行 npm audit（10-30 秒）
- `frontend/` 其他檔有變動 → 執行 lint / typecheck / test（1-2 分鐘）
- 僅 `docs/` 變動 → 跳過所有檢查（<1 秒）

### 2. 後端品質檢查

```bash
cd backend && mvn -B -q test
```

**失敗處理**：

```bash
cd backend && mvn test
# 查看具體錯誤，修正後重試
```

### 3. 前端品質檢查（順序執行）

**3.1 Lint**

```bash
cd frontend && npm run lint
```

失敗時修復：

```bash
cd frontend && npm run lint -- --fix
```

**3.2 TypeScript 型別檢查**

```bash
cd frontend && npm run type-check
```

修復：在編輯器中修正型別錯誤，或檢查 `tsconfig.json` 設定。

**3.3 單元測試**

```bash
cd frontend && npm run test -- --run
```

失敗時調試：

```bash
cd frontend && npm run test:watch
# 互動式修正測試
```

### 4. CVE 掃描（若依賴檔有變）

**後端**：

```bash
cd backend && mvn org.owasp:dependency-check-maven:check
# 報告：backend/target/dependency-check-report.html
```

**前端**：

```bash
cd frontend && npm audit --audit-level=high
```

---

## 失敗時的本地修復指令

Hook 會自動提示確切的修復指令，範例：

```
❌ Frontend Lint failed
修復指令：
  cd frontend && npm run lint -- --fix

❌ Frontend Type-check failed
修復指令：
  cd frontend && npm run type-check
  或開啟相關檔案修正 TypeScript 型別錯誤

❌ Backend Unit Tests failed
修復指令：
  cd backend && mvn test
```

---

## 跳過機制（緊急用）

允許使用 `--no-verify` 跳過 hook，但 **需 Jamie 簽核**：

```bash
git push --no-verify
```

**使用場景**：

- 🔴 Critical 線上 bug（hotfix）
- 🔴 特殊合規要求（legal hold）
- 🟠 依賴無法在本地解決（等待 Linus 評估）

**執行前須**：

1. 確認 PR CI 會重新執行檢查（Jenkins）
2. 通知 Jamie 跳過原因
3. 記錄到 PR description

---

## 常見問題

### Q: npm/mvn 找不到

**A**: 安裝或使用 wrapper

```bash
# 後端：使用 Maven Wrapper
cd backend && ./mvnw test

# 前端：確認 Node.js 已安裝
node --version  # 需 v20+
npm install     # 重新安裝依賴
```

### Q: 首次 push 時失敗（沒有 upstream）

**A**: 正常現象。Hook 會比對 `origin/main`

```bash
# 確保本機有 fetch 最新 origin
git fetch origin main
# 再執行 push
git push -u origin <branch>
```

### Q: 某個檢查太慢，想跳過

**A**: 使用 `--no-verify`（需 Jamie 簽核），或改進設定：

- **後端太慢**：考慮分模組測試
  ```bash
  cd backend && mvn -pl stock-member -am test  # 僅測 stock-member 模組
  ```
- **前端太慢**：檢查是否有冗餘套件
  ```bash
  cd frontend && npm audit  # 清理過期依賴
  ```

### Q: Hook 執行出錯（permission denied）

**A**: 確保 hook 可執行

```bash
chmod +x .githooks/pre-push
git config core.hooksPath .githooks  # 重新設定一次
```

---

## 檢查清單

| 項目 | 確認 |
|------|------|
| git config core.hooksPath 已設定 | ☐ |
| .githooks/pre-push 可執行 | ☐ |
| npm/mvn 已安裝 | ☐ |
| frontend/package.json 存在 npm scripts | ☐ |
| backend/pom.xml 存在 | ☐ |
| 本地測試全部通過 | ☐ |

---

## 後續規劃（由 Sophia 負責 CI）

1. **Jenkins Pipeline**：同步 pre-push 檢查清單
2. **Slack 通知**：push 失敗時自動通知 #development
3. **Metrics**：追蹤 hook 檢查耗時與失敗率
4. **周期優化**：每月評估檢查項目與門檻

---

## 維護與更新

遇到以下情況應由 Linus 更新 hook：

- 新增 Maven module（後端）
- npm scripts 異動（前端）
- 相容性問題（Node.js / Maven 版本）
- CVE 門檻調整（critical → high 等）

更新後在 PR 說明中加註：

```
chore(library): 更新 pre-push quality gate v2.1

- 新增 Java 21 virtual threads 相容性檢查
- 降低 npm audit 門檻至 medium（臨時，待 Linus 評估）
```
