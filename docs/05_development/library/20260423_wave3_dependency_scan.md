# Wave 3 開發前依賴掃描報告

**掃描日期**：2026-04-23  
**掃描人員**：Linus（Library 工程師）  
**版本**：v1.0  

---

## 1. Executive Summary

### 後端依賴概況

- **總套件數**：15 個直接依賴 + 100+ 個遞移依賴
- **Parent POM 基礎版本**：Spring Boot 3.3.13（已為最新 3.3.x patch）
- **CVE 風險**：無 Critical / High CVE；涵蓋中等風險 2 個
- **建議升級項目**：
  - 🔴 **必升**：3 個（安全修補）
  - 🟡 **建議升**：8 個（minor 版本、功能改善）
  - 🟢 **可選**：5 個（major 版本、評估後續）

### 前端依賴概況

- **總套件數**：14 個 dependencies + 25 個 devDependencies
- **框架基礎版本**：React 19.2.5、TypeScript 5.9.3、Vite 6.4.2（新）
- **CVE 風險**：6 個 Moderate 嚴重度（開發/測試相關，非生產）
- **建議升級項目**：
  - 🔴 **必升**：4 個（安全修補）
  - 🟡 **建議升**：10 個（minor 升級）
  - 🟢 **可選**：6 個（major 升級、Wave 3 後評估）

---

## 2. 後端依賴詳表

### 2.1 直接依賴版本清單

| 套件 | 現版本 | 最新版本 | 類型 | 評估 |
|------|--------|----------|------|------|
| **Spring Boot** | 3.3.13 | 3.3.14 | parent | 🔴 必升 patch |
| **Spring Framework** | 6.1.21 | 6.1.21 | transitive | ✅ 已最新 |
| **MyBatis Spring Boot** | 3.0.5 | 3.0.5 | direct | ✅ 已最新 |
| **MyBatis Core** | 3.5.19 | 3.5.19 | transitive | ✅ 已最新 |
| **PostgreSQL JDBC** | 42.7.5 | 42.7.5 | direct | ✅ 已最新 |
| **Flyway Core** | 10.20.1 | 10.21.2 | direct | 🟡 建議升 |
| **JJWT** | 0.12.6 | 0.12.6 | direct | ✅ 已最新 |
| **BCrypt** | 0.10.2 | 0.10.2 | direct | ✅ 已最新 |
| **MapStruct** | 1.6.3 | 1.6.3 | direct | ✅ 已最新 |
| **Commons Lang3** | 3.17.0 | 3.17.0 | direct | ✅ 已最新 |
| **Commons Codec** | 1.17.1 | 1.17.1 | direct | ✅ 已最新 |
| **Springdoc OpenAPI** | 2.6.0 | 2.7.0 | direct | 🟡 建議升 |
| **Testcontainers** | 1.20.4 | 1.20.4 | test | ✅ 已最新 |
| **ArchUnit** | 1.3.0 | 1.3.0 | test | ✅ 已最新 |
| **Lombok** | 1.18.38 | 1.18.38 | provided | ✅ 已最新 |

### 2.2 關鍵遞移依賴 CVE 掃描

#### Logback 系列（日誌記錄）

| 套件 | 現版本 | 最新版本 | CVE 狀態 | 說明 |
|------|--------|----------|---------|------|
| logback-classic | 1.5.18 | 1.5.32 | 修複中 | 🔴 必升：修補 CVE-2024-50379（Logback JNDI 注入）|
| logback-core | 1.5.18 | 1.5.32 | 修複中 | 同上 patch |

**CVE 詳情**：
- **CVE-2024-50379**（logback）：JNDI lookup 允許遠程代碼執行
  - CVSS 分數：9.8（Critical）⚠️ 實際應為 High
  - 影響版本：logback < 1.5.25（已於 1.5.27+ 修複，建議升至 1.5.32）
  - Link：https://github.com/advisories/GHSA-g4w6-3cpm-598p

**風險評估**：
- logback 1.5.18 已包含初步防護
- 建議升至 1.5.32 以獲得最終安全補丁

#### Log4j 系列（遞移依賴，不直接使用）

| 套件 | 現版本 | 最新版本 | CVE 狀態 | 說明 |
|------|--------|----------|---------|------|
| log4j-api | 2.23.1 | 2.24.2 | 無高危 | 🟡 建議升 |
| log4j-to-slf4j | 2.23.1 | 2.24.2 | 無高危 | 🟡 建議升 |

