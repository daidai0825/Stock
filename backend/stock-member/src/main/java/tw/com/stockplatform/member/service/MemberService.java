package tw.com.stockplatform.member.service;

import tw.com.stockplatform.member.dto.request.LoginRequest;
import tw.com.stockplatform.member.dto.request.RefreshTokenRequest;
import tw.com.stockplatform.member.dto.request.RegisterRequest;
import tw.com.stockplatform.member.dto.request.UpdateProfileRequest;
import tw.com.stockplatform.member.dto.response.LoginResponse;
import tw.com.stockplatform.member.dto.response.MemberDTO;
import tw.com.stockplatform.member.dto.response.ProfileResponse;

/**
 * M-MEMBER 服務介面（Wave 2 範圍：register/login/logout/profile/refresh）。
 */
public interface MemberService {

    MemberDTO register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    /**
     * Wave 2 stub：Refresh Token 完整實作（Redis 黑名單 + rotation）留 Wave 3（TD-10）。
     * 目前回 9003 FEATURE_NOT_AVAILABLE，確保 endpoint 存在。
     */
    LoginResponse refreshToken(RefreshTokenRequest request);

    /**
     * Wave 1：黑名單登記骨架。真正撤銷需待 Refresh Token 設計（TD-10）完成。
     */
    void logout(String userId);

    ProfileResponse getProfile(String userId);

    MemberDTO updateProfile(String userId, UpdateProfileRequest request);
}
