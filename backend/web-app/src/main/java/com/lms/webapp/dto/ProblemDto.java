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
public class ProblemDto {
    private Integer problemId;
    private String title;
    private String slug;
    private String description;
    private String difficulty; // EASY, MEDIUM, HARD
    private Integer timeLimitMs;
    private Integer memoryLimitMb;
    @Builder.Default
    private List<String> allowedLangs = new ArrayList<>();
    private Integer score;
    private Integer acceptanceRate;
    private Integer totalSubmit;
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    @Builder.Default
    private List<ProblemExampleDto> examples = new ArrayList<>();
    private Boolean completed;
    private String defaultTemplateCpp;
    private String defaultTemplateJava;
    private String defaultTemplatePython;
}
