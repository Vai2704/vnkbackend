package com.example.vnkapp.dto.admin;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequestDto(
        @NotBlank String username,
        @NotBlank String password
) {}
