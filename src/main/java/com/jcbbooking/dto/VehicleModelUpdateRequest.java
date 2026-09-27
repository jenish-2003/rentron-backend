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
public class VehicleModelUpdateRequest {

    @NotNull(message = "Vehicle type ID is required")
    private Long vehicleTypeId;

    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must not exceed 150 characters")
    private String name;

    @Size(max = 100, message = "Code must not exceed 100 characters")
    private String code;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    private String iconUrl;

    private Integer displayOrder;

    private Boolean active;
}
