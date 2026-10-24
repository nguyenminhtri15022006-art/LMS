package com.lms.webapp.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.lms.webapp.client.GatewayClient;
import com.lms.webapp.dto.form.ChangePasswordForm;
import com.lms.webapp.dto.form.ProfileUpdateForm;
import com.lms.webapp.dto.form.RoleUpdateForm;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
                              @RequestParam(value = "roleUpdated", required = false) String roleUpdated,
                              Model model) {
        try {
            JsonNode response = gatewayClient.get("/identity/users/myinfo", JsonNode.class);
            JsonNode profileData = (response != null && response.has("data")) ? response.get("data") : response;
            model.addAttribute("profile", profileData);

            if (!model.containsAttribute("profileUpdateForm")) {
                ProfileUpdateForm form = new ProfileUpdateForm();
                if (profileData != null) {
                    if (profileData.hasNonNull("fullName")) form.setFullName(profileData.get("fullName").asText());
                    if (profileData.hasNonNull("phoneNumber")) form.setPhoneNumber(profileData.get("phoneNumber").asText());
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
            model.addAttribute("errorMessage", "Không thể tải thông tin hồ sơ cá nhân.");
        }

        if (updated != null) {
            model.addAttribute("successMessage", "Cập nhật hồ sơ cá nhân thành công.");
        }
        if (passwordUpdated != null) {
            model.addAttribute("successMessage", "Đổi mật khẩu thành công.");
        }
        if (roleUpdated != null) {
            model.addAttribute("successMessage", "Cập nhật vai trò thành công.");
        }

        return "profile/index";
    }

    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileUpdateForm") ProfileUpdateForm form,
                                BindingResult bindingResult,
                                Model model) {
        if (bindingResult.hasErrors()) {
            return profilePage(null, null, null, model);
        }

        Map<String, String> payload = new HashMap<>();
        payload.put("fullName", form.getFullName() != null ? form.getFullName() : "");
        payload.put("phoneNumber", form.getPhoneNumber() != null ? form.getPhoneNumber() : "");
        payload.put("bio", form.getBio() != null ? form.getBio() : "");
        payload.put("avatarUrl", form.getAvatarUrl() != null ? form.getAvatarUrl() : "");

        try {
            gatewayClient.put("/identity/users/myinfo", payload, JsonNode.class);
            return "redirect:/profile?updated=true";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Cập nhật hồ sơ thất bại: " + e.getMessage());
            return profilePage(null, null, null, model);
        }
    }

    @PostMapping("/profile/password")
    public String changePassword(@Valid @ModelAttribute("changePasswordForm") ChangePasswordForm form,
                                 BindingResult bindingResult,
                                 Model model) {
        if (bindingResult.hasErrors()) {
            return profilePage(null, null, null, model);
        }

        if (form.getConfirmPassword() != null && !form.getConfirmPassword().equals(form.getNewPassword())) {
            bindingResult.rejectValue("confirmPassword", "Mismatch", "Mật khẩu mới xác nhận không khớp");
            return profilePage(null, null, null, model);
        }

        Map<String, String> payload = Map.of(
                "currentPassword", form.getCurrentPassword(),
                "newPassword", form.getNewPassword()
        );

        try {
            gatewayClient.patch("/identity/users/myinfo/password", payload, JsonNode.class);
            return "redirect:/profile?passwordUpdated=true";
        } catch (Exception e) {
            model.addAttribute("errorMessage", "Đổi mật khẩu thất bại. Vui lòng kiểm tra lại mật khẩu hiện tại.");
            return profilePage(null, null, null, model);
        }
    }

    @PostMapping("/profile/role")
    public String updateRole(@RequestParam("role") String role,
                             HttpSession session) {
        Map<String, String> payload = Map.of("role", role);
        gatewayClient.patch("/identity/users/myinfo/role", payload, JsonNode.class);
        session.setAttribute("role", role);
        return "redirect:/profile?roleUpdated=true";
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
}
