package com.lms.blogservice.service.impl;

import com.lms.blogservice.configuration.GatewayAuthentication;
import com.lms.blogservice.dto.request.CreatePostRequest;
import com.lms.blogservice.dto.request.UpdatePostRequest;
import com.lms.blogservice.dto.response.PostResponse;
import com.lms.blogservice.dto.response.PostSummaryResponse;
import com.lms.blogservice.entity.Post;
import com.lms.blogservice.enums.PostStatus;
import com.lms.blogservice.enums.TargetType;
import com.lms.blogservice.enums.VoteType;
import com.lms.blogservice.exception.BlogException;
import com.lms.blogservice.exception.ErrorCode;
import com.lms.blogservice.mapper.PostMapper;
import com.lms.blogservice.repository.CommentRepository;
import com.lms.blogservice.repository.PostRepository;
import com.lms.blogservice.repository.TagRepository;
import com.lms.blogservice.repository.VoteRepository;
import com.lms.blogservice.service.MinioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock
    PostRepository postRepository;

    @Mock
    TagRepository tagRepository;

    @Mock
    CommentRepository commentRepository;

    @Mock
    VoteRepository voteRepository;

    @Mock
    PostMapper postMapper;

    @Mock
    MinioService minioService;

    @InjectMocks
    PostServiceImpl postService;

    private static final String USER_ID = "user-123";
    private static final String OTHER_USER_ID = "user-999";

    @BeforeEach
    void setUp() {
        GatewayAuthentication auth = new GatewayAuthentication(
                USER_ID, "user@example.com", "ROLE_STUDENT",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("1. Xem danh sách bài viết public chỉ lấy các bài có trạng thái PUBLISHED")
    void getPublicPostsPublishedOnlyReturnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Post post = Post.builder()
                .id(1L)
                .title("Bài viết Spring Boot")
                .status(PostStatus.PUBLISHED)
                .build();
        Page<Post> postPage = new PageImpl<>(List.of(post));

        when(postRepository.findByStatus(PostStatus.PUBLISHED, pageable)).thenReturn(postPage);
        when(postMapper.toPostSummaryResponse(post)).thenReturn(
                PostSummaryResponse.builder().id(1L).title("Bài viết Spring Boot").status(PostStatus.PUBLISHED).build()
        );
        when(commentRepository.countByPostId(1L)).thenReturn(5L);
        when(voteRepository.countByTargetIdAndTargetTypeAndVoteType(1L, TargetType.POST, VoteType.UPVOTE)).thenReturn(10L);

        Page<PostSummaryResponse> result = postService.getPosts(pageable, PostStatus.PUBLISHED, null, null);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals(1L, result.getContent().get(0).getId());
        assertEquals(5L, result.getContent().get(0).getCommentCount());
        assertEquals(10L, result.getContent().get(0).getUpvoteCount());
        verify(postRepository).findByStatus(PostStatus.PUBLISHED, pageable);
    }

    @Test
    @DisplayName("2. Lấy bài viết qua slug tự động tăng viewCount")
    void getPostBySlugPublicIncrementsViewCount() {
        String slug = "bai-viet-spring-boot";
        Post post = Post.builder()
                .id(1L)
                .title("Bài viết Spring Boot")
                .slug(slug)
                .status(PostStatus.PUBLISHED)
                .viewCount(10L)
                .authorId(USER_ID)
                .build();

        when(postRepository.findBySlug(slug)).thenReturn(Optional.of(post));
        when(postMapper.toPostResponse(post)).thenReturn(
                PostResponse.builder().id(1L).slug(slug).viewCount(11L).build()
        );
        when(commentRepository.countByPostId(1L)).thenReturn(2L);
        when(voteRepository.countByTargetIdAndTargetTypeAndVoteType(1L, TargetType.POST, VoteType.UPVOTE)).thenReturn(5L);
        when(voteRepository.countByTargetIdAndTargetTypeAndVoteType(1L, TargetType.POST, VoteType.DOWNVOTE)).thenReturn(1L);

        PostResponse response = postService.getPostBySlug(slug);

        assertNotNull(response);
        assertEquals(11L, post.getViewCount());
        verify(postRepository).save(post);
    }

    @Test
    @DisplayName("3. Tạo bài viết mới thành công với nội dung Markdown và tự động sinh slug")
    void createPostValidMarkdownSuccess() {
        CreatePostRequest request = CreatePostRequest.builder()
                .title("Hướng dẫn Markdown trong Spring Boot")
                .content("# Tiêu đề lớn\n\nNội dung Markdown **in đậm**")
                .summary("Tóm tắt bài viết")
                .status(PostStatus.PUBLISHED)
                .build();

        Post newPost = Post.builder()
                .title(request.getTitle())
                .content(request.getContent())
                .summary(request.getSummary())
                .status(request.getStatus())
                .build();

        Post savedPost = Post.builder()
                .id(10L)
                .title(request.getTitle())
                .slug("huong-dan-markdown-trong-spring-boot")
                .content(request.getContent())
                .summary(request.getSummary())
                .status(request.getStatus())
                .authorId(USER_ID)
                .viewCount(0L)
                .build();

        when(postRepository.existsByTitle(request.getTitle())).thenReturn(false);
        when(postMapper.toPost(request)).thenReturn(newPost);
        when(postRepository.existsBySlug(any())).thenReturn(false);
        when(postRepository.save(any(Post.class))).thenReturn(savedPost);
        when(postMapper.toPostResponse(savedPost)).thenReturn(
                PostResponse.builder().id(10L).title(request.getTitle()).slug("huong-dan-markdown-trong-spring-boot").build()
        );

        PostResponse response = postService.createPost(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        verify(postRepository).save(any(Post.class));
    }

    @Test
    @DisplayName("4. Tác giả cập nhật bài viết của mình thành công")
    void updatePostAuthorSuccess() {
        Long postId = 1L;
        Post existingPost = Post.builder()
                .id(postId)
                .title("Tiêu đề cũ")
                .content("Nội dung cũ")
                .authorId(USER_ID)
                .tags(new ArrayList<>())
                .build();

        UpdatePostRequest updateRequest = UpdatePostRequest.builder()
                .title("Tiêu đề mới")
                .content("Nội dung mới")
                .status(PostStatus.PUBLISHED)
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postRepository.existsByTitle("Tiêu đề mới")).thenReturn(false);
        when(postRepository.save(existingPost)).thenReturn(existingPost);
        when(postMapper.toPostResponse(existingPost)).thenReturn(
                PostResponse.builder().id(postId).title("Tiêu đề mới").build()
        );

        PostResponse response = postService.updatePost(postId, updateRequest);

        assertNotNull(response);
        verify(postMapper).updatePost(existingPost, updateRequest);
        verify(postRepository).save(existingPost);
    }

    @Test
    @DisplayName("5. Người không phải tác giả cập nhật ném lỗi 403 NOT_POST_OWNER")
    void updatePostNotAuthorThrowsForbidden() {
        Long postId = 1L;
        Post existingPost = Post.builder()
                .id(postId)
                .title("Bài viết của tác giả khác")
                .authorId(OTHER_USER_ID)
                .build();

        UpdatePostRequest updateRequest = UpdatePostRequest.builder()
                .title("Tiêu đề cố tình sửa")
                .content("Nội dung cố tình sửa")
                .status(PostStatus.PUBLISHED)
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));

        BlogException exception = assertThrows(BlogException.class, () ->
                postService.updatePost(postId, updateRequest)
        );

        assertEquals(ErrorCode.NOT_POST_OWNER, exception.getErrorCode());
        verify(postRepository, never()).save(any());
    }

    @Test
    @DisplayName("6. Tác giả xóa bài viết của mình thành công")
    void deletePostAuthorSuccess() {
        Long postId = 1L;
        Post existingPost = Post.builder()
                .id(postId)
                .title("Bài viết cần xóa")
                .authorId(USER_ID)
                .thumbnail("http://localhost:9000/lms-blog-bucket/thumbnails/thumb.jpg")
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));

        postService.deletePost(postId);

        verify(minioService).deleteFile(existingPost.getThumbnail());
        verify(postRepository).delete(existingPost);
    }
}
