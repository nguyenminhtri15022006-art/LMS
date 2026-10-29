package com.lms.webapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lms.webapp.client.GatewayClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CourseInstructorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GatewayClient gatewayClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        ObjectNode statsNode = objectMapper.createObjectNode();
        ObjectNode statsData = objectMapper.createObjectNode();
        statsData.put("totalCourses", 5);
        statsData.put("totalStudents", 120);
        statsData.put("totalExercises", 45);
        statsNode.set("data", statsData);

        when(gatewayClient.get(eq("/course/courses/my/stats"), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                .thenReturn(statsNode);

        ObjectNode coursesNode = objectMapper.createObjectNode();
        ObjectNode pageData = objectMapper.createObjectNode();
        ArrayNode contentArray = objectMapper.createArrayNode();
        ObjectNode c1 = objectMapper.createObjectNode();
        c1.put("id", "c-101");
        c1.put("title", "Khóa học Java Spring Boot");
        c1.put("status", "PUBLIC");
        contentArray.add(c1);
        pageData.set("content", contentArray);
        pageData.put("totalElements", 1);
        pageData.put("totalPages", 1);
        pageData.put("number", 0);
        coursesNode.set("data", pageData);

        when(gatewayClient.get(startsWith("/course/courses/my"), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                .thenReturn(coursesNode);

        ObjectNode courseDetailNode = objectMapper.createObjectNode();
        courseDetailNode.set("data", c1);
        when(gatewayClient.get(eq("/course/courses/c-101"), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                .thenReturn(courseDetailNode);

        ObjectNode categoriesNode = objectMapper.createObjectNode();
        ArrayNode catArray = objectMapper.createArrayNode();
        ObjectNode cat1 = objectMapper.createObjectNode();
        cat1.put("id", 1L);
        cat1.put("name", "Lập trình");
        catArray.add(cat1);
        categoriesNode.set("data", catArray);
        when(gatewayClient.get(eq("/course/categories"), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                .thenReturn(categoriesNode);
    }

    @Test
    @DisplayName("Test 1: Học viên thường bị chặn 403 Forbidden, Giảng viên truy cập thành công")
    void instructorEndpointsRequireInstructorRole() throws Exception {
        // 1. Chưa đăng nhập -> redirect to login
        mockMvc.perform(get("/instructor/courses"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // 2. Học viên thường ROLE_USER / ROLE_STUDENT -> 403 Forbidden
        mockMvc.perform(get("/instructor/courses").with(user("student1").roles("STUDENT")))
                .andExpect(status().isForbidden());

        // 3. Giảng viên ROLE_INSTRUCTOR -> 200 OK
        mockMvc.perform(get("/instructor/courses").with(user("teacher1").roles("INSTRUCTOR")))
                .andExpect(status().isOk());

        // 4. Giảng viên ROLE_TEACHER (alias) -> 200 OK
        mockMvc.perform(get("/instructor/courses").with(user("teacher2").roles("TEACHER")))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "instructor", roles = {"INSTRUCTOR"})
    @DisplayName("Test 2: Dashboard hiển thị KPI và danh sách khóa học giảng viên sở hữu")
    void dashboardDisplaysInstructorStats() throws Exception {
        mockMvc.perform(get("/instructor/courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/instructor/dashboard"))
                .andExpect(model().attributeExists("stats"))
                .andExpect(model().attributeExists("coursesPage"));

        verify(gatewayClient, atLeastOnce()).get(eq("/course/courses/my/stats"), any());
        verify(gatewayClient, atLeastOnce()).get(startsWith("/course/courses/my"), any());
    }

    @Test
    @WithMockUser(username = "instructor", roles = {"INSTRUCTOR"})
    @DisplayName("Test 3: Tạo khóa học mới dạng Multipart đóng gói và gọi Gateway")
    void createCourseMultipartCallsGateway() throws Exception {
        MockMultipartFile thumbnail = new MockMultipartFile(
                "thumbnail", "thumb.jpg", "image/jpeg", "image-content".getBytes());

        ObjectNode responseNode = objectMapper.createObjectNode();
        ObjectNode createdData = objectMapper.createObjectNode();
        createdData.put("id", "c-102");
        responseNode.set("data", createdData);

        when(gatewayClient.postMultipart(eq("/course/courses"), any(), any()))
                .thenReturn(responseNode);

        mockMvc.perform(multipart("/instructor/courses/new")
                        .file(thumbnail)
                        .param("title", "Khóa học Microservices mới")
                        .param("description", "Mô tả khóa học chi tiết")
                        .param("status", "PUBLIC")
                        .param("duration", "40")
                        .param("level", "INTERMEDIATE")
                        .param("categoryIds", "1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/instructor/courses"));

        verify(gatewayClient, times(1)).postMultipart(eq("/course/courses"), any(), any());
    }

    @Test
    @WithMockUser(username = "instructor", roles = {"INSTRUCTOR"})
    @DisplayName("Test 4: Thêm node chương/bài học gọi đúng POST /structure/nodes")
    void curriculumAddNodeCallsStructureApi() throws Exception {
        ObjectNode resNode = objectMapper.createObjectNode();
        resNode.put("code", 1000);
        when(gatewayClient.post(eq("/course/courses/c-101/structure/nodes"), any(), any()))
                .thenReturn(resNode);

        String jsonBody = """
                {
                    "type": "folder",
                    "title": "Chương 1: Mở đầu",
                    "order": 1
                }
                """;

        mockMvc.perform(post("/instructor/courses/c-101/structure/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody)
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(gatewayClient, times(1)).post(eq("/course/courses/c-101/structure/nodes"), any(), any());
    }

    @Test
    @WithMockUser(username = "instructor", roles = {"INSTRUCTOR"})
    @DisplayName("Test 5: Sắp xếp lại thứ tự bài giảng gọi PUT /structure/reorder")
    void curriculumReorderCallsPutReorder() throws Exception {
        ObjectNode resNode = objectMapper.createObjectNode();
        resNode.put("code", 1000);
        when(gatewayClient.put(eq("/course/courses/c-101/structure/reorder"), any(), any()))
                .thenReturn(resNode);

        String jsonBody = """
                {
                    "nodes": [
                        {"nodeId": "n-1", "parentId": null, "order": 1},
                        {"nodeId": "n-2", "parentId": null, "order": 2}
                    ]
                }
                """;

        mockMvc.perform(put("/instructor/courses/c-101/structure/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody)
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(gatewayClient, times(1)).put(eq("/course/courses/c-101/structure/reorder"), any(), any());
    }

    @Test
    @WithMockUser(username = "instructor", roles = {"INSTRUCTOR"})
    @DisplayName("Test 6: Quản lý lịch học hỗ trợ PUT và DELETE")
    void scheduleManagementSupportsPutAndDelete() throws Exception {
        ObjectNode resNode = objectMapper.createObjectNode();
        resNode.put("code", 1000);
        when(gatewayClient.put(eq("/course/courses/c-101/schedules/1"), any(), any()))
                .thenReturn(resNode);
        when(gatewayClient.delete(eq("/course/courses/c-101/schedules/1"), any()))
                .thenReturn(resNode);

        String jsonBody = """
                {
                    "title": "Buổi học Q&A",
                    "startTime": "2026-11-01T10:00:00Z",
                    "endTime": "2026-11-01T11:30:00Z",
                    "note": "Trao đổi bài tập"
                }
                """;

        // PUT update schedule
        mockMvc.perform(put("/instructor/courses/c-101/schedules/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonBody)
                        .with(csrf()))
                .andExpect(status().isOk());

        // DELETE schedule
        mockMvc.perform(delete("/instructor/courses/c-101/schedules/1")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(gatewayClient, times(1)).put(eq("/course/courses/c-101/schedules/1"), any(), any());
        verify(gatewayClient, times(1)).delete(eq("/course/courses/c-101/schedules/1"), any());
    }

    @Test
    @WithMockUser(username = "instructor", roles = {"INSTRUCTOR"})
    @DisplayName("Test 7: Xóa thông báo gọi đúng DELETE /announcements/{id}")
    void deleteAnnouncementCallsDeleteEndpoint() throws Exception {
        ObjectNode resNode = objectMapper.createObjectNode();
        resNode.put("code", 1000);
        when(gatewayClient.delete(eq("/course/courses/c-101/announcements/10"), any()))
                .thenReturn(resNode);

        mockMvc.perform(delete("/instructor/courses/c-101/announcements/10")
                        .with(csrf()))
                .andExpect(status().isOk());

        verify(gatewayClient, times(1)).delete(eq("/course/courses/c-101/announcements/10"), any());
    }

    @Test
    @WithMockUser(username = "instructor", roles = {"INSTRUCTOR"})
    @DisplayName("Regression Test 8: Sửa khóa học khi title trống (validation fail) phải giữ model course để không vỡ template")
    void editCourseValidationFailPreservesCourseModel() throws Exception {
        mockMvc.perform(multipart("/instructor/courses/c-101/edit")
                        .param("title", "")
                        .param("description", "Mô tả khóa học")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/instructor/course-form"))
                .andExpect(model().attributeExists("course"))
                .andExpect(model().attribute("isEdit", true))
                .andExpect(model().attributeExists("categories"));

        verify(gatewayClient, never()).putMultipart(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "instructor", roles = {"INSTRUCTOR"})
    @DisplayName("Regression Test 9: Cập nhật khóa học multipart phải chuyển tiếp learningPoints, requirements và không gửi thumbnail nếu không chọn file")
    void editCourseMultipartForwardsLearningPointsAndRequirements() throws Exception {
        ObjectNode responseNode = objectMapper.createObjectNode();
        responseNode.put("code", 1000);
        when(gatewayClient.putMultipart(eq("/course/courses/c-101"), any(), any()))
                .thenReturn(responseNode);

        mockMvc.perform(multipart("/instructor/courses/c-101/edit")
                        .param("title", "Khóa học cập nhật")
                        .param("learningPoints", "Nắm vững Microservices", "Hiểu rõ Spring Cloud")
                        .param("requirements", "Biết Java cơ bản")
                        .param("status", "PUBLIC")
                        .param("duration", "30")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/instructor/courses"));

        verify(gatewayClient, times(1)).putMultipart(eq("/course/courses/c-101"), argThat(body -> {
            boolean hasLp = body.containsKey("learningPoints") && body.get("learningPoints").size() == 2;
            boolean hasReq = body.containsKey("requirements") && body.get("requirements").size() == 1;
            boolean noThumb = !body.containsKey("thumbnail");
            return hasLp && hasReq && noThumb;
        }), any());
    }

    @Test
    @WithMockUser(username = "instructor", roles = {"INSTRUCTOR"})
    @DisplayName("Regression Test 10: Endpoint JSON GET /instructor/courses/{id}/structure gọi Gateway trả ApiResponse")
    void structureJsonEndpointReturnsJsonFromGateway() throws Exception {
        ObjectNode structureNode = objectMapper.createObjectNode();
        ObjectNode dataNode = objectMapper.createObjectNode();
        ArrayNode nodes = objectMapper.createArrayNode();
        ObjectNode n1 = objectMapper.createObjectNode();
        n1.put("id", "n-1");
        n1.put("type", "folder");
        n1.put("title", "Chương 1");
        nodes.add(n1);
        dataNode.set("nodes", nodes);
        structureNode.set("data", dataNode);

        when(gatewayClient.get(eq("/course/courses/c-101/structure"), eq(com.fasterxml.jackson.databind.JsonNode.class)))
                .thenReturn(structureNode);

        mockMvc.perform(get("/instructor/courses/c-101/structure"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.data.nodes[0].title").value("Chương 1"));

        verify(gatewayClient, times(1)).get(eq("/course/courses/c-101/structure"), eq(com.fasterxml.jackson.databind.JsonNode.class));
    }

    @Test
    @WithMockUser(username = "instructor", roles = {"INSTRUCTOR"})
    @DisplayName("Regression Test 11: Validation form DTO chặn dữ liệu rỗng trước khi gọi Gateway và trả về 400 Bad Request")
    void addStructureNodeValidationFailureBlocksGatewayAndReturns400() throws Exception {
        String invalidBody = """
                {
                    "type": "",
                    "title": ""
                }
                """;

        mockMvc.perform(post("/instructor/courses/c-101/structure/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody)
                        .with(csrf()))
                .andExpect(status().isBadRequest());

        verify(gatewayClient, never()).post(eq("/course/courses/c-101/structure/nodes"), any(), any());
    }

    @Test
    @WithMockUser(username = "instructor", roles = {"INSTRUCTOR"})
    @DisplayName("Regression Test 12: Lỗi 403 Forbidden từ downstream Gateway phải được giữ nguyên HTTP status code thay vì biến thành 500")
    void addStructureNodeDownstream403PreservesHttpStatus() throws Exception {
        when(gatewayClient.post(eq("/course/courses/c-101/structure/nodes"), any(), any()))
                .thenThrow(org.springframework.web.client.HttpClientErrorException.create(
                        org.springframework.http.HttpStatus.FORBIDDEN,
                        "Forbidden",
                        org.springframework.http.HttpHeaders.EMPTY,
                        "{\"code\":403,\"message\":\"Không có quyền thực hiện\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8),
                        java.nio.charset.StandardCharsets.UTF_8));

        String validBody = """
                {
                    "type": "folder",
                    "title": "Chương 2: Thiết kế"
                }
                """;

        mockMvc.perform(post("/instructor/courses/c-101/structure/nodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody)
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @WithMockUser(username = "instructor", roles = {"INSTRUCTOR"})
    @DisplayName("Validation Test 13: Reorder nodes danh sách rỗng bị chặn với 400")
    void reorderNodesValidationFailsOnEmptyNodes() throws Exception {
        String invalidBody = """
                {
                    "nodes": []
                }
                """;

        mockMvc.perform(put("/instructor/courses/c-101/structure/reorder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @WithMockUser(username = "instructor", roles = {"INSTRUCTOR"})
    @DisplayName("Validation Test 14: Sửa tiêu đề node để trống bị chặn với 400")
    void updateNodeTitleValidationFailsOnBlankTitle() throws Exception {
        String invalidBody = """
                {
                    "title": "   "
                }
                """;

        mockMvc.perform(patch("/instructor/courses/c-101/structure/nodes/node-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody)
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}

