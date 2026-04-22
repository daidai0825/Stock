# 決策紀錄：v1 平台形態

- **日期**：2026-04-22
- **決策者**：Dale + Jamie 確認
- **議題來源**：Jamie 於 Stage 4 啟動前提出

---

## 議題：v1 是否含行動 APP？

- **方案 A**：純 Web Responsive ✅ **採納**
- 方案 B：Web + React Native APP
- 方案 C：Web + PWA

### 落實要求

| 項目 | 內容 |
|------|------|
| 平台 | 純 Web（Desktop + Mobile RWD） |
| 推播策略 | **Telegram Bot 為主**（已綁定 chat_id 即可推送）+ Web Push（FCM Web SDK，瀏覽器需授權） |
| iOS Web Push | 限 Safari 16.4+，不支援的版本退回 Telegram |
| APP 規劃 | **v2 再評估** React Native 或原生開發 |
| Felix 範圍 | 純 Web；**不需 React Native 開發** |
| 後端推播實作 | FCM（Web）+ Telegram Bot（**APNs 暫緩**，待 v2 行動 APP 啟動再啟） |

### 影響

- ✅ Felix 開發範圍縮小 ~40%（無 RN）
- ✅ Linus 不需評估 RN 相關 lib
- ✅ Bruno 推播 module 簡化（暫不含 APNs）
- ⚠️ iOS 用戶體驗依賴 Telegram，需 onboarding 引導

---

## 簽核

- **使用者拍板**：Dale（2026-04-22）
- **Leader 確認**：Jamie（2026-04-22）
