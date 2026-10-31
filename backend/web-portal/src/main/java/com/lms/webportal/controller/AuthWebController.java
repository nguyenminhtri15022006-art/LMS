package com.lms.webportal.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.lms.webportal.client.GatewayClient;
import com.lms.webportal.dto.LoginDto;
import com.lms.webportal.dto.RegisterDto;
import com.lms.webportal.dto.UserSessionDto;
import com.lms.webportal.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Controller
public class AuthWebController {

    private final AuthService authService;
    private final GatewayClient gatewayClient;

    @Autowired
    public AuthWebController(AuthService authService, @Autowired(required = false) GatewayClient gatewayClient) {
        this.authService = authService;
        this.gatewayClient = gatewayClient;
    }

    @GetMapping("/login")
    public String showLoginPageAlt(@RequestParam(value = "redirectUrl", required = false) String redirectUrl,
                                   Model model,
                                   HttpSession session) {
        return showLoginPage(redirectUrl, model, session);
    }

    @GetMapping("/auth/login")
    public String showLoginPage(@RequestParam(value = "redirectUrl", required = false) String redirectUrl,
                                Model model,
                                HttpSession session) {
        UserSessionDto currentUser = (UserSessionDto) session.getAttribute("currentUser");
        if (currentUser != null) {
            return "redirect:" + (redirectUrl != null && !redirectUrl.isBlank() ? redirectUrl : "/");
        }
        if (!model.containsAttribute("loginDto")) {
            model.addAttribute("loginDto", new LoginDto());
        }
        return "auth/login";
    }

    @PostMapping("/auth/login")
    public String handleLogin(@ModelAttribute("loginDto") LoginDto loginDto,
                              @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
                              HttpServletRequest request,
                              Model model) {
        try {
            UserSessionDto userSession = authService.authenticate(loginDto);

            HttpSession session = request.getSession(true);
            session.setAttribute("currentUser", userSession);
            session.setAttribute("user", userSession);
            session.setAttribute("accessToken", userSession.getAccessToken());
            session.setAttribute("username", userSession.getUsername());
            session.setAttribute("fullname", userSession.getFullname());
            session.setAttribute("role", userSession.getRole());
            session.setAttribute("userId", userSession.getUserId());

            if (redirectUrl != null && !redirectUrl.isBlank() && !redirectUrl.contains("login")) {
                return "redirect:" + redirectUrl;
            }

            if ("ADMIN".equalsIgnoreCase(userSession.getRole())) {
                return "redirect:/admin/dashboard";
            } else if ("INSTRUCTOR".equalsIgnoreCase(userSession.getRole())) {
                return "redirect:/instructor/courses";
            }
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/login";
        } catch (Exception e) {
            model.addAttribute("error", "Đăng nhập thất bại: Tên đăng nhập hoặc mật khẩu không chính xác.");
            return "auth/login";
        }
    }

    @GetMapping("/auth/register")
    public String showRegisterPage(Model model, HttpSession session) {
        UserSessionDto currentUser = (UserSessionDto) session.getAttribute("currentUser");
        if (currentUser != null) {
            return "redirect:/";
        }
        if (!model.containsAttribute("registerDto")) {
            model.addAttribute("registerDto", new RegisterDto());
        }
        return "auth/register";
    }

    @PostMapping("/auth/register")
    public String handleRegister(@ModelAttribute("registerDto") RegisterDto registerDto,
                                 Model model) {
        try {
            authService.register(registerDto);
            return "redirect:/auth/login?registered=true";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/register";
        } catch (Exception e) {
            model.addAttribute("error", "Đăng ký thất bại: " + e.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/auth/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("currentUser");
        session.removeAttribute("user");
        session.removeAttribute("accessToken");
        session.removeAttribute("username");
        session.removeAttribute("fullname");
        session.removeAttribute("role");
        session.removeAttribute("userId");
        session.invalidate();
        return "redirect:/auth/login?logout=true";
    }

    @GetMapping("/profile")
    public String showProfile(HttpSession session, Model model) {
        UserSessionDto currentUser = (UserSessionDto) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/auth/login?redirectUrl=/profile";
        }
        model.addAttribute("user", currentUser);
        model.addAttribute("activeTab", "courses");
        loadProfileData(currentUser, model);
        return "user/profile";
    }

    @GetMapping("/profile/my-courses")
    public String showMyCourses(HttpSession session, Model model) {
        UserSessionDto currentUser = (UserSessionDto) session.getAttribute("currentUser");
        if (currentUser == null) {
            return "redirect:/auth/login?redirectUrl=/profile/my-courses";
        }
        if ("INSTRUCTOR".equalsIgnoreCase(currentUser.getRole())) {
            return "redirect:/instructor/courses";
        }
        model.addAttribute("user", currentUser);
        model.addAttribute("activeTab", "courses");
        loadProfileData(currentUser, model);
        return "user/profile";
    }

    private void loadProfileData(UserSessionDto currentUser, Model model) {
        if (gatewayClient == null) {
            return;
        }
        String role = currentUser.getRole() != null ? currentUser.getRole().toUpperCase() : "";
        try {
            if ("INSTRUCTOR".equals(role)) {
                JsonNode statsRes = gatewayClient.get("/course/courses/my/stats", JsonNode.class);
                if (statsRes != null && statsRes.has("data")) {
                    model.addAttribute("instructorStats", statsRes.get("data"));
                }
                JsonNode coursesRes = gatewayClient.get("/course/courses/my?page=0&size=10", JsonNode.class);
                if (coursesRes != null && coursesRes.has("data")) {
                    model.addAttribute("myCourses", coursesRes.get("data"));
                }
            } else if ("ADMIN".equals(role)) {
                JsonNode allCoursesRes = gatewayClient.get("/course/courses?page=0&size=20", JsonNode.class);
                if (allCoursesRes != null && allCoursesRes.has("data")) {
                    model.addAttribute("allCourses", allCoursesRes.get("data"));
                }
            } else {
                JsonNode enrolledRes = gatewayClient.get("/course/courses/enrolled", JsonNode.class);
                if (enrolledRes != null && enrolledRes.has("data")) {
                    JsonNode enrolledData = enrolledRes.get("data");
                    model.addAttribute("enrolledCourses", enrolledData);

                    Map<String, JsonNode> courseProgressMap = new HashMap<>();
                    if (enrolledData.isArray()) {
                        for (JsonNode courseNode : enrolledData) {
                            String courseId = courseNode.path("id").asText();
                            if (!courseId.isBlank()) {
                                try {
                                    JsonNode pRes = gatewayClient.get("/course/courses/" + courseId + "/progress", JsonNode.class);
                                    if (pRes != null && pRes.has("data")) {
                                        courseProgressMap.put(courseId, pRes.get("data"));
                                    }
                                } catch (Exception ignored) {
                                }
                            }
                        }
                    }
                    model.addAttribute("courseProgressMap", courseProgressMap);
                }
            }
        } catch (Exception e) {
            log.warn("Lỗi tải thông tin profile: {}", e.getMessage());
        }
    }
}
