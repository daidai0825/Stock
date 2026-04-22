package tw.com.stockplatform.member.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Refresh Token 請求。
 * <p>
 * Wave 2 短期：回 9003 FEATURE_NOT_AVAILABLE（endpoint 存在但功能留 Wave 3 TD-10）。
 */
public record RefreshTokenRequest(
    @NotBlank
    String refreshToken
) {}