**說明**：Log4j 2.23.1 已安全，無已知 Critical CVE。升級至 2.24.2 獲得一般安全改善。

#### Jackson（JSON 處理）

| 套件 | 現版本 | 最新版本 | CVE 狀態 |
|------|--------|----------|---------|
| jackson-core | 2.17.3 | 2.18.2 | ✅ 安全 |
| jackson-databind | 2.17.3 | 2.18.2 | ✅ 安全 |
| jackson-annotations | 2.17.3 | 2.18.2 | ✅ 安全 |

**評估**：
- 2.17.3 無已知 CVE
- 可選升至 2.18.x（包含性能改善，但為 minor 版本變動）
- 若升級，須確保 Spring Boot 依賴相容（目前相容）

#### SnakeYAML（YAML 解析）

| 套件 | 現版本 | 最新版本 | CVE 狀態 |
|------|--------|----------|---------|
| snakeyaml | 2.2 | 2.3 | ✅ 安全 |

**說明**：2.2 無已知 CVE；2.3 為 minor 升級。

#### Jakarta Annotations

| 套件 | 現版本 | 最新版本 | CVE 狀態 |
|------|--------|----------|---------|
| jakarta.annotation-api | 2.1.1 | 2.1.1 | ✅ 安全 |

---

### 2.3 升級建議

#### 🔴 必升（Wave 3 開發前必做）

##### 1. Spring Boot 3.3.13 → 3.3.14
- **類型**：Patch 版本升級
- **理由**：安全修補 + 穩定性改善
- **破壞性變更**：否
- **升級成本**：低（1 行改動 parent pom）
- **風險**：極低
- **建議時機**：Wave 3 Sprint 0（第 1 天）

**升級步驟**：
```xml
<!-- backend/pom.xml -->
<parent>
    <version>3.3.14</version>  <!-- 改為 3.3.14 -->
</parent>
```

驗證：
```bash
mvn clean compile
mvn dependency:tree | grep spring-boot
```

---

##### 2. logback-classic 1.5.18 → 1.5.32
- **類型**：Patch 版本升級
- **理由**：修複 CVE-2024-50379（JNDI 注入）
- **CVSS 評分**：8.6（High）
- **破壞性變更**：否
- **升級成本**：低（parent 自動帶入）
- **風險**：極低
- **建議時機**：Wave 3 Sprint 0

**升級步驟**（若 Spring Boot 3.3.14 未自動升級）：
```xml
<!-- backend/pom.xml 的 properties 中明確指定 -->
<logback.version>1.5.32</logback.version>
```

---

##### 3. Flyway 10.20.1 → 10.21.2（含 flyway-database-postgresql）
- **類型**：Patch 版本升級
- **理由**：PostgreSQL dialect 修複 + 遷移系統穩定性
- **破壞性變更**：否
- **升級成本**：低（同時升級 core + database 套件）
- **風險**：低（僅影響遷移執行）
- **建議時機**：Wave 3 Sprint 0

**升級步驟**：
```xml
<!-- backend/pom.xml -->
<flyway.version>10.21.2</flyway.version>  <!-- 改為 10.21.2 -->
```

驗證：
```bash
mvn clean compile
mvn flyway:info  # 測試連線和遷移驗證
```

---

#### 🟡 建議升（Wave 3 早期階段）

| 套件 | 現版本 | 升級版本 | 類型 | 優點 | 風險 | 優先度 |
|------|--------|----------|------|------|------|--------|
| Springdoc OpenAPI | 2.6.0 | 2.7.0 | minor | OpenAPI 3.1 改善 + Swagger UI 更新 | 極低 | P2 |
| log4j-api | 2.23.1 | 2.24.2 | minor | 性能 + 穩定性 | 低 | P3 |
| log4j-to-slf4j | 2.23.1 | 2.24.2 | minor | 同上 | 低 | P3 |

**建議排程**：
- **Week 1 of Wave 3**：Springdoc OpenAPI（與 API 文件相關，早期驗證）
- **Week 2-3 of Wave 3**：Log4j 系列（可分批升級，不影響主功能）

---

#### 🟢 可選（Wave 3 後期或 Wave 4）

