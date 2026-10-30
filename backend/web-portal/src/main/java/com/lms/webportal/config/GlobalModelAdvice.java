package com.lms.webportal.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    public static class DummyCsrf {
        public String getParameterName() { return "_csrf"; }
        public String getToken() { return "dummy-csrf"; }
        public String getHeaderName() { return "X-CSRF-TOKEN"; }
    }

    private static final DummyCsrf CSRF = new DummyCsrf();

    @ModelAttribute("_csrf")
    public DummyCsrf csrf() {
        return CSRF;
    }
}
