package com.lms.blogservice.service.impl;

import com.lms.blogservice.configuration.GatewayAuthentication;
import com.lms.blogservice.dto.request.CreateCommentRequest;
import com.lms.blogservice.dto.response.CommentResponse;
import com.lms.blogservice.entity.Comment;
import com.lms.blogservice.entity.Post;
import com.lms.blogservice.enums.PostStatus;
import com.lms.blogservice.enums.TargetType;
import com.lms.blogservice.enums.VoteType;
import com.lms.blogservice.mapper.CommentMapper;
import com.lms.blogservice.repository.CommentRepository;
import com.lms.blogservice.repository.PostRepository;
import com.lms.blogservice.repository.VoteRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceImplTest {

    @Mock
    PostRepository postRepository;

    @Mock
    CommentRepository commentRepository;

    @Mock
    VoteRepository voteRepository;

    @Mock
    CommentMapper commentMapper;

    @InjectMocks
    CommentServiceImpl commentService;

    private static final String USER_ID = "user-123";

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
    @DisplayName("10. Bình luận gốc vào bài viết thành công")
    void createRootCommentValidPostSuccess() {
        Long postId = 1L;
        Post post = Post.builder().id(postId).status(PostStatus.PUBLISHED).build();

        CreateCommentRequest request = CreateCommentRequest.builder()
                .content("Bài viết rất hay!")
                .parentId(null)
                .build();

        Comment savedComment = Comment.builder()
                .id(100L)
                .post(post)
                .userId(USER_ID)
                .content("Bài viết rất hay!")
                .build();

        CommentResponse commentResponse = CommentResponse.builder()
                .id(100L)
                .postId(postId)
                .userId(USER_ID)
                .content("Bài viết rất hay!")
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);
        when(commentRepository.findByParentId(100L)).thenReturn(Collections.emptyList());
        when(voteRepository.countByTargetIdsAndTypeAndVoteType(any(), eq(TargetType.COMMENT), eq(VoteType.UPVOTE)))
                .thenReturn(Collections.emptyList());
        when(voteRepository.countByTargetIdsAndTypeAndVoteType(any(), eq(TargetType.COMMENT), eq(VoteType.DOWNVOTE)))
                .thenReturn(Collections.emptyList());
        when(commentMapper.toCommentResponse(savedComment)).thenReturn(commentResponse);

        CommentResponse result = commentService.createComment(postId, request);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        assertEquals("Bài viết rất hay!", result.getContent());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("11. Trả lời bình luận khác lồng vào parentId thành công")
    void createReplyCommentValidParentSuccess() {
        Long postId = 1L;
        Long parentId = 100L;
        Post post = Post.builder().id(postId).status(PostStatus.PUBLISHED).build();

        Comment parentComment = Comment.builder()
                .id(parentId)
                .post(post)
                .userId("other-user")
                .content("Comment gốc")
                .parent(null)
                .build();

        CreateCommentRequest request = CreateCommentRequest.builder()
                .content("Tôi đồng ý với bạn!")
                .parentId(parentId)
                .build();

        Comment savedReply = Comment.builder()
                .id(200L)
                .post(post)
                .userId(USER_ID)
                .content("Tôi đồng ý với bạn!")
                .parent(parentComment)
                .build();

        CommentResponse response = CommentResponse.builder()
                .id(200L)
                .postId(postId)
                .parentId(parentId)
                .userId(USER_ID)
                .content("Tôi đồng ý với bạn!")
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.findById(parentId)).thenReturn(Optional.of(parentComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedReply);
        when(voteRepository.countByTargetIdsAndTypeAndVoteType(any(), eq(TargetType.COMMENT), eq(VoteType.UPVOTE)))
                .thenReturn(Collections.emptyList());
        when(voteRepository.countByTargetIdsAndTypeAndVoteType(any(), eq(TargetType.COMMENT), eq(VoteType.DOWNVOTE)))
                .thenReturn(Collections.emptyList());
        when(commentMapper.toCommentResponse(savedReply)).thenReturn(response);

        CommentResponse result = commentService.createComment(postId, request);

        assertNotNull(result);
        assertEquals(200L, result.getId());
        assertEquals(parentId, result.getParentId());
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("12. Tác giả bình luận xóa bình luận của mình thành công")
    void deleteCommentAuthorSuccess() {
        Long postId = 1L;
        Long commentId = 100L;
        Post post = Post.builder().id(postId).build();

        Comment comment = Comment.builder()
                .id(commentId)
                .post(post)
                .userId(USER_ID)
                .content("Comment cần xóa")
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.findByParentId(commentId)).thenReturn(Collections.emptyList());

        commentService.deleteComment(postId, commentId);

        verify(commentRepository).delete(comment);
    }
}
