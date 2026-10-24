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
                // Các route công khai đã xác định rõ trong kế hoạch
                .requestMatchers(
                    "/",
                    "/courses",
                    "/courses/{id:[0-9]+}",
                    "/blog",
                    "/blog/{slug:[a-zA-Z0-9\\-]+}",
                    "/blog/tags",
                    "/login",
                    "/register",
                    "/forgot-password",
                    "/reset-password",
                    "/verify-email",
                    "/auth/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/webjars/**",
                    "/favicon.ico",
                    "/actuator/**"
                ).permitAll()
                // Phân quyền cho trang quản trị viên
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // Phân quyền cho studio giảng viên
                .requestMatchers("/instructor/**").hasAnyRole("TEACHER", "ADMIN")
                // Mọi route khác (bao gồm /profile/**, /settings/**, /my-courses, /courses/my/**, /learn/** và các route CẦN XÁC NHẬN) mặc định yêu cầu đăng nhập
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
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
