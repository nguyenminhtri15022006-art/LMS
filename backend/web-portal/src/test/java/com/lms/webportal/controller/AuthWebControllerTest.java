package com.lms.webportal.controller;

import com.lms.webportal.dto.UserSessionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /auth/login returns 200 and renders auth/login view")
    void testShowLoginForm() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeExists("loginDto"))
                .andExpect(content().string(containsString("Đăng Nhập EduLearn")));
    }

    @Test
    @DisplayName("POST /auth/login with valid student credentials succeeds and sets session")
    void testLoginSuccess() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .param("username", "student")
                        .param("password", "12345678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/"))
                .andExpect(request().sessionAttribute("currentUser", org.hamcrest.Matchers.notNullValue()));
    }

    @Test
    @DisplayName("POST /auth/login with invalid password shows error on auth/login")
    void testLoginFailure() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .param("username", "student")
                        .param("password", "123"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @DisplayName("GET /auth/register returns 200 and renders auth/register view")
    void testShowRegisterForm() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registerDto"))
                .andExpect(content().string(containsString("Tạo Tài Khoản Mới")));
    }

    @Test
    @DisplayName("POST /auth/register with valid input redirects to login with registered=true")
    void testRegisterSuccess() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .param("fullname", "Trần Thị B")
                        .param("username", "tranthib")
                        .param("email", "b@ute.edu.vn")
                        .param("password", "12345678")
                        .param("confirmPassword", "12345678")
                        .param("role", "STUDENT"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?registered=true"));
    }

    @Test
    @DisplayName("POST /auth/register with mismatch password shows error")
    void testRegisterPasswordMismatch() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .param("fullname", "Trần Thị B")
                        .param("username", "tranthib")
                        .param("email", "b@ute.edu.vn")
                        .param("password", "12345678")
                        .param("confirmPassword", "different888")
                        .param("role", "STUDENT"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    @DisplayName("GET /profile redirects to login when not authenticated")
    void testProfileUnauthenticated() throws Exception {
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?redirectUrl=/profile"));
    }

    @Test
    @DisplayName("GET /profile returns 200 and renders user/profile when authenticated")
    void testProfileAuthenticated() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", UserSessionDto.builder()
                .userId("usr-student-01")
                .username("student")
                .fullname("Nguyễn Văn Học Viên")
                .email("student@ute.edu.vn")
                .role("STUDENT")
                .build());

        mockMvc.perform(get("/profile").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("user/profile"))
                .andExpect(model().attributeExists("user"))
                .andExpect(content().string(containsString("Nguyễn Văn Học Viên")));
    }

    @Test
    @DisplayName("GET /auth/logout clears session and redirects to login")
    void testLogout() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", UserSessionDto.builder().username("student").build());

        mockMvc.perform(get("/auth/logout").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?logout=true"));
    }
}
