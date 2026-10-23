package com.lms.webportal.controller;

import com.lms.webportal.dto.LoginDto;
import com.lms.webportal.dto.RegisterDto;
import com.lms.webportal.dto.UserSessionDto;
import com.lms.webportal.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthWebController {

    @Autowired
    private AuthService authService;

    @GetMapping("/auth/login")
    public String showLoginForm(HttpSession session, Model model,
                                @RequestParam(value = "registered", required = false) Boolean registered,
                                @RequestParam(value = "logout", required = false) Boolean logout) {
        if (session.getAttribute("currentUser") != null) {
            return "redirect:/";
        }
        if (!model.containsAttribute("loginDto")) {
            model.addAttribute("loginDto", new LoginDto());
        }
        if (Boolean.TRUE.equals(registered)) {
            model.addAttribute("success", "Đăng ký tài khoản thành công! Vui lòng đăng nhập.");
        }
        if (Boolean.TRUE.equals(logout)) {
            model.addAttribute("info", "Bạn đã đăng xuất khỏi hệ thống.");
        }
        return "auth/login";
    }

    @PostMapping("/auth/login")
    public String handleLogin(@ModelAttribute("loginDto") LoginDto loginDto,
                              @RequestParam(value = "redirectUrl", required = false) String redirectUrl,
                              HttpSession session,
                              Model model) {
        try {
            UserSessionDto user = authService.authenticate(loginDto);
            session.setAttribute("currentUser", user);
            if (redirectUrl != null && !redirectUrl.isBlank() && redirectUrl.startsWith("/")) {
                return "redirect:" + redirectUrl;
            }
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "auth/login";
        } catch (Exception e) {
            model.addAttribute("error", "Đăng nhập thất bại: " + e.getMessage());
            return "auth/login";
        }
    }

    @GetMapping("/auth/register")
    public String showRegisterForm(HttpSession session, Model model) {
        if (session.getAttribute("currentUser") != null) {
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
            model.addAttribute("error", "Đăng ký không thành công: " + e.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/auth/logout")
    public String handleLogout(HttpSession session) {
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
        return "user/profile";
    }
}
