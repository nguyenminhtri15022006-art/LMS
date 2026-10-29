package com.lms.assignmentservice.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LeaderboardItemResponse {
    Integer rank;
    String userId;
    String courseId;
    Double totalScore;
    Long completedQuizzes;
    Long solvedProblems;
    String badge; // "GOLD", "SILVER", "BRONZE", "TOP_STUDENT"
}
