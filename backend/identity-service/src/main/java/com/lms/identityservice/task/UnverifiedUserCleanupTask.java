package com.lms.identityservice.task;

import com.lms.identityservice.entity.User;
import com.lms.identityservice.repository.EmailVerificationRepository;
import com.lms.identityservice.repository.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UnverifiedUserCleanupTask {

    UserRepository userRepository;
    EmailVerificationRepository emailVerificationRepository;

    @Scheduled(fixedDelayString = "${app.unverified-cleanup-delay-ms:3600000}", initialDelay = 60000)
    @Transactional
    public void cleanupUnverifiedUsers() {
        try {
            Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
            int deletedOtps = emailVerificationRepository.deleteExpiredOrUsed(Instant.now());

            List<User> expiredUsers = userRepository.findByVerifiedFalseAndCreatedAtBefore(cutoff);
            int deletedUsers = 0;
            if (!expiredUsers.isEmpty()) {
                deletedUsers = expiredUsers.size();
                userRepository.deleteAll(expiredUsers);
            }

            if (deletedUsers > 0 || deletedOtps > 0) {
                log.info("Dọn dẹp định kỳ: đã xóa {} tài khoản chưa kích hoạt quá 24h và {} bản ghi OTP hết hạn/đã dùng",
                        deletedUsers, deletedOtps);
            }
        } catch (Exception e) {
            log.error("Lỗi trong quá trình dọn dẹp tài khoản chưa kích hoạt: {}", e.getMessage(), e);
        }
    }
}
