-- ============================================================
-- V3.1.0  Wave 3：Refresh Token 持久化 + 黑名單（TD-10 清償）
-- 對應模組：stock-member（Wave 1 記憶體版升級為持久化）
-- 對應文件：docs/04_architecture/project/20260423_wave3_flyway-migration-plan.md §3
-- ============================================================

CREATE TABLE IF NOT EXISTS refresh_token (
    token_id        VARCHAR(36)  NOT NULL,
    user_id         VARCHAR(36)  NOT NULL,
    token_hash      VARCHAR(64)  NOT NULL,
    device_info     VARCHAR(512),
    issued_at       TIMESTAMP    NOT NULL,
    expires_at      TIMESTAMP    NOT NULL,
    revoked_at      TIMESTAMP,
    revoked_reason  VARCHAR(30),
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL,
    CONSTRAINT pk_refresh_token PRIMARY KEY (token_id),
    CONSTRAINT fk_refresh_token_users
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE RESTRICT,
    CONSTRAINT ck_refresh_token_revoked_reason
        CHECK (revoked_reason IS NULL
            OR revoked_reason IN ('LOGOUT', 'REFRESH_USED', 'FORCE_REVOKE'))
);

-- SHA-256 hash 查詢（Refresh Token 驗證時用）
CREATE UNIQUE INDEX IF NOT EXISTS uk_refresh_token_hash
    ON refresh_token (token_hash);

-- 使用者所有 active token 查詢（登出所有裝置）
CREATE INDEX IF NOT EXISTS idx_refresh_token_user_active
    ON refresh_token (user_id, is_active);

-- 過期清理排程用
CREATE INDEX IF NOT EXISTS idx_refresh_token_expires
    ON refresh_token (expires_at);

COMMENT ON TABLE  refresh_token               IS 'Wave 3：Refresh Token 持久化（TD-10 清償）；token 不明文存儲，只存 SHA-256 hash';
COMMENT ON COLUMN refresh_token.token_hash    IS 'SHA-256 hash of refresh token（長度 64 chars hex）';
COMMENT ON COLUMN refresh_token.device_info   IS 'User-Agent 摘要（便於除錯，最多 512 字元）';
COMMENT ON COLUMN refresh_token.revoked_reason IS 'LOGOUT（使用者登出）/ REFRESH_USED（rotation 後舊 token）/ FORCE_REVOKE（管理員強制）';
