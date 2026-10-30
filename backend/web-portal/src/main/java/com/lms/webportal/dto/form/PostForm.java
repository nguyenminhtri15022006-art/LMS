package com.lms.webportal.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PostForm {

    Long id;

    @NotBlank(message = "Tiêu đề bài viết không được để trống")
    @Size(min = 5, max = 255, message = "Tiêu đề phải từ 5 đến 255 ký tự")
    String title;

    @Size(max = 500, message = "Tóm tắt không được vượt quá 500 ký tự")
    String summary;

    @NotBlank(message = "Nội dung bài viết không được để trống")
    String content;

    @Builder.Default
    String status = "PUBLISHED"; // DRAFT hoặc PUBLISHED

    @Builder.Default
    List<Long> tagIds = new ArrayList<>();

    MultipartFile file;
}
