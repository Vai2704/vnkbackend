package com.example.vnkapp.dto.profile;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Public request to permanently delete an account. The caller proves ownership
 * with the account's registered email and current password.
 */
public record DeleteAccountRequestDto(
        @NotBlank(message = "Email is required")
        @Email(message = "Enter a valid email")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
