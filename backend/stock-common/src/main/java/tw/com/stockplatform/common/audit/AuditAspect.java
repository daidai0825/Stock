package tw.com.stockplatform.common.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import tw.com.stockplatform.common.trace.TraceIdFilter;
import tw.com.stockplatform.common.util.TimeUtils;

import java.util.UUID;

/**
 * AOP 切面：攔截 {@link Audited} 方法，組裝 {@link AuditEvent} 並交由 publisher 持久化。
 * <p>
 * userId 由 SecurityContext 取得（Wave 1 暫以 MDC "userId" 模擬，Wave 2 接 Security）。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditEventPublisher auditEventPublisher;

    @AfterReturning(pointcut = "@annotation(audited)", returning = "result")
    public void afterReturning(JoinPoint joinPoint, Audited audited, Object result) {
        publish(joinPoint, audited, "SUCCESS");
    }

    @AfterThrowing(pointcut = "@annotation(audited)", throwing = "ex")
    public void afterThrowing(JoinPoint joinPoint, Audited audited, Throwable ex) {
        publish(joinPoint, audited, "FAIL: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
    }

    private void publish(JoinPoint joinPoint, Audited audited, String detail) {
        try {
            AuditEvent event = AuditEvent.builder()
                .auditId(UUID.randomUUID().toString())
                .userId(MDC.get("userId"))
                .action(audited.action())
                .target(audited.target().isEmpty()
                    ? joinPoint.getSignature().toShortString()
                    : audited.target())
                .traceId(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY))
                .detail(detail)
                .createdAt(TimeUtils.now())
                .build();
            auditEventPublisher.publish(event);
        } catch (Exception ex) {
            log.error("AuditAspect publish failed; non-blocking, continue.", ex);
        }
    }
}
