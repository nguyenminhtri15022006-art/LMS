package com.lms.courseservice.service.impl;

import com.lms.courseservice.configuration.GatewayAuthentication;
import com.lms.courseservice.dto.request.CreateScheduleRequest;
import com.lms.courseservice.dto.request.UpdateScheduleRequest;
import com.lms.courseservice.dto.response.ScheduleResponse;
import com.lms.courseservice.entity.mysql.Course;
import com.lms.courseservice.entity.mysql.Schedule;
import com.lms.courseservice.enums.CourseStatus;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.mapper.ScheduleMapper;
import com.lms.courseservice.repository.mysql.CourseRepository;
import com.lms.courseservice.repository.mysql.EnrollmentRepository;
import com.lms.courseservice.repository.mysql.ScheduleRepository;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private ScheduleMapper scheduleMapper;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    private static final String INSTRUCTOR_ID = "instructor-123";
    private static final String COURSE_ID = "course-123";
    private static final Long SCHEDULE_ID = 1L;

    private Course course;

    @BeforeEach
    void setUp() {
        GatewayAuthentication auth = new GatewayAuthentication(
                INSTRUCTOR_ID,
                "instructor@lms.com",
                "ROLE_INSTRUCTOR",
                List.of(new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        course = Course.builder()
                .id(COURSE_ID)
                .title("Lập trình Spring Boot")
                .instructorId(INSTRUCTOR_ID)
                .status(CourseStatus.PUBLIC)
                .meetingUrl("https://meet.google.com/abc-defg-hij")
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("4. createScheduleValidDataReturnsSchedule: Giảng viên tạo lịch học Google Meet thành công")
    void createScheduleValidDataReturnsSchedule() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));

        Instant start = Instant.now().plus(1, ChronoUnit.DAYS);
        Instant end = start.plus(2, ChronoUnit.HOURS);

        CreateScheduleRequest request = CreateScheduleRequest.builder()
                .title("Buổi học Q&A trực tuyến")
                .startTime(start)
                .endTime(end)
                .note("Chuẩn bị câu hỏi trước khi vào Meet")
                .build();

        Schedule savedSchedule = Schedule.builder()
                .id(SCHEDULE_ID)
                .course(course)
                .title(request.getTitle())
                .startTime(start)
                .endTime(end)
                .note(request.getNote())
                .build();

        ScheduleResponse expectedResponse = ScheduleResponse.builder()
                .id(SCHEDULE_ID)
                .courseId(COURSE_ID)
                .title(request.getTitle())
                .startTime(start)
                .endTime(end)
                .meetingUrl(course.getMeetingUrl())
                .build();

        when(scheduleRepository.save(any(Schedule.class))).thenReturn(savedSchedule);
        when(scheduleMapper.toScheduleResponse(savedSchedule)).thenReturn(expectedResponse);

        ScheduleResponse actual = scheduleService.createSchedule(COURSE_ID, request);

        assertThat(actual).isNotNull();
        assertThat(actual.getId()).isEqualTo(SCHEDULE_ID);
        assertThat(actual.getTitle()).isEqualTo("Buổi học Q&A trực tuyến");
        assertThat(actual.getMeetingUrl()).isEqualTo("https://meet.google.com/abc-defg-hij");
    }

    @Test
    @DisplayName("5. createScheduleInvalidTimeThrowsException: Tạo lịch học thất bại khi startTime >= endTime -> Ném ngoại lệ validation")
    void createScheduleInvalidTimeThrowsException() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));

        Instant start = Instant.now().plus(2, ChronoUnit.DAYS);
        Instant end = start.minus(1, ChronoUnit.HOURS); // end trước start!

        CreateScheduleRequest request = CreateScheduleRequest.builder()
                .title("Lịch học thời gian sai")
                .startTime(start)
                .endTime(end)
                .build();

        assertThatThrownBy(() -> scheduleService.createSchedule(COURSE_ID, request))
                .isInstanceOf(CourseException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SCHEDULE_TIME_INVALID);

        verify(scheduleRepository, never()).save(any());
    }

    @Test
    @DisplayName("6. updateScheduleOwnerSuccess: Cập nhật lịch học thành công bởi chính chủ giảng viên")
    void updateScheduleOwnerSuccess() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));

        Schedule existing = Schedule.builder()
                .id(SCHEDULE_ID)
                .course(course)
                .title("Tiêu đề cũ")
                .startTime(Instant.now().plus(1, ChronoUnit.DAYS))
                .endTime(Instant.now().plus(1, ChronoUnit.DAYS).plus(2, ChronoUnit.HOURS))
                .build();

        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(existing));

        Instant newStart = Instant.now().plus(3, ChronoUnit.DAYS);
        Instant newEnd = newStart.plus(2, ChronoUnit.HOURS);

        UpdateScheduleRequest updateReq = UpdateScheduleRequest.builder()
                .title("Tiêu đề mới đã đổi")
                .startTime(newStart)
                .endTime(newEnd)
                .build();

        when(scheduleRepository.save(existing)).thenReturn(existing);

        ScheduleResponse updatedResponse = ScheduleResponse.builder()
                .id(SCHEDULE_ID)
                .title("Tiêu đề mới đã đổi")
                .startTime(newStart)
                .endTime(newEnd)
                .build();

        when(scheduleMapper.toScheduleResponse(existing)).thenReturn(updatedResponse);

        ScheduleResponse result = scheduleService.updateSchedule(COURSE_ID, SCHEDULE_ID, updateReq);

        assertThat(result.getTitle()).isEqualTo("Tiêu đề mới đã đổi");
        verify(scheduleMapper).updateSchedule(existing, updateReq);
        verify(scheduleRepository).save(existing);
    }

    @Test
    @DisplayName("7. deleteScheduleOwnerSuccess: Xóa lịch học thành công khỏi CSDL")
    void deleteScheduleOwnerSuccess() {
        when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));

        Schedule existing = Schedule.builder()
                .id(SCHEDULE_ID)
                .course(course)
                .build();

        when(scheduleRepository.findById(SCHEDULE_ID)).thenReturn(Optional.of(existing));

        scheduleService.deleteSchedule(COURSE_ID, SCHEDULE_ID);

        verify(scheduleRepository).delete(existing);
    }
}
