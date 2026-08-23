package com.jcbbooking.service.impl;

import com.jcbbooking.exception.ResourceNotFoundException;
import com.jcbbooking.model.Pricing;
import com.jcbbooking.model.Product;
import com.jcbbooking.repository.PricingRepository;
import com.jcbbooking.repository.ProductRepository;
import com.jcbbooking.service.PricingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingServiceImpl implements PricingService {

    private final ProductRepository productRepository;
    private final PricingRepository pricingRepository;

    @Override
    public Map<String, Object> calculatePrice(Long productId, Double distanceKm, Double durationHours, Double waitingMinutes) {
        log.info("Calculating price for productId: {}, distanceKm: {}, durationHours: {}, waitingMinutes: {}",
                productId, distanceKm, durationHours, waitingMinutes);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));

        Pricing pricing = pricingRepository.findByProductIdAndActiveTrue(productId)
                .orElseGet(() -> pricingRepository.findByProductId(productId)
                        .orElseThrow(() -> new ResourceNotFoundException("Pricing configuration not found for product: " + productId)));

        double dist = (distanceKm != null && distanceKm > 0) ? distanceKm : 0.0;
        double hours = (durationHours != null && durationHours > 0) ? durationHours : 0.0;
        double waitMins = (waitingMinutes != null && waitingMinutes > 0) ? waitingMinutes : 0.0;

        Map<String, Object> result = new HashMap<>();
        result.put("productId", product.getId());
        result.put("productName", product.getName());
        result.put("productType", product.getProductType());

        double baseAmount = 0.0;
        double distanceAmount = 0.0;
        double timeAmount = 0.0;
        double waitingAmount = 0.0;
        double driverAmount = 0.0;
        double operatorAmount = 0.0;
        double bookingFee = 0.0;
        double surgeMultiplier = pricing.getSurgeMultiplier() != null ? pricing.getSurgeMultiplier() : 1.0;
        double taxPercentage = pricing.getTaxPercentage() != null ? pricing.getTaxPercentage() : 5.0;

        String pType = product.getProductType() != null ? product.getProductType().toUpperCase() : "RIDE";
        boolean isHeavyEquipment = "HEAVY_EQUIPMENT".equals(pType) || "JCB".equals(pType) || "EXCAVATOR".equals(pType) || "CRANE".equals(pType);

        if (isHeavyEquipment) {
            // Model A — Heavy Equipment (JCB/Excavator) Pricing Engine
            result.put("pricingModel", "HEAVY_EQUIPMENT");
            baseAmount = pricing.getBasePrice() != null ? pricing.getBasePrice() : 0.0;
            
            int minHours = pricing.getMinimumHours() != null ? pricing.getMinimumHours() : 0;
            double effectiveHours = Math.max(hours, minHours);
            timeAmount = effectiveHours * (pricing.getPerHourPrice() != null ? pricing.getPerHourPrice() : 0.0);
            
            if (hours > minHours && pricing.getExtraHourRate() != null && pricing.getExtraHourRate() > 0) {
                double extraHours = hours - minHours;
                timeAmount += extraHours * pricing.getExtraHourRate();
            }

            distanceAmount = dist * (pricing.getPerKmPrice() != null ? pricing.getPerKmPrice() : 0.0);
            operatorAmount = pricing.getOperatorCharge() != null ? pricing.getOperatorCharge() : 0.0;
            driverAmount = pricing.getDriverCharge() != null ? pricing.getDriverCharge() : 0.0;
            waitingAmount = (waitMins / 60.0) * (pricing.getWaitingCharge() != null ? pricing.getWaitingCharge() : 0.0);
            double discount = pricing.getDiscount() != null ? pricing.getDiscount() : 0.0;

            double subtotal = Math.max(baseAmount + timeAmount + distanceAmount + operatorAmount + driverAmount + waitingAmount - discount, 0.0);
            double taxAmount = subtotal * (taxPercentage / 100.0);
            double totalAmount = subtotal + taxAmount;

            result.put("baseAmount", baseAmount);
            result.put("timeAmount", timeAmount);
            result.put("distanceAmount", distanceAmount);
            result.put("operatorAmount", operatorAmount);
            result.put("driverAmount", driverAmount);
            result.put("waitingAmount", waitingAmount);
            result.put("bookingFee", 0.0);
            result.put("surgeMultiplier", 1.0);
            result.put("discountAmount", discount);
            result.put("subtotal", subtotal);
            result.put("taxAmount", taxAmount);
            result.put("totalAmount", totalAmount);
        } else {
            // Model B — Ride (Uber/Rapido) Pricing Engine
            result.put("pricingModel", "RIDE");
            baseAmount = pricing.getBasePrice() != null ? pricing.getBasePrice() : 0.0;
            distanceAmount = dist * (pricing.getPerKmPrice() != null ? pricing.getPerKmPrice() : 0.0);
            
            double durationMins = hours * 60.0;
            timeAmount = durationMins * (pricing.getPerMinutePrice() != null ? pricing.getPerMinutePrice() : 0.0);
            bookingFee = pricing.getBookingFee() != null ? pricing.getBookingFee() : 0.0;
            waitingAmount = waitMins * ((pricing.getWaitingCharge() != null ? pricing.getWaitingCharge() : 0.0) / 60.0);
            double discount = pricing.getDiscount() != null ? pricing.getDiscount() : 0.0;

            double rawSubtotal = (baseAmount + distanceAmount + timeAmount + bookingFee + waitingAmount) * surgeMultiplier - discount;
            double minFare = pricing.getMinimumFare() != null ? pricing.getMinimumFare() : 0.0;
            double subtotal = Math.max(rawSubtotal, minFare);
            double taxAmount = subtotal * (taxPercentage / 100.0);
            double totalAmount = subtotal + taxAmount;

            result.put("baseAmount", baseAmount);
            result.put("distanceAmount", distanceAmount);
            result.put("timeAmount", timeAmount);
            result.put("bookingFee", bookingFee);
            result.put("waitingAmount", waitingAmount);
            result.put("surgeMultiplier", surgeMultiplier);
            result.put("discountAmount", discount);
            result.put("operatorAmount", 0.0);
            result.put("driverAmount", 0.0);
            result.put("subtotal", subtotal);
            result.put("taxAmount", taxAmount);
            result.put("totalAmount", totalAmount);
        }

        return result;
    }

    @Override
    public Pricing getPricingByProductId(Long productId) {
        return pricingRepository.findByProductIdAndActiveTrue(productId)
                .orElseGet(() -> pricingRepository.findByProductId(productId)
                        .orElseThrow(() -> new ResourceNotFoundException("Pricing configuration not found for product id: " + productId)));
    }

    @Override
    public Pricing saveOrUpdatePricing(Pricing pricing) {
        Pricing existing = null;
        if (pricing.getId() != null) {
            existing = pricingRepository.findById(pricing.getId()).orElse(null);
        }
        if (existing == null && pricing.getProductId() != null) {
            existing = pricingRepository.findByProductId(pricing.getProductId()).orElse(null);
        }

        if (existing != null) {
            existing.setPricingModel(pricing.getPricingModel());
            existing.setChargingMethod(pricing.getChargingMethod());
            existing.setBasePrice(pricing.getBasePrice() != null ? pricing.getBasePrice() : 0.0);
            existing.setPerKmPrice(pricing.getPerKmPrice() != null ? pricing.getPerKmPrice() : 0.0);
            existing.setPerMinutePrice(pricing.getPerMinutePrice() != null ? pricing.getPerMinutePrice() : 0.0);
            existing.setPerHourPrice(pricing.getPerHourPrice() != null ? pricing.getPerHourPrice() : 0.0);
            existing.setDailyRate(pricing.getDailyRate() != null ? pricing.getDailyRate() : 0.0);
            existing.setExtraHourRate(pricing.getExtraHourRate() != null ? pricing.getExtraHourRate() : 0.0);
            existing.setDiscount(pricing.getDiscount() != null ? pricing.getDiscount() : 0.0);
            existing.setMinimumFare(pricing.getMinimumFare() != null ? pricing.getMinimumFare() : 0.0);
            existing.setMinimumHours(pricing.getMinimumHours() != null ? pricing.getMinimumHours() : 0);
            existing.setWaitingCharge(pricing.getWaitingCharge() != null ? pricing.getWaitingCharge() : 0.0);
            existing.setDriverCharge(pricing.getDriverCharge() != null ? pricing.getDriverCharge() : 0.0);
            existing.setOperatorCharge(pricing.getOperatorCharge() != null ? pricing.getOperatorCharge() : 0.0);
            existing.setBookingFee(pricing.getBookingFee() != null ? pricing.getBookingFee() : 0.0);
            existing.setCancellationFee(pricing.getCancellationFee() != null ? pricing.getCancellationFee() : 0.0);
            existing.setSurgeMultiplier(pricing.getSurgeMultiplier() != null ? pricing.getSurgeMultiplier() : 1.0);
            existing.setTaxPercentage(pricing.getTaxPercentage() != null ? pricing.getTaxPercentage() : 5.0);
            if (pricing.getActive() != null) {
                existing.setActive(pricing.getActive());
            }
            return pricingRepository.save(existing);
        }
        return pricingRepository.save(pricing);
    }
}
