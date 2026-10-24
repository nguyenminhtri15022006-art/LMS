package com.lms.webapp.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

@Component
public class TokenRefreshInterceptor implements ClientHttpRequestInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TokenRefreshInterceptor.class);
    private static final String RETRY_HEADER = "X-LMS-Retry";

    private final ObjectProvider<RestClient> restClientProvider;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public TokenRefreshInterceptor(ObjectProvider<RestClient> restClientProvider,
                                   @Value("${lms.gateway.base-url:http://localhost:8080/api}") String baseUrl,
                                   ObjectMapper objectMapper) {
        this.restClientProvider = restClientProvider;
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        ClientHttpResponse response = execution.execute(request, body);

        if (response.getStatusCode() == HttpStatus.UNAUTHORIZED) {
            String uriString = request.getURI().toString();
            if (request.getHeaders().containsKey(RETRY_HEADER) || uriString.contains("/identity/auth/refresh")) {
                return response;
            }

            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return response;
            }
            HttpServletRequest servletRequest = attributes.getRequest();
            HttpSession session = servletRequest.getSession(false);
            if (session == null) {
                return response;
            }

            String refreshToken = (String) session.getAttribute("refreshToken");
            if (refreshToken == null || refreshToken.isBlank()) {
                return response;
            }

            String newAccessToken = doRefreshToken(refreshToken, session);
            if (newAccessToken != null) {
                RestClient client = restClientProvider.getObject();
                return client.method(request.getMethod())
                        .uri(request.getURI())
                        .headers(h -> {
                            h.putAll(request.getHeaders());
                            h.set(RETRY_HEADER, "true");
                            h.setBearerAuth(newAccessToken);
                        })
                        .body(body)
                        .exchange((clientRequest, clientResponse) -> clientResponse, false);
            }
        }

        return response;
    }

    private String doRefreshToken(String refreshToken, HttpSession session) {
        try {
            RestClient client = restClientProvider.getObject();
            String responseBody = client.post()
                    .uri(baseUrl + "/identity/auth/refresh")
                    .header(RETRY_HEADER, "true")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("{\"refreshToken\":\"" + refreshToken + "\"}")
                    .retrieve()
                    .body(String.class);

            if (responseBody != null) {
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode resultNode = root.path("result");
                if (!resultNode.isMissingNode()) {
                    String newAccessToken = resultNode.path("accessToken").asText();
                    String newRefreshToken = resultNode.path("refreshToken").asText(refreshToken);

                    session.setAttribute("accessToken", newAccessToken);
                    session.setAttribute("refreshToken", newRefreshToken);
                    return newAccessToken;
                }
            }
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            session.invalidate();
        }
        return null;
    }
}
