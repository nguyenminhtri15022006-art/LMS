package com.lms.chatbotservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatResponse {
    String sessionId;
    String messageId;
    String reply;
    @Builder.Default
    String sender = "AI";
    @Builder.Default
    Instant timestamp = Instant.now();
}
