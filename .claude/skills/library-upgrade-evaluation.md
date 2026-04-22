---
name: library-upgrade-evaluation
description: 套件升級評估模板。由 Linus（套件管家）使用，每週掃描傳遞依賴與 CVE，產出升級風險評估報告，含風險分級與升級計畫。
---

# Skill: Library Upgrade Evaluation

## 使用時機

- **使用者**：Linus（套件管家）
- **輸出路徑**：
  - 週報：`docs/10_library/weekly/YYYYMMDD_LibraryReport.md`
  - 升級評估：`docs/10_library/upgrade/YYYYMMDD_UPGRADE-{lib}_v{from}_to_v{to}.md`
  - CVE 通報：`docs/10_library/cve/YYYYMMDD_CVE-{id}.md`
- **觸發時機**：
  - 每週一固定掃描（含傳遞依賴 + CVE）
  - CVE 公告即時通報
  - Renovate / Dependabot PR 評估

---

## 套件分類

| 類別 | 範例 | 升級頻率 | 風險容忍 |
|------|------|----------|----------|
| **Core**（語言/框架） | Java, Spring Boot, React | 每年 1-2 次 | 低 |
| **Infrastructure** | MyBatis, Redis client, AWS SDK | 半年 | 低 |
| **Security** | Spring Security, jjwt, BCrypt | 立即（CVE） | 零 |
| **Utility** | Lombok, MapStruct, Lodash | 季 | 中 |
| **Dev tool** | JUnit, Mockito, Vitest | 半年 | 高 |
| **UI library** | Ant Design, dayjs | 季 | 中 |

---

## 模板 1：每週套件報告

