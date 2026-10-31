package com.lms.webportal.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.lms.webportal.client.GatewayClient;
import com.lms.webportal.dto.ChapterDto;
import com.lms.webportal.dto.CourseDto;
import com.lms.webportal.dto.CourseFilterDto;
import com.lms.webportal.dto.LessonDto;
import com.lms.webportal.dto.UserSessionDto;
import com.lms.webportal.service.CourseService;
import com.lms.webportal.service.PayosService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
public class CourseWebController {

    @Autowired
    private CourseService courseService;

    @Autowired
    private GatewayClient gatewayClient;

    @Autowired
    private PayosService payosService;

    @GetMapping("/courses")
    public String listCourses(@ModelAttribute("filter") CourseFilterDto filter,
                              Model model) {
        List<CourseDto> courses = courseService.getCourses(filter);
        List<String> categories = courseService.getAllCategories();

        model.addAttribute("courses", courses);
        model.addAttribute("categories", categories);
        model.addAttribute("pageTitle", "Khám Phá Khóa Học - EduLearn LMS");
        return "courses/list";
    }

    @GetMapping("/courses/{id}")
    public String showCourseDetail(@PathVariable("id") String id,
                                   @RequestParam(value = "enrolled", required = false) Boolean enrolled,
                                   HttpSession session,
                                   Model model) {
        CourseDto course = courseService.getCourseById(id);
        if (course == null) {
            return "redirect:/courses";
        }

        if (Boolean.TRUE.equals(enrolled)) {
            course.setEnrolled(true);
            model.addAttribute("success", "Chúc mừng bạn đã đăng ký tham gia khóa học thành công!");
        } else if (session != null && session.getAttribute("currentUser") != null) {
            if (courseService.isUserEnrolled(id)) {
                course.setEnrolled(true);
            }
        }

        model.addAttribute("course", course);
        model.addAttribute("pageTitle", course.getTitle() + " - EduLearn LMS");
        return "courses/detail";
    }

    @PostMapping("/courses/{id}/enroll")
    public String enrollCourse(@PathVariable("id") String id,
                               HttpServletRequest request,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        UserSessionDto user = (UserSessionDto) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/auth/login?redirectUrl=/courses/" + id;
        }

        CourseDto course = courseService.getCourseById(id);
        if (course == null) {
            return "redirect:/courses";
        }

        // Nếu khóa học có học phí (> 0) -> Chuyển hướng sang PayOS VietQR
        if (course.getPrice() > 0) {
            long orderCode = System.currentTimeMillis() % 1000000000L;
            long amount = Math.round(course.getPrice());

            String scheme = request.getScheme();
            String serverName = request.getServerName();
            int serverPort = request.getServerPort();
            String host = scheme + "://" + serverName + (serverPort == 80 || serverPort == 443 ? "" : ":" + serverPort);

            String returnUrl = host + "/payment/payos/success?courseId=" + id + "&orderCode=" + orderCode;
            String cancelUrl = host + "/payment/payos/cancel?courseId=" + id;

            String courseTitle = course.getTitle() != null ? course.getTitle() : "EduLearn";
            String desc = "HP " + (courseTitle.length() > 20 ? courseTitle.substring(0, 20) : courseTitle);

            PayosService.PaymentLinkResult linkResult = payosService.createPaymentLink(orderCode, amount, desc, cancelUrl, returnUrl);
            if (linkResult != null && linkResult.getCheckoutUrl() != null) {
                session.setAttribute("PENDING_PAYOS_ORDER_" + orderCode, id);
                return "redirect:" + linkResult.getCheckoutUrl();
            } else {
                redirectAttributes.addFlashAttribute("errorMessage", "Không thể khởi tạo cổng thanh toán PayOS. Vui lòng thử lại!");
                return "redirect:/courses/" + id;
            }
        }

