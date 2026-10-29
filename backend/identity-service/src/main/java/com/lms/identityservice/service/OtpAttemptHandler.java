package com.lms.identityservice.service;

public interface OtpAttemptHandler {
    void increment(Long verificationId);
}
