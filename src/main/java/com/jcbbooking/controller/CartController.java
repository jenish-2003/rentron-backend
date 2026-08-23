package com.jcbbooking.controller;

import com.jcbbooking.model.Cart;
import com.jcbbooking.repository.CartRepository;
import com.jcbbooking.security.CustomUserDetails;
import com.jcbbooking.service.PricingService;
import com.jcbbooking.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartRepository cartRepository;
    private final PricingService pricingService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCart(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to get cart for customer ID: {}", userDetails.getId());
        List<Cart> items = cartRepository.findAllByCustomerId(userDetails.getId());
        
        double cartTotal = 0.0;
        for (Cart item : items) {
            if (item.getProductId() != null) {
                try {
                    Map<String, Object> calc = pricingService.calculatePrice(
                            item.getProductId(),
                            item.getDistanceKm(),
                            item.getDurationHours(),
                            0.0
                    );
                    Double total = (Double) calc.get("totalAmount");
                    if (total != null) {
                        item.setEstimatedAmount(total);
                        cartTotal += total;
                    }
                } catch (Exception e) {
                    log.warn("Price calculation error for cart item {}: {}", item.getId(), e.getMessage());
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("items", items);
        result.put("cartTotal", cartTotal);
        return ResponseEntity.ok(ApiResponse.success("Cart retrieved successfully", result));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<Cart>> addToCart(
            @RequestBody Cart cart,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to add/update cart item for customer ID: {}", userDetails.getId());

        cart.setCustomerId(userDetails.getId());
        if (cart.getProductId() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Product ID is required for cart item"));
        }

        // Authoritative backend price calculation for cart item estimation
        try {
            Map<String, Object> calc = pricingService.calculatePrice(
                    cart.getProductId(),
                    cart.getDistanceKm(),
                    cart.getDurationHours(),
                    0.0
            );
            Double total = (Double) calc.get("totalAmount");
            if (total != null) {
                cart.setEstimatedAmount(total);
            }
        } catch (Exception e) {
            log.warn("Could not calculate price for cart item: {}", e.getMessage());
        }

        Cart saved = cartRepository.save(cart);
        return ResponseEntity.ok(ApiResponse.success("Item added to cart successfully", saved));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> removeFromCart(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to remove item ID {} from cart for customer ID {}", id, userDetails.getId());
        Cart cart = cartRepository.findByIdAndCustomerId(id, userDetails.getId()).orElse(null);
        if (cart == null) {
            return ResponseEntity.status(403).body(ApiResponse.error("Cart item not found or unauthorized access"));
        }
        cartRepository.delete(cart);
        return ResponseEntity.ok(ApiResponse.success("Item removed from cart"));
    }

    @DeleteMapping
    @Transactional
    public ResponseEntity<ApiResponse<Void>> clearCart(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to clear cart for customer ID: {}", userDetails.getId());
        cartRepository.deleteAllByCustomerId(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully"));
    }
}
