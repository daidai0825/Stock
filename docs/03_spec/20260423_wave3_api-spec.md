# Wave 3 API Spec（含 OpenAPI 3.0 片段）

| 項目 | 內容 |
|------|------|
| 文件版本 | v1.0 |
| 撰寫者 | Peter（PM） |
| 撰寫日期 | 2026-04-23 |
| 狀態 | 已定稿（初版） |
| 對應 SRS | [20260423_wave3_SRS.md](20260423_wave3_SRS.md) |
| 完善責任 | Bruno（後端 DTO / Controller 標註）+ Felix（前端 types / service） |
| 下游使用者 | Bruno、Felix、Brian、Fiona、Quincy、Quinn |

---

## 0. 全域規範

| 項目 | 規範 |
|------|------|
| HTTP 方法 | POST（所有業務端點） |
| HTTP 狀態碼 | 200（業務錯誤透過 `code` 區分） |
| Base URL | `/api/v1` |
| Content-Type | `application/json; charset=UTF-8` |
| 認證 | `Authorization: Bearer {access_token}` |
| 未授權回應 | HTTP 200 + `{ "code": 3001, "message": "未登入，請先登入", "timestamp": "...", "traceId": "..." }` |
| 時間格式 | ISO 8601：`YYYY-MM-DDTHH:mm:ss.sss+08:00` |
| 金融數值 | `BigDecimal` → JSON string（保留精度） |

---

## 1. Wave 3 API 端點總覽

| # | 路徑 | 模組 | 認證 | 說明 |
|---|------|------|------|------|
| 1 | POST /api/v1/stock/search | Search | 否 | 個股搜尋 |
| 2 | POST /api/v1/stock/hot-search | Search | 否 | 熱門搜尋前 10 名 |
| 3 | POST /api/v1/watchlist/add | Watchlist | 是 | 加入自選股 |
| 4 | POST /api/v1/watchlist/remove | Watchlist | 是 | 移除自選股 |
| 5 | POST /api/v1/watchlist/list | Watchlist | 是 | 查詢自選股清單（含報價） |
| 6 | POST /api/v1/alert/create | Alert | 是 | 建立價格警示 |
| 7 | POST /api/v1/alert/update-status | Alert | 是 | 更新警示狀態 |
| 8 | POST /api/v1/alert/delete | Alert | 是 | 刪除警示 |
| 9 | POST /api/v1/alert/list | Alert | 是 | 查詢我的警示清單 |
| 10 | POST /api/v1/user/notification-preference | User | 是 | 查詢 / 更新推播偏好 |
| 11 | POST /api/v1/push/subscribe | Push | 是 | 訂閱 Web Push |
| 12 | POST /api/v1/push/unsubscribe | Push | 是 | 取消訂閱 Web Push |
| 13 | POST /api/v1/auth/refresh | Auth | 否 | 刷新 Access Token |
| 14 | POST /api/v1/auth/logout | Auth | 是 | 登出（撤銷 Refresh Token） |
| 15 | POST /api/v1/search/history/list | SearchHistory | 是 | 查詢搜尋歷史 |
| 16 | POST /api/v1/search/history/remove | SearchHistory | 是 | 移除單筆搜尋歷史 |
| 17 | POST /api/v1/search/history/clear | SearchHistory | 是 | 清除全部搜尋歷史 |

---

## 2. OpenAPI 3.0 YAML

