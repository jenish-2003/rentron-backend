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

    @Builder.Default
    @Column(name = "booking_type", length = 50)
    private String bookingType = "LOCAL"; // LOCAL, SCHEDULED, RENTAL, OUTSTATION, HEAVY_EQUIPMENT

    @Column(name = "rental_package", length = 50)
    private String rentalPackage; // e.g. "1 hr 10 km", "4 hr 40 km"

    @Column(name = "rental_package_id")
    private Long rentalPackageId;

    @Column(name = "outstation_trip_type", length = 30)
    private String outstationTripType; // ONE_WAY, ROUND_TRIP

    @com.fasterxml.jackson.annotation.JsonAlias({"scheduledPickupAt", "scheduledAt"})
    @Column(name = "scheduled_pickup_at")
    private LocalDateTime scheduledPickupAt;

    @com.fasterxml.jackson.annotation.JsonAlias({"stopAddressText", "stopAddress"})
    @Column(name = "stop_address_text", length = 500)
    private String stopAddressText;

    @com.fasterxml.jackson.annotation.JsonAlias({"stopLatitude", "stopLat"})
    @Column(name = "stop_latitude")
    private Double stopLatitude;

    @com.fasterxml.jackson.annotation.JsonAlias({"stopLongitude", "stopLng"})
    @Column(name = "stop_longitude")
    private Double stopLongitude;

    @Column(name = "return_at")
    private LocalDateTime returnAt;

    @Column(name = "return_date", length = 30)
    private String returnDate;

    @Column(name = "return_time", length = 30)
    private String returnTime;

    @com.fasterxml.jackson.annotation.JsonAlias({"pickupAddressText", "pickupAddress"})
    @Column(name = "pickup_address_text", length = 500)
    private String pickupAddressText;

    @com.fasterxml.jackson.annotation.JsonAlias({"destinationAddressText", "destinationAddress", "dropAddress"})
    @Column(name = "destination_address_text", length = 500)
    private String destinationAddressText;

    @Column(name = "pickup_latitude")
    private Double pickupLatitude;

    @Column(name = "pickup_longitude")
    private Double pickupLongitude;

    @Column(name = "drop_latitude")
    private Double dropLatitude;

    @Column(name = "drop_longitude")
    private Double dropLongitude;

    @Builder.Default
    @Column(name = "payment_method", length = 50)
    private String paymentMethod = "CASH"; // CASH, ONLINE, WALLET

    @Builder.Default
    @Column(name = "driver_tip", columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double driverTip = 0.0;

    @Column(name = "otp", length = 10)
    private String otp;

    @Column(name = "ride_start_otp_hash", length = 128)
    private String rideStartOtpHash;

    @Column(name = "otp_generated_at")
    private LocalDateTime otpGeneratedAt;

    @Column(name = "otp_expires_at")
    private LocalDateTime otpExpiresAt;

    @Builder.Default
    @Column(name = "otp_attempts")
    private Integer otpAttempts = 0;

    @Builder.Default
    @Column(name = "otp_max_attempts")
    private Integer otpMaxAttempts = 5;

    @Column(name = "otp_verified_at")
    private LocalDateTime otpVerifiedAt;

    @Column(name = "driver_arrived_at")
    private LocalDateTime driverArrivedAt;

    @Column(name = "ride_started_at")
    private LocalDateTime rideStartedAt;

    @Column(name = "ride_completed_at")
    private LocalDateTime rideCompletedAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "worksite_photo_url", length = 500)
    private String worksitePhotoUrl;

    @Column(name = "booking_date", length = 30)
    private String bookingDate;

    @Column(name = "booking_time", length = 30)
    private String bookingTime;

    @Builder.Default
    @Column(name = "distance_km")
    private Double distanceKm = 0.0;

    @Builder.Default
    @Column(name = "estimated_distance_km")
    private Double estimatedDistanceKm = 0.0;

    @Builder.Default
    @Column(name = "duration_hours")
    private Double durationHours = 0.0;

    @Builder.Default
    @Column(name = "estimated_duration_minutes")
    private Double estimatedDurationMinutes = 0.0;

    @Builder.Default
    @Column(name = "estimated_fare", columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double estimatedFare = 0.0;

    @Builder.Default
    @Column(name = "final_fare", columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double finalFare = 0.0;

    @Column(name = "fare_breakdown_snapshot", columnDefinition = "LONGTEXT")
    private String fareBreakdownSnapshot;

    // --- Frozen Price Snapshot Fields ---
    @Builder.Default
    @Column(name = "base_amount", columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double baseAmount = 0.0;

    @Builder.Default
    @Column(name = "distance_amount", columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double distanceAmount = 0.0;

    @Builder.Default
    @Column(name = "time_amount", columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double timeAmount = 0.0;

    @Builder.Default
    @Column(name = "waiting_amount", columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double waitingAmount = 0.0;

    @Builder.Default
    @Column(name = "driver_amount", columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double driverAmount = 0.0;

    @Builder.Default
    @Column(name = "operator_amount", columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double operatorAmount = 0.0;

    @Builder.Default
    @Column(name = "booking_fee", columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double bookingFee = 0.0;

    @Builder.Default
    @Column(name = "discount_amount", columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double discountAmount = 0.0;

    @Builder.Default
    @Column(name = "tax_amount", columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double taxAmount = 0.0;

    @Builder.Default
    @Column(name = "total_amount", columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
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
