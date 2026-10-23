package com.lms.chatbotservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatMessageDto {
    String messageId;
    String sessionId;
    String sender;
    String content;
    Instant timestamp;
}
