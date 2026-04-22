---
name: test-case-template
description: 測試案例模板。由 Quincy 與 Quinn（QA）使用，產出涵蓋手動測試、Playwright E2E、JMeter 壓測、Postman API 測試的完整測試規劃。
---

# Skill: Test Case Template

## 使用時機

- **使用者**：Quincy（QA #1）、Quinn（QA #2）
- **輸出路徑**：
  - 測試計畫：`docs/07_test/plan/YYYYMMDD_TestPlan_{feature}.md`
  - 手動測試：`docs/07_test/manual/YYYYMMDD_TC_{feature}.md`
  - 自動化（Playwright）：`tests/e2e/{feature}.spec.ts`
  - 壓測（JMeter）：`tests/perf/{feature}.jmx`
  - API 測試（Postman）：`tests/api/{feature}.postman_collection.json`
- **觸發時機**：SRS 定稿後即可開始撰寫；功能完成後執行

---

## 模板 1：測試計畫

```markdown
# 測試計畫：{功能名稱}

- **文件版本**：v1.0
- **撰寫者**：Quincy / Quinn
- **撰寫日期**：YYYY-MM-DD
- **對應 SRS**：[link]
- **對應 PR**：#{number}

---

## 1. 測試範圍

### 1.1 範圍內

- 註冊功能（含驗證信流程）
- 登入功能
- Email 驗證連結

### 1.2 範圍外

- 第三方 SSO（本次未實作）
- 密碼重設（屬另一個 PR）

---

## 2. 測試類型

| 類型 | 工具 | 負責人 | 案例數 |
|------|------|--------|--------|
| 手動測試 | - | Quincy | 24 |
| 自動化 E2E | Playwright | Quinn | 12 |
| API 測試 | Postman | Quincy | 18 |
| 壓力測試 | JMeter | Quinn | 3 場景 |
| 安全性測試 | OWASP ZAP | Quincy | 1 全掃 |

---

## 3. 環境矩陣

| 環境 | URL | 用途 | 資料 |
|------|-----|------|------|
| local | http://localhost:8080 | 開發者自測 | 假資料 |
| dev | https://dev.example.com | 整合測試 | 假資料 |
| uat | https://uat.example.com | 驗收測試 | 半真實資料 |

---

## 4. 瀏覽器矩陣（前端）

| 瀏覽器 | 版本 | 優先級 |
|--------|------|--------|
| Chrome | 最新 + 前 1 版 | P0 |
| Edge | 最新 | P0 |
| Safari | 最新 | P1 |
| Firefox | 最新 | P2 |

行動裝置：

| 裝置 | OS | 優先級 |
|------|-----|--------|
| iPhone 15 | iOS 17 | P0 |
| Pixel 8 | Android 14 | P0 |
| iPad | iPadOS 17 | P1 |

---

## 5. 測試資料準備

| 資料類型 | 來源 | 維護 |
|----------|------|------|
| 測試帳號 | docs/07_test/data/users.csv | Quincy |
| 商品資料 | docs/07_test/data/products.csv | Quinn |
| Mock API | WireMock | Quincy |

---

## 6. 進入/退出條件

### 進入條件

- [ ] PR 已通過 Code Review
- [ ] 開發環境部署成功
- [ ] 測試資料已準備
- [ ] SRS 已定稿

### 退出條件

- [ ] 所有 P0 案例 100% 通過
- [ ] P1 案例 ≥ 90% 通過
- [ ] 無 Blocker bug
- [ ] 壓測達標
- [ ] QA 互審完成

---

## 7. 互審分工

| 範圍 | 撰寫者 | 互審者 |
|------|--------|--------|
| 註冊手動案例 | Quincy | Quinn |
| 登入 E2E | Quinn | Quincy |
| API 測試 | Quincy | Quinn |
| 壓測腳本 | Quinn | Quincy |

---

## 8. 風險

| 風險 | 緩解 |
|------|------|
| Email 服務 dev 環境不穩 | 用 Mailtrap 替代 |
| 壓測影響 dev 環境 | 開獨立 perf 環境 |
| 假資料外洩 | 不使用真實 PII |

---

## 9. 預估時程

| 階段 | 天數 |
|------|------|
| 案例撰寫 | 2 |
| 互審 | 1 |
| 執行 | 3 |
| 缺失修復回測 | 2 |
| **合計** | **8 天** |
```

---

## 模板 2：手動測試案例

