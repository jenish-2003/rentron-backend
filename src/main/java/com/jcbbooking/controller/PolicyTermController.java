package com.jcbbooking.controller;

import com.jcbbooking.model.PolicyTerm;
import com.jcbbooking.repository.PolicyTermRepository;
import com.jcbbooking.util.ApiResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PolicyTermController {

    private final PolicyTermRepository policyTermRepository;

    @PostConstruct
    public void initDefaults() {
        if (policyTermRepository.count() == 0) {
            log.info("Initializing default Policy and Terms for Rentron Customer & Driver platforms...");
            policyTermRepository.saveAll(Arrays.asList(
                    PolicyTerm.builder()
                            .targetApp("CUSTOMER")
                            .policyType("TERMS")
                            .title("Terms & Conditions")
                            .version("1.0")
                            .active(true)
                            .content("Welcome to Rentron. By using our platform for local rides, hourly rentals, and outstation trips, you agree to comply with our service standards, respectful conduct with partners, and prompt payment of confirmed fares. Tolls, state taxes, and parking fees must be borne directly as applicable.")
                            .build(),
                    PolicyTerm.builder()
                            .targetApp("CUSTOMER")
                            .policyType("PRIVACY")
                            .title("Privacy Policy")
                            .version("1.0")
                            .active(true)
                            .content("Rentron values your privacy. We collect trip coordinates, contact details, and transaction history solely to facilitate real-time dispatch, route calculation, invoice delivery, and emergency passenger safety assistance.")
                            .build(),
                    PolicyTerm.builder()
                            .targetApp("CUSTOMER")
                            .policyType("REFUND")
                            .title("Refund Policy")
                            .version("1.0")
                            .active(true)
                            .content("Refunds for cancelled rides or disputes are processed back to the original source of payment within 5 to 7 business days following investigation and approval by our customer support desk.")
                            .build(),
                    PolicyTerm.builder()
                            .targetApp("CUSTOMER")
                            .policyType("CANCELLATION")
                            .title("Cancellation Policy")
                            .version("1.0")
                            .active(true)
                            .content("Free cancellation is allowed within 5 minutes of booking confirmation. Cancellations beyond 5 minutes or after the driver arrives at the pickup point may attract a standard convenience fee of ₹50.")
                            .build(),
                    PolicyTerm.builder()
                            .targetApp("CUSTOMER")
                            .policyType("RENTAL_RULES")
                            .title("Rental Package Rules & Restrictions")
                            .version("1.0")
                            .active(true)
                            .content("1. Rental can be used for local travels only. Package cannot be changed after booking is confirmed.\n2. For usage beyond selected package, additional fare will be applicable as per selected category rate card.\n3. Additional GST applicable on fare. Toll will be added in final bill if applicable; please pay parking fee when required.\n4. Base fare amount is the minimum bill amount a customer has to pay for the package.")
                            .build(),
                    PolicyTerm.builder()
                            .targetApp("CUSTOMER")
                            .policyType("OUTSTATION_TERMS")
                            .title("Outstation Terms & Guidelines")
                            .version("1.0")
                            .active(true)
                            .content("1. Outstation rides exclude toll costs, parking fees, interstate permits, and state taxes.\n2. Additional hours beyond package will be billed hourly.\n3. Driver allowance per 24 hours will apply for multi-day outstation itineraries.\n4. Night time allowance applies for driving between 11:00 PM and 06:00 AM.")
                            .build(),
                    PolicyTerm.builder()
                            .targetApp("DRIVER")
                            .policyType("TERMS")
                            .title("Driver Partner Terms")
                            .version("1.0")
                            .active(true)
                            .content("Driver partners must maintain valid commercial driver licensing, up-to-date vehicular registration & insurance, and uphold five-star courtesy, hygiene, and safe navigation standards on every assigned ride.")
                            .build(),
                    PolicyTerm.builder()
                            .targetApp("DRIVER")
                            .policyType("CANCELLATION")
                            .title("Driver Cancellation & Penalty Policy")
                            .version("1.0")
                            .active(true)
                            .content("Driver cancellations without emergency dispatch notification affect platform acceptance scores and priority assignment tier status.")
                            .build()
            ));
        }
    }

    // Public endpoint for apps
    @GetMapping("/api/v1/policies")
    public ResponseEntity<ApiResponse<?>> getPolicies(
            @RequestParam(required = false, defaultValue = "CUSTOMER") String targetApp,
            @RequestParam(required = false) String policyType) {
        
        List<String> targetList = Arrays.asList(targetApp.toUpperCase(), "BOTH");
        if (policyType != null && !policyType.trim().isEmpty()) {
            Optional<PolicyTerm> policy = policyTermRepository
                    .findFirstByTargetAppInAndPolicyTypeIgnoreCaseAndActiveTrueOrderByUpdatedAtDesc(targetList, policyType.trim());
            return ResponseEntity.ok(ApiResponse.success("Policy retrieved", policy.orElse(null)));
        } else {
            List<PolicyTerm> policies = policyTermRepository.findByTargetAppInAndActiveTrue(targetList);
            return ResponseEntity.ok(ApiResponse.success("Policies retrieved", policies));
        }
    }

    // Admin endpoints
    @GetMapping("/api/v1/admin/policies")
    public ResponseEntity<ApiResponse<List<PolicyTerm>>> listAllPoliciesAdmin() {
        return ResponseEntity.ok(ApiResponse.success("All policies retrieved", policyTermRepository.findAllByOrderByTargetAppAscPolicyTypeAsc()));
    }

    @PostMapping("/api/v1/admin/policies")
    @Transactional
    public ResponseEntity<ApiResponse<PolicyTerm>> createPolicy(@RequestBody PolicyTerm term) {
        if (term.getTitle() == null || term.getTitle().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Title is required"));
        }
        if (term.getTargetApp() == null) term.setTargetApp("CUSTOMER");
        if (term.getPolicyType() == null) term.setPolicyType("TERMS");
        if (term.getActive() == null) term.setActive(true);

        PolicyTerm saved = policyTermRepository.save(term);
        return ResponseEntity.ok(ApiResponse.success("Policy created successfully", saved));
    }

    @PutMapping("/api/v1/admin/policies/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<PolicyTerm>> updatePolicy(
            @PathVariable Long id,
            @RequestBody PolicyTerm updated) {
        return policyTermRepository.findById(id).map(existing -> {
            if (updated.getTitle() != null) existing.setTitle(updated.getTitle());
            if (updated.getContent() != null) existing.setContent(updated.getContent());
            if (updated.getTargetApp() != null) existing.setTargetApp(updated.getTargetApp());
            if (updated.getPolicyType() != null) existing.setPolicyType(updated.getPolicyType());
            if (updated.getActive() != null) existing.setActive(updated.getActive());
            if (updated.getVersion() != null) existing.setVersion(updated.getVersion());
            PolicyTerm saved = policyTermRepository.save(existing);
            return ResponseEntity.ok(ApiResponse.success("Policy updated successfully", saved));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/api/v1/admin/policies/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deletePolicy(@PathVariable Long id) {
        if (policyTermRepository.existsById(id)) {
            policyTermRepository.deleteById(id);
            return ResponseEntity.ok(ApiResponse.success("Policy deleted successfully"));
        }
        return ResponseEntity.notFound().build();
    }
}
