---
name: api-spec-openapi
description: API 規格模板（OpenAPI 3.0）。由 Peter（PM）使用產出初版 API 清單，由 Bruno 補完成完整 OpenAPI YAML。
---

# Skill: API Spec (OpenAPI 3.0)

## 使用時機

- **使用者**：Peter（初稿）+ Bruno（完善）
- **輸出路徑**：
  - SRS 中的 API 清單章節
  - `docs/03_spec/api/openapi.yaml`（完整 OpenAPI）
- **觸發時機**：SRS 撰寫期間

## OpenAPI 3.0 模板

```yaml
openapi: 3.0.3
info:
  title: My Project API
  description: |
    My Project 的 REST API 文件。

    ## 通用規範
    - 所有 API 統一回傳 HTTP 200
    - 業務錯誤透過 `code` 欄位區分
    - 時間格式為 ISO 8601 含時區
  version: 1.0.0
  contact:
    name: 開發團隊
    email: dev@example.com

servers:
  - url: https://api.example.com
    description: Production
  - url: https://dev-api.example.com
    description: Development
  - url: http://localhost:8080
    description: Local

tags:
  - name: User
    description: 使用者管理
  - name: Order
    description: 訂單管理

security:
  - bearerAuth: []

paths:
  /api/v1/user/register:
    post:
      tags: [User]
      summary: 使用者註冊
      description: 建立新的使用者帳號並寄送驗證信
      operationId: registerUser
      security: []  # 公開 endpoint
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/RegisterRequest'
            examples:
              valid:
                summary: 有效的註冊資料
                value:
                  email: test@example.com
                  password: ValidP@ss123!
                  displayName: Test User
      responses:
        '200':
          description: 統一回應（成功或業務錯誤都回 200）
          content:
            application/json:
              schema:
                oneOf:
                  - $ref: '#/components/schemas/RegisterSuccessResponse'
                  - $ref: '#/components/schemas/ErrorResponse'
              examples:
                success:
                  summary: 註冊成功
                  value:
                    code: 0
                    message: success
                    data:
                      userId: 550e8400-e29b-41d4-a716-446655440000
                      email: test@example.com
                      status: UNVERIFIED
                    timestamp: 2026-04-21T10:30:45.123+08:00
                    traceId: abc-123
                emailExists:
                  summary: Email 已被註冊
                  value:
                    code: 2010
                    message: Email 已被註冊
                    timestamp: 2026-04-21T10:30:45.123+08:00
                    traceId: abc-123

  /api/v1/user/get:
    post:
      tags: [User]
      summary: 取得使用者資料
      operationId: getUser
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/GetUserRequest'
      responses:
        '200':
          description: 統一回應
          content:
            application/json:
              schema:
                oneOf:
                  - $ref: '#/components/schemas/GetUserResponse'
                  - $ref: '#/components/schemas/ErrorResponse'

components:
  securitySchemes:
    bearerAuth:
      type: http
      scheme: bearer
      bearerFormat: JWT

  schemas:
    # ===== Request Schemas =====
    RegisterRequest:
      type: object
      required: [email, password]
      properties:
        email:
          type: string
          format: email
          maxLength: 255
          example: test@example.com
        password:
          type: string
          minLength: 12
          maxLength: 128
          example: ValidP@ss123!
        displayName:
          type: string
          maxLength: 100
          example: Test User

    GetUserRequest:
      type: object
      required: [userId]
      properties:
        userId:
          type: string
          format: uuid
          example: 550e8400-e29b-41d4-a716-446655440000

    # ===== Response Schemas =====
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
          example: success
        timestamp:
          type: string
          format: date-time
          description: ISO 8601 含時區
          example: 2026-04-21T10:30:45.123+08:00
        traceId:
          type: string
          example: abc-123-def

    RegisterSuccessResponse:
      allOf:
        - $ref: '#/components/schemas/BaseResponse'
        - type: object
          properties:
            data:
              $ref: '#/components/schemas/UserSummary'

    GetUserResponse:
      allOf:
        - $ref: '#/components/schemas/BaseResponse'
        - type: object
          properties:
            data:
              $ref: '#/components/schemas/UserDetail'

    ErrorResponse:
      allOf:
        - $ref: '#/components/schemas/BaseResponse'
        - type: object
          properties:
            errors:
              type: array
              items:
                $ref: '#/components/schemas/FieldError'

    # ===== Domain Schemas =====
    UserSummary:
      type: object
      properties:
        userId:
          type: string
          format: uuid
        email:
          type: string
          format: email
        status:
          $ref: '#/components/schemas/UserStatus'

    UserDetail:
      allOf:
        - $ref: '#/components/schemas/UserSummary'
        - type: object
          properties:
            displayName:
              type: string
            createdAt:
              type: string
              format: date-time
            updatedAt:
              type: string
              format: date-time
              nullable: true

    UserStatus:
      type: string
      enum: [UNVERIFIED, ACTIVE, EXPIRED, SUSPENDED, CLOSED]

    FieldError:
      type: object
      properties:
        field:
          type: string
          example: email
        message:
          type: string
          example: Email 格式錯誤

    # ===== 通用分頁 =====
    PageRequest:
      type: object
      required: [page, pageSize]
      properties:
        page:
          type: integer
          minimum: 1
          default: 1
        pageSize:
          type: integer
          minimum: 1
          maximum: 100
          default: 20

    PageResponse:
      type: object
      properties:
        items:
          type: array
        page:
          type: integer
        pageSize:
          type: integer
        total:
          type: integer
        totalPages:
          type: integer
```

