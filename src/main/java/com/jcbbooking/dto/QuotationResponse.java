package com.jcbbooking.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotationResponse {
    private String quotationId;
    private LocalDateTime quotationExpiry;
    private Long productId;
    private String productName;
    private String productType;
    private String vehicleCategory;
    private String bookingType;
    private Double estimatedDistanceKm;
    private Double estimatedDurationMinutes;
    private Double estimatedDurationHours;
    private Double totalEstimatedFare;
    private String currency;
    private String pricingVersion;
    private Map<String, Object> itemizedFareBreakdown;
}
