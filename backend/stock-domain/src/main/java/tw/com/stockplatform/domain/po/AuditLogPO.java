package tw.com.stockplatform.domain.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Audit log（依拍板 A4 納入 v1）。對應 AUDIT_LOGS 表。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogPO {

    private String auditId;
    private String userId;
    private String action;
    private String target;
    private String traceId;
    private String detail;
    private LocalDateTime createdAt;
}
