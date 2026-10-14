package com.lms.blogservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.blogservice.configuration.SecurityConfig;
import com.lms.blogservice.dto.response.InternalPostResponse;
import com.lms.blogservice.enums.PostStatus;
import com.lms.blogservice.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {
        PostController.class,
        TagController.class,
        VoteController.class,
        InternalBlogController.class
})
@Import(SecurityConfig.class)
class BlogApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PostService postService;

    @MockBean
    private TagService tagService;

    @MockBean
    private CommentService commentService;

    @MockBean
    private VoteService voteService;

    @MockBean
    private InternalBlogService internalBlogService;

    @Test
    @DisplayName("20. postControllerEnforcesAuthenticationOnCreate: Gọi POST /posts thiếu X-User-Id bị chặn 401")
    void postControllerEnforcesAuthenticationOnCreate() throws Exception {
        mockMvc.perform(post("/posts")
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("21. tagControllerEnforcesAdminRoleOnCreate: Người dùng có vai trò STUDENT tạo tag bị chặn 403 Forbidden")
    void tagControllerEnforcesAdminRoleOnCreate() throws Exception {
        String body = """
                {
                    "name": "JavaScript",
                    "slug": "javascript"
                }
                """;

        mockMvc.perform(post("/tags")
                        .header("X-User-Id", "student-user-123")
                        .header("X-User-Role", "ROLE_STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("22. voteControllerValidatesVoteTypeEnum: VoteRequest thiếu voteType trả về lỗi 400 Bad Request")
    void voteControllerValidatesVoteTypeEnum() throws Exception {
        String invalidBody = "{}";

        mockMvc.perform(post("/posts/1/vote")
                        .header("X-User-Id", "user-123")
                        .header("X-User-Role", "ROLE_STUDENT")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(4499));
    }

    @Test
    @DisplayName("23. internalGetPostHttpContractReturns200: HTTP Contract GET /internal/posts/{postId} trả về HTTP 200 và InternalPostResponse")
    void internalGetPostHttpContractReturns200() throws Exception {
        Long postId = 1L;
        InternalPostResponse response = InternalPostResponse.builder()
                .id(postId)
                .title("Bài viết Contract Test")
                .authorId("author-test-123")
                .status(PostStatus.PUBLISHED)
                .build();

        when(internalBlogService.getPost(postId)).thenReturn(response);

        mockMvc.perform(get("/internal/posts/{postId}", postId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.id").value(postId))
                .andExpect(jsonPath("$.data.title").value("Bài viết Contract Test"))
                .andExpect(jsonPath("$.data.authorId").value("author-test-123"))
                .andExpect(jsonPath("$.data.status").value("PUBLISHED"));
    }

    @Test
    @DisplayName("24. internalPostExistsHttpContractReturnsBoolean: HTTP Contract GET /internal/posts/{postId}/exists trả về HTTP 200 và boolean")
    void internalPostExistsHttpContractReturnsBoolean() throws Exception {
        Long postId = 1L;
        when(internalBlogService.existsPost(postId)).thenReturn(true);

        mockMvc.perform(get("/internal/posts/{postId}/exists", postId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data").value(true));
    }
}