## API 清單表格（適合放在 SRS 中）

```markdown
## API 清單

| # | 路徑 | 方法 | 用途 | 認證 |
|---|------|------|------|------|
| 1 | /api/v1/user/register | POST | 註冊 | 否 |
| 2 | /api/v1/user/login | POST | 登入 | 否 |
| 3 | /api/v1/user/get | POST | 查詢使用者 | 是 |
| 4 | /api/v1/user/list | POST | 列表查詢 | 是 |
| 5 | /api/v1/user/update | POST | 更新 | 是 |
| 6 | /api/v1/user/delete | POST | 刪除 | 是 |
```

## Spring Boot 整合

### 加入依賴（Maven）

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.5.0</version>
</dependency>
```

### Configuration

```java
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("My Project API")
                .version("1.0.0")
                .description("My Project 的 REST API 文件"))
            .components(new Components()
                .addSecuritySchemes("bearerAuth",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
    }
}
```

### Controller 標註

```java
@RestController
@RequestMapping("/api/v1/user")
@Tag(name = "User", description = "使用者管理")
public class UserController {

    @PostMapping("/register")
    @Operation(summary = "使用者註冊", description = "建立新帳號並寄送驗證信")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "統一回應")
    })
    public ApiResponse<UserSummary> register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }
}
```

## 環境策略

| 環境 | Swagger UI |
|------|------------|
| local | ✅ 啟用 |
| dev | ✅ 啟用 |
| uat | ✅ 啟用 |
| stg | ✅ 啟用（內部 IP 限制） |
| preProd | ❌ 關閉 |
| prod | ❌ 關閉 |

```yaml
# application-prod.yml
springdoc:
  api-docs:
    enabled: false
  swagger-ui:
    enabled: false
```

## 撰寫要點

1. **統一回應**：使用 `oneOf` 表達成功與失敗
2. **範例完整**：每個 endpoint 都附 success + error 範例
3. **Schema 重用**：用 `$ref` 避免重複
4. **Tag 分類**：按業務模組分類

## 禁止事項

- 禁止 prod 暴露 Swagger UI
- 禁止 schema 沒有範例值
- 禁止省略錯誤回應 schema
- 禁止 endpoint 不寫 summary
