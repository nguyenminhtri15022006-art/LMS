package com.lms.apigateway.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.apigateway.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

/**
 * AuthFilter — Gateway Filter thực hiện Token Offloading & Whitelist.
 * Xác thực JWT tại Gateway và tiêm header định danh người dùng xuống microservices.
 */
@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    public AuthFilter(JwtUtil jwtUtil, ObjectMapper objectMapper) {
        super(Config.class);
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // Bỏ qua xác thực cho OPTIONS và các request GET thuộc Whitelist
            if (HttpMethod.OPTIONS.equals(request.getMethod()) || isPublicRequest(request)) {
                ServerHttpRequest publicRequest = request.mutate()
                        .headers(this::removeGatewayAuthHeaders)
                        .build();
                return chain.filter(exchange.mutate().request(publicRequest).build());
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            // 1. Trích xuất token từ Authorization header
            Optional<String> tokenOpt = jwtUtil.extractToken(authHeader);
            if (tokenOpt.isEmpty()) {
                return writeError(exchange, ErrorCode.UNAUTHENTICATED);
            }

            // 2. Xác thực JWT
            Optional<Claims> claimsOpt = jwtUtil.verify(tokenOpt.get());
            if (claimsOpt.isEmpty()) {
                return writeError(exchange, ErrorCode.UNAUTHENTICATED);
            }

            Claims claims = claimsOpt.get();
            String userId = claims.getSubject();
            String role = claims.get("scope", String.class);
            String email = claims.get("email", String.class);

            log.debug("Gateway xác thực thành công: userId={} role={} path={}", userId, role, request.getPath());

            // 3. Mutate request: Xóa các headers cũ và inject headers xác thực Gateway
            ServerHttpRequest mutatedRequest = request.mutate()
                    .headers(httpHeaders -> {
                        removeGatewayAuthHeaders(httpHeaders);
                        httpHeaders.set("X-User-Id", userId != null ? userId : "");
                        httpHeaders.set("X-User-Role", role != null ? role : "");
                        httpHeaders.set("X-User-Roles", role != null ? role : "");
                        httpHeaders.set("X-User-Email", email != null ? email : "");
                        httpHeaders.set("X-Gateway-Authenticated", "true");
                    })
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    private Mono<Void> writeError(ServerWebExchange exchange, ErrorCode errorCode) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(errorCode.getHttpStatusCode());
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "code", errorCode.getCode(),
                "message", errorCode.getMessage(),
                "timestamp", Instant.now().toString(),
                "status", errorCode.getHttpStatusCode().value(),
                "path", exchange.getRequest().getPath().value()
        );

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            byte[] fallback = ("{\"error\":\"" + errorCode + "\"}").getBytes(StandardCharsets.UTF_8);
            DataBuffer buffer = response.bufferFactory().wrap(fallback);
            return response.writeWith(Mono.just(buffer));
        }
    }

    private void removeGatewayAuthHeaders(HttpHeaders httpHeaders) {
        httpHeaders.remove(HttpHeaders.AUTHORIZATION);
        httpHeaders.remove("X-User-Id");
        httpHeaders.remove("X-User-Role");
        httpHeaders.remove("X-User-Roles");
        httpHeaders.remove("X-User-Email");
        httpHeaders.remove("X-Gateway-Authenticated");
    }

    private boolean isPublicRequest(ServerHttpRequest request) {
        if (!HttpMethod.GET.equals(request.getMethod())) return false;

        String path = request.getPath().value();
        String normalizedPath = path.startsWith("/api/") ? path.substring(4) : path;

        return normalizedPath.equals("/blog/posts/public")
                || normalizedPath.startsWith("/blog/posts/public/")
                || normalizedPath.equals("/blog/tags")
                || normalizedPath.startsWith("/blog/tags/search")
                || normalizedPath.matches("^/blog/posts/[^/]+/comments$")
                || normalizedPath.equals("/course/categories")
                || normalizedPath.equals("/course/courses")
                || normalizedPath.startsWith("/course/courses/public")
                || normalizedPath.matches("^/course/courses/(?!my$|enrolled$|users$)[^/]+$")
                || normalizedPath.matches("^/course/courses/[^/]+/(students|lessons)/count$")
                || normalizedPath.matches("^/course/courses/[^/]+/structure$")
                || normalizedPath.matches("^/course/courses/[^/]+/structure/lessons$");
    }

    public static class Config {
    }
}
