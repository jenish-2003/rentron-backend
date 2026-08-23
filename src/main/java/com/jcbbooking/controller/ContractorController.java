package com.jcbbooking.controller;

import java.io.File;
import java.util.List;

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

import com.jcbbooking.model.Contractor;
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
@RequestMapping("/api/v1/contractors")
@RequiredArgsConstructor
@Slf4j
public class ContractorController {

    private final ContractorRepository contractorRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final DocumentRepository documentRepository;

    @Value("${core.fileTransfer.primaryUploadFolder:/opt/microservice/upload/images}")
    private String primaryUploadFolder;

    private final DriverRepository driverRepository;
    private final com.jcbbooking.repository.PartnerApprovalRepository partnerApprovalRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Contractor>>> getAllContractors(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to get all contractors");
        if (userDetails != null && userDetails.getUser().getRole() == Role.CONTRACTOR) {
            Contractor contractor = contractorRepository.findByUserId(userDetails.getId())
                    .orElseGet(() -> contractorRepository.findByPhone(userDetails.getUser().getPhone()).orElse(null));
            if (contractor != null) {
                contractor.setAssignedDrivers(driverRepository.findAllByContractorId(contractor.getId()));
                return ResponseEntity.ok(ApiResponse.success("Contractor retrieved successfully", List.of(contractor)));
            }
            return ResponseEntity.ok(ApiResponse.success("No contractor found", List.of()));
        }

        List<Contractor> list = contractorRepository.findAll();
        list.forEach(c -> c.setAssignedDrivers(driverRepository.findAllByContractorId(c.getId())));
        return ResponseEntity.ok(ApiResponse.success("Contractors retrieved successfully", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Contractor>> getContractorById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to get contractor by id: {}", id);
        if (userDetails != null && userDetails.getUser().getRole() == Role.CONTRACTOR) {
            Contractor contractor = contractorRepository.findByUserId(userDetails.getId())
                    .orElseGet(() -> contractorRepository.findByPhone(userDetails.getUser().getPhone()).orElse(null));
            if (contractor == null || !contractor.getId().equals(id)) {
                return ResponseEntity.status(403).body(ApiResponse.error("Unauthorized access to contractor details"));
            }
            contractor.setAssignedDrivers(driverRepository.findAllByContractorId(contractor.getId()));
            return ResponseEntity.ok(ApiResponse.success("Contractor retrieved successfully", contractor));
        }

        return contractorRepository.findById(id)
                .map(contractor -> {
                    contractor.setAssignedDrivers(driverRepository.findAllByContractorId(contractor.getId()));
                    return ResponseEntity.ok(ApiResponse.success("Contractor retrieved successfully", contractor));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<Contractor>> saveContractor(@RequestBody Contractor contractor) {
        log.info("REST request to save/update contractor: {}", contractor);
        boolean isNew = (contractor.getId() == null || contractor.getId() == 0);

        if (isNew) {
            contractor.setId(null);
            if (contractorRepository.existsByPhone(contractor.getPhone())) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Contractor with phone already exists"));
            }
            if (contractor.getStatus() == null) {
                contractor.setStatus("PENDING_VERIFICATION");
            }
            if (contractor.getRating() == null) {
                contractor.setRating(4.0);
            }
        } else {
            Contractor existing = contractorRepository.findById(contractor.getId()).orElse(null);
            if (existing == null) {
                return ResponseEntity.notFound().build();
            }

            // Sync User creation if status is set to ACTIVE
            if ("ACTIVE".equals(contractor.getStatus()) && !"ACTIVE".equals(existing.getStatus())) {
                User user = userRepository.findByPhone(existing.getPhone()).orElse(null);
                if (user == null) {
                    user = User.builder()
                            .fullName(existing.getFullName())
                            .phone(existing.getPhone())
                            .email(existing.getEmail())
                            .role(Role.CONTRACTOR)
                            .contractorId(existing.getId())
                            .active(true)
                            .verified(true)
                            .passwordHash(passwordEncoder.encode("Password123"))
                            .build();
                    user = userRepository.save(user);
                } else {
                    user.setActive(true);
                    user.setRole(Role.CONTRACTOR);
                    user.setContractorId(existing.getId());
                    user = userRepository.save(user);
                }
                existing.setUserId(user.getId());
            }

            // Sync User deactivation if status is set to SUSPENDED
            if ("SUSPENDED".equals(contractor.getStatus()) && !"SUSPENDED".equals(existing.getStatus())) {
                if (existing.getUserId() != null) {
                    userRepository.findById(existing.getUserId()).ifPresent(user -> {
                        user.setActive(false);
                        userRepository.save(user);
                    });
                } else {
                    userRepository.findByPhone(existing.getPhone()).ifPresent(user -> {
                        user.setActive(false);
                        userRepository.save(user);
                        existing.setUserId(user.getId());
                    });
                }
            }

            // Update fields
            existing.setFullName(contractor.getFullName());
            existing.setPhone(contractor.getPhone());
            existing.setEmail(contractor.getEmail());
            existing.setCompanyName(contractor.getCompanyName());
            existing.setGstNumber(contractor.getGstNumber());
            existing.setExperience(contractor.getExperience());
            if (contractor.getStatus() != null) {
                existing.setStatus(contractor.getStatus());
                syncPartnerApprovalStatus(existing.getPhone(), contractor.getStatus());
            }
            contractor = existing;
        }

        Contractor saved = contractorRepository.save(contractor);
        return ResponseEntity.ok(ApiResponse.success(isNew ? "Contractor application submitted" : "Contractor updated successfully", saved));
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
                    .fullName(fullName != null ? fullName : "Contractor Partner")
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
            log.info("Created missing User entry ID {} for phone {}, active={}", user.getId(), phone, active);
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
            log.info("Updated existing User entry ID {} for phone {}, active={}", user.getId(), phone, active);
        }
        return user;
    }

    private void syncPartnerApprovalStatus(String phone, String status) {
        if (phone != null && !phone.trim().isEmpty()) {
            partnerApprovalRepository.findByPhone(phone).ifPresent(pa -> {
                pa.setStatus(status);
                partnerApprovalRepository.save(pa);
                log.info("Synced PartnerApproval status to {} for phone {}", status, phone);
            });
        }
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<ApiResponse<Contractor>> approveContractor(@PathVariable Long id) {
        log.info("REST request to approve contractor ID: {}", id);
        Contractor contractor = contractorRepository.findById(id).orElse(null);
        if (contractor == null) {
            return ResponseEntity.notFound().build();
        }

        User user = syncUserAccount(contractor.getPhone(), contractor.getFullName(), contractor.getEmail(), Role.CONTRACTOR, contractor.getId(), null, true);
        if (user != null) {
            contractor.setUserId(user.getId());
        }
        contractor.setStatus("ACTIVE");
        syncPartnerApprovalStatus(contractor.getPhone(), "ACTIVE");
        Contractor saved = contractorRepository.save(contractor);

        return ResponseEntity.ok(ApiResponse.success("Contractor approved successfully and user account activated", saved));
    }

    @PostMapping("/{id}/reject")
    @Transactional
    public ResponseEntity<ApiResponse<Contractor>> rejectContractor(@PathVariable Long id) {
        log.info("REST request to reject contractor ID: {}", id);
        Contractor contractor = contractorRepository.findById(id).orElse(null);
        if (contractor == null) {
            return ResponseEntity.notFound().build();
        }

        contractor.setStatus("REJECTED");
        syncPartnerApprovalStatus(contractor.getPhone(), "REJECTED");
        Contractor saved = contractorRepository.save(contractor);
        return ResponseEntity.ok(ApiResponse.success("Contractor application rejected", saved));
    }

    @PostMapping("/{id}/suspend")
    @Transactional
    public ResponseEntity<ApiResponse<Contractor>> suspendContractor(@PathVariable Long id) {
        log.info("REST request to suspend contractor ID: {}", id);
        Contractor contractor = contractorRepository.findById(id).orElse(null);
        if (contractor == null) {
            return ResponseEntity.notFound().build();
        }

        contractor.setStatus("SUSPENDED");
        syncPartnerApprovalStatus(contractor.getPhone(), "SUSPENDED");
        User user = syncUserAccount(contractor.getPhone(), contractor.getFullName(), contractor.getEmail(), Role.CONTRACTOR, contractor.getId(), null, false);
        if (user != null) {
            contractor.setUserId(user.getId());
        }
        Contractor saved = contractorRepository.save(contractor);
        return ResponseEntity.ok(ApiResponse.success("Contractor suspended successfully and user account deactivated", saved));
    }

    @PostMapping("/{id}/unsuspend")
    @Transactional
    public ResponseEntity<ApiResponse<Contractor>> unsuspendContractor(@PathVariable Long id) {
        log.info("REST request to unsuspend contractor ID: {}", id);
        Contractor contractor = contractorRepository.findById(id).orElse(null);
        if (contractor == null) {
            return ResponseEntity.notFound().build();
        }

        contractor.setStatus("ACTIVE");
        syncPartnerApprovalStatus(contractor.getPhone(), "ACTIVE");
        User user = syncUserAccount(contractor.getPhone(), contractor.getFullName(), contractor.getEmail(), Role.CONTRACTOR, contractor.getId(), null, true);
        if (user != null) {
            contractor.setUserId(user.getId());
        }
        Contractor saved = contractorRepository.save(contractor);
        return ResponseEntity.ok(ApiResponse.success("Contractor activated successfully and user account activated", saved));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteContractor(@PathVariable Long id) {
        log.info("REST request to delete contractor ID: {}", id);
        if (!contractorRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        // Cascade-delete all associated documents (files + DB records)
        documentRepository.findAllByEntityTypeAndEntityId("CONTRACTOR", id).forEach(doc -> {
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
        contractorRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success("Contractor deleted successfully"));
    }
}
