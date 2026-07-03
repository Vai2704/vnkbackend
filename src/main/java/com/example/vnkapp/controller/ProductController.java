package com.example.vnkapp.controller;

import com.example.vnkapp.dto.common.ApiResponseDto;
import com.example.vnkapp.dto.product.ProductCreateRequestDto;
import com.example.vnkapp.dto.product.ProductDetailDto;
import com.example.vnkapp.dto.product.ProductResponseDto;
import com.example.vnkapp.dto.product.ProductSummaryDto;
import com.example.vnkapp.dto.product.ProductUpdateRequestDto;
import com.example.vnkapp.dto.user.UserResponseDto;
import com.example.vnkapp.security.AuthenticatedUser;
import com.example.vnkapp.service.ProductService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<?> createProduct(@Valid @RequestBody ProductCreateRequestDto request) {
        log.info("Create product request: {}, sku: {}", request.name(), request.sku());
        try {
            ProductResponseDto product = productService.createProduct(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponseDto<>("Ok", null, product));
        } catch (IllegalArgumentException ex) {
            log.warn("Create product failed: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Create product error for: {}", request.name(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't create product due to some issue."));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody ProductUpdateRequestDto request) {
        log.info("Update product: {}", id);
        try {
            ProductResponseDto product = productService.updateProduct(id, request);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, product));
        } catch (IllegalArgumentException ex) {
            log.warn("Update product {} failed: {}", id, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Update product {} error", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't update product due to some issue."));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable UUID id) {
        log.info("Delete product: {}", id);
        try {
            productService.deleteProduct(id);
            return ResponseEntity.ok(new UserResponseDto("Ok", null));
        } catch (IllegalArgumentException ex) {
            log.warn("Delete product {} failed: {}", id, ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Delete product {} error", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't delete product due to some issue."));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getProduct(@PathVariable UUID id) {
        log.info("Get product: {}", id);
        try {
            ProductDetailDto product = productService.getProduct(id);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, product));
        } catch (IllegalArgumentException ex) {
            log.warn("Product not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            log.error("Get product {} error", id, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch product due to some issue."));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllProducts(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        log.info("Get all products, page: {}, size: {}, sortBy: {}", page, size, sortBy);
        try {
            UUID userId = currentUser != null ? currentUser.getId() : null;
            Page<ProductSummaryDto> products = productService.getAllProductsPaginated(page, size, sortBy, sortDir, userId);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, products));
        } catch (Exception ex) {
            log.error("Get all products error", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch products due to some issue."));
        }
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getProductsByCategory(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID categoryId) {
        log.info("Get products for category: {}", categoryId);
        try {
            UUID userId = currentUser != null ? currentUser.getId() : null;
            List<ProductSummaryDto> products = productService.getProductsByCategory(categoryId, userId);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, products));
        } catch (Exception ex) {
            log.error("Get products by category {} error", categoryId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch products due to some issue."));
        }
    }
}
