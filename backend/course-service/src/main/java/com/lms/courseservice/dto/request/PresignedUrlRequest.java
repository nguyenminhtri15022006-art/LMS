package com.lms.courseservice.dto.request;

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
public class PresignedUrlRequest {

    @NotBlank
    String fileName;

    @NotBlank
    String fileType;

    @Pattern(regexp = "^[a-zA-Z0-9_\\-\\/]+$", message = "INVALID_FOLDER")
    String folder;
}
