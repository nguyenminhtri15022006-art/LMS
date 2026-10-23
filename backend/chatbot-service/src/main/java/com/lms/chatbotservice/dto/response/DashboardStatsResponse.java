package com.lms.chatbotservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DashboardStatsResponse {
    long totalSessions;
    long totalMessages;
    long activeUsersCount;
    String llmProvider;
    String modelName;
    String status;
}
