package com.example.vnkapp.dto.dashboard;

import com.example.vnkapp.dto.category.CategoryResponseDto;
import com.example.vnkapp.dto.product.ProductSummaryDto;

import java.util.List;

public record CategoryWithProductsDto(
        CategoryResponseDto category,
        List<ProductSummaryDto> products
) {}
