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
public class VehicleSubModelResponse {
    private Long id;
    private Long vehicleModelId;
    private String vehicleModelName;
    private Long vehicleTypeId;
    private String vehicleTypeName;
    private String name;
    private String code;
    private String description;
    private String manufacturer;
    private String machineClass;
    private String horsePower;
    private String capacity;
    private String fuelType;
    private String iconUrl;
    private Integer displayOrder;
    private Boolean active;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
