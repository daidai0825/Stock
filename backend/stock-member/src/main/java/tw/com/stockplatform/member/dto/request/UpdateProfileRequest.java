package tw.com.stockplatform.member.dto.request;

import jakarta.validation.constraints.Size;

/**
 * /api/v1/member/profile/update 請求。
 * <p>
 * Wave 1 僅開放 displayName + 推播開關。
 */
public record UpdateProfileRequest(
    @Size(max = 100)
    String displayName,

    Boolean notifyEmailEnabled,
    Boolean notifyWebEnabled,
    Boolean notifyTelegramEnabled
) {
}