```markdown
# 套件管理週報 - YYYY-WW

- **撰寫者**：Linus
- **掃描日期**：YYYY-MM-DD
- **掃描範圍**：所有 prod 專案

---

## 1. 摘要

| 指標 | 數量 |
|------|------|
| 直接依賴 | 87 |
| 傳遞依賴 | 412 |
| 過時直接依賴（minor） | 12 |
| 過時直接依賴（major） | 5 |
| 🔴 已知 CVE（嚴重） | 1 |
| 🟡 已知 CVE（中等） | 3 |
| 🟢 已知 CVE（低） | 8 |
| 升級建議數 | 6 |

**本週重點**：
- 🔴 jackson-databind CVE-2024-XXXXX，建議 7 天內升級
- 🟡 Spring Boot 3.2.5 釋出，含 4 個安全修復
- 過時 ≥ 6 個月套件 8 個，建議排入下季

---

## 2. CVE 警示

### 🔴 CVE-2024-XXXXX - jackson-databind

| 項目 | 內容 |
|------|------|
| CVSS | 9.8（Critical） |
| 受影響版本 | 2.15.0 - 2.16.1 |
| 修復版本 | 2.16.2+ |
| 我們使用版本 | 2.15.4 ✅ 受影響 |
| 利用條件 | 反序列化不受信任 input |
| 實際風險 | 中（API 有反序列化邊界） |
| 建議 | 7 天內升級 |
| 升級難度 | 🟢 低（patch 版本） |

→ 詳見 `docs/10_library/cve/20260421_CVE-2024-XXXXX.md`

### 🟡 CVE-2024-YYYYY - Spring Security

[類似結構]

---

## 3. 過時套件清單

### 3.1 後端（Java）

| 套件 | 目前版本 | 最新版本 | 落後 | 類別 | 風險 | 建議 |
|------|----------|----------|------|------|------|------|
| spring-boot-starter | 3.2.0 | 3.2.5 | 5 個 patch | Core | 🟡 | 排入本季 |
| mybatis-spring-boot | 3.0.2 | 3.0.3 | 1 patch | Infra | 🟢 | 排入下次 release |
| oracle-jdbc | 23.3.0 | 23.4.0 | 1 minor | Infra | 🟡 | 評估後升級 |
| jackson-databind | 2.15.4 | 2.16.2 | 1 minor | Core | 🔴 CVE | 7 天內 |
| lombok | 1.18.30 | 1.18.32 | 2 patch | Utility | 🟢 | 排入下季 |

### 3.2 前端（npm）

| 套件 | 目前版本 | 最新版本 | 落後 | 類別 | 風險 | 建議 |
|------|----------|----------|------|------|------|------|
| react | 18.2.0 | 18.3.1 | 1 minor | Core | 🟢 | 下季升級 |
| antd | 5.13.0 | 5.16.0 | 3 minor | UI | 🟡 | 排入本季 |
| axios | 1.6.0 | 1.6.8 | 8 patch | Infra | 🟡 | 排入本月 |
| @tanstack/react-query | 5.20.0 | 5.32.0 | 12 minor | Infra | 🟡 | 排入本季 |
| dayjs | 1.11.10 | 1.11.11 | 1 patch | Utility | 🟢 | 隨手升 |

---

## 4. 傳遞依賴掃描

| 套件 | 引入者 | 版本 | CVE | 處置 |
|------|--------|------|-----|------|
| commons-text | spring-boot | 1.10.0 | CVE-2022-42889 | 強制覆蓋 1.11.0 |
| guava | aws-sdk | 31.0 | - | 評估升級 33.x |

### 強制覆蓋（pom.xml）

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.apache.commons</groupId>
            <artifactId>commons-text</artifactId>
            <version>1.11.0</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## 5. 授權檢查

| 套件 | 授權 | 相容性 |
|------|------|--------|
| 所有直接依賴 | Apache 2.0 / MIT / BSD | ✅ 相容 |
| oracle-jdbc | Oracle Free Use | ⚠️ 需確認商用條款 |
| 無 GPL / AGPL | - | ✅ |

---

## 6. 本週行動項

| # | 行動 | 負責 | 期限 | 狀態 |
|---|------|------|------|------|
| 1 | 升級 jackson-databind 至 2.16.2 | Bruno | 2026-04-28 | 🟡 進行中 |
| 2 | 評估 Spring Boot 3.2.5 升級 | Linus | 2026-04-25 | 🟡 進行中 |
| 3 | 強制覆蓋 commons-text | Bruno | 2026-04-22 | 🔴 待開始 |
| 4 | 評估 antd 升級至 5.16.0 | Felix | 2026-05-05 | 🔴 待開始 |

---

## 7. 掃描工具

| 工具 | 用途 | 執行命令 |
|------|------|----------|
| OWASP Dependency-Check | CVE 掃描 | `mvn dependency-check:check` |
| Snyk | CVE + 授權 | `snyk test` |
| npm audit | npm CVE | `npm audit` |
| Dependabot | 自動 PR | GitHub 設定 |
| Renovate | 自動 PR | renovate.json |

---

## 8. 下週預定

- [ ] 完成本週行動項
- [ ] Spring Boot 3.2.5 PoC（dev 環境）
- [ ] 全面檢視傳遞依賴 ≥ 3 年未更新者
```

---

## 模板 2：升級評估報告

