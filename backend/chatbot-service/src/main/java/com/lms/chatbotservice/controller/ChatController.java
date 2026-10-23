package com.lms.chatbotservice.controller;

import com.lms.chatbotservice.dto.request.ChatRequest;
import com.lms.chatbotservice.dto.response.ApiResponse;
import com.lms.chatbotservice.dto.response.ChatMessageDto;
import com.lms.chatbotservice.dto.response.ChatResponse;
import com.lms.chatbotservice.dto.response.ChatSessionDto;
import com.lms.chatbotservice.service.ChatService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ChatController {

    ChatService chatService;

    @PostMapping
    public ApiResponse<ChatResponse> chat(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody ChatRequest request) {
        return ApiResponse.success(chatService.chat(userId, request));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Valid @RequestBody ChatRequest request) {
        return chatService.streamChat(userId, request);
    }

    @GetMapping("/sessions")
    public ApiResponse<List<ChatSessionDto>> getUserSessions(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ApiResponse.success(chatService.getUserSessions(userId));
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<ChatMessageDto>> getSessionMessages(
            @PathVariable String sessionId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        return ApiResponse.success(chatService.getSessionMessages(sessionId, userId));
    }

    @DeleteMapping("/sessions/{sessionId}")
    public ApiResponse<Void> deleteSession(
            @PathVariable String sessionId,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        chatService.deleteSession(sessionId, userId);
        return ApiResponse.<Void>builder()
                .code(1000)
                .message("Session deleted successfully")
                .build();
    }
}
