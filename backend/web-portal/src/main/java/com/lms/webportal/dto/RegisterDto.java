package com.lms.webportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterDto {
    private String fullname;
    private String username;
    private String email;
    private String password;
    private String confirmPassword;
    private String role; // STUDENT or INSTRUCTOR
}
