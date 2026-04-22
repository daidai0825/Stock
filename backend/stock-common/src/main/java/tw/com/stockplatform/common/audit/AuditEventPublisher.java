package tw.com.stockplatform.common.audit;

/**
 * Audit 事件發送介面。實作位於 stock-boot（透過 Mapper 寫入 audit_logs 表）。
 * <p>
 * 抽象成介面避免 stock-common 直接依賴持久層。
 */
public interface AuditEventPublisher {

    void publish(AuditEvent event);
}
