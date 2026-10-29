package com.lms.webportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmitDto {
    private Integer quizId;
    @Builder.Default
    private Map<Integer, String> answers = new HashMap<>();
    private Integer timeSpentSeconds;
}
