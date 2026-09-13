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
        List<DriverBankAccount> bankAccounts = bankAccountRepository.findAllByUserIdOrderByIdDesc(userDetails.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("totalEarnings", totalEarned);
        response.put("totalJobs", completedBookings.size());
        response.put("withdrawableAmount", withdrawableAmount);
        response.put("totalWithdrawn", totalWithdrawn);
        response.put("completedBookings", completedBookings);
        response.put("withdrawals", withdrawals);
        response.put("bankAccounts", bankAccounts);

        return ResponseEntity.ok(ApiResponse.success("Earnings summary retrieved", response));
    }

    @GetMapping("/bank-accounts")
    public ResponseEntity<ApiResponse<List<DriverBankAccount>>> getBankAccounts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<DriverBankAccount> accounts = bankAccountRepository.findAllByUserIdOrderByIdDesc(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Bank accounts retrieved", accounts));
    }

    @GetMapping("/bank-account")
    public ResponseEntity<ApiResponse<DriverBankAccount>> getSingleBankAccount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        List<DriverBankAccount> accounts = bankAccountRepository.findAllByUserIdOrderByIdDesc(userDetails.getId());
        DriverBankAccount primary = accounts.stream().filter(a -> Boolean.TRUE.equals(a.getIsPrimary())).findFirst()
                .orElse(accounts.isEmpty() ? null : accounts.get(0));
        return ResponseEntity.ok(ApiResponse.success("Bank account retrieved", primary));
    }

    @PostMapping("/bank-accounts")
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
                : (rawAccount != null ? rawAccount : "****0000");

        List<DriverBankAccount> existing = bankAccountRepository.findAllByUserIdOrderByIdDesc(userDetails.getId());
        boolean isFirstAccount = existing.isEmpty();

        DriverBankAccount bank = DriverBankAccount.builder()
                .driverId(driver.getId())
                .userId(userDetails.getId())
                .accountHolderName(bankRequest.getAccountHolderName())
                .accountNumberMasked(masked)
                .ifscCode(bankRequest.getIfscCode() != null ? bankRequest.getIfscCode().toUpperCase() : "")
                .upiId(bankRequest.getUpiId())
                .bankName(bankRequest.getBankName() != null ? bankRequest.getBankName() : "Bank Account")
                .isPrimary(isFirstAccount || Boolean.TRUE.equals(bankRequest.getIsPrimary()))
                .isVerified(true)
                .build();

        if (Boolean.TRUE.equals(bank.getIsPrimary())) {
            existing.forEach(a -> {
                a.setIsPrimary(false);
                bankAccountRepository.save(a);
            });
        }

        DriverBankAccount saved = bankAccountRepository.save(bank);
        return ResponseEntity.ok(ApiResponse.success("Bank account added successfully", saved));
    }

    @PostMapping("/bank-account")
    @Transactional
    public ResponseEntity<ApiResponse<DriverBankAccount>> saveLegacyBankAccount(
            @RequestBody DriverBankAccount bankRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return saveBankAccount(bankRequest, userDetails);
    }

    @DeleteMapping("/bank-accounts/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> deleteBankAccount(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        DriverBankAccount account = bankAccountRepository.findByIdAndUserId(id, userDetails.getId()).orElse(null);
        if (account == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Bank account not found"));
        }
        bankAccountRepository.delete(account);
        return ResponseEntity.ok(ApiResponse.success("Bank account deleted successfully", null));
    }

    @PutMapping("/bank-accounts/{id}/primary")
    @Transactional
    public ResponseEntity<ApiResponse<DriverBankAccount>> setPrimaryBankAccount(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<DriverBankAccount> accounts = bankAccountRepository.findAllByUserIdOrderByIdDesc(userDetails.getId());
        DriverBankAccount target = accounts.stream().filter(a -> a.getId().equals(id)).findFirst().orElse(null);

        if (target == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Bank account not found"));
        }

        accounts.forEach(a -> {
            a.setIsPrimary(a.getId().equals(id));
            bankAccountRepository.save(a);
        });

        return ResponseEntity.ok(ApiResponse.success("Primary bank account updated", target));
    }

    @PostMapping("/withdraw")
    @Transactional
    public ResponseEntity<ApiResponse<DriverWithdrawal>> requestWithdrawal(
            @RequestParam Double amount,
            @RequestParam(required = false) Long bankAccountId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Driver driver = driverRepository.findByUserId(userDetails.getId())
                .orElseGet(() -> driverRepository.findByPhone(userDetails.getUser().getPhone()).orElse(null));

        if (driver == null) {
            return ResponseEntity.status(404).body(ApiResponse.error("Driver profile not found"));
        }

        if (amount == null || amount <= 0) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Withdrawal amount must be greater than zero"));
        }

        // 1. Resolve target bank account
        List<DriverBankAccount> accounts = bankAccountRepository.findAllByUserIdOrderByIdDesc(userDetails.getId());
        if (accounts.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Please add your bank account or UPI details before requesting a payout withdrawal"));
        }

        DriverBankAccount selectedBank = null;
        if (bankAccountId != null) {
            selectedBank = accounts.stream().filter(a -> a.getId().equals(bankAccountId)).findFirst().orElse(null);
        }
        if (selectedBank == null) {
            selectedBank = accounts.stream().filter(a -> Boolean.TRUE.equals(a.getIsPrimary())).findFirst().orElse(accounts.get(0));
        }

        String detailsStr = selectedBank.getBankName() + " - " +
                (selectedBank.getAccountNumberMasked() != null ? "A/C: " + selectedBank.getAccountNumberMasked() : "") +
                (selectedBank.getIfscCode() != null ? " (IFSC: " + selectedBank.getIfscCode() + ")" : "") +
                (selectedBank.getUpiId() != null ? " (UPI: " + selectedBank.getUpiId() + ")" : "") +
                " [Holder: " + selectedBank.getAccountHolderName() + "]";

        // 2. Check withdrawable balance
        List<Booking> completedBookings = bookingRepository.findAllByDriverId(driver.getId())
                .stream().filter(b -> "COMPLETED".equalsIgnoreCase(b.getStatus())).toList();

        double totalEarned = completedBookings.stream()
                .mapToDouble(b -> b.getDriverAmount() != null && b.getDriverAmount() > 0 ? b.getDriverAmount() : b.getTotalAmount() * 0.8)
                .sum();

        List<DriverWithdrawal> withdrawals = withdrawalRepository.findAllByDriverIdOrderByRequestedAtDesc(driver.getId());
        double totalWithdrawnOrPending = withdrawals.stream()
                .filter(w -> "APPROVED".equalsIgnoreCase(w.getStatus()) || "PROCESSED".equalsIgnoreCase(w.getStatus()) || "PENDING".equalsIgnoreCase(w.getStatus()))
                .mapToDouble(DriverWithdrawal::getAmount)
                .sum();

        double withdrawableAmount = Math.max(0.0, totalEarned - totalWithdrawnOrPending);

        if (amount > withdrawableAmount) {
            return ResponseEntity.badRequest().body(ApiResponse.error(
                    String.format("Withdrawal amount (₹%.2f) cannot exceed available withdrawable balance (₹%.2f)", amount, withdrawableAmount)));
        }

        DriverWithdrawal withdrawal = DriverWithdrawal.builder()
                .driverId(driver.getId())
                .userId(userDetails.getId())
                .amount(amount)
                .bankAccountId(selectedBank.getId())
                .bankAccountDetails(detailsStr)
                .driverName(driver.getFullName() != null ? driver.getFullName() : userDetails.getUser().getFullName())
                .driverPhone(driver.getPhone() != null ? driver.getPhone() : userDetails.getUser().getPhone())
                .status("PENDING")
                .referenceNumber("WD-" + System.currentTimeMillis())
                .build();

        DriverWithdrawal saved = withdrawalRepository.save(withdrawal);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal request submitted successfully", saved));
    }
}
