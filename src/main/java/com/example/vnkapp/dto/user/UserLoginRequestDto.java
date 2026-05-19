package com.example.vnkapp.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserLoginRequestDto(
        @NotBlank @Email String email,
        @NotBlank String password
) {}
