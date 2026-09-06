package com.jcbbooking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Builder.Default
    @Column(name = "theme_mode", length = 20)
    private String themeMode = "SYSTEM"; // LIGHT, DARK, SYSTEM

    @Builder.Default
    @Column(name = "preferred_language", length = 30)
    private String preferredLanguage = "English";

    @Builder.Default
    @Column(name = "push_enabled", nullable = false)
    private Boolean pushEnabled = true;

    @Builder.Default
    @Column(name = "booking_offers_enabled", nullable = false)
    private Boolean bookingOffersEnabled = true;

    @Builder.Default
    @Column(name = "booking_updates_enabled", nullable = false)
    private Boolean bookingUpdatesEnabled = true;

    @Builder.Default
    @Column(name = "payment_notifications_enabled", nullable = false)
    private Boolean paymentNotificationsEnabled = true;

    @Builder.Default
    @Column(name = "support_notifications_enabled", nullable = false)
    private Boolean supportNotificationsEnabled = true;

    @Builder.Default
    @Column(name = "promotional_notifications_enabled", nullable = false)
    private Boolean promotionalNotificationsEnabled = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
}
