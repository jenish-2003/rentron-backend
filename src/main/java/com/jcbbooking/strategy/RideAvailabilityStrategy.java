package com.jcbbooking.strategy;

import com.jcbbooking.model.Booking;
import com.jcbbooking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RideAvailabilityStrategy implements ProductAvailabilityStrategy {

    private final BookingRepository bookingRepository;

    private static final List<String> ACTIVE_RIDE_STATUSES = Arrays.asList(
            "ACCEPTED", "DRIVER_ON_THE_WAY", "STARTED", "IN_PROGRESS"
    );

    @Override
    public boolean isDriverAvailable(Long driverId, Booking newBooking) {
        if (driverId == null) return false;
        List<Booking> activeBookings = bookingRepository.findAllByDriverIdAndStatusIn(driverId, ACTIVE_RIDE_STATUSES);
        return activeBookings.isEmpty();
    }

    @Override
    public boolean isContractorAvailable(Long contractorId, Booking newBooking) {
        if (contractorId == null) return false;
        List<Booking> activeBookings = bookingRepository.findAllByContractorIdAndStatusIn(contractorId, ACTIVE_RIDE_STATUSES);
        return activeBookings.isEmpty();
    }
}
