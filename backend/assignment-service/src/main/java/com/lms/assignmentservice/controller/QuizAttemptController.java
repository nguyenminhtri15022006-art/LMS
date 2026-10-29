package com.lms.assignmentservice.controller;

import com.lms.assignmentservice.dto.ApiResponse;
import com.lms.assignmentservice.dto.request.AutoSaveAttemptRequest;
import com.lms.assignmentservice.dto.request.PracticeAnswerRequest;
import com.lms.assignmentservice.dto.request.SubmitAttemptRequest;
import com.lms.assignmentservice.dto.response.AttemptHistoryResponse;
import com.lms.assignmentservice.dto.response.PracticeAnswerResponse;
import com.lms.assignmentservice.dto.response.QuizAttemptResponse;
import com.lms.assignmentservice.dto.response.QuizResultResponse;
import com.lms.assignmentservice.service.QuizAttemptService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/quizzes/{quizId}/attempts")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QuizAttemptController {

    QuizAttemptService quizAttemptService;

    @PostMapping
    public ApiResponse<QuizAttemptResponse> startOrResumeAttempt(@PathVariable Integer quizId) {
        return ApiResponse.<QuizAttemptResponse>builder()
                .data(quizAttemptService.startOrResumeAttempt(quizId))
                .build();
    }

    @GetMapping("/{attemptId}")
    public ApiResponse<QuizAttemptResponse> getInProgressAttempt(
            @PathVariable Integer quizId,
            @PathVariable Long attemptId) {
        return ApiResponse.<QuizAttemptResponse>builder()
                .data(quizAttemptService.getInProgressAttempt(quizId, attemptId))
                .build();
    }

    @PutMapping("/{attemptId}/auto-save")
    public ApiResponse<Void> autoSaveAttempt(
            @PathVariable Integer quizId,
            @PathVariable Long attemptId,
            @Valid @RequestBody AutoSaveAttemptRequest request) {
        quizAttemptService.autoSaveAttempt(quizId, attemptId, request);
        return ApiResponse.<Void>builder()
                .message("Auto-save successful")
                .build();
    }

    @PostMapping("/{attemptId}/practice-check")
    public ApiResponse<PracticeAnswerResponse> checkPracticeAnswer(
            @PathVariable Integer quizId,
            @PathVariable Long attemptId,
            @Valid @RequestBody PracticeAnswerRequest request) {
        return ApiResponse.<PracticeAnswerResponse>builder()
                .data(quizAttemptService.checkPracticeAnswer(quizId, attemptId, request))
                .build();
    }

    @PostMapping("/{attemptId}/submit")
    public ApiResponse<QuizResultResponse> submitAttempt(
            @PathVariable Integer quizId,
            @PathVariable Long attemptId,
            @Valid @RequestBody SubmitAttemptRequest request) {
        return ApiResponse.<QuizResultResponse>builder()
                .data(quizAttemptService.submitAttempt(quizId, attemptId, request))
                .build();
    }

    @GetMapping("/{attemptId}/result")
    public ApiResponse<QuizResultResponse> getQuizResult(
            @PathVariable Integer quizId,
            @PathVariable Long attemptId) {
        return ApiResponse.<QuizResultResponse>builder()
                .data(quizAttemptService.getQuizResult(quizId, attemptId))
                .build();
    }

    @GetMapping
    public ApiResponse<Page<AttemptHistoryResponse>> getAttemptHistory(
            @PathVariable Integer quizId,
            @RequestParam(value = "userId", required = false) String userId,
            @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponse.<Page<AttemptHistoryResponse>>builder()
                .data(quizAttemptService.getAttemptHistory(quizId, userId, pageable))
                .build();
    }
}
