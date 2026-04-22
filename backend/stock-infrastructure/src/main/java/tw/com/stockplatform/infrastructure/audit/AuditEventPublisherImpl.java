package tw.com.stockplatform.infrastructure.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import tw.com.stockplatform.common.audit.AuditEvent;
import tw.com.stockplatform.common.audit.AuditEventPublisher;
import tw.com.stockplatform.domain.po.AuditLogPO;
import tw.com.stockplatform.infrastructure.mapper.AuditLogMapper;

/**
 * Audit publisher 實作：非同步寫入 audit_logs 表（不阻塞主流程，失敗僅 log）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventPublisherImpl implements AuditEventPublisher {

    private final AuditLogMapper auditLogMapper;

    @Override
    @Async("auditExecutor")
    public void publish(AuditEvent event) {
        try {
            AuditLogPO po = AuditLogPO.builder()
                .auditId(event.getAuditId())
                .userId(event.getUserId())
                .action(event.getAction())
                .target(event.getTarget())
                .traceId(event.getTraceId())
                .detail(event.getDetail())
                .createdAt(event.getCreatedAt())
                .build();
            auditLogMapper.insert(po);
        } catch (Exception ex) {
            log.error("Audit log persist failed: action={}, userId={}", event.getAction(), event.getUserId(), ex);
        }
    }
}
