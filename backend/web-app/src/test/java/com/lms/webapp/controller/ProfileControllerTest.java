package com.lms.webapp.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
    void updateRoleCallsPatchEndpoint() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/identity/users/myinfo/role"))
                .andExpect(method(HttpMethod.PATCH))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Role updated\",\"data\":{\"id\":\"u-101\",\"role\":\"INSTRUCTOR\"}}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(post("/profile/role")
                        .with(csrf())
                        .param("role", "INSTRUCTOR"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/profile?roleUpdated=true"));

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
                .andExpect(jsonPath("$.uploadUrl").value("http://localhost:9000/lms-identity-bucket"))
                .andExpect(jsonPath("$.formFields.key").value("avatars/u-101/avatar.png"));

        mockServer.verify();
    }
}
