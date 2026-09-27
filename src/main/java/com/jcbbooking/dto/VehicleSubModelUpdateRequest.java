package com.jcbbooking.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VehicleSubModelUpdateRequest {

    @NotNull(message = "Vehicle model ID is required")
    private Long vehicleModelId;

    @NotBlank(message = "Sub model name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String name;

    @Size(max = 100, message = "Code must not exceed 100 characters")
    private String code;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 150, message = "Manufacturer must not exceed 150 characters")
    private String manufacturer;

    @Size(max = 150, message = "Machine class must not exceed 150 characters")
    private String machineClass;

    @Size(max = 50, message = "Horse power must not exceed 50 characters")
    private String horsePower;

    @Size(max = 100, message = "Capacity must not exceed 100 characters")
    private String capacity;

    @Size(max = 50, message = "Fuel type must not exceed 50 characters")
    private String fuelType;

    private String iconUrl;

    private Integer displayOrder;

    private Boolean active;
}
