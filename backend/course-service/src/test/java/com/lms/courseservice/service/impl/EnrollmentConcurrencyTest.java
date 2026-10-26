package com.lms.courseservice.service.impl;

import com.lms.courseservice.configuration.GatewayAuthentication;
import com.lms.courseservice.enums.CourseLevel;
import com.lms.courseservice.enums.CourseStatus;
import com.lms.courseservice.entity.mysql.Course;
import com.lms.courseservice.entity.mysql.Enrollment;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.repository.mongo.CourseStructureRepository;
import com.lms.courseservice.repository.mongo.LessonRepository;
import com.lms.courseservice.repository.mysql.CourseRepository;
import com.lms.courseservice.repository.mysql.EnrollmentRepository;
import com.lms.courseservice.service.CourseService;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Concurrency Test cho tính năng ghi danh khóa học (Enrollment).
 *
 * <p>Mục tiêu: Đảm bảo khi có nhiều luồng cùng gửi yêu cầu ghi danh đồng thời cho cùng một người dùng
 * và một khóa học, hệ thống chỉ tạo duy nhất 1 bản ghi Enrollment và không xảy ra tình trạng duplicate.
 *
 * <p>Lưu ý kỹ thuật quan trọng:
 * Kiểm thử in-memory với H2 Database nhằm xác nhận tính toàn vẹn của ràng buộc unique constraint
 * ('uq_enrollments_user_course' trên bảng enrollments) và logic kiểm tra tầng ứng dụng.
 * Việc kiểm thử giao dịch ACID, mức cô lập (REPEATABLE READ) và row-level locking thực tế
 * sẽ được kiểm chứng toàn diện trên môi trường MySQL trong Docker Compose.
 */
@SpringBootTest
@ActiveProfiles("test")
class EnrollmentConcurrencyTest {

    @Autowired
    private CourseService courseService;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @MockBean
    private CourseStructureRepository courseStructureRepository;

    @MockBean
    private LessonRepository lessonRepository;

    @MockBean(name = "minioClient")
    private MinioClient minioClient;

    @MockBean(name = "publicMinioClient")
    private MinioClient publicMinioClient;

    private String courseId;
    private final String userId = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();

        Course course = Course.builder()
                .title("Khóa học Lập trình Concurrency")
                .description("Kiểm thử ghi danh đa luồng đồng thời")
                .instructorId(UUID.randomUUID().toString())
                .status(CourseStatus.PUBLIC)
                .level(CourseLevel.INTERMEDIATE)
                .mongoStructureId(UUID.randomUUID().toString())
                .learningPoints(new ArrayList<>(List.of("Xử lý race condition", "Ràng buộc Unique")))
                .build();

        Course savedCourse = courseRepository.save(course);
        courseId = savedCourse.getId();
    }

    @AfterEach
    void tearDown() {
        enrollmentRepository.deleteAll();
        courseRepository.deleteAll();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("29. concurrentEnrollmentRequestsDoNotCreateDuplicateRecords: 10 luồng đồng thời ghi danh cùng 1 user -> Chỉ 1 bản ghi được tạo, không sinh duplicate")
    void concurrentEnrollmentRequestsDoNotCreateDuplicateRecords() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger rejectedCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                // Thiết lập Security Context riêng cho từng luồng
                GatewayAuthentication auth = new GatewayAuthentication(
                        userId,
                        "student@test.com",
                        "STUDENT",
                        List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
                );
                SecurityContextHolder.getContext().setAuthentication(auth);

                readyLatch.countDown();
                try {
                    // Chờ tín hiệu xuất phát để tất cả các luồng cùng thực hiện một lúc
                    startLatch.await();
                    courseService.enrollCourse(courseId);
                    successCount.incrementAndGet();
                } catch (CourseException e) {
                    // Bị chặn bởi logic ứng dụng (ALREADY_ENROLLED)
                    rejectedCount.incrementAndGet();
                } catch (DataIntegrityViolationException e) {
                    // Bị chặn bởi unique constraint tại database
                    rejectedCount.incrementAndGet();
                } catch (Exception e) {
                    // Các ngoại lệ khác
                    rejectedCount.incrementAndGet();
                } finally {
                    SecurityContextHolder.clearContext();
                    finishLatch.countDown();
                }
            });
        }

        // Chờ tất cả luồng sẵn sàng
        boolean allReady = readyLatch.await(5, TimeUnit.SECONDS);
        assertThat(allReady).isTrue();

        // Phát tín hiệu cho các luồng cùng chạy
        startLatch.countDown();

        // Chờ tất cả luồng hoàn thành
        boolean allFinished = finishLatch.await(10, TimeUnit.SECONDS);
        assertThat(allFinished).isTrue();
        executorService.shutdown();

        // 1. Kiểm tra số lần thành công và bị từ chối
        assertThat(successCount.get())
                .as("Chỉ duy nhất 1 luồng được phép ghi danh thành công")
                .isEqualTo(1);
        assertThat(rejectedCount.get())
                .as("Các luồng còn lại phải bị từ chối sạch sẽ")
                .isEqualTo(threadCount - 1);

        // 2. Kiểm tra dữ liệu trong database: duy nhất 1 bản ghi
        List<Enrollment> enrollments = enrollmentRepository.findAll();
        assertThat(enrollments)
                .as("Database chỉ được chứa đúng 1 bản ghi Enrollment cho user và course này")
                .hasSize(1);

        Enrollment singleEnrollment = enrollments.getFirst();
        assertThat(singleEnrollment.getUserId()).isEqualTo(userId);
        assertThat(singleEnrollment.getCourse().getId()).isEqualTo(courseId);
    }
}
