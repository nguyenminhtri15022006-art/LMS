package com.lms.webportal.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lms.webportal.client.GatewayClient;
import com.lms.webportal.dto.form.*;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.*;

@Controller
@Slf4j
public class CourseInstructorController {

    private final GatewayClient gatewayClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CourseInstructorController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    /**
     * Chuyển tiếp route /courses/my về /instructor/courses
     */
    @GetMapping("/courses/my")
    public String myCoursesRedirect() {
        return "redirect:/instructor/courses";
    }

    /**
     * Dashboard Giảng viên: thống kê KPI và danh sách khóa học sở hữu
     */
    @GetMapping("/instructor/courses")
    public String instructorDashboard(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model) {

        JsonNode statsNode = null;
        try {
            JsonNode res = gatewayClient.get("/course/courses/my/stats", JsonNode.class);
            if (res != null && res.has("data")) {
                statsNode = res.get("data");
            }
        } catch (Exception e) {
            log.warn("Lỗi khi tải thống kê giảng viên: {}", e.getMessage());
        }

        JsonNode coursesPageNode = null;
        try {
            JsonNode res = gatewayClient.get("/course/courses/my?page=" + page + "&size=" + size, JsonNode.class);
            if (res != null && res.has("data")) {
                coursesPageNode = res.get("data");
            }
        } catch (Exception e) {
            log.warn("Lỗi khi tải danh sách khóa học giảng viên: {}", e.getMessage());
        }

        model.addAttribute("stats", statsNode != null ? statsNode : objectMapper.createObjectNode());
        model.addAttribute("coursesPage", coursesPageNode != null ? coursesPageNode : objectMapper.createObjectNode());
        return "courses/instructor/dashboard";
    }

    /**
     * Trang tạo mới khóa học
     */
    @GetMapping("/instructor/courses/new")
    public String createCoursePage(Model model) {
        loadCategories(model);
        if (!model.containsAttribute("courseForm")) {
            model.addAttribute("courseForm", new CourseForm());
        }
        model.addAttribute("isEdit", false);
        return "courses/instructor/course-form";
    }

    /**
     * Xử lý tạo mới khóa học dạng multipart
     */
    @PostMapping(value = "/instructor/courses/new", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String createCourse(
            @ModelAttribute("courseForm") @Valid CourseForm courseForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            loadCategories(model);
            model.addAttribute("isEdit", false);
            return "courses/instructor/course-form";
        }

        try {
            MultiValueMap<String, Object> body = buildCourseMultipartBody(courseForm);
            gatewayClient.postMultipart("/course/courses", body, JsonNode.class);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo khóa học thành công!");
            return "redirect:/instructor/courses";
        } catch (Exception e) {
            log.error("Lỗi khi tạo khóa học mới: {}", e.getMessage(), e);
            model.addAttribute("errorMessage", "Không thể tạo khóa học: " + e.getMessage());
            loadCategories(model);
            model.addAttribute("isEdit", false);
            return "courses/instructor/course-form";
        }
    }

