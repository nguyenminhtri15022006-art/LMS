package com.lms.courseservice.service;

import com.lms.courseservice.dto.request.CreateCourseRequest;
import com.lms.courseservice.dto.request.InviteStudentRequest;
import com.lms.courseservice.dto.request.LockCourseRequest;
import com.lms.courseservice.dto.request.UpdateCourseRequest;
import com.lms.courseservice.dto.response.*;
import com.lms.courseservice.enums.CourseLevel;
import com.lms.courseservice.enums.CourseStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CourseService {
    Page<CourseResponse> getAllCourses(Pageable pageable, CourseStatus status, Long categoryId, String keyword, List<CourseLevel> levels);
    CourseResponse getCourseById(String id);
    CourseResponse createCourse(CreateCourseRequest request);
    CourseResponse updateCourse(String id, UpdateCourseRequest request);
    void deleteCourse(String id);
    Page<CourseResponse> getMyCourses(Pageable pageable);
    List<CourseResponse> getEnrolledCourses();
    EnrollmentResponse enrollCourse(String courseId);
    InviteResultResponse inviteStudents(String courseId, InviteStudentRequest request);
    CourseResponse lockCourse(String courseId, LockCourseRequest request);
    CourseResponse unlockCourse(String courseId);
    InstructorStatsResponse getMyStats();
    List<RecentEnrollmentResponse> getRecentEnrollments(int limit);
    long countStudents(String courseId);
    long countLessons(String courseId);
    Page<CourseStudentResponse> getCourseStudents(String courseId, Pageable pageable);
    void removeStudentFromCourse(String courseId, String userId);
    UserInfoDto searchUserByEmail(String email);
}
