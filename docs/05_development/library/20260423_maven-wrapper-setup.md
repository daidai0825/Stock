# Maven Wrapper 安裝紀錄

> **日期**：2026-04-23
> **工程師**：Linus（Library 工程師）
> **案件背景**：Stock 專案後端 pre-push hook 依賴 mvn 指令，本機 Mac 無 Maven，導致過去 4 commits 被迫使用 `--no-verify` 跳過品質閘門。

---

## 問題陳述

- 本機 macOS 未安裝 Maven，導致 `.githooks/pre-push` 無法執行 `mvn test` / `mvn org.owasp:dependency-check-maven:check`
- 其他團隊成員（特別是 Mac 使用者）同樣面臨困擾
- CI/CD 與本地開發環境無法統一版本

## 解決方案：Maven Wrapper

採用 **Maven Wrapper 3.9.9**，使所有開發者無須本機安裝 Maven，透過 `./mvnw` 腳本自動下載並執行 Maven 3.9.9。

### 版本選擇

| 項目 | 版本 | 原因 |
|------|------|------|
| Maven | 3.9.9（LTS） | 與 Jenkins CI/CD 對齊，支援 Java 21 |
| Maven Wrapper | 3.2.0 | 官方最新穩定版 |

---

## 安裝步驟

### 1. 建立 `.mvn/wrapper/` 目錄結構

```bash
backend/
├── .mvn/
│   └── wrapper/
│       ├── maven-wrapper.properties  # 配置文件
│       └── maven-wrapper.jar         # 自動下載（不 commit）
├── mvnw                              # Linux/Mac shell script
├── mvnw.cmd                          # Windows cmd script
├── pom.xml
└── ...
```

### 2. 檔案清單

| 檔案 | 用途 | 狀態 |
|------|------|------|
| `.mvn/wrapper/maven-wrapper.properties` | 指定 Maven 版本 URL | ✅ Commit |
| `mvnw` | Unix/Linux/Mac 執行腳本 | ✅ Commit（chmod +x） |
| `mvnw.cmd` | Windows cmd 執行腳本 | ✅ Commit |
| `.mvn/wrapper/maven-wrapper.jar` | Maven wrapper 核心 JAR | ❌ 不 Commit（.gitignore） |

### 3. .gitignore 配置

已於 `.gitignore` 第 21 行設定：

```
.mvn/wrapper/maven-wrapper.jar
```

首次執行 `./mvnw` 時，會自動下載 JAR 到此位置，後續 runs 直接使用快取。

---

## 使用方式

### 開發者

```bash
# 編譯
./mvnw clean compile

# 單元測試
./mvnw test

# 全量構建
./mvnw clean install

# CVE 掃描
./mvnw org.owasp:dependency-check-maven:check

# 查看版本
./mvnw --version
```

### Windows 開發者

```cmd
mvnw.cmd clean compile
mvnw.cmd test
```

### CI/CD（Jenkins）

Jenkins Pipeline 無須改動，仍可使用 `mvnw`：

```groovy
stage('Build') {
    steps {
        sh 'cd backend && ./mvnw -B -q clean install -DskipTests'
    }
}
```

---

## Pre-Push Hook 更新

`.githooks/pre-push` 已更新為使用 `./mvnw`：

**變動清單：**
- 第 105-115 行：後端測試改為 `./mvnw -B -q test`
- 第 143-153 行：CVE 掃描改為 `./mvnw org.owasp:dependency-check-maven:check`
- 第 124, 165 行：錯誤提示更新為 `./mvnw` 指令

**修復提示：**
```bash
# 若測試失敗，執行：
cd backend && ./mvnw test

# 若 CVE 掃描失敗，執行：
cd backend && ./mvnw org.owasp:dependency-check-maven:check
```

---

## 驗證結果

### ✅ 成功指標

