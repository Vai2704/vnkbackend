package com.example.vnkapp.controller;

import com.example.vnkapp.dto.category.CategoryCreateRequestDto;
import com.example.vnkapp.dto.category.CategoryResponseDto;
import com.example.vnkapp.dto.category.CategoryUpdateRequestDto;
import com.example.vnkapp.dto.common.ApiResponseDto;
import com.example.vnkapp.dto.user.UserResponseDto;
import com.example.vnkapp.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @PostMapping
    public ResponseEntity<?> createCategory(@Valid @RequestBody CategoryCreateRequestDto request) {
        try {
            CategoryResponseDto category = categoryService.createCategory(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponseDto<>("Ok", null, category));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't create category due to some issue."));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CategoryUpdateRequestDto request) {
        try {
            CategoryResponseDto category = categoryService.updateCategory(id, request);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, category));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't update category due to some issue."));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable UUID id) {
        try {
            categoryService.deleteCategory(id);
            return ResponseEntity.ok(new UserResponseDto("Ok", null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't delete category due to some issue."));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCategory(@PathVariable UUID id) {
        try {
            CategoryResponseDto category = categoryService.getCategory(id);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, category));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch category due to some issue."));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllCategories() {
        try {
            List<CategoryResponseDto> categories = categoryService.getAllCategories();
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, categories));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch categories due to some issue."));
        }
    }

    @GetMapping("/{id}/subcategories")
    public ResponseEntity<?> getSubcategories(@PathVariable UUID id) {
        try {
            List<CategoryResponseDto> subcategories = categoryService.getSubcategories(id);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, subcategories));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch subcategories due to some issue."));
        }
    }
}
