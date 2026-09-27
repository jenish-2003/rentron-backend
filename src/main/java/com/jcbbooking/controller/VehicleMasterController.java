package com.jcbbooking.controller;

import com.jcbbooking.dto.VehicleModelResponse;
import com.jcbbooking.dto.VehicleSubModelResponse;
import com.jcbbooking.dto.VehicleTypeResponse;
import com.jcbbooking.service.VehicleMasterService;
import com.jcbbooking.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/vehicle-masters")
@RequiredArgsConstructor
@Slf4j
public class VehicleMasterController {

    private final VehicleMasterService vehicleMasterService;

    @GetMapping("/types")
    public ResponseEntity<ApiResponse<List<VehicleTypeResponse>>> getActiveTypes() {
        log.info("Public/Driver/Customer request to get active vehicle types");
        List<VehicleTypeResponse> list = vehicleMasterService.getAllTypes(true);
        return ResponseEntity.ok(ApiResponse.success("Active vehicle types fetched successfully", list));
    }

    @GetMapping("/types/{typeId}/models")
    public ResponseEntity<ApiResponse<List<VehicleModelResponse>>> getActiveModelsByType(@PathVariable Long typeId) {
        log.info("Public/Driver/Customer request to get active vehicle models for type ID: {}", typeId);
        List<VehicleModelResponse> list = vehicleMasterService.getModelsByType(typeId, true);
        return ResponseEntity.ok(ApiResponse.success("Active vehicle models fetched successfully", list));
    }

    @GetMapping("/models/{modelId}/sub-models")
    public ResponseEntity<ApiResponse<List<VehicleSubModelResponse>>> getActiveSubModelsByModel(@PathVariable Long modelId) {
        log.info("Public/Driver/Customer request to get active vehicle sub-models for model ID: {}", modelId);
        List<VehicleSubModelResponse> list = vehicleMasterService.getSubModelsByModel(modelId, true);
        return ResponseEntity.ok(ApiResponse.success("Active vehicle sub-models fetched successfully", list));
    }

    @GetMapping("/catalog")
    public ResponseEntity<ApiResponse<List<VehicleTypeResponse>>> getActiveCatalog() {
        log.info("Public/Driver/Customer request to get complete vehicle catalog");
        List<VehicleTypeResponse> catalog = vehicleMasterService.getActiveCatalog();
        return ResponseEntity.ok(ApiResponse.success("Vehicle catalog fetched successfully", catalog));
    }
}
