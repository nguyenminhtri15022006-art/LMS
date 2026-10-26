package com.lms.webapp.controller;

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
import org.springframework.test.web.servlet.MvcResult;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GatewayClient gatewayClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Render Admin Dashboard displays KPI stats cards and infrastructure health")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void renderAdminDashboardDisplaysStatsAndHealth() throws Exception {
        String statsJson = """
            {
                "code": 1000,
                "data": {
                    "totalStudents": 240,
                    "newStudentsThisWeek": 15,
                    "totalInstructors": 22,
                    "newInstructorsThisMonth": 4,
                    "totalAdmins": 3
                }
            }
            """;
        when(gatewayClient.get(eq("/identity/admin/users/stats"), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                .thenReturn(objectMapper.readTree(statsJson));

        String coursesJson = """
            {
                "code": 1000,
                "data": { "totalElements": 48 }
            }
            """;
        when(gatewayClient.get(contains("/course/courses"), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                .thenReturn(objectMapper.readTree(coursesJson));

        MvcResult result = mockMvc.perform(get("/admin"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html)
                .contains("Trung Tâm Quản Trị")
                .contains("240")
                .contains("22")
                .contains("48")
                .contains("API Gateway (Port 8080)")
                .contains("Identity Service (8081)");
    }

    @Test
    @DisplayName("Render Admin Users List renders users table, badges and escapes dangerous characters")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void renderAdminUsersListRendersTableAndEscapesXSS() throws Exception {
        String usersPageJson = """
            {
                "code": 1000,
                "data": {
                    "content": [
                        {
                            "userId": "u-admin-1",
                            "username": "super_admin",
                            "fullname": "<script>alert('xss')</script>Tran Van Admin",
                            "email": "admin@edulearn.vn",
                            "role": "ADMIN",
                            "active": true
                        },
                        {
                            "userId": "u-stu-2",
                            "username": "student_locked",
                            "fullname": "Le Thi Hoc Vien",
                            "email": "student@edulearn.vn",
                            "role": "STUDENT",
                            "active": false
                        }
                    ],
                    "totalElements": 2,
                    "totalPages": 1,
                    "number": 0,
                    "size": 10
                }
            }
            """;
        when(gatewayClient.get(contains("/identity/admin/users"), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                .thenReturn(objectMapper.readTree(usersPageJson));

        MvcResult result = mockMvc.perform(get("/admin/users"))
                .andExpect(status().isOk())
                .andReturn();

        String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html)
                .contains("Danh Sách Người Dùng")
                .contains("admin@edulearn.vn")
                .contains("ADMIN")
                .contains("HỌC VIÊN")
                .contains("Hoạt động")
                .contains("Đã khóa")
                .doesNotContain("<script>alert('xss')</script>")
                .contains("&lt;script&gt;alert(&#39;xss&#39;)&lt;/script&gt;Tran Van Admin");
    }

    @Test
    @DisplayName("Render Admin Categories and Tags lists display management tables")
    @WithMockUser(username = "admin_user", roles = {"ADMIN"})
    void renderAdminCategoriesAndTagsTables() throws Exception {
        String categoriesJson = """
            {
                "code": 1000,
                "data": [
                    { "id": 1, "name": "Lập Trình Web", "slug": "lap-trinh-web" },
                    { "id": 2, "name": "Trí Tuệ Nhân Tạo", "slug": "tri-tue-nhan-tao" }
                ]
            }
            """;
        when(gatewayClient.get(eq("/course/categories"), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                .thenReturn(objectMapper.readTree(categoriesJson));

        MvcResult catResult = mockMvc.perform(get("/admin/categories"))
                .andExpect(status().isOk())
                .andReturn();

        String catHtml = catResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(catHtml)
                .contains("Thể Loại Khóa Học")
                .contains("Lập Trình Web")
                .contains("lap-trinh-web")
                .contains("tri-tue-nhan-tao");

        String tagsJson = """
            {
                "code": 1000,
                "data": [
                    { "id": 10, "name": "Spring Boot", "slug": "spring-boot" },
                    { "id": 11, "name": "ReactJS", "slug": "reactjs" }
                ]
            }
            """;
        when(gatewayClient.get(eq("/blog/tags"), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                .thenReturn(objectMapper.readTree(tagsJson));

        MvcResult tagResult = mockMvc.perform(get("/admin/tags"))
                .andExpect(status().isOk())
                .andReturn();

        String tagHtml = tagResult.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(tagHtml)
                .contains("Thẻ Tags Diễn Đàn")
                .contains("Spring Boot")
                .contains("spring-boot")
                .contains("ReactJS");
    }
}
