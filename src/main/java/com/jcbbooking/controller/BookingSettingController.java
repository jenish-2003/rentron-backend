package com.jcbbooking.controller;

import com.jcbbooking.model.BookingAssignment;
import com.jcbbooking.model.BookingSetting;
import com.jcbbooking.repository.BookingAssignmentRepository;
import com.jcbbooking.repository.BookingSettingRepository;
import com.jcbbooking.service.BookingAssignmentService;
import com.jcbbooking.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/settings/booking")
@RequiredArgsConstructor
@Slf4j
public class BookingSettingController {

    private final BookingAssignmentService bookingAssignmentService;
    private final BookingSettingRepository bookingSettingRepository;
    private final BookingAssignmentRepository bookingAssignmentRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<BookingSetting>> getSettings() {
        log.info("REST request to get booking settings");
        BookingSetting settings = bookingAssignmentService.getOrInitSettings();
        
        // Clone for response & sanitize secrets
        BookingSetting safeCopy = BookingSetting.builder()
                .id(settings.getId())
                .settingKey(settings.getSettingKey())
                .keyId(settings.getKeyId())
                .keySecret(settings.getKeySecret() != null ? "******" : null)
                .currency(settings.getCurrency())
                .assignmentType(settings.getAssignmentType())
                .initialRadiusKm(settings.getInitialRadiusKm())
                .radiusIncrementKm(settings.getRadiusIncrementKm())
                .maxRadiusKm(settings.getMaxRadiusKm())
                .assignmentTimeoutSeconds(settings.getAssignmentTimeoutSeconds())
                .maxLocationAgeSeconds(settings.getMaxLocationAgeSeconds())
                .notifyAdminWhenUnassigned(settings.getNotifyAdminWhenUnassigned())
                .mapKey(settings.getMapKey())
                .createdAt(settings.getCreatedAt())
                .updatedAt(settings.getUpdatedAt())
                .build();

        return ResponseEntity.ok(ApiResponse.success("Booking settings retrieved successfully", safeCopy));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BookingSetting>> updateSettings(@RequestBody BookingSetting request) {
        log.info("REST request to update booking settings");
        BookingSetting existing = bookingAssignmentService.getOrInitSettings();

        if (request.getKeyId() != null) existing.setKeyId(request.getKeyId());
        
        // Only update secrets if non-empty and not masked
        if (request.getKeySecret() != null && !request.getKeySecret().isEmpty() && !"******".equals(request.getKeySecret())) {
            existing.setKeySecret(request.getKeySecret());
        }

        if (request.getCurrency() != null) existing.setCurrency(request.getCurrency());
        if (request.getAssignmentType() != null) existing.setAssignmentType(request.getAssignmentType());
        if (request.getInitialRadiusKm() != null) existing.setInitialRadiusKm(request.getInitialRadiusKm());
        if (request.getRadiusIncrementKm() != null) existing.setRadiusIncrementKm(request.getRadiusIncrementKm());
        if (request.getMaxRadiusKm() != null) existing.setMaxRadiusKm(request.getMaxRadiusKm());
        if (request.getAssignmentTimeoutSeconds() != null) existing.setAssignmentTimeoutSeconds(request.getAssignmentTimeoutSeconds());
        if (request.getMaxLocationAgeSeconds() != null) existing.setMaxLocationAgeSeconds(request.getMaxLocationAgeSeconds());
        if (request.getNotifyAdminWhenUnassigned() != null) existing.setNotifyAdminWhenUnassigned(request.getNotifyAdminWhenUnassigned());
        if (request.getMapKey() != null) existing.setMapKey(request.getMapKey());

        BookingSetting saved = bookingSettingRepository.save(existing);
        return ResponseEntity.ok(ApiResponse.success("Booking settings updated successfully", saved));
    }

    @GetMapping("/assignments/{bookingId}")
    public ResponseEntity<ApiResponse<List<BookingAssignment>>> getAssignmentAuditHistory(@PathVariable Long bookingId) {
        log.info("REST request to get assignment audit history for booking ID {}", bookingId);
        List<BookingAssignment> history = bookingAssignmentRepository.findAllByBookingIdOrderByOfferedAtDesc(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Assignment audit history retrieved", history));
    }
}
