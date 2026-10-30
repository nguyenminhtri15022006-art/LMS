package com.lms.webapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodeSubmitDto {
    private Integer problemId;
    private String language; // CPP, JAVA, PYTHON
    private String sourceCode;
}
