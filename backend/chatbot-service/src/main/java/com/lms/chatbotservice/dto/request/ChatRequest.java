package com.lms.chatbotservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatRequest {
    String sessionId;

    @NotBlank(message = "Nội dung câu hỏi không được để trống")
    String message;

    String context; // Vd: problem-1, quiz-10, course-cs101
}