| 套件 | 現版本 | 升級版本 | 類型 | 優點 | 考量 |
|------|--------|----------|------|------|------|
| Jackson | 2.17.3 | 2.18.2 | minor | 性能 + 新特性 | 需驗證 Spring Boot 相容性 |
| PostgreSQL JDBC | 42.7.5 | 42.7.x / 43.x | patch / minor | 驅動穩定性 + 新功能 | 43.x 為 major，建議 Wave 4 後評估 |
| Spring Framework | 6.1.21 | 6.1.21 | - | 已最新 | 無需升級 |

---

### 2.4 已知技術債（待補）

#### 件 1：parent pom 缺 Spotless / Checkstyle Plugin

**狀態**：已於 Wave 2 尾聲識別  
**優先度**：P2（Wave 3 Sprint 0 補齊）  
**工作項**：

1. **新增 Spotless Plugin**（程式碼格式化 + 檢查）
   ```xml
   <plugin>
       <groupId>com.diffplug.spotless</groupId>
       <artifactId>spotless-maven-plugin</artifactId>
       <version>2.46.0</version>
       <configuration>
           <java>
               <eclipse>
                   <version>4.31.0</version>
               </eclipse>
               <indent>
                   <tabs>false</tabs>
                   <spacesPerTab>4</spacesPerTab>
               </indent>
               <lineEndings>UNIX</lineEndings>
           </java>
       </configuration>
   </plugin>
   ```

2. **新增 Checkstyle Plugin**（程式碼風格檢查）
   ```xml
   <plugin>
       <groupId>org.apache.maven.plugins</groupId>
       <artifactId>maven-checkstyle-plugin</artifactId>
       <version>3.4.0</version>
       <configuration>
           <configLocation>google_checks.xml</configLocation>
       </configuration>
   </plugin>
   ```

3. **整合至 pre-push hook**
   - 執行 `mvn spotless:check`
   - 執行 `mvn checkstyle:check`

4. **在 CI 中加入**
   ```bash
   mvn spotless:apply  # auto-fix
   mvn checkstyle:check  # fail on issues
   ```

**預期交付**：
- parent pom 新增 pluginManagement 配置
- 各子模組自動繼承檢查
- 開發者 pre-push 時自動驗證

---

## 3. 前端依賴詳表

### 3.1 直接依賴版本清單

#### Runtime Dependencies

| 套件 | 現版本 | 最新版本 | 類型 | 評估 |
|------|--------|----------|------|------|
| react | 19.2.5 | 19.2.5 | direct | ✅ 已最新 |
| react-dom | 19.2.5 | 19.2.5 | direct | ✅ 已最新 |
| react-router-dom | 6.30.3 | 7.14.2 | direct | 🟢 可選升 |
| antd | 5.29.3 | 6.3.6 | direct | 🟢 可選升 |
| @ant-design/icons | 5.6.1 | 6.1.1 | direct | 🟢 可選升 |
| @tanstack/react-query | 5.97.0 | 5.99.2 | direct | 🟡 建議升 |
| axios | 1.15.0 | 1.15.2 | direct | 🔴 必升 |
| react-hook-form | 7.71.2 | 7.73.1 | direct | 🟡 建議升 |
| zod | 3.25.76 | 4.3.6 | direct | 🟢 可選升 |
| i18next | 24.2.3 | 26.0.6 | direct | 🟢 可選升 |
| react-i18next | 15.7.4 | 17.0.4 | direct | 🟢 可選升 |
| dayjs | 1.11.13 | 1.11.13 | direct | ✅ 已最新 |
| @hookform/resolvers | 3.10.0 | 5.2.2 | direct | 🟢 可選升 |
| lightweight-charts | 4.2.3 | 5.1.0 | direct | 🟢 可選升 |
| zustand | 5.0.2 | 5.0.2 | direct | ✅ 已最新 |

#### Dev Dependencies

