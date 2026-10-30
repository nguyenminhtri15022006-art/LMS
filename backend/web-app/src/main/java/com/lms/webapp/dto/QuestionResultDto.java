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
public class QuestionResultDto {
    private Integer questionId;
    private String content;
    private String selectedOption;
    private String correctOption;
    private Boolean isCorrect;
    private String explanation;
    @Builder.Default
    private List<QuizOptionDto> options = new ArrayList<>();
}
