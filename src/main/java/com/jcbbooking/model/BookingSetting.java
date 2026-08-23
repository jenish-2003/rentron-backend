package com.jcbbooking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "booking_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "setting_key", unique = true, nullable = false, length = 50)
    private String settingKey; // e.g. DEFAULT_SETTINGS

    // --- Payment Configuration ---
    @Builder.Default
    @Column(name = "gateway", length = 50)
    private String gateway = "RAZORPAY";

    @Builder.Default
    @Column(name = "payment_enabled", nullable = false)
    private Boolean paymentEnabled = true;

    @Column(name = "key_id", length = 150)
    private String keyId;

    @Column(name = "key_secret", length = 150)
    private String keySecret;

    @Column(name = "webhook_secret", length = 150)
    private String webhookSecret;

    @Builder.Default
    @Column(name = "currency", length = 10)
    private String currency = "INR";

    @Builder.Default
    @Column(name = "environment", length = 20)
    private String environment = "TEST"; // TEST, LIVE

    // --- Auto-Assignment Configuration ---
    @Builder.Default
    @Column(name = "assignment_type", nullable = false, length = 30)
    private String assignmentType = "AUTO"; // AUTO, MANUAL

    @Builder.Default
    @Column(name = "initial_radius_km", nullable = false)
    private Double initialRadiusKm = 5.0;

    @Builder.Default
    @Column(name = "radius_increment_km", nullable = false)
    private Double radiusIncrementKm = 5.0;

    @Builder.Default
    @Column(name = "max_radius_km", nullable = false)
    private Double maxRadiusKm = 20.0;

    @Builder.Default
    @Column(name = "assignment_timeout_seconds", nullable = false)
    private Integer assignmentTimeoutSeconds = 30;

    @Builder.Default
    @Column(name = "max_location_age_seconds", nullable = false)
    private Integer maxLocationAgeSeconds = 300; // 5 mins

    @Builder.Default
    @Column(name = "notify_admin_when_unassigned", nullable = false)
    private Boolean notifyAdminWhenUnassigned = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
}
