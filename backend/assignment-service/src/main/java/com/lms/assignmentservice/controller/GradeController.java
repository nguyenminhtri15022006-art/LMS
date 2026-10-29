package com.lms.assignmentservice.controller;

import com.lms.assignmentservice.dto.ApiResponse;
import com.lms.assignmentservice.dto.response.CourseGradeSummaryResponse;
import com.lms.assignmentservice.dto.response.GradebookEntryResponse;
import com.lms.assignmentservice.service.GradeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/grades")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GradeController {

    GradeService gradeService;

    @GetMapping("/course/{courseId}")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ApiResponse<List<GradebookEntryResponse>> getGradebookByCourse(@PathVariable String courseId) {
        return ApiResponse.<List<GradebookEntryResponse>>builder()
                .data(gradeService.getGradebookByCourse(courseId))
                .build();
    }

    @GetMapping("/course/{courseId}/summary")
    @PreAuthorize("hasAnyRole('INSTRUCTOR', 'ADMIN')")
    public ApiResponse<CourseGradeSummaryResponse> getCourseGradeSummary(@PathVariable String courseId) {
        return ApiResponse.<CourseGradeSummaryResponse>builder()
                .data(gradeService.getCourseGradeSummary(courseId))
                .build();
    }

    @GetMapping("/my")
    public ApiResponse<List<GradebookEntryResponse>> getMyGrades(
            @RequestParam(value = "courseId", required = false) String courseId) {
        return ApiResponse.<List<GradebookEntryResponse>>builder()
                .data(gradeService.getMyGrades(courseId))
                .build();
    }
}
