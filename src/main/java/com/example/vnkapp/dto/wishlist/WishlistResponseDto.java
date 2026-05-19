package com.example.vnkapp.dto.wishlist;

import java.util.List;

public record WishlistResponseDto(
        List<WishlistItemResponseDto> items,
        Integer totalItems
) {}
