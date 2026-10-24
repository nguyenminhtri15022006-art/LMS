package com.lms.webapp.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired(required = false)
    private SecurityConfig securityConfig;

    @Test
    void csrfProtectionEnabled() throws Exception {
        assertThat(securityConfig).isNotNull();

        // Request POST / không đính kèm CSRF token phải bị từ chối với HTTP 403 Forbidden
        mockMvc.perform(post("/"))
                .andExpect(status().isForbidden());
    }

    @Test
    void sessionCookieHasHttpOnlyAndSameSiteLax() {
        // Red 1: Gọi securityConfig.cookieSerializer() khi chưa khai báo method trong SecurityConfig
        CookieSerializer serializer = securityConfig.cookieSerializer();
        assertThat(serializer).isNotNull();

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        CookieSerializer.CookieValue cookieValue = new CookieSerializer.CookieValue(request, response, "test-session-12345");
        serializer.writeCookieValue(cookieValue);

        String setCookie = response.getHeader("Set-Cookie");
        assertThat(setCookie).isNotNull();
        assertThat(setCookie).containsIgnoringCase("HttpOnly");
        assertThat(setCookie).containsIgnoringCase("SameSite=Lax");
    }

    @Test
    void publicRoutesAccessibleWithoutLogin() throws Exception {
        // Trang chủ trả về 200 OK
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        // Trang login được permitAll (chưa có controller Phase 2 nên MVC trả về 404, không bị redirect 302)
        mockMvc.perform(get("/login"))
                .andExpect(status().isNotFound());

        // Danh mục khóa học công khai không bị redirect login
        mockMvc.perform(get("/courses"))
                .andExpect(status().isNotFound()); // Chưa có controller nhưng được Security cho phép (không 302)

        // Chi tiết khóa học công khai theo ID không bị redirect login
        mockMvc.perform(get("/courses/123"))
                .andExpect(status().isNotFound());

        // Blog công khai không bị redirect login
        mockMvc.perform(get("/blog"))
                .andExpect(status().isNotFound());

        // Chi tiết blog theo slug không bị redirect login
        mockMvc.perform(get("/blog/bai-viet-hay-2026"))
                .andExpect(status().isNotFound());

        // Thẻ tags blog công khai không bị redirect login
        mockMvc.perform(get("/blog/tags"))
                .andExpect(status().isNotFound());
    }

    @Test
    void protectedRoutesRedirectToLoginWhenAnonymous() throws Exception {
        // Trang cá nhân phải yêu cầu đăng nhập
        mockMvc.perform(get("/profile"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // Khóa học của tôi (giảng viên) phải yêu cầu đăng nhập
        mockMvc.perform(get("/courses/my"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // Khóa học đã ghi danh (học viên) phải yêu cầu đăng nhập
        mockMvc.perform(get("/my-courses"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // Ghi danh khóa học phải yêu cầu đăng nhập
        mockMvc.perform(get("/courses/123/enroll"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // Phòng học learning hub phải yêu cầu đăng nhập
        mockMvc.perform(get("/learn/123/lessons/456"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // Tạo bài viết mới phải yêu cầu đăng nhập
        mockMvc.perform(get("/blog/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // Bài viết của tôi phải yêu cầu đăng nhập
        mockMvc.perform(get("/blog/my-posts"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // Route chưa xác định (CẦN XÁC NHẬN) mặc định phải yêu cầu đăng nhập
        mockMvc.perform(get("/settings/security"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // Trang admin phải chuyển hướng đăng nhập khi anonymous
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));

        // Trang giảng viên studio phải chuyển hướng đăng nhập khi anonymous
        mockMvc.perform(get("/instructor/courses"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void roleRestrictedRoutesForbiddenForInsufficientRole() throws Exception {
        // User với role STUDENT truy cập /admin phải bị 403 Forbidden
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());

        // User với role STUDENT truy cập /instructor/** phải bị 403 Forbidden
        mockMvc.perform(get("/instructor/courses"))
                .andExpect(status().isForbidden());

        // User với role STUDENT truy cập /courses/my (quản lý khóa học của giảng viên) phải bị 403 Forbidden
        mockMvc.perform(get("/courses/my"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "INSTRUCTOR")
    void instructorRoutesAccessibleForInstructorRole() throws Exception {
        // Giảng viên với role thực tế INSTRUCTOR (ROLE_INSTRUCTOR trong token) được phép truy cập /courses/my
        mockMvc.perform(get("/courses/my"))
                .andExpect(status().isNotFound()); // Security cho phép qua, đến MVC trả về 404 vì controller thuộc phase sau

        // Giảng viên với role INSTRUCTOR được phép truy cập /instructor/courses
        mockMvc.perform(get("/instructor/courses"))
                .andExpect(status().isNotFound());

        // Giảng viên không có quyền truy cập /admin -> 403 Forbidden
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "TEACHER")
    void instructorRoutesAccessibleForTeacherRoleAlias() throws Exception {
        // Hỗ trợ tương thích alias TEACHER (ROLE_TEACHER) được phép truy cập các route quản lý giảng viên
        mockMvc.perform(get("/courses/my"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/instructor/courses"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminHasAccessToAllAdminAndInstructorRoutes() throws Exception {
        // Quản trị viên ADMIN có quyền truy cập cả trang admin lẫn các route giảng viên
        mockMvc.perform(get("/admin"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/courses/my"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/instructor/courses"))
                .andExpect(status().isNotFound());
    }
}
