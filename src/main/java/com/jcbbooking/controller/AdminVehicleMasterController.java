package com.jcbbooking.controller;

import com.jcbbooking.dto.*;
import com.jcbbooking.security.CustomUserDetails;
import com.jcbbooking.service.VehicleMasterService;
import com.jcbbooking.util.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/vehicle-masters")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class AdminVehicleMasterController {

    private final VehicleMasterService vehicleMasterService;

    // --- VEHICLE TYPES ---

    @PostMapping("/types")
    public ResponseEntity<ApiResponse<VehicleTypeResponse>> createType(
            @Valid @RequestBody VehicleTypeCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Admin request to create vehicle type: {}", request.getName());
        Long userId = userDetails != null ? userDetails.getId() : null;
        VehicleTypeResponse response = vehicleMasterService.createType(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Vehicle type created successfully", response));
    }

    @PutMapping("/types/{id}")
    public ResponseEntity<ApiResponse<VehicleTypeResponse>> updateType(
            @PathVariable Long id,
            @Valid @RequestBody VehicleTypeUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Admin request to update vehicle type ID: {}", id);
        Long userId = userDetails != null ? userDetails.getId() : null;
        VehicleTypeResponse response = vehicleMasterService.updateType(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Vehicle type updated successfully", response));
    }

    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<VehicleTypeResponse>>> getAllTypes(
            @RequestParam(required = false) Boolean active) {
        log.info("Admin request to get vehicle types, activeFilter={}", active);
        List<VehicleTypeResponse> list = vehicleMasterService.getAllTypes(active);
        return ResponseEntity.ok(ApiResponse.success("Vehicle types fetched successfully", list));
    }

    @GetMapping("/types/{id}")
    public ResponseEntity<ApiResponse<VehicleTypeResponse>> getTypeById(@PathVariable Long id) {
        log.info("Admin request to get vehicle type ID: {}", id);
        VehicleTypeResponse type = vehicleMasterService.getTypeById(id);
        return ResponseEntity.ok(ApiResponse.success("Vehicle type fetched successfully", type));
    }

    @PatchMapping("/types/{id}/status")
    public ResponseEntity<ApiResponse<VehicleTypeResponse>> changeTypeStatus(
            @PathVariable Long id,
            @Valid @RequestBody VehicleMasterStatusRequest statusRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Admin request to change vehicle type ID {} active to {}", id, statusRequest.getActive());
        Long userId = userDetails != null ? userDetails.getId() : null;
        VehicleTypeResponse response = vehicleMasterService.changeTypeStatus(id, statusRequest.getActive(), userId);
        return ResponseEntity.ok(ApiResponse.success("Vehicle type status updated successfully", response));
    }

    @DeleteMapping("/types/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteType(@PathVariable Long id) {
        log.info("Admin request to delete vehicle type ID: {}", id);
        vehicleMasterService.deleteType(id);
        return ResponseEntity.ok(ApiResponse.success("Vehicle type deactivated successfully"));
    }

    // --- MODELS ---

    @PostMapping("/models")
    public ResponseEntity<ApiResponse<VehicleModelResponse>> createModel(
            @Valid @RequestBody VehicleModelCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Admin request to create vehicle model: {}", request.getName());
        Long userId = userDetails != null ? userDetails.getId() : null;
        VehicleModelResponse response = vehicleMasterService.createModel(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Vehicle model created successfully", response));
    }

    @PutMapping("/models/{id}")
    public ResponseEntity<ApiResponse<VehicleModelResponse>> updateModel(
            @PathVariable Long id,
            @Valid @RequestBody VehicleModelUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Admin request to update vehicle model ID: {}", id);
        Long userId = userDetails != null ? userDetails.getId() : null;
        VehicleModelResponse response = vehicleMasterService.updateModel(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Vehicle model updated successfully", response));
    }

    @GetMapping("/models")
    public ResponseEntity<ApiResponse<List<VehicleModelResponse>>> getAllModels(
            @RequestParam(required = false) Boolean active) {
        log.info("Admin request to get vehicle models, activeFilter={}", active);
        List<VehicleModelResponse> list = vehicleMasterService.getAllModels(active);
        return ResponseEntity.ok(ApiResponse.success("Vehicle models fetched successfully", list));
    }

    @GetMapping("/models/{id}")
    public ResponseEntity<ApiResponse<VehicleModelResponse>> getModelById(@PathVariable Long id) {
        log.info("Admin request to get vehicle model ID: {}", id);
        VehicleModelResponse model = vehicleMasterService.getModelById(id);
        return ResponseEntity.ok(ApiResponse.success("Vehicle model fetched successfully", model));
    }

    @GetMapping("/types/{typeId}/models")
    public ResponseEntity<ApiResponse<List<VehicleModelResponse>>> getModelsByType(
            @PathVariable Long typeId,
            @RequestParam(required = false) Boolean active) {
        log.info("Admin request to get vehicle models for type ID: {}", typeId);
        List<VehicleModelResponse> list = vehicleMasterService.getModelsByType(typeId, active);
        return ResponseEntity.ok(ApiResponse.success("Vehicle models fetched successfully", list));
    }

    @PatchMapping("/models/{id}/status")
    public ResponseEntity<ApiResponse<VehicleModelResponse>> changeModelStatus(
            @PathVariable Long id,
            @Valid @RequestBody VehicleMasterStatusRequest statusRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Admin request to change vehicle model ID {} active to {}", id, statusRequest.getActive());
        Long userId = userDetails != null ? userDetails.getId() : null;
        VehicleModelResponse response = vehicleMasterService.changeModelStatus(id, statusRequest.getActive(), userId);
        return ResponseEntity.ok(ApiResponse.success("Vehicle model status updated successfully", response));
    }

    @DeleteMapping("/models/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteModel(@PathVariable Long id) {
        log.info("Admin request to delete vehicle model ID: {}", id);
        vehicleMasterService.deleteModel(id);
        return ResponseEntity.ok(ApiResponse.success("Vehicle model deactivated successfully"));
    }

    // --- SUB MODELS ---

    @PostMapping("/sub-models")
    public ResponseEntity<ApiResponse<VehicleSubModelResponse>> createSubModel(
            @Valid @RequestBody VehicleSubModelCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Admin request to create vehicle sub model: {}", request.getName());
        Long userId = userDetails != null ? userDetails.getId() : null;
        VehicleSubModelResponse response = vehicleMasterService.createSubModel(request, userId);
        return ResponseEntity.ok(ApiResponse.success("Vehicle sub model created successfully", response));
    }

    @PutMapping("/sub-models/{id}")
    public ResponseEntity<ApiResponse<VehicleSubModelResponse>> updateSubModel(
            @PathVariable Long id,
            @Valid @RequestBody VehicleSubModelUpdateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Admin request to update vehicle sub model ID: {}", id);
        Long userId = userDetails != null ? userDetails.getId() : null;
        VehicleSubModelResponse response = vehicleMasterService.updateSubModel(id, request, userId);
        return ResponseEntity.ok(ApiResponse.success("Vehicle sub model updated successfully", response));
    }

    @GetMapping("/sub-models")
    public ResponseEntity<ApiResponse<List<VehicleSubModelResponse>>> getAllSubModels(
            @RequestParam(required = false) Boolean active) {
        log.info("Admin request to get vehicle sub models, activeFilter={}", active);
        List<VehicleSubModelResponse> list = vehicleMasterService.getAllSubModels(active);
        return ResponseEntity.ok(ApiResponse.success("Vehicle sub models fetched successfully", list));
    }

    @GetMapping("/sub-models/{id}")
    public ResponseEntity<ApiResponse<VehicleSubModelResponse>> getSubModelById(@PathVariable Long id) {
        log.info("Admin request to get vehicle sub model ID: {}", id);
        VehicleSubModelResponse subModel = vehicleMasterService.getSubModelById(id);
        return ResponseEntity.ok(ApiResponse.success("Vehicle sub model fetched successfully", subModel));
    }

    @GetMapping("/models/{modelId}/sub-models")
    public ResponseEntity<ApiResponse<List<VehicleSubModelResponse>>> getSubModelsByModel(
            @PathVariable Long modelId,
            @RequestParam(required = false) Boolean active) {
        log.info("Admin request to get vehicle sub models for model ID: {}", modelId);
        List<VehicleSubModelResponse> list = vehicleMasterService.getSubModelsByModel(modelId, active);
        return ResponseEntity.ok(ApiResponse.success("Vehicle sub models fetched successfully", list));
    }

    @PatchMapping("/sub-models/{id}/status")
    public ResponseEntity<ApiResponse<VehicleSubModelResponse>> changeSubModelStatus(
            @PathVariable Long id,
            @Valid @RequestBody VehicleMasterStatusRequest statusRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("Admin request to change vehicle sub model ID {} active to {}", id, statusRequest.getActive());
        Long userId = userDetails != null ? userDetails.getId() : null;
        VehicleSubModelResponse response = vehicleMasterService.changeSubModelStatus(id, statusRequest.getActive(), userId);
        return ResponseEntity.ok(ApiResponse.success("Vehicle sub model status updated successfully", response));
    }

    @DeleteMapping("/sub-models/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSubModel(@PathVariable Long id) {
        log.info("Admin request to delete vehicle sub model ID: {}", id);
        vehicleMasterService.deleteSubModel(id);
        return ResponseEntity.ok(ApiResponse.success("Vehicle sub model deactivated successfully"));
    }
}