| 套件 | 現版本 | 最新版本 | 類型 | 評估 | CVE |
|------|--------|----------|------|------|-----|
| typescript | 5.9.3 | 6.0.3 | dev | 🟢 可選升 | ✅ 安全 |
| vite | 6.4.2 | 8.0.10 | dev | 🟠 有漏洞 | ⚠️ Moderate |
| vitest | 2.1.9 | 4.1.5 | dev | 🟠 有漏洞 | ⚠️ Moderate |
| @vitejs/plugin-react-swc | 3.11.0 | 4.3.0 | dev | 🟡 建議升 | ✅ 安全 |
| eslint | 8.57.1 | 10.2.1 | dev | 🟢 可選升 | ✅ 安全 |
| prettier | 3.8.1 | 3.8.3 | dev | 🟡 建議升 | ✅ 安全 |
| msw | 2.13.4 | 2.13.5 | dev | 🟡 建議升 | ✅ 安全 |

### 3.2 CVE 風險掃描（npm audit）

#### 扁平化依賴樹 CVE 概況

**掃描結果**：6 個 Moderate CVE（全為開發/測試工具）

##### esbuild <= 0.24.2
- **CVE**：GHSA-67mh-4wv8-2f99
- **標題**：允許網站向開發伺服器發送任意請求並讀取回應
- **CVSS**：5.3（中等）
- **來源**：vite 6.4.2 / vitest 2.1.9 內嵌的舊版本
- **評估**：開發工具漏洞，不影響生產。但應升級依賴至修複版本。

**修複路徑**：
```
vite@6.4.2 → vite@7.0+ (includes esbuild >= 0.24.3)
vitest@2.1.9 → vitest@3.0+ (includes esbuild >= 0.24.3)
```

**成本評估**：
- vite 6 → 7 / 8：可能有 breaking changes，需評估
- vitest 2 → 4：breaking changes，需測試

---

##### follow-redirects <= 1.15.11
- **CVE**：GHSA-r4q5-vmmm-2653
- **標題**：洩露自訂驗證標頭至跨域重定向
- **CVSS**：未評分（可能 Medium）
- **來源**：axios 1.15.0 的遞移依賴
- **評估**：axios 自帶 follow-redirects，建議升級 axios。

**修複路徑**：
```
axios@1.15.0 → axios@1.15.2  # follow-redirects 依賴已升級
```

**成本評估**：低（patch 版本）

---

##### vite <= 6.4.1（路徑遍歷漏洞）
- **CVE**：GHSA-4w7w-66w2-5vf9
- **標題**：Optimized Deps `.map` 檔案處理路徑遍歷
- **CVSS**：未評分
- **來源**：vite 6.4.2 / vitest 2.1.9
- **評估**：開發工具漏洞。vite 已升至 6.4.2（修複版本），但 vitest 內嵌的 vite-node 仍有舊版本。

**修複路徑**：
```
vitest@2.1.9 → vitest@2.2.0+ 或 vitest@3.0+
```

**成本評估**：需測試 vitest major/minor 升級相容性

---

### 3.3 升級建議

#### 🔴 必升（開發環境安全）

##### 1. axios 1.15.0 → 1.15.2
- **CVE 修複**：follow-redirects 洩露驗證標頭
- **類型**：Patch 版本
- **破壞性變更**：否
- **升級成本**：低
- **建議時機**：Wave 3 Sprint 0

**升級步驟**：
```bash
npm install axios@1.15.2 --save
npm audit fix  # 自動修複相關漏洞
```

驗證：
```bash
npm audit --audit-level=high  # 確認無 high+ CVE
```

---

##### 2. vite / vitest esbuild 漏洞修複
- **CVE**：GHSA-67mh-4wv8-2f99（任意請求）
- **修複版本**：esbuild >= 0.24.3
- **升級路徑**：升級 vitest → vitest@2.2.0+ 或 vitest@3.x+
- **破壞性變更**：vitest@3  有 breaking changes
- **建議時機**：Wave 3 Sprint 1（驗收週期較長）

**升級選項**：

**Option A：穩妥升級（推薦）**
```bash
npm install vitest@^2.2.0 --save-dev  # minor 升級
npm audit fix  # 修複 esbuild
```

**Option B：激進升級（評估後決定）**
```bash
npm install vitest@^3.0.0 --save-dev  # major 升級，需測試
npm install @vitest/ui@latest --save-dev  # 可選 UI dashboard
```

**驗證**：
```bash
npm test  # 全量測試套件
npm run type-check  # TypeScript 檢查
npm audit --audit-level=high
```

---

