package com.lms.webapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "instructor", roles = "INSTRUCTOR")
class CourseInstructorRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GatewayClient gatewayClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("Render Dashboard với danh sách khóa học và KPI thống kê")
    void dashboardRendersCorrectlyWithData() throws Exception {
        ObjectNode statsNode = objectMapper.createObjectNode();
        ObjectNode statsData = objectMapper.createObjectNode();
        statsData.put("totalCourses", 3);
        statsData.put("totalStudents", 55);
        statsData.put("totalExercises", 12);
        statsNode.set("data", statsData);
        when(gatewayClient.get(eq("/course/courses/my/stats"), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                .thenReturn(statsNode);

        ObjectNode coursesNode = objectMapper.createObjectNode();
        ObjectNode pageData = objectMapper.createObjectNode();
        ArrayNode contentArray = objectMapper.createArrayNode();
        ObjectNode c1 = objectMapper.createObjectNode();
        c1.put("id", "c-101");
        c1.put("title", "Khóa học Kiến trúc Microservices");
        c1.put("status", "PUBLIC");
        c1.put("level", "ADVANCED");
        c1.put("duration", 25);
        contentArray.add(c1);
        pageData.set("content", contentArray);
        pageData.put("totalElements", 1);
        pageData.put("totalPages", 1);
        pageData.put("number", 0);
        coursesNode.set("data", pageData);
        when(gatewayClient.get(org.mockito.ArgumentMatchers.argThat(s -> s != null && s.contains("/my?")), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                .thenReturn(coursesNode);

        String html = mockMvc.perform(get("/instructor/courses"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Studio Giảng Viên");
        assertThat(html).contains("Khóa học Kiến trúc Microservices");
        assertThat(html).contains("55");
        assertThat(html).contains("Công khai");
        assertThat(html).contains("/instructor/courses/c-101/manage");
    }

    @Test
    @DisplayName("Render Form tạo khóa học mới")
    void createCourseFormRendersCorrectly() throws Exception {
        ObjectNode catRes = objectMapper.createObjectNode();
        ArrayNode catArr = objectMapper.createArrayNode();
        ObjectNode cat = objectMapper.createObjectNode();
        cat.put("id", 10L);
        cat.put("name", "Khoa học Dữ liệu");
        catArr.add(cat);
        catRes.set("data", catArr);
        when(gatewayClient.get(eq("/course/categories"), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                .thenReturn(catRes);

        String html = mockMvc.perform(get("/instructor/courses/new"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(html).contains("Tạo Khóa Học Mới");
        assertThat(html).contains("Tiêu đề khóa học");
        assertThat(html).contains("Khoa học Dữ liệu");
        assertThat(html).contains("action=\"/instructor/courses/new\"");
    }

    @Test
    @DisplayName("Render Trung tâm Quản lý khóa học (Management Center) với 4 tabs")
    void managementCenterRendersCorrectly() throws Exception {
        ObjectNode cNode = objectMapper.createObjectNode();
        ObjectNode cData = objectMapper.createObjectNode();
        cData.put("id", "c-200");
        cData.put("title", "Lập Trình Web Toàn Diện");
        cData.put("status", "PUBLIC");
        cNode.set("data", cData);
        when(gatewayClient.get(eq("/course/courses/c-200"), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                .thenReturn(cNode);

        ObjectNode sNode = objectMapper.createObjectNode();
        ObjectNode sData = objectMapper.createObjectNode();
        sData.set("nodes", objectMapper.createArrayNode());
        sNode.set("data", sData);
        when(gatewayClient.get(eq("/course/courses/c-200/structure"), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                .thenReturn(sNode);

        String html = mockMvc.perform(get("/instructor/courses/c-200/manage"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        java.nio.file.Path fixtureDir = java.nio.file.Path.of("target", "ui-fixtures");
        java.nio.file.Files.createDirectories(fixtureDir);
        java.nio.file.Files.writeString(fixtureDir.resolve("management-center.html"), html);

        assertThat(html).contains("Lập Trình Web Toàn Diện");
        assertThat(html).contains("Cây Đề Cương Bài Giảng");
        assertThat(html).contains("Học Viên & Lời Mời");
        assertThat(html).contains("Lịch Học Trực Tuyến");
        assertThat(html).contains("Bảng Tin Thông Báo");
        assertThat(html).contains("id=\"curriculumErrorState\"");
        assertThat(html).contains("id=\"curriculumRetryBtn\"");
        assertThat(html).contains("id=\"studentsPagination\"");
        assertThat(html).contains("id=\"announcementsPagination\"");
        assertThat(html).contains("curriculum-builder.js");
        assertThat(html).contains("instructor-management.js");
    }
}
