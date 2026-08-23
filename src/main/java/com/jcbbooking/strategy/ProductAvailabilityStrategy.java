package com.jcbbooking.strategy;

import com.jcbbooking.model.Booking;

public interface ProductAvailabilityStrategy {
    /**
     * Checks if a candidate driver or contractor is available for the given booking
     * according to product-specific business rules.
     */
    boolean isDriverAvailable(Long driverId, Booking newBooking);
    boolean isContractorAvailable(Long contractorId, Booking newBooking);
}
