package tw.com.stockplatform.member.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tw.com.stockplatform.common.constant.ErrorCode;
import tw.com.stockplatform.common.exception.BusinessException;
import tw.com.stockplatform.common.response.ApiResponse;
import tw.com.stockplatform.common.trace.TraceIdFilter;
import tw.com.stockplatform.member.dto.request.LoginRequest;
import tw.com.stockplatform.member.dto.request.RefreshTokenRequest;
import tw.com.stockplatform.member.dto.request.RegisterRequest;
import tw.com.stockplatform.member.dto.request.UpdateProfileRequest;
import tw.com.stockplatform.member.dto.response.LoginResponse;
import tw.com.stockplatform.member.dto.response.MemberDTO;
import tw.com.stockplatform.member.dto.response.ProfileResponse;
import tw.com.stockplatform.member.security.JwtTokenProvider;
import tw.com.stockplatform.member.service.MemberService;

/**
 * M-MEMBER REST endpoints。
 * <p>
 * Wave 2 範圍：register / login / logout / profile/get / profile/update / refresh。
 * <p>
 * M-BE-W2-10: JWT 例外由 {@link tw.com.stockplatform.member.exception.MemberExceptionHandler} 統一攔截，
 * currentUserId() 不再寫 try-catch。
 */
@RestController
@RequestMapping("/api/v1/member")
@RequiredArgsConstructor
public class MemberController {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/register")
    public ApiResponse<MemberDTO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(memberService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(memberService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest httpRequest) {
        String userId = currentUserId(httpRequest);
        memberService.logout(userId);
        return ApiResponse.success();
    }

    @PostMapping("/profile/get")
    public ApiResponse<ProfileResponse> getProfile(HttpServletRequest httpRequest) {
        String userId = currentUserId(httpRequest);
        return ApiResponse.success(memberService.getProfile(userId));
    }

    @PostMapping("/profile/update")
    public ApiResponse<MemberDTO> updateProfile(@Valid @RequestBody UpdateProfileRequest request,
                                                HttpServletRequest httpRequest) {
        String userId = currentUserId(httpRequest);
        return ApiResponse.success(memberService.updateProfile(userId, request));
    }

    /**
     * Refresh Token endpoint（M-BE-W2-11）。
     * <p>
     * Wave 2 短期實作：Refresh Token 完整設計（Redis 黑名單、rotation）留 Wave 3（TD-10）。
     * 目前直接回 9003 FEATURE_NOT_AVAILABLE，但 endpoint 必須存在以利前端對齊。
     * <p>
     * Wave 3 TODO (TD-10): 驗證 refresh token（Redis 比對） → 發新 access token + rotate refresh。
     */
    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success(memberService.refreshToken(request));
    }

    /**
     * 簡易解 token（Wave 1-2；Wave 3 改由 Spring Security Filter 統一處理）。
     * <p>
     * M-BE-W2-10: 移除 try-catch，JwtAuthException 交由
     * {@link tw.com.stockplatform.member.exception.MemberExceptionHandler} 攔截。
     * <ul>
     *   <li>無 Authorization header → 3001 UNAUTHORIZED（BusinessException）</li>
     *   <li>Token 過期 → 3002 TOKEN_EXPIRED（由 MemberExceptionHandler 攔截）</li>
     *   <li>Token 無效 → 3003 TOKEN_INVALID（由 MemberExceptionHandler 攔截）</li>
     * </ul>
     */
    private String currentUserId(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String token = header.substring(BEARER_PREFIX.length());
        // JwtAuthException（含子類）由 MemberExceptionHandler 統一攔截，此處不 catch
        String userId = jwtTokenProvider.parse(token).getSubject();
        MDC.put(TraceIdFilter.USER_ID_MDC_KEY, userId);
        return userId;
    }
}
