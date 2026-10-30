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
public class QuizResultDto {
    private Integer quizId;
    private String quizTitle;
    private Integer totalQuestions;
    private Integer correctCount;
    private Double score;
    private Boolean passed;
    private Integer timeSpentSeconds;
    private String submittedAt;
    @Builder.Default
    private List<QuestionResultDto> questionResults = new ArrayList<>();
}
