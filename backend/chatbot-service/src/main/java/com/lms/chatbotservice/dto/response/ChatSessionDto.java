package com.lms.chatbotservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatSessionDto {
    String sessionId;
    String title;
    Instant createdAt;
    Instant updatedAt;
}