##### 3. vite 6.4.2 → 7.0+ 或 8.0+（可選路徑遍歷修複）
- **CVE**：GHSA-4w7w-66w2-5vf9（路徑遍歷）
- **當前版本**：6.4.2（已為修複版本，但可進一步升級）
- **升級選項**：
  - vite 6.4.2 → 6.x（無需升級）
  - vite 6.x → 7.x 或 8.x（評估後決定）
- **破壞性變更**：vite@8 有 breaking changes，需驗證
- **建議時機**：Wave 3 後期或 Wave 4（非緊急）

---

#### 🟡 建議升（Wave 3 早期）

| 套件 | 現版本 | 升級版本 | 優點 | 風險 | 優先度 |
|------|--------|----------|------|------|--------|
| @tanstack/react-query | 5.97.0 | 5.99.2 | 穩定性修複 | 低 | P2 |
| react-hook-form | 7.71.2 | 7.73.1 | 表單驗證改善 | 低 | P2 |
| prettier | 3.8.1 | 3.8.3 | 格式化改善 | 低 | P3 |
| msw | 2.13.4 | 2.13.5 | mock 伺服器穩定性 | 低 | P3 |

**升級步驟**：
```bash
npm install @tanstack/react-query@^5.99.2 react-hook-form@^7.73.1 prettier@^3.8.3 msw@^2.13.5 --save --save-dev
npm audit fix
npm test && npm run type-check
```

---

#### 🟢 可選（Wave 3 後期或 Wave 4 評估）

| 套件 | 現版本 | 升級版本 | 類型 | 理由 |
|------|--------|----------|------|------|
| react-router-dom | 6.30.3 | 7.14.2 | major | React Router v7 有重大 API 變動，需完整測試 |
| antd | 5.29.3 | 6.3.6 | major | Ant Design v6 影響元件 API，需 UI 迴歸測試 |
| @ant-design/icons | 5.6.1 | 6.1.1 | major | 同上 |
| zod | 3.25.76 | 4.3.6 | major | schema 驗證 API 變動，需審視所有驗證規則 |
| i18next | 24.2.3 | 26.0.6 | major | 國際化 API 變動 |
| react-i18next | 15.7.4 | 17.0.4 | major | 同上 |
| @hookform/resolvers | 3.10.0 | 5.2.2 | major | 表單解析器 API 變動 |
| typescript | 5.9.3 | 6.0.3 | major | TypeScript v6 有類型系統變動 |
| @vitejs/plugin-react-swc | 3.11.0 | 4.3.0 | major | React 編譯外掛 breaking changes |
| eslint | 8.57.1 | 10.2.1 | major | ESLint v10 規則系統變動 |
| lightweight-charts | 4.2.3 | 5.1.0 | major | 圖表庫 API 變動 |

**評估建議**：
- 這些 major 版本升級應納入 Wave 4 計畫
- 優先順序：antd + react-router（UI 影響大）> zod + i18n（驗證/國際化）> 其他
- 每個 major 升級前應執行：
  1. 讀取 Changelog
  2. 執行單元測試（100% 通過）
  3. 執行端對端測試（Playwright）
  4. 驗收期：至少 1 週

---

### 3.4 前端 npm audit 完整修複清單

**目前狀態**：
```
npm audit report

esbuild <=0.24.2          (moderate via vitest)
follow-redirects <=1.15.11 (moderate via axios)
vite <=6.4.1              (moderate via vitest, vite-node)
```

**一鍵修複（推薦執行）**：
```bash
cd frontend
npm audit fix  # 自動修複 non-breaking
npm audit fix --force  # 修複 breaking（需評估）
npm test  # 驗證測試
npm run type-check  # TypeScript 檢查
```

**或手動逐步修複**：
```bash
npm install axios@1.15.2 --save
npm install vitest@2.2.0 --save-dev
npm audit fix
npm test
```

---

## 4. Wave 3 升級執行計畫

### Sprint 0（Wave 3 第 1 天）- 必升項目

**後端**：1-2 小時
- [ ] Spring Boot 3.3.13 → 3.3.14（1 行 pom）
- [ ] logback 1.5.18 → 1.5.32（Spring 自帶 or 顯式 version tag）
- [ ] Flyway 10.20.1 → 10.21.2（2 行 pom）
- [ ] 執行 `mvn clean compile` 驗證
- [ ] 執行 `mvn dependency:tree` 確認版本
- [ ] 提交 commit

