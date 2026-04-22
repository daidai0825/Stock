package tw.com.stockplatform.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * /api/v1/member/register 請求。
 */
public record RegisterRequest(
    @NotBlank(message = "email 必填")
    @Email(message = "email 格式錯誤")
    @Size(max = 255)
    String email,

    @NotBlank(message = "密碼必填")
    @Size(min = 8, max = 64, message = "密碼長度需介於 8-64 字元")
    String password,

    @Size(max = 100)
    String displayName
) {
}
