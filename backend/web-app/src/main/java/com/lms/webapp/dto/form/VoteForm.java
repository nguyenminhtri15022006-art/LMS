package com.lms.webapp.dto.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VoteForm {

    @NotBlank(message = "Loại vote không được để trống")
    @Pattern(regexp = "^(UPVOTE|DOWNVOTE)$", message = "Loại vote phải là UPVOTE hoặc DOWNVOTE")
    String voteType;
}
