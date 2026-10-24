package com.lms.webapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.webapp.client.GatewayClient;
import com.lms.webapp.service.MarkdownService;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Controller
@Slf4j
public class CourseStudentController {

    private final GatewayClient gatewayClient;
    private final MarkdownService markdownService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CourseStudentController(GatewayClient gatewayClient, MarkdownService markdownService) {
        this.gatewayClient = gatewayClient;
        this.markdownService = markdownService;
    }

    @GetMapping("/courses")
    public String catalogPage(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "level", required = false) String level,
            Model model) {

        List<JsonNode> categoriesList = new ArrayList<>();
        try {
            JsonNode catRes = gatewayClient.get("/course/categories", JsonNode.class);
            if (catRes != null && catRes.has("data") && catRes.get("data").isArray()) {
                for (JsonNode node : catRes.get("data")) {
                    categoriesList.add(node);
                }
            }
        } catch (Exception e) {
            log.warn("Không thể tải danh mục từ Gateway: {}", e.getMessage());
        }
        model.addAttribute("categories", categoriesList);

        StringBuilder uri = new StringBuilder("/course/courses?page=").append(page).append("&size=").append(size);
        if (categoryId != null) {
            uri.append("&categoryId=").append(categoryId);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            uri.append("&keyword=").append(URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8));
        }
        if (level != null && !level.trim().isEmpty()) {
            uri.append("&level=").append(level.trim());
        }

        List<JsonNode> courseList = new ArrayList<>();
        JsonNode pageData = null;
        try {
            JsonNode courseRes = gatewayClient.get(uri.toString(), JsonNode.class);
            if (courseRes != null && courseRes.has("data")) {
                pageData = courseRes.get("data");
                if (pageData.has("content") && pageData.get("content").isArray()) {
                    for (JsonNode c : pageData.get("content")) {
                        courseList.add(c);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Không thể tải danh sách khóa học: {}", e.getMessage());
        }

        model.addAttribute("courses", courseList);
        model.addAttribute("pageData", pageData);
        model.addAttribute("currentPage", page);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedLevel", level);

        return "courses/student/catalog";
    }

    @GetMapping("/courses/{id}")
    public String courseDetailPage(@PathVariable("id") String id, HttpSession session, Model model) {
        if ("my".equalsIgnoreCase(id)) {
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        JsonNode course = null;
        JsonNode structure = null;
        boolean isEnrolled = false;

        try {
            JsonNode courseRes = gatewayClient.get("/course/courses/" + id, JsonNode.class);
            if (courseRes != null && courseRes.has("data")) {
                course = courseRes.get("data");
            }
        } catch (Exception e) {
            log.error("Lỗi khi tải chi tiết khóa học {}: {}", id, e.getMessage());
        }

        try {
            JsonNode structRes = gatewayClient.get("/course/courses/" + id + "/structure", JsonNode.class);
            if (structRes != null && structRes.has("data")) {
                structure = structRes.get("data");
            }
        } catch (Exception e) {
            log.warn("Chưa có đề cương cho khóa học {}: {}", id, e.getMessage());
        }

        // Kiểm tra xem học viên hiện tại đã ghi danh chưa
        if (session.getAttribute("username") != null) {
            try {
                JsonNode enrolledRes = gatewayClient.get("/course/courses/enrolled", JsonNode.class);
                if (enrolledRes != null && enrolledRes.has("data") && enrolledRes.get("data").isArray()) {
                    for (JsonNode c : enrolledRes.get("data")) {
                        if (c.hasNonNull("id") && c.get("id").asText().equals(id)) {
                            isEnrolled = true;
                            break;
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        model.addAttribute("course", course);
        model.addAttribute("structure", structure);
        model.addAttribute("isEnrolled", isEnrolled);

        return "courses/student/detail";
    }

    @PostMapping("/courses/{id}/enroll")
    public String enrollCourse(@PathVariable("id") String id) {
        try {
            gatewayClient.post("/course/courses/" + id + "/enroll", Map.of(), JsonNode.class);
        } catch (Exception e) {
            log.warn("Ghi danh khóa học {}: {}", id, e.getMessage());
        }

        // Lấy bài học đầu tiên trong cây đề cương nếu có
        String firstLessonId = null;
        try {
            JsonNode structRes = gatewayClient.get("/course/courses/" + id + "/structure", JsonNode.class);
            if (structRes != null && structRes.has("data")) {
                JsonNode nodes = structRes.get("data").get("nodes");
                if (nodes != null && nodes.isArray()) {
                    for (JsonNode n : nodes) {
                        if (n.hasNonNull("lessonId") && !n.get("lessonId").asText().isBlank()) {
                            firstLessonId = n.get("lessonId").asText();
                            break;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (firstLessonId != null) {
            return "redirect:/learn/" + id + "/lessons/" + firstLessonId;
        }
        return "redirect:/learn/" + id;
    }

    @GetMapping("/my-courses")
    public String myCoursesPage(Model model) {
        List<JsonNode> enrolledList = new ArrayList<>();
        try {
            JsonNode enrolledRes = gatewayClient.get("/course/courses/enrolled", JsonNode.class);
            if (enrolledRes != null && enrolledRes.has("data") && enrolledRes.get("data").isArray()) {
                for (JsonNode c : enrolledRes.get("data")) {
                    enrolledList.add(c);
                }
            }
        } catch (Exception e) {
            log.error("Lỗi khi tải danh sách khóa học đã ghi danh: {}", e.getMessage());
        }

        model.addAttribute("courses", enrolledList);
        return "courses/student/my-courses";
    }

    @GetMapping("/learn/{courseId}")
    public String learningHubDefault(@PathVariable("courseId") String courseId) {
        // Tự động tìm bài học đầu tiên
        String firstLessonId = null;
        try {
            JsonNode structRes = gatewayClient.get("/course/courses/" + courseId + "/structure", JsonNode.class);
            if (structRes != null && structRes.has("data")) {
                JsonNode nodes = structRes.get("data").get("nodes");
                if (nodes != null && nodes.isArray()) {
                    for (JsonNode n : nodes) {
                        if (n.hasNonNull("lessonId") && !n.get("lessonId").asText().isBlank()) {
                            firstLessonId = n.get("lessonId").asText();
                            break;
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (firstLessonId != null) {
            return "redirect:/learn/" + courseId + "/lessons/" + firstLessonId;
        }

        return "redirect:/courses/" + courseId;
    }

    @GetMapping("/learn/{courseId}/lessons/{lessonId}")
    public String learningHub(
            @PathVariable("courseId") String courseId,
            @PathVariable("lessonId") String lessonId,
            Model model) {

        JsonNode course = null;
        JsonNode structure = null;
        JsonNode lesson = null;
        JsonNode progress = null;
        List<JsonNode> schedules = new ArrayList<>();
        List<JsonNode> announcements = new ArrayList<>();

        try {
            JsonNode cRes = gatewayClient.get("/course/courses/" + courseId, JsonNode.class);
            if (cRes != null && cRes.has("data")) {
                course = cRes.get("data");
            }
        } catch (Exception e) {
            log.warn("Không thể tải thông tin khóa học {}: {}", courseId, e.getMessage());
        }

        try {
            JsonNode sRes = gatewayClient.get("/course/courses/" + courseId + "/structure", JsonNode.class);
            if (sRes != null && sRes.has("data")) {
                structure = sRes.get("data");
            }
        } catch (Exception e) {
            log.warn("Không thể tải cây cấu trúc khóa học {}: {}", courseId, e.getMessage());
        }

        try {
            JsonNode lRes = gatewayClient.get("/course/courses/" + courseId + "/lessons/" + lessonId, JsonNode.class);
            if (lRes != null && lRes.has("data")) {
                lesson = lRes.get("data");
            }
        } catch (Exception e) {
            log.warn("Không thể tải chi tiết bài học {}: {}", lessonId, e.getMessage());
        }

        boolean isLessonCompleted = false;
        try {
            JsonNode pRes = gatewayClient.get("/course/courses/" + courseId + "/progress", JsonNode.class);
            if (pRes != null && pRes.has("data")) {
                progress = pRes.get("data");
                if (progress.hasNonNull("completedLessonIds") && progress.get("completedLessonIds").isArray()) {
                    for (JsonNode cid : progress.get("completedLessonIds")) {
                        if (lessonId.equals(cid.asText())) {
                            isLessonCompleted = true;
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Không thể tải tiến độ khóa học {}: {}", courseId, e.getMessage());
        }

        // Xử lý an toàn Markdown lý thuyết nếu có
        String safeContentHtml = "";
        if (lesson != null && lesson.has("content")) {
            JsonNode content = lesson.get("content");
            String rawText = "";
            if (content.hasNonNull("text")) {
                rawText = content.get("text").asText();
            } else if (content.hasNonNull("markdown")) {
                rawText = content.get("markdown").asText();
            } else if (content.hasNonNull("content")) {
                rawText = content.get("content").asText();
            }
            if (!rawText.isBlank()) {
                safeContentHtml = markdownService.renderHtml(rawText);
            }
        }

        model.addAttribute("course", course);
        model.addAttribute("courseId", courseId);
        model.addAttribute("structure", structure);
        model.addAttribute("lesson", lesson);
        model.addAttribute("lessonId", lessonId);
        model.addAttribute("progress", progress);
        model.addAttribute("schedules", schedules);
        model.addAttribute("announcements", announcements);
        model.addAttribute("safeContentHtml", safeContentHtml);
        model.addAttribute("isLessonCompleted", isLessonCompleted);

        return "courses/student/learning-hub";
    }

    @PostMapping(value = "/learn/{courseId}/lessons/{lessonId}/complete", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> completeLesson(
            @PathVariable("courseId") String courseId,
            @PathVariable("lessonId") String lessonId,
            @RequestBody(required = false) Map<String, Object> body) {
        try {
            Object payload = body != null ? body : Map.of();
            gatewayClient.post("/course/courses/" + courseId + "/lessons/" + lessonId + "/complete", payload, JsonNode.class);
            return ResponseEntity.ok(Map.of(
                    "code", 1000,
                    "message", "Đã đánh dấu hoàn thành bài học"
            ));
        } catch (RestClientResponseException e) {
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                    "code", e.getStatusCode().value(),
                    "message", "Không thể cập nhật tiến độ bài học"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "code", 500,
                    "message", "Có lỗi xảy ra khi hoàn thành bài học"
            ));
        }
    }

    @GetMapping(value = "/courses/{courseId}/schedules", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> getSchedulesJson(@PathVariable("courseId") String courseId) {
        try {
            JsonNode res = gatewayClient.get("/course/courses/" + courseId + "/schedules", JsonNode.class);
            return ResponseEntity.ok(res != null ? res : Map.of("data", List.of()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("data", List.of()));
        }
    }

    @GetMapping(value = "/courses/{courseId}/announcements", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> getAnnouncementsJson(@PathVariable("courseId") String courseId) {
        try {
            JsonNode res = gatewayClient.get("/course/courses/" + courseId + "/announcements", JsonNode.class);
            return ResponseEntity.ok(res != null ? res : Map.of("data", Map.of("content", List.of())));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("data", Map.of("content", List.of())));
        }
    }
}
