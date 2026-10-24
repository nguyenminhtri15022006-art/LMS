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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
                .andExpect(model().attributeExists("courses"))
                .andExpect(content().string(containsString("Khám Phá Khóa Học")))
                .andExpect(content().string(containsString("Lap Trinh Java")))
                .andExpect(content().string(containsString("Java Core Co Ban")));

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
                .andExpect(model().attributeExists("structure"))
                .andExpect(content().string(containsString("Java Core Co Ban")))
                .andExpect(content().string(containsString("Chuong 1: Khoi Dau")))
                .andExpect(content().string(containsString("Bai 1: Cai dat JDK")))
                .andExpect(content().string(containsString("action=\"/courses/c-101/enroll\"")))
                .andExpect(content().string(containsString("Đăng Ký Học Ngay")));

        mockServer.verify();
    }

    @Test
    @WithMockUser(username = "bob", roles = "STUDENT")
    void courseDetailRendersContinueLearningWhenAlreadyEnrolled() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Success\",\"data\":{\"id\":\"c-101\",\"title\":\"Java Core Co Ban\",\"description\":\"Khoa hoc day tu dau\",\"level\":\"BEGINNER\",\"instructorId\":\"u-ins-1\"}}",
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101/structure"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Success\",\"data\":{\"id\":\"s-101\",\"courseId\":\"c-101\",\"nodes\":[]}}",
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/enrolled"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Success\",\"data\":[{\"id\":\"c-101\",\"title\":\"Java Core Co Ban\"}]}",
                        MediaType.APPLICATION_JSON
                ));

        org.springframework.mock.web.MockHttpSession session = new org.springframework.mock.web.MockHttpSession();
        session.setAttribute("username", "bob");

        mockMvc.perform(get("/courses/c-101").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/student/detail"))
                .andExpect(model().attribute("isEnrolled", true))
                .andExpect(content().string(containsString("Tiếp Tục Học")))
                .andExpect(content().string(not(containsString("Đăng Ký Học Ngay"))));

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

        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101/progress"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"courseId\":\"c-101\",\"percentComplete\":40.0,\"completedLessons\":2,\"totalLessons\":5}}",
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-102/progress"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"courseId\":\"c-102\",\"percentComplete\":0.0,\"completedLessons\":0,\"totalLessons\":10}}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(get("/my-courses"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/student/my-courses"))
                .andExpect(model().attributeExists("courses"))
                .andExpect(content().string(containsString("Java Core Co Ban")))
                .andExpect(content().string(containsString("Spring Boot Mastery")))
                .andExpect(content().string(containsString("<strong>40</strong>%")));

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
                .andExpect(model().attributeExists("progress"))
                .andExpect(content().string(containsString("Huong dan cai dat JDK 21 tren Windows")))
                .andExpect(content().string(containsString("id=\"courseId\" value=\"c-101\"")))
                .andExpect(content().string(containsString("id=\"lessonId\" value=\"l-201\"")))
                .andExpect(content().string(containsString("id=\"lessonType\" value=\"DOCUMENT\"")))
                .andExpect(content().string(containsString("id=\"completeLessonBtn\"")))
                .andExpect(content().string(containsString("learning-hub.js")));

        mockServer.verify();
    }

    @Test
    @WithMockUser(username = "bob", roles = "STUDENT")
    void learningHubRendersCompletedStateWhenLessonAlreadyCompleted() throws Exception {
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
                        "{\"code\":1000,\"message\":\"Success\",\"data\":{\"courseId\":\"c-101\",\"totalLessons\":1,\"completedLessons\":1,\"percentComplete\":100.0,\"completedLessonIds\":[\"l-201\"]}}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(get("/learn/c-101/lessons/l-201"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/student/learning-hub"))
                .andExpect(model().attribute("isLessonCompleted", true))
                .andExpect(content().string(containsString("bi-check-circle-fill text-success")))
                .andExpect(content().string(containsString("id=\"completeLessonBtn\" class=\"btn btn-success fw-semibold px-4 py-2 shadow-sm d-none\"")))
                .andExpect(content().string(containsString("id=\"completedBadge\" class=\"badge bg-success-subtle text-success border border-success-subtle px-3 py-2 fs-6\"")));

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

        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101/progress"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"courseId\":\"c-101\",\"percentComplete\":100.0,\"completedLessons\":1,\"totalLessons\":1}}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(post("/learn/c-101/lessons/l-201/complete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lessonType\":\"DOCUMENT\"}"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(1000))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.percentComplete").value(100.0));

        mockServer.verify();
    }

    @Test
    @WithMockUser(username = "bob", roles = "STUDENT")
    void completeLessonEndpointHandlesGatewayError() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101/lessons/l-201/complete"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest()
                        .body("{\"code\":1001,\"message\":\"Bài học đã hoàn thành trước đó\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        mockMvc.perform(post("/learn/c-101/lessons/l-201/complete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lessonType\":\"DOCUMENT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(400))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.message").value("Không thể cập nhật tiến độ bài học"));

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

    @Test
    @WithMockUser(username = "bob", roles = "STUDENT")
    void enrollCourse_WhenGatewayFails_RedirectsToCourseDetailWithErrorMessage() throws Exception {
        // Red test cho Nhóm C: Khi Gateway enroll trả lỗi 500 hoặc rớt kết nối
        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101/enroll"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withServerError()
                        .body("{\"code\":9999,\"message\":\"Lỗi hệ thống ghi danh\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        // Controller PHẢI bắt lỗi, KHÔNG được redirect vào /learn/c-101, mà PHẢI redirect về /courses/c-101 kèm flash attribute errorMessage
        mockMvc.perform(post("/courses/c-101/enroll").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/courses/c-101"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash().attributeExists("errorMessage"));

        mockServer.verify();
    }

    @Test
    @WithMockUser(username = "bob", roles = "STUDENT")
    void enrollCourse_SelectsFirstLessonAccordingToTreeOrder_WhenInputNodesAreUnsortedAndMultiLevel() throws Exception {
        // Red test cho Nhóm E: Danh sách nodes chưa sắp xếp và lồng nhiều cấp
        // Order mong muốn trong cây:
        // Folder n-root (order 1)
        //   -> SubFolder n-sub (order 1)
        //       -> Lesson l-target (order 1)
        // Lesson l-wrong (order 2)
        // Nhưng mảng trả về từ Gateway bị đảo thứ tự hoàn toàn: l-wrong xuất hiện trước n-root, subfolder xuất hiện sau cùng
        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101/enroll"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Thành công\"}",
                        MediaType.APPLICATION_JSON
                ));

        String unsortedNodesJson = "{\"code\":1000,\"data\":{\"id\":\"s-101\",\"courseId\":\"c-101\",\"nodes\":[" +
                "{\"id\":\"n-wrong\",\"type\":\"LESSON\",\"title\":\"Bài sau\",\"order\":2,\"lessonId\":\"l-wrong\",\"parentId\":null}," +
                "{\"id\":\"n-root\",\"type\":\"FOLDER\",\"title\":\"Chương 1\",\"order\":1,\"parentId\":null}," +
                "{\"id\":\"n-lesson-1\",\"type\":\"LESSON\",\"title\":\"Bài đầu tiên đúng\",\"order\":1,\"lessonId\":\"l-target\",\"parentId\":\"n-sub\"}," +
                "{\"id\":\"n-sub\",\"type\":\"FOLDER\",\"title\":\"Chương 1.1\",\"order\":1,\"parentId\":\"n-root\"}" +
                "]}}";

        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101/structure"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(unsortedNodesJson, MediaType.APPLICATION_JSON));

        // Phải chuyển hướng chính xác đến bài học đầu tiên theo cây: l-target, KHÔNG PHẢI l-wrong
        mockMvc.perform(post("/courses/c-101/enroll").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl("/learn/c-101/lessons/l-target"));

        mockServer.verify();
    }

    @Test
    @WithMockUser(username = "bob", roles = "STUDENT")
    void completeLesson_WithVideoLessonType_ReturnsUpdatedProgressInResponseData() throws Exception {
        // Red test cho Nhóm D: Hoàn thành bài học VIDEO và trả về tiến độ mới
        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101/lessons/l-202/complete"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("{\"code\":1000,\"message\":\"Đã đánh dấu hoàn thành\"}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101/progress"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Success\",\"data\":{\"courseId\":\"c-101\",\"totalLessons\":4,\"completedLessons\":3,\"percentComplete\":75.0,\"completedLessonIds\":[\"l-201\",\"l-202\",\"l-203\"]}}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(post("/learn/c-101/lessons/l-202/complete")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lessonType\":\"VIDEO\"}"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.code").value(1000))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.percentComplete").value(75.0))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.completedLessons").value(3))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.totalLessons").value(4));

        mockServer.verify();
    }

    @Test
    @WithMockUser(username = "bob", roles = "STUDENT")
    void learningHub_RendersNativeVideo_WhenUrlIsDirectFile() throws Exception {
        // Red test cho Nhóm F: Phân biệt video file trực tiếp (.mp4) -> dùng thẻ <video controls>
        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":1000,\"data\":{\"id\":\"c-101\",\"title\":\"Java Core\"}}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101/structure"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":1000,\"data\":{\"nodes\":[]}}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101/lessons/l-video-direct"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"id\":\"l-video-direct\",\"lessonType\":\"VIDEO\",\"title\":\"Cài đặt JDK Video\",\"content\":{\"videoUrl\":\"https://minio.example.com/videos/lesson1.mp4\"}}}",
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101/progress"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":1000,\"data\":{\"completedLessonIds\":[]}}", MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/learn/c-101/lessons/l-video-direct"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/student/learning-hub"))
                .andExpect(content().string(containsString("<video controls")))
                .andExpect(content().string(containsString("src=\"https://minio.example.com/videos/lesson1.mp4\"")))
                .andExpect(content().string(not(containsString("<iframe"))));

        mockServer.verify();
    }

    @Test
    @WithMockUser(username = "bob", roles = "STUDENT")
    void learningHub_RendersIframe_WhenUrlIsYoutubeEmbed() throws Exception {
        // Red test cho Nhóm F: URL là youtube embed -> render iframe
        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":1000,\"data\":{\"id\":\"c-101\",\"title\":\"Java Core\"}}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101/structure"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":1000,\"data\":{\"nodes\":[]}}", MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101/lessons/l-video-yt"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"id\":\"l-video-yt\",\"lessonType\":\"VIDEO\",\"title\":\"Video Youtube\",\"content\":{\"videoUrl\":\"https://www.youtube.com/embed/dQw4w9WgXcQ\"}}}",
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101/progress"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":1000,\"data\":{\"completedLessonIds\":[]}}", MediaType.APPLICATION_JSON));

        mockMvc.perform(get("/learn/c-101/lessons/l-video-yt"))
                .andExpect(status().isOk())
                .andExpect(view().name("courses/student/learning-hub"))
                .andExpect(content().string(containsString("<iframe")))
                .andExpect(content().string(containsString("src=\"https://www.youtube.com/embed/dQw4w9WgXcQ\"")))
                .andExpect(content().string(containsString("allowfullscreen")));

        mockServer.verify();
    }

    @Test
    @WithMockUser(username = "bob", roles = "STUDENT")
    void getAnnouncementsJson_WithPageParameter_CallsGatewayCorrectly() throws Exception {
        // Red test cho Nhóm B: Phân trang thông báo
        mockServer.expect(requestTo("http://localhost:8080/api/course/courses/c-101/announcements?page=2&size=10"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"content\":[{\"id\":10,\"title\":\"Thong bao page 2\"}],\"totalPages\":3,\"number\":2}}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(get("/courses/c-101/announcements?page=2"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.content[0].title").value("Thong bao page 2"));

        mockServer.verify();
    }
}
