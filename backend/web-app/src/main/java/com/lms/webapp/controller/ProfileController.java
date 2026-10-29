package com.lms.webapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.lms.webapp.client.GatewayClient;
import com.lms.webapp.dto.form.ChangePasswordForm;
import com.lms.webapp.dto.form.ProfileUpdateForm;
import com.lms.webapp.dto.form.RoleUpdateForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ProfileController {

    private final GatewayClient gatewayClient;

    public ProfileController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @GetMapping("/profile")
    public String profilePage(@RequestParam(value = "updated", required = false) String updated,
                              @RequestParam(value = "passwordUpdated", required = false) String passwordUpdated,
                              Model model) {
        try {
            JsonNode response = gatewayClient.get("/identity/users/myinfo", JsonNode.class);
            JsonNode profileData = (response != null && response.has("data")) ? response.get("data") : response;
            model.addAttribute("profile", profileData);

            String displayName = "Người dùng EduLearn";
            if (profileData != null) {
                if (profileData.hasNonNull("fullname")) displayName = profileData.get("fullname").asText();
                else if (profileData.hasNonNull("fullName")) displayName = profileData.get("fullName").asText();
            }
            model.addAttribute("displayName", displayName);

            if (!model.containsAttribute("profileUpdateForm")) {
                ProfileUpdateForm form = new ProfileUpdateForm();
                if (profileData != null) {
                    if (profileData.hasNonNull("fullname")) form.setFullname(profileData.get("fullname").asText());
                    if (profileData.hasNonNull("phone")) form.setPhone(profileData.get("phone").asText());
                    if (profileData.hasNonNull("dob")) form.setDob(formatDobForDateInput(profileData.get("dob")));
                    if (profileData.hasNonNull("bio")) form.setBio(profileData.get("bio").asText());
                    if (profileData.hasNonNull("avatarUrl")) form.setAvatarUrl(profileData.get("avatarUrl").asText());
                }
                model.addAttribute("profileUpdateForm", form);
            }

            if (!model.containsAttribute("changePasswordForm")) {
                model.addAttribute("changePasswordForm", new ChangePasswordForm());
            }

            if (!model.containsAttribute("roleUpdateForm")) {
                String currentRole = (profileData != null && profileData.hasNonNull("role")) ? profileData.get("role").asText() : "STUDENT";
                model.addAttribute("roleUpdateForm", new RoleUpdateForm(currentRole));
            }
        } catch (Exception e) {
            model.addAttribute("profile", null);
            prepareModelForProfileView(model);
            model.addAttribute("errorMessage", "Không thể tải thông tin hồ sơ cá nhân.");
        }

        if (updated != null) {
            model.addAttribute("successMessage", "Cập nhật hồ sơ cá nhân thành công.");
        }
        if (passwordUpdated != null) {
            model.addAttribute("successMessage", "Đổi mật khẩu thành công.");
        }

        return "profile/index";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileUpdateForm") ProfileUpdateForm form,
                                BindingResult bindingResult,
                                Model model) {
        if (bindingResult.hasErrors()) {
            prepareModelForProfileView(model);
            return "profile/index";
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("fullname", form.getFullname());
        payload.put("phone", (form.getPhone() != null && !form.getPhone().isBlank()) ? form.getPhone() : null);
        payload.put("dob", formatDobForDateInput(form.getDob()));
        payload.put("bio", form.getBio());
        payload.put("avatarUrl", form.getAvatarUrl());
        payload.put("discardedAvatarUrls", form.getDiscardedAvatarUrls() != null ? form.getDiscardedAvatarUrls() : List.of());

        try {
            gatewayClient.put("/identity/users/myinfo", payload, JsonNode.class);
            return "redirect:/profile?updated=true";
        } catch (Exception e) {
            prepareModelForProfileView(model);
            model.addAttribute("errorMessage", "Cập nhật hồ sơ thất bại: " + e.getMessage());
            return "profile/index";
        }
    }

    @PostMapping("/profile/password")
    public String changePassword(@Valid @ModelAttribute("changePasswordForm") ChangePasswordForm form,
                                 BindingResult bindingResult,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            prepareModelForProfileView(model);
            return "profile/index";
        }

        if (form.getConfirmPassword() != null && !form.getConfirmPassword().equals(form.getNewPassword())) {
            bindingResult.rejectValue("confirmPassword", "Mismatch", "Mật khẩu mới xác nhận không khớp");
            prepareModelForProfileView(model);
            return "profile/index";
        }

        Map<String, String> payload = Map.of(
                "oldPassword", form.getOldPassword(),
                "newPassword", form.getNewPassword(),
                "confirmPassword", form.getConfirmPassword()
        );

        try {
            gatewayClient.patch("/identity/users/myinfo/password", payload, JsonNode.class);
            return "redirect:/profile?passwordUpdated=true";
        } catch (Exception e) {
            prepareModelForProfileView(model);
            model.addAttribute("errorMessage", "Đổi mật khẩu thất bại. Vui lòng kiểm tra lại mật khẩu hiện tại.");
            return "profile/index";
        }
    }

    @PostMapping("/profile/role")
    public String updateRole(@RequestParam("role") String role,
                             HttpServletRequest request,
                             HttpSession session) {
        Map<String, String> payload = Map.of("role", role);
        gatewayClient.patch("/identity/users/myinfo/role", payload, JsonNode.class);

        // Đồng bộ quyền trong phiên đăng nhập:
        // Do API identity-service hiện tại không tự cấp JWT token mới khi đổi role,
        // JWT accessToken cũ trong session vẫn mang authority cũ (ROLE_STUDENT / ROLE_INSTRUCTOR).
        // Giải pháp an toàn theo đúng contract là kết thúc phiên làm việc hiện tại và chuyển hướng
        // người dùng về trang đăng nhập để nhận JWT token mới chứa đúng role mới từ backend.
        SecurityContextHolder.clearContext();
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/login?roleUpdated=true";
    }

    @PostMapping("/profile/avatar/presigned-url")
    @ResponseBody
    public ResponseEntity<?> getAvatarUploadPolicy(@RequestBody Map<String, Object> requestBody) {
        JsonNode response = gatewayClient.post("/identity/storage/presigned-url", requestBody, JsonNode.class);
        if (response != null && response.has("data")) {
            return ResponseEntity.ok(response.get("data"));
        }
        return ResponseEntity.ok(response);
    }

    private String formatDobForDateInput(JsonNode dobNode) {
        if (dobNode == null || dobNode.isNull()) {
            return null;
        }
        if (dobNode.isNumber()) {
            long epochMillis = dobNode.asLong();
            return java.time.Instant.ofEpochMilli(epochMillis)
                    .atZone(java.time.ZoneOffset.UTC)
                    .toLocalDate()
                    .toString();
        }
        return formatDobForDateInput(dobNode.asText());
    }

    private String formatDobForDateInput(String text) {
        if (text == null || text.isBlank() || "null".equalsIgnoreCase(text.trim())) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.contains("T")) {
            trimmed = trimmed.substring(0, trimmed.indexOf("T"));
        }
        if (trimmed.contains(" ")) {
            trimmed = trimmed.substring(0, trimmed.indexOf(" "));
        }
        return trimmed;
    }

    private void prepareModelForProfileView(Model model) {
        if (!model.containsAttribute("profileUpdateForm")) {
            model.addAttribute("profileUpdateForm", new ProfileUpdateForm());
        }
        if (!model.containsAttribute("changePasswordForm")) {
            model.addAttribute("changePasswordForm", new ChangePasswordForm());
        }
        if (!model.containsAttribute("roleUpdateForm")) {
            model.addAttribute("roleUpdateForm", new RoleUpdateForm("STUDENT"));
        }
        if (!model.containsAttribute("displayName")) {
            model.addAttribute("displayName", "Người dùng EduLearn");
        }
    }
}
