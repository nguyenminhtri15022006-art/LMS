package com.lms.courseservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.courseservice.dto.request.CompleteLessonRequest;
import com.lms.courseservice.dto.request.CreateAnnouncementRequest;
import com.lms.courseservice.dto.request.CreateScheduleRequest;
import com.lms.courseservice.entity.mysql.Course;
import com.lms.courseservice.enums.CourseLevel;
import com.lms.courseservice.enums.CourseStatus;
import com.lms.courseservice.enums.LessonType;
import com.lms.courseservice.repository.mongo.CourseStructureRepository;
import com.lms.courseservice.repository.mongo.LessonRepository;
import com.lms.courseservice.repository.mysql.*;
import com.lms.courseservice.service.CourseStructureService;
import com.lms.courseservice.service.GoogleMeetService;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Functional Test cho các luồng nghiệp vụ tổng hợp của course-service (Phase 8).
 *
 * <p>Mục tiêu: Kiểm thử xuyên suốt các chức năng ghi danh, tiến độ học tập, thông báo và lịch học
 * thông qua MockMvc và H2 in-memory Database thật.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CourseFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private LessonProgressRepository lessonProgressRepository;

    @Autowired
    private AnnouncementRepository announcementRepository;

    @Autowired
    private ScheduleRepository scheduleRepository;

    @MockBean
    private CourseStructureService courseStructureService;

    @MockBean
    private CourseStructureRepository courseStructureRepository;

    @MockBean
    private LessonRepository lessonRepository;

    @MockBean
    private GoogleMeetService googleMeetService;

    @MockBean(name = "minioClient")
    private MinioClient minioClient;

    @MockBean(name = "publicMinioClient")
    private MinioClient publicMinioClient;

    private static final String INSTRUCTOR_ID = "instructor-workflow-1";
    private static final String STUDENT_ID = "student-workflow-1";
    private static final String LESSON_1_ID = "lesson-workflow-1";
    private static final String LESSON_2_ID = "lesson-workflow-2";

    private String courseId;

    @BeforeEach
    void setUp() {
        cleanData();

        Course course = Course.builder()
                .title("Khóa học Microservices Thực chiến")
                .description("Học Spring Boot và Cloud")
                .instructorId(INSTRUCTOR_ID)
                .status(CourseStatus.PUBLIC)
                .level(CourseLevel.BEGINNER)
                .mongoStructureId(UUID.randomUUID().toString())
                .learningPoints(new ArrayList<>(List.of("Microservices", "Docker")))
                .build();

        Course savedCourse = courseRepository.save(course);
        courseId = savedCourse.getId();
    }

    @AfterEach
    void tearDown() {
        cleanData();
        SecurityContextHolder.clearContext();
    }

    private void cleanData() {
        lessonProgressRepository.deleteAll();
        scheduleRepository.deleteAll();
        announcementRepository.deleteAll();
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
    }

    @Test
    @DisplayName("30. studentEnrollsInCourseAndTracksProgressWorkflow: Luồng học viên ghi danh -> theo dõi tiến độ ban đầu 0% -> hoàn thành bài học 1 -> tiến độ tăng lên 50%")
    void studentEnrollsInCourseAndTracksProgressWorkflow() throws Exception {
        when(courseStructureService.countLessons(courseId)).thenReturn(2);
        when(courseStructureService.isLessonInCourse(courseId, LESSON_1_ID)).thenReturn(true);
        when(courseStructureService.isLessonInCourse(courseId, LESSON_2_ID)).thenReturn(true);

        // 1. Học viên ghi danh khóa học
        mockMvc.perform(post("/courses/{id}/enroll", courseId)
                        .header("X-User-Id", STUDENT_ID)
                        .header("X-User-Role", "STUDENT")
                        .header("X-User-Email", "student@test.com")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"));

        // 2. Học viên xem tiến độ học tập ban đầu (chưa hoàn thành bài nào -> 0.0%)
        mockMvc.perform(get("/courses/{courseId}/progress", courseId)
                        .header("X-User-Id", STUDENT_ID)
                        .header("X-User-Role", "STUDENT")
                        .header("X-User-Email", "student@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.courseId").value(courseId))
                .andExpect(jsonPath("$.data.totalLessons").value(2))
                .andExpect(jsonPath("$.data.completedLessons").value(0))
                .andExpect(jsonPath("$.data.percentComplete").value(0.0));

        // 3. Học viên hoàn thành bài học số 1
        CompleteLessonRequest completeRequest = CompleteLessonRequest.builder()
                .lessonType(LessonType.VIDEO)
                .build();

        mockMvc.perform(post("/courses/{courseId}/lessons/{lessonId}/complete", courseId, LESSON_1_ID)
                        .header("X-User-Id", STUDENT_ID)
                        .header("X-User-Role", "STUDENT")
                        .header("X-User-Email", "student@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(completeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));

        // 4. Học viên xem lại tiến độ (đã hoàn thành 1/2 bài -> 50.0%)
        mockMvc.perform(get("/courses/{courseId}/progress", courseId)
                        .header("X-User-Id", STUDENT_ID)
                        .header("X-User-Role", "STUDENT")
                        .header("X-User-Email", "student@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.totalLessons").value(2))
                .andExpect(jsonPath("$.data.completedLessons").value(1))
                .andExpect(jsonPath("$.data.percentComplete").value(50.0))
                .andExpect(jsonPath("$.data.completedLessonIds[0]").value(LESSON_1_ID));
    }

    @Test
    @DisplayName("31. instructorPublishesAnnouncementAndSchedulesMeetingWorkflow: Giảng viên tạo thông báo & lịch học -> Học viên đã ghi danh xem đầy đủ")
    void instructorPublishesAnnouncementAndSchedulesMeetingWorkflow() throws Exception {
        // 1. Học viên ghi danh khóa học trước để có quyền xem thông báo/lịch học
        mockMvc.perform(post("/courses/{id}/enroll", courseId)
                        .header("X-User-Id", STUDENT_ID)
                        .header("X-User-Role", "STUDENT")
                        .header("X-User-Email", "student@test.com"))
                .andExpect(status().isOk());

        // 2. Giảng viên tạo thông báo mới
        CreateAnnouncementRequest announcementReq = CreateAnnouncementRequest.builder()
                .title("Thông báo khai giảng")
                .content("Khóa học sẽ bắt đầu vào tuần tới, các bạn chuẩn bị môi trường.")
                .build();

        mockMvc.perform(post("/courses/{courseId}/announcements", courseId)
                        .header("X-User-Id", INSTRUCTOR_ID)
                        .header("X-User-Role", "INSTRUCTOR")
                        .header("X-User-Email", "instructor@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(announcementReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.title").value("Thông báo khai giảng"));

        // 3. Giảng viên tạo lịch học trực tuyến
        Instant now = Instant.now();
        CreateScheduleRequest scheduleReq = CreateScheduleRequest.builder()
                .title("Buổi học 1: Tổng quan Microservices")
                .startTime(now.plus(2, ChronoUnit.DAYS))
                .endTime(now.plus(2, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS))
                .note("Học qua Google Meet")
                .build();

        mockMvc.perform(post("/courses/{courseId}/schedules", courseId)
                        .header("X-User-Id", INSTRUCTOR_ID)
                        .header("X-User-Role", "INSTRUCTOR")
                        .header("X-User-Email", "instructor@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(scheduleReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.title").value("Buổi học 1: Tổng quan Microservices"));

        // 4. Học viên truy vấn danh sách thông báo của khóa học
        mockMvc.perform(get("/courses/{courseId}/announcements", courseId)
                        .header("X-User-Id", STUDENT_ID)
                        .header("X-User-Role", "STUDENT")
                        .header("X-User-Email", "student@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.content[0].title").value("Thông báo khai giảng"));

        // 5. Học viên truy vấn danh sách lịch học của khóa học
        mockMvc.perform(get("/courses/{courseId}/schedules", courseId)
                        .header("X-User-Id", STUDENT_ID)
                        .header("X-User-Role", "STUDENT")
                        .header("X-User-Email", "student@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data[0].title").value("Buổi học 1: Tổng quan Microservices"));
    }
}
