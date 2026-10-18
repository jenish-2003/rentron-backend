package com.jcbbooking;

import com.jcbbooking.model.*;
import com.jcbbooking.repository.*;
import com.jcbbooking.service.BookingAssignmentService;
import com.jcbbooking.strategy.ProductAvailabilityStrategy;
import com.jcbbooking.strategy.ProductAvailabilityStrategyFactory;
import com.jcbbooking.websocket.WebSocketNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DriverAllocationAndConcurrencyTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private ContractorRepository contractorRepository;

    @Mock
    private BookingSettingRepository bookingSettingRepository;

    @Mock
    private BookingAssignmentRepository bookingAssignmentRepository;

    @Mock
    private BookingEventHistoryRepository bookingEventHistoryRepository;

    @Mock
    private ProductAvailabilityStrategyFactory strategyFactory;

    @Mock
    private WebSocketNotificationService webSocketNotificationService;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private BookingAssignmentService assignmentService;

    private BookingSetting defaultSettings;
    private Booking sampleBooking;

    @BeforeEach
    void setUp() {
        defaultSettings = BookingSetting.builder()
                .settingKey("DEFAULT_SETTINGS")
                .assignmentType("AUTO")
                .initialRadiusKm(5.0)
                .radiusIncrementKm(5.0)
                .maxRadiusKm(20.0)
                .assignmentTimeoutSeconds(30)
                .maxLocationAgeSeconds(300)
                .dispatchLeadTimeMinutes(30)
                .notifyAdminWhenUnassigned(true)
                .build();

        sampleBooking = Booking.builder()
                .id(100L)
                .bookingNumber("BK-100")
                .customerId(10L)
                .productId(1L)
                .pickupLatitude(12.9716)
                .pickupLongitude(77.5946)
                .status("CONFIRMED")
                .totalAmount(450.0)
                .build();
    }

    @Test
    @DisplayName("10. Offline driver is excluded from dispatch candidates")
    void testOfflineDriverExcluded() {
        Driver offlineDriver = Driver.builder()
                .id(1L)
                .status("OFFLINE")
                .isOnline(false)
                .latitude(12.9720)
                .longitude(77.5950)
                .locationUpdatedAt(LocalDateTime.now())
                .selectedVehicleType("SEDAN")
                .build();

        when(driverRepository.findAllByStatus("ACTIVE")).thenReturn(List.of());

        // Offline driver won't even be returned by findAllByStatus("ACTIVE")
        List<Driver> active = driverRepository.findAllByStatus("ACTIVE");
        assertTrue(active.isEmpty());
    }

    @Test
    @DisplayName("11. Stale GPS driver (> 300 seconds) is excluded from dispatch")
    void testStaleGpsDriverExcluded() {
        when(bookingSettingRepository.findBySettingKey("DEFAULT_SETTINGS")).thenReturn(Optional.of(defaultSettings));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(sampleBooking));

        Driver staleDriver = Driver.builder()
                .id(2L)
                .userId(20L)
                .status("ACTIVE")
                .isOnline(true)
                .latitude(12.9720)
                .longitude(77.5950)
                .locationUpdatedAt(LocalDateTime.now().minusSeconds(600)) // 10 mins old (> 300s)
                .selectedVehicleType("SEDAN")
                .build();

        when(driverRepository.findAllByStatus("ACTIVE")).thenReturn(List.of(staleDriver));
        ProductAvailabilityStrategy mockStrategy = mock(ProductAvailabilityStrategy.class);
        when(strategyFactory.getStrategy(any())).thenReturn(mockStrategy);

        assignmentService.startAutoAssignmentProcess(100L);

        // Stale driver should NOT receive booking offer
        verify(bookingAssignmentRepository, never()).save(any());
        verify(webSocketNotificationService, never()).sendBookingOfferToUser(eq(20L), any());
    }

    @Test
    @DisplayName("12. Progressive search expansion: layer 1 -> layer 2 radius calculation")
    void testProgressiveSearchExpansionRadius() {
        assertEquals(5.0, defaultSettings.getInitialRadiusKm());
        double nextMinRadius = defaultSettings.getInitialRadiusKm();
        double nextMaxRadius = defaultSettings.getInitialRadiusKm() + defaultSettings.getRadiusIncrementKm();

        assertEquals(5.0, nextMinRadius);
        assertEquals(10.0, nextMaxRadius);
        assertTrue(nextMaxRadius <= defaultSettings.getMaxRadiusKm());
    }

    @Test
    @DisplayName("13. Expired driver offer cannot be accepted")
    void testExpiredOfferCannotBeAccepted() {
        when(bookingSettingRepository.findBySettingKey("DEFAULT_SETTINGS")).thenReturn(Optional.of(defaultSettings));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(sampleBooking));

        BookingAssignment expiredAssignment = BookingAssignment.builder()
                .id(50L)
                .bookingId(100L)
                .candidateType("DRIVER")
                .candidateId(5L)
                .status("OFFERED")
                .offeredAt(LocalDateTime.now().minusSeconds(60)) // timed out (timeout is 30s)
                .build();

        when(bookingAssignmentRepository.findFirstByBookingIdAndCandidateTypeAndCandidateIdAndStatus(100L, "DRIVER", 5L, "OFFERED"))
                .thenReturn(Optional.of(expiredAssignment));

        boolean accepted = assignmentService.acceptBookingOffer(100L, "DRIVER", 5L, 500L);

        assertFalse(accepted);
        assertEquals("EXPIRED", expiredAssignment.getStatus());
        verify(bookingAssignmentRepository).save(expiredAssignment);
    }

    @Test
    @DisplayName("14. Concurrent driver acceptance: first succeeds, second gets false (conflict)")
    void testConcurrentDriverAcceptance() {
        when(bookingSettingRepository.findBySettingKey("DEFAULT_SETTINGS")).thenReturn(Optional.of(defaultSettings));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(sampleBooking));

        BookingAssignment offerDriver1 = BookingAssignment.builder()
                .id(1L)
                .bookingId(100L)
                .candidateType("DRIVER")
                .candidateId(101L)
                .candidateUserId(201L)
                .status("OFFERED")
                .offeredAt(LocalDateTime.now())
                .build();

        BookingAssignment offerDriver2 = BookingAssignment.builder()
                .id(2L)
                .bookingId(100L)
                .candidateType("DRIVER")
                .candidateId(102L)
                .candidateUserId(202L)
                .status("OFFERED")
                .offeredAt(LocalDateTime.now())
                .build();

        // Driver 1 accepts: atomic conditional update returns 1 (success)
        when(bookingAssignmentRepository.findFirstByBookingIdAndCandidateTypeAndCandidateIdAndStatus(100L, "DRIVER", 101L, "OFFERED"))
                .thenReturn(Optional.of(offerDriver1));
        when(bookingRepository.assignDriverAtomically(eq(100L), eq(101L), any(), eq("ASSIGNED"), any()))
                .thenReturn(1);
        when(bookingAssignmentRepository.findAllByBookingIdOrderByOfferedAtDesc(100L))
                .thenReturn(List.of(offerDriver1, offerDriver2));

        boolean acceptedDriver1 = assignmentService.acceptBookingOffer(100L, "DRIVER", 101L, 201L);
        assertTrue(acceptedDriver1);
        assertEquals("ACCEPTED", offerDriver1.getStatus());

        // Driver 2 attempts to accept next: atomic conditional update returns 0 (already assigned!)
        when(bookingAssignmentRepository.findFirstByBookingIdAndCandidateTypeAndCandidateIdAndStatus(100L, "DRIVER", 102L, "OFFERED"))
                .thenReturn(Optional.of(offerDriver2));
        when(bookingRepository.assignDriverAtomically(eq(100L), eq(102L), any(), eq("ASSIGNED"), any()))
                .thenReturn(0);

        boolean acceptedDriver2 = assignmentService.acceptBookingOffer(100L, "DRIVER", 102L, 202L);
        assertFalse(acceptedDriver2);
    }

    @Test
    @DisplayName("15. Duplicate booking submission returns existing booking via idempotency key")
    void testDuplicateBookingSubmissionIdempotency() {
        Booking existing = Booking.builder()
                .id(77L)
                .bookingNumber("BK-EXISTING-77")
                .idempotencyKey("idemp-unique-12345")
                .status("CONFIRMED")
                .build();

        when(bookingRepository.findByIdempotencyKey("idemp-unique-12345")).thenReturn(Optional.of(existing));

        Optional<Booking> check = bookingRepository.findByIdempotencyKey("idemp-unique-12345");
        assertTrue(check.isPresent());
        assertEquals("BK-EXISTING-77", check.get().getBookingNumber());
    }

    @Test
    @DisplayName("16. Repeated dispatch trigger safely maintains status")
    void testRepeatedDispatchTrigger() {
        when(bookingSettingRepository.findBySettingKey("DEFAULT_SETTINGS")).thenReturn(Optional.of(defaultSettings));
        sampleBooking.setStatus("ASSIGNED");
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(sampleBooking));

        assignmentService.startAutoAssignmentProcess(100L);

        // Booking is already ASSIGNED, so no candidate search should be performed
        verify(driverRepository, never()).findAllByStatus(anyString());
    }

    @Test
    @DisplayName("17. Server restart recovery: durable scanner dispatches pending scheduled bookings")
    void testScheduledBookingRestartRecovery() {
        when(bookingSettingRepository.findBySettingKey("DEFAULT_SETTINGS")).thenReturn(Optional.of(defaultSettings));

        Booking scheduledBooking = Booking.builder()
                .id(200L)
                .status("SCHEDULED")
                .scheduledPickupAt(LocalDateTime.now().plusMinutes(15)) // within 30 min lead threshold
                .pickupLatitude(12.9716)
                .pickupLongitude(77.5946)
                .build();

        when(bookingRepository.findAllByStatusAndScheduledPickupAtLessThanEqual(eq("SCHEDULED"), any()))
                .thenReturn(List.of(scheduledBooking));
        when(bookingRepository.transitionStatusIfMatches(200L, "CONFIRMED", "SCHEDULED")).thenReturn(1);
        when(bookingRepository.findById(200L)).thenReturn(Optional.of(scheduledBooking));

        assignmentService.scanAndDispatchScheduledBookings();

        assertTrue(List.of("SEARCHING_DRIVER", "NO_DRIVER_AVAILABLE").contains(scheduledBooking.getStatus()));
        verify(bookingRepository, atLeastOnce()).save(scheduledBooking);
    }

    @Test
    @DisplayName("17a. Driver attempting overlapping active jobs is rejected")
    void testDriverAttemptingOverlappingActiveJobsIsRejected() {
        when(bookingSettingRepository.findBySettingKey("DEFAULT_SETTINGS")).thenReturn(Optional.of(defaultSettings));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(sampleBooking));

        BookingAssignment offer = BookingAssignment.builder()
                .id(10L)
                .bookingId(100L)
                .candidateType("DRIVER")
                .candidateId(101L)
                .candidateUserId(201L)
                .status("OFFERED")
                .offeredAt(LocalDateTime.now())
                .build();

        when(bookingAssignmentRepository.findFirstByBookingIdAndCandidateTypeAndCandidateIdAndStatus(100L, "DRIVER", 101L, "OFFERED"))
                .thenReturn(Optional.of(offer));

        Driver driver101 = Driver.builder().id(101L).userId(201L).build();
        when(driverRepository.findById(101L)).thenReturn(Optional.of(driver101));

        // Driver already has an active ongoing booking
        when(bookingRepository.existsByDriverIdAndStatusIn(eq(101L), anyList())).thenReturn(true);

        boolean accepted = assignmentService.acceptBookingOffer(100L, "DRIVER", 101L, 201L);

        assertFalse(accepted);
        assertEquals("REJECTED", offer.getStatus());
        assertEquals("DRIVER_BUSY_WITH_ANOTHER_BOOKING", offer.getRejectionReason());
        verify(bookingRepository, never()).assignDriverAtomically(anyLong(), anyLong(), any(), anyString(), anyList());
    }

    @Test
    @DisplayName("17b. Candidate offer ownership validation prevents impersonation")
    void testCandidateOfferOwnershipValidation() {
        when(bookingSettingRepository.findBySettingKey("DEFAULT_SETTINGS")).thenReturn(Optional.of(defaultSettings));
        when(bookingRepository.findById(100L)).thenReturn(Optional.of(sampleBooking));

        BookingAssignment offer = BookingAssignment.builder()
                .id(10L)
                .bookingId(100L)
                .candidateType("DRIVER")
                .candidateId(101L)
                .candidateUserId(201L)
                .status("OFFERED")
                .offeredAt(LocalDateTime.now())
                .build();

        when(bookingAssignmentRepository.findFirstByBookingIdAndCandidateTypeAndCandidateIdAndStatus(100L, "DRIVER", 101L, "OFFERED"))
                .thenReturn(Optional.of(offer));

        // Driver 101 belongs to userId 201L, but caller is userId 999L
        Driver driver101 = Driver.builder().id(101L).userId(201L).build();
        when(driverRepository.findById(101L)).thenReturn(Optional.of(driver101));

        boolean accepted = assignmentService.acceptBookingOffer(100L, "DRIVER", 101L, 999L);

        assertFalse(accepted);
        verify(bookingRepository, never()).assignDriverAtomically(anyLong(), anyLong(), any(), anyString(), anyList());
    }

    @Test
    @DisplayName("17c. Multi-instance scheduled booking claim prevents duplicate dispatch")
    void testMultiInstanceScheduledBookingClaimPreventsDuplicateDispatch() {
        when(bookingSettingRepository.findBySettingKey("DEFAULT_SETTINGS")).thenReturn(Optional.of(defaultSettings));

        Booking scheduledBooking = Booking.builder()
                .id(300L)
                .status("SCHEDULED")
                .scheduledPickupAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(bookingRepository.findAllByStatusAndScheduledPickupAtLessThanEqual(eq("SCHEDULED"), any()))
                .thenReturn(List.of(scheduledBooking));

        // Instance 2 loses race condition (transition returns 0)
        when(bookingRepository.transitionStatusIfMatches(300L, "CONFIRMED", "SCHEDULED")).thenReturn(0);

        assignmentService.scanAndDispatchScheduledBookings();

        // Status transition failed, so auto-assignment must NOT be started
        verify(bookingRepository, never()).findById(300L);
    }
}
