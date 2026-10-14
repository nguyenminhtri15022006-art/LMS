package com.lms.identityservice.controller;

import com.lms.identityservice.configuration.SecurityConfig;
import com.lms.identityservice.dto.request.ForgotPasswordRequest;
import com.lms.identityservice.dto.request.ResetPasswordRequest;
import com.lms.identityservice.dto.response.AuthenticationResponse;
import com.lms.identityservice.dto.response.UserCreationResponse;
import com.lms.identityservice.dto.response.UserFullResponse;
import com.lms.identityservice.service.AuthenticationService;
import com.lms.identityservice.service.EmailVerificationService;
import com.lms.identityservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        AuthenticationController.class,
        EmailVerificationController.class,
        UserController.class
})
@Import(SecurityConfig.class)
class IdentityFunctionalTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AuthenticationService authenticationService;

    @MockBean
    EmailVerificationService emailVerificationService;

    @MockBean
    UserService userService;

    @Test
    void userRegistersLogsInAndUsesGatewayAuthenticatedProfileWorkflow() throws Exception {
        when(authenticationService.createUser(any())).thenReturn(UserCreationResponse.builder()
                .userId("user-1")
                .username("student01")
                .email("student@example.com")
                .role("STUDENT")
                .build());
        when(authenticationService.authenticate(any())).thenReturn(AuthenticationResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .user(UserFullResponse.builder()
                        .userId("user-1")
                        .username("student01")
                        .email("student@example.com")
                        .role("STUDENT")
                        .build())
                .build());
        when(userService.getMyinfo()).thenReturn(UserFullResponse.builder()
                .userId("user-1")
                .username("student01")
                .email("student@example.com")
                .role("STUDENT")
                .build());
        when(userService.updateRole(any())).thenReturn(UserFullResponse.builder()
                .userId("user-1")
                .role("INSTRUCTOR")
                .roleSelected(true)
                .build());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student01",
                                  "password": "secret123",
                                  "fullname": "Student One",
                                  "email": "student@example.com",
                                  "role": "STUDENT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value("user-1"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student01",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"));

        mockMvc.perform(get("/users/myinfo")
                        .headers(gatewayHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("student@example.com"));

        mockMvc.perform(patch("/users/myinfo/role")
                        .headers(gatewayHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "INSTRUCTOR"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("INSTRUCTOR"))
                .andExpect(jsonPath("$.data.roleSelected").value(true));

        verify(authenticationService).createUser(any());
        verify(authenticationService).authenticate(any());
        verify(userService).getMyinfo();
        verify(userService).updateRole(any());
    }

    @Test
    void forgotPasswordAndResetPasswordFlow() throws Exception {
        doNothing().when(emailVerificationService).sendResetPassword(any(ForgotPasswordRequest.class));
        doNothing().when(emailVerificationService).resetPassword(any(ResetPasswordRequest.class));

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "student@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Nếu email tồn tại, mã xác thực đã được gửi đến hộp thư của bạn"));

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "student@example.com",
                                  "otp": "123456",
                                  "newPassword": "newPassword123",
                                  "confirmPassword": "newPassword123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Đặt lại mật khẩu thành công"));

        verify(emailVerificationService).sendResetPassword(any(ForgotPasswordRequest.class));
        verify(emailVerificationService).resetPassword(any(ResetPasswordRequest.class));
    }

    private HttpHeaders gatewayHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "user-1");
        headers.add("X-User-Role", "ROLE_STUDENT");
        headers.add("X-User-Email", "student@example.com");
        return headers;
    }
}
