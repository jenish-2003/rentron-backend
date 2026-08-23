package com.jcbbooking.controller;

import com.jcbbooking.exception.ResourceNotFoundException;
import com.jcbbooking.model.*;
import com.jcbbooking.repository.BookingRepository;
import com.jcbbooking.repository.CartRepository;
import com.jcbbooking.repository.ContractorRepository;
import com.jcbbooking.repository.DriverRepository;
import com.jcbbooking.security.CustomUserDetails;
import com.jcbbooking.service.PricingService;
import com.jcbbooking.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
@Slf4j
public class BookingController {

    private final BookingRepository bookingRepository;
    private final CartRepository cartRepository;
    private final ContractorRepository contractorRepository;
    private final DriverRepository driverRepository;
    private final PricingService pricingService;
    private final com.jcbbooking.service.BookingAssignmentService bookingAssignmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Booking>>> getBookings(@AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to get bookings for user ID: {}", userDetails.getId());
        Role role = userDetails.getUser().getRole();

        if (role == Role.ADMIN) {
            return ResponseEntity.ok(ApiResponse.success("Bookings retrieved successfully", bookingRepository.findAll()));
        } else if (role == Role.CONTRACTOR) {
            Optional<Contractor> conOpt = contractorRepository.findByUserId(userDetails.getId());
            if (conOpt.isEmpty()) {
                conOpt = contractorRepository.findByPhone(userDetails.getUser().getPhone());
            }
            if (conOpt.isPresent()) {
                List<Booking> contractorBookings = bookingRepository.findAllByContractorId(conOpt.get().getId());
                return ResponseEntity.ok(ApiResponse.success("Contractor bookings retrieved successfully", contractorBookings));
            }
            return ResponseEntity.ok(ApiResponse.success("No bookings found", List.of()));
        } else if (role == Role.DRIVER) {
            Optional<Driver> drvOpt = driverRepository.findByUserId(userDetails.getId());
            if (drvOpt.isEmpty()) {
                drvOpt = driverRepository.findByPhone(userDetails.getUser().getPhone());
            }
            if (drvOpt.isPresent()) {
                List<Booking> driverBookings = bookingRepository.findAllByDriverId(drvOpt.get().getId());
                return ResponseEntity.ok(ApiResponse.success("Driver bookings retrieved successfully", driverBookings));
            }
            return ResponseEntity.ok(ApiResponse.success("No bookings found", List.of()));
        } else {
            // CUSTOMER
            List<Booking> customerBookings = bookingRepository.findAllByCustomerId(userDetails.getId());
            return ResponseEntity.ok(ApiResponse.success("Customer bookings retrieved successfully", customerBookings));
        }
    }

    @GetMapping("/contractor/{contractorId}")
    public ResponseEntity<ApiResponse<List<Booking>>> getBookingsByContractorId(@PathVariable Long contractorId) {
        log.info("REST request to get bookings for contractor ID: {}", contractorId);
        List<Booking> list = bookingRepository.findAllByContractorId(contractorId);
        return ResponseEntity.ok(ApiResponse.success("Contractor bookings retrieved successfully", list));
    }

