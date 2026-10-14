package com.lms.apigateway.configuration;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private static final String SIGNER_KEY = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String OTHER_KEY = "fedcba9876543210fedcba9876543210fedcba9876543210fedcba9876543210";

    private final JwtUtil jwtUtil = new JwtUtil(SIGNER_KEY);

    @Test
    @DisplayName("Case 1: extractToken trích xuất token hợp lệ có prefix Bearer, từ chối null/rỗng/prefix khác")
    void extractTokenReturnsBearerValueOnlyWhenHeaderIsValid() {
        assertThat(jwtUtil.extractToken("Bearer valid-token-value")).contains("valid-token-value");
        assertThat(jwtUtil.extractToken("Bearer   ")).isEmpty();
        assertThat(jwtUtil.extractToken("Basic user:pass")).isEmpty();
        assertThat(jwtUtil.extractToken("")).isEmpty();
        assertThat(jwtUtil.extractToken(null)).isEmpty();
    }

    @Test
    @DisplayName("Case 2: verify chấp nhận Access token hợp lệ và giải mã đầy đủ claims")
    void verifyAcceptsAccessTokenAndExposesGatewayClaims() {
        String token = createToken("user-1", "ROLE_STUDENT", "student@example.com", "ACCESS", 600, SIGNER_KEY);

        assertThat(jwtUtil.verify(token))
                .isPresent()
                .get()
                .satisfies(claims -> {
                    assertThat(claims.getSubject()).isEqualTo("user-1");
                    assertThat(claims.get("scope", String.class)).isEqualTo("ROLE_STUDENT");
                    assertThat(claims.get("email", String.class)).isEqualTo("student@example.com");
                });
    }

    @Test
    @DisplayName("Case 3: verify từ chối refresh token, token sai định dạng, hết hạn hoặc sai chữ ký")
    void verifyRejectsRefreshTokenMalformedExpiredAndWrongSignature() {
        // Refresh token bị từ chối
        String refreshToken = createToken("user-1", "ROLE_STUDENT", "student@example.com", "REFRESH", 600, SIGNER_KEY);
        assertThat(jwtUtil.verify(refreshToken)).isEmpty();

        // Token sai định dạng
        assertThat(jwtUtil.verify("not-a-valid-jwt")).isEmpty();

        // Token hết hạn
        String expiredToken = createToken("user-1", "ROLE_STUDENT", "student@example.com", "ACCESS", -600, SIGNER_KEY);
        assertThat(jwtUtil.verify(expiredToken)).isEmpty();

        // Token sai chữ ký
        String wrongSignatureToken = createToken("user-1", "ROLE_STUDENT", "student@example.com", "ACCESS", 600, OTHER_KEY);
        assertThat(jwtUtil.verify(wrongSignatureToken)).isEmpty();
    }

    private String createToken(String subject, String scope, String email, String type, long durationSeconds, String key) {
        return Jwts.builder()
                .subject(subject)
                .claim("scope", scope)
                .claim("email", email)
                .claim("type", type)
                .expiration(Date.from(Instant.now().plusSeconds(durationSeconds)))
                .signWith(Keys.hmacShaKeyFor(key.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
