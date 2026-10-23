package com.lms.webportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntryDto {
    private int rank;
    private String userId;
    private String username;
    private String fullname;
    private int problemsSolved;
    private double quizAverage;
    private int totalScore;
    private String badge; // GOLD, SILVER, BRONZE, TOP_CODER
}
