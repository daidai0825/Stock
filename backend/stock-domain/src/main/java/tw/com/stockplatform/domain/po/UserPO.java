package tw.com.stockplatform.domain.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 對應資料表 USERS（PostgreSQL）。
 * <p>
 * 注意：欄位避開 RDMS 保留字；時間使用 {@link LocalDateTime}（不帶時區，搭配 GMT+8 固定時區架構）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPO {

    private String userId;              // VARCHAR(36) UUID
    private String email;               // VARCHAR(255) UNIQUE
    private String passwordHash;        // VARCHAR(60) BCrypt
    private String displayName;         // VARCHAR(100)
    private String status;              // VARCHAR(20) UserStatus
    private LocalDateTime emailVerifiedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;    // 軟刪除
}
