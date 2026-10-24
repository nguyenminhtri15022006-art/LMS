package com.lms.webapp.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withRawStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

@SpringBootTest
@AutoConfigureMockRestServiceServer
@ActiveProfiles("test")
class GatewayClientTest {

    @Autowired(required = false)
    private GatewayClient gatewayClient;

    @Autowired(required = false)
    private TokenRefreshInterceptor tokenRefreshInterceptor;

    @Autowired(required = false)
    private MockRestServiceServer mockServer;

    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        session = new MockHttpSession();
        request.setSession(session);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void bearerTokenInjectedFromSession() {
        assertThat(gatewayClient).isNotNull();

        session.setAttribute("accessToken", "mock-jwt");

        mockServer.expect(requestTo("http://localhost:8080/api/test"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer mock-jwt"))
                .andRespond(withSuccess("{\"status\":200}", MediaType.APPLICATION_JSON));

        String result = gatewayClient.get("/test", String.class);
        assertThat(result).isNotNull();

        mockServer.verify();
    }

    @Test
    void autoRefreshOn401() {
        // Red 1: tham chiếu tokenRefreshInterceptor khi class chưa tồn tại
        assertThat(tokenRefreshInterceptor).isNotNull();

        session.setAttribute("accessToken", "expired-token");
        session.setAttribute("refreshToken", "valid-refresh-token");

        // 1. Request ban đầu gặp 401 Unauthorized
        mockServer.expect(requestTo("http://localhost:8080/api/test"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer expired-token"))
                .andRespond(withUnauthorizedRequest());

        // 2. Tự động gọi endpoint refresh
        mockServer.expect(requestTo("http://localhost:8080/api/identity/auth/refresh"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"code\":1000,\"result\":{\"accessToken\":\"new-token-999\",\"refreshToken\":\"new-refresh-999\"}}", MediaType.APPLICATION_JSON));

        // 3. Retry lại request ban đầu với token mới
        mockServer.expect(requestTo("http://localhost:8080/api/test"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", "Bearer new-token-999"))
                .andRespond(withSuccess("{\"status\":\"ok\"}", MediaType.APPLICATION_JSON));

        String response = gatewayClient.get("/test", String.class);
        assertThat(response).contains("ok");
        assertThat(session.getAttribute("accessToken")).isEqualTo("new-token-999");

        mockServer.verify();
    }
}
