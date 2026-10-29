package com.lms.courseservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class RecentEnrollmentResponse {
    String courseId;
    String courseTitle;
    String courseThumbnailUrl;
    String userId;
    String fullname;
    String avatarUrl;
    LocalDateTime enrolledAt;
}
