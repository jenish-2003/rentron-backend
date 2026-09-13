package com.jcbbooking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "driver_withdrawals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverWithdrawal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "driver_id", nullable = false)
    private Long driverId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "bank_account_id")
    private Long bankAccountId;

    @Column(name = "bank_account_details", length = 255)
    private String bankAccountDetails;

    @Column(name = "driver_name", length = 150)
    private String driverName;

    @Column(name = "driver_phone", length = 30)
    private String driverPhone;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "amount", nullable = false)
    private Double amount;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, PROCESSED

    @Column(name = "reference_number", length = 100)
    private String referenceNumber;

    @Column(name = "admin_remarks", length = 255)
    private String adminRemarks;

    @CreationTimestamp
    @Column(name = "requested_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime requestedAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
}
