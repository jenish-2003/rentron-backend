package com.jcbbooking.controller;

import com.jcbbooking.model.RentalPackage;
import com.jcbbooking.service.RentalPackageService;
import com.jcbbooking.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rental-packages")
@RequiredArgsConstructor
@Slf4j
public class RentalPackageController {

    private final RentalPackageService rentalPackageService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<RentalPackage>>> getAllPackages() {
        log.info("REST request to get all rental packages");
        return ResponseEntity.ok(ApiResponse.success("Rental packages retrieved successfully", rentalPackageService.getAllPackages()));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<RentalPackage>>> getActivePackages(
            @RequestParam(required = false) String vehicleCategory) {
        log.info("REST request to get active rental packages for category: {}", vehicleCategory);
        List<RentalPackage> active = rentalPackageService.getActivePackages(vehicleCategory);
        return ResponseEntity.ok(ApiResponse.success("Active rental packages retrieved successfully", active));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RentalPackage>> getPackageById(@PathVariable Long id) {
        log.info("REST request to get rental package by id: {}", id);
        return ResponseEntity.ok(ApiResponse.success("Rental package retrieved successfully", rentalPackageService.getPackageById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RentalPackage>> createPackage(@RequestBody RentalPackage rentalPackage) {
        log.info("REST request to create rental package: {}", rentalPackage.getName());
        RentalPackage created = rentalPackageService.createPackage(rentalPackage);
        return ResponseEntity.ok(ApiResponse.success("Rental package created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RentalPackage>> updatePackage(
            @PathVariable Long id,
            @RequestBody RentalPackage rentalPackage) {
        log.info("REST request to update rental package id: {}", id);
        RentalPackage updated = rentalPackageService.updatePackage(id, rentalPackage);
        return ResponseEntity.ok(ApiResponse.success("Rental package updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePackage(@PathVariable Long id) {
        log.info("REST request to delete rental package id: {}", id);
        rentalPackageService.deletePackage(id);
        return ResponseEntity.ok(ApiResponse.success("Rental package deleted successfully"));
    }
}
