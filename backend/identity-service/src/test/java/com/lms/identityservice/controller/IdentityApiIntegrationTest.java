package com.lms.identityservice.controller;

import com.lms.identityservice.configuration.SecurityConfig;
import com.lms.identityservice.dto.response.AuthenticationResponse;
import com.lms.identityservice.dto.response.PresignedUrlResponse;
import com.lms.identityservice.dto.response.UserFullResponse;
import com.lms.identityservice.dto.response.UserPublicProfileResponse;
import com.lms.identityservice.service.AdminUserService;
import com.lms.identityservice.service.AuthenticationService;
import com.lms.identityservice.service.EmailVerificationService;
import com.lms.identityservice.service.InternalUserService;
import com.lms.identityservice.service.StorageService;
import com.lms.identityservice.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
        AuthenticationController.class,
        EmailVerificationController.class,
        UserController.class,
        InternalUserController.class,
        AdminUserController.class,
        StorageController.class
})
@Import(SecurityConfig.class)
class IdentityApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AuthenticationService authenticationService;

    @MockBean
    EmailVerificationService emailVerificationService;

    @MockBean
    UserService userService;

    @MockBean
    InternalUserService internalUserService;

    @MockBean
    AdminUserService adminUserService;

    @MockBean
    StorageService storageService;

    @Test
    void registerValidationErrorsReturnFieldMapWithoutCallingService() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "ab",
                                  "password": "secret123",
                                  "fullname": "Student One",
                                  "email": "student@example.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(9998))
                .andExpect(jsonPath("$.data.username").exists());

        verify(authenticationService, never()).createUser(any());
    }

    @Test
    void loginDelegatesToServiceAndSerializesTokenResponse() throws Exception {
        when(authenticationService.authenticate(any())).thenReturn(AuthenticationResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .user(UserFullResponse.builder()
                        .userId("user-1")
                        .username("student")
                        .build())
                .build());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "student",
                                  "password": "secret123",
                                  "ipAddress": "127.0.0.1",
                                  "deviceInfo": "JUnit"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.user.userId").value("user-1"));
    }

    @Test
    void privateEndpointRejectsRequestWithoutGatewayUserHeader() throws Exception {
        mockMvc.perform(get("/users/myinfo"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(1001));

        verify(userService, never()).getMyinfo();
    }

    @Test
    void internalGetUserHttpContractReturns200AndUserDetails() throws Exception {
        when(internalUserService.getUserById("user-1")).thenReturn(UserFullResponse.builder()
                .userId("user-1")
                .username("student01")
                .email("student01@example.com")
                .role("STUDENT")
                .build());

        mockMvc.perform(get("/internal/user/user-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.username").value("student01"))
                .andExpect(jsonPath("$.email").value("student01@example.com"))
                .andExpect(jsonPath("$.role").value("STUDENT"));

        verify(internalUserService).getUserById("user-1");
    }

    @Test
    void internalPostUsersBatchHttpContractReturns200() throws Exception {
        when(internalUserService.getUsersByIds(any())).thenReturn(List.of(
                UserFullResponse.builder()
                        .userId("user-1")
                        .email("student01@example.com")
                        .build(),
                UserFullResponse.builder()
                        .userId("user-2")
                        .email("student02@example.com")
                        .build()
        ));

        mockMvc.perform(post("/internal/users/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"user-1\", \"user-2\"]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value("user-1"))
                .andExpect(jsonPath("$[1].userId").value("user-2"));

        verify(internalUserService).getUsersByIds(any());
    }

    @Test
    void internalUserExistsHttpContractReturnsBoolean() throws Exception {
        when(internalUserService.existsById("user-1")).thenReturn(true);
        when(internalUserService.existsById("user-999")).thenReturn(false);

        mockMvc.perform(get("/internal/users/user-1/exists"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        mockMvc.perform(get("/internal/users/user-999/exists"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(internalUserService).existsById("user-1");
        verify(internalUserService).existsById("user-999");
    }

    @Test
    void internalSearchUserByEmailHttpContractReturnsUser() throws Exception {
        when(internalUserService.findByEmail(eq("student01@example.com"))).thenReturn(UserFullResponse.builder()
                .userId("user-1")
                .username("student01")
                .email("student01@example.com")
                .build());

        mockMvc.perform(get("/internal/users/search/by-email")
                        .param("email", "student01@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-1"))
                .andExpect(jsonPath("$.email").value("student01@example.com"));

        verify(internalUserService).findByEmail("student01@example.com");
    }

    @Test
    void adminCreateAdminRejectsNonAdminWith403() throws Exception {
        mockMvc.perform(post("/admin/users/admins")
                        .header("X-User-Id", "user-student")
                        .header("X-User-Role", "ROLE_STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "hacker_admin",
                                  "password": "Password@123",
                                  "fullname": "Hacker",
                                  "email": "hacker@example.com"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(1002));

        verify(adminUserService, never()).createAdmin(any());
    }

    @Test
    void adminCreateAdminSucceedsForAdminRole() throws Exception {
        when(adminUserService.createAdmin(any())).thenReturn(UserFullResponse.builder()
                .userId("admin-1")
                .username("newadmin")
                .email("admin@example.com")
                .build());

        mockMvc.perform(post("/admin/users/admins")
                        .header("X-User-Id", "admin-root")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "newadmin",
                                  "password": "Password@123",
                                  "fullname": "New Administrator",
                                  "email": "admin@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.username").value("newadmin"));

        verify(adminUserService).createAdmin(any());
    }

    @Test
    void publicProfileHttpReturns200WithoutAuthentication() throws Exception {
        when(userService.getPublicProfile("user-public-1")).thenReturn(UserPublicProfileResponse.builder()
                .userId("user-public-1")
                .fullname("Public Teacher")
                .avatarUrl("https://storage.example.com/avatar.png")
                .bio("Teacher bio")
                .role("INSTRUCTOR")
                .build());

        mockMvc.perform(get("/users/user-public-1/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.userId").value("user-public-1"))
                .andExpect(jsonPath("$.data.fullname").value("Public Teacher"))
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.password").doesNotExist());

        verify(userService).getPublicProfile("user-public-1");
    }

    @Test
    void storagePresignedUrlHttpReturns200WithUploadUrl() throws Exception {
        when(storageService.generatePresignedUrl(eq("avatar.png"), eq("image/png"), eq("avatars")))
                .thenReturn(PresignedUrlResponse.builder()
                        .uploadUrl("http://localhost:9000/lms-identity-bucket")
                        .formFields(Map.of("key", "avatars/user-1-uuid.png", "policy", "base64policy"))
                        .publicUrl("http://localhost:9000/lms-identity-bucket/avatars/user-1-uuid.png")
                        .build());

        mockMvc.perform(post("/storage/presigned-url")
                        .header("X-User-Id", "user-1")
                        .header("X-User-Role", "ROLE_STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fileName": "avatar.png",
                                  "fileType": "image/png",
                                  "folder": "avatars"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.uploadUrl").value("http://localhost:9000/lms-identity-bucket"))
                .andExpect(jsonPath("$.data.formFields.key").value("avatars/user-1-uuid.png"))
                .andExpect(jsonPath("$.data.publicUrl").value("http://localhost:9000/lms-identity-bucket/avatars/user-1-uuid.png"));

        verify(storageService).generatePresignedUrl("avatar.png", "image/png", "avatars");
    }
}
