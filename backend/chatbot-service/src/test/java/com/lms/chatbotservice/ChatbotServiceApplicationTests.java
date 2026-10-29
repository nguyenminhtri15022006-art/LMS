package com.lms.chatbotservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.data.mongodb.uri=mongodb://localhost:27017/test",
        "spring.data.redis.host=localhost",
        "eureka.client.enabled=false"
})
class ChatbotServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
