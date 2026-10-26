package com.lms.courseservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CourseStudentResponse {
    String userId;
    String username;
    String fullname;
    String email;
    String avatarUrl;
    LocalDateTime enrolledAt;
    String enrollmentStatus;
}
