package com.lms.webportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseResultDto {
    private Long testId;
    private String status; // AC, WA, TLE, MLE, RE, CE
    private Integer timeMs;
    private Integer memoryKb;
    private String input;
    private String expectedOutput;
    private String outputSnippet;
    private Boolean hidden;
}
