package tw.com.stockplatform.common.audit;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * Audit 事件 DTO，由 {@link AuditAspect} 發送至 {@link AuditEventPublisher}。
 * 由業務模組（stock-member 等）注入實作後寫入 audit_logs 表。
 */
@Getter
@Builder
@ToString
public class AuditEvent {

    private final String auditId;
    private final String userId;
    private final String action;
    private final String target;
    private final String traceId;
    private final String detail;
    private final LocalDateTime createdAt;
}
