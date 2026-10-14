package com.lms.blogservice.service.impl;

import com.lms.blogservice.dto.request.CreateTagRequest;
import com.lms.blogservice.dto.response.TagResponse;
import com.lms.blogservice.entity.Tag;
import com.lms.blogservice.mapper.TagMapper;
import com.lms.blogservice.repository.TagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TagServiceImplTest {

    @Mock
    TagRepository tagRepository;

    @Mock
    TagMapper tagMapper;

    @InjectMocks
    TagServiceImpl tagService;

    @Test
    @DisplayName("7. Lấy danh sách toàn bộ thẻ tag công khai")
    void getAllTagsPublicReturnsList() {
        Tag tag1 = Tag.builder().id(1L).name("Java").slug("java").build();
        Tag tag2 = Tag.builder().id(2L).name("Spring Boot").slug("spring-boot").build();

        TagResponse resp1 = TagResponse.builder().id(1L).name("Java").slug("java").build();
        TagResponse resp2 = TagResponse.builder().id(2L).name("Spring Boot").slug("spring-boot").build();

        when(tagRepository.findAll()).thenReturn(List.of(tag1, tag2));
        when(tagMapper.toTagResponse(tag1)).thenReturn(resp1);
        when(tagMapper.toTagResponse(tag2)).thenReturn(resp2);

        List<TagResponse> results = tagService.getAllTags();

        assertNotNull(results);
        assertEquals(2, results.size());
        assertEquals("Java", results.get(0).getName());
        assertEquals("Spring Boot", results.get(1).getName());
        verify(tagRepository).findAll();
    }

    @Test
    @DisplayName("8. Admin tạo mới thẻ tag thành công")
    void createTagAdminSuccess() {
        CreateTagRequest request = CreateTagRequest.builder()
                .name("Microservices")
                .slug("microservices")
                .build();

        Tag newTag = Tag.builder().name("Microservices").slug("microservices").build();
        Tag savedTag = Tag.builder().id(5L).name("Microservices").slug("microservices").build();
        TagResponse response = TagResponse.builder().id(5L).name("Microservices").slug("microservices").build();

        when(tagRepository.existsByName("Microservices")).thenReturn(false);
        when(tagRepository.existsBySlug("microservices")).thenReturn(false);
        when(tagMapper.toTag(request)).thenReturn(newTag);
        when(tagRepository.save(newTag)).thenReturn(savedTag);
        when(tagMapper.toTagResponse(savedTag)).thenReturn(response);

        TagResponse result = tagService.createTag(request);

        assertNotNull(result);
        assertEquals(5L, result.getId());
        assertEquals("Microservices", result.getName());
        verify(tagRepository).save(newTag);
    }

    @Test
    @DisplayName("9. Admin xóa thẻ tag thành công khỏi CSDL khi chưa có bài viết sử dụng")
    void deleteTagAdminSuccess() {
        Long tagId = 1L;
        Tag tag = Tag.builder().id(tagId).name("OldTag").slug("old-tag").build();

        when(tagRepository.findById(tagId)).thenReturn(Optional.of(tag));
        when(tagRepository.isTagUsedByAnyPost(tagId)).thenReturn(false);

        tagService.deleteTag(tagId);

        verify(tagRepository).delete(tag);
    }
}
