package com.lms.courseservice.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseProgressResponse {
    private String courseId;
    private int totalLessons;
    private long completedLessons;
    private double percentComplete;
    private List<String> completedLessonIds;
}
