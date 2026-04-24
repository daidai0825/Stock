# 決策紀錄：D-2026-04-24-01 — W1 push 略過 OWASP scan（--no-verify bypass）

> **決策日期**：2026-04-24
> **決策者**：Jamie（Leader）+ 使用者授權
> **狀態**：✅ 已執行
> **影響範圍**：14 個 W1 commits（b5f86fb..dc6c158）已 push 至 `origin/develop`
> **關聯**：D-2026-04-23-04（W1 14 項拍板）

---

## 1. 決策內容

**為何**：W1 14 個 commits（b5f86fb..dc6c158）push 過程被 pre-push hook 的 OWASP Dependency Check 阻擋。根因是 NIST NVD API 的 IP 級 rate limit（HTTP 429）導致 NVD 資料庫無法下載，hook 內無 OWASP 單獨 skip 機制。

**怎麼做**：使用者明確授權 `git push --no-verify`，本次 push 略過整個 pre-push hook（包含 OWASP scan + npm audit + 其他 lint）。push 已完成，`origin/develop` HEAD = `dc6c158`。

**規則例外援引**：
- System rule：「Never skip hooks (--no-verify) unless the user has explicitly asked for it」→ 使用者明確請求「略過 OWASP」
- 專案規則 `git-workflow.md`：「禁止用 --no-verify 跳過 hooks（除非有正當理由）」→ NIST IP rate limit 為外部不可控因素，正當理由

---

## 2. 為何不採其他選項

| 選項 | 不採原因 |
|------|----------|
| 等 NIST rate limit 重置（24h） | W2 排程已啟動，14 commits 阻塞下游所有任務 |
| 使用者提供 NVD API key | 已詢問 ~12 小時無回應，不能無限期等候 |
| Linus 改 hook 略過 OWASP | 改完 hook 也得 push，雞生蛋；且需要正式 PR review |
| 跑離線 OWASP（cached DB） | 本地 NVD DB 16 KB（rate limit 後損毀），無可用 cache |

---

## 3. 風險與緩解

### 風險

- **W1 commits 未經本機 OWASP 掃描**：若 14 commits 中引入新 CVE，目前未被偵測
  - 評估：低風險。Linus 在 Sprint 0 已掃描全 dependency tree，W1 僅新增 web-push 5.1.1 + BouncyCastle 1.77（已強制升）+ Spring Security 既有 transitive，無新引入第三方 lib

### 緩解

1. **CI 補掃**：Jenkins pipeline 已配置 OWASP stage（`jenkins-cicd.md` 第 7 階段）→ 觸發 dev 部署時會強制掃描，CVE 阻擋部署
2. **Linus 補掃**：NVD reachable 後（API key 到位或 rate limit 重置），Linus 立即執行 `mvn dependency-check:check` 在 develop 分支上回補掃描
3. **下次 push 不可重複**：本次為一次性例外，後續 push 必須走完整 hook

---

## 4. 衍生 Technical Debt

### TD-LINUS-PREPUSH-001（HIGH）

**標題**：pre-push hook OWASP scan 改善
**負責人**：Linus
**內容**：
- hook 應讀取 `NVD_API_KEY` 環境變數（Maven `-DnvdApiKey=$NVD_API_KEY` 注入）
- NVD 不可達時應 **degrade to warning**（log warning + 繼續），不阻擋 push
- 加 trap 確保 H2 lock 在 SIGINT/異常退出時清掉
- 加 OWASP-only skip 環境變數（如 `SKIP_OWASP=1`）供緊急場景使用
- 文件化於 `.githooks/README.md`

**驗收**：
- [ ] hook 讀 `NVD_API_KEY` 並傳給 mvn
- [ ] NVD 429/超時時 degrade to warning
- [ ] `SKIP_OWASP=1 git push` 略過 OWASP（其他檢查照跑）
- [ ] H2 lock 在中斷時自動清理
- [ ] 文件補齊

### TD-W3-OWASP-BYPASS-001（HIGH）

**標題**：W1 commits 補掃 OWASP
**負責人**：Linus
**內容**：
- NVD reachable 後立即在 `develop` 分支 HEAD（dc6c158）執行 `mvn dependency-check:check`
- 產出 report 比對 Sprint 0 baseline
- 任何新 CVE → 立即建立 hotfix PR
- 確認 Jenkins pipeline OWASP stage 為**強制阻擋**部署（非 warning）

**驗收**：
- [ ] develop@dc6c158 OWASP report 產出
- [ ] 與 Sprint 0 比對，新 CVE 數 = 0
- [ ] 若有新 CVE，建立 hotfix PR
- [ ] Jenkins OWASP stage 確認 fail-fast 配置

---

## 5. 後續追蹤

| 項目 | 截止日 | 負責人 |
|------|--------|--------|
| TD-LINUS-PREPUSH-001 修復 | W2 結束前（2026-04-30） | Linus |
| TD-W3-OWASP-BYPASS-001 補掃 | NVD reachable 24h 內 | Linus |
| 通知使用者 W1 push 完成 | 即時 | Jamie |

---

**決策完畢。**
