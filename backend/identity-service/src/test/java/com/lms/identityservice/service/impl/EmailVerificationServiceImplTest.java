package com.lms.identityservice.service.impl;

import com.lms.identityservice.configuration.GatewayAuthentication;
import com.lms.identityservice.dto.request.ForgotPasswordRequest;
import com.lms.identityservice.dto.request.ResetPasswordRequest;
import com.lms.identityservice.dto.request.SendEmailVerificationRequest;
import com.lms.identityservice.dto.request.VerifyEmailRequest;
import com.lms.identityservice.entity.EmailVerification;
import com.lms.identityservice.entity.User;
import com.lms.identityservice.enums.VerificationType;
import com.lms.identityservice.exception.ErrorCode;
import com.lms.identityservice.exception.IdentityException;
import com.lms.identityservice.repository.EmailVerificationRepository;
import com.lms.identityservice.repository.RefreshTokenRepository;
import com.lms.identityservice.repository.UserRepository;
import com.lms.identityservice.service.EmailService;
import com.lms.identityservice.service.OtpAttemptHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceImplTest {

    @Mock
    EmailVerificationRepository emailVerificationRepository;

    @Mock
    RefreshTokenRepository refreshTokenRepository;

    @Mock
    UserRepository userRepository;

    @Mock
    EmailService emailService;

    @Mock
    OtpAttemptHandler otpAttemptHandler;

    @Mock
    PasswordEncoder passwordEncoder;

    EmailVerificationServiceImpl emailVerificationService;

    @BeforeEach
    void setUp() {
        emailVerificationService = new EmailVerificationServiceImpl(
                emailVerificationRepository,
                refreshTokenRepository,
                userRepository,
                emailService,
                otpAttemptHandler,
                passwordEncoder);
        ReflectionTestUtils.setField(emailVerificationService, "otpExpiryMinutes", 10L);
        ReflectionTestUtils.setField(emailVerificationService, "otpMaxAttempts", 5);
        SecurityContextHolder.getContext().setAuthentication(
                new GatewayAuthentication("user-1", "student@example.com", "ROLE_STUDENT", List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sendVerifyEmailStoresOtpAndCallsMailer() {
        User user = User.builder()
                .userId("user-1")
                .email("student@example.com")
                .fullname("Student One")
                .verified(false)
                .build();
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationRepository.findValidOtp(eq(user), eq(VerificationType.VERIFY_EMAIL), any(), eq(5)))
                .thenReturn(Optional.empty());

        emailVerificationService.sendVerifyEmail(SendEmailVerificationRequest.builder()
                .email("student@example.com")
                .build());

        ArgumentCaptor<EmailVerification> otpCaptor = ArgumentCaptor.forClass(EmailVerification.class);
        verify(emailVerificationRepository).save(otpCaptor.capture());
        EmailVerification savedOtp = otpCaptor.getValue();
        assertThat(savedOtp.getUser()).isSameAs(user);
        assertThat(savedOtp.getType()).isEqualTo(VerificationType.VERIFY_EMAIL);
        assertThat(savedOtp.getToken()).matches("^[0-9]{6}$");
        verify(emailService).sendVerifyEmail(eq("student@example.com"), eq("Student One"), eq(savedOtp.getToken()));
    }

    @Test
    void verifyEmailSuccessWithValidOtp() {
        User user = User.builder()
                .userId("user-1")
                .email("student@example.com")
                .verified(false)
                .build();
        EmailVerification otp = EmailVerification.builder()
                .id(10L)
                .user(user)
                .token("123456")
                .type(VerificationType.VERIFY_EMAIL)
                .attempts(0)
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationRepository.findValidOtp(eq(user), eq(VerificationType.VERIFY_EMAIL), any(), eq(5)))
                .thenReturn(Optional.of(otp));

        emailVerificationService.verifyEmail(VerifyEmailRequest.builder()
                .email("student@example.com")
                .otp("123456")
                .build());

        assertThat(user.getVerified()).isTrue();
        assertThat(otp.getUsedAt()).isNotNull();
        verify(emailVerificationRepository).save(otp);
        verify(userRepository).save(user);
    }

    @Test
    void verifyEmailThrowsWhenOtpExpiredOrInvalid() {
        User user = User.builder()
                .userId("user-1")
                .email("student@example.com")
                .verified(false)
                .build();
        EmailVerification otp = EmailVerification.builder()
                .id(10L)
                .user(user)
                .token("123456")
                .type(VerificationType.VERIFY_EMAIL)
                .attempts(1)
                .expiresAt(Instant.now().plusSeconds(600))
                .build();
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationRepository.findValidOtp(eq(user), eq(VerificationType.VERIFY_EMAIL), any(), eq(5)))
                .thenReturn(Optional.of(otp));

        assertThatExceptionOfType(IdentityException.class)
                .isThrownBy(() -> emailVerificationService.verifyEmail(VerifyEmailRequest.builder()
                        .email("student@example.com")
                        .otp("000000")
                        .build()))
                .satisfies(ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_OTP));

        verify(otpAttemptHandler).increment(10L);
    }

    @Test
    void forgotPasswordGeneratesResetOtp() {
        User user = User.builder()
                .userId("user-1")
                .email("student@example.com")
                .fullname("Student One")
                .verified(true)
                .build();
        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationRepository.findValidOtp(eq(user), eq(VerificationType.RESET_PASSWORD), any(), eq(5)))
                .thenReturn(Optional.empty());

        emailVerificationService.sendResetPassword(ForgotPasswordRequest.builder()
                .email("student@example.com")
                .build());

        ArgumentCaptor<EmailVerification> otpCaptor = ArgumentCaptor.forClass(EmailVerification.class);
        verify(emailVerificationRepository).save(otpCaptor.capture());
        EmailVerification savedOtp = otpCaptor.getValue();
        assertThat(savedOtp.getType()).isEqualTo(VerificationType.RESET_PASSWORD);
        assertThat(savedOtp.getToken()).matches("^[0-9]{6}$");
        verify(emailService).sendResetPasswordEmail(eq("student@example.com"), eq("Student One"), eq(savedOtp.getToken()));
    }

    @Test
    void resetPasswordUpdatesPasswordWithValidOtp() {
        User user = User.builder()
                .userId("user-1")
                .email("student@example.com")
                .password("old-hash")
                .build();
        EmailVerification otp = EmailVerification.builder()
                .id(20L)
                .user(user)
                .token("654321")
                .type(VerificationType.RESET_PASSWORD)
                .attempts(0)
                .expiresAt(Instant.now().plusSeconds(600))
                .build();

        when(userRepository.findByEmail("student@example.com")).thenReturn(Optional.of(user));
        when(emailVerificationRepository.findValidOtp(eq(user), eq(VerificationType.RESET_PASSWORD), any(), eq(5)))
                .thenReturn(Optional.of(otp));
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-hash");

        emailVerificationService.resetPassword(ResetPasswordRequest.builder()
                .email("student@example.com")
                .otp("654321")
                .newPassword("newPassword123")
                .confirmPassword("newPassword123")
                .build());

        assertThat(user.getPassword()).isEqualTo("new-hash");
        assertThat(otp.getUsedAt()).isNotNull();
        verify(emailVerificationRepository).save(otp);
        verify(userRepository).save(user);
    }
}
