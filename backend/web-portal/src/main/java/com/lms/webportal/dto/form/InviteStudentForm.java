package com.lms.webportal.dto.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteStudentForm {

    @NotBlank(message = "Email học viên không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;
}
