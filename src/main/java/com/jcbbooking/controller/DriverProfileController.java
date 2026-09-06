package com.jcbbooking.controller;

import com.jcbbooking.model.Driver;
import com.jcbbooking.model.Role;
import com.jcbbooking.model.UserPreference;
import com.jcbbooking.repository.DriverRepository;
import com.jcbbooking.repository.UserPreferenceRepository;
import com.jcbbooking.repository.UserRepository;
import com.jcbbooking.security.CustomUserDetails;
import com.jcbbooking.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/drivers/me")
@RequiredArgsConstructor
@Slf4j
public class DriverProfileController {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Driver>> getMyProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request for authenticated driver profile: {}", userDetails.getId());
        Driver driver = driverRepository.findByUserId(userDetails.getId())
                .orElseGet(() -> driverRepository.findByPhone(userDetails.getUser().getPhone()).orElse(null));

        if (driver == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Driver profile not found"));
        }
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", driver));
    }

    @PutMapping
    @Transactional
    public ResponseEntity<ApiResponse<Driver>> updateMyProfile(
            @RequestBody Driver updateRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to update driver self profile: {}", userDetails.getId());

        Driver existing = driverRepository.findByUserId(userDetails.getId())
                .orElseGet(() -> driverRepository.findByPhone(userDetails.getUser().getPhone()).orElse(null));

        if (existing == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Driver profile not found"));
        }

        // Permitted self updates ONLY
        if (updateRequest.getFullName() != null) existing.setFullName(updateRequest.getFullName());
        if (updateRequest.getEmail() != null) existing.setEmail(updateRequest.getEmail());
        if (updateRequest.getLicenseNumber() != null) existing.setLicenseNumber(updateRequest.getLicenseNumber());
        if (updateRequest.getAadhaarNumber() != null) existing.setAadhaarNumber(updateRequest.getAadhaarNumber());
        if (updateRequest.getExperience() != null) existing.setExperience(updateRequest.getExperience());
        if (updateRequest.getOperationType() != null) existing.setOperationType(updateRequest.getOperationType());
        if (updateRequest.getSelectedVehicleType() != null) existing.setSelectedVehicleType(updateRequest.getSelectedVehicleType());
        if (updateRequest.getSelectedMachineryModel() != null) existing.setSelectedMachineryModel(updateRequest.getSelectedMachineryModel());
        if (updateRequest.getCity() != null) existing.setCity(updateRequest.getCity());
        if (updateRequest.getDob() != null) existing.setDob(updateRequest.getDob());
        if (updateRequest.getGender() != null) existing.setGender(updateRequest.getGender());
        if (updateRequest.getAddress() != null) existing.setAddress(updateRequest.getAddress());
        if (updateRequest.getProfilePhotoUrl() != null) existing.setProfilePhotoUrl(updateRequest.getProfilePhotoUrl());
        if (updateRequest.getPreferredLanguage() != null) existing.setPreferredLanguage(updateRequest.getPreferredLanguage());

        Driver saved = driverRepository.save(existing);

        // Also sync name/email on User entity
        userRepository.findById(userDetails.getId()).ifPresent(user -> {
            if (updateRequest.getFullName() != null) user.setFullName(updateRequest.getFullName());
            if (updateRequest.getEmail() != null) user.setEmail(updateRequest.getEmail());
            userRepository.save(user);
        });

        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", saved));
    }

    @PostMapping("/location")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> updateLocation(
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false, defaultValue = "10.0") Double gpsAccuracy,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid GPS coordinates"));
        }

        Driver driver = driverRepository.findByUserId(userDetails.getId())
                .orElseGet(() -> driverRepository.findByPhone(userDetails.getUser().getPhone()).orElse(null));

        if (driver != null) {
            driver.setLatitude(latitude);
            driver.setLongitude(longitude);
            driver.setGpsAccuracy(gpsAccuracy);
            driver.setLocationUpdatedAt(LocalDateTime.now());
            driverRepository.save(driver);
        }

        return ResponseEntity.ok(ApiResponse.success("Location updated successfully"));
    }

    @PostMapping("/online-status")
    @Transactional
    public ResponseEntity<ApiResponse<Boolean>> toggleOnlineStatus(
            @RequestParam Boolean isOnline,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to toggle online status for user ID {} to {}", userDetails.getId(), isOnline);

        Driver driver = driverRepository.findByUserId(userDetails.getId())
                .orElseGet(() -> driverRepository.findByPhone(userDetails.getUser().getPhone()).orElse(null));

        if (driver == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Driver profile not found"));
        }

        driver.setIsOnline(isOnline);
        if (Boolean.TRUE.equals(isOnline)) {
            if (!"SUSPENDED".equalsIgnoreCase(driver.getStatus())) {
                driver.setStatus("ACTIVE");
            }
        } else {
            if (!"SUSPENDED".equalsIgnoreCase(driver.getStatus())) {
                driver.setStatus("OFFLINE");
            }
        }
        driverRepository.save(driver);

        return ResponseEntity.ok(ApiResponse.success("Online status updated to " + isOnline, isOnline));
    }

    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<UserPreference>> getPreferences(@AuthenticationPrincipal CustomUserDetails userDetails) {
        UserPreference pref = userPreferenceRepository.findByUserId(userDetails.getId())
                .orElseGet(() -> {
                    UserPreference defaultPref = UserPreference.builder()
                            .userId(userDetails.getId())
                            .themeMode("SYSTEM")
                            .preferredLanguage("English")
                            .build();
                    return userPreferenceRepository.save(defaultPref);
                });

        return ResponseEntity.ok(ApiResponse.success("Preferences retrieved", pref));
    }

    @PutMapping("/preferences")
    @Transactional
    public ResponseEntity<ApiResponse<UserPreference>> updatePreferences(
            @RequestBody UserPreference prefRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        UserPreference pref = userPreferenceRepository.findByUserId(userDetails.getId())
                .orElseGet(() -> UserPreference.builder().userId(userDetails.getId()).build());

        if (prefRequest.getThemeMode() != null) pref.setThemeMode(prefRequest.getThemeMode());
        if (prefRequest.getPreferredLanguage() != null) pref.setPreferredLanguage(prefRequest.getPreferredLanguage());
        if (prefRequest.getPushEnabled() != null) pref.setPushEnabled(prefRequest.getPushEnabled());
        if (prefRequest.getBookingOffersEnabled() != null) pref.setBookingOffersEnabled(prefRequest.getBookingOffersEnabled());
        if (prefRequest.getBookingUpdatesEnabled() != null) pref.setBookingUpdatesEnabled(prefRequest.getBookingUpdatesEnabled());
        if (prefRequest.getPaymentNotificationsEnabled() != null) pref.setPaymentNotificationsEnabled(prefRequest.getPaymentNotificationsEnabled());
        if (prefRequest.getSupportNotificationsEnabled() != null) pref.setSupportNotificationsEnabled(prefRequest.getSupportNotificationsEnabled());
        if (prefRequest.getPromotionalNotificationsEnabled() != null) pref.setPromotionalNotificationsEnabled(prefRequest.getPromotionalNotificationsEnabled());

        UserPreference saved = userPreferenceRepository.save(pref);
        return ResponseEntity.ok(ApiResponse.success("Preferences updated", saved));
    }
}
