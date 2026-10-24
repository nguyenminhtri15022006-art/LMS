package com.lms.webapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
@ActiveProfiles("test")
class ProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockRestServiceServer mockServer;

    @Test
    @WithMockUser(username = "alice", roles = "STUDENT")
    void updateProfileCallsPutEndpointWithAccurateFields() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/identity/users/myinfo"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(jsonPath("$.fullname").value("Nguyen Van A"))
                .andExpect(jsonPath("$.phone").value("0912345678"))
                .andExpect(jsonPath("$.dob").value("2000-01-15"))
                .andExpect(jsonPath("$.bio").value("Lap trinh vien Java"))
                .andExpect(jsonPath("$.avatarUrl").value("http://localhost:9000/lms-identity-bucket/avatars/u-101/avatar.png"))
                .andExpect(jsonPath("$.fullName").doesNotExist())
                .andExpect(jsonPath("$.phoneNumber").doesNotExist())
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Profile updated\",\"data\":{\"userId\":\"u-101\",\"fullname\":\"Nguyen Van A\",\"phone\":\"0912345678\",\"dob\":\"2000-01-15\",\"bio\":\"Lap trinh vien Java\",\"avatarUrl\":\"http://localhost:9000/lms-identity-bucket/avatars/u-101/avatar.png\"}}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(post("/profile")
                        .with(csrf())
                        .param("fullname", "Nguyen Van A")
                        .param("phone", "0912345678")
                        .param("dob", "2000-01-15")
                        .param("bio", "Lap trinh vien Java")
                        .param("avatarUrl", "http://localhost:9000/lms-identity-bucket/avatars/u-101/avatar.png"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?updated=true"));

        mockServer.verify();
    }

    @Test
    @WithMockUser(username = "alice", roles = "STUDENT")
    void changePasswordCallsPatchEndpointWithOldNewAndConfirmPassword() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/identity/users/myinfo/password"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(jsonPath("$.oldPassword").value("currentSecret123"))
                .andExpect(jsonPath("$.newPassword").value("newSecret123"))
                .andExpect(jsonPath("$.confirmPassword").value("newSecret123"))
                .andExpect(jsonPath("$.currentPassword").doesNotExist())
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Password updated successfully\",\"data\":null}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(post("/profile/password")
                        .with(csrf())
                        .param("oldPassword", "currentSecret123")
                        .param("newPassword", "newSecret123")
                        .param("confirmPassword", "newSecret123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?passwordUpdated=true"));

        mockServer.verify();
    }

    @Test
    @WithMockUser(username = "alice", roles = "STUDENT")
    void changePasswordFailsValidationWhenMismatch() throws Exception {
        // Mật khẩu xác nhận không khớp -> không gọi Gateway, trả về view profile/index kèm lỗi
        mockMvc.perform(post("/profile/password")
                        .with(csrf())
                        .param("oldPassword", "currentSecret123")
                        .param("newPassword", "newSecret123")
                        .param("confirmPassword", "mismatchSecret456"))
                .andExpect(status().isOk())
                .andExpect(view().name("profile/index"))
                .andExpect(model().attributeHasFieldErrorCode("changePasswordForm", "confirmPassword", "Mismatch"));
    }

    @Test
    @WithMockUser(username = "alice", roles = "STUDENT")
    void updateRoleCallsPatchEndpointAndInvalidatesSession() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/identity/users/myinfo/role"))
                .andExpect(method(HttpMethod.PATCH))
                .andExpect(jsonPath("$.role").value("INSTRUCTOR"))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Role updated\",\"data\":{\"userId\":\"u-101\",\"role\":\"INSTRUCTOR\"}}",
                        MediaType.APPLICATION_JSON
                ));

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("accessToken", "old-student-token");
        session.setAttribute("refreshToken", "old-student-refresh");
        session.setAttribute("username", "alice");
        session.setAttribute("role", "STUDENT");

        mockMvc.perform(post("/profile/role")
                        .with(csrf())
                        .session(session)
                        .param("role", "INSTRUCTOR"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?roleUpdated=true"));

        // Kết thúc phiên làm việc để xóa token cũ, buộc đăng nhập lại để nhận token mới chứa role INSTRUCTOR
        assertThat(session.isInvalid()).isTrue();
        mockServer.verify();
    }

    @Test
    @WithMockUser(username = "alice", roles = "STUDENT")
    void avatarPresignedPostRequested() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/identity/storage/presigned-url"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Presigned URL generated\",\"data\":{\"uploadUrl\":\"http://localhost:9000/lms-identity-bucket\",\"formFields\":{\"key\":\"avatars/u-101/avatar.png\",\"policy\":\"mock-policy-123\"},\"fileUrl\":\"http://localhost:9000/lms-identity-bucket/avatars/u-101/avatar.png\"}}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(post("/profile/avatar/presigned-url")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fileName\":\"avatar.png\",\"fileType\":\"image/png\",\"folder\":\"avatars\"}"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.uploadUrl").value("http://localhost:9000/lms-identity-bucket"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.formFields.key").value("avatars/u-101/avatar.png"));

        mockServer.verify();
    }
}
