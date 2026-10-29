package com.lms.courseservice.service.impl;

import com.lms.courseservice.configuration.GatewayAuthentication;
import com.lms.courseservice.dto.request.UpdateLessonRequest;
import com.lms.courseservice.dto.response.LessonResponse;
import com.lms.courseservice.entity.mongo.Lesson;
import com.lms.courseservice.entity.mysql.Course;
import com.lms.courseservice.enums.CourseStatus;
import com.lms.courseservice.enums.LessonType;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.mapper.LessonMapper;
import com.lms.courseservice.repository.mongo.LessonRepository;
import com.lms.courseservice.repository.mysql.CourseRepository;
import com.lms.courseservice.repository.mysql.EnrollmentRepository;
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

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LessonServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private LessonMapper lessonMapper;

    @InjectMocks
    private LessonServiceImpl lessonService;

    private final String ownerInstructorId = "instructor-owner-111";
    private final String unauthorizedUserId = "unauthorized-user-222";
    private final String courseId = "course-123";
    private final String lessonId = "lesson-456";

    private Course testCourse;
    private Lesson testLesson;
    private LessonResponse testLessonResponse;

    @BeforeEach
    void setUp() {
        testCourse = Course.builder()
                .id(courseId)
                .title("Lập trình Java")
                .instructorId(ownerInstructorId)
                .status(CourseStatus.PRIVATE)
                .build();

        testLesson = Lesson.builder()
                .id(lessonId)
                .courseId(courseId)
                .lessonType(LessonType.VIDEO)
                .content(new HashMap<>(Map.of("video_url", "https://video.mp4")))
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testLessonResponse = LessonResponse.builder()
                .id(lessonId)
                .courseId(courseId)
                .lessonType(LessonType.VIDEO)
                .content(testLesson.getContent())
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new GatewayAuthentication(ownerInstructorId, "owner@example.com", "ROLE_INSTRUCTOR",
                        List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("19. updateLessonContentOwnerSuccess: Giảng viên chủ sở hữu khóa học cập nhật nội dung video URL và Markdown bài học thành công")
    void updateLessonContentOwnerSuccess() {
        UpdateLessonRequest request = UpdateLessonRequest.builder()
                .lessonType(LessonType.VIDEO)
                .content(Map.of("video_url", "https://new-video.mp4", "description", "# Bài giảng mới"))
                .build();

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(testCourse));
        when(lessonRepository.findById(lessonId)).thenReturn(Optional.of(testLesson));
        when(lessonRepository.save(any(Lesson.class))).thenReturn(testLesson);
        when(lessonMapper.toLessonResponse(testLesson)).thenReturn(testLessonResponse);

        LessonResponse result = lessonService.updateLesson(courseId, lessonId, request);

        assertNotNull(result);
        assertEquals(lessonId, result.getId());
        verify(lessonRepository).save(testLesson);
    }

    @Test
    @DisplayName("20. updateLessonContentUnauthorizedThrowsForbidden: Người không phải tác giả khóa học cập nhật nội dung bài học -> Bị từ chối và ném lỗi 403 FORBIDDEN")
    void updateLessonContentUnauthorizedThrowsForbidden() {
        // Set SecurityContext to unauthorized user
        SecurityContextHolder.getContext().setAuthentication(
                new GatewayAuthentication(unauthorizedUserId, "stranger@example.com", "ROLE_INSTRUCTOR",
                        List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
        );

        UpdateLessonRequest request = UpdateLessonRequest.builder()
                .lessonType(LessonType.VIDEO)
                .content(Map.of("video_url", "https://hacked.mp4"))
                .build();

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(testCourse));

        CourseException ex = assertThrows(CourseException.class,
                () -> lessonService.updateLesson(courseId, lessonId, request));

        assertEquals(ErrorCode.NOT_COURSE_OWNER, ex.getErrorCode());
        assertEquals(403, ex.getErrorCode().getHttpStatusCode().value());
        verify(lessonRepository, never()).save(any());
    }
}
