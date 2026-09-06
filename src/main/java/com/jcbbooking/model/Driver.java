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

    @Column(name = "operation_type", length = 50)
    private String operationType; // OWN, RENT_OR_LEASE, FLEET

    @Column(name = "selected_vehicle_type", length = 50)
    private String selectedVehicleType; // CAR, AUTO, BIKE, MACHINERY

    @Column(name = "selected_machinery_model", length = 100)
    private String selectedMachineryModel;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "dob", length = 30)
    private String dob;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "profile_photo_url", length = 500)
    private String profilePhotoUrl;

    @Column(name = "preferred_language", length = 50)
    private String preferredLanguage;

    @Column(name = "referral_code", length = 50)
    private String referralCode;

    @Column(name = "terms_accepted_at")
    private LocalDateTime termsAcceptedAt;

    @Builder.Default
    @Column(name = "is_online", nullable = false)
    private Boolean isOnline = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;
}

