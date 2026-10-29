package com.lms.courseservice.service.impl;

import com.lms.courseservice.configuration.GatewayAuthentication;
import com.lms.courseservice.dto.request.CompleteLessonRequest;
import com.lms.courseservice.dto.response.CourseProgressResponse;
import com.lms.courseservice.entity.mysql.LessonProgress;
import com.lms.courseservice.enums.LessonType;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.repository.mysql.EnrollmentRepository;
import com.lms.courseservice.repository.mysql.LessonProgressRepository;
import com.lms.courseservice.service.CourseStructureService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonProgressServiceImplTest {

    @Mock
    private LessonProgressRepository progressRepo;

    @Mock
    private EnrollmentRepository enrollmentRepo;

    @Mock
    private CourseStructureService structureService;

    @InjectMocks
    private LessonProgressServiceImpl progressService;

    private static final String STUDENT_ID = "student-123";
    private static final String COURSE_ID = "course-456";
    private static final String LESSON_ID = "lesson-789";

    @BeforeEach
    void setUp() {
        GatewayAuthentication auth = new GatewayAuthentication(
                STUDENT_ID,
                "student@lms.com",
                "ROLE_STUDENT",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("1. completeLessonValidStudentMarksCompleted: Học viên đánh dấu hoàn thành bài học -> Lưu bản ghi LessonProgress với completed = true")
    void completeLessonValidStudentMarksCompleted() {
        when(enrollmentRepo.existsByUserIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(true);
        when(structureService.isLessonInCourse(COURSE_ID, LESSON_ID)).thenReturn(true);
        when(progressRepo.findByStudentIdAndLessonId(STUDENT_ID, LESSON_ID)).thenReturn(Optional.empty());

        CompleteLessonRequest request = CompleteLessonRequest.builder()
                .lessonType(LessonType.VIDEO)
                .build();

        progressService.completeLesson(COURSE_ID, LESSON_ID, request);

        verify(progressRepo).save(argThat(lp ->
                lp.isCompleted() &&
                        COURSE_ID.equals(lp.getCourseId()) &&
                        LESSON_ID.equals(lp.getLessonId()) &&
                        STUDENT_ID.equals(lp.getStudentId())
        ));
    }

    @Test
    @DisplayName("2. getCourseProgressEnrolledStudentReturnsPercentage: Tính toán chuẩn xác tỷ lệ phần trăm tiến độ hoàn thành khóa học")
    void getCourseProgressEnrolledStudentReturnsPercentage() {
        when(structureService.countLessons(COURSE_ID)).thenReturn(4);

        LessonProgress lp1 = LessonProgress.builder()
                .studentId(STUDENT_ID)
                .courseId(COURSE_ID)
                .lessonId("lesson-1")
                .completed(true)
                .build();
        LessonProgress lp2 = LessonProgress.builder()
                .studentId(STUDENT_ID)
                .courseId(COURSE_ID)
                .lessonId("lesson-2")
                .completed(true)
                .build();

        when(progressRepo.findByStudentIdAndCourseId(STUDENT_ID, COURSE_ID))
                .thenReturn(List.of(lp1, lp2));

        CourseProgressResponse response = progressService.getCourseProgress(COURSE_ID);

        assertThat(response).isNotNull();
        assertThat(response.getCourseId()).isEqualTo(COURSE_ID);
        assertThat(response.getTotalLessons()).isEqualTo(4);
        assertThat(response.getCompletedLessons()).isEqualTo(2);
        assertThat(response.getPercentComplete()).isEqualTo(50.0);
        assertThat(response.getCompletedLessonIds()).containsExactlyInAnyOrder("lesson-1", "lesson-2");
    }

    @Test
    @DisplayName("3. getAverageProgressInstructorReturnsAverage: Giảng viên xem tiến độ trung bình của tất cả học viên trong khóa")
    void getAverageProgressInstructorReturnsAverage() {
        when(structureService.countLessons(COURSE_ID)).thenReturn(5);
        when(enrollmentRepo.countByCourseId(COURSE_ID)).thenReturn(2L);
        when(progressRepo.countByCourseIdAndCompletedTrue(COURSE_ID)).thenReturn(6L);

        double average = progressService.getAverageProgress(COURSE_ID);

        // 6 * 100 / (5 * 2) = 60.0
        assertThat(average).isEqualTo(60.0);
    }
}
