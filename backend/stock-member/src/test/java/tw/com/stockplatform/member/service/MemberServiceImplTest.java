package tw.com.stockplatform.member.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import tw.com.stockplatform.common.constant.ErrorCode;
import tw.com.stockplatform.common.exception.BusinessException;
import tw.com.stockplatform.common.trace.TraceIdFilter;
import tw.com.stockplatform.common.util.TimeUtils;
import tw.com.stockplatform.domain.enums.UserStatus;
import tw.com.stockplatform.domain.po.UserPO;
import tw.com.stockplatform.member.convertor.MemberConvertor;
import tw.com.stockplatform.member.dto.request.LoginRequest;
import tw.com.stockplatform.member.dto.request.RegisterRequest;
import tw.com.stockplatform.member.dto.response.LoginResponse;
import tw.com.stockplatform.member.dto.response.MemberDTO;
import tw.com.stockplatform.member.repository.MemberMapper;
import tw.com.stockplatform.member.repository.UserPreferenceMapper;
import tw.com.stockplatform.member.security.JwtTokenProvider;
import tw.com.stockplatform.member.security.PasswordEncoder;
import tw.com.stockplatform.member.service.impl.MemberServiceImpl;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock private MemberMapper memberMapper;
    @Mock private UserPreferenceMapper userPreferenceMapper;
    @Mock private MemberConvertor memberConvertor;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private MemberServiceImpl memberService;

    private UserPO existingUser;

    @BeforeEach
    void setUp() {
        existingUser = UserPO.builder()
            .userId("uid-1")
            .email("test@example.com")
            .passwordHash("hashed")
            .displayName("Tester")
            .status(UserStatus.ACTIVE.name())
            .createdAt(TimeUtils.now())
            .build();
    }

    @AfterEach
    void tearDown() {
        // 避免 MDC 被測試之間污染
        MDC.remove(TraceIdFilter.USER_ID_MDC_KEY);
    }

    @Test
    @DisplayName("register：成功時插入 USERS + USER_PREFERENCES，寫入 MDC userId，回傳 DTO")
    void register_Success() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123", "New User");
        MemberDTO expectedDto = new MemberDTO("uid-x", "new@example.com", "New User",
            UserStatus.ACTIVE.name(), TimeUtils.now());

        when(memberMapper.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(memberMapper.insert(any(UserPO.class))).thenReturn(1);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashed");
        when(memberConvertor.toDTO(any(UserPO.class))).thenReturn(expectedDto);

        MemberDTO result = memberService.register(request);

        assertThat(result).isEqualTo(expectedDto);
        // BE-03 驗證：MDC.userId 必須被注入，AuditAspect 才能取得
        assertThat(MDC.get(TraceIdFilter.USER_ID_MDC_KEY)).isNotNull();
        verify(memberMapper).insert(any(UserPO.class));
        verify(userPreferenceMapper).insert(any());
    }

    @Test
    @DisplayName("register：insert 回傳 0 筆時拋 SYSTEM_BUSY（BE-01 驗證 @Insert 回傳 int）")
    void register_InsertNoRows() {
        RegisterRequest request = new RegisterRequest("new@example.com", "password123", "New User");
        when(memberMapper.findByEmail("new@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password123")).thenReturn("$2a$12$hashed");
        when(memberMapper.insert(any(UserPO.class))).thenReturn(0);

        assertThatThrownBy(() -> memberService.register(request))
            .isInstanceOf(BusinessException.class)
            .extracting("code")
            .isEqualTo(ErrorCode.SYSTEM_BUSY.getCode());

        verify(userPreferenceMapper, never()).insert(any());
    }

    @Test
    @DisplayName("register：email 重複時拋 BusinessException(2010)")
    void register_EmailDuplicate() {
        RegisterRequest request = new RegisterRequest("test@example.com", "password123", "Dup");
        when(memberMapper.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> memberService.register(request))
            .isInstanceOf(BusinessException.class)
            .extracting("code")
            .isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED.getCode());

        verify(memberMapper, never()).insert(any(UserPO.class));
    }

    @Test
    @DisplayName("login：密碼正確時回傳 JWT，並寫入 MDC userId（BE-03 audit）")
    void login_Success() {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        MemberDTO dto = new MemberDTO("uid-1", "test@example.com", "Tester",
            UserStatus.ACTIVE.name(), TimeUtils.now());

        when(memberMapper.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken("uid-1", "test@example.com")).thenReturn("jwt-token");
        when(jwtTokenProvider.getAccessTokenTtlSeconds()).thenReturn(900L);
        when(memberConvertor.toDTO(existingUser)).thenReturn(dto);

        LoginResponse response = memberService.login(request);

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresInSeconds()).isEqualTo(900L);
        assertThat(response.member()).isEqualTo(dto);
        // BE-03 驗證：MDC.userId 必須被注入，AuditAspect 才能取得
        assertThat(MDC.get(TraceIdFilter.USER_ID_MDC_KEY)).isEqualTo("uid-1");
    }

    @Test
    @DisplayName("login：密碼錯誤時拋 BusinessException(2011)")
    void login_PasswordIncorrect() {
        LoginRequest request = new LoginRequest("test@example.com", "wrong");
        when(memberMapper.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> memberService.login(request))
            .isInstanceOf(BusinessException.class)
            .extracting("code")
            .isEqualTo(ErrorCode.EMAIL_OR_PASSWORD_INCORRECT.getCode());
    }
}
