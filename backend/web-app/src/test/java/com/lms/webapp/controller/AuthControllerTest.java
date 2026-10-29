package com.lms.webapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
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
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
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

    @Test
    void registrationKeepsAccountAndExplainsOtpDeliveryFailure() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/identity/auth/register"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andRespond(withSuccess("{\"code\":1000}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://localhost:8080/api/identity/auth/verify-email/send"))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError());
        var result = mockMvc.perform(post("/register").with(csrf())
                        .param("username", "otp_test").param("email", "otp@example.com")
                        .param("password", "test-only-password").param("confirmPassword", "test-only-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/verify-email?email=otp%40example.com"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash()
                        .attribute("otpDeliveryFailed", true)).andReturn();
        mockMvc.perform(get("/verify-email").param("email", "otp@example.com")
                        .flashAttrs(result.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Tài khoản đã được tạo")))
                .andExpect(content().string(containsString("data-delivery-failed=\"true\"")))
                .andExpect(content().string(containsString("Gửi lại mã OTP")));
        mockServer.verify();
    }

    @Test
    void registrationSendsOtpWithoutFailureWarningOnSuccess() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/identity/auth/register"))
                .andRespond(withSuccess("{\"code\":1000}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://localhost:8080/api/identity/auth/verify-email/send"))
                .andRespond(withSuccess("{\"code\":1000}", MediaType.APPLICATION_JSON));
        mockMvc.perform(post("/register").with(csrf())
                        .param("username", "otp_test").param("email", "otp@example.com")
                        .param("password", "test-only-password").param("confirmPassword", "test-only-password"))
                .andExpect(redirectedUrl("/verify-email?email=otp%40example.com"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attributeCount(0));
        mockServer.verify();
    }

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
    }

    @Test
    void loginUnverifiedAccountShowsErrorAndActivationLink() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/identity/auth/login"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.FORBIDDEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":1009,\"message\":\"Tài khoản chưa được kích hoạt qua email\",\"data\":{\"email\":\"alice@example.com\"}}"));

        mockMvc.perform(post("/login")
                        .with(csrf())
                        .param("username", "alice")
                        .param("password", "secret123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attribute("unverifiedUser", true))
                .andExpect(model().attribute("unverifiedEmail", "alice@example.com"));
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
