package com.lms.identityservice.repository;

import com.lms.identityservice.entity.EmailVerification;
import com.lms.identityservice.entity.User;
import com.lms.identityservice.enums.VerificationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface EmailVerificationRepository extends JpaRepository<EmailVerification, Long> {

    @Query("""
            SELECT count(e) > 0 FROM EmailVerification e
            WHERE e.user = :user
            AND e.type = :type
            AND e.usedAt is NULL
            AND e.expiresAt > :now
            AND e.attempts < :maxAttempts
            """)
    boolean existsByValidOtp(
            @Param("user") User user,
            @Param("type") VerificationType type,
            @Param("now") Instant now,
            @Param("maxAttempts") int maxAttempts
    );

    @Query("""
            SELECT e FROM EmailVerification e
            WHERE e.user = :user
            AND e.type = :type
            AND e.usedAt is NULL
            AND e.expiresAt > :now
            AND e.attempts < :maxAttempts
            ORDER BY e.createdAt DESC
            """)
    Optional<EmailVerification> findValidOtp(
            @Param("user") User user,
            @Param("type") VerificationType type,
            @Param("now") Instant now,
            @Param("maxAttempts") int maxAttempts
    );

    @Modifying
    @Query("UPDATE EmailVerification e SET e.attempts = e.attempts + 1 WHERE e.id = :id")
    void incrementAttempts(@Param("id") Long id);

    @Modifying
    @Query("""
            DELETE FROM EmailVerification e
            WHERE e.expiresAt < :now OR e.usedAt IS NOT NULL
            """)
    int deleteExpiredOrUsed(@Param("now") Instant now);
}
