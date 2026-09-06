package com.jcbbooking.service;

import com.jcbbooking.model.*;
import com.jcbbooking.repository.*;
import com.jcbbooking.strategy.ProductAvailabilityStrategy;
import com.jcbbooking.strategy.ProductAvailabilityStrategyFactory;
import com.jcbbooking.util.HaversineDistanceUtil;
import com.jcbbooking.websocket.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingAssignmentService {

    private final BookingRepository bookingRepository;
    private final AddressRepository addressRepository;
    private final DriverRepository driverRepository;
    private final ContractorRepository contractorRepository;
    private final BookingSettingRepository bookingSettingRepository;
    private final BookingAssignmentRepository bookingAssignmentRepository;
    private final ProductAvailabilityStrategyFactory strategyFactory;
    private final WebSocketNotificationService webSocketNotificationService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    public BookingSetting getOrInitSettings() {
        return bookingSettingRepository.findBySettingKey("DEFAULT_SETTINGS")
                .orElseGet(() -> {
                    BookingSetting def = BookingSetting.builder()
                            .settingKey("DEFAULT_SETTINGS")
                            .currency("INR")
                            .assignmentType("AUTO")
                            .initialRadiusKm(5.0)
                            .radiusIncrementKm(5.0)
                            .maxRadiusKm(20.0)
                            .assignmentTimeoutSeconds(30)
                            .maxLocationAgeSeconds(300)
                            .notifyAdminWhenUnassigned(true)
                            .build();
                    return bookingSettingRepository.save(def);
                });
    }

    @Async
    public void startAutoAssignmentProcess(Long bookingId) {
        log.info("Starting Auto-Assignment engine for booking ID {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            log.error("Booking ID {} not found for auto assignment", bookingId);
            return;
        }

        BookingSetting settings = getOrInitSettings();
        if (!"AUTO".equalsIgnoreCase(settings.getAssignmentType())) {
            log.info("Assignment type is MANUAL for booking ID {}. Skipping auto-assignment.", bookingId);
            return;
        }

        double pickupLat = 0.0;
        double pickupLon = 0.0;
        if (booking.getPickupAddressId() != null) {
            Address pickupAddr = addressRepository.findById(booking.getPickupAddressId()).orElse(null);
            if (pickupAddr != null && pickupAddr.getLatitude() != null && pickupAddr.getLongitude() != null) {
                pickupLat = pickupAddr.getLatitude();
                pickupLon = pickupAddr.getLongitude();
            }
        }

        executeLayerSearch(bookingId, 1, 0.0, settings.getInitialRadiusKm(), pickupLat, pickupLon, settings);
    }

    private void executeLayerSearch(Long bookingId, int layerIndex, double minRadiusKm, double maxLayerRadiusKm,
                                    double pickupLat, double pickupLon, BookingSetting settings) {

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null || "ASSIGNED".equalsIgnoreCase(booking.getStatus()) || "ACCEPTED".equalsIgnoreCase(booking.getStatus())
                || "CANCELLED".equalsIgnoreCase(booking.getStatus()) || "COMPLETED".equalsIgnoreCase(booking.getStatus())) {
            log.info("Booking ID {} is already terminal or assigned ({})", bookingId, booking != null ? booking.getStatus() : "NULL");
            return;
        }

        log.info("Executing Layer {} Search for Booking ID {} in Radius Range [{} KM - {} KM]",
                layerIndex, bookingId, minRadiusKm, maxLayerRadiusKm);

        ProductAvailabilityStrategy strategy = strategyFactory.getStrategy(booking.getProductId());

        // Find Candidates (Drivers & Contractors)
        List<CandidateWrapper> candidates = findCandidatesInRadius(booking, pickupLat, pickupLon, minRadiusKm, maxLayerRadiusKm, settings, strategy);

        if (!candidates.isEmpty()) {
            log.info("Found {} eligible candidates in Layer {} for Booking ID {}", candidates.size(), layerIndex, bookingId);

            // Log Assignment Offers & Broadcast WebSocket Notifications
            for (CandidateWrapper cand : candidates) {
                BookingAssignment assignment = BookingAssignment.builder()
                        .bookingId(bookingId)
                        .layerIndex(layerIndex)
                        .radiusKm(maxLayerRadiusKm)
                        .candidateType(cand.type)
                        .candidateId(cand.id)
                        .candidateUserId(cand.userId)
                        .distanceKm(cand.distanceKm)
                        .status("OFFERED")
                        .build();
                bookingAssignmentRepository.save(assignment);

                Map<String, Object> offerPayload = new HashMap<>();
                offerPayload.put("type", "NEW_BOOKING");
                offerPayload.put("bookingId", bookingId);
                offerPayload.put("bookingNumber", booking.getBookingNumber());
                offerPayload.put("layerIndex", layerIndex);
                offerPayload.put("radiusKm", maxLayerRadiusKm);
                offerPayload.put("distanceKm", cand.distanceKm);
                offerPayload.put("totalAmount", booking.getTotalAmount());
                offerPayload.put("expiresInSeconds", settings.getAssignmentTimeoutSeconds());

                webSocketNotificationService.sendBookingOfferToUser(cand.userId, offerPayload);
            }

            // Schedule timeout before progressing to next layer
            int timeoutSec = settings.getAssignmentTimeoutSeconds();
            double nextMinRadius = maxLayerRadiusKm;
            double nextMaxRadius = maxLayerRadiusKm + settings.getRadiusIncrementKm();

            scheduler.schedule(() -> {
                Booking freshBooking = bookingRepository.findById(bookingId).orElse(null);
                if (freshBooking != null && "PENDING".equalsIgnoreCase(freshBooking.getStatus()) || "CONFIRMED".equalsIgnoreCase(freshBooking.getStatus())) {
                    if (nextMinRadius < settings.getMaxRadiusKm()) {
                        executeLayerSearch(bookingId, layerIndex + 1, nextMinRadius, Math.min(nextMaxRadius, settings.getMaxRadiusKm()), pickupLat, pickupLon, settings);
                    } else {
                        handleAdminFallback(freshBooking, settings);
                    }
                }
            }, timeoutSec, TimeUnit.SECONDS);

        } else {
            log.info("No eligible candidates found in Layer {} for Booking ID {}. Checking next layer...", layerIndex, bookingId);
            double nextMinRadius = maxLayerRadiusKm;
            double nextMaxRadius = maxLayerRadiusKm + settings.getRadiusIncrementKm();
            if (nextMinRadius < settings.getMaxRadiusKm()) {
                executeLayerSearch(bookingId, layerIndex + 1, nextMinRadius, Math.min(nextMaxRadius, settings.getMaxRadiusKm()), pickupLat, pickupLon, settings);
            } else {
                handleAdminFallback(booking, settings);
            }
        }
    }

    private void handleAdminFallback(Booking booking, BookingSetting settings) {
        log.warn("All Progressive Auto-Assignment Layers completed for Booking ID {} without acceptance.", booking.getId());
        if (Boolean.TRUE.equals(settings.getNotifyAdminWhenUnassigned())) {
            Map<String, Object> alert = new HashMap<>();
            alert.put("type", "BOOKING_ASSIGNMENT_FAILED");
            alert.put("bookingId", booking.getId());
            alert.put("bookingNumber", booking.getBookingNumber());
            alert.put("message", "No driver or contractor accepted the booking within maximum radius of " + settings.getMaxRadiusKm() + " KM.");
            alert.put("maxRadiusKm", settings.getMaxRadiusKm());

            webSocketNotificationService.sendAdminAssignmentFailedNotification(alert);
        }
    }

    /**
     * Atomic Acceptance Concurrency Control: First candidate to execute gets the assignment.
     */
    @Transactional
    public boolean acceptBookingOffer(Long bookingId, String candidateType, Long candidateId, Long userId) {
        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            throw new IllegalArgumentException("Booking not found");
        }

        if (!"PENDING".equalsIgnoreCase(booking.getStatus()) && !"CONFIRMED".equalsIgnoreCase(booking.getStatus())) {
            log.warn("Acceptance rejected for candidate {} (ID {}). Booking ID {} status is {}", candidateType, candidateId, bookingId, booking.getStatus());
            return false;
        }

        // Perform Atomic State Update
        if ("DRIVER".equalsIgnoreCase(candidateType)) {
            booking.setDriverId(candidateId);
            Driver driver = driverRepository.findById(candidateId).orElse(null);
            if (driver != null && driver.getContractorId() != null) {
                booking.setContractorId(driver.getContractorId());
            }
        } else if ("CONTRACTOR".equalsIgnoreCase(candidateType)) {
            booking.setContractorId(candidateId);
        }

        booking.setStatus("ASSIGNED");
        bookingRepository.save(booking);

        // Update Audit Assignment Log
        Optional<BookingAssignment> myAssignmentOpt = bookingAssignmentRepository
                .findByBookingIdAndCandidateTypeAndCandidateIdAndStatus(bookingId, candidateType, candidateId, "OFFERED");
        if (myAssignmentOpt.isPresent()) {
            BookingAssignment myAssignment = myAssignmentOpt.get();
            myAssignment.setStatus("ACCEPTED");
            myAssignment.setRespondedAt(LocalDateTime.now());
            bookingAssignmentRepository.save(myAssignment);
        }

        // Broadcast offer closure to all remaining candidate subscribers
        List<BookingAssignment> allAssignments = bookingAssignmentRepository.findAllByBookingIdOrderByOfferedAtDesc(bookingId);
        for (BookingAssignment ba : allAssignments) {
            if ("OFFERED".equalsIgnoreCase(ba.getStatus())) {
                ba.setStatus("EXPIRED");
                ba.setRespondedAt(LocalDateTime.now());
                bookingAssignmentRepository.save(ba);

                if (ba.getCandidateUserId() != null && !ba.getCandidateUserId().equals(userId)) {
                    Map<String, Object> cancelMsg = new HashMap<>();
                    cancelMsg.put("type", "BOOKING_ALREADY_ASSIGNED");
                    cancelMsg.put("bookingId", bookingId);
                    cancelMsg.put("message", "This booking has already been assigned to another candidate.");
                    webSocketNotificationService.sendOfferCancelledToUser(ba.getCandidateUserId(), cancelMsg);
                }
            }
        }

        log.info("Booking ID {} successfully ASSIGNED to {} ID {}", bookingId, candidateType, candidateId);
        return true;
    }

    private List<CandidateWrapper> findCandidatesInRadius(Booking booking, double pickupLat, double pickupLon,
                                                         double minRadiusKm, double maxRadiusKm, BookingSetting settings,
                                                         ProductAvailabilityStrategy strategy) {

        List<CandidateWrapper> candidates = new ArrayList<>();
        LocalDateTime staleThreshold = LocalDateTime.now().minusSeconds(settings.getMaxLocationAgeSeconds());

        // 1. Search Drivers
        List<Driver> activeDrivers = driverRepository.findAllByStatus("ACTIVE");
        for (Driver d : activeDrivers) {
            if (d.getLatitude() != null && d.getLongitude() != null) {
                // Location freshness check
                if (d.getLocationUpdatedAt() == null || d.getLocationUpdatedAt().isAfter(staleThreshold)) {
                    double dist = HaversineDistanceUtil.calculateDistanceKm(pickupLat, pickupLon, d.getLatitude(), d.getLongitude());
                    if (dist >= minRadiusKm && dist <= maxRadiusKm) {
                        if (strategy.isDriverAvailable(d.getId(), booking)) {
                            candidates.add(new CandidateWrapper("DRIVER", d.getId(), d.getUserId(), dist, d.getRating() != null ? d.getRating() : 4.0));
                        }
                    }
                }
            }
        }

        // 2. Search Contractors
        List<Contractor> activeContractors = contractorRepository.findAllByStatus("ACTIVE");
        for (Contractor c : activeContractors) {
            if (c.getLatitude() != null && c.getLongitude() != null) {
                if (c.getLocationUpdatedAt() == null || c.getLocationUpdatedAt().isAfter(staleThreshold)) {
                    double dist = HaversineDistanceUtil.calculateDistanceKm(pickupLat, pickupLon, c.getLatitude(), c.getLongitude());
                    if (dist >= minRadiusKm && dist <= maxRadiusKm) {
                        if (strategy.isContractorAvailable(c.getId(), booking)) {
                            candidates.add(new CandidateWrapper("CONTRACTOR", c.getId(), c.getUserId(), dist, c.getRating() != null ? c.getRating() : 4.0));
                        }
                    }
                }
            }
        }

        // Sort by Distance ASC, Rating DESC
        candidates.sort((c1, c2) -> {
            int distComp = Double.compare(c1.distanceKm, c2.distanceKm);
            if (distComp != 0) return distComp;
            return Double.compare(c2.rating, c1.rating);
        });

        return candidates;
    }

    private static class CandidateWrapper {
        String type;
        Long id;
        Long userId;
        double distanceKm;
        double rating;

        CandidateWrapper(String type, Long id, Long userId, double distanceKm, double rating) {
            this.type = type;
            this.id = id;
            this.userId = userId;
            this.distanceKm = distanceKm;
            this.rating = rating;
        }
    }
}