```markdown
# 套件升級評估 - {套件名} v{from} → v{to}

- **撰寫者**：Linus
- **撰寫日期**：YYYY-MM-DD
- **狀態**：🟡 評估中 / 🟢 通過 / 🔴 暫緩

---

## 1. 升級資訊

| 項目 | 內容 |
|------|------|
| 套件名稱 | spring-boot-starter |
| 目前版本 | 3.2.0 |
| 目標版本 | 3.2.5 |
| 升級類型 | Patch |
| Release notes | https://github.com/spring-projects/spring-boot/releases/tag/v3.2.5 |
| 本次差異 | 87 commits, 4 安全修復, 23 bug fix |
| 升級理由 | 安全修復 + 累積 bug fix |

---

## 2. 風險評估

| 維度 | 評分 | 說明 |
|------|------|------|
| Breaking change | 🟢 1/5 | Patch 版本，無 API 破壞 |
| 傳遞依賴影響 | 🟡 2/5 | 帶動 jackson 升級 |
| 編譯影響 | 🟢 1/5 | 無 |
| 測試影響 | 🟢 1/5 | 既有測試應全通過 |
| 效能影響 | 🟢 1/5 | 無已知 regression |
| 行為變更 | 🟡 2/5 | actuator 預設行為微調 |
| **總評** | **🟢 1.3/5** | **低風險** |

---

## 3. 影響分析

### 3.1 受影響的功能

- [ ] 啟動流程（無變化）
- [ ] Actuator endpoints（health 預設改為 simple）
- [ ] 自動配置（無變化）
- [ ] DataSource（無變化）

### 3.2 我們專案的觸點

| 模組 | 影響 | 處置 |
|------|------|------|
| my-project-api | 🟢 無 | - |
| my-project-service | 🟢 無 | - |
| my-project-config | 🟡 actuator 配置需檢查 | 補上 `management.endpoint.health.show-details=when-authorized` |

---

## 4. 測試計畫

| 階段 | 動作 | 負責 |
|------|------|------|
| 1 | 建立 feature branch `chore/upgrade-spring-boot-3.2.5` | Bruno |
| 2 | 修改 pom.xml，重新 build | Bruno |
| 3 | 跑全套單元測試 | Jenkins |
| 4 | 部署 dev，跑回歸測試（QA） | Quincy |
| 5 | 部署 uat，使用者驗證 | QA + 業務 |
| 6 | Merge 到 develop，等下次 release | Bruno |

---

## 5. Rollback 計畫

```bash
# 1. revert commit
git revert <upgrade-commit>

# 2. push
git push

# 3. CI 自動部署回舊版
```

---

## 6. 決策

| 角色 | 簽核 | 日期 |
|------|------|------|
| Linus（評估） | ✅ | YYYY-MM-DD |
| Sophia（架構影響） | ✅ | YYYY-MM-DD |
| Brian（程式碼影響） | ✅ | YYYY-MM-DD |
| Jamie（最終） | ✅ | YYYY-MM-DD |

**結論**：✅ 通過，排入 release v1.5.0

---

## 7. 升級後追蹤

| 項目 | 觀察期 | 結果 |
|------|--------|------|
| Error rate | 7 天 | TBD |
| Latency | 7 天 | TBD |
| Memory | 7 天 | TBD |
| 啟動時間 | 7 天 | TBD |

7 天無異常即視為穩定。
```

---

## 模板 3：CVE 即時通報

```markdown
# CVE 通報 - CVE-2024-XXXXX

- **公告日期**：YYYY-MM-DD
- **撰寫者**：Linus
- **緊急程度**：🔴 嚴重 / 🟡 中等 / 🟢 低

---

## 1. CVE 基本資訊

| 項目 | 內容 |
|------|------|
| CVE ID | CVE-2024-XXXXX |
| 套件 | jackson-databind |
| CVSS 3.1 | 9.8（Critical） |
| 攻擊向量 | Network |
| 攻擊複雜度 | Low |
| 需要權限 | None |
| 使用者互動 | None |
| 影響範圍 | Confidentiality + Integrity + Availability |
| 公告連結 | https://nvd.nist.gov/vuln/detail/CVE-2024-XXXXX |

---

## 2. 漏洞描述

jackson-databind 在處理特定多型反序列化時，攻擊者可注入惡意類別觸發 RCE。

---

## 3. 受影響範圍評估

### 3.1 我們是否使用受影響版本

| 專案 | 版本 | 受影響？ |
|------|------|----------|
| project-a | 2.15.4 | ✅ 是 |
| project-b | 2.14.2 | ✅ 是 |
| project-c | 2.16.3 | ❌ 否 |

### 3.2 我們是否觸發攻擊條件

| 條件 | 我們情況 |
|------|----------|
| 有反序列化來自不受信任來源 | ⚠️ API 接受 JSON |
| 有開啟 default typing | ✅ 預設關閉 |
| 有自定義 polymorphic deserialization | ⚠️ 部分 endpoint |

**實際風險**：🟡 中（部分 endpoint 有風險）

---

## 4. 緩解措施

### 4.1 立即（24 小時內）

- [ ] WAF 增加規則攔截可疑 polymorphic payload
- [ ] 暫停受影響 endpoint（如可）

### 4.2 短期（7 天內）

- [ ] 升級 jackson-databind 至 2.16.2+
- [ ] 全部專案部署

### 4.3 長期（1 個月）

- [ ] 加入自動 CVE 掃描到 CI
- [ ] 建立 CVE response runbook

---

## 5. 升級指引

```xml
<!-- pom.xml 強制覆蓋 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>2.16.2</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

