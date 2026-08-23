package com.jcbbooking.controller;

import com.jcbbooking.model.Pricing;
import com.jcbbooking.repository.PricingRepository;
import com.jcbbooking.service.PricingService;
import com.jcbbooking.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pricings")
@RequiredArgsConstructor
@Slf4j
public class PricingController {

    private final PricingRepository pricingRepository;
    private final PricingService pricingService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Pricing>>> getAllPricings() {
        log.info("REST request to get all pricing configurations");
        return ResponseEntity.ok(ApiResponse.success("Pricing configurations retrieved successfully", pricingRepository.findAll()));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ApiResponse<Pricing>> getPricingByProductId(@PathVariable Long productId) {
        log.info("REST request to get pricing by productId: {}", productId);
        try {
            Pricing pricing = pricingService.getPricingByProductId(productId);
            return ResponseEntity.ok(ApiResponse.success("Pricing configuration retrieved successfully", pricing));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<Pricing>> savePricing(@RequestBody Pricing pricing) {
        log.info("REST request to save/update pricing: {}", pricing);
        Pricing saved = pricingService.saveOrUpdatePricing(pricing);
        return ResponseEntity.ok(ApiResponse.success("Pricing configuration saved successfully", saved));
    }

    @GetMapping("/calculate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculatePriceGet(
            @RequestParam Long productId,
            @RequestParam(required = false, defaultValue = "0.0") Double distanceKm,
            @RequestParam(required = false, defaultValue = "0.0") Double durationHours,
            @RequestParam(required = false, defaultValue = "0.0") Double waitingMinutes) {
        log.info("REST request (GET) to calculate price for product ID: {}", productId);
        try {
            Map<String, Object> estimation = pricingService.calculatePrice(productId, distanceKm, durationHours, waitingMinutes);
            return ResponseEntity.ok(ApiResponse.success("Price calculation successful", estimation));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> calculatePricePost(@RequestBody Map<String, Object> req) {
        log.info("REST request (POST) to calculate price: {}", req);
        try {
            Long productId = Long.valueOf(req.get("productId").toString());
            Double distanceKm = req.containsKey("distanceKm") ? Double.valueOf(req.get("distanceKm").toString()) : 0.0;
            Double durationHours = req.containsKey("durationHours") ? Double.valueOf(req.get("durationHours").toString()) : 0.0;
            Double waitingMinutes = req.containsKey("waitingMinutes") ? Double.valueOf(req.get("waitingMinutes").toString()) : 0.0;

            Map<String, Object> estimation = pricingService.calculatePrice(productId, distanceKm, durationHours, waitingMinutes);
            return ResponseEntity.ok(ApiResponse.success("Price calculation successful", estimation));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
