package com.lms.blogservice.service.impl;

import com.lms.blogservice.configuration.GatewayAuthentication;
import com.lms.blogservice.dto.request.VoteRequest;
import com.lms.blogservice.dto.response.VoteResponse;
import com.lms.blogservice.entity.Comment;
import com.lms.blogservice.entity.Post;
import com.lms.blogservice.entity.Vote;
import com.lms.blogservice.enums.PostStatus;
import com.lms.blogservice.enums.TargetType;
import com.lms.blogservice.enums.VoteType;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoteServiceImplTest {

    @Mock
    PostRepository postRepository;

    @Mock
    CommentRepository commentRepository;

    @Mock
    VoteRepository voteRepository;

    @InjectMocks
    VoteServiceImpl voteService;

    private static final String VOTER_ID = "voter-123";
    private static final String AUTHOR_ID = "author-456";

    @BeforeEach
    void setUp() {
        GatewayAuthentication auth = new GatewayAuthentication(
                VOTER_ID, "voter@example.com", "ROLE_STUDENT",
                List.of(new SimpleGrantedAuthority("ROLE_STUDENT"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("13. Bấm UPVOTE lần đầu tăng 1 điểm vote")
    void votePostUpvoteFirstTimeIncreasesScore() {
        Long postId = 1L;
        Post post = Post.builder().id(postId).authorId(AUTHOR_ID).status(PostStatus.PUBLISHED).build();
        VoteRequest request = VoteRequest.builder().voteType(VoteType.UPVOTE).build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(voteRepository.findByUserIdAndTargetIdAndTargetType(VOTER_ID, postId, TargetType.POST))
                .thenReturn(Optional.empty());
        when(voteRepository.countByTargetIdAndTargetTypeAndVoteType(postId, TargetType.POST, VoteType.UPVOTE))
                .thenReturn(1L);
        when(voteRepository.countByTargetIdAndTargetTypeAndVoteType(postId, TargetType.POST, VoteType.DOWNVOTE))
                .thenReturn(0L);

        VoteResponse response = voteService.votePost(postId, request);

        assertNotNull(response);
        assertEquals(VoteType.UPVOTE, response.getVoteType());
        assertEquals(1L, response.getUpvoteCount());
        assertEquals(0L, response.getDownvoteCount());
        verify(voteRepository).save(any(Vote.class));
    }

    @Test
    @DisplayName("14. Bấm lại UPVOTE lần thứ hai hủy vote và giảm điểm")
    void votePostToggleSameVoteCancelsVote() {
        Long postId = 1L;
        Post post = Post.builder().id(postId).authorId(AUTHOR_ID).status(PostStatus.PUBLISHED).build();
        Vote existingVote = Vote.builder()
                .id(10L)
                .userId(VOTER_ID)
                .targetId(postId)
                .targetType(TargetType.POST)
                .voteType(VoteType.UPVOTE)
                .build();
        VoteRequest request = VoteRequest.builder().voteType(VoteType.UPVOTE).build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(voteRepository.findByUserIdAndTargetIdAndTargetType(VOTER_ID, postId, TargetType.POST))
                .thenReturn(Optional.of(existingVote));
        when(voteRepository.countByTargetIdAndTargetTypeAndVoteType(postId, TargetType.POST, VoteType.UPVOTE))
                .thenReturn(0L);
        when(voteRepository.countByTargetIdAndTargetTypeAndVoteType(postId, TargetType.POST, VoteType.DOWNVOTE))
                .thenReturn(0L);

        VoteResponse response = voteService.votePost(postId, request);

        assertNotNull(response);
        assertNull(response.getVoteType(), "Vote type should be null after cancellation");
        assertEquals(0L, response.getUpvoteCount());
        verify(voteRepository).delete(existingVote);
    }

    @Test
    @DisplayName("15. Đổi chiều từ UPVOTE sang DOWNVOTE cập nhật loại vote")
    void votePostSwitchUpToDownDecreasesTwo() {
        Long postId = 1L;
        Post post = Post.builder().id(postId).authorId(AUTHOR_ID).status(PostStatus.PUBLISHED).build();
        Vote existingVote = Vote.builder()
                .id(10L)
                .userId(VOTER_ID)
                .targetId(postId)
                .targetType(TargetType.POST)
                .voteType(VoteType.UPVOTE)
                .build();
        VoteRequest request = VoteRequest.builder().voteType(VoteType.DOWNVOTE).build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(voteRepository.findByUserIdAndTargetIdAndTargetType(VOTER_ID, postId, TargetType.POST))
                .thenReturn(Optional.of(existingVote));
        when(voteRepository.countByTargetIdAndTargetTypeAndVoteType(postId, TargetType.POST, VoteType.UPVOTE))
                .thenReturn(0L);
        when(voteRepository.countByTargetIdAndTargetTypeAndVoteType(postId, TargetType.POST, VoteType.DOWNVOTE))
                .thenReturn(1L);

        VoteResponse response = voteService.votePost(postId, request);

        assertNotNull(response);
        assertEquals(VoteType.DOWNVOTE, response.getVoteType());
        assertEquals(0L, response.getUpvoteCount());
        assertEquals(1L, response.getDownvoteCount());
        assertEquals(VoteType.DOWNVOTE, existingVote.getVoteType());
        verify(voteRepository).save(existingVote);
    }

    @Test
    @DisplayName("16. Bình chọn UPVOTE cho bình luận thành công")
    void voteCommentValidCommentUpdatesScore() {
        Long postId = 1L;
        Long commentId = 50L;
        Post post = Post.builder().id(postId).status(PostStatus.PUBLISHED).build();
        Comment comment = Comment.builder().id(commentId).post(post).userId(AUTHOR_ID).build();
        VoteRequest request = VoteRequest.builder().voteType(VoteType.UPVOTE).build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(comment));
        when(voteRepository.findByUserIdAndTargetIdAndTargetType(VOTER_ID, commentId, TargetType.COMMENT))
                .thenReturn(Optional.empty());
        when(voteRepository.countByTargetIdAndTargetTypeAndVoteType(commentId, TargetType.COMMENT, VoteType.UPVOTE))
                .thenReturn(1L);
        when(voteRepository.countByTargetIdAndTargetTypeAndVoteType(commentId, TargetType.COMMENT, VoteType.DOWNVOTE))
                .thenReturn(0L);

        VoteResponse response = voteService.voteComment(postId, commentId, request);

        assertNotNull(response);
        assertEquals(VoteType.UPVOTE, response.getVoteType());
        assertEquals(1L, response.getUpvoteCount());
        verify(voteRepository).save(any(Vote.class));
    }
}
