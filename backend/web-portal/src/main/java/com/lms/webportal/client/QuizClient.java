package com.lms.webportal.client;

import com.lms.webportal.dto.ApiResponse;
import com.lms.webportal.dto.QuizDto;
import com.lms.webportal.dto.QuizResultDto;
import com.lms.webportal.dto.QuizSubmitDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "quiz-client", url = "${app.gateway.url:http://localhost:8080}")
public interface QuizClient {

    @GetMapping("/api/v1/assignments/quizzes")
    ApiResponse<List<QuizDto>> getQuizzes(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "difficulty", required = false) String difficulty
    );

    @GetMapping("/api/v1/assignments/quizzes/{id}")
    ApiResponse<QuizDto> getQuizById(@PathVariable("id") Integer id);

    @PostMapping("/api/v1/assignments/quizzes/{id}/submit")
    ApiResponse<QuizResultDto> submitQuiz(
            @PathVariable("id") Integer id,
            @RequestBody QuizSubmitDto submitDto,
            @RequestHeader(value = "X-User-Id", required = false) String userId
    );
}
