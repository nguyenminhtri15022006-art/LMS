package com.lms.webapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProblemFilterDto {
    private String keyword;
    private String difficulty; // ALL, EASY, MEDIUM, HARD
    private String tag;
    private String status;     // ALL, SOLVED, UNSOLVED
}