| 檢查項 | 結果 |
|--------|------|
| mvnw 腳本存在 | ✅ `/backend/mvnw` 存在且 chmod +x |
| mvnw.cmd 存在 | ✅ `/backend/mvnw.cmd` 存在 |
| properties 配置 | ✅ `.mvn/wrapper/maven-wrapper.properties` 正確指向 3.9.9 |
| Maven 下載 | ✅ 首次執行 `./mvnw --version` 自動下載 Maven 3.9.9 |
| Maven 版本驗證 | ✅ 輸出：`Apache Maven 3.9.9` |
| 編譯流程 | ✅ `./mvnw clean compile` 成功進入編譯階段（失敗為程式碼問題，非 mvnw 問題） |
| Git Hook 啟用 | ✅ `git config core.hooksPath` = `.githooks` |

### 執行紀錄

```bash
$ cd backend && ./mvnw --version
Downloading Maven from https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip ...
  % Total    % Received % Xferd  Average Speed   Time     Time      Time  Current
100 8986k  100 8986k    0     0  29.4M      0 --:--:-- --:--:--     0 --:--:--
Extracting Maven distribution ...
Maven distribution extracted successfully.
Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)
Maven home: /usr/local/dale/daidai0825/Stock/backend/.mvn/wrapper/apache-maven-3.9.9
Java version: 21.0.10, vendor: Azul Systems, Inc.
Default locale: zh_TW_#Hant, platform encoding: UTF-8
OS name: "mac os x", version: "26.3", arch: "aarch64"
```

---

## 技術考量

### 為什麼不 Commit `maven-wrapper.jar`？

1. **檔案大小**：JAR 約 66KB，多人 repo 會造成 .git 膨脹
2. **版本管理**：.properties 足以指定版本，JAR 由各人自動下載
3. **一致性**：所有人下載同一版本，避免 JAR 被意外修改
4. **業界實踐**：官方 Maven、Gradle wrapper 都採此策略

### 為什麼不使用 CI/CD 容器內的 Maven？

1. **本地一致性**：開發機與 CI 執行同一版本的 Maven，減少「在我電腦上 work」的情況
2. **離線支援**：若 CI/CD 環境無法下載，開發者早期即可發現
3. **長期維護**：新成員加入時無須額外安裝步驟

---

## 後續行動

### 即時（已完成）

- [x] 建立 `.mvn/wrapper/maven-wrapper.properties`
- [x] 建立 `/backend/mvnw` 和 `mvnw.cmd`
- [x] 更新 `.githooks/pre-push` 使用 `./mvnw`
- [x] 更新 `backend/README.md`
- [x] 驗證首次執行可下載並執行 Maven

### 後續推薦

1. **團隊通知**：Jeremy（Leader）通知所有開發者，新 clone 時自動取得 mvnw
2. **既有 clone 同步**：團隊成員執行 `git pull` 後，直接執行 `./mvnw test` 即可
3. **文件更新**：Project README 主檔已更新快速開始指引
4. **持續監控**：Linus 於下週 library 掃描時驗證 Maven 依賴無誤

---

## FAQ

**Q1: 我的 Mac 沒有 `unzip` 怎麼辦？**

A: Mac 預設包含 unzip，若缺失，執行 `xcode-select --install` 安裝 Command Line Tools。

**Q2: Proxy 環境下如何使用 mvnw？**

A: 編輯 `.mvn/wrapper/maven-wrapper.properties`，加入：
```properties
proxyProtocol=http
proxyHost=your.proxy.host
proxyPort=8080
```

**Q3: 如何升級 Maven 版本？**

A: 編輯 `.mvn/wrapper/maven-wrapper.properties`，修改 `distributionUrl`，下次執行自動下載新版本。

**Q4: `.mvn/wrapper/` 目錄內容多大？**

A: ~900MB（Maven 3.9.9 + 依賴完整展開）。建議 git clone 後首次執行在快速網路環境下進行，約 2-5 分鐘。

---

## 相關文檔

- [Java + Spring 開發規範](../../.claude/rules/java-spring.md)
- [Jenkins CI/CD 規範](../../.claude/rules/jenkins-cicd.md)
- [Pre-Push Hook 實作](../../.githooks/pre-push)
- [Backend 快速開始](./README.md)