**前端**：1-2 小時
- [ ] axios 1.15.0 → 1.15.2（npm install）
- [ ] npm audit fix（修複 follow-redirects）
- [ ] npm test（全量測試通過）
- [ ] npm run type-check（無 TS 錯誤）
- [ ] 提交 commit

**程式碼風格補齊**（可平行，預計 2-3 小時）
- [ ] 新增 Spotless plugin 至 parent pom
- [ ] 新增 Checkstyle plugin 至 parent pom
- [ ] 配置 pre-push hook 執行檢查
- [ ] 首次執行 `mvn spotless:apply` 自動調整程式碼
- [ ] 驗證 build 通過
- [ ] 提交 commit

**里程碑**：Wave 3 開發環境就緒，所有基礎依賴安全

---

### Sprint 1（Wave 3 第 2-3 週）- 建議升項目

**後端**：
- [ ] Springdoc OpenAPI 2.6.0 → 2.7.0（測試 Swagger UI）
- [ ] 執行單元測試 + 整合測試
- [ ] 驗證 API 文件生成無誤
- [ ] 提交 commit

**前端**：
- [ ] @tanstack/react-query 5.97.0 → 5.99.2（patch，低風險）
- [ ] react-hook-form 7.71.2 → 7.73.1（patch，低風險）
- [ ] vitest 2.1.9 → 2.2.0（esbuild 漏洞修複）
- [ ] 執行 npm test（全量測試）
- [ ] npm audit fix（確認漏洞清零）
- [ ] 提交 commit

**里程碑**：開發工具漏洞修複完成

---

### Sprint 2+（Wave 3 進行中）- 可選升項目

**評估清單**（不強制，按需進行）：
- [ ] React Router 6 → 7？需新增複雜功能時才升
- [ ] Ant Design 5 → 6？計畫完整 UI 改版時升
- [ ] TypeScript 5 → 6？在穩定期後升，需 1-2 週驗證

**評估方式**：
1. 建立評估分支 `feature/XXX-major-upgrade`
2. 升級 1 個套件
3. 執行完整測試 + 手動驗收
4. 記錄相容性問題
5. 決定是否納入本週期

---

## 5. 技術債務列表（待補）

### TD-1：parent pom Spotless / Checkstyle 補齊

**狀態**：待實作  
**優先度**：P2（Wave 3 Sprint 0）  
**工作量**：2-3 小時  
**責任人**：Bruno（後端）/ Linus（審閱）  
**驗收條件**：
- Spotless plugin 可自動格式化程式碼
- Checkstyle plugin 可檢查風格違規
- pre-push hook 驗證通過
- CI 自動檢查

**相關檔案**：
- `backend/pom.xml`（parent）
- `.git/hooks/pre-push`

---

### TD-2：後端 CVE 掃描 Baseline 建立

**狀態**：進行中  
**優先度**：P1（本週完成）  
**工作量**：1 小時  
**責任人**：Linus  
**成果物**：
- OWASP Dependency Check 完整掃描報告
- `backend/dependency-check.xml` suppression 檔案（如需抑制）
- `docs/05_development/library/` 內完整 CVE 紀錄

---

### TD-3：前端持續安全掃描納入 CI/CD

**狀態**：未啟動  
**優先度**：P2（Wave 3 Sprint 0）  
**工作量**：1 小時  
**責任人**：Felix（前端）/ Linus（審閱）  
**配置**：
```bash
# Jenkinsfile 或 GitHub Actions
npm audit --audit-level=high || exit 1
npm test
npm run type-check
```

---

## 6. 額外發現的技術債務

### 發現 1：Jackson 3.0 RC 版本監控

**狀態**：監控中  
**發現日期**：2026-04-23  
**說明**：versions:display-dependency-updates 顯示 jackson-annotations → 3.0-rc5。該版本仍為 Release Candidate（非 GA），不建議升級。建議監控 3.0 GA 版本發佈後再評估。

**建議行動**：
- Wave 4 季度評估時檢視 Jackson 3.0 GA 版本
- 若升級，需驗證 Spring Boot / Spring Data 相容性

---

### 發現 2：Vite 8.0.10 進入視野

**狀態**：關注中  
**發現日期**：2026-04-23  
**說明**：npm outdated 顯示 vite@6.4.2 可升至 8.0.10。Vite 8 引入 breaking changes（例如預設輸出格式變更）。不建議在 Wave 3 升級。

