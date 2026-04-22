package tw.com.stockplatform.infrastructure.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import tw.com.stockplatform.domain.po.AuditLogPO;

/**
 * AUDIT_LOGS 寫入 mapper。
 */
@Mapper
public interface AuditLogMapper {

    @Insert("""
        INSERT INTO audit_logs
            (audit_id, user_id, action, target, trace_id, detail, created_at)
        VALUES
            (#{auditId}, #{userId}, #{action}, #{target}, #{traceId}, #{detail}, #{createdAt})
        """)
    int insert(AuditLogPO po);
}
