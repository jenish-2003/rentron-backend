package com.jcbbooking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "contractors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Contractor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(name = "phone", unique = true, nullable = false, length = 15)
    private String phone;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "company_name", length = 150)
    private String companyName;

    @Column(name = "gst_number", length = 50)
    private String gstNumber;

    @Column(name = "experience", length = 50)
    private String experience;

    @Builder.Default
    @Column(name = "rating", nullable = false)
    private Double rating = 4.0;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 50)
    private String status = "PENDING_VERIFICATION"; // PENDING_VERIFICATION, ACTIVE, SUSPENDED

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @Transient
    private java.util.List<Driver> assignedDrivers;
}
