package com.lms.courseservice.service.impl;

import com.lms.courseservice.configuration.AssignmentClient;
import com.lms.courseservice.configuration.GatewayAuthentication;
import com.lms.courseservice.configuration.IdentityClient;
import com.lms.courseservice.dto.MeetingInfo;
import com.lms.courseservice.dto.request.CreateCourseRequest;
import com.lms.courseservice.dto.request.InviteStudentRequest;
import com.lms.courseservice.dto.request.LockCourseRequest;
import com.lms.courseservice.dto.request.UpdateCourseRequest;
import com.lms.courseservice.dto.response.*;
import com.lms.courseservice.entity.mongo.CourseStructure;
import com.lms.courseservice.entity.mysql.Announcement;
import com.lms.courseservice.entity.mysql.Category;
import com.lms.courseservice.entity.mysql.Course;
import com.lms.courseservice.entity.mysql.Enrollment;
import com.lms.courseservice.enums.CourseLevel;
import com.lms.courseservice.enums.CourseStatus;
import com.lms.courseservice.enums.EnrollmentStatus;
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
import com.lms.courseservice.service.CourseService;
import com.lms.courseservice.service.GoogleMeetService;
import com.lms.courseservice.service.MinioService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CourseServiceImpl implements CourseService {

    CourseRepository courseRepository;
    CategoryRepository categoryRepository;
    EnrollmentRepository enrollmentRepository;
    AnnouncementRepository announcementRepository;
    CourseStructureRepository courseStructureRepository;
    LessonRepository lessonRepository;
    CourseMapper courseMapper;
    EnrollmentMapper enrollmentMapper;
    MinioService minioService;
    IdentityClient identityClient;
    AssignmentClient assignmentClient;
    GoogleMeetService googleMeetService;

    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponse> getAllCourses(Pageable pageable, CourseStatus status, Long categoryId, String keyword, List<CourseLevel> levels) {
        var spec = CourseSpecifications.hasStatus(status)
                .and(CourseSpecifications.hasCategory(categoryId))
                .and(CourseSpecifications.hasLevelIn(levels))
                .and(CourseSpecifications.keywordMatches(keyword));

        Page<CourseResponse> page = courseRepository.findAll(spec, pageable).map(courseMapper::toCourseResponse);
        populateExerciseCounts(page.getContent());
        return page;
    }

    @Override
    @Transactional(readOnly = true)
    public CourseResponse getCourseById(String id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        CourseResponse response = courseMapper.toCourseResponse(course);
        try {
            var countResponse = assignmentClient.getExerciseCount(id);
            if (countResponse != null && countResponse.getData() != null) {
                response.setExerciseCount(countResponse.getData());
            }
        } catch (Exception e) {
            log.warn("Không lấy được exerciseCount cho course {}: {}", id, e.getMessage());
        }
        return response;
    }

    @Override
    @Transactional
    public CourseResponse createCourse(CreateCourseRequest request) {
        String instructorId = GatewayAuthentication.currentUserId();

        if (courseRepository.existsByTitle(request.getTitle())) {
            throw new CourseException(ErrorCode.COURSE_TITLE_EXISTED);
        }

        Course course = courseMapper.toCourse(request);
        course.setInstructorId(instructorId);
        if (course.getStatus() == null) {
            course.setStatus(CourseStatus.PRIVATE);
        }

        try {
            MeetingInfo meetingInfo = googleMeetService.createMeeting(request.getTitle());
            if (meetingInfo != null) {
                course.setMeetingUrl(meetingInfo.getMeetingUrl());
                course.setGoogleEventId(meetingInfo.getEventId());
            }
        } catch (Exception e) {
            log.warn("Không thể tạo Google Meet: {}", e.getMessage());
        }

        if (request.getThumbnail() != null && !request.getThumbnail().isEmpty()) {
            String thumbnailUrl = minioService.uploadFile(request.getThumbnail(), "thumbnails");
            course.setThumbnailUrl(thumbnailUrl);
        }

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
            course.setCategories(categories);
        }

        if (course.getId() == null) {
            course.setId(UUID.randomUUID().toString());
        }
        course.setMongoStructureId(UUID.randomUUID().toString());
        Course savedCourse = courseRepository.save(course);

        // Tạo CourseStructure trong MongoDB
        CourseStructure structure = CourseStructure.builder()
                .id(savedCourse.getMongoStructureId())
                .courseId(savedCourse.getId())
                .nodes(new ArrayList<>())
                .updatedAt(LocalDateTime.now())
                .build();
        try {
            courseStructureRepository.save(structure);
        } catch (Exception e) {
            log.warn("Lỗi lưu CourseStructure vào MongoDB: {}", e.getMessage());
        }

        return courseMapper.toCourseResponse(savedCourse);
    }

    @Override
    @Transactional
    public CourseResponse updateCourse(String id, UpdateCourseRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        checkCourseOwner(course);
        checkNotLocked(course);

        if (!course.getTitle().equals(request.getTitle())
                && courseRepository.existsByTitle(request.getTitle())) {
            throw new CourseException(ErrorCode.COURSE_TITLE_EXISTED);
        }

        if (request.getThumbnail() != null && !request.getThumbnail().isEmpty()) {
            if (course.getThumbnailUrl() != null) {
                minioService.deleteFile(course.getThumbnailUrl());
            }
            String thumbnailUrl = minioService.uploadFile(request.getThumbnail(), "thumbnails");
            course.setThumbnailUrl(thumbnailUrl);
        }

        if (request.getCategoryIds() != null) {
            List<Category> categories = categoryRepository.findAllById(request.getCategoryIds());
            course.setCategories(categories);
        }

        courseMapper.updateCourse(course, request);
        return courseMapper.toCourseResponse(courseRepository.save(course));
    }

    @Override
    @Transactional
    public void deleteCourse(String id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        checkCourseOwner(course);
        checkNotLocked(course);

        if (course.getGoogleEventId() != null) {
            try {
                googleMeetService.deleteMeeting(course.getGoogleEventId());
            } catch (Exception e) {
                log.warn("Không thể xóa Google Calendar event: {}", e.getMessage());
            }
        }

        if (course.getThumbnailUrl() != null) {
            minioService.deleteFile(course.getThumbnailUrl());
        }

        try {
            lessonRepository.deleteAllByCourseId(id);
            if (course.getMongoStructureId() != null) {
                courseStructureRepository.deleteById(course.getMongoStructureId());
            }
        } catch (Exception e) {
            log.warn("Lỗi xóa dữ liệu MongoDB của khóa học {}: {}", id, e.getMessage());
        }

        enrollmentRepository.deleteAllByCourse(course);
        courseRepository.delete(course);
    }

    @Override
    @Transactional
    public EnrollmentResponse enrollCourse(String courseId) {
        String userId = GatewayAuthentication.currentUserId();

        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        if (course.getStatus() == CourseStatus.LOCKED) {
            throw new CourseException(ErrorCode.COURSE_LOCKED);
        }

        if (course.getStatus() != CourseStatus.PUBLIC) {
            throw new CourseException(ErrorCode.COURSE_NOT_PUBLIC);
        }

        if (enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new CourseException(ErrorCode.ALREADY_ENROLLED);
        }

        Enrollment enrollment = Enrollment.builder()
                .userId(userId)
                .course(course)
                .status(EnrollmentStatus.ACTIVE)
                .build();

        return enrollmentMapper.toEnrollmentResponse(enrollmentRepository.save(enrollment));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseResponse> getMyCourses(Pageable pageable) {
        String instructorId = GatewayAuthentication.currentUserId();
        Page<CourseResponse> page = courseRepository.findByInstructorId(instructorId, pageable)
                .map(courseMapper::toCourseResponse);
        populateExerciseCounts(page.getContent());
        return page;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseResponse> getEnrolledCourses() {
        String userId = GatewayAuthentication.currentUserId();
        List<CourseResponse> courses = enrollmentRepository.findByUserId(userId).stream()
                .map(enrollment -> courseMapper.toCourseResponse(enrollment.getCourse()))
                .collect(Collectors.toList());
        populateExerciseCounts(courses);
        return courses;
    }

    @Override
    @Transactional
    public CourseResponse lockCourse(String courseId, LockCourseRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        if (course.getStatus() == CourseStatus.LOCKED) {
            throw new CourseException(ErrorCode.COURSE_ALREADY_LOCKED);
        }

        course.setStatus(CourseStatus.LOCKED);
        courseRepository.save(course);

        announcementRepository.save(Announcement.builder()
                .course(course)
                .title("[Hệ thống] Khóa học đã bị khóa")
                .content("Khóa học này đã bị Admin khóa.\n\nLý do: " + request.getReason())
                .build());

        return courseMapper.toCourseResponse(course);
    }

    @Override
    @Transactional
    public CourseResponse unlockCourse(String courseId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));

        if (course.getStatus() != CourseStatus.LOCKED) {
            throw new CourseException(ErrorCode.COURSE_NOT_LOCKED);
        }

        course.setStatus(CourseStatus.PRIVATE);
        return courseMapper.toCourseResponse(courseRepository.save(course));
    }

    @Override
    @Transactional(readOnly = true)
    public InstructorStatsResponse getMyStats() {
        String instructorId = GatewayAuthentication.currentUserId();

        long totalCourses = courseRepository.countByInstructorId(instructorId);
        long totalStudents = enrollmentRepository.countTotalStudentsByInstructorId(instructorId);
        long totalExercises = 0;

        try {
            List<String> courseIds = courseRepository.findIdsByInstructorId(instructorId);
            if (!courseIds.isEmpty()) {
                var response = assignmentClient.getExerciseCountBatch(courseIds);
                if (response != null && response.getData() != null) {
                    totalExercises = response.getData().values().stream().mapToLong(Long::longValue).sum();
                }
            }
        } catch (Exception e) {
            log.warn("Không lấy được exerciseCount batch cho instructor {}: {}", instructorId, e.getMessage());
        }

        return InstructorStatsResponse.builder()
                .totalCourses(totalCourses)
                .totalStudents(totalStudents)
                .totalExercises(totalExercises)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecentEnrollmentResponse> getRecentEnrollments(int limit) {
        String instructorId = GatewayAuthentication.currentUserId();
        return enrollmentRepository
                .findRecentByInstructorId(instructorId, PageRequest.of(0, limit))
                .stream()
                .map(e -> {
                    RecentEnrollmentResponse.RecentEnrollmentResponseBuilder builder =
                            RecentEnrollmentResponse.builder()
                                    .courseId(e.getCourse().getId())
                                    .courseTitle(e.getCourse().getTitle())
                                    .courseThumbnailUrl(e.getCourse().getThumbnailUrl())
                                    .userId(e.getUserId())
                                    .enrolledAt(e.getEnrolledAt());
                    try {
                        var userInfo = identityClient.getUserById(e.getUserId());
                        if (userInfo != null) {
                            builder.fullname(userInfo.getFullname());
                            builder.avatarUrl(userInfo.getAvatarUrl());
                        }
                    } catch (Exception ex) {
                        log.warn("Không lấy được thông tin user {}: {}", e.getUserId(), ex.getMessage());
                    }
                    return builder.build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public long countStudents(String courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new CourseException(ErrorCode.COURSE_NOT_FOUND);
        }
        return enrollmentRepository.countByCourseId(courseId);
    }

    @Override
    public long countLessons(String courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new CourseException(ErrorCode.COURSE_NOT_FOUND);
        }
        return lessonRepository.countByCourseId(courseId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CourseStudentResponse> getCourseStudents(String courseId, Pageable pageable) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));
        checkCourseOwner(course);

        return enrollmentRepository.findByCourseId(courseId, pageable)
                .map(enrollment -> {
                    CourseStudentResponse.CourseStudentResponseBuilder builder = CourseStudentResponse.builder()
                            .userId(enrollment.getUserId())
                            .enrolledAt(enrollment.getEnrolledAt())
                            .enrollmentStatus(enrollment.getStatus().name());
                    try {
                        var userInfo = identityClient.getUserById(enrollment.getUserId());
                        if (userInfo != null) {
                            builder.username(userInfo.getUsername());
                            builder.fullname(userInfo.getFullname());
                            builder.email(userInfo.getEmail());
                            builder.avatarUrl(userInfo.getAvatarUrl());
                        }
                    } catch (Exception e) {
                        log.warn("Không lấy được thông tin user {}: {}", enrollment.getUserId(), e.getMessage());
                    }
                    return builder.build();
                });
    }

    @Override
    @Transactional
    public void removeStudentFromCourse(String courseId, String userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));
        checkCourseOwner(course);
        if (!enrollmentRepository.existsByUserIdAndCourseId(userId, courseId)) {
            throw new CourseException(ErrorCode.NOT_ENROLLED);
        }
        enrollmentRepository.deleteByUserIdAndCourseId(userId, courseId);
    }

    @Override
    public UserInfoDto searchUserByEmail(String email) {
        try {
            return identityClient.findByEmail(email);
        } catch (Exception e) {
            log.warn("Không tìm thấy user với email {}: {}", email, e.getMessage());
            return null;
        }
    }

    @Override
    public InviteResultResponse inviteStudents(String courseId, InviteStudentRequest request) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseException(ErrorCode.COURSE_NOT_FOUND));
        checkCourseOwner(course);
        checkNotLocked(course);

        List<String> success = new ArrayList<>();
        List<String> already = new ArrayList<>();
        List<String> notFound = new ArrayList<>();

        for (String email : request.getEmails()) {
            UserInfoDto user = searchUserByEmail(email);
            if (user == null) {
                notFound.add(email);
                continue;
            }
            if (enrollmentRepository.existsByUserIdAndCourseId(user.getId(), courseId)) {
                already.add(email);
                continue;
            }
            enrollmentRepository.save(Enrollment.builder()
                    .userId(user.getId())
                    .course(course)
                    .status(EnrollmentStatus.ACTIVE)
                    .build());
            success.add(email);
        }

        return InviteResultResponse.builder()
                .successEmails(success)
                .alreadyEnrolledEmails(already)
                .notFoundEmails(notFound)
                .build();
    }

    private void checkCourseOwner(Course course) {
        String currentUserId = GatewayAuthentication.currentUserId();
        String currentRole = GatewayAuthentication.currentRole();

        if ("ROLE_ADMIN".equals(currentRole) || "ADMIN".equals(currentRole)) return;

        if (!course.getInstructorId().equals(currentUserId)) {
            throw new CourseException(ErrorCode.NOT_COURSE_OWNER);
        }
    }

    private void checkNotLocked(Course course) {
        if (CourseStatus.LOCKED.equals(course.getStatus())) {
            throw new CourseException(ErrorCode.COURSE_LOCKED);
        }
    }

    private void populateExerciseCounts(List<CourseResponse> courses) {
        if (courses == null || courses.isEmpty()) return;
        try {
            List<String> ids = courses.stream().map(CourseResponse::getId).collect(Collectors.toList());
            var countResponse = assignmentClient.getExerciseCountBatch(ids);
            if (countResponse != null && countResponse.getData() != null) {
                Map<String, Long> counts = countResponse.getData();
                courses.forEach(c -> c.setExerciseCount(counts.getOrDefault(c.getId(), 0L)));
            }
        } catch (Exception e) {
            log.warn("Không lấy được exerciseCount batch: {}", e.getMessage());
        }
    }
}