```markdown
# 手動測試案例：{功能名稱}

- **撰寫者**：Quincy
- **互審者**：Quinn
- **日期**：YYYY-MM-DD
- **對應 SRS**：[link]

---

## 案例索引

| TC-ID | 名稱 | 優先級 | 類型 | 狀態 |
|-------|------|--------|------|------|
| TC-001 | 註冊成功 - 標準路徑 | P0 | 正向 | ☐ |
| TC-002 | 註冊失敗 - Email 已存在 | P0 | 反向 | ☐ |
| TC-003 | 註冊失敗 - 密碼強度不足 | P0 | 反向 | ☐ |
| TC-004 | 註冊邊界 - Email 255 字元 | P1 | 邊界 | ☐ |
| TC-005 | 註冊邊界 - 密碼 12 字元剛好 | P1 | 邊界 | ☐ |
| TC-006 | UI - 表單未填無法送出 | P0 | UI | ☐ |
| TC-007 | UI - 密碼強度條即時更新 | P1 | UI | ☐ |
| TC-008 | A11y - 鍵盤可完成註冊 | P1 | 無障礙 | ☐ |

---

## TC-001：註冊成功 - 標準路徑

| 項目 | 內容 |
|------|------|
| ID | TC-001 |
| 優先級 | P0 |
| 類型 | 正向 |
| 對應 AC | AC-001 |
| 前置條件 | 1. 使用者尚未註冊<br>2. 開啟 https://dev.example.com/register |

### 測試步驟

| # | 操作 | 預期結果 |
|---|------|----------|
| 1 | 在 Email 欄輸入 `tc001@example.com` | 欄位顯示綠色邊框（驗證通過） |
| 2 | 在密碼欄輸入 `ValidP@ss123!` | 強度條顯示「強」 |
| 3 | 在確認密碼欄輸入 `ValidP@ss123!` | 顯示「密碼一致」 |
| 4 | 在顯示名稱輸入 `TC001 User` | 無錯誤 |
| 5 | 勾選「我同意服務條款」 | Checkbox 變藍 |
| 6 | 點擊「註冊」 | 1. 按鈕顯示 spinner<br>2. 3 秒內導向 /verify-email |
| 7 | 檢查 Mailtrap | 收到驗證信，主旨為「請驗證您的信箱」 |
| 8 | 開啟 DB 查詢 | USER_INFO 有一筆 `status=UNVERIFIED` 的紀錄 |

### 驗證點

- [ ] HTTP 請求 status = 200
- [ ] Response body 含 `code: 0`
- [ ] Response body 含 `userId`（UUID 格式）
- [ ] traceId 不為空
- [ ] 驗證信寄達時間 < 30 秒

### 截圖

- screenshot/tc-001-step6.png
- screenshot/tc-001-mailtrap.png

### 備註

- Mailtrap 帳號：見 docs/07_test/credentials.md
- DB 查詢 SQL：`SELECT * FROM USER_INFO WHERE email = 'tc001@example.com';`

---

## TC-002：註冊失敗 - Email 已存在

| 項目 | 內容 |
|------|------|
| ID | TC-002 |
| 優先級 | P0 |
| 類型 | 反向 |
| 對應 AC | AC-002 |
| 前置條件 | DB 中存在 email = `existing@example.com` 的帳號 |

### 測試步驟

| # | 操作 | 預期結果 |
|---|------|----------|
| 1 | 輸入 email `existing@example.com` | 欄位驗證通過 |
| 2 | 輸入合法密碼 `ValidP@ss123!` | 強度通過 |
| 3 | 完成其他欄位 | 表單可送出 |
| 4 | 點擊「註冊」 | 1. API 回傳 code=2010<br>2. 頁面頂部顯示紅色 alert：「此 Email 已被註冊」<br>3. 留在註冊頁<br>4. DB 不新增資料 |

### 驗證點

- [ ] HTTP status = 200（業務錯誤仍 200）
- [ ] code = 2010
- [ ] message = "Email 已被註冊"
- [ ] 不寄送驗證信
- [ ] 不寫入 DB

---

## TC-003：註冊失敗 - 密碼強度不足

[類似結構，覆蓋 6 種弱密碼變體]

| 變體 | 密碼 | 預期錯誤 |
|------|------|----------|
| 太短 | `Abc123!` | 1002 - 密碼最少 12 字元 |
| 無大寫 | `validp@ss123!` | 1002 - 必含大寫 |
| 無小寫 | `VALIDP@SS123!` | 1002 - 必含小寫 |
| 無數字 | `ValidP@ssword!` | 1002 - 必含數字 |
| 無特殊字元 | `ValidPass1234` | 1002 - 必含特殊字元 |
| 與 email 相同 | `tc003@example.com` | 1002 - 不能與 email 相同 |

---

## TC-008：A11y - 鍵盤可完成註冊

| 項目 | 內容 |
|------|------|
| 工具 | NVDA / VoiceOver |
| 標準 | WCAG 2.1 AA |

### 測試步驟

| # | 操作 | 預期結果 |
|---|------|----------|
| 1 | 用 Tab 鍵移動 | Focus 順序：Email → 密碼 → 確認密碼 → 顯示名稱 → 條款 → 註冊 |
| 2 | 螢幕報讀 Email 欄 | 朗讀「Email，編輯，必填」 |
| 3 | 輸入錯誤 email | 朗讀錯誤訊息（aria-live） |
| 4 | 用 Space 勾選條款 | Checkbox 切換 |
| 5 | 用 Enter 送出 | 表單送出 |

---

## 缺失追蹤

| 缺失 ID | TC ID | 描述 | 嚴重度 | 狀態 | 負責人 |
|---------|-------|------|--------|------|--------|
| BUG-001 | TC-002 | Alert 訊息消失太快 | 🟡 Major | 已修 | Felix |

---

**簽核**：Quincy + Quinn 互審完成
**日期**：YYYY-MM-DD
```

