# 決策紀錄：SRS 延伸 3 議題拍板

- **日期**：2026-04-21
- **決策者**：Dale + Jamie 確認
- **議題來源**：Peter 於 SRS 階段提出
- **決策性質**：產品功能 + 技術依賴決策

---

## P1：健康度評分權重策略

- **方案 A**：固定權重
- **方案 B**：完全自訂
- **方案 C**：預設 + preset 微調 ✅ **採納**

### 落實要求

| 項目 | 內容 |
|------|------|
| 預設 preset | 4 種：**保守型**、**積極型**、**技術派**、**價值派** |
| 微調機制 | 滑桿（slider）調整各面向權重 0-100% |
| 儲存 | 每位使用者一份個人化權重設定 |
| API | M-SCORE 模組需新增 `getPresets`、`updateUserWeights`、`resetWeights` 三支 |

---

## P2：歷史評分回顧

- **方案 A**：v1 完整歷史（6 個月 / 1 年 / 3 年） ✅ **採納**
- **方案 B**：v1 僅 7 天
- **方案 C**：v2 再做

### 落實要求

| 項目 | 內容 |
|------|------|
| 時間範圍 | 6 個月、1 年、3 年三種視圖 |
| 寫入頻率 | 每日批次（盤後 14:30 後執行） |
| 儲存規模 | 1800 檔 × 365 天 × 3 年 ≈ 200 萬筆/股票，需設計分區（partition） |
| 壓縮策略 | 6 個月以前資料採日線快照即可，不需細節分數細項 |
| 開發加成 | +2 週、儲存成本 +20% |

---

## P3：推播管道

- **方案 A**：APP Push only
- **方案 B**：APP Push + Email
- **方案 C**：APP Push + LINE Messaging API
- **方案 D**：APP Push + Telegram Bot ✅ **採納**

### 落實要求

| 項目 | 內容 |
|------|------|
| APP Push | FCM（Android）+ APNs（iOS） |
| Telegram | Bot API 整合，使用者綁定 chat_id |
| 成本 | $0（兩者皆免費） |
| 風險 | Telegram 在台用戶基數較小，需 onboarding 引導綁定 |
| 排除 | LINE Notify（已停服 2025/3/31）、LINE Messaging API（成本不確定）、SMS |

---

## 規範澄清：Redis 不屬「本地快取」

- **議題**：全域規範 `system-design.md` 寫「禁止設計本地快取」，是否包含 Redis？
- **拍板**：✅ **不包含**

### 範圍定義

| 類型 | 是否禁止 | 範例 |
|------|----------|------|
| 進程內快取（local cache） | 🔴 禁止 | Caffeine、Guava Cache、ConcurrentHashMap 自製 |
| 分散式快取（distributed cache） | 🟢 允許 | Redis、Memcached、Hazelcast cluster |
| 應用層 session 快取 | 🟢 允許 | Spring Session + Redis |

### 落實要求

- Sophia 架構設計可使用 Redis 作為快取層、訊息佇列輔助
- 預計用途：健康度評分快取、即時報價快取、session 儲存

---

## 簽核

- **使用者拍板**：Dale（2026-04-21）
- **Leader 確認**：Jamie（2026-04-21）
