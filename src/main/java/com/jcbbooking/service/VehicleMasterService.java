package com.jcbbooking.service;

import com.jcbbooking.dto.*;

import java.util.List;

public interface VehicleMasterService {

    // --- Vehicle Type ---
    VehicleTypeResponse createType(VehicleTypeCreateRequest request, Long userId);
    VehicleTypeResponse updateType(Long id, VehicleTypeUpdateRequest request, Long userId);
    VehicleTypeResponse changeTypeStatus(Long id, Boolean active, Long userId);
    List<VehicleTypeResponse> getAllTypes(Boolean activeOnly);
    VehicleTypeResponse getTypeById(Long id);
    void deleteType(Long id);

    // --- Vehicle Model ---
    VehicleModelResponse createModel(VehicleModelCreateRequest request, Long userId);
    VehicleModelResponse updateModel(Long id, VehicleModelUpdateRequest request, Long userId);
    VehicleModelResponse changeModelStatus(Long id, Boolean active, Long userId);
    List<VehicleModelResponse> getAllModels(Boolean activeOnly);
    List<VehicleModelResponse> getModelsByType(Long typeId, Boolean activeOnly);
    VehicleModelResponse getModelById(Long id);
    void deleteModel(Long id);

    // --- Vehicle Sub Model ---
    VehicleSubModelResponse createSubModel(VehicleSubModelCreateRequest request, Long userId);
    VehicleSubModelResponse updateSubModel(Long id, VehicleSubModelUpdateRequest request, Long userId);
    VehicleSubModelResponse changeSubModelStatus(Long id, Boolean active, Long userId);
    List<VehicleSubModelResponse> getAllSubModels(Boolean activeOnly);
    List<VehicleSubModelResponse> getSubModelsByModel(Long modelId, Boolean activeOnly);
    VehicleSubModelResponse getSubModelById(Long id);
    void deleteSubModel(Long id);

    // --- Catalog & Hierarchy Validation ---
    List<VehicleTypeResponse> getActiveCatalog();
    void validateHierarchy(Long typeId, Long modelId, Long subModelId);
}