```yaml
openapi: 3.0.3
info:
  title: Taiwan Stock Analysis Platform - Wave 3 API
  description: |
    Wave 3 新增端點規格。

    ## 通用規範
    - 所有 API 統一回傳 HTTP 200
    - 業務錯誤透過 `code` 欄位區分（0 = 成功）
    - 未授權一律回傳 code = 3001（不使用 HTTP 401）
    - 時間格式 ISO 8601 含時區（+08:00）
    - 金融數值一律以 string 序列化（BigDecimal 精度保護）

    ## 認證
    使用 JWT Bearer Token。
    - Access Token 有效期：15 分鐘
    - Refresh Token 有效期：7 天
    - 過期後用 /auth/refresh 換取新 token

  version: 0.3.0
  contact:
    name: Wave 3 開發團隊

servers:
  - url: https://api.stock-platform.example.com
    description: Production
  - url: https://dev-api.stock-platform.example.com
    description: Development
  - url: http://localhost:8080
    description: Local

tags:
  - name: Search
    description: 個股搜尋與熱門搜尋
  - name: SearchHistory
    description: 搜尋歷史紀錄（需登入）
  - name: Watchlist
    description: 自選股管理（需登入）
  - name: Alert
    description: 價格警示管理（需登入）
  - name: User
    description: 使用者推播偏好設定
  - name: Push
    description: Web Push 訂閱管理
  - name: Auth
    description: 認證（Refresh / Logout）

security:
  - bearerAuth: []

paths:

  # ==========================================
  # Search 模組
  # ==========================================

  /api/v1/stock/search:
    post:
      tags: [Search]
      summary: 個股搜尋
      description: |
        依輸入字串搜尋股票（代號 / 中文名稱 / 英文名稱）。
        - 最少 1 字元觸發
        - 最多回傳 10 筆
        - 排序：代號完全匹配 > 代號前綴 > 名稱完全匹配 > 名稱前綴 > 名稱部分匹配 > 英文名稱前綴
        - 公開端點（不需登入，但帶 token 亦可）
        - 前端在使用者「點選結果」後才觸發搜尋歷史寫入（不在 API 層寫入）
      operationId: searchStock
      security: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/StockSearchRequest'
            examples:
              chineseName:
                summary: 中文部分搜尋
                value:
                  keyword: "台積"
              stockId:
                summary: 代號前綴搜尋
                value:
                  keyword: "2330"
      responses:
        '200':
          description: 統一回應（成功或業務錯誤都回 200）
          content:
            application/json:
              schema:
                oneOf:
                  - $ref: '#/components/schemas/StockSearchResponse'
                  - $ref: '#/components/schemas/ErrorResponse'
              examples:
                success:
                  summary: 搜尋成功
                  value:
                    code: 0
                    message: success
                    data:
                      items:
                        - stockId: "2330"
                          stockName: "台積電"
                          stockNameEn: "TSMC"
                          market: "TWSE"
                          matchType: "NAME_PREFIX"
                      keyword: "台積"
                      total: 1
                    timestamp: "2026-04-23T10:30:45.123+08:00"
                    traceId: "abc-123"
                noResult:
                  summary: 無搜尋結果
                  value:
                    code: 0
                    message: success
                    data:
                      items: []
                      keyword: "abcxyz"
                      total: 0
                    timestamp: "2026-04-23T10:30:45.123+08:00"
                    traceId: "abc-123"
                paramError:
                  summary: keyword 超長
                  value:
                    code: 1004
                    message: "參數長度超過上限"
                    timestamp: "2026-04-23T10:30:45.123+08:00"
                    traceId: "abc-123"

  /api/v1/stock/hot-search:
    post:
      tags: [Search]
      summary: 取熱門搜尋前 10 名
      description: |
        回傳過去 24 小時搜尋次數前 10 名股票。
        - 每小時由排程更新（不保證即時）
        - 搜尋次數 < 3 的股票不出現（隱私保護）
        - 公開端點，不需登入
      operationId: getHotSearch
      security: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
      responses:
        '200':
          description: 統一回應
          content:
            application/json:
              schema:
                oneOf:
                  - $ref: '#/components/schemas/HotSearchResponse'
                  - $ref: '#/components/schemas/ErrorResponse'
              examples:
                success:
                  summary: 熱門搜尋成功
                  value:
                    code: 0
                    message: success
                    data:
                      items:
                        - rank: 1
                          stockId: "2330"
                          stockName: "台積電"
                          market: "TWSE"
                          searchCount: 1523
                        - rank: 2
                          stockId: "2317"
                          stockName: "鴻海"
                          market: "TWSE"
                          searchCount: 987
                      computedAt: "2026-04-23T10:00:00.000+08:00"
                    timestamp: "2026-04-23T10:30:45.123+08:00"
                    traceId: "abc-123"

  # ==========================================
  # SearchHistory 模組
  # ==========================================

  /api/v1/search/history/list:
    post:
      tags: [SearchHistory]
      summary: 查詢搜尋歷史
      description: |
        查詢已登入使用者的最近 10 筆搜尋紀錄，依 searched_at 降序。
        未登入回 3001（前端不顯示歷史區塊）。
      operationId: listSearchHistory
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
      responses:
        '200':
          description: 統一回應
          content:
            application/json:
              schema:
                oneOf:
                  - $ref: '#/components/schemas/SearchHistoryListResponse'
                  - $ref: '#/components/schemas/ErrorResponse'
              examples:
                success:
                  summary: 搜尋歷史查詢成功
                  value:
                    code: 0
                    message: success
                    data:
                      items:
                        - stockId: "2330"
                          stockName: "台積電"
                          market: "TWSE"
                          searchedAt: "2026-04-23T10:25:00.000+08:00"
                        - stockId: "2317"
                          stockName: "鴻海"
                          market: "TWSE"
                          searchedAt: "2026-04-22T15:30:00.000+08:00"
                      total: 2
                    timestamp: "2026-04-23T10:30:45.123+08:00"
                    traceId: "abc-123"
                unauthorized:
                  summary: 未登入
                  value:
                    code: 3001
                    message: "未登入，請先登入"
                    timestamp: "2026-04-23T10:30:45.123+08:00"
                    traceId: "abc-123"

  /api/v1/search/history/remove:
    post:
      tags: [SearchHistory]
      summary: 移除單筆搜尋歷史
      operationId: removeSearchHistory
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/SearchHistoryRemoveRequest'
      responses:
        '200':
          description: 統一回應
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/EmptySuccessResponse'

  /api/v1/search/history/clear:
    post:
      tags: [SearchHistory]
      summary: 清除全部搜尋歷史
      operationId: clearSearchHistory
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
      responses:
        '200':
          description: 統一回應
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/EmptySuccessResponse'

  # ==========================================
  # Watchlist 模組
  # ==========================================

  /api/v1/watchlist/add:
    post:
      tags: [Watchlist]
      summary: 加入自選股
      description: |
        將指定股票加入已登入使用者的自選股清單。
        - 每位使用者上限 50 檔
        - 重複加入回 2020
        - 超出上限回 2021
        - 樂觀更新：前端先更新 UI，失敗後回滾
      operationId: addWatchlistItem
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/WatchlistAddRequest'
            examples:
              valid:
                value:
                  stockId: "2330"
      responses:
        '200':
          description: 統一回應
          content:
            application/json:
              schema:
                oneOf:
                  - $ref: '#/components/schemas/WatchlistAddResponse'
                  - $ref: '#/components/schemas/ErrorResponse'
              examples:
                success:
                  summary: 加入成功
                  value:
                    code: 0
                    message: success
                    data:
                      itemId: "550e8400-e29b-41d4-a716-446655440000"
                      stockId: "2330"
                      stockName: "台積電"
                      market: "TWSE"
                      createdAt: "2026-04-23T10:30:45.123+08:00"
                    timestamp: "2026-04-23T10:30:45.123+08:00"
                    traceId: "abc-123"
                alreadyExists:
                  summary: 已在自選股
                  value:
                    code: 2020
                    message: "該股票已在自選股清單"
                    timestamp: "2026-04-23T10:30:45.123+08:00"
                    traceId: "abc-123"
                limitReached:
                  summary: 已達 50 檔上限
                  value:
                    code: 2021
                    message: "自選股已達上限 50 檔"
                    timestamp: "2026-04-23T10:30:45.123+08:00"
                    traceId: "abc-123"
                unauthorized:
                  summary: 未登入
                  value:
                    code: 3001
                    message: "未登入，請先登入"
                    timestamp: "2026-04-23T10:30:45.123+08:00"
                    traceId: "abc-123"

  /api/v1/watchlist/remove:
    post:
      tags: [Watchlist]
      summary: 移除自選股
      operationId: removeWatchlistItem
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/WatchlistRemoveRequest'
      responses:
        '200':
          description: 統一回應
          content:
            application/json:
              schema:
                oneOf:
                  - $ref: '#/components/schemas/EmptySuccessResponse'
                  - $ref: '#/components/schemas/ErrorResponse'
              examples:
                success:
                  value:
                    code: 0
                    message: success
                    data: null
                    timestamp: "2026-04-23T10:30:45.123+08:00"
                    traceId: "abc-123"
                notInWatchlist:
                  summary: 股票不在自選股清單
                  value:
                    code: 2023
                    message: "該股票不在自選股清單"
                    timestamp: "2026-04-23T10:30:45.123+08:00"
                    traceId: "abc-123"

  /api/v1/watchlist/list:
    post:
      tags: [Watchlist]
      summary: 查詢自選股清單（含即時報價）
      description: |
        查詢已登入使用者的完整自選股清單，每筆附帶最新報價。
        - 後端使用批次查詢，P95 < 500ms（10 檔）
        - 若某股報價暫時無法取得，quote 欄位回 null 並附 quoteError
        - 避免 N+1 查詢
      operationId: listWatchlist
      requestBody:
        required: true
        content:
          application/json:
            schema:
              type: object
      responses:
        '200':
          description: 統一回應
          content:
            application/json:
              schema:
                oneOf:
                  - $ref: '#/components/schemas/WatchlistListResponse'
                  - $ref: '#/components/schemas/ErrorResponse'
              examples:
                success:
                  value:
                    code: 0
                    message: success
                    data:
                      items:
                        - itemId: "uuid-xxx"
                          stockId: "2330"
                          stockName: "台積電"
                          market: "TWSE"
                          createdAt: "2026-04-23T09:00:00.000+08:00"
                          quote:
                            price: "1050.00"
                            change: "+19.00"
                            changePercent: "+1.84"
                            volume: 28450000
                            quoteDate: "2026-04-23"
                            isStale: false
                          quoteError: null
                      total: 1
                    timestamp: "2026-04-23T10:30:45.123+08:00"
                    traceId: "abc-123"

  # ==========================================
  # Alert 模組
  # ==========================================

  /api/v1/alert/create:
    post:
      tags: [Alert]
      summary: 建立價格警示
      description: |
        為指定股票建立價格警示條件。
        - 每股最多 5 條（ACTIVE + PAUSED 合計）
        - 相同條件重複建立回 2030
      operationId: createAlert
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/AlertCreateRequest'
            examples:
              priceBelow:
                summary: 向下突破 580 元
                value:
                  stockId: "2330"
                  alertType: "PRICE_BELOW"
                  threshold: "580.00"
                  changeDirection: null
              changePercent:
                summary: 跌幅超過 5%
                value:
                  stockId: "2330"
                  alertType: "CHANGE_PERCENT"
                  threshold: "5.00"
                  changeDirection: "DOWN"
      responses:
        '200':
          description: 統一回應
          content:
            application/json:
              schema:
                oneOf:
                  - $ref: '#/components/schemas/AlertCreateResponse'
                  - $ref: '#/components/schemas/ErrorResponse'
              examples:
                success:
                  value:
                    code: 0
                    message: success
                    data:
                      alertId: "uuid-yyy"
                      stockId: "2330"
                      stockName: "台積電"
                      alertType: "PRICE_BELOW"
                      threshold: "580.00"
                      changeDirection: null
                      status: "ACTIVE"
                      createdAt: "2026-04-23T10:30:45.123+08:00"
                    timestamp: "2026-04-23T10:30:45.123+08:00"
                    traceId: "abc-123"
                duplicate:
                  value:
                    code: 2030
                    message: "已存在相同提醒條件"
                    timestamp: "2026-04-23T10:30:45.123+08:00"
                    traceId: "abc-123"
                limitReached:
                  value:
                    code: 2032
                    message: "已達每股警示上限 5 條"
                    timestamp: "2026-04-23T10:30:45.123+08:00"
                    traceId: "abc-123"

  /api/v1/alert/update-status:
    post:
      tags: [Alert]
      summary: 更新警示狀態（啟用 / 停用 / 重設）
      operationId: updateAlertStatus
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/AlertUpdateStatusRequest'
            examples:
              pause:
                value:
                  alertId: "uuid-yyy"
                  status: "PAUSED"
              reactivate:
                value:
                  alertId: "uuid-yyy"
                  status: "ACTIVE"
      responses:
        '200':
          description: 統一回應
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/EmptySuccessResponse'

  /api/v1/alert/delete:
    post:
      tags: [Alert]
      summary: 刪除價格警示
      description: 軟刪除（status 設為 DELETED），可搭配 list API 過濾。
      operationId: deleteAlert
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/AlertDeleteRequest'
      responses:
        '200':
          description: 統一回應
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/EmptySuccessResponse'

  /api/v1/alert/list:
    post:
      tags: [Alert]
      summary: 查詢我的警示清單
      description: 預設回傳 ACTIVE + PAUSED + TRIGGERED，不回傳 DELETED。
      operationId: listAlerts
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/AlertListRequest'
            examples:
              all:
                summary: 查詢所有股票警示
                value: {}
              byStock:
                summary: 查詢特定股票警示
                value:
                  stockId: "2330"
              byStatus:
                summary: 查詢特定狀態警示
                value:
                  status: "ACTIVE"
      responses:
        '200':
          description: 統一回應
          content:
            application/json:
              schema:
                oneOf:
                  - $ref: '#/components/schemas/AlertListResponse'
                  - $ref: '#/components/schemas/ErrorResponse'

  # ==========================================
  # User 模組（推播偏好）
  # ==========================================

  /api/v1/user/notification-preference:
    post:
      tags: [User]
      summary: 查詢或更新推播偏好
      description: |
        使用 action 欄位區分「查詢」或「更新」。
        - action = "get"：只讀
        - action = "update"：更新指定欄位（僅帶需更新的欄位即可）
        首次查詢若無記錄，後端自動建立預設值（web_push_enabled = true, email_enabled = true）。
      operationId: manageNotificationPreference
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/NotificationPreferenceRequest'
            examples:
              get:
                value:
                  action: "get"
              updateWebPushDenied:
                summary: 前端偵測到使用者拒絕 Web Push
                value:
                  action: "update"
                  webPushStatus: "DENIED"
              disableEmail:
                value:
                  action: "update"
                  emailEnabled: false
      responses:
        '200':
          description: 統一回應
          content:
            application/json:
              schema:
                oneOf:
                  - $ref: '#/components/schemas/NotificationPreferenceResponse'
                  - $ref: '#/components/schemas/ErrorResponse'
              examples:
                success:
                  value:
                    code: 0
                    message: success
                    data:
                      webPushEnabled: true
                      emailEnabled: true
                      webPushStatus: "GRANTED"
                      updatedAt: "2026-04-23T10:30:45.123+08:00"
                    timestamp: "2026-04-23T10:30:45.123+08:00"
                    traceId: "abc-123"

  # ==========================================
  # Push 模組
  # ==========================================

  /api/v1/push/subscribe:
    post:
      tags: [Push]
      summary: 訂閱 Web Push
      description: |
        瀏覽器取得 PushSubscription 物件後，將 endpoint / p256dh / auth 送至後端儲存。
        同一 endpoint 重複訂閱視為 upsert（更新 is_active = true，重設 fail_count = 0）。
      operationId: subscribePush
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PushSubscribeRequest'
            examples:
              chrome:
                value:
                  endpoint: "https://fcm.googleapis.com/fcm/send/xyz"
                  p256dh: "BNcRdreALRFXTkOOUHK1EtK2wtaz5Ry4YfYCA_0QTpQtUbVlTxeseKEuz..."
                  auth: "tBHItJI5svbpez7KI4CCXg=="
                  userAgent: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) ..."
      responses:
        '200':
          description: 統一回應
          content:
            application/json:
              schema:
                oneOf:
                  - $ref: '#/components/schemas/PushSubscribeResponse'
                  - $ref: '#/components/schemas/ErrorResponse'

  /api/v1/push/unsubscribe:
    post:
      tags: [Push]
      summary: 取消訂閱 Web Push
      operationId: unsubscribePush
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/PushUnsubscribeRequest'
      responses:
        '200':
          description: 統一回應
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/EmptySuccessResponse'

  # ==========================================
  # Auth 模組（Refresh + Logout）
  # ==========================================

  /api/v1/auth/refresh:
    post:
      tags: [Auth]
      summary: 刷新 Access Token
      description: |
        使用 Refresh Token 換取新的 Access Token（Refresh Token Rotation）。
        - 每次 refresh 回傳新的 refresh token，舊的立即撤銷
        - refresh token 過期（7 天）回 3002
        - refresh token 被撤銷（登出後）回 3003
      operationId: refreshToken
      security: []
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RefreshTokenRequest'
      responses:
        '200':
          description: 統一回應
          content:
            application/json:
              schema:
                oneOf:
                  - $ref: '#/components/schemas/RefreshTokenResponse'
                  - $ref: '#/components/schemas/ErrorResponse'
              examples:
                success:
                  value:
                    code: 0
                    message: success
                    data:
                      accessToken: "eyJhbGciOiJSUzI1NiJ9..."
                      expiresIn: 900
                      refreshToken: "eyJhbGciOiJSUzI1NiJ9..."
                      refreshExpiresIn: 604800
                    timestamp: "2026-04-23T10:30:45.123+08:00"
                    traceId: "abc-123"
                tokenExpired:
                  value:
                    code: 3002
                    message: "登入憑證已過期"
                    timestamp: "2026-04-23T10:30:45.123+08:00"
                    traceId: "abc-123"
                tokenInvalid:
                  value:
                    code: 3003
                    message: "登入憑證無效或已被撤銷"
                    timestamp: "2026-04-23T10:30:45.123+08:00"
                    traceId: "abc-123"

  /api/v1/auth/logout:
    post:
      tags: [Auth]
      summary: 登出（撤銷 Refresh Token）
      description: |
        - 將 refresh token 的 token_hash 標記為 revoked
        - 將 access token 的 jti 加入 Redis 黑名單（TTL = 剩餘有效期）
        - 前端清除本地存儲的所有 token
      operationId: logout
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/LogoutRequest'
      responses:
        '200':
          description: 統一回應
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/EmptySuccessResponse'

# ==========================================
# Components
# ==========================================

components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT
      description: |
        JWT Access Token（RS256）。
        有效期 15 分鐘，過期後使用 /auth/refresh 換取新 token。

  schemas:

    # ===== 通用 Response Envelope =====

    BaseResponse:
      type: object
      required: [code, message, timestamp, traceId]
      properties:
        code:
          type: integer
          description: 業務錯誤碼，0 表成功
          example: 0
        message:
          type: string
          description: 人類可讀訊息
          example: success
        timestamp:
          type: string
          format: date-time
          description: ISO 8601 含時區（+08:00）
          example: "2026-04-23T10:30:45.123+08:00"
        traceId:
          type: string
          description: 請求追蹤 ID，供日誌查詢
          example: "abc-123-def"

    ErrorResponse:
      allOf:
        - $ref: '#/components/schemas/BaseResponse'
        - type: object
          properties:
            errors:
              type: array
              description: 欄位層級錯誤（1xxx 錯誤時附上）
              items:
                $ref: '#/components/schemas/FieldError'

    EmptySuccessResponse:
      allOf:
        - $ref: '#/components/schemas/BaseResponse'
        - type: object
          properties:
            data:
              nullable: true
              example: null

    FieldError:
      type: object
      properties:
        field:
          type: string
          example: threshold
        message:
          type: string
          example: "threshold 必須大於 0"

    # ===== Search Request / Response =====

    StockSearchRequest:
      type: object
      required: [keyword]
      properties:
        keyword:
          type: string
          minLength: 1
          maxLength: 50
          description: 搜尋字串（代號 / 中文 / 英文）
          example: "台積"
        limit:
          type: integer
          minimum: 1
          maximum: 10
          default: 10
          description: 最大回傳筆數，預設 10

    StockSearchItem:
      type: object
      required: [stockId, stockName, market, matchType]
      properties:
        stockId:
          type: string
          description: 股票代號
          example: "2330"
        stockName:
          type: string
          description: 中文名稱
          example: "台積電"
        stockNameEn:
          type: string
          nullable: true
          description: 英文名稱
          example: "TSMC"
        market:
          type: string
          enum: [TWSE, OTC]
          description: 市場別（上市 / 上櫃）
          example: "TWSE"
        matchType:
          type: string
          enum: [ID_EXACT, ID_PREFIX, NAME_EXACT, NAME_PREFIX, NAME_PARTIAL, NAME_EN_PREFIX]
          description: 匹配類型（供前端 highlight 使用）
          example: "NAME_PREFIX"

    StockSearchResponseData:
      type: object
      properties:
        items:
          type: array
          maxItems: 10
          items:
            $ref: '#/components/schemas/StockSearchItem'
        keyword:
          type: string
          example: "台積"
        total:
          type: integer
          description: 實際命中總數（不含分頁截斷）
          example: 1

    StockSearchResponse:
      allOf:
        - $ref: '#/components/schemas/BaseResponse'
        - type: object
          properties:
            data:
              $ref: '#/components/schemas/StockSearchResponseData'

    HotSearchItem:
      type: object
      properties:
        rank:
          type: integer
          description: 排名（1-10）
          example: 1
        stockId:
          type: string
          example: "2330"
        stockName:
          type: string
          example: "台積電"
        market:
          type: string
          enum: [TWSE, OTC]
        searchCount:
          type: integer
          description: 過去 24 小時搜尋次數（彙總，不含個人識別）
          example: 1523

    HotSearchResponseData:
      type: object
      properties:
        items:
          type: array
          maxItems: 10
          items:
            $ref: '#/components/schemas/HotSearchItem'
        computedAt:
          type: string
          format: date-time
          description: 最近一次排程彙總時間
          example: "2026-04-23T10:00:00.000+08:00"

    HotSearchResponse:
      allOf:
        - $ref: '#/components/schemas/BaseResponse'
        - type: object
          properties:
            data:
              $ref: '#/components/schemas/HotSearchResponseData'

    # ===== SearchHistory Request / Response =====

    SearchHistoryItem:
      type: object
      properties:
        stockId:
          type: string
          example: "2330"
        stockName:
          type: string
          example: "台積電"
        market:
          type: string
          enum: [TWSE, OTC]
        searchedAt:
          type: string
          format: date-time
          example: "2026-04-23T10:25:00.000+08:00"

    SearchHistoryListResponseData:
      type: object
      properties:
        items:
          type: array
          maxItems: 10
          items:
            $ref: '#/components/schemas/SearchHistoryItem'
        total:
          type: integer
          example: 5

    SearchHistoryListResponse:
      allOf:
        - $ref: '#/components/schemas/BaseResponse'
        - type: object
          properties:
            data:
              $ref: '#/components/schemas/SearchHistoryListResponseData'

    SearchHistoryRemoveRequest:
      type: object
      required: [stockId]
      properties:
        stockId:
          type: string
          example: "2330"

    # ===== Watchlist Request / Response =====

    WatchlistAddRequest:
      type: object
      required: [stockId]
      properties:
        stockId:
          type: string
          description: 股票代號
          example: "2330"

    WatchlistItemData:
      type: object
      properties:
        itemId:
          type: string
          format: uuid
          example: "550e8400-e29b-41d4-a716-446655440000"
        stockId:
          type: string
          example: "2330"
        stockName:
          type: string
          example: "台積電"
        market:
          type: string
          enum: [TWSE, OTC]
        createdAt:
          type: string
          format: date-time

    WatchlistAddResponse:
      allOf:
        - $ref: '#/components/schemas/BaseResponse'
        - type: object
          properties:
            data:
              $ref: '#/components/schemas/WatchlistItemData'

    WatchlistRemoveRequest:
      type: object
      required: [stockId]
      properties:
        stockId:
          type: string
          example: "2330"

    WatchlistQuoteSnapshot:
      type: object
      nullable: true
      description: 最新報價快照（來自 quote/list）；若暫時無法取得則為 null
      properties:
        price:
          type: string
          description: BigDecimal string
          example: "1050.00"
        change:
          type: string
          example: "+19.00"
        changePercent:
          type: string
          example: "+1.84"
        volume:
          type: number
          example: 28450000
        quoteDate:
          type: string
          example: "2026-04-23"
        isStale:
          type: boolean
          example: false

    WatchlistListItem:
      allOf:
        - $ref: '#/components/schemas/WatchlistItemData'
        - type: object
          properties:
            quote:
              $ref: '#/components/schemas/WatchlistQuoteSnapshot'
            quoteError:
              type: string
              nullable: true
              description: 若 quote 為 null，此欄位填入錯誤碼字串（例 "5010"）
              example: null

    WatchlistListResponseData:
      type: object
      properties:
        items:
          type: array
          items:
            $ref: '#/components/schemas/WatchlistListItem'
        total:
          type: integer

    WatchlistListResponse:
      allOf:
        - $ref: '#/components/schemas/BaseResponse'
        - type: object
          properties:
            data:
              $ref: '#/components/schemas/WatchlistListResponseData'

    # ===== Alert Request / Response =====

    AlertType:
      type: string
      enum: [PRICE_ABOVE, PRICE_BELOW, CHANGE_PERCENT]
      description: |
        - PRICE_ABOVE：向上突破指定價位
        - PRICE_BELOW：向下突破指定價位
        - CHANGE_PERCENT：漲跌幅超過指定百分比（需搭配 changeDirection）

    AlertStatus:
      type: string
      enum: [ACTIVE, PAUSED, TRIGGERED, DELETED]

    ChangeDirection:
      type: string
      enum: [UP, DOWN, BOTH]
      nullable: true
      description: |
        僅 alertType = CHANGE_PERCENT 時使用：
        - UP：漲幅超過 threshold
        - DOWN：跌幅超過 threshold
        - BOTH：漲或跌幅任一方向超過 threshold

    AlertCreateRequest:
      type: object
      required: [stockId, alertType, threshold]
      properties:
        stockId:
          type: string
          example: "2330"
        alertType:
          $ref: '#/components/schemas/AlertType'
        threshold:
          type: string
          description: BigDecimal string，必須 > 0；CHANGE_PERCENT 為百分比值（例 "5.00" 代表 5%）
          example: "580.00"
        changeDirection:
          $ref: '#/components/schemas/ChangeDirection'

    AlertItem:
      type: object
      properties:
        alertId:
          type: string
          format: uuid
        stockId:
          type: string
        stockName:
          type: string
        alertType:
          $ref: '#/components/schemas/AlertType'
        threshold:
          type: string
        changeDirection:
          $ref: '#/components/schemas/ChangeDirection'
        status:
          $ref: '#/components/schemas/AlertStatus'
        triggeredAt:
          type: string
          format: date-time
          nullable: true
        triggerCount:
          type: integer
          example: 0
        createdAt:
          type: string
          format: date-time

    AlertCreateResponse:
      allOf:
        - $ref: '#/components/schemas/BaseResponse'
        - type: object
          properties:
            data:
              $ref: '#/components/schemas/AlertItem'

    AlertUpdateStatusRequest:
      type: object
      required: [alertId, status]
      properties:
        alertId:
          type: string
          format: uuid
        status:
          type: string
          enum: [ACTIVE, PAUSED]
          description: 允許轉換：ACTIVE ↔ PAUSED；TRIGGERED → ACTIVE（重新啟用）

    AlertDeleteRequest:
      type: object
      required: [alertId]
      properties:
        alertId:
          type: string
          format: uuid

    AlertListRequest:
      type: object
      properties:
        stockId:
          type: string
          nullable: true
          description: 不帶則查所有股票
        status:
          type: string
          enum: [ACTIVE, PAUSED, TRIGGERED]
          nullable: true
          description: 不帶則回傳 ACTIVE + PAUSED + TRIGGERED

    AlertListResponseData:
      type: object
      properties:
        items:
          type: array
          items:
            $ref: '#/components/schemas/AlertItem'
        total:
          type: integer

    AlertListResponse:
      allOf:
        - $ref: '#/components/schemas/BaseResponse'
        - type: object
          properties:
            data:
              $ref: '#/components/schemas/AlertListResponseData'

    # ===== Notification Preference Request / Response =====

    WebPushStatus:
      type: string
      enum: [UNKNOWN, GRANTED, DENIED, UNAVAILABLE]
      description: |
        - UNKNOWN：尚未詢問過權限
        - GRANTED：已授權
        - DENIED：已拒絕（包含推播失敗 3 次自動降級）
        - UNAVAILABLE：瀏覽器不支援（iOS Safari < 16.4）

    NotificationPreferenceRequest:
      type: object
      required: [action]
      properties:
        action:
          type: string
          enum: [get, update]
        webPushEnabled:
          type: boolean
          nullable: true
        emailEnabled:
          type: boolean
          nullable: true
        webPushStatus:
          $ref: '#/components/schemas/WebPushStatus'

    NotificationPreferenceData:
      type: object
      properties:
        webPushEnabled:
          type: boolean
        emailEnabled:
          type: boolean
        webPushStatus:
          $ref: '#/components/schemas/WebPushStatus'
        updatedAt:
          type: string
          format: date-time
          nullable: true

    NotificationPreferenceResponse:
      allOf:
        - $ref: '#/components/schemas/BaseResponse'
        - type: object
          properties:
            data:
              $ref: '#/components/schemas/NotificationPreferenceData'

    # ===== Push Subscribe Request / Response =====

    PushSubscribeRequest:
      type: object
      required: [endpoint, p256dh, auth]
      properties:
        endpoint:
          type: string
          description: Web Push endpoint URL
          example: "https://fcm.googleapis.com/fcm/send/xyz"
        p256dh:
          type: string
          description: ECDH public key（base64url）
        auth:
          type: string
          description: 驗證密鑰（base64url）
        userAgent:
          type: string
          nullable: true
          description: 瀏覽器 User-Agent（供除錯）

    PushSubscribeResponse:
      allOf:
        - $ref: '#/components/schemas/BaseResponse'
        - type: object
          properties:
            data:
              type: object
              properties:
                subscriptionId:
                  type: string
                  format: uuid

    PushUnsubscribeRequest:
      type: object
      required: [endpoint]
      properties:
        endpoint:
          type: string

    # ===== Auth Request / Response =====

    RefreshTokenRequest:
      type: object
      required: [refreshToken]
      properties:
        refreshToken:
          type: string
          description: Refresh Token（JWT）

    RefreshTokenData:
      type: object
      properties:
        accessToken:
          type: string
          description: 新的 Access Token（RS256 JWT）
        expiresIn:
          type: integer
          description: Access Token 有效秒數（固定 900）
          example: 900
        refreshToken:
          type: string
          description: 新的 Refresh Token（舊的已撤銷）
        refreshExpiresIn:
          type: integer
          description: Refresh Token 有效秒數（固定 604800）
          example: 604800

    RefreshTokenResponse:
      allOf:
        - $ref: '#/components/schemas/BaseResponse'
        - type: object
          properties:
            data:
              $ref: '#/components/schemas/RefreshTokenData'

    LogoutRequest:
      type: object
      required: [refreshToken]
      properties:
        refreshToken:
          type: string
          description: 要撤銷的 Refresh Token
```

