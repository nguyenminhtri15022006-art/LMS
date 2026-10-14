package com.lms.courseservice.controller;

import com.lms.courseservice.configuration.SecurityConfig;
import com.lms.courseservice.dto.response.ChatbotCourseResponse;
import com.lms.courseservice.dto.response.InternalCourseResponse;
import com.lms.courseservice.dto.response.InternalLessonResponse;
import com.lms.courseservice.service.InternalCourseService;
import com.lms.courseservice.enums.CourseStatus;
import com.lms.courseservice.enums.LessonType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * HTTP Contract Integration Test cho 8 endpoint nội bộ (/internal/**) của course-service.
 *
 * <p>Mục tiêu: Đảm bảo HTTP status code, format body, JSON schema và đường dẫn URL
 * tuân thủ 100% hợp đồng OpenFeign Client với các service khác (Payment, Q&A, Chatbot, Review).
 */
@WebMvcTest(controllers = InternalCourseController.class)
@Import(SecurityConfig.class)
class InternalCourseApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InternalCourseService internalCourseService;

    private static final String COURSE_ID = "course-test-123";
    private static final String LESSON_ID = "lesson-test-456";
    private static final String USER_ID = "user-test-789";

    @Test
    @DisplayName("21. internalGetCourseHttpContractReturns200: GET /internal/courses/{courseId} -> HTTP 200, JSON InternalCourseResponse")
    void internalGetCourseHttpContractReturns200() throws Exception {
        InternalCourseResponse response = InternalCourseResponse.builder()
                .id(COURSE_ID)
                .title("Lập trình Spring Boot")
                .instructorId("inst-123")
                .status(CourseStatus.PUBLIC)
                .build();

        when(internalCourseService.getCourse(COURSE_ID)).thenReturn(response);

        mockMvc.perform(get("/internal/courses/{courseId}", COURSE_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(COURSE_ID))
                .andExpect(jsonPath("$.title").value("Lập trình Spring Boot"))
                .andExpect(jsonPath("$.instructorId").value("inst-123"))
                .andExpect(jsonPath("$.status").value("PUBLIC"));
    }

    @Test
    @DisplayName("22. internalGetLessonHttpContractReturns200: GET /internal/courses/{courseId}/lessons/{lessonId} -> HTTP 200, JSON InternalLessonResponse")
    void internalGetLessonHttpContractReturns200() throws Exception {
        InternalLessonResponse response = InternalLessonResponse.builder()
                .id(LESSON_ID)
                .lessonType(LessonType.VIDEO)
                .courseId(COURSE_ID)
                .build();

        when(internalCourseService.getLesson(COURSE_ID, LESSON_ID)).thenReturn(response);

        mockMvc.perform(get("/internal/courses/{courseId}/lessons/{lessonId}", COURSE_ID, LESSON_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(LESSON_ID))
                .andExpect(jsonPath("$.lessonType").value("video"))
                .andExpect(jsonPath("$.courseId").value(COURSE_ID));
    }

    @Test
    @DisplayName("23. internalCheckEnrollmentHttpContractReturnsBoolean: GET /internal/courses/{courseId}/enrollments/{userId} -> HTTP 200, Body true")
    void internalCheckEnrollmentHttpContractReturnsBoolean() throws Exception {
        when(internalCourseService.existsEnrollment(COURSE_ID, USER_ID)).thenReturn(true);

        mockMvc.perform(get("/internal/courses/{courseId}/enrollments/{userId}", COURSE_ID, USER_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("24. internalCountStudentsHttpContractReturnsLong: GET /internal/courses/{courseId}/students/count -> HTTP 200, Body số lượng học viên")
    void internalCountStudentsHttpContractReturnsLong() throws Exception {
        when(internalCourseService.countStudents(COURSE_ID)).thenReturn(42L);

        mockMvc.perform(get("/internal/courses/{courseId}/students/count", COURSE_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("42"));
    }

    @Test
    @DisplayName("25. internalCourseExistsHttpContractReturnsBoolean: GET /internal/courses/{courseId}/exists -> HTTP 200, Body true/false")
    void internalCourseExistsHttpContractReturnsBoolean() throws Exception {
        when(internalCourseService.existsCourse(COURSE_ID)).thenReturn(true);

        mockMvc.perform(get("/internal/courses/{courseId}/exists", COURSE_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("26. internalLessonExistsHttpContractReturnsBoolean: GET /internal/courses/{courseId}/lessons/{lessonId}/exists -> HTTP 200, Body true/false")
    void internalLessonExistsHttpContractReturnsBoolean() throws Exception {
        when(internalCourseService.existsLesson(COURSE_ID, LESSON_ID)).thenReturn(true);

        mockMvc.perform(get("/internal/courses/{courseId}/lessons/{lessonId}/exists", COURSE_ID, LESSON_ID))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    @DisplayName("27. internalGetAllCoursesHttpContractReturnsList: GET /internal/courses -> HTTP 200, ApiResponse<List<ChatbotCourseResponse>>")
    void internalGetAllCoursesHttpContractReturnsList() throws Exception {
        ChatbotCourseResponse course1 = ChatbotCourseResponse.builder()
                .id(COURSE_ID)
                .title("Spring Boot Chuyên Sâu")
                .description("Mô tả khóa học")
                .instructorId("inst-1")
                .status("PUBLIC")
                .categoryNames(List.of("Java"))
                .learningPoints(List.of("Microservices"))
                .build();

        when(internalCourseService.getAllPublishedCourses()).thenReturn(List.of(course1));

        mockMvc.perform(get("/internal/courses")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data[0].id").value(COURSE_ID))
                .andExpect(jsonPath("$.data[0].title").value("Spring Boot Chuyên Sâu"));
    }

    @Test
    @DisplayName("28. internalGetEnrolledCoursesHttpContractReturnsList: GET /internal/courses/enrolled?userId={id} -> HTTP 200, ApiResponse<List<ChatbotCourseResponse>>")
    void internalGetEnrolledCoursesHttpContractReturnsList() throws Exception {
        ChatbotCourseResponse course1 = ChatbotCourseResponse.builder()
                .id(COURSE_ID)
                .title("Khóa học đã đăng ký")
                .status("PUBLIC")
                .build();

        when(internalCourseService.getEnrolledCourses(USER_ID)).thenReturn(List.of(course1));

        mockMvc.perform(get("/internal/courses/enrolled")
                        .param("userId", USER_ID)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data[0].id").value(COURSE_ID))
                .andExpect(jsonPath("$.data[0].title").value("Khóa học đã đăng ký"));
    }
}
