package com.lms.webportal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class WebPortalApplicationTests {

    @Test
    @DisplayName("Context loads successfully")
    void contextLoads() {
    }
}
