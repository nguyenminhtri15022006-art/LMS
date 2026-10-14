package com.lms.courseservice.service.impl;

import com.lms.courseservice.dto.request.CreateCategoryRequest;
import com.lms.courseservice.dto.request.UpdateCategoryRequest;
import com.lms.courseservice.dto.response.CategoryResponse;
import com.lms.courseservice.entity.mysql.Category;
import com.lms.courseservice.exception.CourseException;
import com.lms.courseservice.exception.ErrorCode;
import com.lms.courseservice.mapper.CategoryMapper;
import com.lms.courseservice.repository.mysql.CategoryRepository;
import com.lms.courseservice.repository.mysql.CourseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category testCategory;
    private CategoryResponse testResponse;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(1L)
                .name("Lập trình Web")
                .slug("lap-trinh-web")
                .createdAt(LocalDateTime.now())
                .courses(new ArrayList<>())
                .build();

        testResponse = CategoryResponse.builder()
                .id(1L)
                .name("Lập trình Web")
                .slug("lap-trinh-web")
                .createdAt(testCategory.getCreatedAt())
                .build();
    }

    @Test
    @DisplayName("1. getAllCategoriesReturnsList: Lấy danh sách danh mục public thành công")
    void getAllCategoriesReturnsList() {
        when(categoryRepository.findAll()).thenReturn(List.of(testCategory));
        when(categoryMapper.toCategoryResponse(testCategory)).thenReturn(testResponse);

        List<CategoryResponse> result = categoryService.getAllCategories();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Lập trình Web", result.get(0).getName());
        verify(categoryRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("2. createCategoryAdminSuccess: Admin tạo danh mục mới thành công")
    void createCategoryAdminSuccess() {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Khoa học Dữ liệu")
                .slug("khoa-hoc-du-lieu")
                .build();

        when(categoryRepository.existsByName(request.getName())).thenReturn(false);
        when(categoryRepository.existsBySlug(request.getSlug())).thenReturn(false);
        when(categoryMapper.toCategory(request)).thenReturn(testCategory);
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);
        when(categoryMapper.toCategoryResponse(testCategory)).thenReturn(testResponse);

        CategoryResponse result = categoryService.createCategory(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    @DisplayName("3. updateCategoryAdminSuccess: Admin cập nhật tên danh mục thành công")
    void updateCategoryAdminSuccess() {
        UpdateCategoryRequest request = UpdateCategoryRequest.builder()
                .name("Lập trình Web Nâng Cao")
                .slug("lap-trinh-web-nang-cao")
                .build();

        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.existsByName(request.getName())).thenReturn(false);
        when(categoryRepository.existsBySlug(request.getSlug())).thenReturn(false);
        when(categoryRepository.save(testCategory)).thenReturn(testCategory);
        when(categoryMapper.toCategoryResponse(testCategory)).thenReturn(testResponse);

        CategoryResponse result = categoryService.updateCategory(1L, request);

        assertNotNull(result);
        verify(categoryMapper).updateCategory(testCategory, request);
        verify(categoryRepository).save(testCategory);
    }

    @Test
    @DisplayName("4. deleteCategoryAdminSuccess: Admin xóa danh mục thành công")
    void deleteCategoryAdminSuccess() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
        when(courseRepository.existsByCategoriesId(1L)).thenReturn(false);

        assertDoesNotThrow(() -> categoryService.deleteCategory(1L));
        verify(categoryRepository).delete(testCategory);
    }
}
