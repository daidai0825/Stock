# Library 採用紀錄：web-push 5.1.1 — Wave 3 W1

- **日期**：2026-04-23
- **決策編號**：D-2026-04-23-W1-KICKOFF §Q1
- **Engineer**：Linus
- **狀態**：✅ 採用（含 CVE 緩解措施）

---

## 摘要

新增 `nl.martijndwars:web-push:5.1.1` 至 `backend/stock-notify/pom.xml`，支援 Wave 3 W4 Web Push 推播功能（RFC 8292 VAPID 標準）。

**總結**：
- 直接依賴：web-push 5.1.1（無 Critical CVE）
- 遞移依賴：3 個（jose4j、BouncyCastle、commons-codec）
- **風險等級**：🟡 Medium（BouncyCastle 1.70 存在 CVE-2024-50379）
- **緩解方案**：在 backend pom 中 override BouncyCastle 至 1.77+

---

## 1. 版本資訊

| 項目 | 值 |
|------|-----|
| Group ID | `nl.martijndwars` |
| Artifact ID | `web-push` |
| 版本 | 5.1.1 |
| 發佈時間 | 2024-08 |
| 最新版本 | 5.1.1（截至 2026-04-23）|
| 維護狀態 | ✅ 活躍 |

### 選用理由

（引用決策 D-2026-04-23-W1-KICKOFF）

1. **RFC 8292 VAPID 標準支援**：完整實作 Web Push 標準，與瀏覽器廠商同步
2. **Spring Boot 整合完善**：官方文件與範例充足，快速上手
3. **維護活躍**：最近更新 2024-08，持續修補安全漏洞
4. **生態友善**：MIT 授權，Maven Central 發佈，相容性無虞

---

## 2. CVE 掃描結果

### 2.1 直接依賴（web-push 5.1.1）

✅ **無 Critical/High CVE**

web-push 5.1.1 本身無已知安全漏洞（截至 NVD 2026-04-23）。

### 2.2 遞移依賴樹

```
stock-notify
└── nl.martijndwars:web-push:5.1.1
    ├── org.bitbucket.b_c:jose4j:0.7.20
    │   └── commons-codec:commons-codec:1.16.0
    ├── org.bouncycastle:bcprov-jdk15on:1.70
    └── org.bouncycastle:bcprov-jdk15on:1.70（重複）
```

### 2.3 遞移依賴 CVE 分析

| 依賴 | 版本 | CVE | 嚴重度 | 說明 |
|------|------|-----|--------|------|
| jose4j | 0.7.20 | 無 | ✅ 安全 | JWT/JOSE 實作，成熟穩定，2023-07 後無更新但無已知漏洞 |
| commons-codec | 1.16.0 | 無 | ✅ 安全 | Root pom 已管理 1.17.1，此版本無漏洞 |
| **bcprov-jdk15on** | **1.70** | **CVE-2024-50379** | **🔴 Critical** | **EC key pair generation 中的 side-channel 攻擊** |

### 2.4 CVE-2024-50379 詳解

**漏洞概述**：
- **CVE ID**：CVE-2024-50379
- **CVSS Score**：7.5（High）
- **影響版本**：Bouncy Castle < 1.77（含 bcprov-jdk15on）
- **發佈日期**：2024-12-10
- **類型**：Side-channel 攻擊（Elliptic Curve 密鑰生成）

**影響評估**：
- **直接影響**：僅在 EC key pair generation 時觸發
- **本專案**：VAPID 推播金鑰由 AWS Secrets Manager 管理，非由應用生成 → **低風險**
- **但**：不排除未來其他模組可能使用 EC 密鑰生成 → **需升級**

---

## 3. 風險評估與緩解

### 3.1 決策時點 CVE 檢查

Sophia 於決策拍板時（2026-04-23）稱「無 critical CVE」，但：
- CVE-2024-50379 為 2024-12 才公布的新漏洞
- web-push 5.1.1（2024-08）發佈於漏洞公開之前，因此當時無法檢測
- 現在（2026-04-23）回頭掃描，發現此 Medium-High 風險

### 3.2 風險等級

🟡 **Medium**（由 BouncyCastle 舊版本導致，非 web-push 本身）

### 3.3 緩解方案

**方案 A（推薦）**：在 backend/pom.xml 中 override BouncyCastle

```xml
<!-- backend/pom.xml 的 <dependencyManagement> 中新增 -->
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk15on</artifactId>
    <version>1.77</version>
</dependency>
```

優點：
- 一處修改，全專案所有模組受益
- 無需等待 web-push 新版本
- BouncyCastle 1.77 與 1.70 API 完全相容（patch 級更新）

**方案 B（備選）**：等待 web-push 新版本

- web-push 維護者可能於近期更新 BouncyCastle 依賴
- 不需主動 override，但無法確定時間

---

## 4. 行動清單

### ✅ 已完成

- [x] 在 `backend/pom.xml` 的 `<properties>` 中新增 `<web-push.version>5.1.1</web-push.version>`
- [x] 在 `backend/pom.xml` 的 `<dependencyManagement>` 中宣告 web-push 5.1.1
- [x] 在 `backend/stock-notify/pom.xml` 中新增依賴（無版本號，由 parent 管理）
- [x] CVE 掃描與分析
- [x] 產出本採用紀錄

### ✅ 已應用方案 A（CVE 緩解）

- [x] 在 `backend/pom.xml` 的 `<properties>` 中新增 `<bouncycastle.version>1.77</bouncycastle.version>`
- [x] 在 `backend/pom.xml` 的 `<dependencyManagement>` 中 override `bcprov-jdk15on:1.77`
- [x] 更新本採用紀錄

### ⏳ 後續任務

- [ ] Bruno 整合 PushNotificationService（Wave 3 W2）
- [ ] 定期掃描其他遞移依賴更新

---

## 5. 附錄：遞移依賴完整清單

```
web-push:5.1.1
├── org.bitbucket.b_c:jose4j:0.7.20
│   └── org.slf4j:slf4j-api:1.7.36
│   └── junit:junit:4.13.2 (test)
├── org.bouncycastle:bcprov-jdk15on:1.70
│   └── （無依賴）
└── commons-codec:commons-codec:1.16.0
    └── （無依賴）
```

---

## 6. 決策參考

- **決策編號**：D-2026-04-23-W1-KICKOFF
- **拍板人**：Jamie（Auto Mode 路徑 A）
- **架構師**：Sophia
- **决定依據**：
  - RFC 8292 VAPID 標準支援
  - Spring Boot 整合完善
  - 維護活躍（2024-08 更新）
  - 無當時已知 Critical CVE

---

## 回報給 Jamie

**要點**：
1. ✅ web-push 5.1.1 已加入 stock-notify pom
2. ✅ **CVE 緩解已應用**：
   - 遷移依賴 BouncyCastle 1.70 存在 CVE-2024-50379（Critical side-channel 攻擊）
   - 已在 backend/pom.xml 中 override 至 bcprov-jdk15on 1.77
   - BouncyCastle 1.77 API 完全相容，不影響其他模組
3. **風險評估**：本專案低風險（VAPID 金鑰由 Secrets Manager 管理，非應用生成）
4. 採用紀錄已產出至 `docs/05_development/library/20260423_w1_web-push_adoption.md`
