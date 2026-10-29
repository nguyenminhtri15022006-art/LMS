package com.lms.identityservice.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.identityservice.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Security Config cho Identity Service — Token Offloading pattern.
 * <p>
 * Identity Service KHÔNG verify JWT nữa.
 * API Gateway đã verify và forward userId/role qua header.
 * Service này chỉ đọc header X-User-Id, X-User-Role.
 * <p>
 * Lưu ý: Identity Service vẫn là nơi DUY NHẤT SIGN JWT (trong AuthenticationService).
 * Nhưng nó KHÔNG verify JWT của chính mình nữa — Gateway làm việc đó.
 */

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_ENDPOINTS = {
            "/auth/**",
            // Internal endpoints — gọi từ service khác, không qua Gateway
            "/internal/**",
            // Swagger
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/actuator/**"
    };

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.sessionManagement(
                        s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth -> auth
                                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                                .requestMatchers(HttpMethod.GET, "/users/*/profile").permitAll()
                                .requestMatchers("/admin/**").hasAnyAuthority("ROLE_ADMIN", "ADMIN")
                                .anyRequest().authenticated());

        httpSecurity.addFilterBefore(gatewayHeaderFilter(), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e
                        .authenticationEntryPoint((req, res, ex) ->
                                writeError(res, ErrorCode.UNAUTHENTICATED))
                        .accessDeniedHandler((req, res, ex) ->
                                writeError(res, ErrorCode.UNAUTHORIZED)));

        httpSecurity.csrf(AbstractHttpConfigurer::disable);

        return httpSecurity.build();
    }

    @Bean
    public OncePerRequestFilter gatewayHeaderFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request,
                                            HttpServletResponse response,
                                            FilterChain filterChain)
                    throws ServletException, IOException {

                String path = normalizedPath(request);

                // bỏ qua cho public endpoint vs internal endpoint
                if (isPublicPath(path)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                String userId = request.getHeader("X-User-Id");
                String role = request.getHeader("X-User-Role");
                if (role == null || role.isBlank()) {
                    role = request.getHeader("X-User-Roles");
                }
                String email = request.getHeader("X-User-Email");

                if (userId == null || userId.isBlank()) {
                    log.warn("Request đến {} không có X-User-Id header — có thể bypass Gateway", path);
                    writeError(response, ErrorCode.UNAUTHENTICATED);
                    return;
                }

                // Build Authentication from Header (hỗ trợ cả X-User-Role, X-User-Roles và comma-separated roles)
                List<GrantedAuthority> authorities = new ArrayList<>();
                if (role != null && !role.isBlank()) {
                    for (String r : role.split(",")) {
                        String trimmed = r.trim();
                        if (!trimmed.isEmpty()) {
                            String withRole = trimmed.startsWith("ROLE_") ? trimmed : "ROLE_" + trimmed;
                            String withoutRole = trimmed.startsWith("ROLE_") ? trimmed.substring(5) : trimmed;
                            authorities.add(new SimpleGrantedAuthority(withRole));
                            authorities.add(new SimpleGrantedAuthority(withoutRole));
                        }
                    }
                }

                GatewayAuthentication auth = new GatewayAuthentication(userId, email, role, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

                log.debug("Identity xác minh header: userId: {}, Role: {}, Email: {}, Authorities: {}",
                        GatewayAuthentication.currentUserId(),
                        GatewayAuthentication.currentRole(),
                        GatewayAuthentication.currentEmail(),
                        authorities);

                filterChain.doFilter(request, response);
            }

            private boolean isPublicPath(String path) {
                for (String pattern : PUBLIC_ENDPOINTS) {
                    String cleanPattern = pattern.replace("/**", "");
                    if (path.startsWith(cleanPattern) || path.equals(cleanPattern)) return true;
                }
                return path.matches("^/users/[^/]+/profile$");
            }

            private String normalizedPath(HttpServletRequest request) {
                String contextPath = request.getContextPath();
                String requestUri = request.getRequestURI();
                if (contextPath != null && !contextPath.isBlank() && requestUri.startsWith(contextPath)) {
                    requestUri = requestUri.substring(contextPath.length());
                }
                return requestUri.isBlank() ? "/" : requestUri;
            }
        };
    }

    private void writeError(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        response.setStatus(errorCode.getHttpStatusCode().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        Map<String, Object> body = Map.of(
                "code", errorCode.getCode(),
                "message", errorCode.getMessage(),
                "timestamp", Instant.now().toString(),
                "status", errorCode.getHttpStatusCode()
        );
        response.getOutputStream().write(objectMapper.writeValueAsBytes(body));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }
}
