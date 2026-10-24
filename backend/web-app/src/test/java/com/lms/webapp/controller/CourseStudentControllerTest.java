package com.lms.webapp.controller;

import com.lms.webapp.service.MarkdownService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
@ActiveProfiles("test")
class CourseStudentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockRestServiceServer mockServer;

    @Autowired(required = false)
    private MarkdownService markdownService;

    @Test
    void catalogDisplaysCategoriesFromApi() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/course/categories"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Success\",\"data\":[{\"id\":1,\"name\":\"Lap Trinh Java\",\"slug\":\"lap-trinh-java\"}]}",
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo(containsString("http://localhost:8080/api/course/courses")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Success\",\"data\":{\"content\":[{\"id\":\"c-101\",\"title\":\"Java Core Co Ban\",\"status\":\"PUBLISHED\",\"level\":\"BEGINNER\"}],\"pageable\":{\"pageNumber\":0,\"pageSize\":20},\"totalPages\":1,\"totalElements\":1}}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(get("/courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/student/catalog"))
                .andExpect(model().attributeExists("categories"))
                .andExpect(model().attributeExists("courses"));

        mockServer.verify();
    }

    @Test
    void courseDetailShowsOutline() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Success\",\"data\":{\"id\":\"c-101\",\"title\":\"Java Core Co Ban\",\"description\":\"Khoa hoc day tu dau\",\"level\":\"BEGINNER\",\"instructorId\":\"u-ins-1\"}}",
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101/structure"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Success\",\"data\":{\"id\":\"s-101\",\"courseId\":\"c-101\",\"nodes\":[{\"id\":\"n-1\",\"type\":\"FOLDER\",\"title\":\"Chuong 1: Khoi Dau\",\"order\":1},{\"id\":\"n-2\",\"type\":\"LESSON\",\"title\":\"Bai 1: Cai dat JDK\",\"order\":1,\"parentId\":\"n-1\",\"lessonId\":\"l-201\",\"lessonType\":\"DOCUMENT\"}]}}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(get("/courses/c-101"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/student/detail"))
                .andExpect(model().attributeExists("course"))
                .andExpect(model().attributeExists("structure"));

        mockServer.verify();
    }

    @Test
    @WithMockUser(username = "bob", roles = "STUDENT")
    void enrollCourseCallsGateway() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101/enroll"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Ghi danh thanh cong\",\"data\":{\"id\":501,\"courseId\":\"c-101\",\"userId\":\"u-bob\",\"status\":\"ACTIVE\"}}",
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101/structure"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Success\",\"data\":{\"id\":\"s-101\",\"courseId\":\"c-101\",\"nodes\":[{\"id\":\"n-2\",\"type\":\"LESSON\",\"title\":\"Bai 1: Cai dat JDK\",\"order\":1,\"lessonId\":\"l-201\"}]}}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(post("/courses/c-101/enroll").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/learn/c-101/**"));

        mockServer.verify();
    }

    @Test
    @WithMockUser(username = "bob", roles = "STUDENT")
    void enrolledCoursesDisplaysUserCourses() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/enrolled"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Success\",\"data\":[{\"id\":\"c-101\",\"title\":\"Java Core Co Ban\"},{\"id\":\"c-102\",\"title\":\"Spring Boot Mastery\"}]}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(get("/my-courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/student/my-courses"))
                .andExpect(model().attributeExists("courses"));

        mockServer.verify();
    }

    @Test
    @WithMockUser(username = "bob", roles = "STUDENT")
    void lessonDetailEnforcesCourseIdInPath() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Success\",\"data\":{\"id\":\"c-101\",\"title\":\"Java Core Co Ban\"}}",
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101/structure"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Success\",\"data\":{\"id\":\"s-101\",\"courseId\":\"c-101\",\"nodes\":[{\"id\":\"n-2\",\"type\":\"LESSON\",\"title\":\"Bai 1: Cai dat JDK\",\"lessonId\":\"l-201\",\"lessonType\":\"DOCUMENT\"}]}}",
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101/lessons/l-201"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Success\",\"data\":{\"id\":\"l-201\",\"courseId\":\"c-101\",\"lessonType\":\"DOCUMENT\",\"content\":{\"text\":\"Huong dan cai dat JDK 21 tren Windows\"}}}",
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101/progress"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Success\",\"data\":{\"courseId\":\"c-101\",\"totalLessons\":1,\"completedLessons\":0,\"percentComplete\":0.0,\"completedLessonIds\":[]}}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(get("/learn/c-101/lessons/l-201"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/student/learning-hub"))
                .andExpect(model().attributeExists("lesson"))
                .andExpect(model().attributeExists("course"))
                .andExpect(model().attributeExists("progress"));

        mockServer.verify();
    }

    @Test
    @WithMockUser(username = "bob", roles = "STUDENT")
    void completeLessonCallsGatewayWithCourseId() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101/lessons/l-201/complete"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Đã đánh dấu lesson hoàn thành\"}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(post("/learn/c-101/lessons/l-201/complete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lessonType\":\"DOCUMENT\"}"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(1000));

        mockServer.verify();
    }

    @Test
    void markdownLessonRenderedSafely() {
        assertThat(markdownService).isNotNull();

        String rawMarkdown = "# Tieu De Bai Hoc\n\nNoi dung **in dam** va *in nghieng*.\n\n<script>alert('xss');</script>";
        String html = markdownService.renderHtml(rawMarkdown);

        assertThat(html).contains("<h1>Tieu De Bai Hoc</h1>");
        assertThat(html).contains("<strong>in dam</strong>");
        assertThat(html).doesNotContain("<script>");
        assertThat(html).doesNotContain("alert('xss');");
    }
}
