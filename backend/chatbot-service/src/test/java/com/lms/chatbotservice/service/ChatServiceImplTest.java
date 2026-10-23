package com.lms.chatbotservice.service;

import com.lms.chatbotservice.configuration.GeminiProperties;
import com.lms.chatbotservice.document.ChatMessage;
import com.lms.chatbotservice.document.ChatSession;
import com.lms.chatbotservice.dto.request.ChatRequest;
import com.lms.chatbotservice.dto.response.ChatResponse;
import com.lms.chatbotservice.dto.response.ChatSessionDto;
import com.lms.chatbotservice.repository.ChatMessageRepository;
import com.lms.chatbotservice.repository.ChatSessionRepository;
import com.lms.chatbotservice.service.impl.ChatServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceImplTest {

    @Mock
    ChatSessionRepository sessionRepository;

    @Mock
    ChatMessageRepository messageRepository;

    @Mock
    RateLimiterService rateLimiterService;

    @Mock
    GeminiProperties geminiProperties;

    @InjectMocks
    ChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void chatCreatesNewSessionAndReturnsTwoSumExplanation() {
        String userId = "student-01";
        ChatRequest request = ChatRequest.builder()
                .message("Hướng dẫn giải bài toán Two Sum")
                .build();

        ChatSession session = ChatSession.builder()
                .id("session-101")
                .userId(userId)
                .title("Hướng dẫn giải bài toán Two Sum")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ChatMessage savedAiMsg = ChatMessage.builder()
                .id("msg-ai-1")
                .sessionId("session-101")
                .sender("AI")
                .content("Two Sum")
                .timestamp(Instant.now())
                .build();

        when(rateLimiterService.tryAcquire(eq(userId), anyInt(), anyInt())).thenReturn(true);
        when(sessionRepository.save(any(ChatSession.class))).thenReturn(session);
        when(messageRepository.save(any(ChatMessage.class))).thenReturn(savedAiMsg);

        ChatResponse response = chatService.chat(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getSessionId()).isEqualTo("session-101");
        assertThat(response.getSender()).isEqualTo("AI");
        assertThat(response.getReply()).contains("Two Sum");
        assertThat(response.getReply()).contains("HashMap");
    }

    @Test
    void chatRateLimitExceededReturnsPoliteWarning() {
        String userId = "student-spam";
        ChatRequest request = ChatRequest.builder()
                .message("Câu hỏi liên tục")
                .build();

        when(rateLimiterService.tryAcquire(eq(userId), anyInt(), anyInt())).thenReturn(false);

        ChatResponse response = chatService.chat(userId, request);

        assertThat(response).isNotNull();
        assertThat(response.getReply()).contains("quá nhiều câu hỏi");
        verifyNoInteractions(sessionRepository);
    }

    @Test
    void getUserSessionsReturnsMappedDtos() {
        String userId = "student-01";
        ChatSession session = ChatSession.builder()
                .id("session-1")
                .userId(userId)
                .title("Học Spring Boot")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(sessionRepository.findByUserIdOrderByUpdatedAtDesc(userId)).thenReturn(List.of(session));

        List<ChatSessionDto> sessions = chatService.getUserSessions(userId);

        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).getSessionId()).isEqualTo("session-1");
        assertThat(sessions.get(0).getTitle()).isEqualTo("Học Spring Boot");
    }
}
