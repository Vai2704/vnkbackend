package com.example.vnkapp.controller;

import com.example.vnkapp.dto.address.AddressCreateRequestDto;
import com.example.vnkapp.dto.address.AddressResponseDto;
import com.example.vnkapp.dto.address.AddressUpdateRequestDto;
import com.example.vnkapp.dto.common.ApiResponseDto;
import com.example.vnkapp.dto.user.UserResponseDto;
import com.example.vnkapp.security.AuthenticatedUser;
import com.example.vnkapp.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/addresses")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping
    public ResponseEntity<?> addAddress(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @Valid @RequestBody AddressCreateRequestDto request) {
        try {
            AddressResponseDto address = addressService.addAddress(currentUser.getId(), request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new ApiResponseDto<>("Ok", null, address));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't add address due to some issue."));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAddress(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id,
            @Valid @RequestBody AddressUpdateRequestDto request) {
        try {
            AddressResponseDto address = addressService.updateAddress(currentUser.getId(), id, request);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, address));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't update address due to some issue."));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteAddress(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id) {
        try {
            addressService.deleteAddress(currentUser.getId(), id);
            return ResponseEntity.ok(new UserResponseDto("Ok", null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't delete address due to some issue."));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllAddresses(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        try {
            List<AddressResponseDto> addresses = addressService.getAllAddresses(currentUser.getId());
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, addresses));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch addresses due to some issue."));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAddress(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable UUID id) {
        try {
            AddressResponseDto address = addressService.getAddress(currentUser.getId(), id);
            return ResponseEntity.ok(new ApiResponseDto<>("Ok", null, address));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new UserResponseDto(null, ex.getMessage()));
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new UserResponseDto(null, "Can't fetch address due to some issue."));
        }
    }
}
