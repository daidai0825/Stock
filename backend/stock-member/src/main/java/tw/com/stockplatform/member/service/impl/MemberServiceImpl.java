package tw.com.stockplatform.member.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tw.com.stockplatform.common.audit.Audited;
import tw.com.stockplatform.common.constant.ErrorCode;
import tw.com.stockplatform.common.exception.BusinessException;
import tw.com.stockplatform.common.trace.TraceIdFilter;
import tw.com.stockplatform.common.util.TimeUtils;
import tw.com.stockplatform.domain.enums.UserStatus;
import tw.com.stockplatform.domain.po.UserPO;
import tw.com.stockplatform.domain.po.UserPreferencePO;
import tw.com.stockplatform.member.convertor.MemberConvertor;
import tw.com.stockplatform.member.dto.request.LoginRequest;
import tw.com.stockplatform.member.dto.request.RegisterRequest;
import tw.com.stockplatform.member.dto.request.UpdateProfileRequest;
import tw.com.stockplatform.member.dto.response.LoginResponse;
import tw.com.stockplatform.member.dto.response.MemberDTO;
import tw.com.stockplatform.member.dto.response.ProfileResponse;
import tw.com.stockplatform.member.repository.MemberMapper;
import tw.com.stockplatform.member.repository.UserPreferenceMapper;
import tw.com.stockplatform.member.security.JwtTokenProvider;
import tw.com.stockplatform.member.security.PasswordEncoder;
import tw.com.stockplatform.member.dto.request.RefreshTokenRequest;
import tw.com.stockplatform.member.service.MemberService;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * M-MEMBER 服務實作。
 * <ul>
 *   <li>register：BCrypt 雜湊密碼、預設 ACTIVE（Wave 1 略過 email 驗證流程）。</li>
 *   <li>login：驗密碼後簽發 JWT。</li>
 *   <li>logout：Wave 1 stub。</li>
 *   <li>profile：查詢 + 更新。</li>
 * </ul>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberMapper memberMapper;
    private final UserPreferenceMapper userPreferenceMapper;
    private final MemberConvertor memberConvertor;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Audited(action = "MEMBER_REGISTER", target = "USER")
    public MemberDTO register(RegisterRequest request) {
        memberMapper.findByEmail(request.email())
            .ifPresent(po -> {
                throw new BusinessException(ErrorCode.EMAIL_ALREADY_REGISTERED);
            });

        LocalDateTime now = TimeUtils.now();
        UserPO po = UserPO.builder()
            .userId(UUID.randomUUID().toString())
            .email(request.email())
            .passwordHash(passwordEncoder.encode(request.password()))
            .displayName(request.displayName())
            .status(UserStatus.ACTIVE.name())  // Wave 1 略過 email 驗證
            .emailVerifiedAt(now)
            .createdAt(now)
            .updatedAt(now)
            .build();
        int rows = memberMapper.insert(po);
        if (rows != 1) {
            throw new BusinessException(ErrorCode.SYSTEM_BUSY);
        }

        // 預設偏好（推播全開、無 preset）
        UserPreferencePO pref = UserPreferencePO.builder()
            .preferenceId(UUID.randomUUID().toString())
            .userId(po.getUserId())
            .notifyEmailEnabled("Y")
            .notifyWebEnabled("Y")
            .notifyTelegramEnabled("N")
            .createdAt(now)
            .updatedAt(now)
            .build();
        userPreferenceMapper.insert(pref);

        // 寫入 MDC 讓 AuditAspect 的 @AfterReturning 能取到 userId（A4 履行）
        MDC.put(TraceIdFilter.USER_ID_MDC_KEY, po.getUserId());

        log.info("Member registered: userId={}, email={}", po.getUserId(), po.getEmail());
        return memberConvertor.toDTO(po);
    }

    @Override
    @Audited(action = "MEMBER_LOGIN", target = "USER")
    public LoginResponse login(LoginRequest request) {
        UserPO po = memberMapper.findByEmail(request.email())
            .orElseThrow(() -> new BusinessException(ErrorCode.EMAIL_OR_PASSWORD_INCORRECT));

        if (!passwordEncoder.matches(request.password(), po.getPasswordHash())) {
            throw new BusinessException(ErrorCode.EMAIL_OR_PASSWORD_INCORRECT);
        }
        if (UserStatus.SUSPENDED.name().equals(po.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_SUSPENDED);
        }
        if (UserStatus.CLOSED.name().equals(po.getStatus())) {
            throw new BusinessException(ErrorCode.ACCOUNT_DELETED);
        }

        String token = jwtTokenProvider.createAccessToken(po.getUserId(), po.getEmail());

        // 寫入 MDC 讓 AuditAspect 的 @AfterReturning 能取到 userId（A4 履行）
        MDC.put(TraceIdFilter.USER_ID_MDC_KEY, po.getUserId());

        return new LoginResponse(
            token,
            "Bearer",
            jwtTokenProvider.getAccessTokenTtlSeconds(),
            memberConvertor.toDTO(po)
        );
    }

    @Override
    public LoginResponse refreshToken(RefreshTokenRequest request) {
        // M-BE-W2-11: Wave 2 stub。
        // Wave 3 TODO (TD-10): 驗證 refresh token（Redis 黑名單比對）→ 發新 access token + rotate refresh
        log.info("Member refreshToken: feature not yet implemented (TD-10), returning 9003");
        throw new BusinessException(ErrorCode.FEATURE_NOT_AVAILABLE);
    }

    @Override
    public void logout(String userId) {
        // Wave 2 stub：Refresh Token + Redis 黑名單留至 Wave 3（TD-10）
        log.info("Member logout (stub): userId={}", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(String userId) {
        UserPO po = memberMapper.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UserPreferencePO pref = userPreferenceMapper.findByUserId(userId)
            .orElse(UserPreferencePO.builder()
                .notifyEmailEnabled("Y")
                .notifyWebEnabled("Y")
                .notifyTelegramEnabled("N")
                .build());

        return new ProfileResponse(
            memberConvertor.toDTO(po),
            "Y".equalsIgnoreCase(pref.getNotifyEmailEnabled()),
            "Y".equalsIgnoreCase(pref.getNotifyWebEnabled()),
            "Y".equalsIgnoreCase(pref.getNotifyTelegramEnabled())
        );
    }

    @Override
    @Audited(action = "MEMBER_PROFILE_UPDATE", target = "USER")
    public MemberDTO updateProfile(String userId, UpdateProfileRequest request) {
        UserPO po = memberMapper.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        LocalDateTime now = TimeUtils.now();

        if (request.displayName() != null) {
            memberMapper.updateDisplayName(userId, request.displayName(), now);
            po.setDisplayName(request.displayName());
            po.setUpdatedAt(now);
        }

        if (request.notifyEmailEnabled() != null
            || request.notifyWebEnabled() != null
            || request.notifyTelegramEnabled() != null) {

            UserPreferencePO existing = userPreferenceMapper.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            String emailFlag = toFlag(request.notifyEmailEnabled(), existing.getNotifyEmailEnabled());
            String webFlag = toFlag(request.notifyWebEnabled(), existing.getNotifyWebEnabled());
            String telegramFlag = toFlag(request.notifyTelegramEnabled(), existing.getNotifyTelegramEnabled());
            userPreferenceMapper.updateNotifyFlags(userId, emailFlag, webFlag, telegramFlag, now);
        }

        return memberConvertor.toDTO(po);
    }

    private String toFlag(Boolean requested, String fallback) {
        if (requested == null) {
            return fallback == null ? "Y" : fallback;
        }
        return Boolean.TRUE.equals(requested) ? "Y" : "N";
    }
}
