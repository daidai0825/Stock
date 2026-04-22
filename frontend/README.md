# 台股股票分析平台 — 前端

Stage 4 / Wave A：前端基礎與 M-MEMBER / M-WATCHLIST 骨架。

- 技術棧：React 19 + TypeScript 5 + Vite 6 + Ant Design 5 + Zustand + TanStack Query
- 平台形態：純 Web Responsive（依 [decision-platform-form](../docs/01_leader/decisions/20260422_decision-platform-form.md)，無 React Native）
- API：`/api/v1/{module}/{action}` 全 POST + Envelope（code/message/data）+ HTTP 200

---

## 啟動步驟

### 必備環境

| 項目 | 版本 |
|------|------|
| Node.js | >= 20 (LTS) |
| npm | >= 10 |

### 安裝與執行

```bash
# 1. 進入前端目錄
cd frontend

# 2. 安裝依賴（首次）
npm ci   # 嚴格依 package-lock.json，CI 也用此

# 開發階段可用：
npm install

# 3. 啟動本地開發伺服器（預設 http://localhost:5173）
npm run dev

# 4. 型別檢查
npm run type-check

# 5. ESLint
npm run lint

# 6. 單元測試
npm test

# 7. 正式打包
npm run build
```

### 環境變數

| 變數 | 必填 | 說明 |
|------|------|------|
| `VITE_API_BASE_URL` | Y | 後端 base URL，例：`http://localhost:8080` |
| `VITE_LOG_LEVEL` | N | `debug` / `info` / `warn` / `error` |
| `VITE_APP_ENV` | N | `local` / `dev` / `prod` 等 |

對應檔案：

- `.env.local`（本機，不 commit；目前已 commit 範本）
- `.env.development`（dev 環境）
- `.env.production`（prod 環境，由 CI 注入，**不 commit**）

---

## 目錄結構

```
frontend/
├── src/
│   ├── App.tsx                 # 根 App，注入 QueryClient + ConfigProvider
│   ├── main.tsx                # ReactDOM 入口
│   ├── routes/
│   │   └── AppRouter.tsx       # 公開 / 受保護 路由
│   ├── pages/
│   │   ├── Login/              # M-MEMBER 登入
│   │   ├── Register/           # M-MEMBER 註冊
│   │   ├── Home/               # 首頁總覽
│   │   ├── Watchlist/          # M-WATCHLIST 自選股（Wave A 骨架）
│   │   └── Profile/            # 個人資料
│   ├── components/
│   │   ├── common/Layout/      # AppLayout / PublicLayout / ProtectedRoute
│   │   └── business/           # （Wave B 起）
│   ├── services/
│   │   ├── http.ts             # axios + Envelope 攔截器
│   │   ├── memberService.ts    # M-MEMBER API
│   │   └── watchlistService.ts # M-WATCH（Wave A mock）
│   ├── stores/
│   │   └── authStore.ts        # Zustand + persist (localStorage)
│   ├── hooks/
│   │   ├── useAuth.ts          # TanStack mutation
│   │   └── useWatchlist.ts     # TanStack query
│   ├── types/                  # ApiResponse / member / watchlist
│   ├── constants/              # errorCodes / routes
│   ├── i18n/                   # zh-TW（預設） / en
│   ├── utils/                  # datetime（GMT+8）
│   ├── styles/global.css
│   └── test/                   # vitest setup + testUtils
├── package.json
├── tsconfig.json               # strict + noUncheckedIndexedAccess
├── vite.config.ts
├── .eslintrc.cjs
└── .prettierrc
```

---

## 對應後端 API 範例

### 登入

```http
POST /api/v1/member/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123"
}
```

成功回應：

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "accessToken": "eyJhbGciOi...",
    "refreshToken": "eyJhbGciOi...",
    "expiresIn": 3600,
    "user": {
      "userId": "uuid",
      "email": "user@example.com",
      "displayName": "Tester",
      "status": "ACTIVE",
      "emailVerifiedAt": "2026-04-22T08:00:00.000+08:00",
      "notifyEmailEnabled": true,
      "notifyWebEnabled": true,
      "createdAt": "2026-04-22T08:00:00.000+08:00"
    }
  },
  "timestamp": "2026-04-22T08:00:00.000+08:00",
  "traceId": "abc-123"
}
```

業務錯誤回應（仍是 HTTP 200）：

```json
{
  "code": 2011,
  "message": "密碼錯誤",
  "timestamp": "2026-04-22T08:00:00.000+08:00",
  "traceId": "abc-124"
}
```

### 自選股清單（Wave B 上線）

```http
POST /api/v1/watchlist/list
Authorization: Bearer {jwt}
Content-Type: application/json

{}
```

---

## 合規規範（強制）

依 [SRS §0 D1 拍板](../docs/03_spec/20260421_SRS_stock-analysis-mvp.md)，前端**絕不**出現以下字眼：

- ❌ 「建議買入」「建議賣出」「應該」「投資建議」「強烈買進」
- ✅ 改用：「個股健康度」「技術型態觀察」「籌碼動向觀察」「條件達成提醒」「5 級訊號燈」

所有頁面 Footer 必須顯示 i18n 的 `disclaimer` 文案（PublicLayout / AppLayout 已內建）。

---

## 已知 TODO（Wave B/C）

詳見 [docs/05_development/frontend/20260422_waveA_skeleton.md](../docs/05_development/frontend/20260422_waveA_skeleton.md)。
