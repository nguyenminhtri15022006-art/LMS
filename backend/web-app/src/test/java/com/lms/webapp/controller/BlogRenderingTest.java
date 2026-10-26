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
                        "{\"code\":1000,\"data\":{\"id\":50,\"title\":\"Kiến Trúc Microservices\",\"slug\":\"kien-truc-microservices\",\"content\":\"" + markdownContent + "\",\"authorId\":\"author-50\",\"createdAt\":\"2026-10-25T15:30:00\",\"upvoteCount\":42,\"downvoteCount\":3,\"commentCount\":3,\"tags\":[{\"id\":2,\"name\":\"Spring Cloud\"}]}}",
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo("http://localhost:8080/api/identity/users/author-50/profile"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":1000,\"data\":{\"userId\":\"author-50\",\"fullname\":\"Tran Van C\",\"avatarUrl\":\"/images/avatar.png\"}}", MediaType.APPLICATION_JSON));

        String commentsJson = "{\"code\":1000,\"data\":{\"content\":["
                + "{"
                + "  \"id\": 501,"
                + "  \"userId\": \"user-student-1\","
                + "  \"content\": \"Bài viết rất hay và chi tiết!\","
                + "  \"createdAt\": \"2026-10-25T16:00:00\","
                + "  \"upvoteCount\": 5,"
                + "  \"downvoteCount\": 0,"
                + "  \"replies\": ["
                + "    {"
                + "      \"id\": 502,"
                + "      \"parentId\": 501,"
                + "      \"userId\": \"user-student-2\","
                + "      \"content\": \"Bình luận con hữu ích 1!\","
                + "      \"createdAt\": \"2026-10-25T16:05:00\","
                + "      \"upvoteCount\": 2,"
                + "      \"downvoteCount\": 0,"
                + "      \"replies\": []"
                + "    },"
                + "    {"
                + "      \"id\": 503,"
                + "      \"parentId\": 501,"
                + "      \"userId\": \"user-student-3\","
                + "      \"content\": \"Bình luận con hữu ích 2!\","
                + "      \"createdAt\": \"2026-10-25T16:10:00\","
                + "      \"upvoteCount\": 1,"
                + "      \"downvoteCount\": 0,"
                + "      \"replies\": []"
                + "    }"
                + "  ]"
                + "}"
                + "],\"totalPages\":1,\"totalElements\":3}}";

        mockServer.expect(requestTo(containsString("http://localhost:8080/api/blog/posts/50/comments")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(commentsJson, MediaType.APPLICATION_JSON));

        mockServer.expect(requestTo("http://localhost:8080/api/identity/users/user-student-1/profile"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":1000,\"data\":{\"userId\":\"user-student-1\",\"fullname\":\"Học viên A\"}}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://localhost:8080/api/identity/users/user-student-2/profile"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":1000,\"data\":{\"userId\":\"user-student-2\",\"fullname\":\"Học viên B\"}}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://localhost:8080/api/identity/users/user-student-3/profile"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":1000,\"data\":{\"userId\":\"user-student-3\",\"fullname\":\"Học viên C\"}}", MediaType.APPLICATION_JSON));

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

        // Assert dữ liệu bài viết & bình luận 2 cấp
        assertThat(html).contains("data-post-id=\"50\"");
        assertThat(html).contains("data-comment-id=\"501\"");
        assertThat(html).contains("data-comment-id=\"502\"");
        assertThat(html).contains("data-comment-id=\"503\"");
        assertThat(html).contains("Bài viết rất hay và chi tiết!");
        assertThat(html).contains("Bình luận con hữu ích 1!");
        assertThat(html).contains("Bình luận con hữu ích 2!");
        assertThat(html).contains("Học viên A");
        assertThat(html).contains("Học viên B");
        assertThat(html).contains("Học viên C");

        // Giới hạn 2 cấp: Bình luận gốc (501) có form gửi reply
        assertThat(html).contains("btn-submit-reply\" data-parent-id=\"501\"");
        // Bình luận con (502, 503) tuyệt đối KHÔNG có nút trả lời hay form gửi reply
        assertThat(html).doesNotContain("btn-submit-reply\" data-parent-id=\"502\"");
        assertThat(html).doesNotContain("btn-submit-reply\" data-parent-id=\"503\"");
    }

    @Test
    @DisplayName("renderTwoLevelCommentsTreePreservesHierarchyAndRejectsThirdLevel — Render cây bình luận 2 cấp chuẩn, cấm cấp 3 và chống XSS")
    void renderTwoLevelCommentsTreePreservesHierarchyAndRejectsThirdLevel() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/blog/posts/public/nested-comments-post"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"id\":88,\"title\":\"Bai Viet Co Binh Luan Long Nhau\",\"slug\":\"nested-comments-post\",\"content\":\"Noi dung bai viet\",\"authorId\":\"user-author\",\"createdAt\":\"2026-10-25T10:00:00\"}}",
                        MediaType.APPLICATION_JSON
                ));

        // Mock profile cho tác giả
        mockServer.expect(requestTo("http://localhost:8080/api/identity/users/user-author/profile"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"userId\":\"user-author\",\"fullname\":\"Tac Gia Goc\"}}",
                        MediaType.APPLICATION_JSON
                ));

        // Response bình luận 2 cấp chuẩn backend: Cha (501) -> Con 1 (502) & Con 2 (503)
        String nestedCommentsJson = "{\"code\":1000,\"data\":{\"content\":["
                + "{"
                + "  \"id\": 501,"
                + "  \"userId\": \"user-parent\","
                + "  \"content\": \"Bình luận gốc cấp 0\","
                + "  \"upvoteCount\": 10,"
                + "  \"downvoteCount\": 1,"
                + "  \"replies\": ["
                + "    {"
                + "      \"id\": 502,"
                + "      \"parentId\": 501,"
                + "      \"userId\": \"user-child-1\","
                + "      \"content\": \"Bình luận con 1 <script>alert('xss-child-1')</script>\","
                + "      \"upvoteCount\": 4,"
                + "      \"downvoteCount\": 0,"
                + "      \"replies\": []"
                + "    },"
                + "    {"
                + "      \"id\": 503,"
                + "      \"parentId\": 501,"
                + "      \"userId\": \"user-child-2\","
                + "      \"content\": \"Bình luận con 2 <script>alert('xss-child-2')</script>\","
                + "      \"upvoteCount\": 2,"
                + "      \"downvoteCount\": 0,"
                + "      \"replies\": []"
                + "    }"
                + "  ]"
                + "}"
                + "],\"totalPages\":1,\"totalElements\":3}}";

        mockServer.expect(requestTo(containsString("http://localhost:8080/api/blog/posts/88/comments")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(nestedCommentsJson, MediaType.APPLICATION_JSON));

        // Mock profile cho người bình luận
        mockServer.expect(requestTo("http://localhost:8080/api/identity/users/user-parent/profile"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":1000,\"data\":{\"userId\":\"user-parent\",\"fullname\":\"Nguyen Van Cha\"}}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://localhost:8080/api/identity/users/user-child-1/profile"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":1000,\"data\":{\"userId\":\"user-child-1\",\"fullname\":\"Nguyen Thi Con 1\"}}", MediaType.APPLICATION_JSON));
        mockServer.expect(requestTo("http://localhost:8080/api/identity/users/user-child-2/profile"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":1000,\"data\":{\"userId\":\"user-child-2\",\"fullname\":\"Nguyen Thi Con 2\"}}", MediaType.APPLICATION_JSON));

        String html = mockMvc.perform(get("/blog/nested-comments-post"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 1. Kiểm tra hiển thị đủ bình luận 2 cấp: Cha (501), Con 1 (502), Con 2 (503)
        assertThat(html).contains("data-comment-id=\"501\"");
        assertThat(html).contains("data-comment-id=\"502\"");
        assertThat(html).contains("data-comment-id=\"503\"");

        // 2. Cả 2 con đều có parentId trỏ về cha 501
        assertThat(html).contains("data-parent-id=\"501\"");

        // 3. Kiểm tra nội dung text
        assertThat(html).contains("Bình luận gốc cấp 0");
        assertThat(html).contains("Bình luận con 1");
        assertThat(html).contains("Bình luận con 2");

        // 4. Kiểm tra an toàn XSS: không thẻ script nào được render thực thi
        assertThat(html).doesNotContain("<script>alert('xss-child-1')</script>");
        assertThat(html).doesNotContain("<script>alert('xss-child-2')</script>");
        assertThat(html).contains("&lt;script&gt;alert(");

        // 5. Kiểm tra thông tin tác giả bình luận đã được enrich
        assertThat(html).contains("Nguyen Van Cha");
        assertThat(html).contains("Nguyen Thi Con 1");
        assertThat(html).contains("Nguyen Thi Con 2");

        // 6. Ràng buộc 2 cấp: bình luận con không có nút trả lời hay form gửi reply
        assertThat(html).contains("btn-submit-reply\" data-parent-id=\"501\"");
        assertThat(html).doesNotContain("btn-submit-reply\" data-parent-id=\"502\"");
        assertThat(html).doesNotContain("btn-submit-reply\" data-parent-id=\"503\"");
    }
}
