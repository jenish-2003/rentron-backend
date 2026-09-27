package com.jcbbooking.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "driver_id")
    private Long driverId;

    @Column(name = "vehicle_type_id")
    private Long vehicleTypeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_type_id", insertable = false, updatable = false)
    private VehicleType vehicleType;

    @Column(name = "vehicle_model_id")
    private Long vehicleModelId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_model_id", insertable = false, updatable = false)
    private VehicleModel vehicleModel;

    @Column(name = "vehicle_sub_model_id")
    private Long vehicleSubModelId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_sub_model_id", insertable = false, updatable = false)
    private VehicleSubModel vehicleSubModel;

    // --- Legacy / Snapshot String Fields for Backward Compatibility ---
    @Column(name = "reg_number", length = 50)
    private String regNumber;

    @Column(name = "vehicle_name", length = 150)
    private String vehicleName;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "machinery_model", length = 100)
    private String machineryModel;

    @Column(name = "machinery_sub_category", length = 100)
    private String machinerySubCategory;

    @Column(name = "experience", length = 50)
    private String experience;

    @Column(name = "mfg_year", length = 20)
    private String mfgYear;

    @Column(name = "machine_class", length = 100)
    private String machineClass;

    @Column(name = "chassis_vin", length = 100)
    private String chassisVin;

    @Column(name = "engine_serial", length = 100)
    private String engineSerial;

    @Column(name = "powertrain", length = 50)
    private String powertrain;

    @Builder.Default
    @Column(name = "status", nullable = false, length = 50)
    private String status = "UNDER_REVIEW"; // UNDER_REVIEW, APPROVED, REJECTED, ACTIVE

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name = "fetch_mode", length = 50)
    private String fetchMode;

    @Builder.Default
    @Column(name = "is_vahan_synced", nullable = false)
    private Boolean isVahanSynced = false;

    @Column(name = "submitted_on", length = 50)
    private String submittedOn;

    @Lob
    @Column(name = "vehicle_description", columnDefinition = "LONGTEXT")
    private String vehicleDescription;

    @Column(name = "driver_name", length = 100)
    private String driverName;

    @Column(name = "driver_phone", length = 30)
    private String driverPhone;

    @Lob
    @Column(name = "rc_front_url", columnDefinition = "LONGTEXT")
    private String rcFrontUrl;

    @Lob
    @Column(name = "rc_back_url", columnDefinition = "LONGTEXT")
    private String rcBackUrl;

    @Lob
    @Column(name = "insurance_url", columnDefinition = "LONGTEXT")
    private String insuranceUrl;

    @Lob
    @Column(name = "front_photo_url", columnDefinition = "LONGTEXT")
    private String frontPhotoUrl;

    @Lob
    @Column(name = "side_photo_url", columnDefinition = "LONGTEXT")
    private String sidePhotoUrl;

    @Lob
    @Column(name = "rear_photo_url", columnDefinition = "LONGTEXT")
    private String rearPhotoUrl;

    @Lob
    @Column(name = "cabin_photo_url", columnDefinition = "LONGTEXT")
    private String cabinPhotoUrl;

    @Lob
    @Column(name = "engine_plate_photo_url", columnDefinition = "LONGTEXT")
    private String enginePlatePhotoUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP")
    private LocalDateTime updatedAt;
}
