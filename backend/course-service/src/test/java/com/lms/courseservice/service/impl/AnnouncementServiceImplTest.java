package com.lms.courseservice.service.impl;

import com.lms.courseservice.configuration.GatewayAuthentication;
import com.lms.courseservice.dto.request.CreateAnnouncementRequest;
import com.lms.courseservice.dto.response.AnnouncementResponse;
import com.lms.courseservice.entity.mysql.Announcement;
import com.lms.courseservice.entity.mysql.Course;
import com.lms.courseservice.enums.CourseStatus;
import com.lms.courseservice.mapper.AnnouncementMapper;
import com.lms.courseservice.repository.mysql.AnnouncementRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnnouncementServiceImplTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private AnnouncementRepository announcementRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private AnnouncementMapper announcementMapper;

    @InjectMocks
    private AnnouncementServiceImpl announcementService;

    private static final String INSTRUCTOR_ID = "instructor-123";
    private static final String STUDENT_ID = "student-123";
    private static final String COURSE_ID = "course-123";
    private static final Long ANNOUNCEMENT_ID = 1L;

    private Course course;

    @BeforeEach
    void setUp() {
        course = Course.builder()
                .id(COURSE_ID)
                .title("Lập trình Java")
                .instructorId(INSTRUCTOR_ID)
                .status(CourseStatus.PUBLIC)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("8. createAnnouncementInstructorSuccess: Giảng viên đăng thông báo khóa học thành công")
    void createAnnouncementInstructorSuccess() {
        GatewayAuthentication auth = new GatewayAuthentication(
                INSTRUCTOR_ID, "inst@lms.com", "ROLE_INSTRUCTOR",
                List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));

        CreateAnnouncementRequest request = CreateAnnouncementRequest.builder()
                .title("Thông báo nộp bài tập tuần 1")
                .content("Hạn chót là 23h59 Chủ nhật")
                .build();

        Announcement savedAnnouncement = Announcement.builder()
                .id(ANNOUNCEMENT_ID)
                .course(course)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        AnnouncementResponse response = AnnouncementResponse.builder()
                .id(ANNOUNCEMENT_ID)
                .courseId(COURSE_ID)
                .title(request.getTitle())
                .content(request.getContent())
                .build();

        when(announcementRepository.save(any(Announcement.class))).thenReturn(savedAnnouncement);
        when(announcementMapper.toAnnouncementResponse(savedAnnouncement)).thenReturn(response);

        AnnouncementResponse actual = announcementService.createAnnouncement(COURSE_ID, request);

        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(ANNOUNCEMENT_ID);
        assertThat(actual.getTitle()).isEqualTo("Thông báo nộp bài tập tuần 1");
    }

    @Test
    @DisplayName("9. getAnnouncementsEnrolledStudentReturnsList: Học viên xem danh sách thông báo của khóa học đã ghi danh")
    void getAnnouncementsEnrolledStudentReturnsList() {
        GatewayAuthentication auth = new GatewayAuthentication(
                STUDENT_ID, "student@lms.com", "ROLE_STUDENT",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        when(enrollmentRepository.existsByUserIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(true);

        Pageable pageable = PageRequest.of(0, 10);
        Announcement announcement = Announcement.builder()
                .id(ANNOUNCEMENT_ID)
                .course(course)
                .title("Thông báo 1")
                .build();

        Page<Announcement> page = new PageImpl<>(List.of(announcement));
        when(announcementRepository.findByCourseId(COURSE_ID, pageable)).thenReturn(page);

        AnnouncementResponse response = AnnouncementResponse.builder()
                .id(ANNOUNCEMENT_ID)
                .title("Thông báo 1")
                .build();
        when(announcementMapper.toAnnouncementResponse(announcement)).thenReturn(response);

        Page<AnnouncementResponse> result = announcementService.getAnnouncements(COURSE_ID, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Thông báo 1");
    }

    @Test
    @DisplayName("10. deleteAnnouncementInstructorSuccess: Giảng viên xóa thông báo của mình thành công")
    void deleteAnnouncementInstructorSuccess() {
        GatewayAuthentication auth = new GatewayAuthentication(
                INSTRUCTOR_ID, "inst@lms.com", "ROLE_INSTRUCTOR",
                List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));

        Announcement announcement = Announcement.builder()
                .id(ANNOUNCEMENT_ID)
                .course(course)
                .build();

        when(announcementRepository.findById(ANNOUNCEMENT_ID)).thenReturn(Optional.of(announcement));

        announcementService.deleteAnnouncement(COURSE_ID, ANNOUNCEMENT_ID);

        verify(announcementRepository).delete(announcement);
    }
}
