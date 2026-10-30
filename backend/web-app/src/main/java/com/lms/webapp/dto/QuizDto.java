package com.lms.webapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizDto {
    private Integer quizId;
    private String courseId;
    private String title;
    private String description;
    private Integer totalScore;
    private Integer passScore;
    private Integer maxAttempts;
    private Integer durationMinutes;
    private Integer questionCount;
    private String category;
    private String difficulty; // EASY, MEDIUM, HARD
    private Boolean completed;
    private Double bestScore;
    @Builder.Default
    private List<QuizQuestionDto> questions = new ArrayList<>();
}
