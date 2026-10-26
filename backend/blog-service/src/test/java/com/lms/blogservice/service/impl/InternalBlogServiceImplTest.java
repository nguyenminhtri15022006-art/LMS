package com.lms.blogservice.service.impl;

import com.lms.blogservice.dto.response.InternalPostResponse;
import com.lms.blogservice.entity.Post;
import com.lms.blogservice.enums.PostStatus;
import com.lms.blogservice.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalBlogServiceImplTest {

    @Mock
    PostRepository postRepository;

    @InjectMocks
    InternalBlogServiceImpl internalBlogService;

    @Test
    @DisplayName("18. Service nội bộ truy vấn thông tin bài viết theo postId trả về InternalPostResponse")
    void internalGetPostReturnsPostResponse() {
        Long postId = 1L;
        Post post = Post.builder()
                .id(postId)
                .title("Bài viết nội bộ Feign")
                .authorId("author-123")
                .status(PostStatus.PUBLISHED)
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        InternalPostResponse response = internalBlogService.getPost(postId);

        assertNotNull(response);
        assertEquals(postId, response.getId());
        assertEquals("Bài viết nội bộ Feign", response.getTitle());
        assertEquals("author-123", response.getAuthorId());
        assertEquals(PostStatus.PUBLISHED, response.getStatus());
        verify(postRepository).findById(postId);
    }

    @Test
    @DisplayName("19. Service nội bộ kiểm tra bài viết tồn tại trong CSDL trả về boolean")
    void internalExistsPostReturnsTrueWhenExists() {
        Long existingId = 1L;
        Long nonExistingId = 999L;

        when(postRepository.existsById(existingId)).thenReturn(true);
        when(postRepository.existsById(nonExistingId)).thenReturn(false);

        boolean exists = internalBlogService.existsPost(existingId);
        boolean notExists = internalBlogService.existsPost(nonExistingId);

        assertTrue(exists);
        assertFalse(notExists);
        verify(postRepository).existsById(existingId);
        verify(postRepository).existsById(nonExistingId);
    }
}
