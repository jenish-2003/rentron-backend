package com.jcbbooking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_sub_models", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"vehicle_model_id", "name"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSubModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vehicle_model_id", nullable = false)
    private Long vehicleModelId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_model_id", insertable = false, updatable = false)
    private VehicleModel vehicleModel;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "code", length = 100)
    private String code;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "manufacturer", length = 150)
    private String manufacturer;

    @Column(name = "machine_class", length = 150)
    private String machineClass;

    @Column(name = "horse_power", length = 50)
    private String horsePower;

    @Column(name = "capacity", length = 100)
    private String capacity;

    @Column(name = "fuel_type", length = 50)
    private String fuelType;

    @Lob
    @Column(name = "icon_url", columnDefinition = "LONGTEXT")
    private String iconUrl;

    @Builder.Default
    @Column(name = "display_order", nullable = false)
    private Integer displayOrder = 0;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
}
