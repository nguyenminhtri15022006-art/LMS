package com.lms.webportal.dto.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseForm {

    @NotBlank(message = "Tiêu đề khóa học không được để trống")
    @Size(max = 500, message = "Tiêu đề tối đa 500 ký tự")
    private String title;

    private String description;

    @Builder.Default
    private String status = "PUBLIC";

    @Min(value = 1, message = "Thời lượng khóa học tối thiểu là 1 giờ")
    private Integer duration;

    @Builder.Default
    private String level = "BEGINNER";

    @Builder.Default
    private List<Long> categoryIds = new ArrayList<>();

    @Builder.Default
    private List<String> learningPoints = new ArrayList<>();

    @Builder.Default
    private List<String> requirements = new ArrayList<>();

    private MultipartFile thumbnail;

    public String getLearningPointsText() {
        return (learningPoints != null && !learningPoints.isEmpty()) ? String.join("\n", learningPoints) : "";
    }

    public void setLearningPointsText(String text) {
        if (text != null && !text.isBlank()) {
            this.learningPoints = java.util.Arrays.stream(text.split("\\r?\\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        } else {
            this.learningPoints = new ArrayList<>();
        }
    }

    public String getRequirementsText() {
        return (requirements != null && !requirements.isEmpty()) ? String.join("\n", requirements) : "";
    }

    public void setRequirementsText(String text) {
        if (text != null && !text.isBlank()) {
            this.requirements = java.util.Arrays.stream(text.split("\\r?\\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        } else {
            this.requirements = new ArrayList<>();
        }
    }
}
