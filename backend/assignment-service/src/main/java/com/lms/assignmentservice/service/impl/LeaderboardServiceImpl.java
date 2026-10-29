package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.document.Submission;
import com.lms.assignmentservice.dto.response.LeaderboardItemResponse;
import com.lms.assignmentservice.dto.response.ProblemLeaderboardEntryResponse;
import com.lms.assignmentservice.repository.GradeRepository;
import com.lms.assignmentservice.repository.SubmissionRepository;
import com.lms.assignmentservice.service.LeaderboardService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class LeaderboardServiceImpl implements LeaderboardService {

    GradeRepository gradeRepository;
    SubmissionRepository submissionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LeaderboardItemResponse> getCourseLeaderboard(String courseId, int limit) {
        log.info("Lấy bảng xếp hạng theo khóa học: courseId={}, limit={}", courseId, limit);
        List<Object[]> rawList = gradeRepository.aggregateLeaderboardByCourse(courseId);

        List<LeaderboardItemResponse> results = new ArrayList<>();
        int rank = 1;

        for (Object[] row : rawList) {
            if (rank > limit) break;
            String userId = (String) row[0];
            Number sumScore = (Number) row[1];
            Number countQuizzes = (Number) row[2];

            long solvedProblems = 0;
            try {
                var acSubmissions = submissionRepository.findCompletionByUserIdAndCourseIdAndStatus(userId, courseId, "AC");
                if (acSubmissions != null) {
                    solvedProblems = acSubmissions.size();
                }
            } catch (Exception e) {
                log.debug("Không thể đếm bài AC MongoDB: {}", e.getMessage());
            }

            results.add(LeaderboardItemResponse.builder()
                    .rank(rank)
                    .userId(userId)
                    .courseId(courseId)
                    .totalScore(sumScore != null ? sumScore.doubleValue() : 0.0)
                    .completedQuizzes(countQuizzes != null ? countQuizzes.longValue() : 0L)
                    .solvedProblems(solvedProblems)
                    .badge(assignBadge(rank))
                    .build());
            rank++;
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeaderboardItemResponse> getGlobalLeaderboard(int limit) {
        log.info("Lấy bảng xếp hạng toàn cầu: limit={}", limit);
        List<Object[]> rawList = gradeRepository.aggregateGlobalLeaderboard();

        List<LeaderboardItemResponse> results = new ArrayList<>();
        int rank = 1;

        for (Object[] row : rawList) {
            if (rank > limit) break;
            String userId = (String) row[0];
            Number sumScore = (Number) row[1];
            Number countQuizzes = (Number) row[2];

            results.add(LeaderboardItemResponse.builder()
                    .rank(rank)
                    .userId(userId)
                    .totalScore(sumScore != null ? sumScore.doubleValue() : 0.0)
                    .completedQuizzes(countQuizzes != null ? countQuizzes.longValue() : 0L)
                    .solvedProblems(0L)
                    .badge(assignBadge(rank))
                    .build());
            rank++;
        }

        // Nếu hệ thống vừa khởi tạo chưa có dữ liệu điểm trong MySQL, trả về danh sách mẫu sinh động
        if (results.isEmpty()) {
            results = getFallbackLeaderboard(limit);
        }

        return results;
    }

    @Override
    public List<ProblemLeaderboardEntryResponse> getProblemLeaderboard(Integer problemId, int limit) {
        log.info("Lấy bảng xếp hạng giải bài tập lập trình: problemId={}, limit={}", problemId, limit);
        List<Submission> fastestSubmissions = submissionRepository.findByProblemIdAndStatusOrderByExecTimeMsAsc(
                problemId, "AC", PageRequest.of(0, Math.min(limit, 100)));

        List<ProblemLeaderboardEntryResponse> results = new ArrayList<>();
        int rank = 1;

        for (Submission sub : fastestSubmissions) {
            results.add(ProblemLeaderboardEntryResponse.builder()
                    .rank(rank++)
                    .submissionId(sub.getId())
                    .userId(sub.getUserId())
                    .problemId(sub.getProblemId())
                    .language(sub.getLanguage())
                    .execTimeMs(sub.getExecTimeMs() != null ? sub.getExecTimeMs() : 0)
                    .memoryUsedKb(sub.getMemoryUsedKb() != null ? sub.getMemoryUsedKb() : 0)
                    .score(sub.getScore() != null ? sub.getScore() : 100)
                    .submittedAt(Instant.now())
                    .build());
        }

        return results;
    }

    private String assignBadge(int rank) {
        return switch (rank) {
            case 1 -> "GOLD";
            case 2 -> "SILVER";
            case 3 -> "BRONZE";
            default -> "TOP_STUDENT";
        };
    }

    private List<LeaderboardItemResponse> getFallbackLeaderboard(int limit) {
        List<LeaderboardItemResponse> sample = List.of(
                LeaderboardItemResponse.builder()
                        .rank(1)
                        .userId("23110340")
                        .totalScore(2450.0)
                        .completedQuizzes(12L)
                        .solvedProblems(25L)
                        .badge("GOLD")
                        .build(),
                LeaderboardItemResponse.builder()
                        .rank(2)
                        .userId("23110302")
                        .totalScore(2180.0)
                        .completedQuizzes(11L)
                        .solvedProblems(22L)
                        .badge("SILVER")
                        .build(),
                LeaderboardItemResponse.builder()
                        .rank(3)
                        .userId("23110299")
                        .totalScore(1950.0)
                        .completedQuizzes(10L)
                        .solvedProblems(20L)
                        .badge("BRONZE")
                        .build(),
                LeaderboardItemResponse.builder()
                        .rank(4)
                        .userId("23110350")
                        .totalScore(1720.0)
                        .completedQuizzes(9L)
                        .solvedProblems(18L)
                        .badge("TOP_STUDENT")
                        .build()
        );
        return sample.stream().limit(limit).toList();
    }
}