    /**
     * Trang chỉnh sửa thông tin khóa học
     */
    @GetMapping("/instructor/courses/{id}/edit")
    public String editCoursePage(@PathVariable String id, Model model, RedirectAttributes redirectAttributes) {
        try {
            JsonNode res = gatewayClient.get("/course/courses/" + id, JsonNode.class);
            if (res == null || !res.has("data")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy khóa học");
                return "redirect:/instructor/courses";
            }
            JsonNode courseData = res.get("data");

            CourseForm form = new CourseForm();
            form.setTitle(courseData.hasNonNull("title") ? courseData.get("title").asText() : "");
            form.setDescription(courseData.hasNonNull("description") ? courseData.get("description").asText() : "");
            form.setStatus(courseData.hasNonNull("status") ? courseData.get("status").asText() : "PUBLIC");
            form.setDuration(courseData.hasNonNull("duration") ? courseData.get("duration").asInt() : 1);
            form.setLevel(courseData.hasNonNull("level") ? courseData.get("level").asText() : "BEGINNER");
            form.setPrice(courseData.hasNonNull("price") ? courseData.get("price").asDouble() : 0.0);

            List<Long> catIds = new ArrayList<>();
            if (courseData.has("categories") && courseData.get("categories").isArray()) {
                for (JsonNode cat : courseData.get("categories")) {
                    if (cat.hasNonNull("id")) {
                        catIds.add(cat.get("id").asLong());
                    }
                }
            }
            form.setCategoryIds(catIds);

            List<String> learningPoints = new ArrayList<>();
            if (courseData.has("learningPoints") && courseData.get("learningPoints").isArray()) {
                for (JsonNode lp : courseData.get("learningPoints")) {
                    if (lp.isTextual() && !lp.asText().isBlank()) {
                        learningPoints.add(lp.asText());
                    }
                }
            }
            form.setLearningPoints(learningPoints);
            if (!learningPoints.isEmpty()) {
                form.setLearningPointsText(String.join("\n", learningPoints));
            }

            List<String> requirements = new ArrayList<>();
            if (courseData.has("requirements") && courseData.get("requirements").isArray()) {
                for (JsonNode req : courseData.get("requirements")) {
                    if (req.isTextual() && !req.asText().isBlank()) {
                        requirements.add(req.asText());
                    }
                }
            }
            form.setRequirements(requirements);
            if (!requirements.isEmpty()) {
                form.setRequirementsText(String.join("\n", requirements));
            }

            model.addAttribute("courseForm", form);
            model.addAttribute("course", courseData);
            model.addAttribute("isEdit", true);
            loadCategories(model);
            return "courses/instructor/course-form";
        } catch (Exception e) {
            log.error("Lỗi khi tải thông tin khóa học {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tải thông tin khóa học: " + e.getMessage());
            return "redirect:/instructor/courses";
        }
    }

    /**
     * Xử lý cập nhật thông tin khóa học dạng multipart
     */
    @PostMapping(value = "/instructor/courses/{id}/edit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public String updateCourse(
            @PathVariable String id,
            @ModelAttribute("courseForm") @Valid CourseForm courseForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            loadCategories(model);
            ObjectNode dummyCourse = objectMapper.createObjectNode();
            dummyCourse.put("id", id);
            model.addAttribute("course", dummyCourse);
            model.addAttribute("isEdit", true);
            return "courses/instructor/course-form";
        }

