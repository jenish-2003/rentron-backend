package com.jcbbooking.service;

import com.jcbbooking.model.Pricing;

import java.util.Map;

public interface PricingService {
    Map<String, Object> calculatePrice(Long productId, Double distanceKm, Double durationHours, Double waitingMinutes);
    Pricing getPricingByProductId(Long productId);
    Pricing saveOrUpdatePricing(Pricing pricing);
}
