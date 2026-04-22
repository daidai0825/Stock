# Rule: API 設計規範

> **適用範圍**：`*Controller.java`、`**/controller/**`、`**/dto/**`、前端 services 層
> **適用 Agents**：Bruno、Brian、Felix、Fiona、Peter

---

## 核心設計理念

採用 **Envelope Pattern（信封模式）**，統一 HTTP 200 + 業務錯誤碼，主要使用 POST 方法降低串接複雜度。

## 統一介面原則

| 項目 | 規範 |
|------|------|
| HTTP 狀態碼 | 統一 200（除非網路層錯誤） |
| HTTP 方法 | 主要使用 POST |
| 路徑格式 | `/api/{version}/{module}/{action}` |
| Content-Type | `application/json` |
| 字元編碼 | UTF-8 |
| 時間格式 | ISO 8601（含時區）|

## 為什麼統一 POST？

1. **降低串接難度**：客戶端只處理 200 狀態
2. **避免中間層干擾**：某些 proxy/firewall 對非 200 狀態特殊處理
3. **跨平台相容**：某些環境對 PUT/DELETE 支援不完整
4. **簡化錯誤處理**：統一在應用層處理

## 標準回應結構

### 成功回應

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "userId": "550e8400-e29b-41d4-a716-446655440000",
    "email": "test@example.com"
  },
  "timestamp": "2026-04-21T10:30:45.123+08:00",
  "traceId": "abc-123-def"
}
```

### 業務錯誤回應

```json
{
  "code": 2001,
  "message": "使用者不存在",
  "errors": [
    {
      "field": "userId",
      "message": "找不到此使用者"
    }
  ],
  "timestamp": "2026-04-21T10:30:45.123+08:00",
  "traceId": "abc-123-def"
}
```

## 業務錯誤碼分段

| 範圍 | 用途 |
|------|------|
| 0 | 成功 |
| 1000-1999 | 參數校驗錯誤 |
| 2000-2999 | 業務邏輯錯誤 |
| 3000-3999 | 權限相關錯誤 |
| 4000-4999 | 資源相關錯誤 |
| 5000-5999 | 第三方服務錯誤 |
| 9000-9999 | 系統錯誤 |

### 具體範例

| Code | Message |
|------|---------|
| 0 | 成功 |
| 1001 | 必填參數缺失 |
| 1002 | 參數格式錯誤 |
| 1003 | 參數值超出範圍 |
| 2001 | 使用者不存在 |
| 2002 | 訂單已過期 |
| 2003 | 庫存不足 |
| 3001 | 未登入 |
| 3002 | 無權限 |
| 4001 | 資源不存在 |
| 4002 | 資源已刪除 |
| 5001 | 支付服務異常 |
| 5002 | 簡訊服務異常 |
| 9001 | 系統繁忙 |
| 9999 | 未知錯誤 |

## 路徑設計

### 統一 POST 範例

```
POST /api/v1/user/create
POST /api/v1/user/update
POST /api/v1/user/delete
POST /api/v1/user/get
POST /api/v1/user/list
POST /api/v1/user/search
```

### Action 命名

| Action | 用途 |
|--------|------|
| create | 新增 |
| update | 更新 |
| delete | 刪除 |
| get | 查單一 |
| list | 查列表（含分頁） |
| search | 進階搜尋 |
| export | 匯出 |
| import | 匯入 |
| approve | 審核通過 |
| reject | 審核拒絕 |

## 請求格式範例

### 查詢單一

```http
POST /api/v1/user/get
Content-Type: application/json

{
  "userId": "550e8400-e29b-41d4-a716-446655440000"
}
```

### 列表查詢

```http
POST /api/v1/user/list
Content-Type: application/json

{
  "page": 1,
  "pageSize": 20,
  "filters": {
    "status": "ACTIVE",
    "createdAfter": "2026-01-01T00:00:00.000+08:00"
  },
  "sort": [
    { "field": "createdAt", "order": "desc" }
  ]
}
```

### 列表回應

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "items": [...],
    "page": 1,
    "pageSize": 20,
    "total": 105,
    "totalPages": 6
  },
  "timestamp": "...",
  "traceId": "..."
}
```

### 更新

```http
POST /api/v1/user/update
Content-Type: application/json

{
  "userId": "...",
  "data": {
    "displayName": "新名稱"
  }
}
```

## 時間格式（ISO 8601）

- **格式**：`YYYY-MM-DDTHH:mm:ss.sss±HH:MM` 或 `Z` 結尾
- **必含時區**
- **毫秒精度**：保留 3 位小數
- **使用 T 分隔**

範例：
```
2026-04-21T10:30:45.123+08:00
2026-04-21T02:30:45.123Z
```

## API 分類

### 強制標準化

- **公開 API**：對外開放
- **系統 API**：健康檢查、監控

### 建議標準化

- **業務 API**：複雜業務流程

### 可選標準化

- **內部 API**：高頻、批次處理（可彈性）

## 例外情境

以下情境**可使用標準 HTTP 方法**：

- 檔案上傳（POST multipart）
- 檔案下載（GET，回傳 binary）
- WebSocket
- Server-Sent Events
- 健康檢查（GET /health）
- 第三方規範要求

## API 版本管理

- 路徑加版本：`/api/v1/...`、`/api/v2/...`
- 重大變更才升 major 版本
- 同時支援多版本（過渡期）
- 棄用通知：在 response header 加 `Deprecation: true` 與 `Sunset: <date>`

## 安全標頭

```http
Authorization: Bearer {jwt_token}
X-Request-Id: {client_generated_uuid}  # optional, 用於追蹤
```

回應標頭：
```http
X-Trace-Id: {server_traceId}
X-Response-Time: 123ms
```

## 限流與配額

API Gateway 層處理。回應標頭：
```http
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 998
X-RateLimit-Reset: 1729507800
```

超限回應：
```json
{
  "code": 9002,
  "message": "請求過於頻繁，請稍後再試",
  "timestamp": "...",
  "traceId": "..."
}
```

## 文件化

- 使用 OpenAPI 3.0（Swagger）
- 開發環境提供 Swagger UI
- prod 環境**禁止**暴露 Swagger UI

## 禁止事項

- **禁止**使用 HTTP 4xx/5xx 狀態碼回傳業務錯誤（除網路層錯誤外）
- **禁止**回應結構不一致（必須統一 Envelope）
- **禁止**時間欄位無時區
- **禁止**錯誤碼隨意命名（必須按分段）
- **禁止**省略 traceId
- **禁止**在 GET 請求帶大量參數（用 POST + body）
- **禁止**API 路徑使用駝峰（用 kebab-case 或 lowercase）
- **禁止**REST API 回傳 Optional 包裝
