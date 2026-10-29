package com.lms.webapp.dto.form;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LessonUpdateForm {

    private String title;

    @NotBlank(message = "Loại bài học không được để trống")
    private String lessonType;

    private String videoUrl;

    private Object content;

    private Integer durationSeconds;
}
