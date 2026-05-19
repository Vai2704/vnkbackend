package com.example.vnkapp.dto.address;

import com.example.vnkapp.enums.common.AddressType;
import jakarta.validation.constraints.NotBlank;

public record AddressCreateRequestDto(
        AddressType addressType,

        @NotBlank(message = "Full name is required")
        String fullName,

        @NotBlank(message = "Phone is required")
        String phone,

        @NotBlank(message = "Address line 1 is required")
        String addressLine1,

        String addressLine2,

        @NotBlank(message = "City is required")
        String city,

        @NotBlank(message = "State is required")
        String state,

        @NotBlank(message = "Postal code is required")
        String postalCode,

        String country,

        String landmark,

        Boolean isDefault,

        Double latitude,

        Double longitude
) {}
