package com.lms.courseservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserInfoDto {
    String id;
    String username;
    String fullname;
    String email;
    String avatarUrl;
}
