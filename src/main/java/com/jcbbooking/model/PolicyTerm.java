package com.jcbbooking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "policy_terms")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PolicyTerm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_app", nullable = false, length = 30)
    private String targetApp; // CUSTOMER, DRIVER, BOTH

    @Column(name = "policy_type", nullable = false, length = 50)
    private String policyType; // TERMS, PRIVACY, REFUND, CANCELLATION, RENTAL_RULES, OUTSTATION_TERMS

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Lob
    @Column(name = "content", columnDefinition = "LONGTEXT")
    private String content;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean active = true;

    @Builder.Default
    @Column(name = "version", length = 20)
    private String version = "1.0";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