---

## 模板 3：Playwright E2E 測試

```typescript
// tests/e2e/register.spec.ts
import { test, expect } from '@playwright/test';

test.describe('使用者註冊流程', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/register');
  });

  test('TC-001: 標準註冊成功', async ({ page }) => {
    await page.getByLabel('Email').fill('e2e-001@example.com');
    await page.getByLabel('密碼', { exact: true }).fill('ValidP@ss123!');
    await page.getByLabel('確認密碼').fill('ValidP@ss123!');
    await page.getByLabel('顯示名稱').fill('E2E User');
    await page.getByRole('checkbox', { name: '我同意服務條款' }).check();

    const responsePromise = page.waitForResponse('**/api/v1/user/register');
    await page.getByRole('button', { name: '註冊' }).click();

    const response = await responsePromise;
    expect(response.status()).toBe(200);
    const body = await response.json();
    expect(body.code).toBe(0);
    expect(body.data.userId).toMatch(/^[0-9a-f-]{36}$/);

    await expect(page).toHaveURL('/verify-email');
    await expect(page.getByText('請至信箱完成驗證')).toBeVisible();
  });

  test('TC-002: Email 已存在', async ({ page }) => {
    await page.getByLabel('Email').fill('existing@example.com');
    await page.getByLabel('密碼', { exact: true }).fill('ValidP@ss123!');
    await page.getByLabel('確認密碼').fill('ValidP@ss123!');
    await page.getByRole('checkbox', { name: '我同意服務條款' }).check();
    await page.getByRole('button', { name: '註冊' }).click();

    await expect(page.getByRole('alert')).toContainText('此 Email 已被註冊');
    await expect(page).toHaveURL('/register');
  });

  test('TC-003a: 密碼太短被擋下', async ({ page }) => {
    await page.getByLabel('Email').fill('e2e-003@example.com');
    await page.getByLabel('密碼', { exact: true }).fill('Short1!');

    await expect(page.getByText('密碼最少 12 字元')).toBeVisible();
    await expect(page.getByRole('button', { name: '註冊' })).toBeDisabled();
  });
});

test.describe('響應式設計', () => {
  test('iPhone 15 註冊頁正常顯示', async ({ browser }) => {
    const context = await browser.newContext({
      viewport: { width: 390, height: 844 },
      userAgent: 'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0)',
    });
    const page = await context.newPage();
    await page.goto('/register');
    await expect(page.getByLabel('Email')).toBeVisible();
  });
});
```

### Playwright 配置

```typescript
// playwright.config.ts
import { defineConfig, devices } from '@playwright/test';

export default defineConfig({
  testDir: './tests/e2e',
  fullyParallel: true,
  retries: process.env.CI ? 2 : 0,
  reporter: [['html'], ['junit', { outputFile: 'test-results/junit.xml' }]],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'https://dev.example.com',
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    { name: 'webkit', use: { ...devices['Desktop Safari'] } },
    { name: 'iPhone 15', use: { ...devices['iPhone 15'] } },
  ],
});
```

---

## 模板 4：JMeter 壓測

### 場景設計

| 場景 | 並發 | 持續時間 | 目標 |
|------|------|----------|------|
| 註冊 baseline | 50 RPS | 10 min | p95 < 2s, error < 1% |
| 註冊 peak | 100 RPS | 5 min | p95 < 3s, error < 5% |
| 註冊 stress | 200 RPS | 2 min | 找出極限 |

### JMX 結構（重點）

