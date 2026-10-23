package com.lms.assignmentservice.service.impl;

import com.lms.assignmentservice.dto.response.CourseGradeSummaryResponse;
import com.lms.assignmentservice.dto.response.GradebookEntryResponse;
import com.lms.assignmentservice.entity.Grade;
import com.lms.assignmentservice.exception.AssignmentException;
import com.lms.assignmentservice.exception.ErrorCode;
import com.lms.assignmentservice.repository.GradeRepository;
import com.lms.assignmentservice.service.AssignmentAuthorizationService;
import com.lms.assignmentservice.service.GradeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class GradeServiceImpl implements GradeService {

    GradeRepository gradeRepository;
    AssignmentAuthorizationService authorizationService;

    @Override
    @Transactional(readOnly = true)
    public List<GradebookEntryResponse> getGradebookByCourse(String courseId) {
        log.info("Lấy bảng điểm Gradebook cho khóa học: courseId={}", courseId);
        if (!authorizationService.isAdminOrInstructor()) {
            throw new AssignmentException(ErrorCode.ACCESS_DENIED);
        }

        List<Grade> grades = gradeRepository.findByCourseId(courseId);
        return grades.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<GradebookEntryResponse> getMyGrades(String courseId) {
        String currentUserId = authorizationService.currentUserId();
        log.info("Lấy bảng điểm cá nhân cho học viên: userId={}, courseId={}", currentUserId, courseId);

        List<Grade> grades;
        if (courseId != null && !courseId.isBlank()) {
            grades = gradeRepository.findByUserIdAndCourseId(currentUserId, courseId);
        } else {
            grades = gradeRepository.findByUserId(currentUserId);
        }

        return grades.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CourseGradeSummaryResponse getCourseGradeSummary(String courseId) {
        List<GradebookEntryResponse> entries = getGradebookByCourse(courseId);

        Set<String> uniqueStudents = new HashSet<>();
        Set<Integer> uniqueQuizzes = new HashSet<>();
        double totalScoreSum = 0.0;

        for (GradebookEntryResponse entry : entries) {
            if (entry.getUserId() != null) uniqueStudents.add(entry.getUserId());
            if (entry.getQuizId() != null) uniqueQuizzes.add(entry.getQuizId());
            if (entry.getBestScore() != null) totalScoreSum += entry.getBestScore();
        }

        double averageScore = entries.isEmpty() ? 0.0 : Math.round((totalScoreSum / entries.size()) * 100.0) / 100.0;

        return CourseGradeSummaryResponse.builder()
                .courseId(courseId)
                .totalStudents(uniqueStudents.size())
                .totalQuizzes(uniqueQuizzes.size())
                .averageScore(averageScore)
                .grades(entries)
                .build();
    }

    private GradebookEntryResponse mapToResponse(Grade grade) {
        return GradebookEntryResponse.builder()
                .id(grade.getId())
                .userId(grade.getUserId())
                .courseId(grade.getCourseId())
                .quizId(grade.getQuiz() != null ? grade.getQuiz().getQuizId() : null)
                .quizTitle(grade.getQuiz() != null ? grade.getQuiz().getTitle() : null)
                .bestScore(grade.getBestScore())
                .attemptsCount(grade.getAttemptsCount())
                .lastAttemptAt(grade.getLastAttemptAt())
                .updatedAt(grade.getUpdatedAt())
                .build();
    }
}
