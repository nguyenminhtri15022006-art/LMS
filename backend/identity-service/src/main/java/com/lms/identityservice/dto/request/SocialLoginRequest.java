package com.lms.identityservice.dto.request;

import com.lms.identityservice.enums.RegisterableRole;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SocialLoginRequest {
    @NotBlank(message = "TOKEN_BLANK")
    String code;

    @NotBlank(message = "Redirect URI không được để trống")
    String redirectUri;

    RegisterableRole role;

    String ipAddress;
    String deviceInfo;
}
