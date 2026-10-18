package com.jcbbooking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "pricings", uniqueConstraints = {
    @UniqueConstraint(name = "UK_PRICINGS_PRODUCT", columnNames = {"product_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pricing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    @Column(name = "pricing_model", length = 50)
    private String pricingModel; // RIDE or HEAVY_EQUIPMENT

    @Column(name = "charging_method", length = 50)
    private String chargingMethod; // HOURLY, DAILY, HOURLY_DAILY

    @Builder.Default
    @Column(name = "base_price", nullable = false, columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double basePrice = 0.0;

    @Builder.Default
    @Column(name = "per_km_price", nullable = false, columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double perKmPrice = 0.0;

    @Builder.Default
    @Column(name = "per_minute_price", nullable = false, columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double perMinutePrice = 0.0;

    @Builder.Default
    @Column(name = "per_hour_price", nullable = false, columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double perHourPrice = 0.0;

    @Builder.Default
    @Column(name = "daily_rate", columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double dailyRate = 0.0;

    @Builder.Default
    @Column(name = "extra_hour_rate", columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double extraHourRate = 0.0;

    @Builder.Default
    @Column(name = "discount", columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double discount = 0.0;

    @Builder.Default
    @Column(name = "minimum_fare", nullable = false, columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double minimumFare = 0.0;

    @Builder.Default
    @Column(name = "minimum_hours", nullable = false)
    private Integer minimumHours = 0;

    @Builder.Default
    @Column(name = "waiting_charge", nullable = false, columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double waitingCharge = 0.0;

    @Builder.Default
    @Column(name = "driver_charge", nullable = false, columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double driverCharge = 0.0;

    @Builder.Default
    @Column(name = "operator_charge", nullable = false, columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double operatorCharge = 0.0;

    @Builder.Default
    @Column(name = "booking_fee", nullable = false, columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double bookingFee = 0.0;

    @Builder.Default
    @Column(name = "cancellation_fee", nullable = false, columnDefinition = "DECIMAL(12,2) DEFAULT 0.00")
    private Double cancellationFee = 0.0;

    @Builder.Default
    @Column(name = "surge_multiplier", nullable = false, columnDefinition = "DECIMAL(4,2) DEFAULT 1.00")
    private Double surgeMultiplier = 1.0;

    @Builder.Default
    @Column(name = "tax_percentage", nullable = false, columnDefinition = "DECIMAL(5,2) DEFAULT 5.00")
    private Double taxPercentage = 5.0;

    @Builder.Default
    @Column(name = "night_allowance", nullable = false, columnDefinition = "DECIMAL(12,2) DEFAULT 300.00")
    private Double nightAllowance = 300.0;

    @Builder.Default
    @Column(name = "night_allowance_start_hour", nullable = false)
    private Integer nightAllowanceStartHour = 23;

    @Builder.Default
    @Column(name = "night_allowance_end_hour", nullable = false)
    private Integer nightAllowanceEndHour = 6;

    @Builder.Default
    @Column(name = "min_outstation_km_one_way", nullable = false)
    private Double minOutstationKmOneWay = 50.0;

    @Builder.Default
    @Column(name = "min_outstation_km_round_trip", nullable = false)
    private Double minOutstationKmRoundTrip = 250.0;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
}
