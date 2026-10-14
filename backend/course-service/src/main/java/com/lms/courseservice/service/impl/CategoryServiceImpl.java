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
import com.lms.courseservice.service.CategoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CategoryServiceImpl implements CategoryService {

    CategoryRepository categoryRepository;
    CourseRepository courseRepository;
    CategoryMapper categoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(categoryMapper::toCategoryResponse)
                .toList();
    }

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new CourseException(ErrorCode.CATEGORY_NAME_EXISTED);
        }

        if (request.getSlug() != null && !request.getSlug().isBlank()
                && categoryRepository.existsBySlug(request.getSlug())) {
            throw new CourseException(ErrorCode.SLUG_ALREADY_EXISTS);
        }

        Category category = categoryMapper.toCategory(request);
        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, UpdateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CourseException(ErrorCode.CATEGORY_NOT_FOUND));

        if (!category.getName().equals(request.getName())
                && categoryRepository.existsByName(request.getName())) {
            throw new CourseException(ErrorCode.CATEGORY_NAME_EXISTED);
        }

        if (request.getSlug() != null && !request.getSlug().isBlank()
                && !request.getSlug().equals(category.getSlug())
                && categoryRepository.existsBySlug(request.getSlug())) {
            throw new CourseException(ErrorCode.SLUG_ALREADY_EXISTS);
        }

        categoryMapper.updateCategory(category, request);
        return categoryMapper.toCategoryResponse(categoryRepository.save(category));
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new CourseException(ErrorCode.CATEGORY_NOT_FOUND));

        if (category.getCourses() != null && !category.getCourses().isEmpty()) {
            throw new CourseException(ErrorCode.CANNOT_DELETE_CATEGORY);
        }

        if (courseRepository.existsByCategoriesId(id)) {
            throw new CourseException(ErrorCode.CANNOT_DELETE_CATEGORY);
        }

        categoryRepository.delete(category);
    }
}
