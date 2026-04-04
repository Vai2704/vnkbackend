package com.example.vnkapp.dto;

public record ApiResponseDto<T>(
        String status,
        String error,
        T data
) {}
