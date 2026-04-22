-- ============================================================
-- V1.0.0  建立 M-MEMBER 模組資料表（PostgreSQL 16）
-- 對應 SRS §2.1 USER_INFO + ProjectArch §6.2 USER_PREFERENCE
-- 主鍵 VARCHAR(36) UUID；時間 TIMESTAMP WITHOUT TIME ZONE（GMT+8 固定時區）
-- ============================================================

CREATE TABLE IF NOT EXISTS users (
    user_id            VARCHAR(36)  NOT NULL,
    email              VARCHAR(255) NOT NULL,
    password_hash      VARCHAR(60)  NOT NULL,
    display_name       VARCHAR(100),
    status             VARCHAR(20)  NOT NULL DEFAULT 'UNVERIFIED',
    email_verified_at  TIMESTAMP WITHOUT TIME ZONE,
    created_at         TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITHOUT TIME ZONE,
    deleted_at         TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_users PRIMARY KEY (user_id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_users_email ON users (email);
CREATE INDEX IF NOT EXISTS idx_users_status_deleted ON users (status, deleted_at);

COMMENT ON TABLE  users IS '會員主檔（M-MEMBER）';
COMMENT ON COLUMN users.user_id       IS 'UUID 字串';
COMMENT ON COLUMN users.email         IS 'Email（唯一）';
COMMENT ON COLUMN users.password_hash IS 'BCrypt 雜湊（cost 12）';
COMMENT ON COLUMN users.status        IS 'UNVERIFIED / ACTIVE / SUSPENDED / CLOSED';
COMMENT ON COLUMN users.deleted_at    IS '軟刪除時間，30 日後永久刪除';

-- 推播偏好（與 USERS 1:1）
CREATE TABLE IF NOT EXISTS user_preferences (
    preference_id            VARCHAR(36)  NOT NULL,
    user_id                  VARCHAR(36)  NOT NULL,
    preset_type              VARCHAR(20),
    tech_weight              NUMERIC(5,2),
    chip_weight              NUMERIC(5,2),
    fund_weight              NUMERIC(5,2),
    risk_weight              NUMERIC(5,2),
    news_weight              NUMERIC(5,2),
    notify_email_enabled     CHAR(1)      NOT NULL DEFAULT 'Y',
    notify_web_enabled       CHAR(1)      NOT NULL DEFAULT 'Y',
    notify_telegram_enabled  CHAR(1)      NOT NULL DEFAULT 'N',
    telegram_chat_id         VARCHAR(50),
    fcm_token                VARCHAR(255),
    created_at               TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    updated_at               TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT pk_user_preferences PRIMARY KEY (preference_id),
    CONSTRAINT fk_user_preferences_users
        FOREIGN KEY (user_id) REFERENCES users (user_id)
        ON DELETE CASCADE
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_user_preferences_user_id
    ON user_preferences (user_id);

COMMENT ON TABLE  user_preferences IS '會員推播偏好與健康度權重設定（M-MEMBER, M-NOTIFY 共用）';
COMMENT ON COLUMN user_preferences.preset_type IS 'CONSERVATIVE / AGGRESSIVE / TECHNICAL / VALUE / NULL（自訂）';
