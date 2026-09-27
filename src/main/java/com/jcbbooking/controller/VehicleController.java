package com.jcbbooking.controller;

import com.jcbbooking.dto.VehicleRegistrationRequest;
import com.jcbbooking.dto.VehicleResponse;
import com.jcbbooking.security.CustomUserDetails;
import com.jcbbooking.service.VehicleService;
import com.jcbbooking.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicles")
@RequiredArgsConstructor
@Slf4j
public class VehicleController {

    private final VehicleService vehicleService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<VehicleResponse>> registerVehicle(
            @Valid @RequestBody VehicleRegistrationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to register vehicle with regNumber: {}", request.getRegNumber());
        Long userId = userDetails != null ? userDetails.getId() : null;
        VehicleResponse response = vehicleService.registerVehicle(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Vehicle registered successfully and pending verification", response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponse>> createVehicle(
            @Valid @RequestBody VehicleRegistrationRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return registerVehicle(request, userDetails);
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getMyVehicles(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to get vehicles for authenticated user");
        Long userId = userDetails != null ? userDetails.getId() : null;
        List<VehicleResponse> vehicles = vehicleService.getMyVehicles(userId);
        return ResponseEntity.ok(ApiResponse.success("Vehicles retrieved successfully", vehicles));
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getVehiclesByDriverId(@PathVariable Long driverId) {
        log.info("REST request to get vehicles for driver ID: {}", driverId);
        List<VehicleResponse> vehicles = vehicleService.getVehiclesByDriverId(driverId);
        return ResponseEntity.ok(ApiResponse.success("Vehicles retrieved successfully", vehicles));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getAllVehiclesForAdmin() {
        log.info("REST request for all vehicles (Admin)");
        List<VehicleResponse> vehicles = vehicleService.getAllVehiclesForAdmin();
        return ResponseEntity.ok(ApiResponse.success("All vehicles retrieved successfully", vehicles));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> getVehicleById(@PathVariable Long id) {
        log.info("REST request to get vehicle by ID: {}", id);
        VehicleResponse vehicle = vehicleService.getVehicleById(id);
        return ResponseEntity.ok(ApiResponse.success("Vehicle retrieved successfully", vehicle));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<ApiResponse<VehicleResponse>> approveVehicle(@PathVariable Long id) {
        log.info("Admin request to approve vehicle ID: {}", id);
        VehicleResponse vehicle = vehicleService.approveVehicle(id);
        return ResponseEntity.ok(ApiResponse.success("Vehicle approved successfully", vehicle));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<VehicleResponse>> rejectVehicle(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Documents unverified") String reason) {
        log.info("Admin request to reject vehicle ID: {} with reason: {}", id, reason);
        VehicleResponse vehicle = vehicleService.rejectVehicle(id, reason);
        return ResponseEntity.ok(ApiResponse.success("Vehicle rejected successfully", vehicle));
    }
}
