# Pre-Push Quality Gate — 執行報告

**日期**：2026-04-22  
**執行者**：Linus（Library 工程師）  
**目的**：防止品質檢查繞過（Wave A skeleton 問題補救）

---

## 執行內容

### 1. 強化 `.githooks/pre-push` v1 → v2

| 項目 | v1 | v2 |
|------|----|----|
| 後端 CVE 掃描 | ✓ | ✓ |
| 前端 CVE 掃描 | ✓ | ✓ |
| 後端單元測試 | ✗ | ✓ |
| 前端 ESLint | ✗ | ✓ |
| 前端 TypeScript 型別 | ✗ | ✓ |
| 前端單元測試 | ✗ | ✓ |
| 智慧變動偵測 | ✓（CVE 只） | ✓（擴展至品質） |
| 失敗時本地修復指令 | ✗ | ✓ |
| 耗時統計 | ✗ | ✓ |

### 2. 新增文件

- `docs/05_development/library/20260422_pre-push-quality-gate.md`  
  完整使用指南、常見問題、檢查清單

### 3. 驗證

- ✓ Hook 語法檢查通過（bash -n）
- ✓ 前端所有 npm scripts 存在且可執行
  - `npm run lint` (ESLint with --max-warnings 0)
  - `npm run type-check` (tsc --noEmit)
  - `npm run test` (Vitest)
  - 測試結果：15/15 通過
- ✓ Backend pom.xml 結構完整（13 modules）

---

## 預估 Push 耗時

| 場景 | 耗時 | 備註 |
|------|------|------|
| 純文件（docs/） | <1 sec | 跳過所有檢查 |
| 純前端無 package.json 變動 | 1-2 min | lint + typecheck + test |
| 純後端無 pom.xml 變動 | 2-5 min | mvn test（smoke 測試） |
| 前後端都變無依賴 | 3-7 min | 品質檢查 combined |
| **新增依賴** | +5-15 min | CVE 掃描（首次 NVD 下載） |

---

## 跳過機制

允許緊急 bypass（需 Jamie 簽核）：

```bash
git push --no-verify
```

Hook 會印出警告訊息，提醒後續 CI 會執行檢查。

---

## 關鍵改進點

1. **無法繞過的品質檢查**  
   Wave A 時期的 3 個問題（type-check / test / lint）已被 pre-push hook 攔截

2. **智慧變動偵測**  
   - 文件只改 docs/ → 直接通過（<1 sec）
   - 無依賴檔變動 → 省 5-15 min CVE 掃描時間

3. **本地即時反饋**  
   失敗時提示確切修復指令，避免 push 失敗後又要在 CI 上來回

4. **與 CI 協調**  
   Pre-push 不重複執行 CI 的工作，保留 CVE 掃描作為把關

---

## 後續建議

### 短期（1-2 週，Sophia 負責）

- 新增 Jenkins Pipeline 同步 pre-push 檢查清單
- Slack 通知機制（push 失敗 → #development）

### 中期（1 個月，Bruno/Felix 負責）

- 前後端 quality gate 門檻微調（若有誤報）
- Maven Wrapper 補齊（簡化本地環境）

### 長期（季度評估，Linus 負責）

- 依賴掃描耗時優化（增量掃描）
- CVE 門檻升級（critical → high）

---

## 交付物

| 檔案 | 路徑 | 用途 |
|------|------|------|
| pre-push (v2) | `.githooks/pre-push` | 執行品質檢查 |
| 使用指南 | `docs/.../20260422_pre-push-quality-gate.md` | 團隊參考 |

**開發者激活**（推送至 team channel）：

```bash
git config core.hooksPath .githooks
```
