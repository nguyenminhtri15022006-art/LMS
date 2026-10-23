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
class QuizWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /quizzes returns 200 OK and renders quizzes/list")
    void testListQuizzes() throws Exception {
        mockMvc.perform(get("/quizzes"))
                .andExpect(status().isOk())
                .andExpect(view().name("quizzes/list"))
                .andExpect(model().attributeExists("quizzes"))
                .andExpect(content().string(containsString("EduLearn LMS")));
    }

    @Test
    @DisplayName("GET /quizzes/{id} with valid id renders quiz overview")
    void testShowQuizOverviewValid() throws Exception {
        mockMvc.perform(get("/quizzes/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("quizzes/overview"))
                .andExpect(model().attributeExists("quiz"))
                .andExpect(content().string(containsString("Microservices")));
    }

    @Test
    @DisplayName("GET /quizzes/{id} with invalid id redirects to /quizzes")
    void testShowQuizOverviewInvalid() throws Exception {
        mockMvc.perform(get("/quizzes/999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/quizzes"));
    }

    @Test
    @DisplayName("GET /quizzes/{id}/take without login redirects to login")
    void testTakeQuizUnauthenticated() throws Exception {
        mockMvc.perform(get("/quizzes/1/take"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?redirectUrl=/quizzes/1/take"));
    }

    @Test
    @DisplayName("GET /quizzes/{id}/take with login renders exam room")
    void testTakeQuizAuthenticated() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", UserSessionDto.builder()
                .userId("usr-student-01")
                .username("student")
                .role("STUDENT")
                .build());

        mockMvc.perform(get("/quizzes/1/take").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("quizzes/take"))
                .andExpect(model().attributeExists("quiz"))
                .andExpect(model().attributeExists("quizSubmitDto"))
                .andExpect(content().string(containsString("countdownTimer")));
    }

    @Test
    @DisplayName("POST /quizzes/{id}/submit evaluates answers and redirects to result")
    void testSubmitQuizAuthenticated() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", UserSessionDto.builder()
                .userId("usr-student-01")
                .username("student")
                .role("STUDENT")
                .build());

        mockMvc.perform(post("/quizzes/1/submit")
                        .session(session)
                        .param("answers[101]", "B")
                        .param("answers[102]", "B")
                        .param("timeSpentSeconds", "120"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/quizzes/1/result"))
                .andExpect(flash().attributeExists("quizResult"));
    }

    @Test
    @DisplayName("GET /quizzes/{id}/result renders result view")
    void testShowQuizResult() throws Exception {
        mockMvc.perform(get("/quizzes/1/result"))
                .andExpect(status().isOk())
                .andExpect(view().name("quizzes/result"))
                .andExpect(model().attributeExists("quiz"))
                .andExpect(model().attributeExists("quizResult"));
    }

    @Test
    @DisplayName("GET /questions renders question bank view")
    void testShowQuestionBank() throws Exception {
        mockMvc.perform(get("/questions"))
                .andExpect(status().isOk())
                .andExpect(view().name("questions/bank"))
                .andExpect(model().attributeExists("questions"));
    }
}
