package com.example.vnkapp.dto.address;

import com.example.vnkapp.enums.common.AddressType;

public record AddressUpdateRequestDto(
        AddressType addressType,
        String fullName,
        String phone,
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String postalCode,
        String country,
        String landmark,
        Boolean isDefault,
        Double latitude,
        Double longitude
) {}
