package com.lms.webapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockRestServiceServer mockServer;

    @Test
    void loginPageRendersWithCsrf() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeExists("loginForm"))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void loginSuccessSetsSessionAndRedirects() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/identity/auth/login"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Login success\",\"data\":{\"accessToken\":\"test-access-jwt\",\"refreshToken\":\"test-refresh-jwt\",\"user\":{\"id\":\"u-101\",\"username\":\"alice\",\"email\":\"alice@example.com\",\"role\":\"STUDENT\"}}}",
                        MediaType.APPLICATION_JSON
                ));

        MockHttpSession session = new MockHttpSession();

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .session(session)
                        .param("username", "alice")
                        .param("password", "secret123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        assertThat(session.getAttribute("accessToken")).isEqualTo("test-access-jwt");
        assertThat(session.getAttribute("refreshToken")).isEqualTo("test-refresh-jwt");
        assertThat(session.getAttribute("username")).isEqualTo("alice");
        mockServer.verify();
    }

    @Test
    void registerValidationRejectsShortPassword() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "bob")
                        .param("email", "bob@example.com")
                        .param("password", "short7")
                        .param("confirmPassword", "short7"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrorCode("registerForm", "password", "Size"));
    }

    @Test
    void verifyEmailOtpSuccess() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/identity/auth/verify-email"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"OTP verified successfully\"}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(post("/verify-email")
                        .with(csrf())
                        .param("email", "bob@example.com")
                        .param("otp", "123456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?verified=true"));

        mockServer.verify();
    }

    @Test
    void resetPasswordEnforces8Chars() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/identity/auth/reset-password"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Password reset successfully\"}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(post("/reset-password")
                        .with(csrf())
                        .param("email", "bob@example.com")
                        .param("otp", "123456")
                        .param("newPassword", "validpassword123")
                        .param("confirmPassword", "validpassword123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?reset=true"));

        mockServer.verify();
    }

    @Test
    void googleOAuthFlowValidatesState() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("oauth_state", "valid-secret-state-xyz");

        // 1. Giả mạo state -> từ chối 400 Bad Request
        mockMvc.perform(get("/auth/callback/google")
                        .session(session)
                        .param("code", "google-code-123")
                        .param("state", "tampered-state"))
                .andExpect(status().isBadRequest());

        // 2. State chuẩn -> gọi Gateway đổi code lấy token và redirect /
        mockServer.expect(requestTo("http://localhost:8080/api/identity/auth/social-login/google"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Social login success\",\"data\":{\"accessToken\":\"google-jwt-access\",\"refreshToken\":\"google-jwt-refresh\",\"user\":{\"id\":\"u-google-1\",\"username\":\"google_user\",\"email\":\"google@example.com\",\"role\":\"STUDENT\"}}}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(get("/auth/callback/google")
                        .session(session)
                        .param("code", "google-code-123")
                        .param("state", "valid-secret-state-xyz"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"));

        assertThat(session.getAttribute("accessToken")).isEqualTo("google-jwt-access");
        assertThat(session.getAttribute("refreshToken")).isEqualTo("google-jwt-refresh");
        mockServer.verify();
    }

    @Test
    void resendVerifyEmailOtpSuccessCallsGatewayEndpoint() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/identity/auth/verify-email/send"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.email").value("bob@example.com"))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Mã xác thực đã được gửi đến bob@example.com\"}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(post("/verify-email/resend")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bob@example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(1000))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").value(containsString("bob@example.com")));

        mockServer.verify();
    }

    @Test
    void resendVerifyEmailOtpFailureWhenGatewayReturnsError() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/identity/auth/verify-email/send"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.email").value("bob@example.com"))
                .andRespond(withBadRequest()
                        .body("{\"code\":1001,\"message\":\"Email không tồn tại hoặc tài khoản đã được kích hoạt\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/verify-email/resend")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bob@example.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(400))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").isNotEmpty());

        mockServer.verify();
    }

    @Test
    void resendVerifyEmailOtpRejectsBlankEmail() throws Exception {
        mockMvc.perform(post("/verify-email/resend")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(400))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").isNotEmpty());
    }
}
