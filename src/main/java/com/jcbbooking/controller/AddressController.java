package com.jcbbooking.controller;

import com.jcbbooking.model.Address;
import com.jcbbooking.repository.AddressRepository;
import com.jcbbooking.security.CustomUserDetails;
import com.jcbbooking.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
@Slf4j
public class AddressController {

    private final AddressRepository addressRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Address>>> getMyAddresses(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to get addresses for user ID: {}", userDetails.getId());
        List<Address> addresses = addressRepository.findAllByUserIdAndActiveTrue(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Addresses retrieved successfully", addresses));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Address>> getAddressById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to get address ID {} for user ID {}", id, userDetails.getId());
        Address address = addressRepository.findByIdAndUserId(id, userDetails.getId()).orElse(null);
        if (address == null) {
            return ResponseEntity.status(403).body(ApiResponse.error("Address not found or unauthorized access"));
        }
        return ResponseEntity.ok(ApiResponse.success("Address retrieved successfully", address));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<Address>> saveAddress(
            @RequestBody Address address,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to save address for user ID: {}", userDetails.getId());

        boolean isNew = (address.getId() == null || address.getId() == 0);
        if (isNew) {
            address.setId(null);
            address.setUserId(userDetails.getId());
            address.setActive(true);
        } else {
            Address existing = addressRepository.findByIdAndUserId(address.getId(), userDetails.getId()).orElse(null);
            if (existing == null) {
                return ResponseEntity.status(403).body(ApiResponse.error("Address not found or unauthorized access"));
            }
            existing.setAddressType(address.getAddressType());
            existing.setLabel(address.getLabel());
            existing.setContactName(address.getContactName());
            existing.setPhone(address.getPhone());
            existing.setAddressLine1(address.getAddressLine1());
            existing.setAddressLine2(address.getAddressLine2());
            existing.setCity(address.getCity());
            existing.setState(address.getState());
            existing.setPostalCode(address.getPostalCode());
            existing.setLatitude(address.getLatitude());
            existing.setLongitude(address.getLongitude());
            existing.setLandmark(address.getLandmark());
            if (address.getIsDefault() != null) {
                existing.setIsDefault(address.getIsDefault());
            }
            address = existing;
        }

        // If setting default, unset existing default addresses for this user
        if (Boolean.TRUE.equals(address.getIsDefault())) {
            List<Address> userAddresses = addressRepository.findAllByUserId(userDetails.getId());
            for (Address a : userAddresses) {
                if (!a.getId().equals(address.getId()) && Boolean.TRUE.equals(a.getIsDefault())) {
                    a.setIsDefault(false);
                    addressRepository.save(a);
                }
            }
        }

        Address saved = addressRepository.save(address);
        return ResponseEntity.ok(ApiResponse.success(isNew ? "Address created successfully" : "Address updated successfully", saved));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to delete address ID {} for user ID {}", id, userDetails.getId());
        Address address = addressRepository.findByIdAndUserId(id, userDetails.getId()).orElse(null);
        if (address == null) {
            return ResponseEntity.status(403).body(ApiResponse.error("Address not found or unauthorized access"));
        }
        address.setActive(false);
        addressRepository.save(address);
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully"));
    }
}
