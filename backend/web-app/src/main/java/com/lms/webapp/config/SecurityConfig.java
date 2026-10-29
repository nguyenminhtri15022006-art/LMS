package com.lms.webapp.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                // Các route blog cá nhân và tạo/sửa nội dung yêu cầu đăng nhập trước khi xét slug public
                .requestMatchers(
                    "/blog/new",
                    "/blog/my-posts",
                    "/blog/posts/**",
                    "/blog/view/**"
                ).authenticated()
                // Phân quyền cho studio giảng viên và các route quản lý khóa học của giảng viên (hỗ trợ cả role chuẩn INSTRUCTOR và alias TEACHER)
                .requestMatchers(
                    "/courses/my",
                    "/courses/my/**",
                    "/instructor/**"
                ).hasAnyRole("INSTRUCTOR", "TEACHER", "ADMIN")
                // Các route học tập và ghi danh yêu cầu đăng nhập
                .requestMatchers(
                    "/courses/*/enroll",
                    "/courses/{id}/enroll",
                    "/my-courses",
                    "/my-courses/**",
                    "/learn/**"
                ).authenticated()
                // Các route công khai đã xác định rõ trong kế hoạch
                .requestMatchers(
                    "/",
                    "/courses",
                    "/courses/{id:[a-zA-Z0-9\\-]+}",
                    "/blog",
                    "/blog/{slug:[a-zA-Z0-9\\-]+}",
                    "/blog/tags",
                    "/login",
                    "/register",
                    "/forgot-password",
                    "/reset-password",
                    "/verify-email",
                    "/verify-email/**",
                    "/auth/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/webjars/**",
                    "/favicon.ico",
                    "/actuator/**"
                ).permitAll()
                // Phân quyền cho trang quản trị viên
                .requestMatchers("/admin", "/admin/**").hasRole("ADMIN")
                // Mọi route khác (bao gồm /profile/**, /settings/** và các route CẦN XÁC NHẬN) mặc định yêu cầu đăng nhập
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/security-login-process")
                .defaultSuccessUrl("/", false)
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public CookieSerializer cookieSerializer() {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName("JSESSIONID");
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        return serializer;
    }
}
