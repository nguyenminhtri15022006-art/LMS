package com.lms.blogservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lms.blogservice.dto.request.CreateCommentRequest;
import com.lms.blogservice.dto.request.VoteRequest;
import com.lms.blogservice.enums.VoteType;
import com.lms.blogservice.repository.CommentRepository;
import com.lms.blogservice.repository.PostRepository;
import com.lms.blogservice.repository.TagRepository;
import com.lms.blogservice.repository.VoteRepository;
import io.minio.MinioClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Functional Test cho chu trình nghiệp vụ hoàn chỉnh của Blog Service (Phase 9).
 *
 * <p>Mục tiêu: Tạo bài viết -> Đọc bài -> Bình luận lồng nhau -> Vote bài viết và vote bình luận.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BlogFunctionalTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private VoteRepository voteRepository;

    @MockBean
    private MinioClient minioClient;

    private static final String AUTHOR_ID = "author-workflow-1";
    private static final String READER_ID = "reader-workflow-2";

    @BeforeEach
    void setUp() {
        cleanData();
    }

    @AfterEach
    void tearDown() {
        cleanData();
        SecurityContextHolder.clearContext();
    }

    private void cleanData() {
        voteRepository.deleteAll();
        commentRepository.deleteAll();
        postRepository.deleteAll();
        tagRepository.deleteAll();
    }

    @Test
    @DisplayName("25. userCreatesPostAddsCommentAndVotesWorkflow: Chu trình hoàn chỉnh Tạo bài viết -> Đọc bài -> Bình luận lồng nhau -> Vote bài viết và vote bình luận")
    void userCreatesPostAddsCommentAndVotesWorkflow() throws Exception {
        // 1. Tác giả tạo bài viết mới (Multipart Request)
        MockMultipartFile file = new MockMultipartFile(
                "thumbnail",
                "test.png",
                MediaType.IMAGE_PNG_VALUE,
                "dummy image content".getBytes()
        );

        MvcResult createPostResult = mockMvc.perform(multipart("/posts")
                        .file(file)
                        .param("title", "Bài viết thực chiến Spring Boot và VueJS")
                        .param("content", "# Hướng dẫn thực hành\n\nNội dung Markdown chi tiết bài viết.")
                        .param("summary", "Tóm tắt bài viết hướng dẫn")
                        .param("status", "PUBLISHED")
                        .header("X-User-Id", AUTHOR_ID)
                        .header("X-User-Role", "ROLE_INSTRUCTOR")
                        .header("X-User-Email", "author@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.title").value("Bài viết thực chiến Spring Boot và VueJS"))
                .andReturn();

        String createPostResponseStr = createPostResult.getResponse().getContentAsString();
        Long postId = objectMapper.readTree(createPostResponseStr).path("data").path("id").asLong();
        String slug = objectMapper.readTree(createPostResponseStr).path("data").path("slug").asText();

        // 2. Người đọc xem chi tiết bài viết qua slug công khai (Public API -> viewCount tự động tăng)
        mockMvc.perform(get("/posts/public/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.slug").value(slug))
                .andExpect(jsonPath("$.data.viewCount").value(1));

        // 3. Người đọc gửi bình luận gốc vào bài viết
        CreateCommentRequest rootCommentReq = CreateCommentRequest.builder()
                .content("Cảm ơn tác giả, bài viết giải thích rất chi tiết!")
                .parentId(null)
                .build();

        MvcResult rootCommentResult = mockMvc.perform(post("/posts/{postId}/comments", postId)
                        .header("X-User-Id", READER_ID)
                        .header("X-User-Role", "ROLE_STUDENT")
                        .header("X-User-Email", "reader@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rootCommentReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.content").value("Cảm ơn tác giả, bài viết giải thích rất chi tiết!"))
                .andReturn();

        Long rootCommentId = objectMapper.readTree(rootCommentResult.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        // 4. Tác giả trả lời (reply) vào bình luận gốc
        CreateCommentRequest replyReq = CreateCommentRequest.builder()
                .content("Cảm ơn bạn đã đọc bài! Hãy bình chọn nếu thấy hữu ích nhé.")
                .parentId(rootCommentId)
                .build();

        mockMvc.perform(post("/posts/{postId}/comments", postId)
                        .header("X-User-Id", AUTHOR_ID)
                        .header("X-User-Role", "ROLE_INSTRUCTOR")
                        .header("X-User-Email", "author@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.parentId").value(rootCommentId));

        // 5. Người đọc bình chọn UPVOTE cho bài viết
        VoteRequest votePostReq = VoteRequest.builder()
                .voteType(VoteType.UPVOTE)
                .build();

        mockMvc.perform(post("/posts/{postId}/vote", postId)
                        .header("X-User-Id", READER_ID)
                        .header("X-User-Role", "ROLE_STUDENT")
                        .header("X-User-Email", "reader@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(votePostReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.voteType").value("UPVOTE"))
                .andExpect(jsonPath("$.data.upvoteCount").value(1));

        // 6. Tác giả bình chọn UPVOTE cho bình luận gốc của người đọc
        VoteRequest voteCommentReq = VoteRequest.builder()
                .voteType(VoteType.UPVOTE)
                .build();

        mockMvc.perform(post("/posts/{postId}/comments/{commentId}/vote", postId, rootCommentId)
                        .header("X-User-Id", AUTHOR_ID)
                        .header("X-User-Role", "ROLE_INSTRUCTOR")
                        .header("X-User-Email", "author@example.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(voteCommentReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.voteType").value("UPVOTE"))
                .andExpect(jsonPath("$.data.upvoteCount").value(1));

        // 7. Truy vấn danh sách bình luận công khai để xác nhận comment gốc lồng replies và vote counts
        mockMvc.perform(get("/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.data.content[0].id").value(rootCommentId))
                .andExpect(jsonPath("$.data.content[0].upvoteCount").value(1))
                .andExpect(jsonPath("$.data.content[0].replies[0].parentId").value(rootCommentId));
    }
}
