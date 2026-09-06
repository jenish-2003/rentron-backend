package com.jcbbooking.controller;

import com.jcbbooking.model.Booking;
import com.jcbbooking.model.Driver;
import com.jcbbooking.model.DriverBankAccount;
import com.jcbbooking.model.DriverWithdrawal;
import com.jcbbooking.repository.BookingRepository;
import com.jcbbooking.repository.DriverBankAccountRepository;
import com.jcbbooking.repository.DriverRepository;
import com.jcbbooking.repository.DriverWithdrawalRepository;
import com.jcbbooking.security.CustomUserDetails;
import com.jcbbooking.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/drivers/me/earnings")
@RequiredArgsConstructor
@Slf4j
public class DriverEarningsController {

    private final DriverRepository driverRepository;
    private final BookingRepository bookingRepository;
    private final DriverBankAccountRepository bankAccountRepository;
    private final DriverWithdrawalRepository withdrawalRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEarningsSummary(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        Driver driver = driverRepository.findByUserId(userDetails.getId())
                .orElseGet(() -> driverRepository.findByPhone(userDetails.getUser().getPhone()).orElse(null));

        if (driver == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Driver profile not found"));
        }

        List<Booking> completedBookings = bookingRepository.findAllByDriverId(driver.getId())
                .stream().filter(b -> "COMPLETED".equalsIgnoreCase(b.getStatus())).toList();

        double totalEarned = completedBookings.stream()
                .mapToDouble(b -> b.getDriverAmount() != null && b.getDriverAmount() > 0 ? b.getDriverAmount() : b.getTotalAmount() * 0.8)
                .sum();

        List<DriverWithdrawal> withdrawals = withdrawalRepository.findAllByDriverIdOrderByRequestedAtDesc(driver.getId());
        double totalWithdrawn = withdrawals.stream()
                .filter(w -> "APPROVED".equalsIgnoreCase(w.getStatus()) || "PROCESSED".equalsIgnoreCase(w.getStatus()))
                .mapToDouble(DriverWithdrawal::getAmount)
                .sum();

        double withdrawableAmount = Math.max(0.0, totalEarned - totalWithdrawn);

        Map<String, Object> response = new HashMap<>();
        response.put("totalEarnings", totalEarned);
        response.put("totalJobs", completedBookings.size());
        response.put("withdrawableAmount", withdrawableAmount);
        response.put("totalWithdrawn", totalWithdrawn);
        response.put("completedBookings", completedBookings);
        response.put("withdrawals", withdrawals);

        return ResponseEntity.ok(ApiResponse.success("Earnings summary retrieved", response));
    }

    @GetMapping("/bank-account")
    public ResponseEntity<ApiResponse<DriverBankAccount>> getBankAccount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        DriverBankAccount bank = bankAccountRepository.findByUserId(userDetails.getId()).orElse(null);
        return ResponseEntity.ok(ApiResponse.success("Bank account retrieved", bank));
    }

    @PostMapping("/bank-account")
    @Transactional
    public ResponseEntity<ApiResponse<DriverBankAccount>> saveBankAccount(
            @RequestBody DriverBankAccount bankRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Driver driver = driverRepository.findByUserId(userDetails.getId())
                .orElseGet(() -> driverRepository.findByPhone(userDetails.getUser().getPhone()).orElse(null));

        if (driver == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Driver profile not found"));
        }

        String rawAccount = bankRequest.getAccountNumberMasked();
        String masked = rawAccount != null && rawAccount.length() >= 4
                ? "****" + rawAccount.substring(rawAccount.length() - 4)
                : "****0000";

        DriverBankAccount bank = bankAccountRepository.findByUserId(userDetails.getId())
                .orElseGet(() -> DriverBankAccount.builder()
                        .driverId(driver.getId())
                        .userId(userDetails.getId())
                        .build());

        bank.setAccountHolderName(bankRequest.getAccountHolderName());
        bank.setAccountNumberMasked(masked);
        bank.setIfscCode(bankRequest.getIfscCode());
        bank.setUpiId(bankRequest.getUpiId());
        bank.setIsVerified(true);

        DriverBankAccount saved = bankAccountRepository.save(bank);
        return ResponseEntity.ok(ApiResponse.success("Bank account saved successfully", saved));
    }

    @PostMapping("/withdraw")
    @Transactional
    public ResponseEntity<ApiResponse<DriverWithdrawal>> requestWithdrawal(
            @RequestParam Double amount,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Driver driver = driverRepository.findByUserId(userDetails.getId())
                .orElseGet(() -> driverRepository.findByPhone(userDetails.getUser().getPhone()).orElse(null));

        if (driver == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Driver profile not found"));
        }

        if (amount <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Withdrawal amount must be greater than zero"));
        }

        DriverWithdrawal withdrawal = DriverWithdrawal.builder()
                .driverId(driver.getId())
                .userId(userDetails.getId())
                .amount(amount)
                .status("PENDING")
                .referenceNumber("WD-" + System.currentTimeMillis())
                .build();

        DriverWithdrawal saved = withdrawalRepository.save(withdrawal);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal request submitted successfully", saved));
    }
}
