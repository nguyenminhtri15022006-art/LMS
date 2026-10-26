package com.lms.courseservice.entity.mysql;

import com.lms.courseservice.enums.LessonType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Entity
@Table(name = "lesson_progress")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LessonProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(nullable = false, length = 36)
    String studentId;

    @Column(nullable = false, length = 36)
    String courseId;

    @Column(nullable = false, length = 36)
    String lessonId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    LessonType lessonType;

    @Column(name = "is_completed")
    boolean completed;

    LocalDateTime lastAccessed;
}
