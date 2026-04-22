package tw.com.stockplatform.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * /api/v1/member/login 請求。
 */
public record LoginRequest(
    @NotBlank @Email String email,
    @NotBlank String password
) {
}