```xml
<TestPlan>
  <ThreadGroup>
    <stringProp name="ThreadGroup.num_threads">100</stringProp>
    <stringProp name="ThreadGroup.ramp_time">60</stringProp>
    <stringProp name="ThreadGroup.duration">600</stringProp>
  </ThreadGroup>

  <CSVDataSet>
    <stringProp name="filename">testdata/registrations.csv</stringProp>
    <stringProp name="variableNames">email,password</stringProp>
  </CSVDataSet>

  <HTTPSamplerProxy>
    <stringProp name="HTTPSampler.domain">${BASE_URL}</stringProp>
    <stringProp name="HTTPSampler.path">/api/v1/user/register</stringProp>
    <stringProp name="HTTPSampler.method">POST</stringProp>
    <elementProp name="HTTPsampler.Arguments">
      <collectionProp>
        <elementProp>
          <stringProp name="Argument.value">{"email":"${email}","password":"${password}"}</stringProp>
        </elementProp>
      </collectionProp>
    </elementProp>
  </HTTPSamplerProxy>

  <ResponseAssertion>
    <stringProp>Test"code":0</stringProp>
  </ResponseAssertion>
</TestPlan>
```

### 執行與報告

```bash
jmeter -n -t register-baseline.jmx \
  -JBASE_URL=dev.example.com \
  -l results/baseline.jtl \
  -e -o reports/baseline-html
```

### 驗收指標

| 指標 | 目標 | 工具 |
|------|------|------|
| p50 latency | < 500ms | JMeter |
| p95 latency | < 2000ms | JMeter |
| p99 latency | < 5000ms | JMeter |
| Error rate | < 1% | JMeter |
| Throughput | ≥ 100 RPS | JMeter |
| CPU（ECS） | < 70% | CloudWatch |
| RDS connections | < 80% pool | CloudWatch |

---

## 模板 5：Postman API 測試

### Collection 結構

```
{feature}.postman_collection.json
├── Auth
│   ├── Login (取得 token)
│   └── Refresh Token
├── User Register
│   ├── 01 - 註冊成功
│   ├── 02 - Email 已存在
│   ├── 03 - 密碼強度不足（6 種變體）
│   ├── 04 - 必填缺失
│   └── 05 - SQL Injection 測試
└── User Verify
    ├── 06 - Token 有效
    └── 07 - Token 過期
```

### Request 範例

```json
{
  "name": "01 - 註冊成功",
  "request": {
    "method": "POST",
    "header": [{ "key": "Content-Type", "value": "application/json" }],
    "url": "{{baseUrl}}/api/v1/user/register",
    "body": {
      "mode": "raw",
      "raw": "{\"email\":\"{{$randomEmail}}\",\"password\":\"ValidP@ss123!\"}"
    }
  },
  "event": [
    {
      "listen": "test",
      "script": {
        "exec": [
          "pm.test('HTTP 200', () => pm.response.to.have.status(200));",
          "const json = pm.response.json();",
          "pm.test('code = 0', () => pm.expect(json.code).to.eql(0));",
          "pm.test('userId is UUID', () => pm.expect(json.data.userId).to.match(/^[0-9a-f-]{36}$/));",
          "pm.test('traceId exists', () => pm.expect(json.traceId).to.not.be.empty);",
          "pm.test('latency < 2s', () => pm.expect(pm.response.responseTime).to.be.below(2000));",
          "pm.collectionVariables.set('lastUserId', json.data.userId);"
        ]
      }
    }
  ]
}
```

### Newman CI 整合

```bash
newman run register.postman_collection.json \
  --environment dev.postman_environment.json \
  --reporters cli,junit,htmlextra \
  --reporter-junit-export results/api-tests.xml
```

---

## QA 互審清單

| 項目 | 自查 | 互審 |
|------|------|------|
| 案例覆蓋所有 AC | ☐ | ☐ |
| P0 案例齊全 | ☐ | ☐ |
| 邊界條件覆蓋 | ☐ | ☐ |
| 反向案例完整 | ☐ | ☐ |
| 預期結果可驗證 | ☐ | ☐ |
| 截圖/錄影齊全 | ☐ | ☐ |
| 自動化腳本可重跑 | ☐ | ☐ |
| 壓測腳本資料隔離 | ☐ | ☐ |
| 無 Hardcode 帳密 | ☐ | ☐ |

---

## 撰寫要點

1. **案例可重現**：步驟具體、預期明確、資料齊全
2. **覆蓋面廣**：正向 + 反向 + 邊界 + 例外
3. **自動化優先**：穩定的回歸用 Playwright 自動化
4. **互審強制**：兩位 QA 互審後才視為定稿
5. **資料隔離**：壓測與自動化用獨立帳號池

## 禁止事項

- 禁止只有正向案例
- 禁止案例描述含糊（「測一下」「檢查看看」）
- 禁止用真實使用者資料
- 禁止省略預期結果
- 禁止跳過 QA 互審
- 禁止在程式碼提交真實帳密
