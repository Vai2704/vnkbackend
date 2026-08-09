package com.example.vnkapp.dto.dashboard;

import com.example.vnkapp.dto.faq.FaqResponseDto;
import com.example.vnkapp.dto.product.ProductSummaryDto;

import java.util.List;

public record DashboardResponseDto(
        List<ProductSummaryDto> products,
        List<CategoryWithProductsDto> categorywiseProducts,
        List<ProductSummaryDto> bestSellers,
        List<FaqResponseDto> faqs,
        List<DashboardReviewDto> reviews
) {}
