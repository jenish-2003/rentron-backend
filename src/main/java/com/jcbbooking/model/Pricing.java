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
    @Column(name = "base_price", nullable = false)
    private Double basePrice = 0.0;

    @Builder.Default
    @Column(name = "per_km_price", nullable = false)
    private Double perKmPrice = 0.0;

    @Builder.Default
    @Column(name = "per_minute_price", nullable = false)
    private Double perMinutePrice = 0.0;

    @Builder.Default
    @Column(name = "per_hour_price", nullable = false)
    private Double perHourPrice = 0.0;

    @Builder.Default
    @Column(name = "daily_rate")
    private Double dailyRate = 0.0;

    @Builder.Default
    @Column(name = "extra_hour_rate")
    private Double extraHourRate = 0.0;

    @Builder.Default
    @Column(name = "discount")
    private Double discount = 0.0;

    @Builder.Default
    @Column(name = "minimum_fare", nullable = false)
    private Double minimumFare = 0.0;

    @Builder.Default
    @Column(name = "minimum_hours", nullable = false)
    private Integer minimumHours = 0;

    @Builder.Default
    @Column(name = "waiting_charge", nullable = false)
    private Double waitingCharge = 0.0;

    @Builder.Default
    @Column(name = "driver_charge", nullable = false)
    private Double driverCharge = 0.0;

    @Builder.Default
    @Column(name = "operator_charge", nullable = false)
    private Double operatorCharge = 0.0;

    @Builder.Default
    @Column(name = "booking_fee", nullable = false)
    private Double bookingFee = 0.0;

    @Builder.Default
    @Column(name = "cancellation_fee", nullable = false)
    private Double cancellationFee = 0.0;

    @Builder.Default
    @Column(name = "surge_multiplier", nullable = false)
    private Double surgeMultiplier = 1.0;

    @Builder.Default
    @Column(name = "tax_percentage", nullable = false)
    private Double taxPercentage = 5.0;

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
