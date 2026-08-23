package com.jcbbooking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "carts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "pickup_address_id")
    private Long pickupAddressId;

    @Column(name = "destination_address_id")
    private Long destinationAddressId;

    @Column(name = "booking_date", length = 30)
    private String bookingDate;

    @Column(name = "booking_time", length = 30)
    private String bookingTime;

    @Builder.Default
    @Column(name = "duration_hours")
    private Double durationHours = 0.0;

    @Builder.Default
    @Column(name = "distance_km")
    private Double distanceKm = 0.0;

    @Builder.Default
    @Column(name = "estimated_amount")
    private Double estimatedAmount = 0.0;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
}