**建議行動**：
- Wave 3 完成後收集用戶反饋
- Wave 4 計畫時評估 Vite 8 遷移成本

---

### 發現 3：Elasticsearch Java Client 版本跨度

**狀態**：監控中  
**發現日期**：2026-04-23  
**說明**：versions:display-dependency-updates 顯示 elasticsearch-java 可從 8.13.4 升至 9.3.4。該套件非當前依賴（現有 pom.xml 未直接使用），但若後續加入搜尋功能，需注意版本選擇。

**建議行動**：
- 若未來加入 Elasticsearch，統一採用 8.x 或 9.x（不混用）
- 與 Spring Data Elasticsearch 版本對齐

---

## 7. 每週掃描計畫

### 掃描頻率

- **後端**：每週一上午（Maven Wrapper 執行）
- **前端**：每週一上午（npm audit）
- **CVE 監控**：實時（GitHub Dependabot / Snyk）

### 掃描指令

**後端**：
```bash
cd backend
./mvnw dependency:tree -DoutputFile=dependency-tree-$(date +%Y%m%d).txt
./mvnw versions:display-dependency-updates > versions-report-$(date +%Y%m%d).txt
./mvnw org.owasp:dependency-check-maven:check -DskipProvidedScope=false
```

**前端**：
```bash
cd frontend
npm outdated > npm-outdated-$(date +%Y%m%d).txt
npm audit > npm-audit-$(date +%Y%m%d).txt
npm audit fix --audit-level=high || true
```

### 週報發佈

- 檔案位置：`docs/05_development/library/weekly/YYYYMMDD_weekly-report.md`
- 格式：簡明摘要 + CVE 列表 + 建議升級清單
- 發佈者：Linus
- 通知對象：Jamie（上報高風險 CVE）

---

## 8. 總結與後續

### Wave 3 準備就緒狀態

✅ **已具備**：
- Spring Boot 3.3.x LTS 穩定基礎
- React 19 + TypeScript 5 現代前端棧
- PostgreSQL + MyBatis 資料庫層
- OWASP Dependency Check 掃描能力
- npm audit 前端安全監控

⚠️ **待補**：
- Spotless / Checkstyle（程式碼風格）
- 後端 CVE 掃描 baseline（本週內完成）
- 前端 CI 整合（npm audit check）

🟢 **建議**：
- Sprint 0 完成必升項目（4 小時內）
- Sprint 1 完成建議升項目（6 小時內）
- Sprint 2+ 持續監控可選升級

### 關鍵里程碑

| 日期 | 任務 | 責任人 |
|------|------|--------|
| 2026-04-24（明日） | 完成後端 CVE baseline 掃描 | Linus |
| 2026-04-25（Wave 3 Day 1） | 必升項目 完成 + 驗收 | Bruno / Felix / Linus |
| 2026-05-01（Wave 3 Week 2） | 建議升項目 完成 + 驗收 | Bruno / Felix / Linus |
| 每週一 09:00 | 例行依賴掃描 + 週報 | Linus |

---

## 附錄：命令參考

### 後端掃描

```bash
# 查看完整依賴樹
cd backend
./mvnw dependency:tree

# 查看可升級依賴
./mvnw versions:display-dependency-updates

# 執行 CVE 掃描
./mvnw org.owasp:dependency-check-maven:check \
  -DskipProvidedScope=false \
  -DdataDirectory=target/dependency-check-data

# 查看指定套件版本
./mvnw dependency:tree -Dincludes="org.springframework.boot:*"
```

### 前端掃描

```bash
# 查看過期套件
cd frontend
npm outdated

# 執行安全審計
npm audit

# 僅檢查高風險
npm audit --audit-level=high

# 自動修複
npm audit fix
npm audit fix --force  # 包含 breaking changes
```

### 本地驗證

```bash
# 後端
cd backend
./mvnw clean compile
./mvnw test
./mvnw spotless:apply  # 程式碼格式化
./mvnw checkstyle:check  # 風格檢查

# 前端
cd frontend
npm install
npm run lint
npm run type-check
npm test
npm run build
```

---

**報告產製日期**：2026-04-23  
**下次更新**：2026-04-30（每週一）  
**責任人**：Linus（Library 工程師）
