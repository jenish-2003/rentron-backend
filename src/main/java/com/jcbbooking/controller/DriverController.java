package com.jcbbooking.controller;

import com.jcbbooking.model.Driver;
import com.jcbbooking.model.Role;
import com.jcbbooking.model.User;
import com.jcbbooking.repository.DocumentRepository;
import com.jcbbooking.repository.DriverRepository;
import com.jcbbooking.repository.UserRepository;
import com.jcbbooking.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.util.List;
import java.util.Optional;

import com.jcbbooking.repository.ContractorRepository;
import com.jcbbooking.security.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

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

    @Value("${core.fileTransfer.primaryUploadFolder:/opt/microservice/upload/images}")
    private String primaryUploadFolder;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Driver>>> getAllDrivers() {
        log.info("REST request to get all drivers");
        return ResponseEntity.ok(ApiResponse.success("Drivers retrieved successfully", driverRepository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Driver>> getDriverById(@PathVariable Long id) {
        log.info("REST request to get driver by id: {}", id);
        return driverRepository.findById(id)
                .map(driver -> ResponseEntity.ok(ApiResponse.success("Driver retrieved successfully", driver)))
                .orElse(ResponseEntity.notFound().build());
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
            }
            driver = existing;
        }

        Driver saved = driverRepository.save(driver);
        return ResponseEntity.ok(ApiResponse.success(isNew ? "Driver application submitted" : "Driver updated successfully", saved));
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<ApiResponse<Driver>> approveDriver(@PathVariable Long id) {
        log.info("REST request to approve driver ID: {}", id);
        Driver driver = driverRepository.findById(id).orElse(null);
        if (driver == null) {
            return ResponseEntity.notFound().build();
        }

        // Find or create User
        User user = userRepository.findByPhone(driver.getPhone()).orElse(null);
        if (user == null) {
            user = User.builder()
                    .fullName(driver.getFullName())
                    .phone(driver.getPhone())
                    .email(driver.getEmail())
                    .role(Role.DRIVER)
                    .active(true)
                    .verified(true)
                    .passwordHash(passwordEncoder.encode("Password123"))
                    .build();
            user = userRepository.save(user);
        } else {
            user.setActive(true);
            user.setRole(Role.DRIVER);
            user = userRepository.save(user);
        }

        driver.setUserId(user.getId());
        driver.setStatus("ACTIVE");
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
        if (driver.getUserId() != null) {
            userRepository.findById(driver.getUserId()).ifPresent(user -> {
                user.setActive(false);
                userRepository.save(user);
            });
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
        if (driver.getUserId() != null) {
            userRepository.findById(driver.getUserId()).ifPresent(user -> {
                user.setActive(true);
                userRepository.save(user);
            });
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
