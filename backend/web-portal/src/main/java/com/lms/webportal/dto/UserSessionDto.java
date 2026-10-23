package com.lms.webportal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSessionDto implements Serializable {
    private String userId;
    private String username;
    private String fullname;
    private String email;
    private String role; // STUDENT, INSTRUCTOR, ADMIN
    private String avatarUrl;
    private String accessToken;
}
