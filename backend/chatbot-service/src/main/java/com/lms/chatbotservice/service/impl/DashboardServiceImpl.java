package com.lms.chatbotservice.service.impl;

import com.lms.chatbotservice.configuration.GeminiProperties;
import com.lms.chatbotservice.dto.response.DashboardStatsResponse;
import com.lms.chatbotservice.repository.ChatMessageRepository;
import com.lms.chatbotservice.repository.ChatSessionRepository;
import com.lms.chatbotservice.service.DashboardService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DashboardServiceImpl implements DashboardService {

    ChatSessionRepository sessionRepository;
    ChatMessageRepository messageRepository;
    GeminiProperties geminiProperties;

    @Override
    public DashboardStatsResponse getDashboardStats() {
        long sessions = 0;
        long messages = 0;
        try {
            sessions = sessionRepository.count();
            messages = messageRepository.count();
        } catch (Exception ignored) {
        }

        return DashboardStatsResponse.builder()
                .totalSessions(sessions)
                .totalMessages(messages)
                .activeUsersCount(Math.max(1, sessions))
                .llmProvider(geminiProperties.getProvider() != null ? geminiProperties.getProvider() : "DeepSeek / OpenCode")
                .modelName(geminiProperties.getModel() != null ? geminiProperties.getModel() : "deepseek-chat")
                .status("ONLINE")
                .build();
    }
}
