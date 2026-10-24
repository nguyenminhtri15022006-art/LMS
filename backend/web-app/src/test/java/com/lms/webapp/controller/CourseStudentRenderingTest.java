package com.lms.webapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
@ActiveProfiles("test")
@WithMockUser(username = "bob", roles = "STUDENT")
class CourseStudentRenderingTest {
    @Autowired MockMvc mvc;
    @Autowired MockRestServiceServer server;
    @Autowired ObjectMapper mapper;

    private static final String TREE = """
        {"nodes":[
          {"id":"late","type":"LESSON","title":"LATE_LESSON","order":2,"lessonId":"l-late"},
          {"id":"deep","parentId":"sub","type":"LESSON","title":"DEEP_LESSON","order":1,"lessonId":"l-deep","lessonType":"VIDEO"},
          {"id":"root","type":"FOLDER","title":"ROOT_FOLDER","order":1},
          {"id":"sub","parentId":"root","type":"FOLDER","title":"SUB_FOLDER","order":1}
        ]}
        """;

    private void respond(String suffix, String data) {
        server.expect(requestTo("http://localhost:8080/api/course/courses/c-101" + suffix))
                .andRespond(withSuccess("{\"code\":1000,\"data\":" + data + "}", MediaType.APPLICATION_JSON));
    }

    private void courseAndTree() {
        respond("", "{\"id\":\"c-101\",\"title\":\"Course\"}");
        respond("/structure", TREE);
    }

    private String renderHub(String videoUrl) throws Exception {
        courseAndTree();
        var lesson = mapper.createObjectNode().put("id", "l-deep").put("lessonType", "VIDEO");
        lesson.putObject("content").put("videoUrl", videoUrl);
        respond("/lessons/l-deep", lesson.toString());
        respond("/progress", "{\"totalLessons\":4,\"completedLessons\":1,\"percentComplete\":25,\"completedLessonIds\":[\"l-late\"]}");
        String html = mvc.perform(get("/learn/c-101/lessons/l-deep"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        server.verify();
        return html;
    }

    @Test
    void renderedPageIncludesModalTargetsAndExportsRealHtmlForJavascriptTests() throws Exception {
        String html = renderHub("https://cdn.example/lesson.mp4");
        Path fixtureDir = Path.of("target", "ui-fixtures");
        Files.createDirectories(fixtureDir);
        Files.writeString(fixtureDir.resolve("learning-hub.html"), html);
        try (var bootstrap = getClass().getResourceAsStream("/META-INF/resources/webjars/bootstrap/5.3.3/js/bootstrap.bundle.min.js")) {
            assertThat(bootstrap).isNotNull();
            Files.write(fixtureDir.resolve("bootstrap.bundle.min.js"), bootstrap.readAllBytes());
        }
        assertThat(html).contains("id=\"schedulesModal\"", "id=\"announcementsModal\"",
                "id=\"schedulesContent\"", "id=\"announcementsContent\"");
    }

    @Test
    void detailRendersDeepLessonsInTreeOrder() throws Exception {
        courseAndTree();
        String html = mvc.perform(get("/courses/c-101")).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        server.verify();
        assertTreeOrder(html);
    }

    @Test
    void hubRendersDeepLessonLinkAndCompletionState() throws Exception {
        String html = renderHub("https://cdn.example/lesson.mp4");
        assertTreeOrder(html);
        assertThat(html).contains("href=\"/learn/c-101/lessons/l-deep\"", "data-lesson-id=\"l-late\"", "data-checked=\"true\"");
    }

    @Test
    void executableVideoUrlNeverReachesIframeOrSource() throws Exception {
        String html = renderHub("javascript:alert(1)//youtube.com");
        assertThat(html).doesNotContain("<iframe", "<video", "javascript:alert")
                .contains("Video bài học chưa sẵn sàng");
    }

    @Test
    void lookalikeProviderHostNeverGetsIframePrivileges() throws Exception {
        String html = renderHub("https://youtube.com.evil.example/embed/abc");
        assertThat(html).doesNotContain("<iframe").contains("<video controls");
    }

    private void assertTreeOrder(String html) {
        assertThat(html).contains("ROOT_FOLDER", "SUB_FOLDER", "DEEP_LESSON", "LATE_LESSON");
        assertThat(html.indexOf("ROOT_FOLDER")).isLessThan(html.indexOf("SUB_FOLDER"));
        assertThat(html.indexOf("SUB_FOLDER")).isLessThan(html.indexOf("DEEP_LESSON"));
        assertThat(html.indexOf("DEEP_LESSON")).isLessThan(html.indexOf("LATE_LESSON"));
    }
}
