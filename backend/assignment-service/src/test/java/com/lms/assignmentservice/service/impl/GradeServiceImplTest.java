package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.dto.response.CourseGradeSummaryResponse;
import com.lms.assignmentservice.dto.response.GradebookEntryResponse;
import com.lms.assignmentservice.entity.Grade;
import com.lms.assignmentservice.entity.Quiz;
import com.lms.assignmentservice.repository.GradeRepository;
import com.lms.assignmentservice.service.AssignmentAuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GradeServiceImplTest {

    @Mock
    GradeRepository gradeRepository;

    @Mock
    AssignmentAuthorizationService authorizationService;

    @InjectMocks
    GradeServiceImpl gradeService;

    Quiz quiz;
    Grade grade1;
    Grade grade2;

    @BeforeEach
    void setUp() {
        quiz = Quiz.builder()
                .quizId(101)
                .title("Kiểm tra giữa kỳ Java & Spring")
                .courseId("course-cs101")
                .build();

        grade1 = Grade.builder()
                .id(1L)
                .userId("student-01")
                .courseId("course-cs101")
                .quiz(quiz)
                .bestScore(9.5f)
                .attemptsCount(2)
                .lastAttemptAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        grade2 = Grade.builder()
                .id(2L)
                .userId("student-02")
                .courseId("course-cs101")
                .quiz(quiz)
                .bestScore(8.0f)
                .attemptsCount(1)
                .lastAttemptAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void getGradebookByCourseRequiresInstructorOrAdminAndReturnsEntries() {
        String courseId = "course-cs101";
        when(authorizationService.isAdminOrInstructor()).thenReturn(true);
        when(gradeRepository.findByCourseId(courseId)).thenReturn(List.of(grade1, grade2));

        List<GradebookEntryResponse> result = gradeService.getGradebookByCourse(courseId);

        verify(authorizationService).isAdminOrInstructor();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getUserId()).isEqualTo("student-01");
        assertThat(result.get(0).getBestScore()).isEqualTo(9.5f);
        assertThat(result.get(0).getQuizTitle()).isEqualTo("Kiểm tra giữa kỳ Java & Spring");
        assertThat(result.get(1).getUserId()).isEqualTo("student-02");
        assertThat(result.get(1).getBestScore()).isEqualTo(8.0f);
    }

    @Test
    void getMyGradesReturnsGradesForCurrentStudent() {
        String studentId = "student-01";
        String courseId = "course-cs101";
        when(authorizationService.currentUserId()).thenReturn(studentId);
        when(gradeRepository.findByUserIdAndCourseId(studentId, courseId)).thenReturn(List.of(grade1));

        List<GradebookEntryResponse> result = gradeService.getMyGrades(courseId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUserId()).isEqualTo(studentId);
        assertThat(result.get(0).getBestScore()).isEqualTo(9.5f);
    }

    @Test
    void getCourseGradeSummaryComputesStatisticsCorrectly() {
        String courseId = "course-cs101";
        when(authorizationService.isAdminOrInstructor()).thenReturn(true);
        when(gradeRepository.findByCourseId(courseId)).thenReturn(List.of(grade1, grade2));

        CourseGradeSummaryResponse summary = gradeService.getCourseGradeSummary(courseId);

        assertThat(summary.getCourseId()).isEqualTo(courseId);
        assertThat(summary.getTotalStudents()).isEqualTo(2);
        assertThat(summary.getTotalQuizzes()).isEqualTo(1);
        // (9.5 + 8.0) / 2 = 8.75
        assertThat(summary.getAverageScore()).isEqualTo(8.75);
        assertThat(summary.getGrades()).hasSize(2);
    }
}
