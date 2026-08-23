package com.jcbbooking.controller;

import java.io.File;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jcbbooking.model.Driver;
import com.jcbbooking.model.Role;
import com.jcbbooking.model.User;
import com.jcbbooking.repository.ContractorRepository;
import com.jcbbooking.repository.DocumentRepository;
import com.jcbbooking.repository.DriverRepository;
import com.jcbbooking.repository.UserRepository;
import com.jcbbooking.security.CustomUserDetails;
import com.jcbbooking.util.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/drivers")
@RequiredArgsConstructor
@Slf4j
public class DriverController {

    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ContractorRepository contractorRepository;
    private final DocumentRepository documentRepository;
    private final com.jcbbooking.repository.PartnerApprovalRepository partnerApprovalRepository;

    @Value("${core.fileTransfer.primaryUploadFolder:/opt/microservice/upload/images}")
    private String primaryUploadFolder;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Driver>>> getAllDrivers(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to get all drivers");
        if (userDetails != null) {
            Role role = userDetails.getUser().getRole();
            if (role == Role.CONTRACTOR) {
                Optional<com.jcbbooking.model.Contractor> conOpt = contractorRepository.findByUserId(userDetails.getId());
                if (conOpt.isEmpty()) {
                    conOpt = contractorRepository.findByPhone(userDetails.getUser().getPhone());
                }
                if (conOpt.isPresent()) {
                    List<Driver> contractorDrivers = driverRepository.findAllByContractorId(conOpt.get().getId());
                    return ResponseEntity.ok(ApiResponse.success("Drivers retrieved successfully", contractorDrivers));
                }
                return ResponseEntity.ok(ApiResponse.success("No assigned drivers found", List.of()));
            } else if (role == Role.DRIVER) {
                Optional<Driver> selfOpt = driverRepository.findByUserId(userDetails.getId());
                if (selfOpt.isEmpty()) {
                    selfOpt = driverRepository.findByPhone(userDetails.getUser().getPhone());
                }
                return selfOpt.map(driver -> ResponseEntity.ok(ApiResponse.success("Driver retrieved successfully", List.of(driver))))
                        .orElseGet(() -> ResponseEntity.ok(ApiResponse.success("Driver not found", List.of())));
            }
        }
        return ResponseEntity.ok(ApiResponse.success("Drivers retrieved successfully", driverRepository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Driver>> getDriverById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to get driver by id: {}", id);
        Driver driver = driverRepository.findById(id).orElse(null);
        if (driver == null) {
            return ResponseEntity.notFound().build();
        }

        if (userDetails != null) {
            Role role = userDetails.getUser().getRole();
            if (role == Role.CONTRACTOR) {
                Optional<com.jcbbooking.model.Contractor> conOpt = contractorRepository.findByUserId(userDetails.getId());
                if (conOpt.isEmpty()) {
                    conOpt = contractorRepository.findByPhone(userDetails.getUser().getPhone());
                }
                if (conOpt.isEmpty() || !conOpt.get().getId().equals(driver.getContractorId())) {
                    return ResponseEntity.status(403).body(ApiResponse.error("Unauthorized access to driver details"));
                }
            } else if (role == Role.DRIVER) {
                Optional<Driver> selfOpt = driverRepository.findByUserId(userDetails.getId());
                if (selfOpt.isEmpty()) {
                    selfOpt = driverRepository.findByPhone(userDetails.getUser().getPhone());
                }
                if (selfOpt.isEmpty() || !selfOpt.get().getId().equals(driver.getId())) {
                    return ResponseEntity.status(403).body(ApiResponse.error("Unauthorized access to driver details"));
                }
            }
        }

        return ResponseEntity.ok(ApiResponse.success("Driver retrieved successfully", driver));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<Driver>> saveDriver(
            @RequestBody Driver driver,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to save/update driver: {}", driver);
        boolean isNew = (driver.getId() == null || driver.getId() == 0);

        if (isNew) {
            driver.setId(null);
            if (driverRepository.existsByPhone(driver.getPhone())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Driver with phone already exists"));
            }
            if (driver.getStatus() == null) {
                driver.setStatus("PENDING_VERIFICATION");
            }
            if (driver.getRating() == null) {
                driver.setRating(0.0);
            }
            if (driver.getTotalJobs() == null) {
                driver.setTotalJobs(0);
            }
            if (driver.getTotalEarnings() == null) {
                driver.setTotalEarnings(0.0);
            }
            
            // Auto-assign contractorId if created by a Contractor user
            if (userDetails != null && userDetails.getUser().getRole() == Role.CONTRACTOR) {
                java.util.Optional<com.jcbbooking.model.Contractor> conOpt = contractorRepository.findByUserId(userDetails.getId());
                if (conOpt.isPresent()) {
                    driver.setContractorId(conOpt.get().getId());
                }
            }
        } else {
            Driver existing = driverRepository.findById(driver.getId()).orElse(null);
            if (existing == null) {
                return ResponseEntity.notFound().build();
            }

            // Sync User creation if status is set to ACTIVE
            if ("ACTIVE".equals(driver.getStatus()) && !"ACTIVE".equals(existing.getStatus())) {
                syncUserAccount(existing.getPhone(), existing.getFullName(), existing.getEmail(), Role.DRIVER, existing.getContractorId(), existing.getId(), true);
            }

            // Sync User deactivation if status is set to SUSPENDED
            if ("SUSPENDED".equals(driver.getStatus()) && !"SUSPENDED".equals(existing.getStatus())) {
                syncUserAccount(existing.getPhone(), existing.getFullName(), existing.getEmail(), Role.DRIVER, existing.getContractorId(), existing.getId(), false);
            }

            // Update fields
            existing.setFullName(driver.getFullName());
            existing.setPhone(driver.getPhone());
            existing.setEmail(driver.getEmail());
            existing.setLicenseNumber(driver.getLicenseNumber());
            existing.setAadhaarNumber(driver.getAadhaarNumber());
            existing.setExperience(driver.getExperience());
            if (driver.getContractorId() != null) {
                existing.setContractorId(driver.getContractorId());
            }
            if (driver.getStatus() != null) {
                existing.setStatus(driver.getStatus());
                syncPartnerApprovalStatus(existing.getPhone(), driver.getStatus());
            }
            driver = existing;
        }

        Driver saved = driverRepository.save(driver);
        return ResponseEntity.ok(ApiResponse.success(isNew ? "Driver application submitted" : "Driver updated successfully", saved));
    }

    private User syncUserAccount(String phone, String fullName, String email, Role role, Long contractorId, Long driverId, boolean active) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }
        String customPass = partnerApprovalRepository.findByPhone(phone)
                .map(com.jcbbooking.model.PartnerApproval::getPassword)
                .filter(p -> p != null && !p.trim().isEmpty())
                .orElse("Password123");

        User user = userRepository.findByPhone(phone).orElse(null);
        if (user == null) {
            user = User.builder()
                    .fullName(fullName != null ? fullName : "Driver Partner")
                    .phone(phone)
                    .email(email)
                    .role(role)
                    .contractorId(contractorId)
                    .driverId(driverId)
                    .active(active)
                    .verified(true)
                    .passwordHash(passwordEncoder.encode(customPass))
                    .build();
            user = userRepository.save(user);
            log.info("Created missing User entry ID {} for driver phone {}, active={}", user.getId(), phone, active);
        } else {
            user.setActive(active);
            user.setVerified(true);
            if (!"Password123".equals(customPass)) {
                user.setPasswordHash(passwordEncoder.encode(customPass));
            }
            if (role != null) user.setRole(role);
            if (contractorId != null) user.setContractorId(contractorId);
            if (driverId != null) user.setDriverId(driverId);
            if (fullName != null && !fullName.trim().isEmpty()) user.setFullName(fullName);
            if (email != null && !email.trim().isEmpty()) user.setEmail(email);
            user = userRepository.save(user);
            log.info("Updated existing User entry ID {} for driver phone {}, active={}", user.getId(), phone, active);
        }
        return user;
    }

    private void syncPartnerApprovalStatus(String phone, String status) {
        if (phone != null && !phone.trim().isEmpty()) {
            partnerApprovalRepository.findByPhone(phone).ifPresent(pa -> {
                pa.setStatus(status);
                partnerApprovalRepository.save(pa);
                log.info("Synced PartnerApproval status to {} for driver phone {}", status, phone);
            });
        }
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<ApiResponse<Driver>> approveDriver(@PathVariable Long id) {
        log.info("REST request to approve driver ID: {}", id);
        Driver driver = driverRepository.findById(id).orElse(null);
        if (driver == null) {
            return ResponseEntity.notFound().build();
        }

        User user = syncUserAccount(driver.getPhone(), driver.getFullName(), driver.getEmail(), Role.DRIVER, driver.getContractorId(), driver.getId(), true);
        if (user != null) {
            driver.setUserId(user.getId());
        }
        driver.setStatus("ACTIVE");
        syncPartnerApprovalStatus(driver.getPhone(), "ACTIVE");
        Driver saved = driverRepository.save(driver);

        return ResponseEntity.ok(ApiResponse.success("Driver approved successfully and user account activated", saved));
    }

    @PostMapping("/{id}/reject")
    @Transactional
    public ResponseEntity<ApiResponse<Driver>> rejectDriver(@PathVariable Long id) {
        log.info("REST request to reject driver ID: {}", id);
        Driver driver = driverRepository.findById(id).orElse(null);
        if (driver == null) {
            return ResponseEntity.notFound().build();
        }

        driver.setStatus("REJECTED");
        syncPartnerApprovalStatus(driver.getPhone(), "REJECTED");
        Driver saved = driverRepository.save(driver);
        return ResponseEntity.ok(ApiResponse.success("Driver application rejected", saved));
    }

    @PostMapping("/{id}/suspend")
    @Transactional
    public ResponseEntity<ApiResponse<Driver>> suspendDriver(@PathVariable Long id) {
        log.info("REST request to suspend driver ID: {}", id);
        Driver driver = driverRepository.findById(id).orElse(null);
        if (driver == null) {
            return ResponseEntity.notFound().build();
        }

        driver.setStatus("SUSPENDED");
        syncPartnerApprovalStatus(driver.getPhone(), "SUSPENDED");
        User user = syncUserAccount(driver.getPhone(), driver.getFullName(), driver.getEmail(), Role.DRIVER, driver.getContractorId(), driver.getId(), false);
        if (user != null) {
            driver.setUserId(user.getId());
        }
        Driver saved = driverRepository.save(driver);
        return ResponseEntity.ok(ApiResponse.success("Driver suspended successfully and user account deactivated", saved));
    }

    @PostMapping("/{id}/unsuspend")
    @Transactional
    public ResponseEntity<ApiResponse<Driver>> unsuspendDriver(@PathVariable Long id) {
        log.info("REST request to unsuspend driver ID: {}", id);
        Driver driver = driverRepository.findById(id).orElse(null);
        if (driver == null) {
            return ResponseEntity.notFound().build();
        }

        driver.setStatus("ACTIVE");
        syncPartnerApprovalStatus(driver.getPhone(), "ACTIVE");
        User user = syncUserAccount(driver.getPhone(), driver.getFullName(), driver.getEmail(), Role.DRIVER, driver.getContractorId(), driver.getId(), true);
        if (user != null) {
            driver.setUserId(user.getId());
        }
        Driver saved = driverRepository.save(driver);
        return ResponseEntity.ok(ApiResponse.success("Driver activated successfully and user account activated", saved));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteDriver(@PathVariable Long id) {
        log.info("REST request to delete driver ID: {}", id);
        if (!driverRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        // Cascade-delete all associated documents (files + DB records)
        documentRepository.findAllByEntityTypeAndEntityId("DRIVER", id).forEach(doc -> {
            try {
                File file = new File(doc.getFilePath());
                if (file.exists()) {
                    file.delete();
                    log.info("Deleted document file: {}", doc.getFilePath());
                }
            } catch (Exception ex) {
                log.warn("Could not delete document file: {}", doc.getFilePath(), ex);
            }
            documentRepository.delete(doc);
        });
        driverRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Driver deleted successfully"));
    }
}
