package com.example.vnkapp.service;

import com.example.vnkapp.dto.product.ProductCreateRequestDto;
import com.example.vnkapp.dto.product.ProductDetailDto;
import com.example.vnkapp.dto.product.ProductResponseDto;
import com.example.vnkapp.dto.product.ProductSummaryDto;
import com.example.vnkapp.dto.product.ProductUpdateRequestDto;
import com.example.vnkapp.dto.review.ReviewResponseDto;
import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.Product;
import com.example.vnkapp.repository.CartItemRepository;
import com.example.vnkapp.repository.CartRepository;
import com.example.vnkapp.repository.ProductCategoryRepository;
import com.example.vnkapp.repository.ProductRepository;
import com.example.vnkapp.repository.ProductReviewRepository;
import com.example.vnkapp.repository.WishlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final WishlistRepository wishlistRepository;
    private final ProductReviewRepository productReviewRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public ProductService(ProductRepository productRepository,
                          ProductCategoryRepository productCategoryRepository,
                          WishlistRepository wishlistRepository,
                          ProductReviewRepository productReviewRepository,
                          CartRepository cartRepository,
                          CartItemRepository cartItemRepository) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.wishlistRepository = wishlistRepository;
        this.productReviewRepository = productReviewRepository;
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
    }

    @Transactional
    public ProductResponseDto createProduct(ProductCreateRequestDto dto) {
        log.debug("Creating product: {}, sku: {}", dto.name(), dto.sku());
        // Validate that category exists
        if (dto.categoryId() == null) {
            log.warn("Create product failed - no categoryId provided");
            throw new IllegalArgumentException("Please provide category of the product");
        }

        productCategoryRepository.findByIdAndStatusActive(dto.categoryId())
                .orElseThrow(() -> {
                    log.warn("Category not found: {}", dto.categoryId());
                    return new IllegalArgumentException("Please provide category of the product. Category not found with ID: " + dto.categoryId());
                });

        // Generate slug from name
        String slug = generateSlug(dto.name());

        // Check for duplicate slug
        if (productRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        // Check for duplicate SKU
        if (productRepository.existsBySku(dto.sku())) {
            log.warn("Duplicate SKU: {}", dto.sku());
            throw new IllegalArgumentException("SKU already exists: " + dto.sku());
        }

        Product product = Product.builder()
                .categoryId(dto.categoryId())
                .name(dto.name())
                .slug(slug)
                .sku(dto.sku())
                .description(dto.description())
                .shortDescription(dto.shortDescription())
                .price(dto.price())
                .compareAtPrice(dto.compareAtPrice())
                .costPrice(dto.costPrice())
                .stockQuantity(dto.stockQuantity() != null ? dto.stockQuantity() : 0)
                .lowStockThreshold(dto.lowStockThreshold() != null ? dto.lowStockThreshold() : 10)
                .weightGrams(dto.weightGrams())
                .brand(dto.brand())
                .ingredients(dto.ingredients())
                .howToUse(dto.howToUse())
                .isFeatured(dto.isFeatured() != null ? dto.isFeatured() : false)
                .metaTitle(dto.metaTitle())
                .metaDescription(dto.metaDescription())
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created: {}, sku: {}", savedProduct.getId(), dto.sku());
        return ProductResponseDto.fromEntity(savedProduct);
    }

    @Transactional
    public ProductResponseDto updateProduct(UUID productId, ProductUpdateRequestDto dto) {
        log.debug("Updating product: {}", productId);
        Product product = productRepository.findByIdAndStatusActive(productId)
                .orElseThrow(() -> {
                    log.warn("Product not found: {}", productId);
                    return new IllegalArgumentException("Product not found");
                });

        // Update fields if provided
        if (dto.categoryId() != null) {
            // Validate that the new category exists
            productCategoryRepository.findByIdAndStatusActive(dto.categoryId())
                    .orElseThrow(() -> {
                        log.warn("Category not found: {}", dto.categoryId());
                        return new IllegalArgumentException("Please provide a valid category. Category not found with ID: " + dto.categoryId());
                    });
            product.setCategoryId(dto.categoryId());
        }

        if (dto.name() != null && !dto.name().isBlank()) {
            product.setName(dto.name());
            // Update slug when name changes
            String newSlug = generateSlug(dto.name());
            if (!newSlug.equals(product.getSlug())) {
                if (productRepository.existsBySlugAndIdNot(newSlug, productId)) {
                    newSlug = newSlug + "-" + System.currentTimeMillis();
                }
                product.setSlug(newSlug);
            }
        }

        if (dto.sku() != null && !dto.sku().isBlank()) {
            if (!dto.sku().equals(product.getSku()) && productRepository.existsBySkuAndIdNot(dto.sku(), productId)) {
                log.warn("Duplicate SKU {} for product: {}", dto.sku(), productId);
                throw new IllegalArgumentException("SKU already exists: " + dto.sku());
            }
            product.setSku(dto.sku());
        }

        if (dto.description() != null) {
            product.setDescription(dto.description());
        }

        if (dto.shortDescription() != null) {
            product.setShortDescription(dto.shortDescription());
        }

        if (dto.price() != null) {
            product.setPrice(dto.price());
        }

        if (dto.compareAtPrice() != null) {
            product.setCompareAtPrice(dto.compareAtPrice());
        }

        if (dto.costPrice() != null) {
            product.setCostPrice(dto.costPrice());
        }

        if (dto.stockQuantity() != null) {
            product.setStockQuantity(dto.stockQuantity());
        }

        if (dto.lowStockThreshold() != null) {
            product.setLowStockThreshold(dto.lowStockThreshold());
        }

        if (dto.weightGrams() != null) {
            product.setWeightGrams(dto.weightGrams());
        }

        if (dto.brand() != null) {
            product.setBrand(dto.brand());
        }

        if (dto.ingredients() != null) {
            product.setIngredients(dto.ingredients());
        }

        if (dto.howToUse() != null) {
            product.setHowToUse(dto.howToUse());
        }

        if (dto.isFeatured() != null) {
            product.setIsFeatured(dto.isFeatured());
        }

        if (dto.metaTitle() != null) {
            product.setMetaTitle(dto.metaTitle());
        }

        if (dto.metaDescription() != null) {
            product.setMetaDescription(dto.metaDescription());
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated: {}", productId);
        return ProductResponseDto.fromEntity(updatedProduct);
    }

    @Transactional
    public void deleteProduct(UUID productId) {
        log.debug("Deleting product: {}", productId);
        Product product = productRepository.findByIdAndStatusActive(productId)
                .orElseThrow(() -> {
                    log.warn("Product not found: {}", productId);
                    return new IllegalArgumentException("Product not found");
                });

        // Soft delete - set status to inactive
        product.setStatus(BaseEntity.STATUS_INACTIVE);
        productRepository.save(product);
        log.info("Product deleted: {}", productId);
    }

    @Transactional(readOnly = true)
    public ProductDetailDto getProduct(UUID productId, UUID userId) {
        log.debug("Fetching product: {}, userId: {}", productId, userId);
        Product product = productRepository.findByIdAndStatusActive(productId)
                .orElseThrow(() -> {
                    log.warn("Product not found: {}", productId);
                    return new IllegalArgumentException("Product not found");
                });

        List<ReviewResponseDto> reviews = productReviewRepository.findApprovedByProductId(productId)
                .stream()
                .map(ReviewResponseDto::fromEntity)
                .toList();

        boolean isWishlisted = false;
        boolean isInCart = false;

        if (userId != null) {
            isWishlisted = wishlistRepository.existsByUserIdAndProductIdActive(userId, productId);
            isInCart = cartRepository.findByUserIdActive(userId)
                    .map(cart -> cartItemRepository.findByCartIdAndProductIdActive(cart.getId(), productId).isPresent())
                    .orElse(false);
        }

        return ProductDetailDto.fromEntity(product, isWishlisted, isInCart, reviews);
    }

    @Transactional(readOnly = true)
    public Page<ProductSummaryDto> getAllProductsPaginated(int page, int size, String sortBy, String sortDir, UUID userId) {
        log.debug("Fetching products page: {}, size: {}, sortBy: {}, userId: {}", page, size, sortBy, userId);
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Set<UUID> wishlisted = userId != null
                ? wishlistRepository.findWishlistedProductIds(userId)
                : Collections.emptySet();

        return productRepository.findAllActivePaginated(pageable)
                .map(p -> ProductSummaryDto.fromEntity(p, wishlisted.contains(p.getId())));
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryDto> getProductsByCategory(UUID categoryId, UUID userId) {
        log.debug("Fetching products for category: {}, userId: {}", categoryId, userId);

        Set<UUID> wishlisted = userId != null
                ? wishlistRepository.findWishlistedProductIds(userId)
                : Collections.emptySet();

        return productRepository.findByCategoryIdActive(categoryId)
                .stream()
                .map(p -> ProductSummaryDto.fromEntity(p, wishlisted.contains(p.getId())))
                .toList();
    }

    private String generateSlug(String input) {
        String noWhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
