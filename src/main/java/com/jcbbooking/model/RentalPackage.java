package com.jcbbooking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "rental_packages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RentalPackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name; // e.g. "1 hr 10 km", "2 hr 20 km", "4 hr 40 km", "8 hr 80 km"

    @Builder.Default
    @Column(name = "vehicle_category", nullable = false, length = 50)
    private String vehicleCategory = "ALL"; // ALL, MINI, SEDAN, SUV

    @Builder.Default
    @Column(name = "included_hours", nullable = false)
    private Double includedHours = 1.0;

    @Builder.Default
    @Column(name = "included_km", nullable = false)
    private Double includedKm = 10.0;

    @Builder.Default
    @Column(name = "base_package_fare", nullable = false, columnDefinition = "DECIMAL(12,2) DEFAULT 349.00")
    private Double basePackageFare = 349.0;

    @Builder.Default
    @Column(name = "extra_km_price", nullable = false, columnDefinition = "DECIMAL(12,2) DEFAULT 11.00")
    private Double extraKmPrice = 11.0;

    @Builder.Default
    @Column(name = "extra_hour_price", nullable = false, columnDefinition = "DECIMAL(12,2) DEFAULT 90.00")
    private Double extraHourPrice = 90.0;

    @Builder.Default
    @Column(name = "booking_fee", nullable = false, columnDefinition = "DECIMAL(12,2) DEFAULT 18.00")
    private Double bookingFee = 18.0;

    @Builder.Default
    @Column(name = "tax_percentage", nullable = false, columnDefinition = "DECIMAL(5,2) DEFAULT 5.00")
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
