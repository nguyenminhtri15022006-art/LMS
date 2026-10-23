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
public class ProblemLeaderboardEntryResponse {
    Integer rank;
    String submissionId;
    String userId;
    Integer problemId;
    String language;
    Integer execTimeMs;
    Integer memoryUsedKb;
    Integer score;
    Instant submittedAt;
}
