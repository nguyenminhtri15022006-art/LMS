package com.lms.webportal.controller;

import com.lms.webportal.dto.UserSessionDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CourseWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /courses returns 200 OK and renders courses/list")
    void testListCourses() throws Exception {
        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/list"))
                .andExpect(model().attributeExists("courses"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(content().string(containsString("EduLearn LMS")));
    }

    @Test
    @DisplayName("GET /courses with keyword filters matching courses")
    void testFilterCoursesByKeyword() throws Exception {
        mockMvc.perform(get("/courses").param("keyword", "Spring Boot"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/list"))
                .andExpect(model().attributeExists("courses"))
                .andExpect(content().string(containsString("Spring Boot Microservices")));
    }

    @Test
    @DisplayName("GET /courses/{id} with valid id returns 200 OK and renders detail")
    void testShowCourseDetailValid() throws Exception {
        mockMvc.perform(get("/courses/crs-01"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/detail"))
                .andExpect(model().attributeExists("course"))
                .andExpect(content().string(containsString("Spring Boot Microservices")));
    }

    @Test
    @DisplayName("GET /courses/{id} with invalid id redirects to /courses")
    void testShowCourseDetailInvalid() throws Exception {
        mockMvc.perform(get("/courses/non-existent-id"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses"));
    }

    @Test
    @DisplayName("POST /courses/{id}/enroll without login redirects to login")
    void testEnrollUnauthenticated() throws Exception {
        mockMvc.perform(post("/courses/crs-01/enroll"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?redirectUrl=/courses/crs-01"));
    }

    @Test
    @DisplayName("POST /courses/{id}/enroll with logged-in user succeeds")
    void testEnrollAuthenticated() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", UserSessionDto.builder()
                .userId("usr-student-01")
                .username("student")
                .fullname("Nguyễn Văn Học Viên")
                .role("STUDENT")
                .build());

        mockMvc.perform(post("/courses/crs-01/enroll").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/courses/crs-01?enrolled=true"));
    }

    @Test
    @DisplayName("GET /courses/{id}/lessons/{lessonId} renders lesson viewer")
    void testViewLesson() throws Exception {
        mockMvc.perform(get("/courses/crs-01/lessons/les-101"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/lesson"))
                .andExpect(model().attributeExists("course"))
                .andExpect(model().attributeExists("currentLesson"))
                .andExpect(content().string(containsString("Monolith vs Microservices")));
    }
}
