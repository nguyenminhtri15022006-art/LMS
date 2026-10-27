package com.lms.identityservice.service.impl;

import com.lms.identityservice.dto.request.AuthenticationRequest;
import com.lms.identityservice.dto.request.RefreshTokenRequest;
import com.lms.identityservice.entity.RefreshToken;
import com.lms.identityservice.entity.User;
import com.lms.identityservice.enums.AuthProvider;
import com.lms.identityservice.enums.RoleType;
import com.lms.identityservice.enums.TokenType;
import com.lms.identityservice.exception.ErrorCode;
import com.lms.identityservice.exception.IdentityException;
import com.lms.identityservice.mapper.UserMapper;
import com.lms.identityservice.repository.RefreshTokenRepository;
import com.lms.identityservice.repository.UserRepository;
import com.lms.identityservice.service.GoogleOAuthService;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserLockedConsistencyTest {

    private static final String SIGNER_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Mock
    UserRepository userRepository;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    UserMapper userMapper;

    @Mock
    GoogleOAuthService googleOAuthService;

    AuthenticationServiceImpl authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationServiceImpl(
                userRepository,
                refreshTokenRepository,
                passwordEncoder,
                userMapper,
                googleOAuthService
        );
        ReflectionTestUtils.setField(authenticationService, "signerKey", SIGNER_KEY);
        ReflectionTestUtils.setField(authenticationService, "validDuration", 900L);
        ReflectionTestUtils.setField(authenticationService, "refreshableDuration", 604800L);
    }

    private User lockedUser() {
        return User.builder()
                .userId("locked-user-id")
                .username("locked_user")
                .email("locked@example.com")
                .password("encoded-pass")
                .role(RoleType.STUDENT)
                .authProvider(AuthProvider.LOCAL)
                .active(false) // Đã bị khóa bởi Admin (toggleActive)
                .build();
    }

    private String createSignedToken(String userId, String tokenType) throws JOSEException {
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(userId)
                .claim("type", tokenType)
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + 600000))
                .build();

        JWSObject jwsObject = new JWSObject(
                new JWSHeader(JWSAlgorithm.HS512),
                new Payload(claimsSet.toJSONObject())
        );
        jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes(StandardCharsets.UTF_8)));
        return jwsObject.serialize();
    }

    @Test
    @DisplayName("1. lockedUserCannotLogin: User bị khóa không thể đăng nhập -> ném USER_INACTIVE (mã 1006)")
    void lockedUserCannotLogin() {
        User user = lockedUser();
        when(userRepository.findByUsername("locked_user")).thenReturn(Optional.of(user));

        assertThatExceptionOfType(IdentityException.class)
                .isThrownBy(() -> authenticationService.authenticate(AuthenticationRequest.builder()
                        .username("locked_user")
                        .password("any-pass")
                        .build()))
                .satisfies(ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_INACTIVE);
                    assertThat(ex.getErrorCode().getCode()).isEqualTo(1006);
                });

        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("2. lockedUserCannotRefreshToken: User bị khóa không thể refresh token -> ném USER_INACTIVE (mã 1006)")
    void lockedUserCannotRefreshToken() throws JOSEException {
        User user = lockedUser();
        String validRefreshToken = createSignedToken("locked-user-id", TokenType.REFRESH.name());

        RefreshToken storedToken = RefreshToken.builder()
                .tokenId("token-uuid")
                .user(user)
                .tokenHash("hash-value")
                .revoked(false)
                .build();

        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(storedToken));
        when(userRepository.findById("locked-user-id")).thenReturn(Optional.of(user));

        assertThatExceptionOfType(IdentityException.class)
                .isThrownBy(() -> authenticationService.refreshToken(RefreshTokenRequest.builder()
                        .refreshToken(validRefreshToken)
                        .build()))
                .satisfies(ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.USER_INACTIVE);
                    assertThat(ex.getErrorCode().getCode()).isEqualTo(1006);
                });

        // Đảm bảo không cấp token mới khi user bị khóa
        verify(refreshTokenRepository, never()).save(argThat(token -> !token.isRevoked()));
    }
}
