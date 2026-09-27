package com.jcbbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleResponse {

    private Long id;
    private Long userId;
    private Long driverId;

    private Long vehicleTypeId;
    private String vehicleTypeName;

    private Long vehicleModelId;
    private String vehicleModelName;

    private Long vehicleSubModelId;
    private String vehicleSubModelName;

    private String regNumber;
    private String vehicleName;

    private String category;
    private String machineryModel;
    private String machinerySubCategory;
    private String experience;
    private String mfgYear;
    private String machineClass;
    private String chassisVin;
    private String engineSerial;
    private String powertrain;

    private String status;
    private Boolean isActive;
    private String rejectionReason;

    private String fetchMode;
    private Boolean isVahanSynced;
    private String submittedOn;
    private String vehicleDescription;

    private String driverName;
    private String driverPhone;

    private String rcFrontUrl;
    private String rcBackUrl;
    private String insuranceUrl;
    private String frontPhotoUrl;
    private String sidePhotoUrl;
    private String rearPhotoUrl;
    private String cabinPhotoUrl;
    private String enginePlatePhotoUrl;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
