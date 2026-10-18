package com.jcbbooking.service.impl;

import com.jcbbooking.dto.QuotationRequest;
import com.jcbbooking.dto.QuotationResponse;
import com.jcbbooking.exception.ResourceNotFoundException;
import com.jcbbooking.model.Pricing;
import com.jcbbooking.model.Product;
import com.jcbbooking.model.RentalPackage;
import com.jcbbooking.repository.PricingRepository;
import com.jcbbooking.repository.ProductRepository;
import com.jcbbooking.repository.RentalPackageRepository;
import com.jcbbooking.service.PricingService;
import com.jcbbooking.util.HaversineDistanceUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PricingServiceImpl implements PricingService {

    private final ProductRepository productRepository;
    private final PricingRepository pricingRepository;
    private final RentalPackageRepository rentalPackageRepository;

    @Override
    public Map<String, Object> calculatePrice(Long productId, Double distanceKm, Double durationHours, Double waitingMinutes) {
        return calculatePriceAdvanced(productId, distanceKm, durationHours, waitingMinutes, null, null, null);
    }

    @Override
    public Map<String, Object> calculatePriceAdvanced(Long productId, Double distanceKm, Double durationHours, Double waitingMinutes,
                                                      String bookingType, String rentalPackage, String outstationTripType) {
        log.info("Calculating price for productId: {}, distanceKm: {}, durationHours: {}, bookingType: {}, rentalPackage: {}, outstationTripType: {}",
                productId, distanceKm, durationHours, bookingType, rentalPackage, outstationTripType);

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
        String bType = bookingType != null ? bookingType.toUpperCase() : ("HEAVY_EQUIPMENT".equals(pType) || "JCB".equals(pType) ? "HEAVY_EQUIPMENT" : "LOCAL");

        boolean isHeavyEquipment = "HEAVY_EQUIPMENT".equals(bType) || "HEAVY_EQUIPMENT".equals(pType) || "JCB".equals(pType) || "EXCAVATOR".equals(pType) || "CRANE".equals(pType);

        if ("RENTAL".equals(bType)) {
            // Model C — Dynamic Rental Packages Pricing Engine
            result.put("pricingModel", "RENTAL");

            // Look up configured package from DB if available
            RentalPackage pkgEntity = null;
            if (rentalPackage != null && !rentalPackage.trim().isEmpty()) {
                pkgEntity = rentalPackageRepository.findByNameIgnoreCase(rentalPackage.trim()).orElse(null);
            }

            double pkgHours = pkgEntity != null ? pkgEntity.getIncludedHours() : 4.0;
            double pkgKm = pkgEntity != null ? pkgEntity.getIncludedKm() : 40.0;
            double basePkgFare = pkgEntity != null ? pkgEntity.getBasePackageFare() : 1099.0;
            double extraKmRate = pkgEntity != null ? pkgEntity.getExtraKmPrice() : 11.0;
            double extraHourRate = pkgEntity != null ? pkgEntity.getExtraHourPrice() : 90.0;
            bookingFee = pkgEntity != null ? pkgEntity.getBookingFee() : (pricing.getBookingFee() != null ? pricing.getBookingFee() : 18.0);
            taxPercentage = pkgEntity != null ? pkgEntity.getTaxPercentage() : taxPercentage;

            if (pkgEntity == null && rentalPackage != null) {
                String pkgLower = rentalPackage.toLowerCase();
                if (pkgLower.contains("1 hr") || pkgLower.contains("10 km")) {
                    pkgHours = 1.0; pkgKm = 10.0; basePkgFare = 349.0;
                } else if (pkgLower.contains("2 hr") || pkgLower.contains("20 km")) {
                    pkgHours = 2.0; pkgKm = 20.0; basePkgFare = 599.0;
                } else if (pkgLower.contains("4 hr") || pkgLower.contains("40 km")) {
                    pkgHours = 4.0; pkgKm = 40.0; basePkgFare = 1099.0;
                } else if (pkgLower.contains("8 hr") || pkgLower.contains("80 km")) {
                    pkgHours = 8.0; pkgKm = 80.0; basePkgFare = 1999.0;
                }
            }

            baseAmount = Math.round(basePkgFare * 100.0) / 100.0;

            if (dist > pkgKm) {
                distanceAmount = Math.round((dist - pkgKm) * extraKmRate * 100.0) / 100.0;
            }
            double totalMins = hours * 60.0;
            if (totalMins > (pkgHours * 60.0)) {
                timeAmount = Math.round(((totalMins - (pkgHours * 60.0)) / 60.0) * extraHourRate * 100.0) / 100.0;
            }

            double subtotal = baseAmount + distanceAmount + timeAmount + bookingFee;
            double taxAmount = Math.round(subtotal * (taxPercentage / 100.0) * 100.0) / 100.0;
            double totalAmount = Math.round((subtotal + taxAmount) * 100.0) / 100.0;

            result.put("rentalPackage", rentalPackage != null ? rentalPackage : (pkgHours + " hr " + pkgKm + " km"));
            result.put("baseAmount", baseAmount);
            result.put("distanceAmount", distanceAmount);
            result.put("timeAmount", timeAmount);
            result.put("bookingFee", bookingFee);
            result.put("waitingAmount", 0.0);
            result.put("operatorAmount", 0.0);
            result.put("driverAmount", 0.0);
            result.put("extraKmRate", extraKmRate);
            result.put("extraHourRate", extraHourRate);
            result.put("packageHours", pkgHours);
            result.put("packageKm", pkgKm);
            result.put("subtotal", subtotal);
            result.put("taxAmount", taxAmount);
            result.put("totalAmount", totalAmount);

        } else if ("OUTSTATION".equals(bType)) {
            // Model D — Outstation (One-Way & Round-Trip) Dynamic Pricing Engine
            result.put("pricingModel", "OUTSTATION");
            boolean isRoundTrip = "ROUND_TRIP".equalsIgnoreCase(outstationTripType);
            result.put("outstationTripType", isRoundTrip ? "ROUND_TRIP" : "ONE_WAY");

            double perKmRate = pricing.getPerKmPrice() != null && pricing.getPerKmPrice() > 0 ? pricing.getPerKmPrice() : 13.0;
            String prodName = product.getName() != null ? product.getName().toUpperCase() : "";
            if (prodName.contains("SEDAN")) perKmRate = Math.max(perKmRate, 14.5);
            if (prodName.contains("SUV")) perKmRate = Math.max(perKmRate, 17.0);

            double minOneWay = pricing.getMinOutstationKmOneWay() != null ? pricing.getMinOutstationKmOneWay() : 50.0;
            double minRoundTrip = pricing.getMinOutstationKmRoundTrip() != null ? pricing.getMinOutstationKmRoundTrip() : 250.0;

            double effectiveKm = isRoundTrip ? Math.max(dist * 2.0, minRoundTrip) : Math.max(dist, minOneWay);
            baseAmount = Math.round(effectiveKm * perKmRate * 100.0) / 100.0;

            // Extra KM after 800km discount if applicable
            double extraKmAllowance = 0.0;
            if (effectiveKm > 800.0) {
                extraKmAllowance = Math.round((effectiveKm - 800.0) * (perKmRate * 0.9) * 100.0) / 100.0;
            }
            distanceAmount = extraKmAllowance;

            int days = Math.max(1, (int) Math.ceil(hours > 0 ? hours / 24.0 : 1.0));
            double dailyDriverAllowance = pricing.getDriverCharge() != null && pricing.getDriverCharge() > 0 ? pricing.getDriverCharge() : 400.0;
            driverAmount = dailyDriverAllowance * days;

            double nightAllowanceRate = pricing.getNightAllowance() != null && pricing.getNightAllowance() > 0 ? pricing.getNightAllowance() : 300.0;
            waitingAmount = isRoundTrip ? (nightAllowanceRate * Math.max(1, days)) : (hours > 6.0 ? nightAllowanceRate : 0.0);

            bookingFee = pricing.getBookingFee() != null && pricing.getBookingFee() > 0 ? pricing.getBookingFee() : 18.0;
            double subtotal = baseAmount + distanceAmount + driverAmount + waitingAmount + bookingFee;
            double taxAmount = Math.round(subtotal * (taxPercentage / 100.0) * 100.0) / 100.0;
            double totalAmount = Math.round((subtotal + taxAmount) * 100.0) / 100.0;

            result.put("baseAmount", baseAmount);
            result.put("distanceAmount", distanceAmount);
            result.put("timeAmount", 0.0);
            result.put("driverAmount", driverAmount);
            result.put("waitingAmount", waitingAmount);
            result.put("bookingFee", bookingFee);
            result.put("effectiveKm", effectiveKm);
            result.put("perKmRate", perKmRate);
            result.put("driverDailyAllowance", dailyDriverAllowance);
            result.put("nightTimeAllowance", waitingAmount);
            result.put("subtotal", subtotal);
            result.put("taxAmount", taxAmount);
            result.put("totalAmount", totalAmount);

        } else if (isHeavyEquipment) {
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

            double rawSubtotal = Math.max(baseAmount + timeAmount + distanceAmount + operatorAmount + driverAmount + waitingAmount - discount, 0.0);
            double subtotal = Math.round(rawSubtotal * 100.0) / 100.0;
            double taxAmount = Math.round(subtotal * (taxPercentage / 100.0) * 100.0) / 100.0;
            double totalAmount = Math.round((subtotal + taxAmount) * 100.0) / 100.0;

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
            // Model B — Ride (City / Local / Scheduled) Pricing Engine
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
            double subtotal = Math.round(Math.max(rawSubtotal, minFare) * 100.0) / 100.0;
            double taxAmount = Math.round(subtotal * (taxPercentage / 100.0) * 100.0) / 100.0;
            double totalAmount = Math.round((subtotal + taxAmount) * 100.0) / 100.0;

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
    public QuotationResponse generateAuthoritativeQuotation(QuotationRequest request) {
        log.info("Generating authoritative quotation for product: {}, type: {}", request.getProductId(), request.getBookingType());

        if (request.getProductId() == null) {
            throw new IllegalArgumentException("Product ID is required for quotation");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + request.getProductId()));

        // Validate Coordinates if provided
        if (request.getPickupLatitude() != null && request.getPickupLongitude() != null) {
            if (request.getPickupLatitude() < -90.0 || request.getPickupLatitude() > 90.0
                    || request.getPickupLongitude() < -180.0 || request.getPickupLongitude() > 180.0) {
                throw new IllegalArgumentException("Invalid pickup GPS coordinates");
            }
        }
        if (request.getDropLatitude() != null && request.getDropLongitude() != null) {
            if (request.getDropLatitude() < -90.0 || request.getDropLatitude() > 90.0
                    || request.getDropLongitude() < -180.0 || request.getDropLongitude() > 180.0) {
                throw new IllegalArgumentException("Invalid drop GPS coordinates");
            }
        }

        // Distance Estimation
        double calculatedDist = 0.0;
        if (request.getPickupLatitude() != null && request.getPickupLongitude() != null
                && request.getDropLatitude() != null && request.getDropLongitude() != null) {
            double straightKm = HaversineDistanceUtil.calculateDistanceKm(
                    request.getPickupLatitude(), request.getPickupLongitude(),
                    request.getDropLatitude(), request.getDropLongitude());
            calculatedDist = Math.round(straightKm * 1.25 * 100.0) / 100.0; // 1.25 road curvature routing factor
        }

        if (calculatedDist <= 0.0 && request.getDistanceKm() != null && request.getDistanceKm() > 0.0) {
            calculatedDist = request.getDistanceKm();
        }

        String rawBType = request.getBookingType() != null ? request.getBookingType().trim().toUpperCase() : "LOCAL";
        String normalizedType = rawBType;
        if ("LOCAL_RIDE".equals(rawBType)) normalizedType = "LOCAL";

        // Duration Estimation
        double durationHours = request.getDurationHours() != null && request.getDurationHours() > 0 ? request.getDurationHours() : 0.0;
        if (durationHours <= 0.0) {
            if ("OUTSTATION".equals(normalizedType)) {
                durationHours = Math.max(2.0, Math.round((calculatedDist / 50.0) * 10.0) / 10.0);
            } else if ("RENTAL".equals(normalizedType)) {
                durationHours = 4.0;
            } else if ("HEAVY_EQUIPMENT".equals(normalizedType)) {
                durationHours = 4.0;
            } else {
                durationHours = Math.max(0.2, Math.round((calculatedDist / 25.0) * 10.0) / 10.0);
            }
        }

        // Validation for Scheduled & Outstation Return
        if ("SCHEDULED".equals(normalizedType)) {
            if (request.getScheduledPickupAt() != null && request.getScheduledPickupAt().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Scheduled pickup cannot be in the past");
            }
        }
        if ("OUTSTATION".equals(normalizedType) && "ROUND_TRIP".equalsIgnoreCase(request.getOutstationTripType())) {
            if (request.getReturnAt() != null && request.getScheduledPickupAt() != null
                    && request.getReturnAt().isBefore(request.getScheduledPickupAt())) {
                throw new IllegalArgumentException("Return time cannot be before scheduled pickup time for round trip");
            }
        }

        String rentalPkgName = request.getRentalPackageName();
        if (request.getRentalPackageId() != null) {
            rentalPackageRepository.findById(request.getRentalPackageId()).ifPresent(p -> {
                // use package name
            });
            RentalPackage pkg = rentalPackageRepository.findById(request.getRentalPackageId()).orElse(null);
            if (pkg != null) {
                rentalPkgName = pkg.getName();
                if (request.getDurationHours() == null || request.getDurationHours() <= 0) {
                    durationHours = pkg.getIncludedHours();
                }
                if (calculatedDist <= 0.0) {
                    calculatedDist = pkg.getIncludedKm();
                }
            }
        }

        Map<String, Object> breakdown = calculatePriceAdvanced(
                request.getProductId(),
                calculatedDist,
                durationHours,
                request.getWaitingMinutes() != null ? request.getWaitingMinutes() : 0.0,
                normalizedType,
                rentalPkgName,
                request.getOutstationTripType()
        );

        double totalFare = (Double) breakdown.getOrDefault("totalAmount", 0.0);

        return QuotationResponse.builder()
                .quotationId("QUOTE-" + UUID.randomUUID().toString())
                .quotationExpiry(LocalDateTime.now().plusMinutes(15))
                .productId(product.getId())
                .productName(product.getName())
                .productType(product.getProductType())
                .vehicleCategory(product.getCategory())
                .bookingType(rawBType)
                .estimatedDistanceKm(calculatedDist)
                .estimatedDurationHours(durationHours)
                .estimatedDurationMinutes(durationHours * 60.0)
                .totalEstimatedFare(totalFare)
                .currency("INR")
                .pricingVersion("v1.2-authoritative")
                .itemizedFareBreakdown(breakdown)
                .build();
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
            if (pricing.getNightAllowance() != null) existing.setNightAllowance(pricing.getNightAllowance());
            if (pricing.getNightAllowanceStartHour() != null) existing.setNightAllowanceStartHour(pricing.getNightAllowanceStartHour());
            if (pricing.getNightAllowanceEndHour() != null) existing.setNightAllowanceEndHour(pricing.getNightAllowanceEndHour());
            if (pricing.getMinOutstationKmOneWay() != null) existing.setMinOutstationKmOneWay(pricing.getMinOutstationKmOneWay());
            if (pricing.getMinOutstationKmRoundTrip() != null) existing.setMinOutstationKmRoundTrip(pricing.getMinOutstationKmRoundTrip());
            if (pricing.getActive() != null) {
                existing.setActive(pricing.getActive());
            }
            return pricingRepository.save(existing);
        }
        return pricingRepository.save(pricing);
    }
}
