package com.lms.courseservice.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InitiateMultipartRequest {

    @NotBlank
    String fileName;

    @NotBlank
    String fileType;

    @Pattern(regexp = "^[a-zA-Z0-9_\\-\\/]+$", message = "INVALID_FOLDER")
    String folder;

    @Min(1)
    @Max(500)
    int partCount;
}
