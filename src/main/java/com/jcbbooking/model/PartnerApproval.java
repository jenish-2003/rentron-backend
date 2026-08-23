package com.jcbbooking.model;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "partner_approvals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PartnerApproval {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "partner_type", nullable = false, length = 50)
    private String partnerType; // CONTRACTOR, DRIVER

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "phone", unique = true, nullable = false, length = 15)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "experience", length = 50)
    private String experience;

    @Column(name = "password", length = 255)
    private String password;

    // Contractor specific fields
    @Column(name = "company_name", length = 150)
    private String companyName;

    @Column(name = "gst_number", length = 50)
    private String gstNumber;

    // Driver specific fields
    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    @Column(name = "aadhaar_number", length = 50)
    private String aadhaarNumber;

    @Column(name = "contractor_id")
    private Long contractorId;

    @Column(name = "user_id")
    private Long userId;

    @Builder.Default
    @Column(name = "rating", nullable = false)
    private Double rating = 4.0;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING_VERIFICATION"; // PENDING_VERIFICATION, ACTIVE, OFFLINE, BUSY, SUSPENDED, REJECTED

    @Builder.Default
    @Column(name = "total_jobs", nullable = false)
    private Integer totalJobs = 0;

    @Builder.Default
    @Column(name = "total_earnings", nullable = false)
    private Double totalEarnings = 0.0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;

    @Transient
    private List<Driver> assignedDrivers;

    @Transient
    private List<Document> documents;
}
