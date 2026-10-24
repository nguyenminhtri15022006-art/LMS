package com.lms.webapp.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
}
