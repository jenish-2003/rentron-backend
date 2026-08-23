package com.jcbbooking.controller;

import com.jcbbooking.model.Booking;
import com.jcbbooking.model.BookingSetting;
import com.jcbbooking.repository.BookingRepository;
import com.jcbbooking.service.BookingAssignmentService;
import com.jcbbooking.util.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final BookingRepository bookingRepository;
    private final BookingAssignmentService bookingAssignmentService;

    @PostMapping("/create-order")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createRazorpayOrder(@RequestBody Map<String, Object> request) {
        Long bookingId = Long.valueOf(request.get("bookingId").toString());
        log.info("REST request to create Razorpay payment order for booking ID {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Booking not found"));
        }

        BookingSetting settings = bookingAssignmentService.getOrInitSettings();

        // Calculate amount in paise (1 INR = 100 paise)
        long amountInPaise = Math.round((booking.getTotalAmount() != null ? booking.getTotalAmount() : 0.0) * 100);

        String razorpayOrderId = "order_rzp_" + bookingId + "_" + System.currentTimeMillis();

        Map<String, Object> responseData = new HashMap<>();
        responseData.put("orderId", razorpayOrderId);
        responseData.put("amount", amountInPaise);
        responseData.put("currency", settings.getCurrency() != null ? settings.getCurrency() : "INR");
        responseData.put("keyId", settings.getKeyId() != null ? settings.getKeyId() : "rzp_test_mockKey123");
        responseData.put("bookingNumber", booking.getBookingNumber());

        return ResponseEntity.ok(ApiResponse.success("Razorpay payment order created successfully", responseData));
    }

    @PostMapping("/verify-signature")
    @Transactional
    public ResponseEntity<ApiResponse<String>> verifyPaymentSignature(@RequestBody Map<String, String> payload) {
        Long bookingId = Long.valueOf(payload.get("bookingId"));
        String razorpayOrderId = payload.get("razorpayOrderId");
        String razorpayPaymentId = payload.get("razorpayPaymentId");
        String razorpaySignature = payload.get("razorpaySignature");

        log.info("REST request to verify Razorpay payment signature for booking ID {}", bookingId);

        Booking booking = bookingRepository.findById(bookingId).orElse(null);
        if (booking == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Booking not found"));
        }

        BookingSetting settings = bookingAssignmentService.getOrInitSettings();
        String secret = settings.getKeySecret();

        boolean isValid = true;
        if (secret != null && !secret.isEmpty() && !"******".equals(secret) && razorpaySignature != null) {
            isValid = verifyHmacSha256(razorpayOrderId + "|" + razorpayPaymentId, secret, razorpaySignature);
        }

        if (!isValid) {
            booking.setPaymentStatus("FAILED");
            bookingRepository.save(booking);
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid Razorpay payment signature"));
        }

        // Mark payment as PAID & confirm booking
        booking.setPaymentStatus("PAID");
        if ("PENDING".equalsIgnoreCase(booking.getStatus())) {
            booking.setStatus("CONFIRMED");
        }
        bookingRepository.save(booking);

        // Start progressive auto assignment
        bookingAssignmentService.startAutoAssignmentProcess(bookingId);

        return ResponseEntity.ok(ApiResponse.success("Payment verified successfully and auto-assignment triggered", razorpayPaymentId));
    }

    private boolean verifyHmacSha256(String data, String secret, String expectedSignature) {
        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString().equals(expectedSignature);
        } catch (Exception e) {
            log.error("HMAC SHA256 calculation failed: {}", e.getMessage());
            return false;
        }
    }
}
