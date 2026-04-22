package tw.com.stockplatform.member.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import tw.com.stockplatform.common.constant.ErrorCode;
import tw.com.stockplatform.common.exception.BusinessException;
import tw.com.stockplatform.common.trace.TraceIdFilter;
import tw.com.stockplatform.member.security.JwtTokenProvider;
import tw.com.stockplatform.member.service.MemberService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * MemberController.currentUserId 例外分流測試（BE-02 / BE-03 驗證）。
 * <p>
 * 透過 logout endpoint 觸發 currentUserId 解析路徑：
 * <ul>
 *   <li>無 header → 3001 UNAUTHORIZED</li>
 *   <li>過期 token → 3002 TOKEN_EXPIRED</li>
 *   <li>無效 token → 3003 TOKEN_INVALID</li>
 *   <li>合法 token → MDC.userId 被注入（BE-03 audit 履行）</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class MemberControllerTest {

    @Mock private MemberService memberService;
    @Mock private JwtTokenProvider jwtTokenProvider;

    private MemberController controller;

    @BeforeEach
    void setUp() {
        controller = new MemberController(memberService, jwtTokenProvider);
    }

    @AfterEach
    void tearDown() {
        MDC.remove(TraceIdFilter.USER_ID_MDC_KEY);
    }

    @Test
    @DisplayName("currentUserId：無 Authorization header 拋 3001 UNAUTHORIZED")
    void currentUserId_NoHeader() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn(null);

        assertThatThrownBy(() -> controller.logout(request))
            .isInstanceOf(BusinessException.class)
            .extracting("code")
            .isEqualTo(ErrorCode.UNAUTHORIZED.getCode());
    }

    @Test
    @DisplayName("currentUserId：header 不是 Bearer 開頭拋 3001 UNAUTHORIZED")
    void currentUserId_HeaderWithoutBearer() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Basic abc");

        assertThatThrownBy(() -> controller.logout(request))
            .isInstanceOf(BusinessException.class)
            .extracting("code")
            .isEqualTo(ErrorCode.UNAUTHORIZED.getCode());
    }

    @Test
    @DisplayName("currentUserId：過期 token 拋 3002 TOKEN_EXPIRED（BE-02 修復驗證）")
    void currentUserId_ExpiredToken() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer expired.token.value");
        when(jwtTokenProvider.parse("expired.token.value"))
            .thenThrow(new ExpiredJwtException(null, null, "expired"));

        assertThatThrownBy(() -> controller.logout(request))
            .isInstanceOf(BusinessException.class)
            .extracting("code")
            .isEqualTo(ErrorCode.TOKEN_EXPIRED.getCode());
    }

    @Test
    @DisplayName("currentUserId：無效 token 拋 3003 TOKEN_INVALID（BE-02 修復驗證）")
    void currentUserId_InvalidToken() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer broken.token");
        when(jwtTokenProvider.parse("broken.token"))
            .thenThrow(new MalformedJwtException("malformed"));

        assertThatThrownBy(() -> controller.logout(request))
            .isInstanceOf(BusinessException.class)
            .extracting("code")
            .isEqualTo(ErrorCode.TOKEN_INVALID.getCode());
    }

    @Test
    @DisplayName("currentUserId：合法 token 解出 userId 並寫入 MDC（BE-03 audit 履行）")
    void currentUserId_ValidTokenInjectsMdc() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.token");
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("uid-42");
        when(jwtTokenProvider.parse("valid.token")).thenReturn(claims);

        controller.logout(request);

        assertThat(MDC.get(TraceIdFilter.USER_ID_MDC_KEY)).isEqualTo("uid-42");
    }
}
