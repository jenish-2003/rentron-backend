package com.jcbbooking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "drivers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "contractor_id")
    private Long contractorId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "phone", unique = true, nullable = false, length = 15)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    @Column(name = "aadhaar_number", length = 50)
    private String aadhaarNumber;

    @Column(name = "experience", length = 50)
    private String experience;

    @Builder.Default
    @Column(name = "rating", nullable = false)
    private Double rating = 4.0;

    @Builder.Default
    @Column(name = "total_jobs", nullable = false)
    private Integer totalJobs = 0;

    @Builder.Default
    @Column(name = "total_earnings", nullable = false)
    private Double totalEarnings = 0.0;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING_VERIFICATION"; // PENDING_VERIFICATION, ACTIVE, OFFLINE, BUSY, SUSPENDED

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "gps_accuracy")
    private Double gpsAccuracy;

    @Column(name = "location_updated_at")
    private LocalDateTime locationUpdatedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}
