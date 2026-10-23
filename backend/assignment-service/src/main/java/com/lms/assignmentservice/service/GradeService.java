package com.lms.assignmentservice.service;

import com.lms.assignmentservice.dto.response.CourseGradeSummaryResponse;
import com.lms.assignmentservice.dto.response.GradebookEntryResponse;

import java.util.List;

public interface GradeService {
    List<GradebookEntryResponse> getGradebookByCourse(String courseId);

    List<GradebookEntryResponse> getMyGrades(String courseId);

    CourseGradeSummaryResponse getCourseGradeSummary(String courseId);
}
