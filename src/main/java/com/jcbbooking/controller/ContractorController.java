package com.jcbbooking.controller;

import com.jcbbooking.model.Contractor;
import com.jcbbooking.model.Role;
import com.jcbbooking.model.User;
import com.jcbbooking.repository.ContractorRepository;
import com.jcbbooking.repository.DocumentRepository;
import com.jcbbooking.repository.UserRepository;
import com.jcbbooking.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

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

    @GetMapping
    public ResponseEntity<ApiResponse<List<Contractor>>> getAllContractors() {
        log.info("REST request to get all contractors");
        return ResponseEntity.ok(ApiResponse.success("Contractors retrieved successfully", contractorRepository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Contractor>> getContractorById(@PathVariable Long id) {
        log.info("REST request to get contractor by id: {}", id);
        return contractorRepository.findById(id)
                .map(contractor -> ResponseEntity.ok(ApiResponse.success("Contractor retrieved successfully", contractor)))
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
            // Update fields
            existing.setFullName(contractor.getFullName());
            existing.setPhone(contractor.getPhone());
            existing.setEmail(contractor.getEmail());
            existing.setCompanyName(contractor.getCompanyName());
            existing.setGstNumber(contractor.getGstNumber());
            existing.setExperience(contractor.getExperience());
            if (contractor.getStatus() != null) {
                existing.setStatus(contractor.getStatus());
            }
            contractor = existing;
        }

        Contractor saved = contractorRepository.save(contractor);
        return ResponseEntity.ok(ApiResponse.success(isNew ? "Contractor application submitted" : "Contractor updated successfully", saved));
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<ApiResponse<Contractor>> approveContractor(@PathVariable Long id) {
        log.info("REST request to approve contractor ID: {}", id);
        Contractor contractor = contractorRepository.findById(id).orElse(null);
        if (contractor == null) {
            return ResponseEntity.notFound().build();
        }

        // Find or create User
        User user = userRepository.findByPhone(contractor.getPhone()).orElse(null);
        if (user == null) {
            user = User.builder()
                    .fullName(contractor.getFullName())
                    .phone(contractor.getPhone())
                    .email(contractor.getEmail())
                    .role(Role.CONTRACTOR)
                    .active(true)
                    .verified(true)
                    .passwordHash(passwordEncoder.encode("Password123"))
                    .build();
            user = userRepository.save(user);
        } else {
            user.setActive(true);
            user.setRole(Role.CONTRACTOR);
            user = userRepository.save(user);
        }

        contractor.setUserId(user.getId());
        contractor.setStatus("ACTIVE");
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
        if (contractor.getUserId() != null) {
            userRepository.findById(contractor.getUserId()).ifPresent(user -> {
                user.setActive(false);
                userRepository.save(user);
            });
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
        if (contractor.getUserId() != null) {
            userRepository.findById(contractor.getUserId()).ifPresent(user -> {
                user.setActive(true);
                userRepository.save(user);
            });
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
