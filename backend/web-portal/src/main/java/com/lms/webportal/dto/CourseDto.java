package com.lms.webportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseDto {
    private String id;
    private String title;
    private String slug;
    private String description;
    private String shortDescription;
    private String category;
    private String difficulty; // BEGINNER, INTERMEDIATE, ADVANCED
    private String instructorName;
    private String instructorTitle;
    private String instructorAvatar;
    private String thumbnailUrl;
    private int totalLessons;
    private int durationHours;
    private double rating;
    private int reviewsCount;
    private int studentsEnrolled;
    private double price;
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    @Builder.Default
    private List<ChapterDto> chapters = new ArrayList<>();
    @Builder.Default
    private List<String> outcomes = new ArrayList<>();
    private boolean isEnrolled;
}
