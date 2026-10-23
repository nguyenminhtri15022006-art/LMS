package com.lms.assignmentservice.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GradebookEntryResponse {
    Long id;
    String userId;
    String courseId;
    Integer quizId;
    String quizTitle;
    Float bestScore;
    Integer attemptsCount;
    Instant lastAttemptAt;
    Instant updatedAt;
}
