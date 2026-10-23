package com.lms.assignmentservice.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseGradeSummaryResponse {
    String courseId;
    Integer totalStudents;
    Integer totalQuizzes;
    Double averageScore;
    List<GradebookEntryResponse> grades;
}
