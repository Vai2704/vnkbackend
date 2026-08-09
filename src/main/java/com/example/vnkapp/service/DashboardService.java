package com.example.vnkapp.service;

import com.example.vnkapp.dto.category.CategoryResponseDto;
import com.example.vnkapp.dto.dashboard.CategoryWithProductsDto;
import com.example.vnkapp.dto.dashboard.DashboardResponseDto;
import com.example.vnkapp.dto.dashboard.DashboardReviewDto;
import com.example.vnkapp.dto.faq.FaqResponseDto;
import com.example.vnkapp.dto.product.ProductSummaryDto;
import com.example.vnkapp.entity.Product;
import com.example.vnkapp.entity.ProductCategory;
import com.example.vnkapp.entity.ProductReview;
import com.example.vnkapp.entity.User;
import com.example.vnkapp.repository.FaqRepository;
import com.example.vnkapp.repository.ProductCategoryRepository;
import com.example.vnkapp.repository.ProductRepository;
import com.example.vnkapp.repository.ProductReviewRepository;
import com.example.vnkapp.repository.UserRepository;
import com.example.vnkapp.repository.WishlistRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    private static final int PRODUCTS_LIMIT = 10;
    private static final int PRODUCTS_PER_CATEGORY_LIMIT = 8;
    private static final int BEST_SELLERS_LIMIT = 10;
    private static final int REVIEWS_LIMIT = 10;

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final ProductReviewRepository productReviewRepository;
    private final FaqRepository faqRepository;
    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;

    public DashboardService(ProductRepository productRepository,
                            ProductCategoryRepository productCategoryRepository,
                            ProductReviewRepository productReviewRepository,
                            FaqRepository faqRepository,
                            WishlistRepository wishlistRepository,
                            UserRepository userRepository) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.productReviewRepository = productReviewRepository;
        this.faqRepository = faqRepository;
        this.wishlistRepository = wishlistRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponseDto getDashboard(UUID userId) {
        log.debug("Building dashboard for userId: {}", userId);

        Set<UUID> wishlisted = userId != null
                ? wishlistRepository.findWishlistedProductIds(userId)
                : Collections.emptySet();

        List<ProductSummaryDto> products = productRepository.findLatestActive(PRODUCTS_LIMIT)
                .stream()
                .map(p -> ProductSummaryDto.fromEntity(p, wishlisted.contains(p.getId())))
                .toList();

        List<CategoryWithProductsDto> categorywiseProducts = productCategoryRepository.findAllActive()
                .stream()
                .map(category -> toCategoryWithProducts(category, wishlisted))
                .filter(c -> !c.products().isEmpty())
                .toList();

        List<ProductSummaryDto> bestSellers = productRepository.findFeaturedActive(BEST_SELLERS_LIMIT)
                .stream()
                .map(p -> ProductSummaryDto.fromEntity(p, wishlisted.contains(p.getId())))
                .toList();

        List<FaqResponseDto> faqs = faqRepository.findAllActive()
                .stream()
                .map(FaqResponseDto::fromEntity)
                .toList();

        List<DashboardReviewDto> reviews = productReviewRepository.findRecentApproved(REVIEWS_LIMIT)
                .stream()
                .map(this::toDashboardReview)
                .toList();

        return new DashboardResponseDto(products, categorywiseProducts, bestSellers, faqs, reviews);
    }

    private CategoryWithProductsDto toCategoryWithProducts(ProductCategory category, Set<UUID> wishlisted) {
        List<ProductSummaryDto> products = productRepository.findTopByCategoryActive(category.getId(), PRODUCTS_PER_CATEGORY_LIMIT)
                .stream()
                .map(p -> ProductSummaryDto.fromEntity(p, wishlisted.contains(p.getId())))
                .toList();
        return new CategoryWithProductsDto(CategoryResponseDto.fromEntity(category), products);
    }

    private DashboardReviewDto toDashboardReview(ProductReview review) {
        Product product = productRepository.findByIdAndStatusActive(review.getProductId()).orElse(null);
        User user = userRepository.findById(review.getUserId()).orElse(null);

        String productName = product != null ? product.getName() : "Unknown product";
        String productThumbnail = null;
        if (product != null && product.getImageUrls() != null && !product.getImageUrls().isEmpty()) {
            productThumbnail = product.getImageUrls().get(0);
        }
        String userName = user != null ? user.getUsername() : "Anonymous";

        return new DashboardReviewDto(
                review.getId(),
                review.getProductId(),
                productName,
                productThumbnail,
                review.getUserId(),
                userName,
                review.getRating(),
                review.getTitle(),
                review.getComment(),
                review.getIsVerifiedPurchase(),
                review.getCreatedAt()
        );
    }
}
