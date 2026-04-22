# Rule: Jenkins CI/CD 規範

> **適用範圍**：`Jenkinsfile`、`*.groovy`、`.jenkins/`
> **適用 Agents**：Sophia、Bruno、Felix
> **CI/CD 工具**：Jenkins（Declarative Pipeline）

---

## 原則

- 使用 **Declarative Pipeline**（不使用 Scripted）
- Pipeline as Code（`Jenkinsfile` 放在專案根目錄）
- 多分支策略（Multibranch Pipeline）
- 環境變數透過 Jenkins Credentials 注入

## 標準 Pipeline 階段

```
1. Checkout       → 取得程式碼
2. Build          → 編譯
3. Lint           → 靜態檢查
4. Unit Test      → 單元測試
5. SonarQube      → 程式碼品質掃描
6. Dependency     → 依賴掃描（OWASP Dependency Check）
7. Package        → 打包（JAR/WAR/Docker image）
8. Integration    → 整合測試（Testcontainers）
9. E2E Test       → Playwright（針對 dev 環境）
10. Deploy        → 部署到目標環境
11. Smoke Test    → 部署後快速驗證
12. Notify        → 通知（Slack / Teams / Email）
```

## 後端 Jenkinsfile 範例（Maven）

```groovy
pipeline {
    agent {
        docker {
            image 'maven:3.9-eclipse-temurin-21'
            args '-v $HOME/.m2:/root/.m2'
        }
    }

    environment {
        SONAR_TOKEN = credentials('sonar-token')
        DOCKER_REGISTRY = 'registry.example.com'
        APP_NAME = 'my-service'
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
        ansiColor('xterm')
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean compile -DskipTests'
            }
        }

        stage('Lint & Format') {
            steps {
                sh 'mvn spotless:check'
            }
        }

        stage('Unit Test') {
            steps {
                sh 'mvn test'
            }
            post {
                always {
                    junit '**/target/surefire-reports/*.xml'
                    jacoco execPattern: 'target/jacoco.exec'
                }
            }
        }

        stage('SonarQube Analysis') {
            steps {
                withSonarQubeEnv('sonar-server') {
                    sh 'mvn sonar:sonar'
                }
            }
        }

        stage('Quality Gate') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Dependency Check') {
            steps {
                sh 'mvn org.owasp:dependency-check-maven:check'
            }
            post {
                always {
                    publishHTML([
                        reportDir: 'target',
                        reportFiles: 'dependency-check-report.html',
                        reportName: 'Dependency Check'
                    ])
                }
            }
        }

        stage('Integration Test') {
            steps {
                sh 'mvn verify -DskipUnitTests'
            }
        }

        stage('Package') {
            steps {
                sh 'mvn package -DskipTests'
                script {
                    def image = docker.build("${DOCKER_REGISTRY}/${APP_NAME}:${env.BUILD_NUMBER}")
                    docker.withRegistry("https://${DOCKER_REGISTRY}", 'docker-credentials') {
                        image.push()
                        image.push('latest')
                    }
                }
            }
        }

        stage('Deploy to Dev') {
            when {
                branch 'develop'
            }
            steps {
                sh """
                    kubectl set image deployment/${APP_NAME} \
                        ${APP_NAME}=${DOCKER_REGISTRY}/${APP_NAME}:${env.BUILD_NUMBER} \
                        --namespace=dev
                """
            }
        }

        stage('Smoke Test') {
            when {
                branch 'develop'
            }
            steps {
                sh 'curl -f https://dev.example.com/health'
            }
        }
    }

    post {
        success {
            slackSend(channel: '#deployments', color: 'good',
                message: "✅ ${env.JOB_NAME} #${env.BUILD_NUMBER} 部署成功")
        }
        failure {
            slackSend(channel: '#deployments', color: 'danger',
                message: "❌ ${env.JOB_NAME} #${env.BUILD_NUMBER} 部署失敗")
        }
    }
}
```

## 前端 Jenkinsfile 範例（Vite + React）

```groovy
pipeline {
    agent {
        docker {
            image 'node:20-alpine'
        }
    }

    environment {
        VITE_API_BASE_URL = credentials("api-base-url-${env.BRANCH_NAME}")
    }

    stages {
        stage('Install') {
            steps {
                sh 'npm ci'
            }
        }

        stage('Lint') {
            steps {
                sh 'npm run lint'
            }
        }

        stage('Type Check') {
            steps {
                sh 'npm run typecheck'
            }
        }

        stage('Unit Test') {
            steps {
                sh 'npm run test:coverage'
            }
            post {
                always {
                    publishHTML([
                        reportDir: 'coverage',
                        reportFiles: 'index.html',
                        reportName: 'Coverage Report'
                    ])
                }
            }
        }

        stage('Dependency Audit') {
            steps {
                sh 'npm audit --audit-level=high'
            }
        }

        stage('Build') {
            steps {
                sh 'npm run build'
            }
        }

        stage('E2E Test') {
            steps {
                sh 'npx playwright test'
            }
            post {
                always {
                    publishHTML([
                        reportDir: 'playwright-report',
                        reportFiles: 'index.html',
                        reportName: 'Playwright Report'
                    ])
                }
            }
        }

        stage('Deploy') {
            when {
                branch 'develop'
            }
            steps {
                sh 'aws s3 sync dist/ s3://my-app-dev/ --delete'
                sh 'aws cloudfront create-invalidation --distribution-id $CF_DIST_ID --paths "/*"'
            }
        }
    }
}
```

## 環境分支對應

| 分支 | 部署環境 | 觸發條件 |
|------|----------|----------|
| `feature/*` | - | 僅 build & test |
| `bugfix/*` | - | 僅 build & test |
| `develop` | dev | 自動部署 |
| `release/*` | uat、stg | 手動觸發 |
| `main` | preProd → prod | 手動 + 審批 |
| `hotfix/*` | prod（直接） | 手動 + 緊急審批 |

## Credentials 管理

| Credential ID | 用途 |
|---------------|------|
| `sonar-token` | SonarQube API token |
| `docker-credentials` | Docker registry |
| `aws-credentials` | AWS deploy |
| `db-credentials-dev` | 資料庫帳密 |
| `slack-webhook` | 通知 |

**禁止**將 credentials 寫入 Jenkinsfile。

## 通知策略

| 狀態 | 通知 |
|------|------|
| ✅ 成功（dev） | Slack `#deployments`（簡訊） |
| ✅ 成功（prod） | Slack `#deployments` + Email |
| ❌ 失敗 | Slack `#deployments-alert` + Email + 觸發 oncall |
| ⚠️ 不穩定 | Slack `#deployments` |

## Quality Gate

SonarQube Quality Gate 條件：
- 單元測試覆蓋率 ≥ 80%
- 重複程式碼 ≤ 3%
- Maintainability Rating ≥ A
- Security Rating ≥ A
- 無 Blocker / Critical issues

不通過則 abort pipeline。

## 部署策略

| 環境 | 策略 |
|------|------|
| dev | Rolling update |
| uat / stg | Rolling update |
| preProd | Blue-Green |
| prod | Blue-Green 或 Canary |

## 回滾

每次部署保留前 5 個版本，支援一鍵回滾：

```bash
kubectl rollout undo deployment/my-service --namespace=prod
```

## 禁止事項

- **禁止**使用 Scripted Pipeline（用 Declarative）
- **禁止**將 credentials 寫入 Jenkinsfile
- **禁止**`main` 分支自動部署 prod（必須人工審批）
- **禁止**跳過 Quality Gate
- **禁止**跳過依賴掃描
- **禁止**省略 smoke test
- **禁止**不發送通知
