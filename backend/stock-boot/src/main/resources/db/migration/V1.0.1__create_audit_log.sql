-- ============================================================
-- V1.0.1  建立 AUDIT_LOGS 表（依拍板 A4 納入 v1）
-- 寫入由 AuditAspect 非同步觸發，不阻塞主流程
-- ============================================================

CREATE TABLE IF NOT EXISTS audit_logs (
    audit_id    VARCHAR(36)   NOT NULL,
    user_id     VARCHAR(36),
    action      VARCHAR(50)   NOT NULL,
    target      VARCHAR(200),
    trace_id    VARCHAR(64),
    detail      VARCHAR(2000),
    created_at  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_audit_logs PRIMARY KEY (audit_id)
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_user_id_created
    ON audit_logs (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action_created
    ON audit_logs (action, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_trace_id
    ON audit_logs (trace_id);

COMMENT ON TABLE  audit_logs IS 'Audit 紀錄（M-MEMBER 註冊/登入/個資更新等）';
COMMENT ON COLUMN audit_logs.action IS 'MEMBER_REGISTER / MEMBER_LOGIN / MEMBER_PROFILE_UPDATE 等';
COMMENT ON COLUMN audit_logs.target IS '目標資源類型或方法簽名';
COMMENT ON COLUMN audit_logs.detail IS 'SUCCESS 或 FAIL: 詳細訊息（避免敏感資料）';
