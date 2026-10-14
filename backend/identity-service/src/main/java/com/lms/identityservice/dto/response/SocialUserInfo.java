package com.lms.identityservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SocialUserInfo {
    String providerId;
    String email;
    String fullname;
    String avatarUrl;
}
