package com.lms.courseservice.repository.mysql;

import com.lms.courseservice.entity.mysql.Course;
import com.lms.courseservice.entity.mysql.Enrollment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByUserIdAndCourseId(String userId, String courseId);
    Optional<Enrollment> findByUserIdAndCourseId(String userId, String courseId);
    List<Enrollment> findByUserId(String userId);
    Page<Enrollment> findByCourseId(String courseId, Pageable pageable);
    void deleteByUserIdAndCourseId(String userId, String courseId);
    void deleteAllByCourse(Course course);
    long countByCourseId(String courseId);

    @Query("SELECT COUNT(DISTINCT e.userId) FROM Enrollment e WHERE e.course.instructorId = :instructorId")
    long countTotalStudentsByInstructorId(@Param("instructorId") String instructorId);

    @Query("SELECT e FROM Enrollment e WHERE e.course.instructorId = :instructorId ORDER BY e.enrolledAt DESC")
    List<Enrollment> findRecentByInstructorId(@Param("instructorId") String instructorId, Pageable pageable);
}
