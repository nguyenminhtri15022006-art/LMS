package com.lms.courseservice.service.impl;

import com.lms.courseservice.configuration.AssignmentClient;
import com.lms.courseservice.configuration.GatewayAuthentication;
import com.lms.courseservice.configuration.IdentityClient;
import com.lms.courseservice.dto.MeetingInfo;
import com.lms.courseservice.dto.request.CreateCourseRequest;
import com.lms.courseservice.dto.request.LockCourseRequest;
import com.lms.courseservice.dto.request.UpdateCourseRequest;
import com.lms.courseservice.dto.response.CourseResponse;
import com.lms.courseservice.entity.mongo.CourseStructure;
import com.lms.courseservice.entity.mysql.Announcement;
import com.lms.courseservice.entity.mysql.Course;
import com.lms.courseservice.enums.CourseLevel;
import com.lms.courseservice.enums.CourseStatus;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.mapper.CourseMapper;
import com.lms.courseservice.mapper.EnrollmentMapper;
import com.lms.courseservice.repository.mongo.CourseStructureRepository;
import com.lms.courseservice.repository.mongo.LessonRepository;
import com.lms.courseservice.repository.mysql.AnnouncementRepository;
import com.lms.courseservice.repository.mysql.CategoryRepository;
import com.lms.courseservice.repository.mysql.CourseRepository;
import com.lms.courseservice.repository.mysql.EnrollmentRepository;
import com.lms.courseservice.service.GoogleMeetService;
import com.lms.courseservice.service.MinioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseServiceImplTest {

    @Mock
    private CourseRepository courseRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private AnnouncementRepository announcementRepository;
    @Mock
    private CourseStructureRepository courseStructureRepository;
    @Mock
    private LessonRepository lessonRepository;
    @Mock
    private CourseMapper courseMapper;
    @Mock
    private EnrollmentMapper enrollmentMapper;
    @Mock
    private MinioService minioService;
    @Mock
    private IdentityClient identityClient;
    @Mock
    private AssignmentClient assignmentClient;
    @Mock
    private GoogleMeetService googleMeetService;

    @InjectMocks
    private CourseServiceImpl courseService;

    private final String instructorId = "inst-1111-2222";
    private final String otherUserId = "other-3333-4444";
    private final String courseId = "course-uuid-1234";

    private Course testCourse;
    private CourseResponse testCourseResponse;

    @BeforeEach
    void setUp() {
        testCourse = Course.builder()
                .id(courseId)
                .title("Spring Boot Microservices")
                .description("Học Spring Boot toàn diện")
                .instructorId(instructorId)
                .status(CourseStatus.PRIVATE)
                .level(CourseLevel.BEGINNER)
                .mongoStructureId("structure-uuid-5678")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testCourseResponse = CourseResponse.builder()
                .id(courseId)
                .title("Spring Boot Microservices")
                .description("Học Spring Boot toàn diện")
                .instructorId(instructorId)
                .status(CourseStatus.PRIVATE)
                .level(CourseLevel.BEGINNER)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new GatewayAuthentication(instructorId, "instructor@example.com", "ROLE_INSTRUCTOR",
                        List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("5. createCourseInstructorSuccess: Giảng viên tạo khóa học mới -> Trạng thái mặc định là CourseStatus.PRIVATE")
    void createCourseInstructorSuccess() {
        CreateCourseRequest request = CreateCourseRequest.builder()
                .title("Spring Boot Microservices")
                .description("Học Spring Boot toàn diện")
                .status(null)
                .level(CourseLevel.BEGINNER)
                .build();

        when(courseRepository.existsByTitle(request.getTitle())).thenReturn(false);
        when(courseMapper.toCourse(request)).thenReturn(testCourse);
        when(googleMeetService.createMeeting(any())).thenReturn(MeetingInfo.builder().meetingUrl("https://meet.google.com/abc").build());
        when(courseRepository.save(any(Course.class))).thenReturn(testCourse);
        when(courseStructureRepository.save(any(CourseStructure.class))).thenReturn(
                CourseStructure.builder().id("struct-1").courseId(courseId).nodes(new ArrayList<>()).build()
        );
        when(courseMapper.toCourseResponse(any(Course.class))).thenReturn(testCourseResponse);

        CourseResponse result = courseService.createCourse(request);

        assertNotNull(result);
        assertEquals(CourseStatus.PRIVATE, result.getStatus());
        verify(courseRepository, atLeastOnce()).save(any(Course.class));
        verify(courseStructureRepository).save(any(CourseStructure.class));
    }

    @Test
    @DisplayName("6. updateCourseOwnerSuccess: Chủ sở hữu cập nhật thông tin khóa học thành công")
    void updateCourseOwnerSuccess() {
        UpdateCourseRequest request = UpdateCourseRequest.builder()
                .title("Spring Boot Microservices Cập Nhật")
                .status(CourseStatus.PUBLIC)
                .level(CourseLevel.INTERMEDIATE)
                .build();

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(testCourse));
        when(courseRepository.save(testCourse)).thenReturn(testCourse);
        when(courseMapper.toCourseResponse(testCourse)).thenReturn(testCourseResponse);

        CourseResponse result = courseService.updateCourse(courseId, request);

        assertNotNull(result);
        verify(courseMapper).updateCourse(testCourse, request);
        verify(courseRepository).save(testCourse);
    }

    @Test
    @DisplayName("7. updateCourseNotOwnerThrowsForbidden: Người không phải tác giả cập nhật -> Ném lỗi 403 FORBIDDEN")
    void updateCourseNotOwnerThrowsForbidden() {
        // Switch security context to another user
        SecurityContextHolder.getContext().setAuthentication(
                new GatewayAuthentication(otherUserId, "other@example.com", "ROLE_INSTRUCTOR",
                        List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR")))
        );

        UpdateCourseRequest request = UpdateCourseRequest.builder()
                .title("Spring Boot Hack")
                .status(CourseStatus.PUBLIC)
                .build();

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(testCourse));

        CourseException ex = assertThrows(CourseException.class,
                () -> courseService.updateCourse(courseId, request));

        assertEquals(ErrorCode.NOT_COURSE_OWNER, ex.getErrorCode());
        verify(courseRepository, never()).save(any());
    }

    @Test
    @DisplayName("8. deleteCourseOwnerSuccess: Tác giả xóa khóa học của mình thành công")
    void deleteCourseOwnerSuccess() {
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(testCourse));

        assertDoesNotThrow(() -> courseService.deleteCourse(courseId));

        verify(lessonRepository).deleteAllByCourseId(courseId);
        verify(courseStructureRepository).deleteById(testCourse.getMongoStructureId());
        verify(enrollmentRepository).deleteAllByCourse(testCourse);
        verify(courseRepository).delete(testCourse);
    }

    @Test
    @DisplayName("9. getMyCoursesInstructorReturnsPage: Giảng viên xem danh sách các khóa học do chính mình tạo")
    void getMyCoursesInstructorReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Course> page = new PageImpl<>(List.of(testCourse), pageable, 1);

        when(courseRepository.findByInstructorId(instructorId, pageable)).thenReturn(page);
        when(courseMapper.toCourseResponse(testCourse)).thenReturn(testCourseResponse);

        Page<CourseResponse> result = courseService.getMyCourses(pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(courseId, result.getContent().get(0).getId());
        verify(courseRepository).findByInstructorId(instructorId, pageable);
    }

    @Test
    @DisplayName("10. lockCourseAdminUpdatesStatus: Admin khóa khóa học vi phạm -> Cập nhật trạng thái thành CourseStatus.LOCKED")
    void lockCourseAdminUpdatesStatus() {
        LockCourseRequest request = LockCourseRequest.builder()
                .reason("Nội dung vi phạm bản quyền")
                .build();

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(testCourse));
        when(courseRepository.save(testCourse)).thenReturn(testCourse);

        CourseResponse lockedResponse = CourseResponse.builder()
                .id(courseId)
                .status(CourseStatus.LOCKED)
                .build();
        when(courseMapper.toCourseResponse(testCourse)).thenReturn(lockedResponse);

        CourseResponse result = courseService.lockCourse(courseId, request);

        assertNotNull(result);
        assertEquals(CourseStatus.LOCKED, result.getStatus());
        assertEquals(CourseStatus.LOCKED, testCourse.getStatus());
        verify(announcementRepository).save(any(Announcement.class));
        verify(courseRepository).save(testCourse);
    }

    @Test
    @DisplayName("11. unlockCourseAdminUpdatesStatus: Admin mở khóa khóa học -> Cập nhật trạng thái thành CourseStatus.PRIVATE")
    void unlockCourseAdminUpdatesStatus() {
        testCourse.setStatus(CourseStatus.LOCKED);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(testCourse));
        when(courseRepository.save(testCourse)).thenReturn(testCourse);

        CourseResponse unlockedResponse = CourseResponse.builder()
                .id(courseId)
                .status(CourseStatus.PRIVATE)
                .build();
        when(courseMapper.toCourseResponse(testCourse)).thenReturn(unlockedResponse);

        CourseResponse result = courseService.unlockCourse(courseId);

        assertNotNull(result);
        assertEquals(CourseStatus.PRIVATE, result.getStatus());
        assertEquals(CourseStatus.PRIVATE, testCourse.getStatus());
        verify(courseRepository).save(testCourse);
    }

    @Test
    @DisplayName("12. getCourseByIdPublicReturnsDetails: Người dùng xem chi tiết khóa học kèm thông tin giảng viên và danh mục")
    void getCourseByIdPublicReturnsDetails() {
        when(courseRepository.findById(courseId)).thenReturn(Optional.of(testCourse));
        when(courseMapper.toCourseResponse(testCourse)).thenReturn(testCourseResponse);

        CourseResponse result = courseService.getCourseById(courseId);

        assertNotNull(result);
        assertEquals(courseId, result.getId());
        assertEquals("Spring Boot Microservices", result.getTitle());
        verify(courseRepository).findById(courseId);
    }
}