---

## 3. Wave 3 前端 TypeScript 型別定義

Felix 必須採用以下型別，不得與本文件不一致。

```typescript
// ---- 搜尋 ----

export type MatchType =
  | 'ID_EXACT'
  | 'ID_PREFIX'
  | 'NAME_EXACT'
  | 'NAME_PREFIX'
  | 'NAME_PARTIAL'
  | 'NAME_EN_PREFIX';

export interface StockSearchItem {
  stockId: string;
  stockName: string;
  stockNameEn: string | null;
  market: 'TWSE' | 'OTC';
  matchType: MatchType;
}

export interface StockSearchResult {
  items: StockSearchItem[];
  keyword: string;
  total: number;
}

export interface HotSearchItem {
  rank: number;
  stockId: string;
  stockName: string;
  market: 'TWSE' | 'OTC';
  searchCount: number;
}

export interface HotSearchResult {
  items: HotSearchItem[];
  computedAt: string; // ISO 8601
}

// ---- 搜尋歷史 ----

export interface SearchHistoryItem {
  stockId: string;
  stockName: string;
  market: 'TWSE' | 'OTC';
  searchedAt: string; // ISO 8601
}

// ---- Watchlist ----

export interface WatchlistQuoteSnapshot {
  price: string;         // BigDecimal string
  change: string;
  changePercent: string;
  volume: number;
  quoteDate: string;     // YYYY-MM-DD
  isStale: boolean;
}

export interface WatchlistItem {
  itemId: string;        // UUID
  stockId: string;
  stockName: string;
  market: 'TWSE' | 'OTC';
  createdAt: string;
  quote: WatchlistQuoteSnapshot | null;
  quoteError: string | null;
}

// ---- Price Alert ----

export type AlertType = 'PRICE_ABOVE' | 'PRICE_BELOW' | 'CHANGE_PERCENT';
export type AlertStatus = 'ACTIVE' | 'PAUSED' | 'TRIGGERED' | 'DELETED';
export type ChangeDirection = 'UP' | 'DOWN' | 'BOTH';

export interface AlertItem {
  alertId: string;
  stockId: string;
  stockName: string;
  alertType: AlertType;
  threshold: string;     // BigDecimal string
  changeDirection: ChangeDirection | null;
  status: AlertStatus;
  triggeredAt: string | null;
  triggerCount: number;
  createdAt: string;
}

// ---- Notification Preference ----

export type WebPushStatus = 'UNKNOWN' | 'GRANTED' | 'DENIED' | 'UNAVAILABLE';

export interface NotificationPreference {
  webPushEnabled: boolean;
  emailEnabled: boolean;
  webPushStatus: WebPushStatus;
  updatedAt: string | null;
}

// ---- Auth ----

export interface TokenPair {
  accessToken: string;
  expiresIn: number;         // 900 (秒)
  refreshToken: string;
  refreshExpiresIn: number;  // 604800 (秒)
}

// ---- Pending Watchlist Intent（sessionStorage 暫存格式）----

export interface PendingWatchlistIntent {
  stockId: string;
  stockName: string;
  expiredAt: number; // Unix timestamp ms（now + 5 分鐘）
}
```

---

## 4. Wave 3 新增錯誤碼補充

波 3 新增，需同步更新至 [20260422_errorCodes_central.md](20260422_errorCodes_central.md)：

| Code | Constant | 說明 |
|------|----------|------|
| `2023` | `WATCHLIST_NOT_FOUND` | 移除不存在的 Watchlist 記錄 |
| `2032` | `ALERT_LIMIT_REACHED` | 每股超過 5 條警示 |

---

## 5. 環境策略

| 環境 | Swagger UI 開放 |
|------|----------------|
| local | 開放（`springdoc.swagger-ui.enabled: true`） |
| dev | 開放 |
| uat | 開放（內部 IP 限制） |
| stg | 開放（內部 IP 限制） |
| preProd | 關閉 |
| prod | 關閉（`springdoc.api-docs.enabled: false`） |

---

## 6. 變更控管

本文件為 Wave 3 API 的初版規格（Peter 產出）。後續調整流程：

1. PR 標題加 `[SRS-CHANGE][api-spec]` 前綴
2. Bruno（後端）+ Felix（前端 types）必須在同一 PR 同步更新
3. Brian + Fiona 在同一 review cycle 確認
4. 若涉及錯誤碼異動，同步更新 `20260422_errorCodes_central.md`
