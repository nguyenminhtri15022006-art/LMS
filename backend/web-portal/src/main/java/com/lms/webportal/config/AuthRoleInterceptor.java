package com.lms.webportal.config;

import com.lms.webportal.dto.UserSessionDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthRoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();
        HttpSession session = request.getSession(false);
        UserSessionDto user = (session != null) ? (UserSessionDto) session.getAttribute("currentUser") : null;

        if (uri.startsWith("/admin")) {
            if (user == null) {
                response.sendRedirect("/auth/login?redirectUrl=" + uri);
                return false;
            }
            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                response.sendRedirect("/?error=unauthorized");
                return false;
            }
        } else if (uri.startsWith("/instructor") || uri.startsWith("/courses/my")) {
            if (user == null) {
                response.sendRedirect("/auth/login?redirectUrl=" + uri);
                return false;
            }
            if (!"INSTRUCTOR".equalsIgnoreCase(user.getRole()) && !"ADMIN".equalsIgnoreCase(user.getRole())) {
                response.sendRedirect("/?error=unauthorized");
                return false;
            }
        }

        return true;
    }
}
