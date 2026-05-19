package com.example.vnkapp.service;

import com.example.vnkapp.dto.address.AddressCreateRequestDto;
import com.example.vnkapp.dto.address.AddressResponseDto;
import com.example.vnkapp.dto.address.AddressUpdateRequestDto;
import com.example.vnkapp.entity.Address;
import com.example.vnkapp.entity.BaseEntity;
import com.example.vnkapp.enums.common.AddressType;
import com.example.vnkapp.repository.AddressRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AddressService {

    private final AddressRepository addressRepository;

    public AddressService(AddressRepository addressRepository) {
        this.addressRepository = addressRepository;
    }

    @Transactional
    public AddressResponseDto addAddress(UUID userId, AddressCreateRequestDto dto) {
        // If this is set as default, clear other defaults
        boolean isDefault = dto.isDefault() != null && dto.isDefault();
        if (isDefault) {
            addressRepository.clearDefaultForUser(userId);
        }

        // If this is the first address, make it default
        List<Address> existingAddresses = addressRepository.findByUserIdActive(userId);
        if (existingAddresses.isEmpty()) {
            isDefault = true;
        }

        Address address = Address.builder()
                .userId(userId)
                .addressType(dto.addressType() != null ? dto.addressType() : AddressType.HOME)
                .fullName(dto.fullName())
                .phone(dto.phone())
                .addressLine1(dto.addressLine1())
                .addressLine2(dto.addressLine2())
                .city(dto.city())
                .state(dto.state())
                .postalCode(dto.postalCode())
                .country(dto.country() != null && !dto.country().isBlank() ? dto.country() : "India")
                .landmark(dto.landmark())
                .isDefault(isDefault)
                .latitude(dto.latitude())
                .longitude(dto.longitude())
                .build();

        Address savedAddress = addressRepository.save(address);
        return AddressResponseDto.fromEntity(savedAddress);
    }

    @Transactional
    public AddressResponseDto updateAddress(UUID userId, UUID addressId, AddressUpdateRequestDto dto) {
        Address address = addressRepository.findByIdAndUserIdActive(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));

        // If setting as default, clear other defaults
        if (dto.isDefault() != null && dto.isDefault() && !address.getIsDefault()) {
            addressRepository.clearDefaultForUser(userId);
            address.setIsDefault(true);
        }

        if (dto.addressType() != null) {
            address.setAddressType(dto.addressType());
        }

        if (dto.fullName() != null && !dto.fullName().isBlank()) {
            address.setFullName(dto.fullName());
        }

        if (dto.phone() != null && !dto.phone().isBlank()) {
            address.setPhone(dto.phone());
        }

        if (dto.addressLine1() != null && !dto.addressLine1().isBlank()) {
            address.setAddressLine1(dto.addressLine1());
        }

        if (dto.addressLine2() != null) {
            address.setAddressLine2(dto.addressLine2().isBlank() ? null : dto.addressLine2());
        }

        if (dto.city() != null && !dto.city().isBlank()) {
            address.setCity(dto.city());
        }

        if (dto.state() != null && !dto.state().isBlank()) {
            address.setState(dto.state());
        }

        if (dto.postalCode() != null && !dto.postalCode().isBlank()) {
            address.setPostalCode(dto.postalCode());
        }

        if (dto.country() != null && !dto.country().isBlank()) {
            address.setCountry(dto.country());
        }

        if (dto.landmark() != null) {
            address.setLandmark(dto.landmark().isBlank() ? null : dto.landmark());
        }

        if (dto.latitude() != null) {
            address.setLatitude(dto.latitude());
        }

        if (dto.longitude() != null) {
            address.setLongitude(dto.longitude());
        }

        Address updatedAddress = addressRepository.save(address);
        return AddressResponseDto.fromEntity(updatedAddress);
    }

    @Transactional
    public void deleteAddress(UUID userId, UUID addressId) {
        Address address = addressRepository.findByIdAndUserIdActive(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));

        boolean wasDefault = address.getIsDefault();

        // Soft delete
        address.setStatus(BaseEntity.STATUS_INACTIVE);
        addressRepository.save(address);

        // If deleted address was default, set another as default
        if (wasDefault) {
            List<Address> remainingAddresses = addressRepository.findByUserIdActive(userId);
            if (!remainingAddresses.isEmpty()) {
                Address newDefault = remainingAddresses.get(0);
                newDefault.setIsDefault(true);
                addressRepository.save(newDefault);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<AddressResponseDto> getAllAddresses(UUID userId) {
        return addressRepository.findByUserIdActive(userId)
                .stream()
                .map(AddressResponseDto::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public AddressResponseDto getAddress(UUID userId, UUID addressId) {
        Address address = addressRepository.findByIdAndUserIdActive(addressId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found"));

        return AddressResponseDto.fromEntity(address);
    }
}
