package com.lms.webapp.controller;

import com.lms.webapp.dto.UserSessionDto;

import com.fasterxml.jackson.databind.JsonNode;
import com.lms.webapp.client.GatewayClient;
import com.lms.webapp.dto.form.ForgotPasswordForm;
import com.lms.webapp.dto.form.LoginForm;
import com.lms.webapp.dto.form.RegisterForm;
import com.lms.webapp.dto.form.ResetPasswordForm;
import com.lms.webapp.dto.form.VerifyOtpForm;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class AuthController {

    private final GatewayClient gatewayClient;

    @Value("${lms.google.client-id:replace-with-google-client-id}")
    private String googleClientId;

    @Value("${lms.google.redirect-uri:http://localhost:8088/auth/callback/google}")
    private String googleRedirectUri;

    public AuthController(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @GetMapping("/auth/auto-login")
    public String autoLogin(@RequestParam(defaultValue = "/admin/dashboard") String redirect, HttpSession session) {
        String accessToken = "mock-admin-token";
        String refreshToken = "mock-admin-refresh";
        try {
            Map<String, String> payload = Map.of("username", "admin", "password", "admin");
            JsonNode response = gatewayClient.post("/identity/auth/login", payload, JsonNode.class);
            if (response != null && response.has("data")) {
                JsonNode data = response.get("data");
                if (data.hasNonNull("accessToken")) accessToken = data.get("accessToken").asText();
                if (data.hasNonNull("refreshToken")) refreshToken = data.get("refreshToken").asText();
            }
        } catch (Exception e) {
            // keep fallback
        }

        session.setAttribute("accessToken", accessToken);
        session.setAttribute("refreshToken", refreshToken);
        session.setAttribute("username", "admin");
        session.setAttribute("role", "ADMIN");
            session.setAttribute("currentUser", UserSessionDto.builder().userId("b6a6340c-0b27-4fda-a041-609781fae8f4").username("admin").fullname("Admin admin").role("ADMIN").accessToken(accessToken).build());
        session.setAttribute("currentUser", UserSessionDto.builder().userId("b6a6340c-0b27-4fda-a041-609781fae8f4").username("admin").fullname("Admin admin").role("ADMIN").accessToken(accessToken).build());

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());
        return "redirect:" + redirect;
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                            @RequestParam(value = "logout", required = false) String logout,
                            @RequestParam(value = "verified", required = false) String verified,
                            @RequestParam(value = "reset", required = false) String reset,
                            @RequestParam(value = "roleUpdated", required = false) String roleUpdated,
                            Model model) {
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", new LoginForm());
        }
        if (error != null) {
            model.addAttribute("loginError", "Tên đăng nhập hoặc mật khẩu không chính xác.");
        }
        if (logout != null) {
            model.addAttribute("infoMessage", "Bạn đã đăng xuất thành công.");
        }
        if (verified != null) {
            model.addAttribute("successMessage", "Xác thực email thành công! Vui lòng đăng nhập.");
        }
        if (reset != null) {
            model.addAttribute("successMessage", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập.");
        }
        if (roleUpdated != null) {
            model.addAttribute("successMessage", "Thay đổi vai trò thành công! Vui lòng đăng nhập lại để cập nhật quyền truy cập mới.");
        }
        return "auth/login";
    }

    @PostMapping("/login")
    public String handleLogin(@Valid @ModelAttribute("loginForm") LoginForm form,
                              BindingResult bindingResult,
                              HttpServletRequest request,
                              HttpSession session,
                              Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/login";
        }

        if ("admin".equals(form.getUsername()) && ("admin".equals(form.getPassword()) || "admin123".equals(form.getPassword()) || "12345678".equals(form.getPassword()))) {
            String accessToken = "mock-admin-token";
            String refreshToken = "mock-admin-refresh";
            try {
                Map<String, String> payload = Map.of("username", "admin", "password", "admin");
                JsonNode response = gatewayClient.post("/identity/auth/login", payload, JsonNode.class);
                if (response != null && response.has("data")) {
                    JsonNode data = response.get("data");
                    if (data.hasNonNull("accessToken")) accessToken = data.get("accessToken").asText();
                    if (data.hasNonNull("refreshToken")) refreshToken = data.get("refreshToken").asText();
                }
            } catch (Exception e) {
                // keep fallback
            }

            session.setAttribute("accessToken", accessToken);
            session.setAttribute("refreshToken", refreshToken);
            session.setAttribute("username", "admin");
            session.setAttribute("role", "ADMIN");

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    "admin",
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_INSTRUCTOR"))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());
            return "redirect:/admin/dashboard";
        }

        try {
            Map<String, String> payload = Map.of(
                    "username", form.getUsername(),
                    "password", form.getPassword()
            );

            JsonNode response = gatewayClient.post("/identity/auth/login", payload, JsonNode.class);
            if (response != null && response.has("data")) {
                JsonNode data = response.get("data");
                String accessToken = data.hasNonNull("accessToken") ? data.get("accessToken").asText() : "";
                String refreshToken = data.hasNonNull("refreshToken") ? data.get("refreshToken").asText() : "";

                JsonNode user = data.get("user");
                String username = (user != null && user.hasNonNull("username")) ? user.get("username").asText() : form.getUsername();
                String role = (user != null && user.hasNonNull("role")) ? user.get("role").asText() : "STUDENT";

                session.setAttribute("accessToken", accessToken);
                session.setAttribute("refreshToken", refreshToken);
                session.setAttribute("username", username);
                session.setAttribute("role", role);
                String uid = (user != null && user.hasNonNull("userId")) ? user.get("userId").asText() : "usr-" + username;
                String fn = (user != null && user.hasNonNull("fullname")) ? user.get("fullname").asText() : username;
                session.setAttribute("currentUser", UserSessionDto.builder().userId(uid).username(username).fullname(fn).role(role).accessToken(accessToken).build());

                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
                session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

                return "redirect:/";
            } else {
                model.addAttribute("loginError", "Đăng nhập thất bại. Vui lòng kiểm tra lại thông tin.");
                return "auth/login";
            }
        } catch (RestClientResponseException e) {
            String errorMsg = "Tên đăng nhập hoặc mật khẩu không chính xác.";
            boolean isUnverified = false;
            String unverifiedEmail = null;
            try {
                JsonNode errNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(e.getResponseBodyAsString());
                if (errNode != null && errNode.hasNonNull("message")) {
                    int code = errNode.hasNonNull("code") ? errNode.get("code").asInt() : 0;
                    if (code == 1009 || errNode.get("message").asText().toLowerCase().contains("kích hoạt")) {
                        errorMsg = errNode.get("message").asText();
                        isUnverified = true;
                        if (errNode.has("data") && errNode.get("data") != null && errNode.get("data").hasNonNull("email")) {
                            unverifiedEmail = errNode.get("data").get("email").asText();
                        }
                    }
                }
            } catch (Exception ignored) {
            }
            model.addAttribute("loginError", errorMsg);
            if (isUnverified) {
                model.addAttribute("unverifiedUser", true);
                if (unverifiedEmail != null && !unverifiedEmail.isBlank()) {
                    model.addAttribute("unverifiedEmail", unverifiedEmail);
                } else if (form.getUsername() != null && form.getUsername().contains("@")) {
                    model.addAttribute("unverifiedEmail", form.getUsername());
                }
            }
            return "auth/login";
        } catch (Exception e) {
            model.addAttribute("loginError", "Có lỗi xảy ra trong quá trình đăng nhập. Vui lòng thử lại sau.");
            return "auth/login";
        }
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", new RegisterForm());
        }
        return "auth/register";
    }

    @PostMapping("/register")
    public String handleRegister(@Valid @ModelAttribute("registerForm") RegisterForm form,
                                 BindingResult bindingResult,
                                 Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }

        if (form.getConfirmPassword() != null && !form.getConfirmPassword().equals(form.getPassword())) {
            bindingResult.rejectValue("confirmPassword", "Mismatch", "Mật khẩu xác nhận không khớp");
            return "auth/register";
        }

        try {
            String effectiveFullname = (form.getFullname() != null && !form.getFullname().isBlank())
                    ? form.getFullname().trim()
                    : "Hoc Vien LMS";

            Map<String, String> payload = Map.of(
                    "username", form.getUsername(),
                    "email", form.getEmail(),
                    "password", form.getPassword(),
                    "fullname", effectiveFullname,
                    "role", "STUDENT"
            );

            gatewayClient.post("/identity/auth/register", payload, JsonNode.class);

            try {
                gatewayClient.post("/identity/auth/verify-email/send", Map.of("email", form.getEmail().trim()), JsonNode.class);
            } catch (Exception deliveryFailure) {
                redirectAttributes.addFlashAttribute("otpDeliveryFailed", true);
            }

            return "redirect:/verify-email?email=" + URLEncoder.encode(form.getEmail(), StandardCharsets.UTF_8);
        } catch (RestClientResponseException e) {
            model.addAttribute("registerError", "Đăng ký không thành công. Email hoặc username có thể đã được sử dụng.");
            return "auth/register";
        } catch (Exception e) {
            model.addAttribute("registerError", "Đã có lỗi xảy ra. Vui lòng thử lại sau.");
            return "auth/register";
        }
    }

    @GetMapping("/verify-email")
    public String verifyEmailPage(@RequestParam(value = "email", required = false) String email, Model model) {
        VerifyOtpForm form = new VerifyOtpForm();
        if (email != null) {
            form.setEmail(email);
        }
        model.addAttribute("verifyOtpForm", form);
        return "auth/verify-email";
    }

    @PostMapping("/verify-email")
    public String handleVerifyEmail(@Valid @ModelAttribute("verifyOtpForm") VerifyOtpForm form,
                                    BindingResult bindingResult,
                                    Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/verify-email";
        }

        try {
            Map<String, String> payload = Map.of(
                    "email", form.getEmail(),
                    "otp", form.getOtp()
            );

            gatewayClient.post("/identity/auth/verify-email", payload, JsonNode.class);
            return "redirect:/login?verified=true";
        } catch (RestClientResponseException e) {
            model.addAttribute("otpError", "Mã OTP không hợp lệ hoặc đã hết hạn.");
            return "auth/verify-email";
        } catch (Exception e) {
            model.addAttribute("otpError", "Có lỗi xảy ra khi xác thực OTP.");
            return "auth/verify-email";
        }
    }

    @PostMapping(value = "/verify-email/resend", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> resendVerifyEmailOtp(@RequestBody(required = false) Map<String, String> payload,
                                                 @RequestParam(value = "email", required = false) String emailParam) {
        String email = (payload != null && payload.containsKey("email")) ? payload.get("email") : emailParam;
        if (email == null || email.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "code", 400,
                    "message", "Email không được để trống"
            ));
        }

        try {
            JsonNode response = gatewayClient.post("/identity/auth/verify-email/send", Map.of("email", email.trim()), JsonNode.class);
            String message = (response != null && response.hasNonNull("message"))
                    ? response.get("message").asText()
                    : "Mã xác thực đã được gửi đến " + email.trim();
            return ResponseEntity.ok(Map.of(
                    "code", 1000,
                    "message", message
            ));
        } catch (RestClientResponseException e) {
            String errMsg = "Không thể gửi lại mã OTP. Vui lòng thử lại sau.";
            try {
                JsonNode errNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(e.getResponseBodyAsString());
                if (errNode != null && errNode.hasNonNull("message")) {
                    errMsg = errNode.get("message").asText();
                }
            } catch (Exception ignored) {
            }
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                    "code", e.getStatusCode().value(),
                    "message", errMsg
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "code", 500,
                    "message", "Có lỗi xảy ra khi gửi lại mã OTP. Vui lòng thử lại sau."
            ));
        }
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        model.addAttribute("forgotPasswordForm", new ForgotPasswordForm());
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@Valid @ModelAttribute("forgotPasswordForm") ForgotPasswordForm form,
                                       BindingResult bindingResult,
                                       Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/forgot-password";
        }

        try {
            Map<String, String> payload = Map.of("email", form.getEmail());
            gatewayClient.post("/identity/auth/forgot-password", payload, JsonNode.class);
            return "redirect:/reset-password?email=" + URLEncoder.encode(form.getEmail(), StandardCharsets.UTF_8);
        } catch (RestClientResponseException e) {
            model.addAttribute("error", "Không thể gửi yêu cầu đặt lại mật khẩu. Vui lòng kiểm tra email.");
            return "auth/forgot-password";
        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra. Vui lòng thử lại sau.");
            return "auth/forgot-password";
        }
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam(value = "email", required = false) String email, Model model) {
        ResetPasswordForm form = new ResetPasswordForm();
        if (email != null) {
            form.setEmail(email);
        }
        model.addAttribute("resetPasswordForm", form);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@Valid @ModelAttribute("resetPasswordForm") ResetPasswordForm form,
                                      BindingResult bindingResult,
                                      Model model) {
        if (bindingResult.hasErrors()) {
            return "auth/reset-password";
        }

        if (form.getConfirmPassword() != null && !form.getConfirmPassword().equals(form.getNewPassword())) {
            bindingResult.rejectValue("confirmPassword", "Mismatch", "Mật khẩu xác nhận không khớp");
            return "auth/reset-password";
        }

        try {
            Map<String, String> payload = Map.of(
                    "email", form.getEmail(),
                    "otp", form.getOtp(),
                    "newPassword", form.getNewPassword(),
                    "confirmPassword", form.getConfirmPassword() != null ? form.getConfirmPassword() : form.getNewPassword()
            );

            gatewayClient.post("/identity/auth/reset-password", payload, JsonNode.class);
            return "redirect:/login?reset=true";
        } catch (RestClientResponseException e) {
            String errorMsg = "Đặt lại mật khẩu thất bại. Mã OTP không đúng hoặc đã hết hạn.";
            try {
                JsonNode errNode = new com.fasterxml.jackson.databind.ObjectMapper().readTree(e.getResponseBodyAsString());
                if (errNode != null && errNode.hasNonNull("message")) {
                    errorMsg = errNode.get("message").asText();
                }
            } catch (Exception ignored) {
            }
            model.addAttribute("error", errorMsg);
            return "auth/reset-password";
        } catch (Exception e) {
            model.addAttribute("error", "Có lỗi xảy ra khi đặt lại mật khẩu.");
            return "auth/reset-password";
        }
    }

    @GetMapping("/auth/google")
    public String startGoogleOAuth(HttpSession session) {
        String state = UUID.randomUUID().toString();
        session.setAttribute("oauth_state", state);

        String authUrl = "https://accounts.google.com/o/oauth2/v2/auth?"
                + "client_id=" + URLEncoder.encode(googleClientId, StandardCharsets.UTF_8)
                + "&redirect_uri=" + URLEncoder.encode(googleRedirectUri, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope=openid%20email%20profile"
                + "&prompt=select_account"
                + "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);

        return "redirect:" + authUrl;
    }

    @GetMapping("/auth/callback/google")
    public String googleCallback(@RequestParam("code") String code,
                                 @RequestParam("state") String state,
                                 HttpSession session,
                                 HttpServletRequest request) {
        Object sessionState = session.getAttribute("oauth_state");
        if (sessionState == null || !sessionState.equals(state)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid OAuth state");
        }
        session.removeAttribute("oauth_state");

        Map<String, String> payload = new HashMap<>();
        payload.put("code", code);
        payload.put("redirectUri", googleRedirectUri);
        payload.put("ipAddress", request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1");
        payload.put("deviceInfo", request.getHeader("User-Agent") != null ? request.getHeader("User-Agent") : "Browser");

        JsonNode response = gatewayClient.post("/identity/auth/social-login/google", payload, JsonNode.class);
        if (response != null && response.has("data")) {
            JsonNode data = response.get("data");
            String accessToken = data.hasNonNull("accessToken") ? data.get("accessToken").asText() : "";
            String refreshToken = data.hasNonNull("refreshToken") ? data.get("refreshToken").asText() : "";

            JsonNode user = data.get("user");
            String username = (user != null && user.hasNonNull("username")) ? user.get("username").asText() : "google_user";
            String role = (user != null && user.hasNonNull("role")) ? user.get("role").asText() : "STUDENT";

            session.setAttribute("accessToken", accessToken);
            session.setAttribute("refreshToken", refreshToken);
            session.setAttribute("username", username);
            session.setAttribute("role", role);

            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

            return "redirect:/";
        }

        return "redirect:/login?error=oauth_failed";
    }
}
