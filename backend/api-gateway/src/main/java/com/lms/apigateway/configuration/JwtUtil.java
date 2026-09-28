package com.lms.apigateway.configuration;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
public class JwtUtil {

    private static final Logger log = LoggerFactory.getLogger(JwtUtil.class);

    private final SecretKey secretKey;

    public JwtUtil(@Value("${jwt.signer-key:0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef}") String signerKey) {
        this.secretKey = Keys.hmacShaKeyFor(signerKey.getBytes(StandardCharsets.UTF_8));
    }

    public Optional<Claims> verify(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // Kiểm tra đây là ACCESS token, không phải REFRESH token
            String type = claims.get("type", String.class);
            if (!"ACCESS".equals(type)) {
                log.warn("Token type không hợp lệ: {}", type);
                return Optional.empty();
            }

            return Optional.of(claims);

        } catch (JwtException e) {
            log.debug("JWT không hợp lệ: {}", e.getMessage());
            return Optional.empty();
        } catch (Exception e) {
            log.error("Lỗi không xác định khi verify JWT", e);
            return Optional.empty();
        }
    }

    public Optional<String> extractToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        String token = authorizationHeader.substring(7).trim();
        return token.isBlank() ? Optional.empty() : Optional.of(token);
    }
}
