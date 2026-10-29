package com.lms.courseservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.courseservice.dto.request.CreateCategoryRequest;
import com.lms.courseservice.dto.request.UpdateLessonRequest;
import com.lms.courseservice.entity.mongo.CourseStructure;
import com.lms.courseservice.entity.mongo.StructureNode;
import com.lms.courseservice.entity.mysql.Category;
import com.lms.courseservice.entity.mysql.Course;
import com.lms.courseservice.enums.CourseLevel;
import com.lms.courseservice.enums.CourseStatus;
import com.lms.courseservice.enums.LessonType;
import com.lms.courseservice.enums.NodeType;
import com.lms.courseservice.repository.mongo.CourseStructureRepository;
import com.lms.courseservice.repository.mongo.LessonRepository;
import com.lms.courseservice.repository.mysql.CategoryRepository;
import com.lms.courseservice.repository.mysql.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CourseApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @MockBean
    private CourseStructureRepository courseStructureRepository;

    @MockBean
    private LessonRepository lessonRepository;

    private Course savedCourse;

    @BeforeEach
    void setUp() {
        courseRepository.deleteAll();
        categoryRepository.deleteAll();

        savedCourse = courseRepository.save(Course.builder()
                .title("Java Spring Pro")
                .instructorId("owner-instructor-123")
                .status(CourseStatus.PUBLIC)
                .level(CourseLevel.BEGINNER)
                .mongoStructureId("structure-mongo-999")
                .build());
    }

    @Test
    @DisplayName("21. categoryControllerEnforcesAdminRole: Phân quyền RBAC từ chối gọi tạo danh mục nếu không có role ADMIN")
    void categoryControllerEnforcesAdminRole() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Khoa học máy tính")
                .slug("khoa-hoc-may-tinh")
                .build();

        mockMvc.perform(post("/categories")
                        .header("X-User-Id", "student-user-456")
                        .header("X-User-Role", "ROLE_STUDENT")
                        .header("X-User-Email", "student@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(2002));
    }

    @Test
    @DisplayName("22. courseControllerValidationFailsOnBlankTitle: Bắt lỗi validation khi tạo khóa học thiếu tiêu đề bắt buộc")
    void courseControllerValidationFailsOnBlankTitle() throws Exception {
        mockMvc.perform(post("/courses")
                        .header("X-User-Id", "owner-instructor-123")
                        .header("X-User-Role", "ROLE_INSTRUCTOR")
                        .header("X-User-Email", "instructor@example.com")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("title", "")
                        .param("status", "PRIVATE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(2499))
                .andExpect(jsonPath("$.data.title").exists());
    }

    @Test
    @DisplayName("23. courseStructureEndpointReturnsTreeJson: Controller trả về cấu trúc cây JSON chuẩn format data")
    void courseStructureEndpointReturnsTreeJson() throws Exception {
        CourseStructure structure = CourseStructure.builder()
                .id(savedCourse.getMongoStructureId())
                .courseId(savedCourse.getId())
                .nodes(new ArrayList<>(List.of(
                        StructureNode.builder()
                                .id("node-1")
                                .type(NodeType.FOLDER)
                                .title("Chương 1")
                                .order(0)
                                .build()
                )))
                .updatedAt(LocalDateTime.now())
                .build();

        when(courseStructureRepository.findByCourseId(savedCourse.getId())).thenReturn(Optional.of(structure));

        mockMvc.perform(get("/courses/" + savedCourse.getId() + "/structure"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.courseId").value(savedCourse.getId()))
                .andExpect(jsonPath("$.data.nodes[0].title").value("Chương 1"));
    }

    @Test
    @DisplayName("24. lessonControllerEnforcesOwnerAuthorizationOnUpdate: Controller chặn request sửa bài học từ người không có quyền (HTTP 403)")
    void lessonControllerEnforcesOwnerAuthorizationOnUpdate() throws Exception {
        UpdateLessonRequest request = UpdateLessonRequest.builder()
                .lessonType(LessonType.VIDEO)
                .content(Map.of("video_url", "https://video.mp4"))
                .build();

        mockMvc.perform(put("/courses/" + savedCourse.getId() + "/lessons/lesson-789")
                        .header("X-User-Id", "stranger-instructor-999")
                        .header("X-User-Role", "ROLE_INSTRUCTOR")
                        .header("X-User-Email", "stranger@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(2003))
                .andExpect(jsonPath("$.message").value("Bạn không phải giảng viên của khóa học này"));
    }

    @Test
    @DisplayName("25. gatewayContractSupportsXUserRolesHeader: Controller nhận diện vai trò hợp lệ qua header X-User-Roles")
    void gatewayContractSupportsXUserRolesHeader() throws Exception {
        CourseStructure structure = CourseStructure.builder()
                .id("structure-999")
                .courseId(savedCourse.getId())
                .nodes(new java.util.ArrayList<>())
                .updatedAt(LocalDateTime.now())
                .build();
        when(courseStructureRepository.findByCourseId(savedCourse.getId())).thenReturn(Optional.of(structure));

        mockMvc.perform(get("/courses/" + savedCourse.getId() + "/structure")
                        .header("X-User-Id", "student-user-456")
                        .header("X-User-Roles", "ROLE_STUDENT")
                        .header("X-User-Email", "student@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));
    }
}
