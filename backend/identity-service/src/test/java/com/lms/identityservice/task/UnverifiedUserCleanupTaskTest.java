package com.lms.identityservice.task;

import com.lms.identityservice.entity.User;
import com.lms.identityservice.repository.EmailVerificationRepository;
import com.lms.identityservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UnverifiedUserCleanupTaskTest {

    @Mock
    UserRepository userRepository;

    @Mock
    EmailVerificationRepository emailVerificationRepository;

    @InjectMocks
    UnverifiedUserCleanupTask cleanupTask;

    @Test
    void cleanupUnverifiedUsersDeletesExpiredOtpsAndUsers() {
        when(emailVerificationRepository.deleteExpiredOrUsed(any(Instant.class))).thenReturn(2);
        User user1 = User.builder().userId("u-1").username("unverified1").verified(false).build();
        when(userRepository.findByVerifiedFalseAndCreatedAtBefore(any(Instant.class)))
                .thenReturn(List.of(user1));

        cleanupTask.cleanupUnverifiedUsers();

        verify(emailVerificationRepository).deleteExpiredOrUsed(any(Instant.class));
        verify(userRepository).deleteAll(List.of(user1));
    }

    @Test
    void cleanupUnverifiedUsersHandlesEmptyList() {
        when(emailVerificationRepository.deleteExpiredOrUsed(any(Instant.class))).thenReturn(0);
        when(userRepository.findByVerifiedFalseAndCreatedAtBefore(any(Instant.class)))
                .thenReturn(List.of());

        cleanupTask.cleanupUnverifiedUsers();

        verify(emailVerificationRepository).deleteExpiredOrUsed(any(Instant.class));
        verify(userRepository, never()).deleteAll(any());
    }
}
