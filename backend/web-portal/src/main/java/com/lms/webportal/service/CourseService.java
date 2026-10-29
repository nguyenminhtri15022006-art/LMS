package com.lms.webportal.service;

import com.lms.webportal.client.CourseClient;
import com.lms.webportal.dto.ApiResponse;
import com.lms.webportal.dto.ChapterDto;
import com.lms.webportal.dto.CourseDto;
import com.lms.webportal.dto.CourseFilterDto;
import com.lms.webportal.dto.LessonDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private static final Logger log = LoggerFactory.getLogger(CourseService.class);

    @Autowired(required = false)
    private CourseClient courseClient;

    private final Map<String, CourseDto> courseRepository = new ConcurrentHashMap<>();
    private final Set<String> enrolledCourses = ConcurrentHashMap.newKeySet();

    public CourseService() {
        initDefaultCourses();
    }

    private void initDefaultCourses() {
        // Course 1: Spring Boot Microservices
        LessonDto l1 = LessonDto.builder()
                .id("les-101")
                .courseId("crs-01")
                .chapterId("ch-01")
                .title("Tổng quan kiến trúc Monolith vs Microservices")
                .orderIndex(1)
                .type("VIDEO")
                .durationMinutes(25)
                .videoUrl("https://www.youtube.com/embed/dQw4w9WgXcQ")
                .content("### 1. Khái niệm kiến trúc Microservices\nTrong bài này, chúng ta phân tích sự khác nhau giữa kiến trúc nguyên khối (Monolith) và hệ thống phân tán (Microservices).\n\n- **Ưu điểm**: Độc lập triển khai, khả năng mở rộng linh hoạt từng dịch vụ, phân chia đội ngũ rõ ràng.\n- **Thách thức**: Quản lý giao dịch phân tán (Distributed Transactions), Service Discovery, API Gateway và giám sát hệ thống.")
                .isPreview(true)
                .build();

        LessonDto l2 = LessonDto.builder()
                .id("les-102")
                .courseId("crs-01")
                .chapterId("ch-01")
                .title("Thiết lập Spring Cloud Eureka Service Registry")
                .orderIndex(2)
                .type("ARTICLE")
                .durationMinutes(30)
                .content("### Hướng dẫn cài đặt Eureka Server\nThêm dependency `spring-cloud-starter-netflix-eureka-server` vào tệp `pom.xml`:\n\n```xml\n<dependency>\n    <groupId>org.springframework.cloud</groupId>\n    <artifactId>spring-cloud-starter-netflix-eureka-server</artifactId>\n</dependency>\n```\n\nĐánh dấu `@EnableEurekaServer` trong class khởi chạy chính.")
                .isPreview(true)
                .build();

        LessonDto l3 = LessonDto.builder()
                .id("les-103")
                .courseId("crs-01")
                .chapterId("ch-02")
                .title("Cấu hình Spring Cloud Gateway & Token Offloading")
                .orderIndex(3)
                .type("CODE_LAB")
                .durationMinutes(45)
                .content("Thực hành viết `AuthFilter` trong Spring Cloud Gateway để xác thực JWT tập trung.")
                .build();

        ChapterDto ch1 = ChapterDto.builder()
                .id("ch-01")
                .title("Chương 1: Cơ sở Kiến trúc Phân tán")
                .orderIndex(1)
                .lessons(List.of(l1, l2))
                .build();

        ChapterDto ch2 = ChapterDto.builder()
                .id("ch-02")
                .title("Chương 2: API Gateway & Security")
                .orderIndex(2)
                .lessons(List.of(l3))
                .build();

        CourseDto c1 = CourseDto.builder()
                .id("crs-01")
                .title("Lập trình Web Nâng Cao & Spring Boot Microservices")
                .slug("spring-boot-microservices")
                .category("Web Development")
                .difficulty("INTERMEDIATE")
                .shortDescription("Làm chủ kiến trúc Microservices Monorepo: Spring Cloud, Docker, JWT, Redis và RabbitMQ.")
                .description("Khóa học chuyên sâu hướng dẫn sinh viên HCMUTE xây dựng hệ thống quản lý học tập quy mô lớn dựa trên Spring Boot 3, Spring Cloud Gateway, Eureka Server, MySQL và MongoDB.")
                .instructorName("ThS. Nguyễn Minh Trí")
                .instructorTitle("Giảng viên Khoa CNTT - HCMUTE")
                .thumbnailUrl("https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=600&auto=format&fit=crop&q=80")
                .totalLessons(18)
                .durationHours(36)
                .rating(4.9)
                .reviewsCount(142)
                .studentsEnrolled(850)
                .price(0.0)
                .tags(List.of("Java", "Spring Boot", "Microservices", "Docker", "DevOps"))
                .outcomes(List.of(
                        "Hiểu sâu nguyên lý phân rã hệ thống Microservices chuẩn công nghiệp",
                        "Xây dựng API Gateway, Eureka Registry, Token Offloading với JJWT",
                        "Triển khai cơ chế chịu lỗi Circuit Breaker với Resilience4j",
                        "Sử dụng Docker Compose điều phối cụm dịch vụ MySQL, Mongo, Redis"
                ))
                .chapters(List.of(ch1, ch2))
                .build();

        // Course 2: Data Structures & Algorithms
        LessonDto l21 = LessonDto.builder()
                .id("les-201")
                .courseId("crs-02")
                .chapterId("ch-21")
                .title("Kỹ thuật Hai con trỏ (Two Pointers) & Cửa sổ trượt (Sliding Window)")
                .orderIndex(1)
                .type("VIDEO")
                .durationMinutes(40)
                .content("Phân tích độ phức tạp thời gian O(N) và bộ nhớ O(1) khi áp dụng Two Pointers trên mảng đã sắp xếp.")
                .isPreview(true)
                .build();

        ChapterDto ch21 = ChapterDto.builder()
                .id("ch-21")
                .title("Chương 1: Mảng và Chuỗi Ký Tự Nâng Cao")
                .orderIndex(1)
                .lessons(List.of(l21))
                .build();

        CourseDto c2 = CourseDto.builder()
                .id("crs-02")
                .title("Cấu trúc Dữ liệu & Giải thuật cho Phỏng vấn Lập trình")
                .slug("dsa-coding-interview")
                .category("Algorithms")
                .difficulty("ADVANCED")
                .shortDescription("Rèn luyện tư duy thuật toán, giải bài tập LeetCode và chuẩn bị cho các kỳ thi lập trình ICPC.")
                .description("Hệ thống hóa toàn diện các chuyên đề: Dynamic Programming, Graph Algorithms, Trie, Segment Tree và Union-Find.")
                .instructorName("TS. Lê Hoàng Cường")
                .instructorTitle("Trưởng Bộ môn Kỹ thuật Dữ liệu")
                .thumbnailUrl("https://images.unsplash.com/photo-1555066931-4365d14bab8c?w=600&auto=format&fit=crop&q=80")
                .totalLessons(24)
                .durationHours(48)
                .rating(4.8)
                .reviewsCount(98)
                .studentsEnrolled(620)
                .price(0.0)
                .tags(List.of("Algorithms", "C++", "Java", "DSA", "LeetCode"))
                .outcomes(List.of(
                        "Nắm vững các mẫu bài toán thuật toán phổ biến nhất trong phỏng vấn",
                        "Tối ưu hóa độ phức tạp thời gian và không gian bộ nhớ",
                        "Tự tin giải quyết các bài toán trên hệ thống chấm Judge0"
                ))
                .chapters(List.of(ch21))
                .build();

        // Course 3: Modern UI with Thymeleaf & Bootstrap
        CourseDto c3 = CourseDto.builder()
                .id("crs-03")
                .title("Thiết Kế Web Hiện Đại Với Thymeleaf & Bootstrap 5")
                .slug("thymeleaf-bootstrap-web-design")
                .category("Frontend")
                .difficulty("BEGINNER")
                .shortDescription("Xây dựng giao diện web động chuẩn Server-Side Rendering (SSR) chuyên nghiệp, chuẩn responsive.")
                .description("Học cách tận dụng sức mạnh template engine Thymeleaf kết hợp Bootstrap 5 CSS Framework.")
                .instructorName("ThS. Nguyễn Minh Trí")
                .instructorTitle("Giảng viên Khoa CNTT - HCMUTE")
                .thumbnailUrl("https://images.unsplash.com/photo-1507238691740-187a5b1d37b8?w=600&auto=format&fit=crop&q=80")
                .totalLessons(12)
                .durationHours(20)
                .rating(4.7)
                .reviewsCount(65)
                .studentsEnrolled(410)
                .price(0.0)
                .tags(List.of("Thymeleaf", "Bootstrap 5", "HTML5", "CSS3", "JavaScript"))
                .outcomes(List.of(
                        "Làm chủ cú pháp Thymeleaf: th:text, th:each, th:if, th:replace",
                        "Thiết kế giao diện thích ứng responsive trên Mobile & Desktop",
                        "Tích hợp Ajax và xử lý form dữ liệu an toàn"
                ))
                .chapters(Collections.emptyList())
                .build();

        // Course 4: Cloud Native & Docker
        CourseDto c4 = CourseDto.builder()
                .id("crs-04")
                .title("Triển Khai Ứng Dụng Đám Mây & Docker Containerization")
                .slug("docker-cloud-native")
                .category("Cloud & DevOps")
                .difficulty("INTERMEDIATE")
                .shortDescription("Đóng gói ứng dụng Java, thiết lập Docker Compose mạng nội bộ và triển khai Kubernetes.")
                .description("Khóa học thực hành xây dựng môi trường production thực tế cho các dịch vụ phân tán.")
                .instructorName("ThS. Trần Quốc Bảo")
                .instructorTitle("Kỹ sư Cloud Solutions")
                .thumbnailUrl("https://images.unsplash.com/photo-1607799279861-4dd421887fb3?w=600&auto=format&fit=crop&q=80")
                .totalLessons(15)
                .durationHours(28)
                .rating(4.9)
                .reviewsCount(82)
                .studentsEnrolled(530)
                .price(0.0)
                .tags(List.of("Docker", "Kubernetes", "DevOps", "CI/CD"))
                .outcomes(List.of(
                        "Viết Dockerfile đa tầng tối ưu hóa dung lượng image",
                        "Điều phối cụm Multi-container với Docker Compose",
                        "Cấu hình CI/CD Pipeline tự động kiểm thử và build"
                ))
                .chapters(Collections.emptyList())
                .build();

        courseRepository.put(c1.getId(), c1);
        courseRepository.put(c2.getId(), c2);
        courseRepository.put(c3.getId(), c3);
        courseRepository.put(c4.getId(), c4);
    }

    public List<CourseDto> getCourses(CourseFilterDto filter) {
        // Thử lấy từ microservice nếu có kết nối
        if (courseClient != null) {
            try {
                ApiResponse<List<CourseDto>> response = courseClient.getCourses(
                        filter != null ? filter.getKeyword() : null,
                        filter != null ? filter.getCategory() : null,
                        filter != null ? filter.getDifficulty() : null
                );
                if (response != null && response.getResult() != null && !response.getResult().isEmpty()) {
                    return response.getResult();
                }
            } catch (Exception e) {
                log.warn("Không thể kết nối Course Service từ Gateway, dùng dữ liệu nội bộ: {}", e.getMessage());
            }
        }

        // Lọc nội bộ (mock/offline mode)
        return courseRepository.values().stream()
                .filter(c -> {
                    if (filter == null) return true;
                    if (filter.getKeyword() != null && !filter.getKeyword().isBlank()) {
                        String kw = filter.getKeyword().toLowerCase();
                        boolean matchTitle = c.getTitle().toLowerCase().contains(kw);
                        boolean matchDesc = c.getShortDescription() != null && c.getShortDescription().toLowerCase().contains(kw);
                        boolean matchTag = c.getTags().stream().anyMatch(t -> t.toLowerCase().contains(kw));
                        if (!matchTitle && !matchDesc && !matchTag) return false;
                    }
                    if (filter.getCategory() != null && !filter.getCategory().isBlank() && !"ALL".equalsIgnoreCase(filter.getCategory())) {
                        if (!filter.getCategory().equalsIgnoreCase(c.getCategory())) return false;
                    }
                    if (filter.getDifficulty() != null && !filter.getDifficulty().isBlank() && !"ALL".equalsIgnoreCase(filter.getDifficulty())) {
                        if (!filter.getDifficulty().equalsIgnoreCase(c.getDifficulty())) return false;
                    }
                    return true;
                })
                .map(c -> {
                    c.setEnrolled(enrolledCourses.contains(c.getId()));
                    return c;
                })
                .sorted(Comparator.comparing(CourseDto::getId))
                .collect(Collectors.toList());
    }

    public CourseDto getCourseById(String id) {
        if (courseClient != null) {
            try {
                ApiResponse<CourseDto> response = courseClient.getCourseById(id);
                if (response != null && response.getResult() != null) {
                    return response.getResult();
                }
            } catch (Exception e) {
                log.warn("Lỗi gọi Course Client getCourseById: {}", e.getMessage());
            }
        }

        CourseDto c = courseRepository.get(id);
        if (c != null) {
            c.setEnrolled(enrolledCourses.contains(c.getId()));
        }
        return c;
    }

    public LessonDto getLessonById(String courseId, String lessonId) {
        CourseDto course = getCourseById(courseId);
        if (course == null || course.getChapters() == null) {
            return null;
        }

        for (ChapterDto chapter : course.getChapters()) {
            if (chapter.getLessons() != null) {
                for (LessonDto lesson : chapter.getLessons()) {
                    if (lesson.getId().equals(lessonId)) {
                        return lesson;
                    }
                }
            }
        }
        return null;
    }

    public boolean enroll(String courseId, String userId) {
        if (courseRepository.containsKey(courseId)) {
            enrolledCourses.add(courseId);
            CourseDto c = courseRepository.get(courseId);
            c.setStudentsEnrolled(c.getStudentsEnrolled() + 1);
            return true;
        }
        return false;
    }

    public List<String> getAllCategories() {
        return List.of("Web Development", "Algorithms", "Frontend", "Cloud & DevOps");
    }
}
