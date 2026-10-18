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
public class QuotationRequest {
    private String bookingType; // LOCAL_RIDE, SCHEDULED, RENTAL, OUTSTATION, HEAVY_EQUIPMENT
    private Long productId;
    private Double pickupLatitude;
    private Double pickupLongitude;
    private Double dropLatitude;
    private Double dropLongitude;
    private String pickupAddress;
    private String destinationAddress;
    private LocalDateTime scheduledPickupAt;
    private LocalDateTime returnAt;
    private Long rentalPackageId;
    private String rentalPackageName;
    private String outstationTripType; // ONE_WAY, ROUND_TRIP
    private Double distanceKm;
    private Double durationHours;
    private Double waitingMinutes;
    private Boolean operatorRequired;
}
