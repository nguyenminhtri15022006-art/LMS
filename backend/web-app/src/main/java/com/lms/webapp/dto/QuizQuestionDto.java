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
public class QuizQuestionDto {
    private Integer questionId;
    private String content;
    private String explanation;
    private String type; // SINGLE, MULTIPLE
    private Integer score;
    private String category;
    private String difficulty; // EASY, MEDIUM, HARD
    @Builder.Default
    private List<QuizOptionDto> options = new ArrayList<>();
    private String correctAnswer;
}
