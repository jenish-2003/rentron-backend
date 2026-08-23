package com.jcbbooking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "bookings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_number", unique = true, nullable = false, length = 50)
    private String bookingNumber;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "contractor_id")
    private Long contractorId;

    @Column(name = "driver_id")
    private Long driverId;

    @Column(name = "pickup_address_id")
    private Long pickupAddressId;

    @Column(name = "destination_address_id")
    private Long destinationAddressId;

    @Column(name = "booking_date", length = 30)
    private String bookingDate;

    @Column(name = "booking_time", length = 30)
    private String bookingTime;

    @Builder.Default
    @Column(name = "distance_km")
    private Double distanceKm = 0.0;

    @Builder.Default
    @Column(name = "duration_hours")
    private Double durationHours = 0.0;

    // --- Frozen Price Snapshot Fields ---
    @Builder.Default
    @Column(name = "base_amount")
    private Double baseAmount = 0.0;

    @Builder.Default
    @Column(name = "distance_amount")
    private Double distanceAmount = 0.0;

    @Builder.Default
    @Column(name = "time_amount")
    private Double timeAmount = 0.0;

    @Builder.Default
    @Column(name = "waiting_amount")
    private Double waitingAmount = 0.0;

    @Builder.Default
    @Column(name = "driver_amount")
    private Double driverAmount = 0.0;

    @Builder.Default
    @Column(name = "operator_amount")
    private Double operatorAmount = 0.0;

    @Builder.Default
    @Column(name = "booking_fee")
    private Double bookingFee = 0.0;

    @Builder.Default
    @Column(name = "discount_amount")
    private Double discountAmount = 0.0;

    @Builder.Default
    @Column(name = "tax_amount")
    private Double taxAmount = 0.0;

    @Builder.Default
    @Column(name = "total_amount")
    private Double totalAmount = 0.0;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING"; // PENDING, CONFIRMED, ASSIGNED, ACCEPTED, DRIVER_ON_THE_WAY, STARTED, IN_PROGRESS, COMPLETED, CANCELLED, REJECTED

    @Builder.Default
    @Column(name = "payment_status", nullable = false, length = 50)
    private String paymentStatus = "PENDING"; // PENDING, PAID, FAILED

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
}
