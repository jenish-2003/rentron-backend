package com.jcbbooking.controller;

import com.jcbbooking.model.DriverWithdrawal;
import com.jcbbooking.repository.DriverWithdrawalRepository;
import com.jcbbooking.util.ApiResponse;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/payouts")
@RequiredArgsConstructor
@Slf4j
public class AdminPayoutController {

    private final DriverWithdrawalRepository withdrawalRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DriverWithdrawal>>> getAllPayouts() {
        List<DriverWithdrawal> payouts = withdrawalRepository.findAllByOrderByRequestedAtDesc();
        return ResponseEntity.ok(ApiResponse.success("Payout requests retrieved successfully", payouts));
    }

    @PutMapping("/{id}/process")
    @Transactional
    public ResponseEntity<ApiResponse<DriverWithdrawal>> processPayout(
            @PathVariable Long id,
            @RequestBody(required = false) PayoutProcessRequest request) {

        DriverWithdrawal withdrawal = withdrawalRepository.findById(id).orElse(null);
        if (withdrawal == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Payout request not found"));
        }

        withdrawal.setStatus("PROCESSED");
        withdrawal.setProcessedAt(LocalDateTime.now());
        if (request != null && request.getAdminRemarks() != null) {
            withdrawal.setAdminRemarks(request.getAdminRemarks());
        }

        DriverWithdrawal updated = withdrawalRepository.save(withdrawal);
        return ResponseEntity.ok(ApiResponse.success("Payout marked as paid/processed successfully", updated));
    }

    @PutMapping("/{id}/reject")
    @Transactional
    public ResponseEntity<ApiResponse<DriverWithdrawal>> rejectPayout(
            @PathVariable Long id,
            @RequestBody(required = false) PayoutProcessRequest request) {

        DriverWithdrawal withdrawal = withdrawalRepository.findById(id).orElse(null);
        if (withdrawal == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Payout request not found"));
        }

        withdrawal.setStatus("REJECTED");
        withdrawal.setProcessedAt(LocalDateTime.now());
        if (request != null && request.getAdminRemarks() != null) {
            withdrawal.setAdminRemarks(request.getAdminRemarks());
        }

        DriverWithdrawal updated = withdrawalRepository.save(withdrawal);
        return ResponseEntity.ok(ApiResponse.success("Payout request rejected", updated));
    }

    @Data
    public static class PayoutProcessRequest {
        private String adminRemarks;
    }
}
