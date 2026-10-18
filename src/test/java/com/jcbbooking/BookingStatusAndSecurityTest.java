package com.jcbbooking;

import com.jcbbooking.controller.BookingController;
import com.jcbbooking.controller.PaymentController;
import com.jcbbooking.model.*;
import com.jcbbooking.repository.*;
import com.jcbbooking.security.CustomUserDetails;
import com.jcbbooking.service.BookingAssignmentService;
import com.jcbbooking.util.ApiResponse;
import com.jcbbooking.websocket.WebSocketNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingStatusAndSecurityTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private BookingAssignmentService bookingAssignmentService;

    @Mock
    private DriverRepository driverRepository;

    @Mock
    private ContractorRepository contractorRepository;

    @Mock
    private BookingEventHistoryRepository bookingEventHistoryRepository;

    @Mock
    private BookingAssignmentRepository bookingAssignmentRepository;

    @Mock
    private WebSocketNotificationService webSocketNotificationService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private BookingController bookingController;

    @InjectMocks
    private PaymentController paymentController;

    private User customerUser1;
    private User customerUser2;
    private User driverUser1;
    private CustomUserDetails customerDetails1;
    private CustomUserDetails customerDetails2;
    private CustomUserDetails driverDetails1;
    private Booking sampleBooking;
    private Driver sampleDriver1;

    @BeforeEach
    void setUp() {
        customerUser1 = User.builder().id(101L).phone("9876543210").role(Role.CUSTOMER).build();
        customerUser2 = User.builder().id(102L).phone("9876543211").role(Role.CUSTOMER).build();
        driverUser1 = User.builder().id(201L).phone("9876543212").role(Role.DRIVER).build();

        customerDetails1 = new CustomUserDetails(customerUser1);
        customerDetails2 = new CustomUserDetails(customerUser2);
        driverDetails1 = new CustomUserDetails(driverUser1);

        sampleDriver1 = Driver.builder()
                .id(11L)
                .userId(201L)
                .fullName("Ravi Kumar")
                .phone("9876543212")
                .build();

        sampleBooking = Booking.builder()
                .id(1L)
                .bookingNumber("BK-TEST-1")
                .customerId(101L)
                .driverId(11L)
                .status("CONFIRMED")
                .paymentStatus("PENDING")
                .totalAmount(500.0)
                .otp("4521")
                .otpGeneratedAt(LocalDateTime.now())
                .otpExpiresAt(LocalDateTime.now().plusHours(24))
                .otpAttempts(0)
                .otpMaxAttempts(5)
                .build();
    }

    @Test
    @DisplayName("18. Invalid status transition returns HTTP 409 Conflict")
    void testInvalidStatusTransitionReturnsConflict() {
        // PENDING cannot transition directly to STARTED
        sampleBooking.setStatus("PENDING");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));

        ResponseEntity<ApiResponse<Booking>> response = bookingController.updateBookingStatus(
                1L, "STARTED", "Invalid skip", customerDetails1
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Invalid state transition"));
    }

    @Test
    @DisplayName("19. Unauthorized booking access returns HTTP 403 Forbidden")
    void testUnauthorizedBookingAccessReturnsForbidden() {
        // Booking belongs to customer 101L, but accessed by customer 102L
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));

        ResponseEntity<ApiResponse<Booking>> response = bookingController.getBookingById(1L, customerDetails2);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Unauthorized access"));
    }

    @Test
    @DisplayName("20. Duplicate payment verification returns idempotent success without duplicate dispatch")
    void testDuplicatePaymentReplayProtection() {
        sampleBooking.setPaymentStatus("PAID");
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));

        Map<String, String> payload = Map.of(
                "bookingId", "1",
                "razorpayOrderId", "order_123",
                "razorpayPaymentId", "pay_456",
                "razorpaySignature", "sig_789"
        );

        ResponseEntity<ApiResponse<String>> response = paymentController.verifyPaymentSignature(payload);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("idempotent"));

        // Auto assignment should NOT be re-triggered
        verify(bookingAssignmentService, never()).startAutoAssignmentProcess(any());
    }

    @Test
    @DisplayName("21. Invalid payment signature marks payment as FAILED")
    void testInvalidPaymentSignatureMarksFailed() {
        BookingSetting setting = BookingSetting.builder()
                .keySecret("actual_secret_key_12345")
                .build();
        when(bookingAssignmentService.getOrInitSettings()).thenReturn(setting);
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));

        Map<String, String> payload = Map.of(
                "bookingId", "1",
                "razorpayOrderId", "order_123",
                "razorpayPaymentId", "pay_456",
                "razorpaySignature", "invalid_bogus_signature"
        );

        ResponseEntity<ApiResponse<String>> response = paymentController.verifyPaymentSignature(payload);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertEquals("FAILED", sampleBooking.getPaymentStatus());
        verify(bookingRepository).save(sampleBooking);
    }

    @Test
    @DisplayName("22. Ride start OTP verification succeeds, sets otpVerifiedAt and nullifies plaintext OTP")
    void testRideStartOtpVerificationSuccess() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));
        when(driverRepository.findByUserId(201L)).thenReturn(Optional.of(sampleDriver1));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<ApiResponse<Booking>> response = bookingController.verifyOtpAndStart(1L, "4521", driverDetails1);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().isSuccess());
        Booking saved = response.getBody().getData();
        assertEquals("STARTED", saved.getStatus());
        assertNotNull(saved.getOtpVerifiedAt());
        assertNull(saved.getOtp(), "Plaintext OTP must be nullified for one-time use");
    }

    @Test
    @DisplayName("23. Expired ride start OTP returns HTTP 400 Bad Request")
    void testExpiredRideStartOtpReturnsBadRequest() {
        sampleBooking.setOtpExpiresAt(LocalDateTime.now().minusHours(1)); // expired
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));
        when(driverRepository.findByUserId(201L)).thenReturn(Optional.of(sampleDriver1));

        ResponseEntity<ApiResponse<Booking>> response = bookingController.verifyOtpAndStart(1L, "4521", driverDetails1);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("OTP_EXPIRED"));
    }

    @Test
    @DisplayName("24. Replay of already verified OTP returns HTTP 400 Bad Request")
    void testReplayOfAlreadyVerifiedOtpReturnsBadRequest() {
        sampleBooking.setOtpVerifiedAt(LocalDateTime.now().minusMinutes(10)); // already verified
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));
        when(driverRepository.findByUserId(201L)).thenReturn(Optional.of(sampleDriver1));

        ResponseEntity<ApiResponse<Booking>> response = bookingController.verifyOtpAndStart(1L, "4521", driverDetails1);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("OTP_ALREADY_USED"));
    }

    @Test
    @DisplayName("25. OTP verification locked out after maximum retry limit exceeded")
    void testOtpVerificationLockedOutAfterMaxAttempts() {
        sampleBooking.setOtpAttempts(5); // 5 failed attempts
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));
        when(driverRepository.findByUserId(201L)).thenReturn(Optional.of(sampleDriver1));

        ResponseEntity<ApiResponse<Booking>> response = bookingController.verifyOtpAndStart(1L, "4521", driverDetails1);

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("OTP_LOCKED"));
    }

    @Test
    @DisplayName("26. Unauthorized invoice access returns HTTP 403 Forbidden")
    void testUnauthorizedInvoiceAccessReturnsForbidden() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));

        // Booking belongs to customer 101L, customer 102L attempts access
        ResponseEntity<ApiResponse<Map<String, Object>>> response = bookingController.getInvoice(1L, customerDetails2);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Unauthorized access"));
    }

    @Test
    @DisplayName("27. Unauthorized live tracking access returns HTTP 403 Forbidden")
    void testUnauthorizedLiveTrackingAccessReturnsForbidden() {
        when(bookingRepository.findById(1L)).thenReturn(Optional.of(sampleBooking));

        // Customer 102 attempts to track customer 101's booking
        ResponseEntity<ApiResponse<Map<String, Object>>> response = bookingController.getLiveTracking(1L, customerDetails2);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
        assertTrue(response.getBody().getMessage().contains("Unauthorized access"));
    }

    @Test
    @DisplayName("28. Unauthorized candidate attempting to accept offer returns HTTP 403 Forbidden")
    void testUnauthorizedCandidateAcceptanceReturnsForbidden() {
        // Driver 11 belongs to user 201L. Calling as user 102L (customer)
        ResponseEntity<ApiResponse<String>> response = bookingController.acceptBookingOffer(1L, "DRIVER", 11L, customerDetails2);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertFalse(response.getBody().isSuccess());
    }
}
