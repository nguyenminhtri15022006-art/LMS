package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.document.Submission;
import com.lms.assignmentservice.dto.response.LeaderboardItemResponse;
import com.lms.assignmentservice.dto.response.ProblemLeaderboardEntryResponse;
import com.lms.assignmentservice.repository.GradeRepository;
import com.lms.assignmentservice.repository.SubmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceImplTest {

    @Mock
    GradeRepository gradeRepository;

    @Mock
    SubmissionRepository submissionRepository;

    @InjectMocks
    LeaderboardServiceImpl leaderboardService;

    @Test
    void getCourseLeaderboardAggregatesAndAssignsRanksAndBadges() {
        String courseId = "course-1";
        List<Object[]> rawAggregate = List.of(
                new Object[]{"student-top1", 280.0, 3L},
                new Object[]{"student-top2", 250.0, 3L},
                new Object[]{"student-top3", 210.0, 2L},
                new Object[]{"student-top4", 180.0, 2L}
        );
        when(gradeRepository.aggregateLeaderboardByCourse(courseId)).thenReturn(rawAggregate);
        when(submissionRepository.findCompletionByUserIdAndCourseIdAndStatus(eq("student-top1"), eq(courseId), eq("AC")))
                .thenReturn(List.of(new Submission(), new Submission()));

        List<LeaderboardItemResponse> result = leaderboardService.getCourseLeaderboard(courseId, 10);

        assertThat(result).hasSize(4);
        assertThat(result.get(0).getRank()).isEqualTo(1);
        assertThat(result.get(0).getUserId()).isEqualTo("student-top1");
        assertThat(result.get(0).getBadge()).isEqualTo("GOLD");
        assertThat(result.get(0).getSolvedProblems()).isEqualTo(2);

        assertThat(result.get(1).getRank()).isEqualTo(2);
        assertThat(result.get(1).getBadge()).isEqualTo("SILVER");

        assertThat(result.get(2).getRank()).isEqualTo(3);
        assertThat(result.get(2).getBadge()).isEqualTo("BRONZE");

        assertThat(result.get(3).getRank()).isEqualTo(4);
        assertThat(result.get(3).getBadge()).isEqualTo("TOP_STUDENT");
    }

    @Test
    void getGlobalLeaderboardFallsBackToSampleWhenDbEmpty() {
        when(gradeRepository.aggregateGlobalLeaderboard()).thenReturn(List.of());

        List<LeaderboardItemResponse> result = leaderboardService.getGlobalLeaderboard(5);

        assertThat(result).isNotEmpty();
        assertThat(result.getFirst().getRank()).isEqualTo(1);
        assertThat(result.getFirst().getBadge()).isEqualTo("GOLD");
    }

    @Test
    void getProblemLeaderboardReturnsFastestAcceptedSubmissions() {
        Integer problemId = 1;
        Submission s1 = Submission.builder()
                .id("sub-1")
                .problemId(problemId)
                .userId("coder-fast")
                .language("CPP")
                .execTimeMs(8)
                .memoryUsedKb(1024)
                .score(100)
                .build();
        Submission s2 = Submission.builder()
                .id("sub-2")
                .problemId(problemId)
                .userId("coder-second")
                .language("JAVA")
                .execTimeMs(15)
                .memoryUsedKb(2048)
                .score(100)
                .build();

        when(submissionRepository.findByProblemIdAndStatusOrderByExecTimeMsAsc(eq(problemId), eq("AC"), any(PageRequest.class)))
                .thenReturn(List.of(s1, s2));

        List<ProblemLeaderboardEntryResponse> result = leaderboardService.getProblemLeaderboard(problemId, 10);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getRank()).isEqualTo(1);
        assertThat(result.get(0).getUserId()).isEqualTo("coder-fast");
        assertThat(result.get(0).getExecTimeMs()).isEqualTo(8);

        assertThat(result.get(1).getRank()).isEqualTo(2);
        assertThat(result.get(1).getUserId()).isEqualTo("coder-second");
        assertThat(result.get(1).getExecTimeMs()).isEqualTo(15);
    }
}
