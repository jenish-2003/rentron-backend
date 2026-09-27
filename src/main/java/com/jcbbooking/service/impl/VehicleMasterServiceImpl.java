package com.jcbbooking.service.impl;

import com.jcbbooking.dto.*;
import com.jcbbooking.exception.DuplicateResourceException;
import com.jcbbooking.exception.InactiveVehicleMasterException;
import com.jcbbooking.exception.InvalidVehicleHierarchyException;
import com.jcbbooking.exception.ResourceNotFoundException;
import com.jcbbooking.model.VehicleModel;
import com.jcbbooking.model.VehicleSubModel;
import com.jcbbooking.model.VehicleType;
import com.jcbbooking.repository.VehicleModelRepository;
import com.jcbbooking.repository.VehicleRepository;
import com.jcbbooking.repository.VehicleSubModelRepository;
import com.jcbbooking.repository.VehicleTypeRepository;
import com.jcbbooking.service.VehicleMasterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleMasterServiceImpl implements VehicleMasterService {

    private final VehicleTypeRepository vehicleTypeRepository;
    private final VehicleModelRepository vehicleModelRepository;
    private final VehicleSubModelRepository vehicleSubModelRepository;

    // --- VEHICLE TYPE IMPLEMENTATION ---

    @Override
    @Transactional
    public VehicleTypeResponse createType(VehicleTypeCreateRequest request, Long userId) {
        log.info("Creating new VehicleType: {}", request.getName());

        if (vehicleTypeRepository.existsByNameIgnoreCase(request.getName())) {
            throw new DuplicateResourceException("Vehicle type name already exists: " + request.getName());
        }
        if (vehicleTypeRepository.existsByCodeIgnoreCase(request.getCode())) {
            throw new DuplicateResourceException("Vehicle type code already exists: " + request.getCode());
        }

        VehicleType vehicleType = VehicleType.builder()
                .name(request.getName().trim())
                .code(request.getCode().trim().toUpperCase())
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .active(request.getActive() != null ? request.getActive() : true)
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        VehicleType saved = vehicleTypeRepository.save(vehicleType);
        return mapToTypeResponse(saved);
    }

    @Override
    @Transactional
    public VehicleTypeResponse updateType(Long id, VehicleTypeUpdateRequest request, Long userId) {
        log.info("Updating VehicleType ID: {}", id);

        VehicleType existing = vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found with ID: " + id));

        if (vehicleTypeRepository.existsByNameIgnoreCaseAndIdNot(request.getName(), id)) {
            throw new DuplicateResourceException("Vehicle type name already exists: " + request.getName());
        }
        if (vehicleTypeRepository.existsByCodeIgnoreCaseAndIdNot(request.getCode(), id)) {
            throw new DuplicateResourceException("Vehicle type code already exists: " + request.getCode());
        }

        existing.setName(request.getName().trim());
        existing.setCode(request.getCode().trim().toUpperCase());
        existing.setDescription(request.getDescription());
        existing.setIconUrl(request.getIconUrl());
        if (request.getDisplayOrder() != null) existing.setDisplayOrder(request.getDisplayOrder());
        if (request.getActive() != null) existing.setActive(request.getActive());
        existing.setUpdatedBy(userId);

        VehicleType saved = vehicleTypeRepository.save(existing);
        return mapToTypeResponse(saved);
    }

    @Override
    @Transactional
    public VehicleTypeResponse changeTypeStatus(Long id, Boolean active, Long userId) {
        log.info("Changing VehicleType ID {} status to active={}", id, active);

        VehicleType existing = vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found with ID: " + id));

        existing.setActive(active);
        existing.setUpdatedBy(userId);
        VehicleType saved = vehicleTypeRepository.save(existing);
        return mapToTypeResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleTypeResponse> getAllTypes(Boolean activeOnly) {
        List<VehicleType> list = Boolean.TRUE.equals(activeOnly)
                ? vehicleTypeRepository.findByActiveTrueOrderByDisplayOrderAsc()
                : vehicleTypeRepository.findAllByOrderByDisplayOrderAsc();

        return list.stream().map(this::mapToTypeResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleTypeResponse getTypeById(Long id) {
        VehicleType type = vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found with ID: " + id));
        return mapToTypeResponse(type);
    }

    @Override
    @Transactional
    public void deleteType(Long id) {
        log.info("Soft deleting / deactivating VehicleType ID: {}", id);
        VehicleType type = vehicleTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found with ID: " + id));
        type.setActive(false);
        vehicleTypeRepository.save(type);
    }

    // --- VEHICLE MODEL IMPLEMENTATION ---

    @Override
    @Transactional
    public VehicleModelResponse createModel(VehicleModelCreateRequest request, Long userId) {
        log.info("Creating new VehicleModel: {} for Type ID: {}", request.getName(), request.getVehicleTypeId());

        VehicleType vehicleType = vehicleTypeRepository.findById(request.getVehicleTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found with ID: " + request.getVehicleTypeId()));

        if (vehicleModelRepository.existsByVehicleTypeIdAndNameIgnoreCase(request.getVehicleTypeId(), request.getName())) {
            throw new DuplicateResourceException("Vehicle model '" + request.getName() + "' already exists under type '" + vehicleType.getName() + "'");
        }

        VehicleModel vehicleModel = VehicleModel.builder()
                .vehicleTypeId(request.getVehicleTypeId())
                .name(request.getName().trim())
                .code(request.getCode() != null ? request.getCode().trim().toUpperCase() : null)
                .description(request.getDescription())
                .iconUrl(request.getIconUrl())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .active(request.getActive() != null ? request.getActive() : true)
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        VehicleModel saved = vehicleModelRepository.save(vehicleModel);
        return mapToModelResponse(saved, vehicleType.getName());
    }

    @Override
    @Transactional
    public VehicleModelResponse updateModel(Long id, VehicleModelUpdateRequest request, Long userId) {
        log.info("Updating VehicleModel ID: {}", id);

        VehicleModel existing = vehicleModelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle model not found with ID: " + id));

        VehicleType vehicleType = vehicleTypeRepository.findById(request.getVehicleTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found with ID: " + request.getVehicleTypeId()));

        if (vehicleModelRepository.existsByVehicleTypeIdAndNameIgnoreCaseAndIdNot(request.getVehicleTypeId(), request.getName(), id)) {
            throw new DuplicateResourceException("Vehicle model '" + request.getName() + "' already exists under type '" + vehicleType.getName() + "'");
        }

        existing.setVehicleTypeId(request.getVehicleTypeId());
        existing.setName(request.getName().trim());
        existing.setCode(request.getCode() != null ? request.getCode().trim().toUpperCase() : null);
        existing.setDescription(request.getDescription());
        existing.setIconUrl(request.getIconUrl());
        if (request.getDisplayOrder() != null) existing.setDisplayOrder(request.getDisplayOrder());
        if (request.getActive() != null) existing.setActive(request.getActive());
        existing.setUpdatedBy(userId);

        VehicleModel saved = vehicleModelRepository.save(existing);
        return mapToModelResponse(saved, vehicleType.getName());
    }

    @Override
    @Transactional
    public VehicleModelResponse changeModelStatus(Long id, Boolean active, Long userId) {
        log.info("Changing VehicleModel ID {} status to active={}", id, active);

        VehicleModel existing = vehicleModelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle model not found with ID: " + id));

        existing.setActive(active);
        existing.setUpdatedBy(userId);
        VehicleModel saved = vehicleModelRepository.save(existing);
        String typeName = existing.getVehicleType() != null ? existing.getVehicleType().getName() : null;
        return mapToModelResponse(saved, typeName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleModelResponse> getAllModels(Boolean activeOnly) {
        List<VehicleModel> list = Boolean.TRUE.equals(activeOnly)
                ? vehicleModelRepository.findByActiveTrueOrderByDisplayOrderAsc()
                : vehicleModelRepository.findAllByOrderByDisplayOrderAsc();

        return list.stream().map(m -> mapToModelResponse(m, m.getVehicleType() != null ? m.getVehicleType().getName() : null))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleModelResponse> getModelsByType(Long typeId, Boolean activeOnly) {
        VehicleType type = vehicleTypeRepository.findById(typeId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found with ID: " + typeId));

        List<VehicleModel> list = Boolean.TRUE.equals(activeOnly)
                ? vehicleModelRepository.findByVehicleTypeIdAndActiveTrueOrderByDisplayOrderAsc(typeId)
                : vehicleModelRepository.findByVehicleTypeIdOrderByDisplayOrderAsc(typeId);

        return list.stream().map(m -> mapToModelResponse(m, type.getName())).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleModelResponse getModelById(Long id) {
        VehicleModel model = vehicleModelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle model not found with ID: " + id));
        String typeName = model.getVehicleType() != null ? model.getVehicleType().getName() : null;
        return mapToModelResponse(model, typeName);
    }

    @Override
    @Transactional
    public void deleteModel(Long id) {
        log.info("Soft deleting / deactivating VehicleModel ID: {}", id);
        VehicleModel model = vehicleModelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle model not found with ID: " + id));
        model.setActive(false);
        vehicleModelRepository.save(model);
    }

    // --- VEHICLE SUB MODEL IMPLEMENTATION ---

    @Override
    @Transactional
    public VehicleSubModelResponse createSubModel(VehicleSubModelCreateRequest request, Long userId) {
        log.info("Creating new VehicleSubModel: {} for Model ID: {}", request.getName(), request.getVehicleModelId());

        VehicleModel model = vehicleModelRepository.findById(request.getVehicleModelId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle model not found with ID: " + request.getVehicleModelId()));

        if (vehicleSubModelRepository.existsByVehicleModelIdAndNameIgnoreCase(request.getVehicleModelId(), request.getName())) {
            throw new DuplicateResourceException("Vehicle sub model '" + request.getName() + "' already exists under model '" + model.getName() + "'");
        }

        VehicleSubModel subModel = VehicleSubModel.builder()
                .vehicleModelId(request.getVehicleModelId())
                .name(request.getName().trim())
                .code(request.getCode() != null ? request.getCode().trim().toUpperCase() : null)
                .description(request.getDescription())
                .manufacturer(request.getManufacturer())
                .machineClass(request.getMachineClass())
                .horsePower(request.getHorsePower())
                .capacity(request.getCapacity())
                .fuelType(request.getFuelType())
                .iconUrl(request.getIconUrl())
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .active(request.getActive() != null ? request.getActive() : true)
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        VehicleSubModel saved = vehicleSubModelRepository.save(subModel);
        String typeName = model.getVehicleType() != null ? model.getVehicleType().getName() : null;
        return mapToSubModelResponse(saved, model.getName(), model.getVehicleTypeId(), typeName);
    }

    @Override
    @Transactional
    public VehicleSubModelResponse updateSubModel(Long id, VehicleSubModelUpdateRequest request, Long userId) {
        log.info("Updating VehicleSubModel ID: {}", id);

        VehicleSubModel existing = vehicleSubModelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle sub model not found with ID: " + id));

        VehicleModel model = vehicleModelRepository.findById(request.getVehicleModelId())
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle model not found with ID: " + request.getVehicleModelId()));

        if (vehicleSubModelRepository.existsByVehicleModelIdAndNameIgnoreCaseAndIdNot(request.getVehicleModelId(), request.getName(), id)) {
            throw new DuplicateResourceException("Vehicle sub model '" + request.getName() + "' already exists under model '" + model.getName() + "'");
        }

        existing.setVehicleModelId(request.getVehicleModelId());
        existing.setName(request.getName().trim());
        existing.setCode(request.getCode() != null ? request.getCode().trim().toUpperCase() : null);
        existing.setDescription(request.getDescription());
        existing.setManufacturer(request.getManufacturer());
        existing.setMachineClass(request.getMachineClass());
        existing.setHorsePower(request.getHorsePower());
        existing.setCapacity(request.getCapacity());
        existing.setFuelType(request.getFuelType());
        existing.setIconUrl(request.getIconUrl());
        if (request.getDisplayOrder() != null) existing.setDisplayOrder(request.getDisplayOrder());
        if (request.getActive() != null) existing.setActive(request.getActive());
        existing.setUpdatedBy(userId);

        VehicleSubModel saved = vehicleSubModelRepository.save(existing);
        String typeName = model.getVehicleType() != null ? model.getVehicleType().getName() : null;
        return mapToSubModelResponse(saved, model.getName(), model.getVehicleTypeId(), typeName);
    }

    @Override
    @Transactional
    public VehicleSubModelResponse changeSubModelStatus(Long id, Boolean active, Long userId) {
        log.info("Changing VehicleSubModel ID {} status to active={}", id, active);

        VehicleSubModel existing = vehicleSubModelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle sub model not found with ID: " + id));

        existing.setActive(active);
        existing.setUpdatedBy(userId);
        VehicleSubModel saved = vehicleSubModelRepository.save(existing);

        VehicleModel model = existing.getVehicleModel() != null ? existing.getVehicleModel()
                : vehicleModelRepository.findById(existing.getVehicleModelId()).orElse(null);

        String modelName = model != null ? model.getName() : null;
        Long typeId = model != null ? model.getVehicleTypeId() : null;
        String typeName = (model != null && model.getVehicleType() != null) ? model.getVehicleType().getName() : null;

        return mapToSubModelResponse(saved, modelName, typeId, typeName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleSubModelResponse> getAllSubModels(Boolean activeOnly) {
        List<VehicleSubModel> list = Boolean.TRUE.equals(activeOnly)
                ? vehicleSubModelRepository.findByActiveTrueOrderByDisplayOrderAsc()
                : vehicleSubModelRepository.findAllByOrderByDisplayOrderAsc();

        return list.stream().map(sm -> {
            VehicleModel model = sm.getVehicleModel() != null ? sm.getVehicleModel()
                    : vehicleModelRepository.findById(sm.getVehicleModelId()).orElse(null);
            String modelName = model != null ? model.getName() : null;
            Long typeId = model != null ? model.getVehicleTypeId() : null;
            String typeName = (model != null && model.getVehicleType() != null) ? model.getVehicleType().getName() : null;
            return mapToSubModelResponse(sm, modelName, typeId, typeName);
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleSubModelResponse> getSubModelsByModel(Long modelId, Boolean activeOnly) {
        VehicleModel model = vehicleModelRepository.findById(modelId)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle model not found with ID: " + modelId));

        List<VehicleSubModel> list = Boolean.TRUE.equals(activeOnly)
                ? vehicleSubModelRepository.findByVehicleModelIdAndActiveTrueOrderByDisplayOrderAsc(modelId)
                : vehicleSubModelRepository.findByVehicleModelIdOrderByDisplayOrderAsc(modelId);

        String typeName = model.getVehicleType() != null ? model.getVehicleType().getName() : null;

        return list.stream().map(sm -> mapToSubModelResponse(sm, model.getName(), model.getVehicleTypeId(), typeName))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleSubModelResponse getSubModelById(Long id) {
        VehicleSubModel sm = vehicleSubModelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle sub model not found with ID: " + id));

        VehicleModel model = sm.getVehicleModel() != null ? sm.getVehicleModel()
                : vehicleModelRepository.findById(sm.getVehicleModelId()).orElse(null);
        String modelName = model != null ? model.getName() : null;
        Long typeId = model != null ? model.getVehicleTypeId() : null;
        String typeName = (model != null && model.getVehicleType() != null) ? model.getVehicleType().getName() : null;

        return mapToSubModelResponse(sm, modelName, typeId, typeName);
    }

    @Override
    @Transactional
    public void deleteSubModel(Long id) {
        log.info("Soft deleting / deactivating VehicleSubModel ID: {}", id);
        VehicleSubModel sm = vehicleSubModelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle sub model not found with ID: " + id));
        sm.setActive(false);
        vehicleSubModelRepository.save(sm);
    }

    // --- CATALOG & HIERARCHY VALIDATION IMPLEMENTATION ---

    @Override
    @Transactional(readOnly = true)
    public List<VehicleTypeResponse> getActiveCatalog() {
        log.info("Fetching complete active vehicle master catalog");

        List<VehicleType> activeTypes = vehicleTypeRepository.findByActiveTrueOrderByDisplayOrderAsc();
        List<VehicleTypeResponse> catalog = new ArrayList<>();

        for (VehicleType type : activeTypes) {
            VehicleTypeResponse typeResp = mapToTypeResponse(type);

            List<VehicleModel> activeModels = vehicleModelRepository.findByVehicleTypeIdAndActiveTrueOrderByDisplayOrderAsc(type.getId());
            List<VehicleModelResponse> modelResps = new ArrayList<>();

            for (VehicleModel model : activeModels) {
                VehicleModelResponse modelResp = mapToModelResponse(model, type.getName());

                List<VehicleSubModel> activeSubModels = vehicleSubModelRepository.findByVehicleModelIdAndActiveTrueOrderByDisplayOrderAsc(model.getId());
                List<VehicleSubModelResponse> subModelResps = activeSubModels.stream()
                        .map(sm -> mapToSubModelResponse(sm, model.getName(), type.getId(), type.getName()))
                        .collect(Collectors.toList());

                modelResp.setSubModels(subModelResps);
                modelResps.add(modelResp);
            }

            typeResp.setModels(modelResps);
            catalog.add(typeResp);
        }

        return catalog;
    }

    @Override
    @Transactional(readOnly = true)
    public void validateHierarchy(Long typeId, Long modelId, Long subModelId) {
        if (typeId != null) {
            VehicleType type = vehicleTypeRepository.findById(typeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle type not found with ID: " + typeId));
            if (!Boolean.TRUE.equals(type.getActive())) {
                throw new InactiveVehicleMasterException("Selected vehicle type '" + type.getName() + "' is inactive");
            }
        }

        if (modelId != null) {
            VehicleModel model = vehicleModelRepository.findById(modelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle model not found with ID: " + modelId));
            if (!Boolean.TRUE.equals(model.getActive())) {
                throw new InactiveVehicleMasterException("Selected vehicle model '" + model.getName() + "' is inactive");
            }
            if (typeId != null && !model.getVehicleTypeId().equals(typeId)) {
                throw new InvalidVehicleHierarchyException("Vehicle model ID " + modelId + " does not belong to vehicle type ID " + typeId);
            }
        }

        if (subModelId != null) {
            VehicleSubModel subModel = vehicleSubModelRepository.findById(subModelId)
                    .orElseThrow(() -> new ResourceNotFoundException("Vehicle sub model not found with ID: " + subModelId));
            if (!Boolean.TRUE.equals(subModel.getActive())) {
                throw new InactiveVehicleMasterException("Selected vehicle sub-model '" + subModel.getName() + "' is inactive");
            }
            if (modelId != null && !subModel.getVehicleModelId().equals(modelId)) {
                throw new InvalidVehicleHierarchyException("Vehicle sub-model ID " + subModelId + " does not belong to vehicle model ID " + modelId);
            }
        }
    }

    // --- PRIVATE MAPPING HELPERS ---

    private VehicleTypeResponse mapToTypeResponse(VehicleType entity) {
        return VehicleTypeResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .code(entity.getCode())
                .description(entity.getDescription())
                .iconUrl(entity.getIconUrl())
                .displayOrder(entity.getDisplayOrder())
                .active(entity.getActive())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private VehicleModelResponse mapToModelResponse(VehicleModel entity, String vehicleTypeName) {
        return VehicleModelResponse.builder()
                .id(entity.getId())
                .vehicleTypeId(entity.getVehicleTypeId())
                .vehicleTypeName(vehicleTypeName)
                .name(entity.getName())
                .code(entity.getCode())
                .description(entity.getDescription())
                .iconUrl(entity.getIconUrl())
                .displayOrder(entity.getDisplayOrder())
                .active(entity.getActive())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private VehicleSubModelResponse mapToSubModelResponse(VehicleSubModel entity, String vehicleModelName, Long vehicleTypeId, String vehicleTypeName) {
        return VehicleSubModelResponse.builder()
                .id(entity.getId())
                .vehicleModelId(entity.getVehicleModelId())
                .vehicleModelName(vehicleModelName)
                .vehicleTypeId(vehicleTypeId)
                .vehicleTypeName(vehicleTypeName)
                .name(entity.getName())
                .code(entity.getCode())
                .description(entity.getDescription())
                .manufacturer(entity.getManufacturer())
                .machineClass(entity.getMachineClass())
                .horsePower(entity.getHorsePower())
                .capacity(entity.getCapacity())
                .fuelType(entity.getFuelType())
                .iconUrl(entity.getIconUrl())
                .displayOrder(entity.getDisplayOrder())
                .active(entity.getActive())
                .createdBy(entity.getCreatedBy())
                .updatedBy(entity.getUpdatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
