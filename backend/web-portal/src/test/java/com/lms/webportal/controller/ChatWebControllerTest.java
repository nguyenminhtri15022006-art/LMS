package com.lms.webportal.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatWebControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /chat renders full page AI Assistant")
    void testShowChatPage() throws Exception {
        mockMvc.perform(get("/chat"))
                .andExpect(status().isOk())
                .andExpect(view().name("chat/index"))
                .andExpect(content().string(containsString("EduLearn LMS")));
    }

    @Test
    @DisplayName("POST /api/portal/chat returns AI answer JSON")
    void testProcessChatEndpoint() throws Exception {
        String jsonRequest = "{\"prompt\":\"Hướng dẫn thuật toán Two Sum\",\"sessionId\":\"test-sess-01\"}";

        mockMvc.perform(post("/api/portal/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").exists())
                .andExpect(jsonPath("$.sessionId").value("test-sess-01"));
    }
}
