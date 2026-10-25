package com.lms.webapp.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
@ActiveProfiles("test")
class BlogRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockRestServiceServer mockServer;

    @Test
    @DisplayName("blogIndexRendersSafelyAndOutputsFixture — Render danh sách bài viết an toàn XSS và xuất file fixture")
    void blogIndexRendersSafelyAndOutputsFixture() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/blog/tags"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":[{\"id\":1,\"name\":\"Java Core\"},{\"id\":2,\"name\":\"Spring Cloud\"}]}",
                        MediaType.APPLICATION_JSON
                ));

        String maliciousTitle = "<script>alert('xss-blog-index')</script>Tiêu đề bài viết thử nghiệm";

        mockServer.expect(requestTo(containsString("http://localhost:8080/api/blog/posts/public")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"content\":[{\"id\":10,\"title\":\"" + maliciousTitle + "\",\"slug\":\"tieu-de-bai-viet\",\"summary\":\"Tom tat ngan\",\"viewCount\":25,\"upvoteCount\":10,\"downvoteCount\":1,\"commentCount\":3,\"tags\":[{\"id\":1,\"name\":\"Java Core\"}]}],\"pageable\":{\"pageNumber\":0,\"pageSize\":10},\"totalPages\":1,\"totalElements\":1}}",
                        MediaType.APPLICATION_JSON
                ));

        String html = mockMvc.perform(get("/blog"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Ghi fixture phục vụ kiểm thử JS
        Path fixtureDir = Path.of("target", "ui-fixtures");
        Files.createDirectories(fixtureDir);
        Files.writeString(fixtureDir.resolve("blog-index.html"), html);

        // Assert an toàn XSS
        assertThat(html).doesNotContain("<script>alert('xss-blog-index')</script>");
        assertThat(html).contains("&lt;script&gt;alert(");
        assertThat(html).contains("Java Core");
        assertThat(html).contains("tieu-de-bai-viet");
    }

    @Test
    @DisplayName("blogDetailRendersMarkdownSafelyAndOutputsFixture — Render chi tiết bài viết, chuyển đổi Markdown và an toàn XSS")
    void blogDetailRendersMarkdownSafelyAndOutputsFixture() throws Exception {
        String markdownContent = "# Hướng Dẫn Kiến Trúc Microservices\\n\\nBài viết này chia sẻ về **Spring Cloud Gateway** và MongoDB.\\n\\n<img src=x onerror=alert('xss-img')>";

        mockServer.expect(requestTo("http://localhost:8080/api/blog/posts/public/kien-truc-microservices"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"id\":50,\"title\":\"Kiến Trúc Microservices\",\"slug\":\"kien-truc-microservices\",\"content\":\"" + markdownContent + "\",\"authorName\":\"Tran Van C\",\"createdAt\":\"2026-10-25T15:30:00\",\"upvoteCount\":42,\"downvoteCount\":3,\"commentCount\":2,\"tags\":[{\"id\":2,\"name\":\"Spring Cloud\"}]}}",
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo(containsString("http://localhost:8080/api/blog/posts/50/comments")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"content\":[{\"id\":501,\"content\":\"Bài viết rất hay và chi tiết!\",\"authorName\":\"Học viên A\",\"createdAt\":\"2026-10-25T16:00:00\",\"upvoteCount\":5,\"downvoteCount\":0}],\"totalPages\":1,\"totalElements\":1}}",
                        MediaType.APPLICATION_JSON
                ));

        String html = mockMvc.perform(get("/blog/kien-truc-microservices"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // Ghi fixture
        Path fixtureDir = Path.of("target", "ui-fixtures");
        Files.createDirectories(fixtureDir);
        Files.writeString(fixtureDir.resolve("blog-detail.html"), html);

        // Assert chuyển đổi Markdown thành HTML
        assertThat(html).contains("<h1>Hướng Dẫn Kiến Trúc Microservices</h1>");
        assertThat(html).contains("<strong>Spring Cloud Gateway</strong>");

        // Assert an toàn XSS trong Markdown: thẻ HTML thô bị escape, không sinh thẻ <img> thực thi
        assertThat(html).doesNotContain("<img src=x onerror=");
        assertThat(html).contains("&lt;img src=x");

        // Assert dữ liệu bài viết & bình luận
        assertThat(html).contains("data-post-id=\"50\"");
        assertThat(html).contains("data-comment-id=\"501\"");
        assertThat(html).contains("Bài viết rất hay và chi tiết!");
        assertThat(html).contains("Tran Van C");
    }
}
