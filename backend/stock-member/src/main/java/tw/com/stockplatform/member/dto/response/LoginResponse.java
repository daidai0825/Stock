package tw.com.stockplatform.member.dto.response;

/**
 * 登入回應，含 JWT。
 */
public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds,
    MemberDTO member
) {
}
