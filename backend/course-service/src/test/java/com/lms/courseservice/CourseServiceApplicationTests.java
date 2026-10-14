package com.lms.courseservice;

import com.lms.courseservice.repository.mongo.CourseStructureRepository;
import com.lms.courseservice.repository.mongo.LessonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class CourseServiceApplicationTests {

    @MockBean
    private CourseStructureRepository courseStructureRepository;

    @MockBean
    private LessonRepository lessonRepository;

    @Test
    void contextLoads() {
    }
}
