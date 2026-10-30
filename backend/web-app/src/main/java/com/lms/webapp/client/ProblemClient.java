package com.lms.webapp.client;

import com.lms.webapp.dto.ApiResponse;
import com.lms.webapp.dto.CodeSubmitDto;
import com.lms.webapp.dto.ProblemDto;
import com.lms.webapp.dto.SubmissionResultDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "problem-client", url = "${app.gateway.url:http://localhost:8080}")
public interface ProblemClient {

    @GetMapping("/api/v1/assignments/problems")
    ApiResponse<List<ProblemDto>> getProblems(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "difficulty", required = false) String difficulty
    );

    @GetMapping("/api/v1/assignments/problems/{id}")
    ApiResponse<ProblemDto> getProblemById(@PathVariable("id") Integer id);

    @PostMapping("/api/v1/assignments/problems/{id}/submit")
    ApiResponse<SubmissionResultDto> submitCode(
            @PathVariable("id") Integer id,
            @RequestBody CodeSubmitDto submitDto,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    );

    @PostMapping("/api/assignment/submissions")
    ApiResponse<SubmissionResultDto> submitToJudge(
            @RequestBody CodeSubmitDto submitDto,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    );

    @GetMapping("/api/assignment/submissions/{id}")
    ApiResponse<SubmissionResultDto> getSubmission(
            @PathVariable("id") String id
    );
}
