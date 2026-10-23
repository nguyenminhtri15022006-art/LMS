package com.lms.assignmentservice.repository;

import com.lms.assignmentservice.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GradeRepository extends JpaRepository<Grade, Long> {
    Optional<Grade> findByUserIdAndQuiz_QuizId(String userId, Integer quizId);

    List<Grade> findByCourseId(String courseId);

    List<Grade> findByUserIdAndCourseId(String userId, String courseId);

    List<Grade> findByUserId(String userId);

    @Query("""
            SELECT g.userId, SUM(g.bestScore), COUNT(g)
            FROM Grade g
            WHERE g.courseId = :courseId
            GROUP BY g.userId
            ORDER BY SUM(g.bestScore) DESC
            """)
    List<Object[]> aggregateLeaderboardByCourse(@Param("courseId") String courseId);

    @Query("""
            SELECT g.userId, SUM(g.bestScore), COUNT(g)
            FROM Grade g
            GROUP BY g.userId
            ORDER BY SUM(g.bestScore) DESC
            """)
    List<Object[]> aggregateGlobalLeaderboard();
}
