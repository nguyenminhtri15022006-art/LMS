package com.lms.webportal.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthRoleInterceptor authRoleInterceptor;

    public WebMvcConfig(AuthRoleInterceptor authRoleInterceptor) {
        this.authRoleInterceptor = authRoleInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authRoleInterceptor)
                .addPathPatterns("/admin/**", "/instructor/**", "/courses/my/**");
    }
}
