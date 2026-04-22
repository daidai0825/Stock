package tw.com.stockplatform.member.dto.response;

import java.time.LocalDateTime;

/**
 * 對外個人資料 DTO（不含密碼雜湊與內部欄位）。
 */
public record MemberDTO(
    String userId,
    String email,
    String displayName,
    String status,
    LocalDateTime createdAt
) {
}
