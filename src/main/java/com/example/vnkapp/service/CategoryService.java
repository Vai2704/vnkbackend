package com.example.vnkapp.service;

import com.example.vnkapp.dto.category.CategoryCreateRequestDto;
import com.example.vnkapp.dto.category.CategoryResponseDto;
import com.example.vnkapp.dto.category.CategoryUpdateRequestDto;
import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.ProductCategory;
import com.example.vnkapp.repository.ProductCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class CategoryService {

    private final ProductCategoryRepository categoryRepository;

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public CategoryService(ProductCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional
    public CategoryResponseDto createCategory(CategoryCreateRequestDto dto) {
        // Generate slug from name
        String slug = generateSlug(dto.name());

        // Check for duplicate slug
        if (categoryRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        // Validate parent exists if provided
        if (dto.parentId() != null) {
            categoryRepository.findByIdAndStatusActive(dto.parentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found"));
        }

        ProductCategory category = ProductCategory.builder()
                .name(dto.name())
                .slug(slug)
                .description(dto.description())
                .imageUrl(dto.imageUrl())
                .parentId(dto.parentId())
                .displayOrder(dto.displayOrder() != null ? dto.displayOrder() : 0)
                .isFeatured(dto.isFeatured() != null ? dto.isFeatured() : false)
                .build();

        ProductCategory savedCategory = categoryRepository.save(category);
        return CategoryResponseDto.fromEntity(savedCategory);
    }

    @Transactional
    public CategoryResponseDto updateCategory(UUID categoryId, CategoryUpdateRequestDto dto) {
        ProductCategory category = categoryRepository.findByIdAndStatusActive(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        if (dto.name() != null && !dto.name().isBlank()) {
            category.setName(dto.name());
            // Update slug when name changes
            String newSlug = generateSlug(dto.name());
            if (!newSlug.equals(category.getSlug())) {
                if (categoryRepository.existsBySlugAndIdNot(newSlug, categoryId)) {
                    newSlug = newSlug + "-" + System.currentTimeMillis();
                }
                category.setSlug(newSlug);
            }
        }

        if (dto.description() != null) {
            category.setDescription(dto.description());
        }

        if (dto.imageUrl() != null) {
            category.setImageUrl(dto.imageUrl());
        }

        if (dto.parentId() != null) {
            // Validate parent exists and is not self
            if (dto.parentId().equals(categoryId)) {
                throw new IllegalArgumentException("Category cannot be its own parent");
            }
            categoryRepository.findByIdAndStatusActive(dto.parentId())
                    .orElseThrow(() -> new IllegalArgumentException("Parent category not found"));
            category.setParentId(dto.parentId());
        }

        if (dto.displayOrder() != null) {
            category.setDisplayOrder(dto.displayOrder());
        }

        if (dto.isFeatured() != null) {
            category.setIsFeatured(dto.isFeatured());
        }

        ProductCategory updatedCategory = categoryRepository.save(category);
        return CategoryResponseDto.fromEntity(updatedCategory);
    }

    @Transactional
    public void deleteCategory(UUID categoryId) {
        ProductCategory category = categoryRepository.findByIdAndStatusActive(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        // Soft delete - set status to inactive
        category.setStatus(BaseEntity.STATUS_INACTIVE);
        categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponseDto getCategory(UUID categoryId) {
        ProductCategory category = categoryRepository.findByIdAndStatusActive(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found"));

        return CategoryResponseDto.fromEntity(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getAllCategories() {
        return categoryRepository.findAllActive()
                .stream()
                .map(CategoryResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponseDto> getSubcategories(UUID parentId) {
        return categoryRepository.findByParentIdActive(parentId)
                .stream()
                .map(CategoryResponseDto::fromEntity)
                .toList();
    }

    private String generateSlug(String input) {
        String noWhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
