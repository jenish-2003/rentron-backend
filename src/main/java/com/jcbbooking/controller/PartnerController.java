package com.jcbbooking.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.jcbbooking.model.Contractor;
import com.jcbbooking.model.Document;
import com.jcbbooking.model.Driver;
import com.jcbbooking.model.PartnerApproval;
import com.jcbbooking.model.Role;
import com.jcbbooking.model.User;
import com.jcbbooking.repository.ContractorRepository;
import com.jcbbooking.repository.DocumentRepository;
import com.jcbbooking.repository.DriverRepository;
import com.jcbbooking.repository.PartnerApprovalRepository;
import com.jcbbooking.repository.UserRepository;
import com.jcbbooking.security.CustomUserDetails;
import com.jcbbooking.util.ApiResponse;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/v1/partners")
@RequiredArgsConstructor
@Slf4j
public class PartnerController {

    private final PartnerApprovalRepository partnerApprovalRepository;
    private final ContractorRepository contractorRepository;
    private final DriverRepository driverRepository;
    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.jcbbooking.repository.DriverBankAccountRepository bankAccountRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPartners(
            @RequestParam(required = false, defaultValue = "ALL") String type,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to get partners with type filter: {}", type);
        
        Map<String, Object> response = new HashMap<>();

        if (userDetails == null || userDetails.getUser() == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Unauthorized request"));
        }

        Role userRole = userDetails.getUser().getRole();

        if (userRole == Role.CONTRACTOR) {
            // BACKEND SECURITY ENFORCEMENT: Contractor can only view their own record and assigned drivers
            Contractor contractor = contractorRepository.findByUserId(userDetails.getId())
                    .orElseGet(() -> contractorRepository.findByPhone(userDetails.getUser().getPhone()).orElse(null));

            if (contractor == null) {
                return ResponseEntity.status(404).body(ApiResponse.error("Contractor profile not found for authenticated user"));
            }

            List<Driver> assignedDrivers = driverRepository.findAllByContractorId(contractor.getId());
            contractor.setAssignedDrivers(assignedDrivers);

            if ("DRIVER".equalsIgnoreCase(type)) {
                response.put("drivers", assignedDrivers);
            } else if ("CONTRACTOR".equalsIgnoreCase(type)) {
                response.put("contractors", List.of(contractor));
            } else {
                response.put("contractors", List.of(contractor));
                response.put("drivers", assignedDrivers);
            }

            return ResponseEntity.ok(ApiResponse.success("Partner data retrieved successfully for contractor", response));
        }

        if (userRole == Role.DRIVER) {
            // BACKEND SECURITY ENFORCEMENT: Driver can only view self profile
            Driver driver = driverRepository.findByUserId(userDetails.getId())
                    .orElseGet(() -> driverRepository.findByPhone(userDetails.getUser().getPhone()).orElse(null));

            if (driver == null) {
                return ResponseEntity.status(404).body(ApiResponse.error("Driver profile not found for authenticated user"));
            }

            response.put("drivers", List.of(driver));
            return ResponseEntity.ok(ApiResponse.success("Partner data retrieved successfully for driver", response));
        }

        // ADMIN has unrestricted visibility
        List<Contractor> contractors = contractorRepository.findAll();
        contractors.forEach(c -> c.setAssignedDrivers(driverRepository.findAllByContractorId(c.getId())));
        List<Driver> drivers = driverRepository.findAll();

        List<PartnerApproval> approvals = partnerApprovalRepository.findAll();
        approvals.forEach(app -> {
            String eType = "CONTRACTOR".equalsIgnoreCase(app.getPartnerType()) ? "CONTRACTOR" : "DRIVER";
            app.setDocuments(documentRepository.findAllByEntityTypeAndEntityId(eType, app.getId()));
        });

        // Compute Metric Stats
        long totalContractors = contractors.size();
        long totalDrivers = drivers.size();
        long totalPartners = totalContractors + totalDrivers;
        
        long activeContractors = contractors.stream().filter(c -> "ACTIVE".equalsIgnoreCase(c.getStatus()) || "ONLINE".equalsIgnoreCase(c.getStatus())).count();
        long activeDrivers = drivers.stream().filter(d -> "ACTIVE".equalsIgnoreCase(d.getStatus()) || "ONLINE".equalsIgnoreCase(d.getStatus()) || "ACTIVE_ONLINE".equalsIgnoreCase(d.getStatus())).count();
        long activeApprovals = approvals.stream().filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus())).count();
        long onlineActiveTotal = Math.max(activeContractors + activeDrivers, activeApprovals);

        long busyDrivers = drivers.stream().filter(d -> "BUSY".equalsIgnoreCase(d.getStatus())).count();
        
        long pendingContractors = contractors.stream().filter(c -> "PENDING_VERIFICATION".equalsIgnoreCase(c.getStatus())).count();
        long pendingDrivers = drivers.stream().filter(d -> "PENDING_VERIFICATION".equalsIgnoreCase(d.getStatus())).count();
        long pendingApprovals = approvals.stream().filter(a -> "PENDING_VERIFICATION".equalsIgnoreCase(a.getStatus())).count();
        long pendingVerification = Math.max(pendingApprovals, pendingContractors + pendingDrivers);

        java.time.LocalDateTime startOfToday = java.time.LocalDateTime.now().with(java.time.LocalTime.MIN);
        long approvedToday = approvals.stream()
                .filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus()) && (a.getUpdatedAt() == null || a.getUpdatedAt().isAfter(startOfToday)))
                .count();
        long rejectedToday = approvals.stream()
                .filter(a -> "REJECTED".equalsIgnoreCase(a.getStatus()) && (a.getUpdatedAt() == null || a.getUpdatedAt().isAfter(startOfToday)))
                .count();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalPartners", totalPartners);
        stats.put("totalContractors", totalContractors);
        stats.put("totalDrivers", totalDrivers);
        stats.put("onlineDrivers", onlineActiveTotal);
        stats.put("busyDrivers", busyDrivers);
        stats.put("pendingVerification", pendingVerification);
        stats.put("approvedToday", approvedToday);
        stats.put("rejectedToday", rejectedToday);

        response.put("contractors", contractors);
        response.put("drivers", drivers);
        response.put("approvals", approvals);
        response.put("stats", stats);

        return ResponseEntity.ok(ApiResponse.success("Partners retrieved successfully", response));
    }

    private String formatDateOnly(java.time.LocalDateTime dt) {
        if (dt == null) return "";
        return dt.toLocalDate().toString();
    }

    @GetMapping("/verification-queue")
    @Transactional(readOnly = true)
    public ResponseEntity<ApiResponse<List<PartnerApproval>>> getVerificationQueue(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String date) {
        log.info("REST request to get partner verification queue status: {}, date: {}", status, date);

        List<PartnerApproval> list = new java.util.ArrayList<>(partnerApprovalRepository.findAll());
        Set<String> existingPhones = list.stream()
                .map(PartnerApproval::getPhone)
                .filter(p -> p != null && !p.trim().isEmpty())
                .collect(Collectors.toSet());

        // In-memory sync for contractors not in approvals table (NO DB SAVE ON GET!)
        contractorRepository.findAll().forEach(c -> {
            if (c.getPhone() != null && !existingPhones.contains(c.getPhone())) {
                PartnerApproval pa = PartnerApproval.builder()
                        .id(c.getId())
                        .fullName(c.getFullName())
                        .phone(c.getPhone())
                        .email(c.getEmail())
                        .companyName(c.getCompanyName())
                        .gstNumber(c.getGstNumber())
                        .experience(c.getExperience())
                        .partnerType("CONTRACTOR")
                        .status(c.getStatus() != null ? c.getStatus() : "PENDING_VERIFICATION")
                        .createdAt(c.getCreatedAt() != null ? c.getCreatedAt() : java.time.LocalDateTime.now())
                        .updatedAt(c.getCreatedAt() != null ? c.getCreatedAt() : java.time.LocalDateTime.now())
                        .build();
                list.add(pa);
                existingPhones.add(c.getPhone());
            }
        });

        // In-memory sync for drivers not in approvals table (NO DB SAVE ON GET!)
        driverRepository.findAll().forEach(d -> {
            if (d.getPhone() != null && !existingPhones.contains(d.getPhone())) {
                PartnerApproval pa = PartnerApproval.builder()
                        .id(d.getId())
                        .fullName(d.getFullName())
                        .phone(d.getPhone())
                        .email(d.getEmail())
                        .licenseNumber(d.getLicenseNumber())
                        .aadhaarNumber(d.getAadhaarNumber())
                        .experience(d.getExperience())
                        .partnerType("DRIVER")
                        .status(d.getStatus() != null ? d.getStatus() : "PENDING_VERIFICATION")
                        .contractorId(d.getContractorId())
                        .createdAt(d.getCreatedAt() != null ? d.getCreatedAt() : java.time.LocalDateTime.now())
                        .updatedAt(d.getCreatedAt() != null ? d.getCreatedAt() : java.time.LocalDateTime.now())
                        .build();
                list.add(pa);
                existingPhones.add(d.getPhone());
            }
        });

        Stream<PartnerApproval> stream = list.stream();

        // Filter by Status (if ALL or empty, return all statuses)
        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            String targetStatus = status.toUpperCase();
            if ("ACTIVE".equalsIgnoreCase(targetStatus)) {
                stream = stream.filter(app -> "ACTIVE".equalsIgnoreCase(app.getStatus()) || "ONLINE".equalsIgnoreCase(app.getStatus()));
            } else {
                stream = stream.filter(app -> targetStatus.equalsIgnoreCase(app.getStatus()));
            }
        } else if (status == null) {
            // Default parameter fallback if omitted entirely
            stream = stream.filter(app -> "PENDING_VERIFICATION".equalsIgnoreCase(app.getStatus()));
        }

        // Filter by Date (if specified)
        if (date != null && !date.trim().isEmpty()) {
            final String targetDate = date.trim();
            stream = stream.filter(app -> {
                String cDate = formatDateOnly(app.getCreatedAt());
                String uDate = formatDateOnly(app.getUpdatedAt());
                // If dates in DB are null/empty, include the record so data is not lost
                if (cDate.isEmpty() && uDate.isEmpty()) {
                    return true;
                }
                return targetDate.equals(cDate) || targetDate.equals(uDate);
            });
        }

        List<PartnerApproval> result = stream.collect(Collectors.toList());

        result.forEach(app -> {
            if (app.getId() != null) {
                String eType = "CONTRACTOR".equalsIgnoreCase(app.getPartnerType()) ? "CONTRACTOR" : "DRIVER";
                app.setDocuments(documentRepository.findAllByEntityTypeAndEntityId(eType, app.getId()));
            }
        });

        return ResponseEntity.ok(ApiResponse.success("Verification queue retrieved successfully", result));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<PartnerApproval>> registerPartner(@RequestBody PartnerApproval partner) {
        log.info("REST request to register partner: {}", partner);
        if (partner.getPhone() == null || partner.getPhone().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Phone number is required"));
        }
        PartnerApproval existingApproval = partnerApprovalRepository.findByPhone(partner.getPhone()).orElse(null);
        Driver existingDriver = driverRepository.findByPhone(partner.getPhone()).orElse(null);
        Contractor existingContractor = contractorRepository.findByPhone(partner.getPhone()).orElse(null);

        if (existingApproval != null || existingDriver != null || existingContractor != null) {
            boolean isPendingOrDraft = (existingApproval != null && ("PENDING_VERIFICATION".equalsIgnoreCase(existingApproval.getStatus()) || "DRAFT".equalsIgnoreCase(existingApproval.getStatus()))) ||
                               (existingDriver != null && ("PENDING_VERIFICATION".equalsIgnoreCase(existingDriver.getStatus()) || "DRAFT".equalsIgnoreCase(existingDriver.getStatus()))) ||
                               (existingContractor != null && ("PENDING_VERIFICATION".equalsIgnoreCase(existingContractor.getStatus()) || "DRAFT".equalsIgnoreCase(existingContractor.getStatus())));

            if (isPendingOrDraft) {
                log.info("Partner with phone {} already exists under DRAFT/PENDING_VERIFICATION. Updating existing record.", partner.getPhone());
                if (existingApproval != null) {
                    if (partner.getFullName() != null) existingApproval.setFullName(partner.getFullName());
                    if (partner.getEmail() != null) existingApproval.setEmail(partner.getEmail());
                    if (partner.getExperience() != null) existingApproval.setExperience(partner.getExperience());
                    existingApproval.setStatus("PENDING_VERIFICATION");
                    partnerApprovalRepository.save(existingApproval);
                }
                if (existingDriver != null) {
                    if (partner.getFullName() != null) existingDriver.setFullName(partner.getFullName());
                    if (partner.getEmail() != null) existingDriver.setEmail(partner.getEmail());
                    if (partner.getExperience() != null) existingDriver.setExperience(partner.getExperience());
                    existingDriver.setStatus("PENDING_VERIFICATION");
                    driverRepository.save(existingDriver);
                }
                userRepository.findByPhone(partner.getPhone()).ifPresent(u -> {
                    if (partner.getFullName() != null && !partner.getFullName().trim().isEmpty()) u.setFullName(partner.getFullName());
                    if (partner.getEmail() != null && !partner.getEmail().trim().isEmpty()) u.setEmail(partner.getEmail());
                    userRepository.save(u);
                });
                
                // Sync Bank Account
                saveOrUpdateBankAccount(partner.getPhone(), partner.getBankHolder(), partner.getBankAccountNo(), partner.getBankIfsc(), partner.getBankUpi(), partner.getFullName());

                PartnerApproval returnObj = existingApproval != null ? existingApproval : partner;
                return ResponseEntity.ok(ApiResponse.success("Partner application updated successfully and pending verification", returnObj));
            } else {
                return ResponseEntity.badRequest().body(ApiResponse.error("Partner with phone already exists"));
            }
        }

        if (partner.getStatus() == null) {
            partner.setStatus("PENDING_VERIFICATION");
        }
        if (partner.getRating() == null) {
            partner.setRating(4.0);
        }
        if (partner.getTotalJobs() == null) {
            partner.setTotalJobs(0);
        }
        if (partner.getTotalEarnings() == null) {
            partner.setTotalEarnings(0.0);
        }

        // 1. Create or resolve User entry with active = false (0) until Admin approves
        Role role = "CONTRACTOR".equalsIgnoreCase(partner.getPartnerType()) ? Role.CONTRACTOR : Role.DRIVER;
        User user = userRepository.findByPhone(partner.getPhone()).orElse(null);
        if (user == null) {
            String rawPassword = partner.getPassword() != null && !partner.getPassword().trim().isEmpty()
                    ? partner.getPassword() : "123456";
            user = User.builder()
                    .phone(partner.getPhone())
                    .fullName(partner.getFullName())
                    .email(partner.getEmail())
                    .role(role)
                    .verified(true)
                    .active(false) // active = 0 (Pending Admin Approval)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .build();
            user = userRepository.save(user);
        }

        partner.setUserId(user.getId());
        partner.setId(null);
        PartnerApproval savedApproval = partnerApprovalRepository.save(partner);

        // 2. Also sync save to legacy tables so legacy views & queries remain 100% active
        if ("CONTRACTOR".equalsIgnoreCase(partner.getPartnerType())) {
            Contractor contractor = Contractor.builder()
                    .fullName(partner.getFullName())
                    .phone(partner.getPhone())
                    .email(partner.getEmail())
                    .companyName(partner.getCompanyName())
                    .gstNumber(partner.getGstNumber())
                    .experience(partner.getExperience())
                    .status(partner.getStatus())
                    .rating(partner.getRating())
                    .build();
            Contractor savedContractor = contractorRepository.save(contractor);
            user.setContractorId(savedContractor.getId());
            userRepository.save(user);
        } else {
            Driver driver = Driver.builder()
                    .userId(user.getId())
                    .fullName(partner.getFullName())
                    .phone(partner.getPhone())
                    .email(partner.getEmail())
                    .licenseNumber(partner.getLicenseNumber())
                    .aadhaarNumber(partner.getAadhaarNumber())
                    .experience(partner.getExperience())
                    .contractorId(partner.getContractorId())
                    .status(partner.getStatus())
                    .rating(partner.getRating())
                    .totalJobs(0)
                    .totalEarnings(0.0)
                    .build();
            Driver savedDriver = driverRepository.save(driver);
            user.setDriverId(savedDriver.getId());
            userRepository.save(user);
        }

        // Sync Bank Account
        saveOrUpdateBankAccount(partner.getPhone(), partner.getBankHolder(), partner.getBankAccountNo(), partner.getBankIfsc(), partner.getBankUpi(), partner.getFullName());

        return ResponseEntity.ok(ApiResponse.success("Partner application registered successfully and pending verification", savedApproval));
    }

    private void saveOrUpdateBankAccount(String phone, String bankHolder, String bankAccountNo, String bankIfsc, String bankUpi, String defaultName) {
        if ((bankAccountNo != null && !bankAccountNo.trim().isEmpty()) ||
            (bankUpi != null && !bankUpi.trim().isEmpty()) ||
            (bankHolder != null && !bankHolder.trim().isEmpty())) {

            User user = userRepository.findByPhone(phone).orElse(null);
            Driver driver = driverRepository.findByPhone(phone).orElse(null);

            if (user != null || driver != null) {
                Long userId = user != null ? user.getId() : (driver != null ? driver.getUserId() : 0L);
                Long driverId = driver != null ? driver.getId() : (userId != null ? userId : 0L);

                if (userId != null && userId > 0) {
                    String masked = bankAccountNo != null && bankAccountNo.length() >= 4
                            ? "****" + bankAccountNo.substring(bankAccountNo.length() - 4)
                            : (bankAccountNo != null ? bankAccountNo : (bankUpi != null ? bankUpi : "****0000"));

                    String bName = (bankIfsc != null && !bankIfsc.trim().isEmpty()) ? "Bank Account" : ((bankUpi != null && !bankUpi.trim().isEmpty()) ? "UPI" : "Bank Account");

                    com.jcbbooking.model.DriverBankAccount existingBank = null;
                    if (userId != null && userId > 0) {
                        existingBank = bankAccountRepository.findAllByUserIdOrderByIdDesc(userId).stream().findFirst().orElse(null);
                    }
                    if (existingBank == null && driverId != null && driverId > 0) {
                        existingBank = bankAccountRepository.findAllByDriverIdOrderByIdDesc(driverId).stream().findFirst().orElse(null);
                    }

                    if (existingBank != null) {
                        if (bankHolder != null && !bankHolder.trim().isEmpty()) existingBank.setAccountHolderName(bankHolder);
                        if (bankAccountNo != null && !bankAccountNo.trim().isEmpty()) existingBank.setAccountNumberMasked(masked);
                        if (bankIfsc != null && !bankIfsc.trim().isEmpty()) existingBank.setIfscCode(bankIfsc.toUpperCase());
                        if (bankUpi != null && !bankUpi.trim().isEmpty()) existingBank.setUpiId(bankUpi);
                        existingBank.setBankName(bName);
                        if (driverId != null && driverId > 0) existingBank.setDriverId(driverId);
                        if (userId != null && userId > 0) existingBank.setUserId(userId);
                        bankAccountRepository.save(existingBank);
                        log.info("Updated DriverBankAccount ID {} for user ID {}", existingBank.getId(), userId);
                    } else {
                        com.jcbbooking.model.DriverBankAccount newBank = com.jcbbooking.model.DriverBankAccount.builder()
                                .driverId(driverId != null ? driverId : userId)
                                .userId(userId)
                                .accountHolderName(bankHolder != null && !bankHolder.trim().isEmpty() ? bankHolder : (defaultName != null ? defaultName : "Driver"))
                                .accountNumberMasked(masked)
                                .ifscCode(bankIfsc != null ? bankIfsc.toUpperCase() : "")
                                .upiId(bankUpi)
                                .bankName(bName)
                                .isPrimary(true)
                                .isVerified(true)
                                .build();
                        bankAccountRepository.save(newBank);
                        log.info("Saved new DriverBankAccount for user ID {}", userId);
                    }
                }
            }
        }
    }

    @PostMapping("/{id}/approve")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> approvePartner(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "DRIVER") String type) {
        log.info("REST request to approve partner ID {} of type {}", id, type);

        // 1. Resolve partner details by ID or phone
        PartnerApproval approval = partnerApprovalRepository.findById(id).orElse(null);
        Contractor contractor = contractorRepository.findById(id).orElse(null);
        Driver driver = driverRepository.findById(id).orElse(null);

        String phone = null;
        String name = null;
        String email = null;
        String pType = type;

        if (approval != null) {
            phone = approval.getPhone();
            name = approval.getFullName();
            email = approval.getEmail();
            pType = approval.getPartnerType() != null ? approval.getPartnerType() : type;
        } else if (contractor != null) {
            phone = contractor.getPhone();
            name = contractor.getFullName();
            email = contractor.getEmail();
            pType = "CONTRACTOR";
        } else if (driver != null) {
            phone = driver.getPhone();
            name = driver.getFullName();
            email = driver.getEmail();
            pType = "DRIVER";
        }

        if (phone != null) {
            if (approval == null) {
                approval = partnerApprovalRepository.findByPhone(phone).orElse(null);
            }
            if (contractor == null && "CONTRACTOR".equalsIgnoreCase(pType)) {
                contractor = contractorRepository.findByPhone(phone).orElse(null);
            }
            if (driver == null && "DRIVER".equalsIgnoreCase(pType)) {
                driver = driverRepository.findByPhone(phone).orElse(null);
            }
        }

        if (name == null && contractor != null) name = contractor.getFullName();
        if (name == null && driver != null) name = driver.getFullName();
        if (phone == null && contractor != null) phone = contractor.getPhone();
        if (phone == null && driver != null) phone = driver.getPhone();

        if (phone == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Partner entity not found for ID: " + id));
        }

        Role role = "CONTRACTOR".equalsIgnoreCase(pType) ? Role.CONTRACTOR : Role.DRIVER;
        String rawPassword = (approval != null && approval.getPassword() != null && !approval.getPassword().trim().isEmpty())
                ? approval.getPassword() : "Password123";

        // 2. Find or create User record in users table
        User user = userRepository.findByPhone(phone).orElse(null);
        if (user == null) {
            user = User.builder()
                    .fullName(name)
                    .phone(phone)
                    .email(email)
                    .role(role)
                    .verified(true)
                    .active(true)
                    .passwordHash(passwordEncoder.encode(rawPassword))
                    .build();
            if (role == Role.CONTRACTOR && contractor != null) {
                user.setContractorId(contractor.getId());
            } else if (role == Role.DRIVER && driver != null) {
                user.setDriverId(driver.getId());
            }
            user = userRepository.save(user);
            log.info("Created new User login entry ID: {} for approved partner phone: {}", user.getId(), phone);
        } else {
            user.setActive(true);
            user.setVerified(true);
            user.setRole(role);
            if (approval != null && approval.getPassword() != null && !approval.getPassword().trim().isEmpty()) {
                user.setPasswordHash(passwordEncoder.encode(approval.getPassword()));
            }
            if (role == Role.CONTRACTOR && contractor != null) {
                user.setContractorId(contractor.getId());
            } else if (role == Role.DRIVER && driver != null) {
                user.setDriverId(driver.getId());
            }
            user = userRepository.save(user);
            log.info("Updated existing User login entry ID: {} for approved partner phone: {}", user.getId(), phone);
        }

        // 3. Update status on Contractor, Driver, and PartnerApproval to ACTIVE
        if (contractor != null) {
            contractor.setStatus("ACTIVE");
            contractor.setUserId(user.getId());
            contractorRepository.save(contractor);
        }
        if (driver != null) {
            driver.setStatus("ACTIVE");
            driver.setUserId(user.getId());
            driverRepository.save(driver);
        }
        if (approval == null && phone != null) {
            approval = partnerApprovalRepository.findByPhone(phone).orElse(null);
        }
        if (approval != null) {
            approval.setStatus("ACTIVE");
            approval.setUserId(user.getId());
            partnerApprovalRepository.save(approval);
        }

        Map<String, Object> resMap = new HashMap<>();
        resMap.put("user", user);
        resMap.put("approvalId", id);

        return ResponseEntity.ok(ApiResponse.success("Partner approved successfully and user account activated", resMap));
    }

    @PostMapping("/{id}/reject")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> rejectPartner(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "DRIVER") String type) {
        log.info("REST request to reject partner ID {}", id);

        PartnerApproval app = partnerApprovalRepository.findById(id).orElse(null);
        Contractor c = contractorRepository.findById(id).orElse(null);
        Driver d = driverRepository.findById(id).orElse(null);

        String p = (app != null) ? app.getPhone() : (c != null ? c.getPhone() : (d != null ? d.getPhone() : null));
        if (p != null) {
            if (app == null) app = partnerApprovalRepository.findByPhone(p).orElse(null);
            if (c == null) c = contractorRepository.findByPhone(p).orElse(null);
            if (d == null) d = driverRepository.findByPhone(p).orElse(null);
        }

        if (app != null) {
            app.setStatus("REJECTED");
            partnerApprovalRepository.save(app);
        }
        if (c != null) {
            c.setStatus("REJECTED");
            contractorRepository.save(c);
        }
        if (d != null) {
            d.setStatus("REJECTED");
            driverRepository.save(d);
        }

        return ResponseEntity.ok(ApiResponse.success("Partner application rejected successfully"));
    }

    @PostMapping("/assign-driver")
    @Transactional
    public ResponseEntity<ApiResponse<Driver>> assignDriverToContractor(
            @RequestParam Long driverId,
            @RequestParam(required = false) Long contractorId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to assign driver ID {} to contractor ID {}", driverId, contractorId);

        Driver driver = driverRepository.findById(driverId).orElse(null);
        if (driver == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Driver not found with id: " + driverId));
        }

        // Validate security permissions
        if (userDetails.getUser().getRole() == Role.CONTRACTOR) {
            Contractor contractor = contractorRepository.findByUserId(userDetails.getId())
                    .orElseGet(() -> contractorRepository.findByPhone(userDetails.getUser().getPhone()).orElse(null));
            if (contractor == null) {
                return ResponseEntity.status(403).body(ApiResponse.error("Authenticated contractor profile not found"));
            }
            contractorId = contractor.getId(); // Force logged-in contractor identity
        }

        if (contractorId != null && contractorId > 0) {
            if (!contractorRepository.existsById(contractorId)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Contractor not found with id: " + contractorId));
            }
            driver.setContractorId(contractorId);
        } else {
            driver.setContractorId(null); // Unassign driver
        }

        Driver saved = driverRepository.save(driver);
        String msg = (contractorId != null && contractorId > 0)
                ? "Driver assigned to contractor successfully"
                : "Driver unassigned successfully";
        return ResponseEntity.ok(ApiResponse.success(msg, saved));
    }
}
