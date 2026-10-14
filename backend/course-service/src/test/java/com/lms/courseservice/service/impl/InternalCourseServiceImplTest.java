package com.lms.courseservice.service.impl;

import com.lms.courseservice.dto.response.ChatbotCourseResponse;
import com.lms.courseservice.dto.response.InternalCourseResponse;
import com.lms.courseservice.dto.response.InternalLessonResponse;
import com.lms.courseservice.entity.mongo.Lesson;
import com.lms.courseservice.entity.mysql.Category;
import com.lms.courseservice.entity.mysql.Course;
import com.lms.courseservice.entity.mysql.Enrollment;
import com.lms.courseservice.enums.CourseStatus;
import com.lms.courseservice.enums.LessonType;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.repository.mongo.LessonRepository;
import com.lms.courseservice.repository.mysql.CourseRepository;
import com.lms.courseservice.repository.mysql.EnrollmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalCourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @InjectMocks
    private InternalCourseServiceImpl internalCourseService;

    private static final String COURSE_ID = "course-123";
    private static final String LESSON_ID = "lesson-123";
    private static final String USER_ID = "user-123";
    private static final String INSTRUCTOR_ID = "instructor-123";

    private Course course;
    private Lesson lesson;

    @BeforeEach
    void setUp() {
        course = Course.builder()
                .id(COURSE_ID)
                .title("Lập trình Spring Boot")
                .description("Khóa học chuyên sâu")
                .instructorId(INSTRUCTOR_ID)
                .status(CourseStatus.PUBLIC)
                .categories(List.of(Category.builder().name("Backend").build()))
                .build();

        lesson = Lesson.builder()
                .id(LESSON_ID)
                .courseId(COURSE_ID)
                .lessonType(LessonType.VIDEO)
                .build();
    }

    @Test
    @DisplayName("15. internalGetCourseReturnsCourseResponse: Service truy vấn thông tin khóa học hợp lệ -> Trả về InternalCourseResponse")
    void internalGetCourseReturnsCourseResponse() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));

        InternalCourseResponse response = internalCourseService.getCourse(COURSE_ID);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(COURSE_ID);
        assertThat(response.getTitle()).isEqualTo("Lập trình Spring Boot");
        assertThat(response.getInstructorId()).isEqualTo(INSTRUCTOR_ID);
        assertThat(response.getStatus()).isEqualTo(CourseStatus.PUBLIC);
    }

    @Test
    @DisplayName("16. internalGetLessonReturnsLessonResponse: Service xác thực bài học đính kèm quiz -> Trả về InternalLessonResponse")
    void internalGetLessonReturnsLessonResponse() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(lessonRepository.findById(LESSON_ID)).thenReturn(Optional.of(lesson));

        InternalLessonResponse response = internalCourseService.getLesson(COURSE_ID, LESSON_ID);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(LESSON_ID);
        assertThat(response.getCourseId()).isEqualTo(COURSE_ID);
        assertThat(response.getLessonType()).isEqualTo(LessonType.VIDEO);
    }

    @Test
    @DisplayName("17. internalCheckEnrollmentEnrolledUserReturnsTrue: Service kiểm tra học viên đã ghi danh -> Trả về true")
    void internalCheckEnrollmentEnrolledUserReturnsTrue() {
        when(enrollmentRepository.existsByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(true);

        boolean enrolled = internalCourseService.existsEnrollment(COURSE_ID, USER_ID);

        assertThat(enrolled).isTrue();
    }

    @Test
    @DisplayName("18. internalCheckEnrollmentUnenrolledUserReturnsFalse: Học viên chưa ghi danh -> Trả về false")
    void internalCheckEnrollmentUnenrolledUserReturnsFalse() {
        when(enrollmentRepository.existsByUserIdAndCourseId(USER_ID, COURSE_ID)).thenReturn(false);

        boolean enrolled = internalCourseService.existsEnrollment(COURSE_ID, USER_ID);

        assertThat(enrolled).isFalse();
    }

    @Test
    @DisplayName("19. internalCountStudentsReturnsCount: Trả về số lượng học viên ghi danh trong khóa học")
    void internalCountStudentsReturnsCount() {
        when(enrollmentRepository.countByCourseId(COURSE_ID)).thenReturn(42L);

        long count = internalCourseService.countStudents(COURSE_ID);

        assertThat(count).isEqualTo(42L);
    }

    @Test
    @DisplayName("20. internalGetAllPublishedCoursesReturnsChatbotList: Service lấy danh sách toàn bộ khóa học công khai (PUBLIC)")
    void internalGetAllPublishedCoursesReturnsChatbotList() {
        when(courseRepository.findByStatus(eq(CourseStatus.PUBLIC), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(course)));

        List<ChatbotCourseResponse> courses = internalCourseService.getAllPublishedCourses();

        assertThat(courses).hasSize(1);
        assertThat(courses.get(0).getId()).isEqualTo(COURSE_ID);
        assertThat(courses.get(0).getTitle()).isEqualTo("Lập trình Spring Boot");
        assertThat(courses.get(0).getCategoryNames()).contains("Backend");
    }

    @Test
    @DisplayName("21. internalGetEnrolledCoursesReturnsChatbotList: Service lấy danh sách khóa học của học viên theo userId")
    void internalGetEnrolledCoursesReturnsChatbotList() {
        Enrollment enrollment = Enrollment.builder()
                .userId(USER_ID)
                .course(course)
                .build();

        when(enrollmentRepository.findByUserId(USER_ID)).thenReturn(List.of(enrollment));

        List<ChatbotCourseResponse> courses = internalCourseService.getEnrolledCourses(USER_ID);

        assertThat(courses).hasSize(1);
        assertThat(courses.get(0).getId()).isEqualTo(COURSE_ID);
        assertThat(courses.get(0).getTitle()).isEqualTo("Lập trình Spring Boot");
    }
}
