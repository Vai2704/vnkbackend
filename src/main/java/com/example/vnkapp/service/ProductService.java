package com.example.vnkapp.service;

import com.example.vnkapp.dto.product.ProductCreateRequestDto;
import com.example.vnkapp.dto.product.ProductResponseDto;
import com.example.vnkapp.dto.product.ProductUpdateRequestDto;
import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.entity.Product;
import com.example.vnkapp.repository.ProductCategoryRepository;
import com.example.vnkapp.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public ProductService(ProductRepository productRepository,
                          ProductCategoryRepository productCategoryRepository) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
    }

    @Transactional
    public ProductResponseDto createProduct(ProductCreateRequestDto dto) {
        // Validate that category exists
        if (dto.categoryId() == null) {
            throw new IllegalArgumentException("Please provide category of the product");
        }

        productCategoryRepository.findByIdAndStatusActive(dto.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Please provide category of the product. Category not found with ID: " + dto.categoryId()));

        // Generate slug from name
        String slug = generateSlug(dto.name());

        // Check for duplicate slug
        if (productRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis();
        }

        // Check for duplicate SKU
        if (productRepository.existsBySku(dto.sku())) {
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
        return ProductResponseDto.fromEntity(savedProduct);
    }

    @Transactional
    public ProductResponseDto updateProduct(UUID productId, ProductUpdateRequestDto dto) {
        Product product = productRepository.findByIdAndStatusActive(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Update fields if provided
        if (dto.categoryId() != null) {
            // Validate that the new category exists
            productCategoryRepository.findByIdAndStatusActive(dto.categoryId())
                    .orElseThrow(() -> new IllegalArgumentException("Please provide a valid category. Category not found with ID: " + dto.categoryId()));
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
        return ProductResponseDto.fromEntity(updatedProduct);
    }

    @Transactional
    public void deleteProduct(UUID productId) {
        Product product = productRepository.findByIdAndStatusActive(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        // Soft delete - set status to inactive
        product.setStatus(BaseEntity.STATUS_INACTIVE);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public ProductResponseDto getProduct(UUID productId) {
        Product product = productRepository.findByIdAndStatusActive(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));

        return ProductResponseDto.fromEntity(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProducts() {
        return productRepository.findAllActive()
                .stream()
                .map(ProductResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ProductResponseDto> getAllProductsPaginated(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        return productRepository.findAllActivePaginated(pageable)
                .map(ProductResponseDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public List<ProductResponseDto> getProductsByCategory(UUID categoryId) {
        return productRepository.findByCategoryIdActive(categoryId)
                .stream()
                .map(ProductResponseDto::fromEntity)
                .toList();
    }

    private String generateSlug(String input) {
        String noWhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        return slug.toLowerCase(Locale.ENGLISH);
    }
}
