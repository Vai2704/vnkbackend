package com.example.vnkapp.dto.admin;

import com.example.vnkapp.enums.admin.AdminRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCreateRequestDto(
        @NotBlank String username,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,
        String fullName
) {}
