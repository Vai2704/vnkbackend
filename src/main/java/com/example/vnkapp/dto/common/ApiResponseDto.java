package com.example.vnkapp.dto.common;

public record ApiResponseDto<T>(
        String status,
        String error,
        T data
) {}
