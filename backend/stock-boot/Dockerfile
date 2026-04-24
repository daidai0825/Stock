# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# 先只複製 pom.xml 讓 dependency 層可被快取
COPY pom.xml ./
COPY stock-common/pom.xml     stock-common/
COPY stock-domain/pom.xml     stock-domain/
COPY stock-infrastructure/pom.xml stock-infrastructure/
COPY stock-member/pom.xml     stock-member/
COPY stock-watchlist/pom.xml  stock-watchlist/
COPY stock-quote/pom.xml      stock-quote/
COPY stock-technical/pom.xml  stock-technical/
COPY stock-chip/pom.xml       stock-chip/
COPY stock-fundamental/pom.xml stock-fundamental/
COPY stock-news/pom.xml       stock-news/
COPY stock-risk/pom.xml       stock-risk/
COPY stock-score/pom.xml      stock-score/
COPY stock-notify/pom.xml     stock-notify/
COPY stock-boot/pom.xml       stock-boot/

# 預先下載所有依賴（利用 Docker layer cache）
RUN mvn dependency:go-offline -q

# 複製所有原始碼
COPY stock-common/src      stock-common/src
COPY stock-domain/src      stock-domain/src
COPY stock-infrastructure/src stock-infrastructure/src
COPY stock-member/src      stock-member/src
COPY stock-watchlist/src   stock-watchlist/src
COPY stock-quote/src       stock-quote/src
COPY stock-technical/src   stock-technical/src
COPY stock-chip/src        stock-chip/src
COPY stock-fundamental/src stock-fundamental/src
COPY stock-news/src        stock-news/src
COPY stock-risk/src        stock-risk/src
COPY stock-score/src       stock-score/src
COPY stock-notify/src      stock-notify/src
COPY stock-boot/src        stock-boot/src

# 編譯並打包（跳過測試，測試在 Jenkins Pipeline 中執行）
RUN mvn package -DskipTests -pl stock-boot -am -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# 建立非 root 使用者（安全規範：禁止容器以 root 執行）
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# 從 builder stage 複製打包後的 fat JAR
COPY --from=builder /build/stock-boot/target/stock-boot-*.jar app.jar

# 調整檔案擁有者
RUN chown appuser:appgroup app.jar

USER appuser

EXPOSE 8080

# HEALTHCHECK 對 actuator liveness endpoint
HEALTHCHECK \
    --interval=30s \
    --timeout=5s \
    --start-period=90s \
    --retries=3 \
    CMD wget --quiet --tries=1 --spider \
        http://localhost:8080/actuator/health/liveness || exit 1

ENTRYPOINT [ \
    "java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "app.jar" \
]
