package com.jcbbooking.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleRegistrationRequest {

    private Long id;

    private Long driverId;

    private Long userId;

    private Long vehicleTypeId;

    private Long vehicleModelId;

    private Long vehicleSubModelId;

    @NotBlank(message = "Registration number is required")
    private String regNumber;

    private String vehicleName;

    // Snapshot legacy fields
    private String category;

    private String machineryModel;

    private String machinerySubCategory;

    private String experience;

    private String mfgYear;

    private String machineClass;

    private String chassisVin;

    private String engineSerial;

    private String powertrain;

    private String vehicleDescription;

    private String fetchMode;

    private Boolean isVahanSynced;

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
}
