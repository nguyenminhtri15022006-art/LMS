package com.lms.webapp.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonDto {
    private String id;
    private String courseId;
    private String chapterId;
    private String title;
    private int orderIndex;
    private String type; // VIDEO, ARTICLE, CODE_LAB, QUIZ
    private int durationMinutes;
    private String videoUrl;
    private String content;
    private boolean isCompleted;
    private boolean isPreview;
}