        try {
            MultiValueMap<String, Object> body = buildCourseMultipartBody(courseForm);
            gatewayClient.putMultipart("/course/courses/" + id, body, JsonNode.class);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật khóa học thành công!");
            return "redirect:/instructor/courses";
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật khóa học {}: {}", id, e.getMessage(), e);
            model.addAttribute("errorMessage", "Không thể cập nhật khóa học: " + e.getMessage());
            loadCategories(model);
            ObjectNode dummyCourse = objectMapper.createObjectNode();
            dummyCourse.put("id", id);
            model.addAttribute("course", dummyCourse);
            model.addAttribute("isEdit", true);
            return "courses/instructor/course-form";
        }
    }

    /**
     * Xóa khóa học
     */
    @PostMapping("/instructor/courses/{id}/delete")
    public String deleteCourse(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            gatewayClient.delete("/course/courses/" + id, JsonNode.class);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa khóa học thành công!");
        } catch (Exception e) {
            log.error("Lỗi khi xóa khóa học {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa khóa học: " + e.getMessage());
        }
        return "redirect:/instructor/courses";
    }

    /**
     * Trung tâm quản lý khóa học (Cây bài giảng, Học viên, Lịch học, Thông báo)
     */
    @GetMapping({"/instructor/courses/{id}/manage", "/instructor/courses/{id}"})
    public String courseManagementCenter(
            @PathVariable String id,
            @RequestParam(value = "tab", defaultValue = "curriculum") String activeTab,
            Model model,
            RedirectAttributes redirectAttributes) {

        try {
            JsonNode courseRes = gatewayClient.get("/course/courses/" + id, JsonNode.class);
            if (courseRes == null || !courseRes.has("data")) {
                redirectAttributes.addFlashAttribute("errorMessage", "Không tìm thấy khóa học");
                return "redirect:/instructor/courses";
            }
            model.addAttribute("course", courseRes.get("data"));

            // Cấu trúc cây đề cương
            JsonNode structureRes = null;
            try {
                structureRes = gatewayClient.get("/course/courses/" + id + "/structure", JsonNode.class);
            } catch (Exception e) {
                log.warn("Không thể tải cấu trúc đề cương cho {}: {}", id, e.getMessage());
            }
            model.addAttribute("structure", structureRes != null && structureRes.has("data") ? structureRes.get("data") : objectMapper.createObjectNode());

            model.addAttribute("activeTab", activeTab);
            return "courses/instructor/management-center";
        } catch (Exception e) {
            log.error("Lỗi khi tải thông tin quản lý khóa học {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
            return "redirect:/instructor/courses";
        }
    }

    // ==========================================
    // CÁC ENDPOINT AJAX DÀNH CHO CÂY BÀI GIẢNG (CURRICULUM)
    // ==========================================

    @GetMapping("/instructor/courses/{courseId}/structure")
    @ResponseBody
    public ResponseEntity<?> getCourseStructure(@PathVariable String courseId) {
        try {
            JsonNode res = gatewayClient.get("/course/courses/" + courseId + "/structure", JsonNode.class);
            return ResponseEntity.ok(res != null ? res : Map.of("code", 1000, "data", Map.of("nodes", List.of())));
        } catch (RestClientResponseException e) {
            return handleGatewayException(e, "Không thể tải cấu trúc đề cương khóa học");
        } catch (Exception e) {
            log.error("Lỗi khi tải cấu trúc đề cương {}: {}", courseId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "Lỗi hệ thống khi tải cấu trúc đề cương"));
        }
    }

    @PostMapping("/instructor/courses/{courseId}/structure/nodes")
    @ResponseBody
    public ResponseEntity<?> addStructureNode(
            @PathVariable String courseId,
            @Valid @RequestBody StructureNodeForm form,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage()));
        }
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            payload.put("type", form.getType());
            payload.put("title", form.getTitle().trim());
            if (form.getParentId() != null && !form.getParentId().isBlank()) {
                payload.put("parentId", form.getParentId().trim());
            }
            if (form.getOrder() != null) {
                payload.put("order", form.getOrder());
            }
            JsonNode res = gatewayClient.post("/course/courses/" + courseId + "/structure/nodes", payload, JsonNode.class);
            return ResponseEntity.ok(res != null ? res : Map.of("code", 1000));
        } catch (RestClientResponseException e) {
            return handleGatewayException(e, "Không thể thêm mục vào cấu trúc giáo trình");
        } catch (Exception e) {
            log.error("Lỗi khi thêm node cấu trúc cho {}: {}", courseId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "Lỗi hệ thống khi thêm mục giáo trình"));
        }
    }

    @PutMapping("/instructor/courses/{courseId}/structure/reorder")
    @ResponseBody
    public ResponseEntity<?> reorderStructureNodes(
            @PathVariable String courseId,
            @Valid @RequestBody ReorderNodesForm form,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage()));
        }
        try {
            JsonNode res = gatewayClient.put("/course/courses/" + courseId + "/structure/reorder", form, JsonNode.class);
            return ResponseEntity.ok(res != null ? res : Map.of("code", 1000));
        } catch (RestClientResponseException e) {
            return handleGatewayException(e, "Không thể sắp xếp lại cây bài giảng");
        } catch (Exception e) {
            log.error("Lỗi khi sắp xếp lại cấu trúc bài giảng {}: {}", courseId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "Lỗi hệ thống khi sắp xếp lại cấu trúc bài giảng"));
        }
    }

    @PatchMapping("/instructor/courses/{courseId}/structure/nodes/{nodeId}")
    @ResponseBody
    public ResponseEntity<?> updateNodeTitle(
            @PathVariable String courseId,
            @PathVariable String nodeId,
            @Valid @RequestBody NodeTitleForm form,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage()));
        }
        try {
            Map<String, String> payload = Map.of("title", form.getTitle().trim());
            JsonNode res = gatewayClient.patch("/course/courses/" + courseId + "/structure/nodes/" + nodeId, payload, JsonNode.class);
            return ResponseEntity.ok(res != null ? res : Map.of("code", 1000));
        } catch (RestClientResponseException e) {
            return handleGatewayException(e, "Không thể sửa tiêu đề mục giáo trình");
        } catch (Exception e) {
            log.error("Lỗi khi sửa tiêu đề node {} {}: {}", courseId, nodeId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "Lỗi hệ thống khi sửa tiêu đề mục giáo trình"));
        }
    }

    @DeleteMapping("/instructor/courses/{courseId}/structure/nodes/{nodeId}")
    @ResponseBody
    public ResponseEntity<?> deleteStructureNode(
            @PathVariable String courseId,
            @PathVariable String nodeId) {
        try {
            JsonNode res = gatewayClient.delete("/course/courses/" + courseId + "/structure/nodes/" + nodeId, JsonNode.class);
            return ResponseEntity.ok(res != null ? res : Map.of("code", 1000));
        } catch (RestClientResponseException e) {
            return handleGatewayException(e, "Không thể xóa mục giáo trình");
        } catch (Exception e) {
            log.error("Lỗi khi xóa node {} {}: {}", courseId, nodeId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "Lỗi hệ thống khi xóa mục giáo trình"));
        }
    }

    @GetMapping("/instructor/courses/{courseId}/lessons/{lessonId}")
    @ResponseBody
    public ResponseEntity<?> getLessonDetail(
            @PathVariable String courseId,
            @PathVariable String lessonId) {
        try {
            JsonNode res = gatewayClient.get("/course/courses/" + courseId + "/lessons/" + lessonId, JsonNode.class);
            return ResponseEntity.ok(res != null ? res : Map.of("code", 1000));
        } catch (RestClientResponseException e) {
            return handleGatewayException(e, "Không thể tải chi tiết bài học");
        } catch (Exception e) {
            log.error("Lỗi khi tải chi tiết bài học {} {}: {}", courseId, lessonId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "Lỗi hệ thống khi tải chi tiết bài học"));
        }
    }

    @PutMapping("/instructor/courses/{courseId}/lessons/{lessonId}")
    @ResponseBody
    public ResponseEntity<?> updateLessonContent(
            @PathVariable String courseId,
            @PathVariable String lessonId,
            @Valid @RequestBody LessonUpdateForm form,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage()));
        }
        try {
            Map<String, Object> contentMap = new HashMap<>();
            String type = form.getLessonType() != null ? form.getLessonType().trim() : "VIDEO";
            if ("VIDEO".equalsIgnoreCase(type)) {
                if (form.getContent() instanceof Map<?, ?> m && m.containsKey("videoUrl")) {
                    contentMap.put("videoUrl", String.valueOf(m.get("videoUrl")));
                } else if (form.getVideoUrl() != null && !form.getVideoUrl().isBlank()) {
                    contentMap.put("videoUrl", form.getVideoUrl().trim());
                } else if (form.getContent() instanceof String s) {
                    contentMap.put("videoUrl", s.trim());
                }
            } else {
                if (form.getContent() instanceof Map<?, ?> m && m.containsKey("text")) {
                    contentMap.put("text", String.valueOf(m.get("text")));
                } else if (form.getContent() instanceof String s) {
                    contentMap.put("text", s);
                }
            }
            Map<String, Object> payload = Map.of(
                    "lessonType", type,
                    "content", contentMap
            );
            JsonNode res = gatewayClient.put("/course/courses/" + courseId + "/lessons/" + lessonId, payload, JsonNode.class);
            return ResponseEntity.ok(res != null ? res : Map.of("code", 1000));
        } catch (RestClientResponseException e) {
            return handleGatewayException(e, "Không thể cập nhật nội dung bài học");
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật nội dung bài học {} {}: {}", courseId, lessonId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "Lỗi hệ thống khi cập nhật nội dung bài học"));
        }
    }

    // ==========================================
    // CÁC ENDPOINT AJAX DÀNH CHO HỌC VIÊN (STUDENTS)
    // ==========================================

    @GetMapping("/instructor/courses/{courseId}/students")
    @ResponseBody
    public ResponseEntity<?> getCourseStudents(
            @PathVariable String courseId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        try {
            JsonNode res = gatewayClient.get("/course/courses/" + courseId + "/students?page=" + page + "&size=" + size, JsonNode.class);
            return ResponseEntity.ok(res != null ? res : Map.of("code", 1000));
        } catch (RestClientResponseException e) {
            return handleGatewayException(e, "Không thể tải danh sách học viên");
        } catch (Exception e) {
            log.error("Lỗi khi tải danh sách học viên {}: {}", courseId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "Lỗi hệ thống khi tải danh sách học viên"));
        }
    }

    @PostMapping("/instructor/courses/{courseId}/invite")
    @ResponseBody
    public ResponseEntity<?> inviteStudent(
            @PathVariable String courseId,
            @Valid @RequestBody InviteStudentForm form,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage()));
        }
        try {
            ObjectNode payload = objectMapper.createObjectNode();
            ArrayNode emails = objectMapper.createArrayNode();
            emails.add(form.getEmail().trim());
            payload.set("emails", emails);

            JsonNode res = gatewayClient.post("/course/courses/" + courseId + "/invite", payload, JsonNode.class);
            return ResponseEntity.ok(res != null ? res : Map.of("code", 1000));
        } catch (RestClientResponseException e) {
            return handleGatewayException(e, "Không thể mời học viên");
        } catch (Exception e) {
            log.error("Lỗi khi mời học viên vào khóa học {}: {}", courseId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "Lỗi hệ thống khi mời học viên"));
        }
    }

    @DeleteMapping("/instructor/courses/{courseId}/students/{userId}")
    @ResponseBody
    public ResponseEntity<?> removeStudent(
            @PathVariable String courseId,
            @PathVariable String userId) {
        try {
            JsonNode res = gatewayClient.delete("/course/courses/" + courseId + "/students/" + userId, JsonNode.class);
            return ResponseEntity.ok(res != null ? res : Map.of("code", 1000));
        } catch (RestClientResponseException e) {
            return handleGatewayException(e, "Không thể xóa học viên");
        } catch (Exception e) {
            log.error("Lỗi khi xóa học viên {} khỏi {}: {}", userId, courseId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "Lỗi hệ thống khi xóa học viên"));
        }
    }

    // ==========================================
    // CÁC ENDPOINT AJAX DÀNH CHO LỊCH HỌC (SCHEDULES)
    // ==========================================

    @GetMapping("/instructor/courses/{courseId}/schedules")
    @ResponseBody
    public ResponseEntity<?> getCourseSchedules(@PathVariable String courseId) {
        try {
            JsonNode res = gatewayClient.get("/course/courses/" + courseId + "/schedules", JsonNode.class);
            return ResponseEntity.ok(res != null ? res : Map.of("code", 1000));
        } catch (RestClientResponseException e) {
            return handleGatewayException(e, "Không thể tải lịch học");
        } catch (Exception e) {
            log.error("Lỗi khi tải lịch học {}: {}", courseId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "Lỗi hệ thống khi tải lịch học"));
        }
    }

    @PostMapping("/instructor/courses/{courseId}/schedules")
    @ResponseBody
    public ResponseEntity<?> createSchedule(
            @PathVariable String courseId,
            @Valid @RequestBody ScheduleForm form,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage()));
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", form.getTitle().trim());
            payload.put("startTime", form.getStartTime());
            payload.put("endTime", form.getEndTime());
            if (form.getNote() != null) {
                payload.put("note", form.getNote().trim());
            }
            JsonNode res = gatewayClient.post("/course/courses/" + courseId + "/schedules", payload, JsonNode.class);
            return ResponseEntity.ok(res != null ? res : Map.of("code", 1000));
        } catch (RestClientResponseException e) {
            return handleGatewayException(e, "Không thể tạo lịch học");
        } catch (Exception e) {
            log.error("Lỗi khi tạo lịch học {}: {}", courseId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "Lỗi hệ thống khi tạo lịch học"));
        }
    }

    @PutMapping("/instructor/courses/{courseId}/schedules/{scheduleId}")
    @ResponseBody
    public ResponseEntity<?> updateSchedule(
            @PathVariable String courseId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleForm form,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage()));
        }
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", form.getTitle().trim());
            payload.put("startTime", form.getStartTime());
            payload.put("endTime", form.getEndTime());
            if (form.getNote() != null) {
                payload.put("note", form.getNote().trim());
            }
            JsonNode res = gatewayClient.put("/course/courses/" + courseId + "/schedules/" + scheduleId, payload, JsonNode.class);
            return ResponseEntity.ok(res != null ? res : Map.of("code", 1000));
        } catch (RestClientResponseException e) {
            return handleGatewayException(e, "Không thể cập nhật lịch học");
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật lịch học {} {}: {}", courseId, scheduleId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "Lỗi hệ thống khi cập nhật lịch học"));
        }
    }

    @DeleteMapping("/instructor/courses/{courseId}/schedules/{scheduleId}")
    @ResponseBody
    public ResponseEntity<?> deleteSchedule(
            @PathVariable String courseId,
            @PathVariable Long scheduleId) {
        try {
            JsonNode res = gatewayClient.delete("/course/courses/" + courseId + "/schedules/" + scheduleId, JsonNode.class);
            return ResponseEntity.ok(res != null ? res : Map.of("code", 1000));
        } catch (RestClientResponseException e) {
            return handleGatewayException(e, "Không thể xóa lịch học");
        } catch (Exception e) {
            log.error("Lỗi khi xóa lịch học {} {}: {}", courseId, scheduleId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "Lỗi hệ thống khi xóa lịch học"));
        }
    }

    // ==========================================
    // CÁC ENDPOINT AJAX DÀNH CHO THÔNG BÁO (ANNOUNCEMENTS)
    // ==========================================

    @GetMapping("/instructor/courses/{courseId}/announcements")
    @ResponseBody
    public ResponseEntity<?> getCourseAnnouncements(
            @PathVariable String courseId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        try {
            JsonNode res = gatewayClient.get("/course/courses/" + courseId + "/announcements?page=" + page + "&size=" + size, JsonNode.class);
            return ResponseEntity.ok(res != null ? res : Map.of("code", 1000));
        } catch (RestClientResponseException e) {
            return handleGatewayException(e, "Không thể tải thông báo");
        } catch (Exception e) {
            log.error("Lỗi khi tải thông báo {}: {}", courseId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "Lỗi hệ thống khi tải thông báo"));
        }
    }

    @PostMapping("/instructor/courses/{courseId}/announcements")
    @ResponseBody
    public ResponseEntity<?> createAnnouncement(
            @PathVariable String courseId,
            @Valid @RequestBody AnnouncementForm form,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(Map.of("code", 400, "message", Objects.requireNonNull(bindingResult.getFieldError()).getDefaultMessage()));
        }
        try {
            Map<String, Object> payload = Map.of(
                    "title", form.getTitle().trim(),
                    "content", form.getContent().trim()
            );
            JsonNode res = gatewayClient.post("/course/courses/" + courseId + "/announcements", payload, JsonNode.class);
            return ResponseEntity.ok(res != null ? res : Map.of("code", 1000));
        } catch (RestClientResponseException e) {
            return handleGatewayException(e, "Không thể tạo thông báo");
        } catch (Exception e) {
            log.error("Lỗi khi tạo thông báo {}: {}", courseId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "Lỗi hệ thống khi tạo thông báo"));
        }
    }

    @DeleteMapping("/instructor/courses/{courseId}/announcements/{announcementId}")
    @ResponseBody
    public ResponseEntity<?> deleteAnnouncement(
            @PathVariable String courseId,
            @PathVariable Long announcementId) {
        try {
            JsonNode res = gatewayClient.delete("/course/courses/" + courseId + "/announcements/" + announcementId, JsonNode.class);
            return ResponseEntity.ok(res != null ? res : Map.of("code", 1000));
        } catch (RestClientResponseException e) {
            return handleGatewayException(e, "Không thể xóa thông báo");
        } catch (Exception e) {
            log.error("Lỗi khi xóa thông báo {} {}: {}", courseId, announcementId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("code", 500, "message", "Lỗi hệ thống khi xóa thông báo"));
        }
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private ResponseEntity<?> handleGatewayException(RestClientResponseException e, String fallbackMessage) {
        int statusCode = e.getStatusCode().value();
        String safeMsg = fallbackMessage;
        try {
            JsonNode errorNode = objectMapper.readTree(e.getResponseBodyAsString());
            if (errorNode != null && errorNode.hasNonNull("message") && !errorNode.get("message").asText().isBlank()) {
                safeMsg = errorNode.get("message").asText();
            }
        } catch (Exception ignored) {
        }
        if (safeMsg.equals(fallbackMessage)) {
            if (statusCode == 400) safeMsg = "Dữ liệu yêu cầu không hợp lệ";
            else if (statusCode == 401) safeMsg = "Phiên đăng nhập đã hết hạn";
            else if (statusCode == 403) safeMsg = "Bạn không có quyền thực hiện thao tác này";
            else if (statusCode == 404) safeMsg = "Không tìm thấy tài nguyên yêu cầu";
        }
        return ResponseEntity.status(e.getStatusCode())
                .body(Map.of("code", statusCode, "message", safeMsg));
    }

    private void loadCategories(Model model) {
        List<JsonNode> categories = new ArrayList<>();
        try {
            JsonNode res = gatewayClient.get("/course/categories", JsonNode.class);
            if (res != null && res.has("data") && res.get("data").isArray()) {
                for (JsonNode cat : res.get("data")) {
                    categories.add(cat);
                }
            }
        } catch (Exception e) {
            log.warn("Lỗi khi tải danh mục: {}", e.getMessage());
        }
        model.addAttribute("categories", categories);
    }

    private MultiValueMap<String, Object> buildCourseMultipartBody(CourseForm form) throws IOException {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("title", form.getTitle());
        if (form.getDescription() != null) {
            body.add("description", form.getDescription());
        }
        if (form.getStatus() != null) {
            body.add("status", form.getStatus());
        }
        if (form.getDuration() != null) {
            body.add("duration", form.getDuration());
        }
        if (form.getLevel() != null) {
            body.add("level", form.getLevel());
        }
        if (form.getPrice() != null) {
            body.add("price", form.getPrice());
        }
        if (form.getCategoryIds() != null) {
            for (Long catId : form.getCategoryIds()) {
                body.add("categoryIds", catId);
            }
        }
        if (form.getLearningPoints() != null) {
            for (String lp : form.getLearningPoints()) {
                if (lp != null && !lp.isBlank()) {
                    body.add("learningPoints", lp.trim());
                }
            }
        }
        if (form.getRequirements() != null) {
            for (String req : form.getRequirements()) {
                if (req != null && !req.isBlank()) {
                    body.add("requirements", req.trim());
                }
            }
        }
        if (form.getThumbnail() != null && !form.getThumbnail().isEmpty() && form.getThumbnail().getSize() > 0) {
            body.add("thumbnail", new ByteArrayResource(form.getThumbnail().getBytes()) {
                @Override
                public String getFilename() {
                    return form.getThumbnail().getOriginalFilename();
                }
            });
        }
        return body;
    }
}
