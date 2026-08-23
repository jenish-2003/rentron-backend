package com.jcbbooking.strategy;

import com.jcbbooking.model.Product;
import com.jcbbooking.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProductAvailabilityStrategyFactory {

    private final ProductRepository productRepository;
    private final RideAvailabilityStrategy rideAvailabilityStrategy;
    private final JcbAvailabilityStrategy jcbAvailabilityStrategy;

    public ProductAvailabilityStrategy getStrategy(Long productId) {
        if (productId == null) {
            return jcbAvailabilityStrategy;
        }

        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return jcbAvailabilityStrategy;
        }

        String productType = product.getProductType();
        if (productType == null) {
            productType = product.getCategory();
        }

        if (productType != null) {
            String typeUpper = productType.toUpperCase();
            if (typeUpper.contains("RIDE") || typeUpper.contains("UBER") || typeUpper.contains("RAPIDO")
                    || typeUpper.contains("BIKE") || typeUpper.contains("AUTO") || typeUpper.contains("CAR")
                    || typeUpper.contains("SEDAN") || typeUpper.contains("SUV") || typeUpper.contains("TAXI")) {
                return rideAvailabilityStrategy;
            }
        }

        return jcbAvailabilityStrategy;
    }
}
