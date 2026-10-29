package com.lms.chatbotservice.service;

import com.lms.chatbotservice.configuration.GeminiProperties;
import com.lms.chatbotservice.dto.response.DashboardStatsResponse;
import com.lms.chatbotservice.repository.ChatMessageRepository;
import com.lms.chatbotservice.repository.ChatSessionRepository;
import com.lms.chatbotservice.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    ChatSessionRepository sessionRepository;

    @Mock
    ChatMessageRepository messageRepository;

    @Mock
    GeminiProperties geminiProperties;

    @InjectMocks
    DashboardServiceImpl dashboardService;

    @Test
    void getDashboardStatsReturnsAggregatedMetrics() {
        when(sessionRepository.count()).thenReturn(15L);
        when(messageRepository.count()).thenReturn(68L);
        when(geminiProperties.getProvider()).thenReturn("DeepSeek LLM");
        when(geminiProperties.getModel()).thenReturn("deepseek-v4-flash");

        DashboardStatsResponse stats = dashboardService.getDashboardStats();

        assertThat(stats).isNotNull();
        assertThat(stats.getTotalSessions()).isEqualTo(15L);
        assertThat(stats.getTotalMessages()).isEqualTo(68L);
        assertThat(stats.getLlmProvider()).isEqualTo("DeepSeek LLM");
        assertThat(stats.getStatus()).isEqualTo("ONLINE");
    }
}
