package com.jcbbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleModelResponse {
    private Long id;
    private Long vehicleTypeId;
    private String vehicleTypeName;
    private String name;
    private String code;
    private String description;
    private String iconUrl;
    private Integer displayOrder;
    private Boolean active;
    private Long createdBy;
    private Long updatedBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Optional nested sub-models for catalog
    private List<VehicleSubModelResponse> subModels;
}
