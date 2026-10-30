package com.lms.webapp.client;

import com.lms.webapp.dto.ApiResponse;
import com.lms.webapp.dto.ChatRequestDto;
import com.lms.webapp.dto.ChatResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "chatbot-client", url = "${app.gateway.url:http://localhost:8080}")
public interface ChatbotClient {

    @PostMapping("/api/v1/chat")
    ApiResponse<ChatResponseDto> sendMessage(@RequestBody ChatRequestDto request);
}
