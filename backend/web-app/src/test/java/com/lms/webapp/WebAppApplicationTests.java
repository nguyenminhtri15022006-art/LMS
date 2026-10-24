package com.lms.webapp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class WebAppApplicationTests {

    @Test
    void contextLoads() {
        // Kiểm tra Spring ApplicationContext khởi động thành công với cấu hình chuẩn @SpringBootApplication
    }
}
