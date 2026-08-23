package com.jcbbooking.strategy;

import com.jcbbooking.model.Booking;
import com.jcbbooking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JcbAvailabilityStrategy implements ProductAvailabilityStrategy {

    private final BookingRepository bookingRepository;

    private static final List<String> SCHEDULED_STATUSES = Arrays.asList(
            "CONFIRMED", "ASSIGNED", "ACCEPTED", "STARTED", "IN_PROGRESS"
    );

    @Override
    public boolean isDriverAvailable(Long driverId, Booking newBooking) {
        if (driverId == null) return false;
        List<Booking> existingBookings = bookingRepository.findAllByDriverIdAndStatusIn(driverId, SCHEDULED_STATUSES);
        return !hasScheduleOverlap(existingBookings, newBooking);
    }

    @Override
    public boolean isContractorAvailable(Long contractorId, Booking newBooking) {
        if (contractorId == null) return false;
        List<Booking> existingBookings = bookingRepository.findAllByContractorIdAndStatusIn(contractorId, SCHEDULED_STATUSES);
        return !hasScheduleOverlap(existingBookings, newBooking);
    }

    private boolean hasScheduleOverlap(List<Booking> existingBookings, Booking newBooking) {
        if (existingBookings == null || existingBookings.isEmpty()) {
            return false;
        }

        String newDate = newBooking.getBookingDate();
        if (newDate == null || newDate.trim().isEmpty()) {
            // Default to same-day check if no date specified
            return !existingBookings.isEmpty();
        }

        for (Booking existing : existingBookings) {
            if (newDate.equalsIgnoreCase(existing.getBookingDate())) {
                // Same date: check time slot overlap
                if (isTimeOverlapping(existing.getBookingTime(), newBooking.getBookingTime(),
                        existing.getDurationHours(), newBooking.getDurationHours())) {
                    log.info("Schedule overlap detected between existing booking {} and new booking for date {}", existing.getBookingNumber(), newDate);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean isTimeOverlapping(String time1Str, String time2Str, Double duration1Hours, Double duration2Hours) {
        int start1 = parseTimeToMinutes(time1Str);
        int start2 = parseTimeToMinutes(time2Str);

        double dur1 = (duration1Hours != null && duration1Hours > 0) ? duration1Hours : 4.0; // Default 4 hrs for JCB
        double dur2 = (duration2Hours != null && duration2Hours > 0) ? duration2Hours : 4.0;

        int end1 = start1 + (int) (dur1 * 60);
        int end2 = start2 + (int) (dur2 * 60);

        // Existing Start < New End AND Existing End > New Start
        return (start1 < end2 && end1 > start2);
    }

    private int parseTimeToMinutes(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            return 9 * 60; // Default 09:00 AM if null
        }
        try {
            String clean = timeStr.trim().split("-")[0].trim();
            String[] parts = clean.split(":");
            int hours = Integer.parseInt(parts[0]);
            int minutes = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return hours * 60 + minutes;
        } catch (Exception e) {
            return 9 * 60;
        }
    }
}
