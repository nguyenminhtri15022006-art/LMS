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
class ProblemWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /problems returns 200 OK and renders problems/list")
    void testListProblems() throws Exception {
        mockMvc.perform(get("/problems"))
                .andExpect(status().isOk())
                .andExpect(view().name("problems/list"))
                .andExpect(model().attributeExists("problems"))
                .andExpect(model().attributeExists("totalProblems"))
                .andExpect(content().string(containsString("EduLearn Code Judge")));
    }

    @Test
    @DisplayName("GET /problems with keyword filters matching problem")
    void testFilterProblemsByKeyword() throws Exception {
        mockMvc.perform(get("/problems").param("keyword", "Two Sum"))
                .andExpect(status().isOk())
                .andExpect(view().name("problems/list"))
                .andExpect(model().attributeExists("problems"))
                .andExpect(content().string(containsString("Two Sum")));
    }

    @Test
    @DisplayName("GET /problems/{id} with valid id renders problem workspace")
    void testShowSolveWorkspaceValid() throws Exception {
        mockMvc.perform(get("/problems/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("problems/solve"))
                .andExpect(model().attributeExists("problem"))
                .andExpect(model().attributeExists("codeSubmitDto"))
                .andExpect(model().attributeExists("submissions"))
                .andExpect(content().string(containsString("Two Sum")));
    }

    @Test
    @DisplayName("GET /problems/{id} with invalid id redirects to /problems")
    void testShowSolveWorkspaceInvalid() throws Exception {
        mockMvc.perform(get("/problems/9999"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/problems"));
    }

    @Test
    @DisplayName("POST /problems/{id}/submit without login redirects to login")
    void testSubmitCodeUnauthenticated() throws Exception {
        mockMvc.perform(post("/problems/1/submit")
                        .param("language", "CPP")
                        .param("sourceCode", "#include <iostream>"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login?redirectUrl=/problems/1"));
    }

    @Test
    @DisplayName("POST /problems/{id}/submit with logged-in user succeeds and redirects with result")
    void testSubmitCodeAuthenticated() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("currentUser", UserSessionDto.builder()
                .userId("usr-student-01")
                .username("student")
                .fullname("Nguyễn Văn Học Viên")
                .role("STUDENT")
                .build());

        mockMvc.perform(post("/problems/1/submit")
                        .session(session)
                        .param("language", "CPP")
                        .param("sourceCode", "#include <iostream>\nusing namespace std;\nint main() { return 0; }"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/problems/1"))
                .andExpect(flash().attributeExists("lastResult"));
    }

    @Test
    @DisplayName("GET /problems/{id}?lang=JAVA loads Java template")
    void testLoadJavaLanguageTemplate() throws Exception {
        mockMvc.perform(get("/problems/1").param("lang", "JAVA"))
                .andExpect(status().isOk())
                .andExpect(view().name("problems/solve"))
                .andExpect(model().attribute("selectedLang", "JAVA"));
    }
}
