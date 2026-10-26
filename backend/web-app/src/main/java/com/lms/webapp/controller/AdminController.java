package com.lms.webapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.lms.webapp.client.GatewayClient;
import com.lms.webapp.dto.form.*;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@Slf4j
public class AdminController {

    private final GatewayClient gatewayClient;

    public AdminController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    // ==========================================
    // 1. DASHBOARD TỔNG QUAN
    // ==========================================

    @GetMapping({"", "/", "/dashboard"})
    public String dashboard(Model model) {
        boolean usersStatsLoaded = false;
        try {
            JsonNode statsRes = gatewayClient.get("/identity/admin/users/stats", JsonNode.class);
            if (statsRes != null && statsRes.has("data")) {
                model.addAttribute("stats", statsRes.get("data"));
                usersStatsLoaded = true;
            } else {
                model.addAttribute("stats", null);
            }
        } catch (Exception e) {
            log.warn("Không thể tải thống kê người dùng từ Gateway: {}", e.getMessage());
            model.addAttribute("stats", null);
        }
        model.addAttribute("usersStatsLoaded", usersStatsLoaded);

        boolean coursesStatsLoaded = false;
        try {
            JsonNode coursesRes = gatewayClient.get("/course/courses?page=0&size=1", JsonNode.class);
            if (coursesRes != null && coursesRes.has("data")) {
                JsonNode data = coursesRes.get("data");
                if (data.has("totalElements")) {
                    model.addAttribute("totalCourses", data.get("totalElements").asLong());
                    coursesStatsLoaded = true;
                }
            }
        } catch (Exception e) {
            log.warn("Không thể tải tổng số khóa học từ Gateway: {}", e.getMessage());
        }
        if (!coursesStatsLoaded) {
            model.addAttribute("totalCourses", null);
        }
        model.addAttribute("coursesStatsLoaded", coursesStatsLoaded);

        model.addAttribute("activeNav", "dashboard");
        return "admin/dashboard";
    }

    // ==========================================
    // 2. QUẢN LÝ NGƯỜI DÙNG (USERS)
    // ==========================================

