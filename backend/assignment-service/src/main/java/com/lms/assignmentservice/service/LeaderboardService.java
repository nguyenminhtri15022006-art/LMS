package com.lms.assignmentservice.service;

import com.lms.assignmentservice.dto.response.LeaderboardItemResponse;
import com.lms.assignmentservice.dto.response.ProblemLeaderboardEntryResponse;

import java.util.List;

public interface LeaderboardService {
    List<LeaderboardItemResponse> getCourseLeaderboard(String courseId, int limit);

    List<LeaderboardItemResponse> getGlobalLeaderboard(int limit);

    List<ProblemLeaderboardEntryResponse> getProblemLeaderboard(Integer problemId, int limit);
}
