package com.jcbbooking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "booking_assignments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingAssignment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "layer_index", nullable = false)
    private Integer layerIndex; // 1, 2, 3, 4

    @Column(name = "radius_km", nullable = false)
    private Double radiusKm;

    @Column(name = "candidate_type", length = 30)
    private String candidateType; // DRIVER, CONTRACTOR

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "candidate_user_id")
    private Long candidateUserId;

    @Column(name = "distance_km")
    private Double distanceKm;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 30)
    private String status = "OFFERED"; // OFFERED, ACCEPTED, REJECTED, EXPIRED, CANCELLED, SKIPPED

    @CreationTimestamp
    @Column(name = "offered_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime offeredAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
}
