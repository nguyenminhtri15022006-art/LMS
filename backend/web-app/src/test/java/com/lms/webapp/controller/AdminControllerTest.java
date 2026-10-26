package com.lms.webapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.webapp.client.GatewayClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientException;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
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

    // ==========================================
    // 1. KIỂM THỬ PHÂN QUYỀN ANONYMOUS, NON-ADMIN, ADMIN
    // ==========================================

    @Test
    @DisplayName("1.1 Anonymous request to /admin and sub-routes redirects to /login (HTTP 302)")
    void anonymousAccessToAdminRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/admin/users"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        mockMvc.perform(get("/admin/courses"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("1.2 Authenticated non-admin user is rejected with HTTP 403 Forbidden")
    @WithMockUser(username = "student_user", roles = {"STUDENT"})
    void authenticatedNonAdminAccessToAdminReturns403() throws Exception {
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

    // ==========================================
    // 2. DASHBOARD KPI: PHÂN BIỆT LỖI VỚI SỐ 0 THẬT
    // ==========================================

    @Test
    @DisplayName("2.1 Dashboard calls stats endpoint and loads KPI metrics when successful")
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
                .andExpect(model().attribute("usersStatsLoaded", true))
                .andExpect(model().attribute("coursesStatsLoaded", true))
                .andExpect(model().attributeExists("stats"))
                .andExpect(model().attribute("totalCourses", 25L));

        verify(gatewayClient).get(eq("/identity/admin/users/stats"), eq(JsonNode.class));
    }

    @Test
    @DisplayName("2.2 Dashboard marks failure flags when Gateway fails and does not disguise error as 0")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void dashboardGatewayFailureMarksStatsAsFailedAndDoesNotDefaultToZero() throws Exception {
        when(gatewayClient.get(eq("/identity/admin/users/stats"), eq(JsonNode.class)))
                .thenThrow(new RestClientException("Connection refused to identity-service"));
        when(gatewayClient.get(contains("/course/courses"), eq(JsonNode.class)))
                .thenThrow(new RestClientException("Connection refused to course-service"));

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attribute("usersStatsLoaded", false))
                .andExpect(model().attribute("coursesStatsLoaded", false))
                .andExpect(model().attributeDoesNotExist("stats"))
                .andExpect(model().attributeDoesNotExist("totalCourses"));
    }

    @Test
    @DisplayName("2.3 Dashboard preserves successful source when other source fails")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void dashboardPartialFailureRetainsSuccessfulSource() throws Exception {
        // Users stats fails
        when(gatewayClient.get(eq("/identity/admin/users/stats"), eq(JsonNode.class)))
                .thenThrow(new RestClientException("Timeout connecting to identity-service"));

        // Courses stats succeeds with real count = 0
        String coursesJson = "{\"code\": 1000, \"data\": {\"totalElements\": 0}}";
        when(gatewayClient.get(contains("/course/courses"), eq(JsonNode.class)))
                .thenReturn(objectMapper.readTree(coursesJson));

        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"))
                .andExpect(model().attribute("usersStatsLoaded", false))
                .andExpect(model().attribute("coursesStatsLoaded", true))
                .andExpect(model().attribute("totalCourses", 0L));
    }

    // ==========================================
    // 3. QUẢN LÝ NGƯỜI DÙNG & VALIDATION TẠO ADMIN
    // ==========================================

    @Test
    @DisplayName("3.1 Users list loads and passes filter and pagination params to Gateway")
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
        when(gatewayClient.get(contains("/identity/admin/users"), eq(JsonNode.class)))
                .thenReturn(objectMapper.readTree(usersPageJson));

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
    }

    @Test
    @DisplayName("3.2 Create admin rejects 4-character username and does NOT call Gateway")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void createAdminRejects4CharUsernameWithoutCallingGateway() throws Exception {
        mockMvc.perform(post("/admin/users/admins")
                        .with(csrf())
                        .param("username", "abcd") // 4 chars -> should fail
                        .param("password", "AdminPassword123")
                        .param("email", "admin@example.com")
                        .param("fullname", "Nguyen Van Admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(gatewayClient, never()).post(eq("/identity/admin/users/admins"), any(), any());
    }

    @Test
    @DisplayName("3.3 Create admin rejects username with invalid characters and does NOT call Gateway")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void createAdminRejectsInvalidUsernameCharactersWithoutCallingGateway() throws Exception {
        mockMvc.perform(post("/admin/users/admins")
                        .with(csrf())
                        .param("username", "admin@root") // '@' not allowed in ^[a-zA-Z0-9_]+$
                        .param("password", "AdminPassword123")
                        .param("email", "admin@example.com")
                        .param("fullname", "Nguyen Van Admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(gatewayClient, never()).post(eq("/identity/admin/users/admins"), any(), any());
    }

    @Test
    @DisplayName("3.4 Create admin rejects password containing whitespace and does NOT call Gateway")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void createAdminRejectsPasswordWithWhitespaceWithoutCallingGateway() throws Exception {
        mockMvc.perform(post("/admin/users/admins")
                        .with(csrf())
                        .param("username", "admin_valid")
                        .param("password", "pass word 123") // Contains spaces -> violates ^\S{8,}$
                        .param("email", "admin@example.com")
                        .param("fullname", "Nguyen Van Admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(gatewayClient, never()).post(eq("/identity/admin/users/admins"), any(), any());
    }

    @Test
    @DisplayName("3.5 Create admin rejects invalid fullname and does NOT call Gateway")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void createAdminRejectsInvalidFullnameWithoutCallingGateway() throws Exception {
        // Less than 3 chars
        mockMvc.perform(post("/admin/users/admins")
                        .with(csrf())
                        .param("username", "admin_valid")
                        .param("password", "AdminPass123")
                        .param("email", "admin@example.com")
                        .param("fullname", "AB")) // Too short
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("errorMessage"));

        // Contains numbers or special chars
        mockMvc.perform(post("/admin/users/admins")
                        .with(csrf())
                        .param("username", "admin_valid")
                        .param("password", "AdminPass123")
                        .param("email", "admin@example.com")
                        .param("fullname", "Admin 12345")) // Numbers not allowed in ^[A-Za-zÀ-ỹ\s]+$
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("errorMessage"));

        verify(gatewayClient, never()).post(eq("/identity/admin/users/admins"), any(), any());
    }

    @Test
    @DisplayName("3.6 Create admin accepts valid 5-char username and valid fields, then forwards to Gateway")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void createAdminAccepts5CharValidUsernameAndCallsGateway() throws Exception {
        String responseJson = "{\"code\": 1000, \"data\": {\"userId\": \"u-new-admin\"}}";
        when(gatewayClient.post(eq("/identity/admin/users/admins"), any(), eq(JsonNode.class)))
                .thenReturn(objectMapper.readTree(responseJson));

        mockMvc.perform(post("/admin/users/admins")
                        .with(csrf())
                        .param("username", "adm01") // Exactly 5 valid characters
                        .param("password", "SecretPass123")
                        .param("email", "newadmin@example.com")
                        .param("fullname", "Lê Văn Quản Trị"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"))
                .andExpect(flash().attributeExists("successMessage"));

        verify(gatewayClient).post(eq("/identity/admin/users/admins"),
                argThat(body -> body instanceof Map && "adm01".equals(((Map<?, ?>) body).get("username"))
                        && "SecretPass123".equals(((Map<?, ?>) body).get("password"))),
                eq(JsonNode.class));
    }

    @Test
    @DisplayName("3.7 Toggle user active calls PATCH endpoint")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void toggleUserActiveCallsPatchEndpoint() throws Exception {
        String responseJson = "{\"code\": 1000, \"data\": {\"userId\": \"u-123\", \"active\": false}}";
        when(gatewayClient.patch(eq("/identity/admin/users/u-123/active"), isNull(), eq(JsonNode.class)))
                .thenReturn(objectMapper.readTree(responseJson));

        mockMvc.perform(post("/admin/users/u-123/toggle-active").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(gatewayClient).patch(eq("/identity/admin/users/u-123/active"), isNull(), eq(JsonNode.class));
    }

    @Test
    @DisplayName("3.8 Admin reset password calls PATCH endpoint with AdminResetPasswordRequest")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void adminResetPasswordCallsPatchEndpoint() throws Exception {
        String responseJson = "{\"code\": 1000, \"data\": {\"userId\": \"u-123\"}}";
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
    @DisplayName("3.9 Toggle admin active calls PATCH endpoint")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void toggleAdminActiveCallsPatchEndpoint() throws Exception {
        String responseJson = "{\"code\": 1000, \"data\": {\"userId\": \"u-admin-2\", \"active\": true}}";
        when(gatewayClient.patch(eq("/identity/admin/users/u-admin-2/admin-active"), isNull(), eq(JsonNode.class)))
                .thenReturn(objectMapper.readTree(responseJson));

        mockMvc.perform(post("/admin/users/u-admin-2/toggle-admin-active").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/users"));

        verify(gatewayClient).patch(eq("/identity/admin/users/u-admin-2/admin-active"), isNull(), eq(JsonNode.class));
    }

    // ==========================================
    // 4. QUẢN LÝ KHÓA HỌC & CRUD CATEGORIES/TAGS
    // ==========================================

    @Test
    @DisplayName("4.1 Courses list loads and filters admin courses via Gateway")
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
    }

    @Test
    @DisplayName("4.2 Lock and unlock course call PATCH endpoints")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void lockAndUnlockCourseCallsPatchEndpoints() throws Exception {
        String lockJson = "{\"code\": 1000, \"data\": { \"id\": \"c-101\", \"status\": \"LOCKED\" }}";
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
    @DisplayName("4.3 Category and Tag CRUD call correct endpoints")
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

        // Update Category
        mockMvc.perform(post("/admin/categories/1")
                        .with(csrf())
                        .param("name", "Lập trình Web Cải Tiến")
                        .param("slug", "lap-trinh-web-cai-tien"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"));

        // Delete Category
        mockMvc.perform(post("/admin/categories/1/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/categories"));

        // Create Tag
        mockMvc.perform(post("/admin/tags")
                        .with(csrf())
                        .param("name", "Java")
                        .param("slug", "java"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tags"));

        // Update Tag
        mockMvc.perform(post("/admin/tags/5")
                        .with(csrf())
                        .param("name", "Java 21")
                        .param("slug", "java-21"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tags"));

        // Delete Tag
        mockMvc.perform(post("/admin/tags/5/delete").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tags"));
    }
}
