package com.lms.identityservice.service.impl;

import com.lms.identityservice.repository.EmailVerificationRepository;
import com.lms.identityservice.service.OtpAttemptHandler;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OtpAttemptHandlerImpl implements OtpAttemptHandler {

    EmailVerificationRepository emailVerificationRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void increment(Long verificationId) {
        emailVerificationRepository.incrementAttempts(verificationId);
    }
}
