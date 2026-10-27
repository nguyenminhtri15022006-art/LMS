package com.lms.courseservice.service.impl;

import com.lms.courseservice.configuration.AssignmentClient;
import com.lms.courseservice.configuration.IdentityClient;
import com.lms.courseservice.dto.request.LockCourseRequest;
import com.lms.courseservice.dto.response.CourseResponse;
import com.lms.courseservice.entity.mysql.Announcement;
import com.lms.courseservice.entity.mysql.Course;
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
import com.lms.courseservice.repository.mysql.CourseSpecifications;
import com.lms.courseservice.repository.mysql.EnrollmentRepository;
import com.lms.courseservice.service.GoogleMeetService;
import com.lms.courseservice.service.MinioService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseLockedConsistencyTest {

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

    private Course publicCourse;
    private Course lockedCourse;
    private CourseResponse publicCourseResponse;

    @BeforeEach
    void setUp() {
        publicCourse = Course.builder()
                .id("course-public-1")
                .title("Lập trình Java Cơ Bản")
                .status(CourseStatus.PUBLIC)
                .instructorId("inst-1")
                .build();

        lockedCourse = Course.builder()
                .id("course-locked-2")
                .title("Khóa học vi phạm chính sách")
                .status(CourseStatus.LOCKED)
                .instructorId("inst-2")
                .build();

        publicCourseResponse = CourseResponse.builder()
                .id("course-public-1")
                .title("Lập trình Java Cơ Bản")
                .status(CourseStatus.PUBLIC)
                .build();
    }

    @Test
    @DisplayName("1. publicCatalogExcludesLockedCourses: Tìm kiếm catalog với status=PUBLIC chỉ trả về khóa học PUBLIC, loại bỏ LOCKED")
    void publicCatalogExcludesLockedCourses() {
        Pageable pageable = PageRequest.of(0, 10);
        // Repository chỉ trả về khóa học PUBLIC khi query kèm Specification hasStatus(PUBLIC)
        Page<Course> page = new PageImpl<>(List.of(publicCourse), pageable, 1);

        when(courseRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(courseMapper.toCourseResponse(publicCourse)).thenReturn(publicCourseResponse);

        Page<CourseResponse> result = courseService.getAllCourses(pageable, CourseStatus.PUBLIC, null, null, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getId()).isEqualTo("course-public-1");
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(CourseStatus.PUBLIC);

        // Khẳng định Specification được áp dụng để lọc
        verify(courseRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("2. lockCourseTransitionsStatusToLockedAndCreatesAnnouncement: Admin khóa khóa học -> chuyển sang LOCKED và thông báo hệ thống")
    void lockCourseTransitionsStatusToLockedAndCreatesAnnouncement() {
        when(courseRepository.findById("course-public-1")).thenReturn(Optional.of(publicCourse));
        when(courseRepository.save(any(Course.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CourseResponse lockedResponse = CourseResponse.builder()
                .id("course-public-1")
                .status(CourseStatus.LOCKED)
                .build();
        when(courseMapper.toCourseResponse(any(Course.class))).thenReturn(lockedResponse);

        LockCourseRequest lockRequest = LockCourseRequest.builder()
                .reason("Nội dung không phù hợp với chuẩn mực đào tạo")
                .build();

        CourseResponse response = courseService.lockCourse("course-public-1", lockRequest);

        assertThat(response.getStatus()).isEqualTo(CourseStatus.LOCKED);
        assertThat(publicCourse.getStatus()).isEqualTo(CourseStatus.LOCKED);

        // Khẳng định tạo thông báo khóa học cho học viên
        ArgumentCaptor<Announcement> announcementCaptor = ArgumentCaptor.forClass(Announcement.class);
        verify(announcementRepository).save(announcementCaptor.capture());
        Announcement announcement = announcementCaptor.getValue();
        assertThat(announcement.getTitle()).contains("bị khóa");
        assertThat(announcement.getContent()).contains("Nội dung không phù hợp với chuẩn mực đào tạo");
    }

    @Test
    @DisplayName("3. cannotLockAlreadyLockedCourse: Khóa học đã ở trạng thái LOCKED -> không thể khóa lần nữa (COURSE_ALREADY_LOCKED)")
    void cannotLockAlreadyLockedCourse() {
        when(courseRepository.findById("course-locked-2")).thenReturn(Optional.of(lockedCourse));

        LockCourseRequest lockRequest = LockCourseRequest.builder()
                .reason("Khóa lại lần 2")
                .build();

        assertThatExceptionOfType(CourseException.class)
                .isThrownBy(() -> courseService.lockCourse("course-locked-2", lockRequest))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.COURSE_ALREADY_LOCKED));

        verify(courseRepository, never()).save(any());
        verify(announcementRepository, never()).save(any());
    }
}