---

## 6. 行動項追蹤

| # | 動作 | 負責 | 期限 | 狀態 |
|---|------|------|------|------|
| 1 | WAF 規則部署 | DevOps | 2026-04-22 | 🔴 待開始 |
| 2 | project-a 升級 | Bruno | 2026-04-25 | 🔴 待開始 |
| 3 | project-b 升級 | Bruno | 2026-04-28 | 🔴 待開始 |
| 4 | 自動掃描 CI | Linus + DevOps | 2026-05-15 | 🔴 待開始 |

---

## 7. 通知對象

- [x] Jamie（已通報）
- [x] Bruno（已通報）
- [x] Brian（已通報）
- [x] DevOps（已通報）
- [ ] 客戶（評估中）

---

## 8. 後續追蹤

| 日期 | 狀態 |
|------|------|
| YYYY-MM-DD | CVE 公告 |
| YYYY-MM-DD | WAF 規則部署完成 |
| YYYY-MM-DD | project-a 升級完成 |
| YYYY-MM-DD | 全部專案升級完成 |
| YYYY-MM-DD | CVE 關閉 |
```

---

## 升級分級判定標準

| 等級 | 處理時程 | 範例 |
|------|----------|------|
| 🔴 緊急 | 7 天內 | CVE Critical（CVSS ≥ 9.0）且實際可利用 |
| 🟠 急 | 30 天內 | CVE High（7.0-8.9）或 break change 安全相關 |
| 🟡 中 | 季 | minor 版本含 bug fix |
| 🟢 低 | 半年 | patch 版本、純改善 |

---

## 升級決策矩陣

| 情境 | 動作 |
|------|------|
| Patch 版本 + 無 break + 含 bug fix | 直接升級 |
| Minor 版本 + 無 break | 排入下次 release |
| Major 版本 | 必須完整評估 + Sophia/Preston 簽核 |
| 含 CVE | 緊急升級流程 |
| EOL 套件 | 找替代方案，列入專案 backlog |

---

## 自動化工具配置

### Renovate

```json
{
  "extends": ["config:base"],
  "schedule": ["before 5am on monday"],
  "vulnerabilityAlerts": {
    "labels": ["security"],
    "schedule": ["at any time"]
  },
  "packageRules": [
    {
      "matchUpdateTypes": ["patch"],
      "automerge": true,
      "automergeType": "pr"
    },
    {
      "matchUpdateTypes": ["minor"],
      "automerge": false
    },
    {
      "matchUpdateTypes": ["major"],
      "automerge": false,
      "labels": ["major-upgrade", "needs-review"]
    }
  ]
}
```

### Maven 掃描

```bash
# Dependency Check
mvn org.owasp:dependency-check-maven:check \
  -DfailBuildOnCVSS=7

# Versions 報告
mvn versions:display-dependency-updates
mvn versions:display-plugin-updates
```

### npm 掃描

```bash
npm audit --audit-level=moderate
npm outdated
npx depcheck  # 找未使用的依賴
```

---

## 撰寫要點

1. **量化風險**：CVSS、實際可利用性、影響範圍
2. **行動明確**：每個發現都有負責人與期限
3. **追蹤完整**：從發現到關閉全紀錄
4. **自動化優先**：能讓工具做的不要手動

## 禁止事項

- 禁止對 CVE 視而不見
- 禁止 patch 版本拖延超過 1 個月
- 禁止 major 升級無評估直接做
- 禁止套件版本不上 lock file
- 禁止使用已 EOL 的套件
- 禁止傳遞依賴 CVE 不處理
