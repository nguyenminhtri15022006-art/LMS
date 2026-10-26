package com.lms.courseservice.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class InviteResultResponse {
    List<String> successEmails;
    List<String> alreadyEnrolledEmails;
    List<String> notFoundEmails;
}
