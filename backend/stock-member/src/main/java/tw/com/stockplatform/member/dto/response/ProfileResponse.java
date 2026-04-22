package tw.com.stockplatform.member.dto.response;

/**
 * 個人資料回應（含偏好開關）。
 */
public record ProfileResponse(
    MemberDTO member,
    Boolean notifyEmailEnabled,
    Boolean notifyWebEnabled,
    Boolean notifyTelegramEnabled
) {
}
