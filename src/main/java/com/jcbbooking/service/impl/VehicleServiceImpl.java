package com.jcbbooking.service.impl;

import com.jcbbooking.dto.VehicleRegistrationRequest;
import com.jcbbooking.dto.VehicleResponse;
import com.jcbbooking.exception.ResourceNotFoundException;
import com.jcbbooking.model.*;
import com.jcbbooking.repository.*;
import com.jcbbooking.service.VehicleMasterService;
import com.jcbbooking.service.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import com.jcbbooking.websocket.WebSocketNotificationService;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleServiceImpl implements VehicleService {

    private final VehicleRepository vehicleRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final VehicleModelRepository vehicleModelRepository;
    private final VehicleSubModelRepository vehicleSubModelRepository;
    private final DriverRepository driverRepository;
    private final VehicleMasterService vehicleMasterService;
    private final UserNotificationRepository userNotificationRepository;
    private final WebSocketNotificationService webSocketNotificationService;

    @Override
    @Transactional
    public VehicleResponse registerVehicle(VehicleRegistrationRequest request, Long authenticatedUserId) {
        log.info("Registering vehicle for regNumber: {}, userId: {}", request.getRegNumber(), authenticatedUserId);

        // 1. Server-side hierarchy validation
        vehicleMasterService.validateHierarchy(request.getVehicleTypeId(), request.getVehicleModelId(), request.getVehicleSubModelId());

        // Resolve driver ID
        Long driverId = request.getDriverId();
        if (driverId == null && authenticatedUserId != null) {
            Driver driver = driverRepository.findByUserId(authenticatedUserId)
                    .orElseGet(() -> driverRepository.findByPhone("").orElse(null));
            if (driver != null) {
                driverId = driver.getId();
            }
        }

        Vehicle vehicle = null;
        if (request.getId() != null && request.getId() > 0) {
            vehicle = vehicleRepository.findById(request.getId()).orElse(null);
        }

        if (vehicle == null && request.getRegNumber() != null) {
            vehicle = vehicleRepository.findByRegNumberIgnoreCase(request.getRegNumber()).orElse(null);
        }

        if (vehicle == null) {
            vehicle = new Vehicle();
            vehicle.setStatus("PENDING_VERIFICATION");
            vehicle.setIsActive(false);
        }

        vehicle.setUserId(authenticatedUserId != null ? authenticatedUserId : request.getUserId());
        vehicle.setDriverId(driverId);
        vehicle.setVehicleTypeId(request.getVehicleTypeId());
        vehicle.setVehicleModelId(request.getVehicleModelId());
        vehicle.setVehicleSubModelId(request.getVehicleSubModelId());
        vehicle.setRegNumber(request.getRegNumber().trim().toUpperCase());

        // Snapshot strings fallback/population
        String catName = request.getCategory();
        String modelName = request.getMachineryModel();
        String subCatName = request.getMachinerySubCategory();
        String vName = request.getVehicleName();
        String mClass = request.getMachineClass();

        if (request.getVehicleTypeId() != null) {
            VehicleType vt = vehicleTypeRepository.findById(request.getVehicleTypeId()).orElse(null);
            if (vt != null) {
                catName = vt.getName();
                vehicle.setVehicleType(vt);
            }
        }
        if (request.getVehicleModelId() != null) {
            VehicleModel vm = vehicleModelRepository.findById(request.getVehicleModelId()).orElse(null);
            if (vm != null) {
                modelName = vm.getName();
                vehicle.setVehicleModel(vm);
            }
        }
        if (request.getVehicleSubModelId() != null) {
            VehicleSubModel vsm = vehicleSubModelRepository.findById(request.getVehicleSubModelId()).orElse(null);
            if (vsm != null) {
                subCatName = vsm.getName();
                vName = vsm.getName();
                if (vsm.getMachineClass() != null && !vsm.getMachineClass().trim().isEmpty()) {
                    mClass = vsm.getMachineClass();
                }
                vehicle.setVehicleSubModel(vsm);
            }
        }

        vehicle.setCategory(catName);
        vehicle.setMachineryModel(modelName);
        vehicle.setMachinerySubCategory(subCatName);
        vehicle.setVehicleName(vName != null ? vName : modelName);
        vehicle.setMachineClass(mClass);

        if (request.getExperience() != null) vehicle.setExperience(request.getExperience());
        if (request.getMfgYear() != null) vehicle.setMfgYear(request.getMfgYear());
        if (request.getChassisVin() != null) vehicle.setChassisVin(request.getChassisVin());
        if (request.getEngineSerial() != null) vehicle.setEngineSerial(request.getEngineSerial());
        if (request.getPowertrain() != null) vehicle.setPowertrain(request.getPowertrain());
        if (request.getFetchMode() != null) vehicle.setFetchMode(request.getFetchMode());
        if (request.getIsVahanSynced() != null) vehicle.setIsVahanSynced(request.getIsVahanSynced());
        if (request.getVehicleDescription() != null) vehicle.setVehicleDescription(request.getVehicleDescription());
        if (request.getDriverName() != null) vehicle.setDriverName(request.getDriverName());
        if (request.getDriverPhone() != null) vehicle.setDriverPhone(request.getDriverPhone());

        // Documents / Photos
        if (request.getRcFrontUrl() != null) vehicle.setRcFrontUrl(request.getRcFrontUrl());
        if (request.getRcBackUrl() != null) vehicle.setRcBackUrl(request.getRcBackUrl());
        if (request.getInsuranceUrl() != null) vehicle.setInsuranceUrl(request.getInsuranceUrl());
        if (request.getFrontPhotoUrl() != null) vehicle.setFrontPhotoUrl(request.getFrontPhotoUrl());
        if (request.getSidePhotoUrl() != null) vehicle.setSidePhotoUrl(request.getSidePhotoUrl());
        if (request.getRearPhotoUrl() != null) vehicle.setRearPhotoUrl(request.getRearPhotoUrl());
        if (request.getCabinPhotoUrl() != null) vehicle.setCabinPhotoUrl(request.getCabinPhotoUrl());
        if (request.getEnginePlatePhotoUrl() != null) vehicle.setEnginePlatePhotoUrl(request.getEnginePlatePhotoUrl());

        Vehicle saved = vehicleRepository.save(vehicle);
        log.info("Saved vehicle ID: {} with status: {}", saved.getId(), saved.getStatus());
        return mapToVehicleResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getMyVehicles(Long authenticatedUserId) {
        if (authenticatedUserId == null) return List.of();
        Driver driver = driverRepository.findByUserId(authenticatedUserId).orElse(null);
        Long driverId = driver != null ? driver.getId() : null;

        List<Vehicle> list = vehicleRepository.findByUserIdOrDriverId(authenticatedUserId, driverId);
        return mapAndDeduplicate(list);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getVehiclesByDriverId(Long driverId) {
        if (driverId == null) return List.of();
        Driver driver = driverRepository.findById(driverId).orElse(null);
        Long userId = driver != null ? driver.getUserId() : null;
        List<Vehicle> list = vehicleRepository.findByUserIdOrDriverId(userId, driverId);
        return mapAndDeduplicate(list);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getVehiclesByUserId(Long userId) {
        if (userId == null) return List.of();
        Driver driver = driverRepository.findByUserId(userId).orElse(null);
        Long driverId = driver != null ? driver.getId() : null;
        List<Vehicle> list = vehicleRepository.findByUserIdOrDriverId(userId, driverId);
        return mapAndDeduplicate(list);
    }

    private List<VehicleResponse> mapAndDeduplicate(List<Vehicle> list) {
        if (list == null || list.isEmpty()) return List.of();
        return list.stream()
                .collect(Collectors.toMap(Vehicle::getId, v -> v, (v1, v2) -> v1))
                .values().stream()
                .map(this::mapToVehicleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<VehicleResponse> getAllVehiclesForAdmin() {
        return vehicleRepository.findAll().stream()
                .map(this::mapToVehicleResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public VehicleResponse getVehicleById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + id));
        return mapToVehicleResponse(vehicle);
    }

    @Override
    @Transactional
    public VehicleResponse approveVehicle(Long id) {
        log.info("Approving vehicle ID: {}", id);
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + id));

        vehicle.setStatus("APPROVED");
        vehicle.setIsActive(true);
        vehicle.setRejectionReason(null);
        Vehicle saved = vehicleRepository.save(vehicle);

        sendVehicleStatusNotification(
                saved,
                "Vehicle Verification Approved!",
                "Your vehicle registration (" + saved.getRegNumber() + " - " + (saved.getVehicleName() != null ? saved.getVehicleName() : "Equipment") + ") has been approved by Admin!"
        );

        return mapToVehicleResponse(saved);
    }

    @Override
    @Transactional
    public VehicleResponse rejectVehicle(Long id, String reason) {
        log.info("Rejecting vehicle ID: {} with reason: {}", id, reason);
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + id));

        vehicle.setStatus("REJECTED");
        vehicle.setIsActive(false);
        vehicle.setRejectionReason(reason);
        Vehicle saved = vehicleRepository.save(vehicle);

        sendVehicleStatusNotification(
                saved,
                "Vehicle Verification Rejected",
                "Your vehicle registration (" + saved.getRegNumber() + ") was rejected. Reason: " + reason
        );

        return mapToVehicleResponse(saved);
    }

    @Override
    @Transactional
    public VehicleResponse activateVehicle(Long id, Long authenticatedUserId) {
        log.info("Activating vehicle ID: {} for user ID: {}", id, authenticatedUserId);
        Vehicle targetVehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Vehicle not found with ID: " + id));

        Long driverId = targetVehicle.getDriverId();
        Long userId = targetVehicle.getUserId() != null ? targetVehicle.getUserId() : authenticatedUserId;

        List<Vehicle> userVehicles = List.of();
        if (driverId != null) {
            userVehicles = vehicleRepository.findByDriverId(driverId);
        } else if (userId != null) {
            userVehicles = vehicleRepository.findByUserId(userId);
        }

        for (Vehicle v : userVehicles) {
            if (v.getId().equals(id)) {
                v.setIsActive(true);
                v.setStatus("Active");
            } else {
                v.setIsActive(false);
            }
        }
        if (!userVehicles.isEmpty()) {
            vehicleRepository.saveAll(userVehicles);
        }

        targetVehicle.setIsActive(true);
        targetVehicle.setStatus("Active");
        Vehicle saved = vehicleRepository.save(targetVehicle);

        if (driverId != null) {
            Driver driver = driverRepository.findById(driverId).orElse(null);
            if (driver != null) {
                driver.setSelectedVehicleType(saved.getCategory());
                driver.setSelectedMachineryModel(saved.getMachineryModel() != null ? saved.getMachineryModel() : saved.getVehicleName());
                driverRepository.save(driver);
            }
        }

        return mapToVehicleResponse(saved);
    }

    private void sendVehicleStatusNotification(Vehicle vehicle, String title, String message) {
        Long targetUserId = vehicle.getUserId();
        if (targetUserId == null && vehicle.getDriverId() != null) {
            Driver driver = driverRepository.findById(vehicle.getDriverId()).orElse(null);
            if (driver != null) {
                targetUserId = driver.getUserId();
            }
        }
        if (targetUserId == null) {
            log.warn("Cannot send vehicle notification: No target userId found for vehicle ID {}", vehicle.getId());
            return;
        }

        UserNotification notification = UserNotification.builder()
                .userId(targetUserId)
                .targetType("INDIVIDUAL_USER")
                .notificationType("VEHICLE")
                .title(title)
                .message(message)
                .deepLink("/profile")
                .isRead(false)
                .build();
        userNotificationRepository.save(notification);

        Map<String, Object> wsPayload = new HashMap<>();
        wsPayload.put("type", "INBOX_NOTIFICATION");
        wsPayload.put("title", title);
        wsPayload.put("message", message);
        wsPayload.put("notificationType", "VEHICLE");
        wsPayload.put("deepLink", "/profile");
        webSocketNotificationService.sendBookingOfferToUser(targetUserId, wsPayload);

        log.info("Successfully dispatched vehicle notification to userId {}: {}", targetUserId, title);
    }

    private VehicleResponse mapToVehicleResponse(Vehicle v) {
        String typeName = v.getVehicleType() != null ? v.getVehicleType().getName() : v.getCategory();
        String modelName = v.getVehicleModel() != null ? v.getVehicleModel().getName() : v.getMachineryModel();
        String subModelName = v.getVehicleSubModel() != null ? v.getVehicleSubModel().getName() : v.getMachinerySubCategory();

        return VehicleResponse.builder()
                .id(v.getId())
                .userId(v.getUserId())
                .driverId(v.getDriverId())
                .vehicleTypeId(v.getVehicleTypeId())
                .vehicleTypeName(typeName)
                .vehicleModelId(v.getVehicleModelId())
                .vehicleModelName(modelName)
                .vehicleSubModelId(v.getVehicleSubModelId())
                .vehicleSubModelName(subModelName)
                .regNumber(v.getRegNumber())
                .vehicleName(v.getVehicleName() != null ? v.getVehicleName() : modelName)
                .category(v.getCategory())
                .machineryModel(v.getMachineryModel())
                .machinerySubCategory(v.getMachinerySubCategory())
                .experience(v.getExperience())
                .mfgYear(v.getMfgYear())
                .machineClass(v.getMachineClass())
                .chassisVin(v.getChassisVin())
                .engineSerial(v.getEngineSerial())
                .powertrain(v.getPowertrain())
                .status(v.getStatus())
                .isActive(v.getIsActive())
                .rejectionReason(v.getRejectionReason())
                .fetchMode(v.getFetchMode())
                .isVahanSynced(v.getIsVahanSynced())
                .submittedOn(v.getSubmittedOn())
                .vehicleDescription(v.getVehicleDescription())
                .driverName(v.getDriverName())
                .driverPhone(v.getDriverPhone())
                .rcFrontUrl(v.getRcFrontUrl())
                .rcBackUrl(v.getRcBackUrl())
                .insuranceUrl(v.getInsuranceUrl())
                .frontPhotoUrl(v.getFrontPhotoUrl())
                .sidePhotoUrl(v.getSidePhotoUrl())
                .rearPhotoUrl(v.getRearPhotoUrl())
                .cabinPhotoUrl(v.getCabinPhotoUrl())
                .enginePlatePhotoUrl(v.getEnginePlatePhotoUrl())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }
}
