package com.lms.courseservice.configuration;



import com.fasterxml.jackson.databind.ObjectMapper;

import com.lms.courseservice.exception.ErrorCode;

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

import org.springframework.security.web.SecurityFilterChain;

import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import org.springframework.web.filter.OncePerRequestFilter;



import java.io.IOException;

import java.nio.charset.StandardCharsets;

import java.time.Instant;

import java.util.ArrayList;

import java.util.List;

import java.util.Map;



@Slf4j

@Configuration

@EnableWebSecurity

@EnableMethodSecurity

public class SecurityConfig {



    private static final String[] PUBLIC_ENDPOINTS = {

            "/actuator/**",

            "/error",

            "/internal/**"

    };



    private static final String[] PUBLIC_GET_ENDPOINTS = {

            "/categories/**",

            "/courses",

            "/courses/*",

            "/courses/*/**"

    };



    private final ObjectMapper objectMapper;



    public SecurityConfig(ObjectMapper objectMapper) {

        this.objectMapper = objectMapper;

    }



    @Bean

    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {

        httpSecurity.sessionManagement(

                        s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(

                        auth -> auth

                                .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                                .requestMatchers(HttpMethod.GET, PUBLIC_GET_ENDPOINTS).permitAll()

                                .anyRequest().authenticated());



        httpSecurity.addFilterBefore(gatewayHeaderFilter(), UsernamePasswordAuthenticationFilter.class)

                .exceptionHandling(e -> e

                        .authenticationEntryPoint((req, res, ex) ->

                                writeError(res, ErrorCode.UNAUTHENTICATED))

                        .accessDeniedHandler((req, res, ex) ->

                                writeError(res, ErrorCode.ACCESS_DENIED)));



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



                String userId = request.getHeader("X-User-Id");
                String role = request.getHeader("X-User-Role");
                if (role == null || role.isBlank()) {
                    role = request.getHeader("X-User-Roles");
                }
                String email = request.getHeader("X-User-Email");

                // Nếu request có thông tin xác thực từ Gateway, luôn nạp Authentication vào SecurityContext
                if (userId != null && !userId.isBlank()) {
                    List<GrantedAuthority> authorities = new ArrayList<>();
                    if (role != null && !role.isBlank()) {
                        for (String r : role.split(",")) {
                            String trimmed = r.trim();
                            if (!trimmed.isEmpty()) {
                                authorities.add(new SimpleGrantedAuthority(trimmed));
                                if (!trimmed.startsWith("ROLE_")) {
                                    authorities.add(new SimpleGrantedAuthority("ROLE_" + trimmed));
                                } else if (trimmed.length() > 5) {
                                    authorities.add(new SimpleGrantedAuthority(trimmed.substring(5)));
                                }
                            }
                        }
                    }
                    GatewayAuthentication auth = new GatewayAuthentication(userId, email, role, authorities);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    filterChain.doFilter(request, response);
                    return;
                }

                // Nếu không có X-User-Id, kiểm tra xem có phải endpoint công khai (Public) không
                if (isPublicPath(path) || isPublicGetPath(request)) {
                    filterChain.doFilter(request, response);
                    return;
                }

                log.warn("Request đến {} không có X-User-Id header — từ chối 401", path);
                writeError(response, ErrorCode.UNAUTHENTICATED);
                return;
            }





            private boolean isPublicPath(String path) {

                for (String pattern : PUBLIC_ENDPOINTS) {

                    String cleanPattern = pattern.replace("/**", "");

                    if (path.startsWith(cleanPattern) || path.equals(cleanPattern)) return true;

                }

                return false;

            }



            private boolean isPublicGetPath(HttpServletRequest request) {

                if (!HttpMethod.GET.name().equals(request.getMethod())) return false;



                String path = normalizedPath(request);

                if (path.equals("/courses/my") || path.equals("/courses/enrolled") || path.contains("/progress")) {
                    return false;
                }

                return path.startsWith("/categories") || path.startsWith("/courses");

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

}

