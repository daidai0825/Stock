package tw.com.stockplatform.common.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tw.com.stockplatform.common.constant.ErrorCode;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    @DisplayName("success：code=0、含資料與時間戳")
    void success_BasicShape() {
        ApiResponse<String> response = ApiResponse.success("hello");

        assertThat(response.getCode()).isZero();
        assertThat(response.getMessage()).isEqualTo("success");
        assertThat(response.getData()).isEqualTo("hello");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("fail(ErrorCode)：帶入業務錯誤碼與訊息")
    void fail_ByErrorCode() {
        ApiResponse<Void> response = ApiResponse.fail(ErrorCode.EMAIL_ALREADY_REGISTERED);

        assertThat(response.getCode()).isEqualTo(2010);
        assertThat(response.getMessage()).isEqualTo("Email 已被註冊");
        assertThat(response.getData()).isNull();
    }

    @Test
    @DisplayName("ErrorCode.fromCode：可由數字反查 enum")
    void errorCode_FromCode() {
        assertThat(ErrorCode.fromCode(2010)).contains(ErrorCode.EMAIL_ALREADY_REGISTERED);
        assertThat(ErrorCode.fromCode(99999)).isEmpty();
    }
}
