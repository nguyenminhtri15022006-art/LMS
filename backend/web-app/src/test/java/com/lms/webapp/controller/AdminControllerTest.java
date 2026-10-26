package com.lms.webapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.webapp.client.GatewayClient;
import com.lms.webapp.dto.form.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GatewayClient gatewayClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("1. Admin endpoints reject non-admin users with 403 Forbidden")
    @WithMockUser(username = "regular_student", roles = {"STUDENT"})
    void adminEndpointsRejectNonAdminWith403() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/courses"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/tags"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("2. Dashboard calls stats endpoint and loads KPI metrics")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void dashboardCallsStatsEndpoint() throws Exception {
        String statsJson = """
            {
                "code": 1000,
                "data": {
                    "totalStudents": 150,
                    "newStudentsThisWeek": 12,
                    "totalInstructors": 18,
                    "newInstructorsThisMonth": 3,
                    "totalAdmins": 2
                }
            }
            """;
        JsonNode statsNode = objectMapper.readTree(statsJson);
        when(gatewayClient.get(eq("/identity/admin/users/stats"), eq(JsonNode.class)))
                .thenReturn(statsNode);

        String coursesJson = """
            {
                "code": 1000,
                "data": {
                    "totalElements": 25,
                    "content": []
                }
            }
            """;
        JsonNode coursesNode = objectMapper.readTree(coursesJson);
        when(gatewayClient.get(contains("/course/courses"), eq(JsonNode.class)))
                .thenReturn(coursesNode);

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attributeExists("stats"))
                .andExpect(model().attributeExists("totalCourses"));

        verify(gatewayClient).get(eq("/identity/admin/users/stats"), eq(JsonNode.class));
    }

    @Test
    @DisplayName("3. Users list loads and passes filter and pagination params to Gateway")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void usersListLoadsAndFilters() throws Exception {
        String usersPageJson = """
            {
                "code": 1000,
                "data": {
                    "content": [
                        {
                            "userId": "u-001",
                            "username": "student1",
                            "fullname": "Nguyen Van A",
                            "email": "a@example.com",
                            "role": "STUDENT",
                            "active": true
                        }
                    ],
                    "totalElements": 1,
                    "totalPages": 1,
                    "number": 0,
                    "size": 10
                }
            }
            """;
        JsonNode usersPageNode = objectMapper.readTree(usersPageJson);
        when(gatewayClient.get(contains("/identity/admin/users"), eq(JsonNode.class)))
                .thenReturn(usersPageNode);

        mockMvc.perform(get("/admin/users")
                        .param("page", "0")
                        .param("size", "10")
                        .param("role", "STUDENT")
                        .param("keyword", "Nguyen"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/users/list"))
                .andExpect(model().attributeExists("usersPage"))
                .andExpect(model().attribute("selectedRole", "STUDENT"))
                .andExpect(model().attribute("keyword", "Nguyen"));

        verify(gatewayClient).get(argThat(uri -> uri.contains("/identity/admin/users")
                && uri.contains("role=STUDENT")
                && uri.contains("keyword=Nguyen")), eq(JsonNode.class));
    }

    @Test
    @DisplayName("4. Toggle user active calls PATCH endpoint")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void toggleUserActiveCallsPatchEndpoint() throws Exception {
        String responseJson = """
            {
                "code": 1000,
                "data": {
                    "userId": "u-123",
                    "active": false
                }
            }
            """;
        when(gatewayClient.patch(eq("/identity/admin/users/u-123/active"), isNull(), eq(JsonNode.class)))
                .thenReturn(objectMapper.readTree(responseJson));

        mockMvc.perform(post("/admin/users/u-123/toggle-active").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(gatewayClient).patch(eq("/identity/admin/users/u-123/active"), isNull(), eq(JsonNode.class));
    }

    @Test
    @DisplayName("5. Admin reset password calls PATCH endpoint with AdminResetPasswordRequest")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void adminResetPasswordCallsPatchEndpoint() throws Exception {
        String responseJson = """
            {
                "code": 1000,
                "data": {
                    "userId": "u-123"
                }
            }
            """;
        when(gatewayClient.patch(eq("/identity/admin/users/u-123/password"), any(), eq(JsonNode.class)))
                .thenReturn(objectMapper.readTree(responseJson));

        mockMvc.perform(post("/admin/users/u-123/reset-password")
                        .with(csrf())
                        .param("newPassword", "NewStrongPass123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(gatewayClient).patch(eq("/identity/admin/users/u-123/password"),
                argThat(body -> body instanceof Map && "NewStrongPass123".equals(((Map<?, ?>) body).get("newPassword"))),
                eq(JsonNode.class));
    }

    @Test
    @DisplayName("6. Toggle admin active calls PATCH endpoint")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void toggleAdminActiveCallsPatchEndpoint() throws Exception {
        String responseJson = """
            {
                "code": 1000,
                "data": {
                    "userId": "u-admin-2",
                    "active": true
                }
            }
            """;
        when(gatewayClient.patch(eq("/identity/admin/users/u-admin-2/admin-active"), isNull(), eq(JsonNode.class)))
                .thenReturn(objectMapper.readTree(responseJson));

        mockMvc.perform(post("/admin/users/u-admin-2/toggle-admin-active").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(gatewayClient).patch(eq("/identity/admin/users/u-admin-2/admin-active"), isNull(), eq(JsonNode.class));
    }

    @Test
    @DisplayName("7. Create admin calls POST /identity/admin/users/admins endpoint")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void createAdminCallsPostEndpoint() throws Exception {
        String responseJson = """
            {
                "code": 1000,
                "data": {
                    "userId": "u-new-admin"
                }
            }
            """;
        when(gatewayClient.post(eq("/identity/admin/users/admins"), any(), eq(JsonNode.class)))
                .thenReturn(objectMapper.readTree(responseJson));

        mockMvc.perform(post("/admin/users/admins")
                        .with(csrf())
                        .param("username", "admin_new")
                        .param("password", "Pass123456")
                        .param("email", "newadmin@example.com")
                        .param("fullname", "Super Admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(gatewayClient).post(eq("/identity/admin/users/admins"),
                argThat(body -> body instanceof Map && "admin_new".equals(((Map<?, ?>) body).get("username"))),
                eq(JsonNode.class));
    }

    @Test
    @DisplayName("8. Courses list loads and filters admin courses via Gateway")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void coursesListLoadsAndFilters() throws Exception {
        String coursesPageJson = """
            {
                "code": 1000,
                "data": {
                    "content": [
                        {
                            "id": "c-101",
                            "title": "Spring Boot Masterclass",
                            "status": "PUBLIC",
                            "level": "BEGINNER"
                        }
                    ],
                    "totalElements": 1,
                    "totalPages": 1
                }
            }
            """;
        when(gatewayClient.get(contains("/course/courses"), eq(JsonNode.class)))
                .thenReturn(objectMapper.readTree(coursesPageJson));

        mockMvc.perform(get("/admin/courses")
                        .param("page", "0")
                        .param("status", "PUBLIC")
                        .param("keyword", "Spring"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/courses/list"))
                .andExpect(model().attributeExists("coursesPage"));

        verify(gatewayClient).get(argThat(uri -> uri.contains("/course/courses")
                && uri.contains("status=PUBLIC")
                && uri.contains("keyword=Spring")), eq(JsonNode.class));
    }

    @Test
    @DisplayName("9. Lock and unlock course call PATCH endpoints")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void lockAndUnlockCourseCallsPatchEndpoints() throws Exception {
        String lockJson = """
            {
                "code": 1000,
                "data": { "id": "c-101", "status": "LOCKED" }
            }
            """;
        when(gatewayClient.patch(eq("/course/courses/c-101/lock"), any(), eq(JsonNode.class)))
                .thenReturn(objectMapper.readTree(lockJson));

        mockMvc.perform(post("/admin/courses/c-101/lock")
                        .with(csrf())
                        .param("reason", "Vi phạm bản quyền nội dung"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/courses"));

        verify(gatewayClient).patch(eq("/course/courses/c-101/lock"),
                argThat(body -> body instanceof Map && "Vi phạm bản quyền nội dung".equals(((Map<?, ?>) body).get("reason"))),
                eq(JsonNode.class));

        when(gatewayClient.patch(eq("/course/courses/c-101/unlock"), isNull(), eq(JsonNode.class)))
                .thenReturn(objectMapper.readTree(lockJson));

        mockMvc.perform(post("/admin/courses/c-101/unlock").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/courses"));

        verify(gatewayClient).patch(eq("/course/courses/c-101/unlock"), isNull(), eq(JsonNode.class));
    }

    @Test
    @DisplayName("10. Category and Tag CRUD call correct endpoints")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void categoryAndTagCrudCallsCorrectEndpoints() throws Exception {
        String successJson = "{\"code\": 1000, \"data\": {}}";
        when(gatewayClient.post(eq("/course/categories"), any(), eq(JsonNode.class)))
                .thenReturn(objectMapper.readTree(successJson));
        when(gatewayClient.put(eq("/course/categories/1"), any(), eq(JsonNode.class)))
                .thenReturn(objectMapper.readTree(successJson));
        when(gatewayClient.delete(eq("/course/categories/1"), eq(JsonNode.class)))
                .thenReturn(objectMapper.readTree(successJson));

        when(gatewayClient.post(eq("/blog/tags"), any(), eq(JsonNode.class)))
                .thenReturn(objectMapper.readTree(successJson));
        when(gatewayClient.put(eq("/blog/tags/5"), any(), eq(JsonNode.class)))
                .thenReturn(objectMapper.readTree(successJson));
        when(gatewayClient.delete(eq("/blog/tags/5"), eq(JsonNode.class)))
                .thenReturn(objectMapper.readTree(successJson));

        // Create Category
        mockMvc.perform(post("/admin/categories")
                        .with(csrf())
                        .param("name", "Lập trình Web")
                        .param("slug", "lap-trinh-web"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"));
        verify(gatewayClient).post(eq("/course/categories"), any(), eq(JsonNode.class));

        // Update Category
        mockMvc.perform(post("/admin/categories/1")
                        .with(csrf())
                        .param("name", "Lập trình Web Cải Tiến")
                        .param("slug", "lap-trinh-web-cai-tien"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"));
        verify(gatewayClient).put(eq("/course/categories/1"), any(), eq(JsonNode.class));

        // Delete Category
        mockMvc.perform(post("/admin/categories/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"));
        verify(gatewayClient).delete(eq("/course/categories/1"), eq(JsonNode.class));

        // Create Tag
        mockMvc.perform(post("/admin/tags")
                        .with(csrf())
                        .param("name", "Java")
                        .param("slug", "java"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tags"));
        verify(gatewayClient).post(eq("/blog/tags"), any(), eq(JsonNode.class));

        // Update Tag
        mockMvc.perform(post("/admin/tags/5")
                        .with(csrf())
                        .param("name", "Java 21")
                        .param("slug", "java-21"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tags"));
        verify(gatewayClient).put(eq("/blog/tags/5"), any(), eq(JsonNode.class));

        // Delete Tag
        mockMvc.perform(post("/admin/tags/5/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tags"));
        verify(gatewayClient).delete(eq("/blog/tags/5"), eq(JsonNode.class));
    }
}