        // Khóa học miễn phí -> Ghi danh ngay lập tức
        courseService.enroll(id, user.getUserId());
        return "redirect:/courses/" + id + "?enrolled=true";
    }

    @GetMapping("/payment/payos/success")
    public String payosSuccess(@RequestParam("courseId") String courseId,
                               @RequestParam(value = "orderCode", required = false) Long orderCode,
                               @RequestParam(value = "status", required = false) String returnStatus,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        UserSessionDto user = (UserSessionDto) session.getAttribute("currentUser");
        if (user == null) {
            return "redirect:/auth/login?redirectUrl=/courses/" + courseId;
        }

        boolean isPaid = false;
        if (orderCode != null) {
            String payosStatus = payosService.getPaymentStatus(orderCode);
            log.info("PayOS callback kiểm tra orderCode {}: status={}", orderCode, payosStatus);
            if ("PAID".equalsIgnoreCase(payosStatus) || "PAID".equalsIgnoreCase(returnStatus)) {
                isPaid = true;
            }
        } else if ("PAID".equalsIgnoreCase(returnStatus)) {
            isPaid = true;
        }

        if (isPaid) {
            try {
                courseService.enroll(courseId, user.getUserId());
                if (orderCode != null) {
                    session.removeAttribute("PENDING_PAYOS_ORDER_" + orderCode);
                }
                redirectAttributes.addFlashAttribute("successMessage", "Thanh toán học phí thành công qua PayOS VietQR! Bạn đã được ghi danh vào khóa học.");
                return "redirect:/courses/" + courseId + "?enrolled=true&paymentSuccess=true";
            } catch (Exception e) {
                log.error("Lỗi khi ghi danh sau thanh toán: {}", e.getMessage());
                redirectAttributes.addFlashAttribute("errorMessage", "Thanh toán thành công nhưng có lỗi kích hoạt: " + e.getMessage());
                return "redirect:/courses/" + courseId;
            }
        } else {
            log.warn("Giao dịch PayOS chưa hoàn tất hoặc thất bại. orderCode={}", orderCode);
            redirectAttributes.addFlashAttribute("errorMessage", "Giao dịch thanh toán chưa hoàn tất hoặc không hợp lệ.");
            return "redirect:/courses/" + courseId;
        }
    }

    @GetMapping("/payment/payos/cancel")
    public String payosCancel(@RequestParam("courseId") String courseId,
                              RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("warningMessage", "Bạn đã hủy giao dịch thanh toán PayOS.");
        return "redirect:/courses/" + courseId;
    }

    @GetMapping("/courses/{id}/lessons/{lessonId}")
    public String viewLesson(@PathVariable("id") String id,
                             @PathVariable("lessonId") String lessonId,
                             HttpSession session,
                             Model model) {
        CourseDto course = courseService.getCourseById(id);
        if (course == null) {
            return "redirect:/courses";
        }

        LessonDto currentLesson = courseService.getLessonById(id, lessonId);
        if (currentLesson == null) {
            // Lấy bài học đầu tiên nếu không tìm thấy bài chỉ định
            if (course.getChapters() != null && !course.getChapters().isEmpty()) {
                ChapterDto firstChapter = course.getChapters().get(0);
                if (firstChapter.getLessons() != null && !firstChapter.getLessons().isEmpty()) {
                    currentLesson = firstChapter.getLessons().get(0);
                }
            }
        }

        if (currentLesson == null) {
            return "redirect:/courses/" + id;
        }

        // Tìm bài học trước và sau để hỗ trợ điều hướng
        List<LessonDto> flatLessons = new ArrayList<>();
        if (course.getChapters() != null) {
            for (ChapterDto chapter : course.getChapters()) {
                if (chapter.getLessons() != null) {
                    flatLessons.addAll(chapter.getLessons());
                }
            }
        }

        LessonDto prevLesson = null;
        LessonDto nextLesson = null;
        for (int i = 0; i < flatLessons.size(); i++) {
            if (flatLessons.get(i).getId().equals(currentLesson.getId())) {
                if (i > 0) prevLesson = flatLessons.get(i - 1);
                if (i < flatLessons.size() - 1) nextLesson = flatLessons.get(i + 1);
                break;
            }
        }

        // Thống kê tiến độ học tập cho học viên đã đăng nhập
        int totalLessonCount = flatLessons.size();
        long completedLessonCount = 0;
        int percentComplete = 0;
        List<String> completedLessonIds = new ArrayList<>();
        boolean isCurrentLessonCompleted = false;

        if (session != null && session.getAttribute("currentUser") != null) {
            try {
                JsonNode progressRes = gatewayClient.get("/course/courses/" + id + "/progress", JsonNode.class);
                if (progressRes != null && progressRes.has("data")) {
                    JsonNode progressData = progressRes.get("data");
                    if (progressData.has("totalLessons") && progressData.get("totalLessons").asInt() > 0) {
                        totalLessonCount = progressData.get("totalLessons").asInt();
                    }
                    completedLessonCount = progressData.path("completedLessons").asLong(0);
                    percentComplete = (int) Math.round(progressData.path("percentComplete").asDouble(0.0));
                    if (progressData.has("completedLessonIds") && progressData.get("completedLessonIds").isArray()) {
                        for (JsonNode n : progressData.get("completedLessonIds")) {
                            completedLessonIds.add(n.asText());
                        }
                    }
                    isCurrentLessonCompleted = completedLessonIds.contains(currentLesson.getId());
                }
            } catch (Exception e) {
                // If progress API fails or user not enrolled, keep defaults
            }
        }

        model.addAttribute("course", course);
        model.addAttribute("currentLesson", currentLesson);
        model.addAttribute("prevLesson", prevLesson);
        model.addAttribute("nextLesson", nextLesson);
        model.addAttribute("totalLessonCount", totalLessonCount);
        model.addAttribute("completedLessonCount", completedLessonCount);
        model.addAttribute("percentComplete", percentComplete);
        model.addAttribute("completedLessonIds", completedLessonIds);
        model.addAttribute("isCurrentLessonCompleted", isCurrentLessonCompleted);
        model.addAttribute("pageTitle", currentLesson.getTitle() + " - " + course.getTitle());
        return "courses/lesson";
    }

    @PostMapping("/courses/{courseId}/lessons/{lessonId}/complete")
    @ResponseBody
    public ResponseEntity<?> completeLesson(@PathVariable("courseId") String courseId,
                                            @PathVariable("lessonId") String lessonId,
                                            HttpSession session) {
        if (session == null || session.getAttribute("currentUser") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Vui lòng đăng nhập để lưu tiến độ học tập."));
        }
        try {
            gatewayClient.post("/course/courses/" + courseId + "/lessons/" + lessonId + "/complete",
                    Map.of("lessonType", "VIDEO"), JsonNode.class);

            JsonNode progressRes = gatewayClient.get("/course/courses/" + courseId + "/progress", JsonNode.class);
            return ResponseEntity.ok(progressRes != null ? progressRes.get("data") : Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Lỗi cập nhật tiến độ: " + e.getMessage()));
        }
    }

    @GetMapping("/courses/{courseId}/announcements")
    @ResponseBody
    public ResponseEntity<?> getCourseAnnouncements(@PathVariable("courseId") String courseId,
                                                    @RequestParam(value = "page", defaultValue = "0") int page,
                                                    @RequestParam(value = "size", defaultValue = "10") int size) {
        try {
            JsonNode res = gatewayClient.get("/course/courses/" + courseId + "/announcements?page=" + page + "&size=" + size, JsonNode.class);
            return ResponseEntity.ok(res != null ? res : Map.of("code", 1000));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("data", Map.of("content", List.of(), "totalPages", 1)));
        }
    }

    @GetMapping("/courses/{courseId}/schedules")
    @ResponseBody
    public ResponseEntity<?> getCourseSchedules(@PathVariable("courseId") String courseId) {
        try {
            JsonNode res = gatewayClient.get("/course/courses/" + courseId + "/schedules", JsonNode.class);
            return ResponseEntity.ok(res != null ? res : Map.of("code", 1000));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("data", List.of()));
        }
    }
}
