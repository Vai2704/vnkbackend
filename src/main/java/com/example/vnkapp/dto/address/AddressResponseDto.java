package com.example.vnkapp.dto.address;

import com.example.vnkapp.entity.Address;
import com.example.vnkapp.enums.common.AddressType;

import java.time.Instant;
import java.util.UUID;

public record AddressResponseDto(
        UUID id,
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
        Double longitude,
        Instant createdAt,
        Instant updatedAt
) {
    public static AddressResponseDto fromEntity(Address address) {
        return new AddressResponseDto(
                address.getId(),
                address.getAddressType(),
                address.getFullName(),
                address.getPhone(),
                address.getAddressLine1(),
                address.getAddressLine2(),
                address.getCity(),
                address.getState(),
                address.getPostalCode(),
                address.getCountry(),
                address.getLandmark(),
                address.getIsDefault(),
                address.getLatitude(),
                address.getLongitude(),
                address.getCreatedAt(),
                address.getUpdatedAt()
        );
    }
}
