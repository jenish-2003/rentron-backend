package com.jcbbooking.service;

import com.jcbbooking.model.Driver;
import com.jcbbooking.model.UserNotification;
import com.jcbbooking.model.Vehicle;
import com.jcbbooking.repository.UserNotificationRepository;
import com.jcbbooking.repository.VehicleRepository;
import com.jcbbooking.websocket.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final UserNotificationRepository userNotificationRepository;
    private final WebSocketNotificationService webSocketNotificationService;
    private final com.jcbbooking.repository.DriverRepository driverRepository;

    public List<Vehicle> getAllVehicles() {
        return vehicleRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Vehicle> getVehiclesByUserId(Long userId) {
        List<Vehicle> list = vehicleRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (list == null || list.isEmpty()) {
            Driver d = driverRepository.findByUserId(userId).orElse(null);
            if (d != null) {
                Vehicle v = createDefaultVehicleIfNoneExists(userId, d.getId(), d.getSelectedVehicleType(), d.getSelectedMachineryModel());
                if (v != null) return List.of(v);
            }
        }
        return list;
    }

    public List<Vehicle> getVehiclesByDriverId(Long driverId) {
        List<Vehicle> list = vehicleRepository.findByDriverIdOrderByCreatedAtDesc(driverId);
        if (list == null || list.isEmpty()) {
            Driver d = driverRepository.findById(driverId).orElse(null);
            if (d != null) {
                Vehicle v = createDefaultVehicleIfNoneExists(d.getUserId(), driverId, d.getSelectedVehicleType(), d.getSelectedMachineryModel());
                if (v != null) return List.of(v);
            }
        }
        return list;
    }

    public Optional<Vehicle> getVehicleById(Long id) {
        return vehicleRepository.findById(id);
    }

    @Transactional
    public Vehicle createVehicle(Vehicle vehicle) {
        if (vehicle.getSubmittedOn() == null || vehicle.getSubmittedOn().isEmpty()) {
            vehicle.setSubmittedOn(LocalDateTime.now().toString());
        }
        if (vehicle.getStatus() == null || vehicle.getStatus().isEmpty()) {
            vehicle.setStatus("UNDER_REVIEW");
        }

        // Deduplication check: return existing if regNumber and userId match an existing record
        if (vehicle.getRegNumber() != null && !vehicle.getRegNumber().trim().isEmpty() && vehicle.getUserId() != null) {
            List<Vehicle> existing = vehicleRepository.findByUserIdOrderByCreatedAtDesc(vehicle.getUserId());
            for (Vehicle v : existing) {
                if (v.getRegNumber() != null && vehicle.getRegNumber().trim().equalsIgnoreCase(v.getRegNumber().trim())) {
                    log.warn("Duplicate vehicle creation attempt detected for RegNumber: {} and UserId: {}. Returning existing record ID: {}", vehicle.getRegNumber(), vehicle.getUserId(), v.getId());
                    return v;
                }
            }
        }

        log.info("Creating new vehicle: {} (Reg: {}) for UserId: {}", vehicle.getVehicleName(), vehicle.getRegNumber(), vehicle.getUserId());
        return vehicleRepository.save(vehicle);
    }

    @Transactional
    public Vehicle updateVehicle(Long id, Vehicle updatedDetails) {
        Vehicle existing = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with ID: " + id));

        if (updatedDetails.getRegNumber() != null) existing.setRegNumber(updatedDetails.getRegNumber());
        if (updatedDetails.getVehicleName() != null) existing.setVehicleName(updatedDetails.getVehicleName());
        if (updatedDetails.getCategory() != null) existing.setCategory(updatedDetails.getCategory());
        if (updatedDetails.getMachineryModel() != null) existing.setMachineryModel(updatedDetails.getMachineryModel());
        if (updatedDetails.getMachinerySubCategory() != null) existing.setMachinerySubCategory(updatedDetails.getMachinerySubCategory());
        if (updatedDetails.getExperience() != null) existing.setExperience(updatedDetails.getExperience());
        if (updatedDetails.getMfgYear() != null) existing.setMfgYear(updatedDetails.getMfgYear());
        if (updatedDetails.getMachineClass() != null) existing.setMachineClass(updatedDetails.getMachineClass());
        if (updatedDetails.getChassisVin() != null) existing.setChassisVin(updatedDetails.getChassisVin());
        if (updatedDetails.getEngineSerial() != null) existing.setEngineSerial(updatedDetails.getEngineSerial());
        if (updatedDetails.getPowertrain() != null) existing.setPowertrain(updatedDetails.getPowertrain());
        if (updatedDetails.getVehicleDescription() != null) existing.setVehicleDescription(updatedDetails.getVehicleDescription());
        if (updatedDetails.getRcFrontUrl() != null) existing.setRcFrontUrl(updatedDetails.getRcFrontUrl());
        if (updatedDetails.getRcBackUrl() != null) existing.setRcBackUrl(updatedDetails.getRcBackUrl());
        if (updatedDetails.getInsuranceUrl() != null) existing.setInsuranceUrl(updatedDetails.getInsuranceUrl());
        if (updatedDetails.getFrontPhotoUrl() != null) existing.setFrontPhotoUrl(updatedDetails.getFrontPhotoUrl());
        if (updatedDetails.getSidePhotoUrl() != null) existing.setSidePhotoUrl(updatedDetails.getSidePhotoUrl());
        if (updatedDetails.getRearPhotoUrl() != null) existing.setRearPhotoUrl(updatedDetails.getRearPhotoUrl());
        if (updatedDetails.getCabinPhotoUrl() != null) existing.setCabinPhotoUrl(updatedDetails.getCabinPhotoUrl());
        if (updatedDetails.getEnginePlatePhotoUrl() != null) existing.setEnginePlatePhotoUrl(updatedDetails.getEnginePlatePhotoUrl());
        if (updatedDetails.getStatus() != null) existing.setStatus(updatedDetails.getStatus());
        if (updatedDetails.getRejectionReason() != null) {
            existing.setRejectionReason(updatedDetails.getRejectionReason().trim().isEmpty() ? null : updatedDetails.getRejectionReason());
        } else if ("UNDER_REVIEW".equalsIgnoreCase(updatedDetails.getStatus()) || "PENDING_VERIFICATION".equalsIgnoreCase(updatedDetails.getStatus())) {
            existing.setRejectionReason(null);
        }

        return vehicleRepository.save(existing);
    }

    @Transactional
    public Vehicle approveVehicle(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with ID: " + id));

        vehicle.setStatus("APPROVED");
        vehicle.setIsActive(true);
        vehicle.setRejectionReason(null);
        log.info("Vehicle ID {} ({}) APPROVED", id, vehicle.getRegNumber());
        Vehicle saved = vehicleRepository.save(vehicle);

        sendVehicleStatusNotification(saved, true, null);
        return saved;
    }

    @Transactional
    public Vehicle rejectVehicle(Long id, String reason) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with ID: " + id));

        vehicle.setStatus("REJECTED");
        vehicle.setIsActive(false);
        vehicle.setRejectionReason(reason);
        log.info("Vehicle ID {} ({}) REJECTED. Reason: {}", id, vehicle.getRegNumber(), reason);
        Vehicle saved = vehicleRepository.save(vehicle);

        sendVehicleStatusNotification(saved, false, reason);
        return saved;
    }

    private void sendVehicleStatusNotification(Vehicle vehicle, boolean isApproved, String reason) {
        if (vehicle.getUserId() == null) return;

        String title = isApproved ? "Vehicle Approved 🎉" : "Vehicle Verification Rejected ⚠️";
        String vName = vehicle.getVehicleName() != null ? vehicle.getVehicleName() : "Equipment";
        String reg = vehicle.getRegNumber() != null ? vehicle.getRegNumber() : "";
        String message = isApproved
                ? String.format("Your vehicle %s (Reg: %s) has been approved by admin and is active for job allocations!", vName, reg)
                : String.format("Your vehicle %s (Reg: %s) was rejected. Reason: %s", vName, reg, reason != null ? reason : "Document mismatch");

        try {
            UserNotification notification = UserNotification.builder()
                    .userId(vehicle.getUserId())
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

            webSocketNotificationService.sendBookingOfferToUser(vehicle.getUserId(), wsPayload);
            log.info("Dispatched vehicle status notification to UserId {}: {}", vehicle.getUserId(), title);
        } catch (Exception e) {
            log.warn("Failed to dispatch vehicle notification for UserId {}: {}", vehicle.getUserId(), e.getMessage());
        }
    }

    @Transactional
    public Vehicle activateVehicle(Long id, Long userId) {
        Vehicle target = vehicleRepository.findById(id).orElse(null);
        if (target == null) {
            throw new RuntimeException("Vehicle not found with ID: " + id);
        }

        Long driverId = target.getDriverId();
        Long effectiveUserId = (userId != null) ? userId : target.getUserId();

        List<Vehicle> userVehicles = List.of();
        if (driverId != null) {
            userVehicles = vehicleRepository.findByDriverIdOrderByCreatedAtDesc(driverId);
        }
        if ((userVehicles == null || userVehicles.isEmpty()) && effectiveUserId != null) {
            userVehicles = vehicleRepository.findByUserIdOrderByCreatedAtDesc(effectiveUserId);
        }

        if (userVehicles != null) {
            for (Vehicle v : userVehicles) {
                boolean isMatch = v.getId().equals(id);
                v.setIsActive(isMatch);
                if (isMatch) {
                    v.setStatus("Active");
                } else if ("Active".equalsIgnoreCase(v.getStatus())) {
                    v.setStatus("APPROVED");
                }
                vehicleRepository.save(v);
            }
        }

        target.setIsActive(true);
        target.setStatus("Active");
        target = vehicleRepository.save(target);

        // Update Driver's selectedVehicleType and selectedMachineryModel
        Driver driver = null;
        if (driverId != null) {
            driver = driverRepository.findById(driverId).orElse(null);
        }
        if (driver == null && effectiveUserId != null) {
            driver = driverRepository.findByUserId(effectiveUserId).orElse(null);
        }
        if (driver != null) {
            driver.setSelectedVehicleType(target.getCategory());
            driver.setSelectedMachineryModel(target.getMachineryModel() != null ? target.getMachineryModel() : target.getVehicleName());
            driverRepository.save(driver);
            log.info("Activated vehicle ID {} for Driver ID {} with selectedVehicleType: {}", id, driver.getId(), target.getCategory());
        }

        return target;
    }

    @Transactional
    public Vehicle createDefaultVehicleIfNoneExists(Long userId, Long driverId, String vehicleType, String machineryModel) {
        if (userId == null && driverId == null) return null;

        List<Vehicle> existing = vehicleRepository.findByUserIdOrDriverId(userId, driverId);
        if (existing != null && !existing.isEmpty()) {
            return existing.get(0);
        }

        String category = (vehicleType != null && !vehicleType.trim().isEmpty()) ? vehicleType.trim() : "Machinery";
        String model = (machineryModel != null && !machineryModel.trim().isEmpty()) ? machineryModel.trim() : "JCB 3DX";

        Vehicle defaultVehicle = Vehicle.builder()
                .userId(userId)
                .driverId(driverId)
                .vehicleName(model)
                .category(category)
                .machineryModel(model)
                .machinerySubCategory("")
                .regNumber("")
                .mfgYear("")
                .machineClass("")
                .chassisVin("")
                .engineSerial("")
                .vehicleDescription("")
                .status("UNDER_REVIEW")
                .isActive(true)
                .submittedOn(LocalDateTime.now().toString())
                .fetchMode("MANUAL")
                .isVahanSynced(false)
                .build();

        Vehicle saved = vehicleRepository.save(defaultVehicle);
        log.info("Created blank vehicle record ID {} ({}) for UserId: {} / DriverId: {}", saved.getId(), model, userId, driverId);
        return saved;
    }
}
