package com.jcbbooking.service;

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
        return vehicleRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Vehicle> getVehiclesByDriverId(Long driverId) {
        return vehicleRepository.findByDriverIdOrderByCreatedAtDesc(driverId);
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
        List<Vehicle> userVehicles = List.of();
        if (target != null && target.getDriverId() != null) {
            userVehicles = vehicleRepository.findByDriverIdOrderByCreatedAtDesc(target.getDriverId());
        }
        if ((userVehicles == null || userVehicles.isEmpty()) && userId != null) {
            userVehicles = vehicleRepository.findByUserIdOrderByCreatedAtDesc(userId);
        }

        if (userVehicles != null) {
            for (Vehicle v : userVehicles) {
                boolean isMatch = v.getId().equals(id);
                v.setIsActive(isMatch);
                if (isMatch) {
                    v.setStatus("Active");
                }
                vehicleRepository.save(v);
            }
        }

        if (target != null) {
            target.setIsActive(true);
            target.setStatus("Active");
            target = vehicleRepository.save(target);

            Long driverId = target.getDriverId();
            if (driverId != null) {
                com.jcbbooking.model.Driver driver = driverRepository.findById(driverId).orElse(null);
                if (driver != null) {
                    driver.setSelectedVehicleType(target.getCategory());
                    driver.setSelectedMachineryModel(target.getMachineryModel() != null ? target.getMachineryModel() : target.getVehicleName());
                    driverRepository.save(driver);
                }
            }
        }
        return target;
    }
}
