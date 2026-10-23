package com.lms.assignmentservice.controller;

import com.lms.assignmentservice.dto.ApiResponse;
import com.lms.assignmentservice.dto.response.LeaderboardItemResponse;
import com.lms.assignmentservice.dto.response.ProblemLeaderboardEntryResponse;
import com.lms.assignmentservice.service.LeaderboardService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/leaderboard")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LeaderboardController {

    LeaderboardService leaderboardService;

    @GetMapping
    public ApiResponse<List<LeaderboardItemResponse>> getGlobalLeaderboard(
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return ApiResponse.<List<LeaderboardItemResponse>>builder()
                .data(leaderboardService.getGlobalLeaderboard(limit))
                .build();
    }

    @GetMapping("/courses/{courseId}")
    public ApiResponse<List<LeaderboardItemResponse>> getCourseLeaderboard(
            @PathVariable String courseId,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return ApiResponse.<List<LeaderboardItemResponse>>builder()
                .data(leaderboardService.getCourseLeaderboard(courseId, limit))
                .build();
    }

    @GetMapping("/problems/{problemId}")
    public ApiResponse<List<ProblemLeaderboardEntryResponse>> getProblemLeaderboard(
            @PathVariable Integer problemId,
            @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return ApiResponse.<List<ProblemLeaderboardEntryResponse>>builder()
                .data(leaderboardService.getProblemLeaderboard(problemId, limit))
                .build();
    }
}
