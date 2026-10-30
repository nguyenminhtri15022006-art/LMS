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
public class SubmissionResultDto {
    private String submissionId;
    private Integer problemId;
    private String userId;
    private String language;
    private String status; // AC, WA, TLE, MLE, CE, RE, PENDING
    private Integer score;
    private Integer execTimeMs;
    private Integer memoryUsedKb;
    private String submittedAt;
    private String sourceCode;
    private String compileError;
    @Builder.Default
    private List<TestCaseResultDto> testCaseResults = new ArrayList<>();
}