    @GetMapping("/users")
    public String usersList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String keyword,
            Model model) {

        StringBuilder uri = new StringBuilder("/identity/admin/users?page=").append(page)
                .append("&size=").append(size);

        if (role != null && !role.trim().isEmpty()) {
            uri.append("&role=").append(URLEncoder.encode(role.trim(), StandardCharsets.UTF_8));
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            uri.append("&keyword=").append(URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8));
        }

        try {
            JsonNode usersRes = gatewayClient.get(uri.toString(), JsonNode.class);
            if (usersRes != null && usersRes.has("data")) {
                model.addAttribute("usersPage", usersRes.get("data"));
            } else {
                model.addAttribute("usersPage", null);
            }
        } catch (Exception e) {
            log.warn("Không thể tải danh sách người dùng từ Gateway: {}", e.getMessage());
            model.addAttribute("usersPage", null);
            model.addAttribute("errorMessage", "Không thể tải danh sách người dùng: " + e.getMessage());
        }

        model.addAttribute("selectedRole", role);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("activeNav", "users");
        return "admin/users/list";
    }

    @PostMapping("/users/{id}/toggle-active")
    public String toggleUserActive(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            gatewayClient.patch("/identity/admin/users/" + id + "/active", null, JsonNode.class);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái người dùng thành công.");
        } catch (Exception e) {
            log.error("Lỗi khi toggle trạng thái user {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể thay đổi trạng thái người dùng: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/reset-password")
    public String adminResetPassword(
            @PathVariable String id,
            @ModelAttribute @Valid AdminResetPasswordForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/admin/users";
        }

        try {
            Map<String, Object> body = Map.of("newPassword", form.getNewPassword());
            gatewayClient.patch("/identity/admin/users/" + id + "/password", body, JsonNode.class);
            redirectAttributes.addFlashAttribute("successMessage", "Đặt lại mật khẩu cho người dùng thành công.");
        } catch (Exception e) {
            log.error("Lỗi khi reset password cho user {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể đặt lại mật khẩu: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle-admin-active")
    public String toggleAdminActive(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            gatewayClient.patch("/identity/admin/users/" + id + "/admin-active", null, JsonNode.class);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật quyền quản trị viên thành công.");
        } catch (Exception e) {
            log.error("Lỗi khi toggle admin active cho user {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể thay đổi quyền admin: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/admins")
    public String createAdmin(
            @ModelAttribute @Valid AdminCreateAdminForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/admin/users";
        }

        try {
            Map<String, Object> body = new HashMap<>();
            body.put("username", form.getUsername());
            body.put("password", form.getPassword());
            body.put("email", form.getEmail());
            body.put("fullname", form.getFullname());

            gatewayClient.post("/identity/admin/users/admins", body, JsonNode.class);
            redirectAttributes.addFlashAttribute("successMessage", "Tạo tài khoản Quản trị viên mới thành công.");
        } catch (Exception e) {
            log.error("Lỗi khi tạo admin mới: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tạo tài khoản admin: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ==========================================
    // 3. QUẢN LÝ KHÓA HỌC (COURSES)
    // ==========================================

    @GetMapping("/courses")
    public String coursesList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword,
            Model model) {

        StringBuilder uri = new StringBuilder("/course/courses?page=").append(page)
                .append("&size=").append(size);

        if (status != null && !status.trim().isEmpty()) {
            uri.append("&status=").append(URLEncoder.encode(status.trim(), StandardCharsets.UTF_8));
        }
        if (level != null && !level.trim().isEmpty()) {
            uri.append("&level=").append(URLEncoder.encode(level.trim(), StandardCharsets.UTF_8));
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            uri.append("&keyword=").append(URLEncoder.encode(keyword.trim(), StandardCharsets.UTF_8));
        }

        try {
            JsonNode coursesRes = gatewayClient.get(uri.toString(), JsonNode.class);
            if (coursesRes != null && coursesRes.has("data")) {
                model.addAttribute("coursesPage", coursesRes.get("data"));
            } else {
                model.addAttribute("coursesPage", null);
            }
        } catch (Exception e) {
            log.warn("Không thể tải danh sách khóa học từ Gateway: {}", e.getMessage());
            model.addAttribute("coursesPage", null);
            model.addAttribute("errorMessage", "Không thể tải danh sách khóa học: " + e.getMessage());
        }

        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedLevel", level);
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("activeNav", "courses");
        return "admin/courses/list";
    }

    @PostMapping("/courses/{id}/lock")
    public String lockCourse(
            @PathVariable String id,
            @ModelAttribute @Valid LockCourseForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/admin/courses";
        }

        try {
            Map<String, Object> body = Map.of("reason", form.getReason());
            gatewayClient.patch("/course/courses/" + id + "/lock", body, JsonNode.class);
            redirectAttributes.addFlashAttribute("successMessage", "Đã khóa khóa học thành công.");
        } catch (Exception e) {
            log.error("Lỗi khi khóa khóa học {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể khóa khóa học: " + e.getMessage());
        }
        return "redirect:/admin/courses";
    }

    @PostMapping("/courses/{id}/unlock")
    public String unlockCourse(@PathVariable String id, RedirectAttributes redirectAttributes) {
        try {
            gatewayClient.patch("/course/courses/" + id + "/unlock", null, JsonNode.class);
            redirectAttributes.addFlashAttribute("successMessage", "Đã mở khóa khóa học thành công.");
        } catch (Exception e) {
            log.error("Lỗi khi mở khóa khóa học {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể mở khóa khóa học: " + e.getMessage());
        }
        return "redirect:/admin/courses";
    }

    // ==========================================
    // 4. QUẢN LÝ DANH MỤC KHÓA HỌC (CATEGORIES)
    // ==========================================

    @GetMapping("/categories")
    public String categoriesList(Model model) {
        try {
            JsonNode catRes = gatewayClient.get("/course/categories", JsonNode.class);
            if (catRes != null && catRes.has("data")) {
                model.addAttribute("categories", catRes.get("data"));
            } else {
                model.addAttribute("categories", null);
            }
        } catch (Exception e) {
            log.warn("Không thể tải danh sách danh mục từ Gateway: {}", e.getMessage());
            model.addAttribute("categories", null);
            model.addAttribute("errorMessage", "Không thể tải danh mục: " + e.getMessage());
        }

        model.addAttribute("activeNav", "categories");
        return "admin/categories/list";
    }

    @PostMapping("/categories")
    public String createCategory(
            @ModelAttribute @Valid CategoryForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/admin/categories";
        }

        try {
            Map<String, Object> body = Map.of("name", form.getName(), "slug", form.getSlug());
            gatewayClient.post("/course/categories", body, JsonNode.class);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm thể loại khóa học thành công.");
        } catch (Exception e) {
            log.error("Lỗi khi tạo danh mục: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tạo thể loại: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}")
    public String updateCategory(
            @PathVariable Long id,
            @ModelAttribute @Valid CategoryForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/admin/categories";
        }

        try {
            Map<String, Object> body = Map.of("name", form.getName(), "slug", form.getSlug());
            gatewayClient.put("/course/categories/" + id, body, JsonNode.class);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thể loại khóa học thành công.");
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật danh mục {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật thể loại: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            gatewayClient.delete("/course/categories/" + id, JsonNode.class);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa thể loại khóa học thành công.");
        } catch (Exception e) {
            log.error("Lỗi khi xóa danh mục {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa thể loại: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    // ==========================================
    // 5. QUẢN LÝ THẺ DIỄN ĐÀN (TAGS)
    // ==========================================

    @GetMapping("/tags")
    public String tagsList(Model model) {
        try {
            JsonNode tagRes = gatewayClient.get("/blog/tags", JsonNode.class);
            if (tagRes != null && tagRes.has("data")) {
                model.addAttribute("tags", tagRes.get("data"));
            } else {
                model.addAttribute("tags", null);
            }
        } catch (Exception e) {
            log.warn("Không thể tải danh sách thẻ từ Gateway: {}", e.getMessage());
            model.addAttribute("tags", null);
            model.addAttribute("errorMessage", "Không thể tải thẻ tags: " + e.getMessage());
        }

        model.addAttribute("activeNav", "tags");
        return "admin/tags/list";
    }

    @PostMapping("/tags")
    public String createTag(
            @ModelAttribute @Valid TagForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/admin/tags";
        }

        try {
            Map<String, Object> body = Map.of("name", form.getName(), "slug", form.getSlug());
            gatewayClient.post("/blog/tags", body, JsonNode.class);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm thẻ tag diễn đàn thành công.");
        } catch (Exception e) {
            log.error("Lỗi khi tạo tag: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể tạo thẻ tag: " + e.getMessage());
        }
        return "redirect:/admin/tags";
    }

    @PostMapping("/tags/{id}")
    public String updateTag(
            @PathVariable Long id,
            @ModelAttribute @Valid TagForm form,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            String errorMsg = bindingResult.getAllErrors().get(0).getDefaultMessage();
            redirectAttributes.addFlashAttribute("errorMessage", errorMsg);
            return "redirect:/admin/tags";
        }

        try {
            Map<String, Object> body = Map.of("name", form.getName(), "slug", form.getSlug());
            gatewayClient.put("/blog/tags/" + id, body, JsonNode.class);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật thẻ tag thành công.");
        } catch (Exception e) {
            log.error("Lỗi khi cập nhật tag {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể cập nhật thẻ tag: " + e.getMessage());
        }
        return "redirect:/admin/tags";
    }

    @PostMapping("/tags/{id}/delete")
    public String deleteTag(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            gatewayClient.delete("/blog/tags/" + id, JsonNode.class);
            redirectAttributes.addFlashAttribute("successMessage", "Xóa thẻ tag diễn đàn thành công.");
        } catch (Exception e) {
            log.error("Lỗi khi xóa tag {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa thẻ tag: " + e.getMessage());
        }
        return "redirect:/admin/tags";
    }
}
