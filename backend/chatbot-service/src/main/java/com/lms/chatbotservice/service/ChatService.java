package com.lms.chatbotservice.service;

import com.lms.chatbotservice.dto.request.ChatRequest;
import com.lms.chatbotservice.dto.response.ChatMessageDto;
import com.lms.chatbotservice.dto.response.ChatResponse;
import com.lms.chatbotservice.dto.response.ChatSessionDto;
import reactor.core.publisher.Flux;

import java.util.List;

public interface ChatService {
    ChatResponse chat(String userId, ChatRequest request);

    Flux<String> streamChat(String userId, ChatRequest request);

    List<ChatSessionDto> getUserSessions(String userId);

    List<ChatMessageDto> getSessionMessages(String sessionId, String userId);

    void deleteSession(String sessionId, String userId);
}