    @GetMapping("/driver/{driverId}")
    public ResponseEntity<ApiResponse<List<Booking>>> getBookingsByDriverId(@PathVariable Long driverId) {
        log.info("REST request to get bookings for driver ID: {}", driverId);
        List<Booking> list = bookingRepository.findAllByDriverId(driverId);
        return ResponseEntity.ok(ApiResponse.success("Driver bookings retrieved successfully", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Booking>> getBookingById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to get booking ID {} for user ID {}", id, userDetails.getId());
        Booking booking = bookingRepository.findById(id).orElse(null);
        if (booking == null) {
            return ResponseEntity.notFound().build();
        }

        // Ownership validation
        Role role = userDetails.getUser().getRole();
        if (role == Role.CUSTOMER && !booking.getCustomerId().equals(userDetails.getId())) {
            return ResponseEntity.status(403).body(ApiResponse.error("Unauthorized access to booking"));
        } else if (role == Role.DRIVER) {
            Optional<Driver> drvOpt = driverRepository.findByUserId(userDetails.getId());
            if (drvOpt.isEmpty() || !booking.getDriverId().equals(drvOpt.get().getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Unauthorized access to booking"));
            }
        } else if (role == Role.CONTRACTOR) {
            Optional<Contractor> conOpt = contractorRepository.findByUserId(userDetails.getId());
            if (conOpt.isEmpty() || !booking.getContractorId().equals(conOpt.get().getId())) {
                return ResponseEntity.status(403).body(ApiResponse.error("Unauthorized access to booking"));
            }
        }

        return ResponseEntity.ok(ApiResponse.success("Booking retrieved successfully", booking));
    }

    @PostMapping("/confirm")
    @Transactional
    public ResponseEntity<ApiResponse<Booking>> confirmBooking(
            @RequestBody Booking bookingRequest,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to confirm booking for customer ID: {}", userDetails.getId());

        if (bookingRequest.getProductId() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Product ID is required for booking confirmation"));
        }

        Long customerId = userDetails.getId();
        bookingRequest.setCustomerId(customerId);

        // Authoritative Backend Price Calculation & Price Snapshot
        Map<String, Object> pricingCalc = pricingService.calculatePrice(
                bookingRequest.getProductId(),
                bookingRequest.getDistanceKm(),
                bookingRequest.getDurationHours(),
                0.0
        );

        bookingRequest.setBaseAmount((Double) pricingCalc.getOrDefault("baseAmount", 0.0));
        bookingRequest.setDistanceAmount((Double) pricingCalc.getOrDefault("distanceAmount", 0.0));
        bookingRequest.setTimeAmount((Double) pricingCalc.getOrDefault("timeAmount", 0.0));
        bookingRequest.setWaitingAmount((Double) pricingCalc.getOrDefault("waitingAmount", 0.0));
        bookingRequest.setDriverAmount((Double) pricingCalc.getOrDefault("driverAmount", 0.0));
        bookingRequest.setOperatorAmount((Double) pricingCalc.getOrDefault("operatorAmount", 0.0));
        bookingRequest.setBookingFee((Double) pricingCalc.getOrDefault("bookingFee", 0.0));
        bookingRequest.setTaxAmount((Double) pricingCalc.getOrDefault("taxAmount", 0.0));
        bookingRequest.setTotalAmount((Double) pricingCalc.getOrDefault("totalAmount", 0.0));

        if (bookingRequest.getBookingNumber() == null || bookingRequest.getBookingNumber().trim().isEmpty()) {
            bookingRequest.setBookingNumber("BK-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase());
        }

        if (bookingRequest.getStatus() == null) {
            bookingRequest.setStatus("CONFIRMED");
        }
        if (bookingRequest.getPaymentStatus() == null) {
            bookingRequest.setPaymentStatus("PENDING");
        }

        Booking savedBooking = bookingRepository.save(bookingRequest);

        // Atomically clear customer cart after confirmation
        cartRepository.deleteAllByCustomerId(customerId);

        // Trigger Progressive Auto-Assignment Engine
        bookingAssignmentService.startAutoAssignmentProcess(savedBooking.getId());

        log.info("Booking confirmed successfully. Booking Number: {}", savedBooking.getBookingNumber());
        return ResponseEntity.ok(ApiResponse.success("Booking confirmed successfully", savedBooking));
    }

    @PostMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<String>> acceptBookingOffer(
            @PathVariable Long id,
            @RequestParam String candidateType,
            @RequestParam Long candidateId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request by {} ID {} to accept booking offer for booking ID {}", candidateType, candidateId, id);
        boolean success = bookingAssignmentService.acceptBookingOffer(id, candidateType, candidateId, userDetails.getId());
        if (success) {
            return ResponseEntity.ok(ApiResponse.success("Booking offer accepted successfully", "ASSIGNED"));
        } else {
            return ResponseEntity.status(409).body(ApiResponse.error("BOOKING_ALREADY_ASSIGNED: This booking has already been assigned to another candidate"));
        }
    }

    @PostMapping("/{id}/assign-driver")
    @Transactional
    public ResponseEntity<ApiResponse<Booking>> assignDriver(
            @PathVariable Long id,
            @RequestParam(required = false) Long contractorId,
            @RequestParam(required = false) Long driverId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to assign contractor/driver to booking ID: {}", id);

        Booking booking = bookingRepository.findById(id).orElse(null);
        if (booking == null) {
            return ResponseEntity.notFound().build();
        }

        if (contractorId != null) {
            if (!contractorRepository.existsById(contractorId)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Contractor not found with id: " + contractorId));
            }
            booking.setContractorId(contractorId);
        }

        if (driverId != null) {
            if (!driverRepository.existsById(driverId)) {
                return ResponseEntity.badRequest().body(ApiResponse.error("Driver not found with id: " + driverId));
            }
            booking.setDriverId(driverId);
            booking.setStatus("ASSIGNED");
        }

        Booking saved = bookingRepository.save(booking);
        return ResponseEntity.ok(ApiResponse.success("Driver assigned to booking successfully", saved));
    }

    @PostMapping("/{id}/status")
    @Transactional
    public ResponseEntity<ApiResponse<Booking>> updateBookingStatus(
            @PathVariable Long id,
            @RequestParam String newStatus,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("REST request to update status of booking ID {} to {}", id, newStatus);

        Booking booking = bookingRepository.findById(id).orElse(null);
        if (booking == null) {
            return ResponseEntity.notFound().build();
        }

        String currentStatus = booking.getStatus();
        if ("COMPLETED".equals(currentStatus) || "CANCELLED".equals(currentStatus)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Cannot change status of a " + currentStatus + " booking"));
        }

        // Validate lifecycle transitions
        boolean validTransition = false;
        switch (currentStatus) {
            case "PENDING":
                validTransition = List.of("CONFIRMED", "CANCELLED", "REJECTED").contains(newStatus);
                break;
            case "CONFIRMED":
                validTransition = List.of("ASSIGNED", "CANCELLED").contains(newStatus);
                break;
            case "ASSIGNED":
                validTransition = List.of("ACCEPTED", "REJECTED", "CANCELLED").contains(newStatus);
                break;
            case "ACCEPTED":
                validTransition = List.of("DRIVER_ON_THE_WAY", "STARTED", "CANCELLED").contains(newStatus);
                break;
            case "DRIVER_ON_THE_WAY":
                validTransition = List.of("STARTED", "CANCELLED").contains(newStatus);
                break;
            case "STARTED":
                validTransition = List.of("IN_PROGRESS", "COMPLETED", "CANCELLED").contains(newStatus);
                break;
            case "IN_PROGRESS":
                validTransition = List.of("COMPLETED", "CANCELLED").contains(newStatus);
                break;
            default:
                validTransition = true;
                break;
        }

        if (!validTransition) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid status transition from " + currentStatus + " to " + newStatus));
        }

        booking.setStatus(newStatus);
        Booking saved = bookingRepository.save(booking);
        return ResponseEntity.ok(ApiResponse.success("Booking status updated to " + newStatus, saved));
    }
}
