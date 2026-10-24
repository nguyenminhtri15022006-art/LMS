package com.lms.chatbotservice.service;

import com.lms.chatbotservice.configuration.GeminiProperties;
import com.lms.chatbotservice.document.ChatMessage;
import com.lms.chatbotservice.document.ChatSession;
import com.lms.chatbotservice.dto.request.ChatRequest;
import com.lms.chatbotservice.dto.response.ChatMessageDto;
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
import reactor.core.publisher.Flux;

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

    @Test
    void getSessionMessagesReturnsChronologicalList() {
        String sessionId = "session-123";
        String userId = "student-01";
        ChatMessage m1 = ChatMessage.builder().id("m1").sessionId(sessionId).sender("USER").content("Hi").timestamp(Instant.now().minusSeconds(10)).build();
        ChatMessage m2 = ChatMessage.builder().id("m2").sessionId(sessionId).sender("AI").content("Hello!").timestamp(Instant.now()).build();

        when(messageRepository.findBySessionIdOrderByTimestampAsc(sessionId)).thenReturn(List.of(m1, m2));

        List<ChatMessageDto> messages = chatService.getSessionMessages(sessionId, userId);

        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getContent()).isEqualTo("Hi");
        assertThat(messages.get(1).getContent()).isEqualTo("Hello!");
    }

    @Test
    void deleteSessionOnlyDeletesIfUserOwnsSession() {
        String sessionId = "session-delete-1";
        String userId = "student-01";
        ChatSession session = ChatSession.builder()
                .id(sessionId)
                .userId(userId)
                .title("Delete me")
                .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        chatService.deleteSession(sessionId, userId);

        verify(messageRepository).deleteBySessionId(sessionId);
        verify(sessionRepository).delete(session);
    }

    @Test
    void deleteSessionDoesNotDeleteIfWrongUser() {
        String sessionId = "session-delete-2";
        String ownerId = "student-owner";
        String intruderId = "student-intruder";
        ChatSession session = ChatSession.builder()
                .id(sessionId)
                .userId(ownerId)
                .title("Keep me")
                .build();

        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        chatService.deleteSession(sessionId, intruderId);

        verify(messageRepository, never()).deleteBySessionId(anyString());
        verify(sessionRepository, never()).delete(any(ChatSession.class));
    }

    @Test
    void streamChatReturnsSseFluxWithChunks() {
        String userId = "student-stream";
        ChatRequest request = ChatRequest.builder()
                .message("Thuật toán Dijkstra")
                .build();

        ChatSession session = ChatSession.builder()
                .id("session-stream-1")
                .userId(userId)
                .title("Dijkstra")
                .build();

        ChatMessage savedAiMsg = ChatMessage.builder()
                .id("msg-stream-1")
                .sessionId("session-stream-1")
                .sender("AI")
                .content("Thuật toán Dijkstra")
                .timestamp(Instant.now())
                .build();

        when(rateLimiterService.tryAcquire(eq(userId), anyInt(), anyInt())).thenReturn(true);
        when(sessionRepository.save(any(ChatSession.class))).thenReturn(session);
        when(messageRepository.save(any(ChatMessage.class))).thenReturn(savedAiMsg);

        Flux<String> stream = chatService.streamChat(userId, request);
        List<String> chunks = stream.collectList().block();

        assertThat(chunks).isNotEmpty();
        String fullResponse = String.join("", chunks);
        assertThat(fullResponse).contains("Dijkstra");
    }

    @Test
    void streamChatRateLimitedReturnsPoliteErrorMessage() {
        String userId = "student-flood";
        ChatRequest request = ChatRequest.builder()
                .message("Stream spam")
                .build();

        when(rateLimiterService.tryAcquire(eq(userId), anyInt(), anyInt())).thenReturn(false);

        Flux<String> stream = chatService.streamChat(userId, request);
        List<String> chunks = stream.collectList().block();

        assertThat(chunks).isNotEmpty();
        String fullResponse = String.join("", chunks);
        assertThat(fullResponse).contains("quá nhiều câu hỏi");
        verifyNoInteractions(sessionRepository);
    }
}
