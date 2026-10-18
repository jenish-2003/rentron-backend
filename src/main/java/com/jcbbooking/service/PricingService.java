package com.jcbbooking.service;

import com.jcbbooking.model.Pricing;

import java.util.Map;

public interface PricingService {
    Map<String, Object> calculatePrice(Long productId, Double distanceKm, Double durationHours, Double waitingMinutes);
    Map<String, Object> calculatePriceAdvanced(Long productId, Double distanceKm, Double durationHours, Double waitingMinutes, String bookingType, String rentalPackage, String outstationTripType);
    com.jcbbooking.dto.QuotationResponse generateAuthoritativeQuotation(com.jcbbooking.dto.QuotationRequest request);
    Pricing getPricingByProductId(Long productId);
    Pricing saveOrUpdatePricing(Pricing pricing);
}
