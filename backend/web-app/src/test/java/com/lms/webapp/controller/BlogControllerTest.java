package com.lms.webapp.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureMockRestServiceServer;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureMockRestServiceServer
@ActiveProfiles("test")
class BlogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MockRestServiceServer mockServer;

    @Test
    @DisplayName("Test 1: publicBlogListCallsPublicEndpoint — GET /blog công khai gọi /api/blog/posts/public")
    void publicBlogListCallsPublicEndpoint() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/blog/tags"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":[{\"id\":1,\"name\":\"Java\",\"slug\":\"java\"}]}",
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo(containsString("http://localhost:8080/api/blog/posts/public")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"content\":[{\"id\":10,\"title\":\"Hoc Spring Boot 3\",\"slug\":\"hoc-spring-boot-3\",\"summary\":\"Bai viet hay\",\"viewCount\":100,\"upvoteCount\":5,\"downvoteCount\":1,\"commentCount\":2,\"tags\":[{\"id\":1,\"name\":\"Java\"}]}],\"pageable\":{\"pageNumber\":0,\"pageSize\":10},\"totalPages\":1,\"totalElements\":1}}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(get("/blog"))
                .andExpect(status().isOk())
                .andExpect(view().name("blog/index"))
                .andExpect(model().attributeExists("posts", "tags"));
    }

    @Test
    @DisplayName("Test 2: publicDetailBySlugRendersMarkdownSafely — GET /blog/{slug} gọi /api/blog/posts/public/{slug}, tra cứu tác giả từ Identity Service và render Markdown an toàn")
    void publicDetailBySlugRendersMarkdownSafely() throws Exception {
        // PostResponse chuẩn backend: chỉ có authorId, không có authorName
        mockServer.expect(requestTo("http://localhost:8080/api/blog/posts/public/hoc-spring-boot-3"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"id\":10,\"title\":\"Hoc Spring Boot 3\",\"slug\":\"hoc-spring-boot-3\",\"content\":\"# Tieu De Lon\\n\\nNoi dung mo ta chi tiet.\",\"authorId\":\"author-123\",\"upvoteCount\":12,\"downvoteCount\":0,\"commentCount\":1}}",
                        MediaType.APPLICATION_JSON
                ));

        // Tra cứu profile tác giả bài viết từ Identity Service
        mockServer.expect(requestTo("http://localhost:8080/api/identity/users/author-123/profile"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"userId\":\"author-123\",\"fullname\":\"Nguyen Van Tac Gia\",\"avatarUrl\":\"https://cdn/author.jpg\"}}",
                        MediaType.APPLICATION_JSON
                ));

        // CommentResponse chuẩn backend: chỉ có userId, không có authorName
        mockServer.expect(requestTo(containsString("http://localhost:8080/api/blog/posts/10/comments")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"content\":[{\"id\":101,\"content\":\"Bai viet rat huu ich!\",\"userId\":\"commenter-456\",\"createdAt\":\"2026-10-25T10:00:00\",\"upvoteCount\":2,\"downvoteCount\":0}],\"totalPages\":1,\"totalElements\":1}}",
                        MediaType.APPLICATION_JSON
                ));

        // Tra cứu profile người bình luận từ Identity Service
        mockServer.expect(requestTo("http://localhost:8080/api/identity/users/commenter-456/profile"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"userId\":\"commenter-456\",\"fullname\":\"Tran Thi Binh Luan\",\"avatarUrl\":\"https://cdn/commenter.jpg\"}}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(get("/blog/hoc-spring-boot-3"))
                .andExpect(status().isOk())
                .andExpect(view().name("blog/detail"))
                .andExpect(model().attributeExists("post", "renderedContent", "comments"));
    }

    @Test
    @DisplayName("Test 2b: publicDetailAuthorLookupFailureFallsBackGracefully — Khi tra cứu tác giả lỗi 404, fallback an toàn không ném ngoại lệ")
    void publicDetailAuthorLookupFailureFallsBackGracefully() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/blog/posts/public/hoc-spring-boot-3"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"id\":10,\"title\":\"Hoc Spring Boot 3\",\"slug\":\"hoc-spring-boot-3\",\"content\":\"Noi dung\",\"authorId\":\"deleted-user-999\"}}",
                        MediaType.APPLICATION_JSON
                ));

        // Identity trả về 404 Not Found (user đã bị xóa)
        mockServer.expect(requestTo("http://localhost:8080/api/identity/users/deleted-user-999/profile"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withStatus(org.springframework.http.HttpStatus.NOT_FOUND));

        mockServer.expect(requestTo(containsString("http://localhost:8080/api/blog/posts/10/comments")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"content\":[],\"totalPages\":0,\"totalElements\":0}}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(get("/blog/hoc-spring-boot-3"))
                .andExpect(status().isOk())
                .andExpect(view().name("blog/detail"));
    }

    @Test
    @DisplayName("Test 3a: postByIdUnauthenticatedRedirectsToLogin — Chưa đăng nhập bị chuyển hướng về /login")
    void postByIdUnauthenticatedRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/blog/view/10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));
    }

    @Test
    @WithMockUser(username = "testuser")
    @DisplayName("Test 3b: postByIdAuthenticatedReturnsDetailView — Đã đăng nhập gọi /api/blog/posts/10 và trả về detail view")
    void postByIdAuthenticatedReturnsDetailView() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/blog/posts/10"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"id\":10,\"title\":\"Hoc Spring Boot 3\",\"slug\":\"hoc-spring-boot-3\",\"content\":\"# Noi dung\",\"authorId\":\"author-123\",\"upvoteCount\":0,\"downvoteCount\":0}}",
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo("http://localhost:8080/api/identity/users/author-123/profile"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"userId\":\"author-123\",\"fullname\":\"Tac Gia Mau\"}}",
                        MediaType.APPLICATION_JSON
                ));

        mockServer.expect(requestTo(containsString("http://localhost:8080/api/blog/posts/10/comments")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"content\":[],\"totalPages\":0,\"totalElements\":0}}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(get("/blog/view/10"))
                .andExpect(status().isOk())
                .andExpect(view().name("blog/detail"));
    }

    @Test
    @WithMockUser(username = "instructor1", roles = {"USER"})
    @DisplayName("Test 4: createPostMultipartCallsGateway — POST /blog/new gửi multipart với tên part 'thumbnail', filename và content type")
    void createPostMultipartCallsGateway() throws Exception {
        // Bắt buộc request gửi sang Gateway phải có part tên là "thumbnail" (chứ không phải "file"), có filename và content type
        mockServer.expect(requestTo("http://localhost:8080/api/blog/posts"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("name=\"thumbnail\"; filename=\"cover.png\"")))
                .andExpect(content().string(containsString("Content-Type: image/png")))
                .andExpect(content().string(containsString("fake image content")))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"id\":25,\"title\":\"Bai Viet Moi\",\"slug\":\"bai-viet-moi\"}}",
                        MediaType.APPLICATION_JSON
                ));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "cover.png",
                MediaType.IMAGE_PNG_VALUE,
                "fake image content".getBytes()
        );

        mockMvc.perform(multipart("/blog/new")
                        .file(file)
                        .param("title", "Bai Viet Moi Chuyen Sau")
                        .param("summary", "Tom tat bai viet ngan gon")
                        .param("content", "# Chi tiet bai viet markdown")
                        .param("status", "PUBLISHED")
                        .param("tagIds", "1", "2")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/bai-viet-moi"));
    }

    @Test
    @WithMockUser(username = "instructor1", roles = {"USER"})
    @DisplayName("Test 4b: updatePostWithEmptyTagsSendsClearTagsTrue — Sửa bài viết bỏ toàn bộ tags phải gửi clearTags=true và không gửi tagIds")
    void updatePostWithEmptyTagsSendsClearTagsTrue() throws Exception {
        // Khi bỏ chọn hết tags: phải gửi clearTags=true, không gửi tagIds, không gửi thumbnail nếu không có file mới
        mockServer.expect(requestTo("http://localhost:8080/api/blog/posts/10"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().string(containsString("name=\"clearTags\"")))
                .andExpect(content().string(containsString("true")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("name=\"tagIds\""))))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("name=\"thumbnail\""))))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"id\":10,\"title\":\"Bai Viet Da Sua\",\"slug\":\"bai-viet-da-sua\"}}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(multipart("/blog/10/edit")
                        .param("title", "Bai Viet Da Sua")
                        .param("summary", "Tom tat moi")
                        .param("content", "Noi dung markdown moi")
                        .param("status", "PUBLISHED")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/bai-viet-da-sua"));
    }

    @Test
    @WithMockUser(username = "instructor1", roles = {"USER"})
    @DisplayName("Test 4c: updatePostWithNewThumbnailAndSelectedTagsSendsBothCorrectly — Sửa bài có thumbnail mới và tags phải gửi part 'thumbnail' và 'tagIds'")
    void updatePostWithNewThumbnailAndSelectedTagsSendsBothCorrectly() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/blog/posts/10"))
                .andExpect(method(HttpMethod.PUT))
                .andExpect(content().string(containsString("name=\"thumbnail\"; filename=\"new-thumb.png\"")))
                .andExpect(content().string(containsString("name=\"tagIds\"")))
                .andExpect(content().string(org.hamcrest.Matchers.not(containsString("name=\"clearTags\""))))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"id\":10,\"title\":\"Bai Viet Da Cap Nhat\",\"slug\":\"bai-viet-da-cap-nhat\"}}",
                        MediaType.APPLICATION_JSON
                ));

        MockMultipartFile newThumb = new MockMultipartFile(
                "file",
                "new-thumb.png",
                MediaType.IMAGE_PNG_VALUE,
                "new thumb content".getBytes()
        );

        mockMvc.perform(multipart("/blog/10/edit")
                        .file(newThumb)
                        .param("title", "Bai Viet Da Cap Nhat")
                        .param("summary", "Tom tat cap nhat")
                        .param("content", "Noi dung cap nhat")
                        .param("status", "PUBLISHED")
                        .param("tagIds", "3", "4")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/blog/bai-viet-da-cap-nhat"));
    }

    @Test
    @WithMockUser(username = "user1")
    @DisplayName("Test 5a: addCommentCallsGateway — Thêm comment POST /blog/posts/{id}/comments gọi Gateway")
    void addCommentCallsGateway() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/blog/posts/10/comments"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"id\":105,\"content\":\"Binh luan test\",\"authorName\":\"user1\"}}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(post("/blog/posts/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Binh luan test\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.id").value(105));
    }

    @Test
    @WithMockUser(username = "user1")
    @DisplayName("Test 5b: updateCommentCallsGateway — Sửa comment PUT /blog/posts/{id}/comments/{cid} gọi Gateway")
    void updateCommentCallsGateway() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/blog/posts/10/comments/105"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"id\":105,\"content\":\"Binh luan da cap nhat\"}}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(put("/blog/posts/10/comments/105")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Binh luan da cap nhat\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("Binh luan da cap nhat"));
    }

    @Test
    @WithMockUser(username = "user1")
    @DisplayName("Test 5c: deleteCommentCallsGateway — Xóa comment DELETE /blog/posts/{id}/comments/{cid} gọi Gateway")
    void deleteCommentCallsGateway() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/blog/posts/10/comments/105"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"message\":\"Xoa binh luan thanh cong\"}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(delete("/blog/posts/10/comments/105")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Xoa binh luan thanh cong"));
    }

    @Test
    @WithMockUser(username = "user1")
    @DisplayName("Test 6: votePostCallsVoteApi — Vote bài viết gửi đúng voteType UPVOTE")
    void votePostCallsVoteApi() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/blog/posts/10/vote"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"voteType\":\"UPVOTE\"")))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"upvoteCount\":11,\"downvoteCount\":2,\"currentVote\":\"UPVOTE\"}}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(post("/blog/posts/10/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voteType\":\"UPVOTE\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.upvoteCount").value(11))
                .andExpect(jsonPath("$.data.currentVote").value("UPVOTE"));
    }

    @Test
    @WithMockUser(username = "user1")
    @DisplayName("Test 7: voteCommentCallsCommentVoteApi — Vote bình luận gọi đúng URL /posts/10/comments/101/vote")
    void voteCommentCallsCommentVoteApi() throws Exception {
        mockServer.expect(requestTo("http://localhost:8080/api/blog/posts/10/comments/101/vote"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().string(containsString("\"voteType\":\"UPVOTE\"")))
                .andRespond(withSuccess(
                        "{\"code\":1000,\"data\":{\"upvoteCount\":5,\"downvoteCount\":0,\"currentVote\":\"UPVOTE\"}}",
                        MediaType.APPLICATION_JSON
                ));

        mockMvc.perform(post("/blog/posts/10/comments/101/vote")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"voteType\":\"UPVOTE\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.upvoteCount").value(5));
    }
}
