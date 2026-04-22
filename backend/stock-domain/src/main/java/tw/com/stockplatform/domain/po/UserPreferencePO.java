package tw.com.stockplatform.domain.po;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 對應 USER_PREFERENCES（與 USERS 1:1）。
 * Wave 1 僅落地基礎欄位；preset_type / 權重 / 推播 token 留至 Wave 3+ 使用。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferencePO {

    private String preferenceId;
    private String userId;

    private String presetType;          // CONSERVATIVE / AGGRESSIVE / TECHNICAL / VALUE
    private BigDecimal techWeight;
    private BigDecimal chipWeight;
    private BigDecimal fundWeight;
    private BigDecimal riskWeight;
    private BigDecimal newsWeight;

    private String notifyEmailEnabled;  // CHAR(1) Y/N
    private String notifyWebEnabled;    // CHAR(1) Y/N
    private String notifyTelegramEnabled; // CHAR(1) Y/N

    private String telegramChatId;
    private String fcmToken;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
